package com.jompastech.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jompastech.backend.exception.EntityNotFoundException;
import com.jompastech.backend.model.dto.BoatRequestDTO;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.dto.cloudinary.BoatPhotoResponseDTO;
import com.jompastech.backend.model.dto.cloudinary.PhotoOrderUpdateDTO;
import com.jompastech.backend.model.entity.BoatPhoto;
import com.jompastech.backend.security.service.UserDetailsImpl;
import com.jompastech.backend.service.BoatService;
import com.jompastech.backend.service.CloudinaryService;
import com.jompastech.backend.model.dto.cloudinary.CloudinaryUploadResult;
import com.jompastech.backend.service.PhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/boats")
@RequiredArgsConstructor
@Tag(name = "Boats", description = "Operations related to boat management")
public class BoatController {

    private final BoatService boatService;
    private final CloudinaryService cloudinaryService;
    private final PhotoService photoService;

    @Operation(
            summary = "Create a new boat",
            description = "Creates a new boat with optional image uploads to Cloudinary storage"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Boat created successfully",
                    content = @Content(schema = @Schema(implementation = BoatResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or failed image upload"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BoatResponseDTO> createBoat(
            @Parameter(
                    description = "Boat data in JSON format",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BoatRequestDTO.class)
                    )
            )
            @RequestPart("boat") String boatJson,

            @Parameter(
                    description = "Boat images (optional, max 10 files, 10MB each)",
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(
                                            type = "string",
                                            format = "binary"
                                    )
                            )
                    )
            )
            @RequestPart(value = "images", required = false) List<MultipartFile> images,

            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String email = userDetails.getUsername();

        log.info("Creating new boat. JSON received: {} chars, for user: {}", boatJson.length(), email);

        // Check if email is null (authentication failed)
        if (email == null) {
            log.error("User email is null - authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Parse JSON to DTO
            ObjectMapper objectMapper = new ObjectMapper();
            BoatRequestDTO dto = objectMapper.readValue(boatJson, BoatRequestDTO.class);

            log.info("Parsed boat DTO: {}, images: {}", dto.getName(), images != null ? images.size() : 0);

            List<CloudinaryUploadResult> uploadedImages = new ArrayList<>();
            List<BoatPhoto> boatPhotos = new ArrayList<>();

            try {
                // 1. Process images if provided
                if (images != null && !images.isEmpty()) {
                    // Validate quantity
                    if (images.size() > 10) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                    }

                    // Upload images to Cloudinary
                    uploadedImages = cloudinaryService.uploadImages(images);

                    // Create BoatPhoto entities from upload results
                    for (int i = 0; i < uploadedImages.size(); i++) {
                        CloudinaryUploadResult result = uploadedImages.get(i);
                        BoatPhoto boatPhoto = new BoatPhoto();
                        boatPhoto.setPhotoUrl(result.getUrl());
                        boatPhoto.setPublicId(result.getPublicId());
                        boatPhoto.setFileName(result.getFileName());
                        boatPhoto.setOrdem(i); // Order based on position in list

                        boatPhotos.add(boatPhoto);
                    }
                }

                // 2. Save boat with photos using the email
                BoatResponseDTO savedBoat = boatService.saveWithPhotos(dto, boatPhotos, email);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedBoat);

            } catch (Exception e) {
                log.error("Failed to create boat: {}", e.getMessage(), e);

                // 3. Rollback: delete uploaded images from Cloudinary if boat creation failed
                if (!uploadedImages.isEmpty()) {
                    rollbackCloudinaryUploads(uploadedImages);
                }

                // Return appropriate error response
                if (e instanceof IllegalArgumentException || e instanceof IOException) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse boat JSON: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
    }

    /**
     * Rollback uploaded images from Cloudinary in case of failure.
     */
    private void rollbackCloudinaryUploads(List<CloudinaryUploadResult> uploadedImages) {
        log.warn("Rolling back {} uploaded images due to boat creation failure", uploadedImages.size());

        List<String> publicIds = new ArrayList<>();
        for (CloudinaryUploadResult result : uploadedImages) {
            publicIds.add(result.getPublicId());
        }

        try {
            cloudinaryService.deleteImages(publicIds);
            log.info("Rollback completed: {} images deleted from Cloudinary", publicIds.size());
        } catch (IOException ex) {
            log.error("Failed to delete images during rollback. Manual cleanup required for public IDs: {}",
                    publicIds, ex);
            // Do not rethrow the exception to avoid overwriting the original exception.
        }
    }

    @Operation(
            summary = "Get boat by ID",
            description = "Retrieves a specific boat by its unique identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Boat found",
                    content = @Content(schema = @Schema(implementation = BoatResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Boat not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BoatResponseDTO> getBoat(
            @Parameter(description = "ID of the boat to retrieve", required = true)
            @PathVariable Long id) {

        BoatResponseDTO boat = boatService.findById(id);
        return ResponseEntity.ok(boat);
    }

    @Operation(
            summary = "Get all boats",
            description = "Retrieves a list of all available boats"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of boats retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BoatResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<BoatResponseDTO>> getAllBoats() {
        return ResponseEntity.ok(boatService.findAllBoats());
    }

    @Operation(
            summary = "Get boat photos",
            description = "Retrieves all photos for a specific boat"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photos retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Boat not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{boatId}/photos")
    public ResponseEntity<List<BoatPhotoResponseDTO>> getBoatPhotos(
            @Parameter(description = "ID of the boat", required = true)
            @PathVariable Long boatId) {

        try {
            List<BoatPhotoResponseDTO> photos = photoService.getBoatPhotos(boatId);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            log.error("Error getting photos for boat ID {}: {}", boatId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Add photos to boat",
            description = "Uploads and adds new photos to an existing boat"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photos added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or maximum photos exceeded"),
            @ApiResponse(responseCode = "404", description = "Boat not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/{boatId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<BoatPhotoResponseDTO>> addPhotosToBoat(
            @Parameter(description = "ID of the boat", required = true)
            @PathVariable Long boatId,

            @Parameter(description = "Images to upload (max 10 files, 10MB each)")
            @RequestPart(value = "images", required = true) List<MultipartFile> images,

            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String email = userDetails.getUsername();

        log.info("Adding {} photos to boat ID: {}, user: {}", images.size(), boatId, email);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Validate number of images
            if (images.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null);
            }

            List<BoatPhotoResponseDTO> addedPhotos = photoService.addPhotosToBoat(boatId, images);
            return ResponseEntity.ok(addedPhotos);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error adding photos to boat ID {}: {}", boatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Delete boat photo",
            description = "Deletes a specific photo from a boat and from Cloudinary"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Photo deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Boat or photo not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{boatId}/photos/{photoId}")
    public ResponseEntity<Void> deleteBoatPhoto(
            @Parameter(description = "ID of the boat", required = true)
            @PathVariable Long boatId,

            @Parameter(description = "ID of the photo to delete", required = true)
            @PathVariable Long photoId,

            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String email = userDetails.getUsername();

        log.info("Deleting photo ID: {} from boat ID: {}, user: {}", photoId, boatId, email);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            photoService.deletePhoto(boatId, photoId);
            return ResponseEntity.noContent().build();

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error deleting photo ID {} from boat ID {}: {}", photoId, boatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Update photo order",
            description = "Updates the display order of photos for a boat"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo order updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid photo IDs"),
            @ApiResponse(responseCode = "404", description = "Boat not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{boatId}/photos/order")
    public ResponseEntity<Void> updatePhotoOrder(
            @Parameter(description = "ID of the boat", required = true)
            @PathVariable Long boatId,

            @Parameter(description = "Photo order update data", required = true)
            @Valid @RequestBody PhotoOrderUpdateDTO orderUpdate,

            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String email = userDetails.getUsername();

        log.info("Updating photo order for boat ID: {}, user: {}", boatId, email);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            photoService.updatePhotoOrder(boatId, orderUpdate);
            return ResponseEntity.ok().build();

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error updating photo order for boat ID {}: {}", boatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Get boats by current user with pagination",
            description = "Retrieves a paginated list of boats owned by the currently authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of boats retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BoatResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/my-boats")
    public ResponseEntity<Page<BoatResponseDTO>> getMyBoatsPaginated(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sort) {

        String email = userDetails.getUsername();

        log.info("GET /api/boats/my-boats requested by user: {}", email);

        if (email == null) {
            log.error("User email is null - authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
            Page<BoatResponseDTO> boats = boatService.findBoatsByUserPaginated(email, pageable);
            return ResponseEntity.ok(boats);
        } catch (EntityNotFoundException e) {
            log.error("User not found with email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error getting user boats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Update boat",
            description = "Updates an existing boat with new information"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Boat updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Boat not found"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "User is not the owner of the boat")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BoatResponseDTO> updateBoat(
            @Parameter(description = "ID of the boat to update", required = true)
            @PathVariable Long id,

            @Parameter(description = "Updated boat data", required = true)
            @Valid @RequestBody BoatRequestDTO boatDTO,

            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String email = userDetails.getUsername();

        log.info("Updating boat ID: {}, user: {}", id, email);

        if (email == null) {
            log.error("User email is null - authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var updatedBoat = boatService.updateBoat(id, boatDTO, email);
            return ResponseEntity.ok(updatedBoat);
        } catch (RuntimeException e) {
            log.error("Error updating boat ID {}: {}", id, e.getMessage());

            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else if (e.getMessage().contains("not the owner")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
    }
}