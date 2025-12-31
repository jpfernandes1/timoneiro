package com.jompastech.backend.service;

import com.jompastech.backend.model.dto.cloudinary.BoatPhotoResponseDTO;
import com.jompastech.backend.model.dto.cloudinary.PhotoOrderUpdateDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatPhoto;
import com.jompastech.backend.repository.BoatPhotoRepository;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.model.dto.cloudinary.CloudinaryUploadResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {

    private final BoatRepository boatRepository;
    private final BoatPhotoRepository boatPhotoRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Adds new photos to an existing boat.
     *
     * @param boatId the ID of the boat
     * @param files the image files to upload
     * @return list of created photo DTOs
     * @throws IOException if upload fails
     */
    @Transactional
    public List<BoatPhotoResponseDTO> addPhotosToBoat(Long boatId, List<MultipartFile> files) throws IOException {
        Boat boat = boatRepository.findById(boatId)
                .orElseThrow(() -> new EntityNotFoundException("Boat not found with ID: " + boatId));

        // Validate file count
        long existingPhotoCount = boatPhotoRepository.countByBoatId(boatId);
        if (existingPhotoCount + files.size() > 20) {
            throw new IllegalArgumentException("Maximum 20 photos allowed per boat");
        }

        List<CloudinaryUploadResult> uploadResults = new ArrayList<>();
        List<BoatPhoto> newPhotos = new ArrayList<>();

        try {
            // Upload images to Cloudinary
            uploadResults = cloudinaryService.uploadImages(files);

            // Determine starting order
            int startOrder = (int) existingPhotoCount;

            // Create BoatPhoto entities
            for (int i = 0; i < uploadResults.size(); i++) {
                CloudinaryUploadResult result = uploadResults.get(i);

                BoatPhoto photo = new BoatPhoto();
                photo.setPhotoUrl(result.getUrl());
                photo.setPublicId(result.getPublicId());
                photo.setFileName(result.getFileName());
                photo.setOrdem(startOrder + i);
                photo.setBoat(boat);

                newPhotos.add(photo);
            }

            // Save all photos
            boatPhotoRepository.saveAll(newPhotos);

            log.info("Added {} photos to boat ID: {}", newPhotos.size(), boatId);

            return newPhotos.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // Rollback uploaded images from Cloudinary
            if (!uploadResults.isEmpty()) {
                rollbackUploads(uploadResults);
            }
            throw e;
        }
    }

    /**
     * Deletes a specific photo from a boat and from Cloudinary.
     *
     * @param boatId the ID of the boat
     * @param photoId the ID of the photo to delete
     * @throws IOException if deletion from Cloudinary fails
     */
    @Transactional
    public void deletePhoto(Long boatId, Long photoId) throws IOException {
        BoatPhoto photo = boatPhotoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException("Photo not found with ID: " + photoId));

        // Verify the photo belongs to the boat
        if (!photo.getBoat().getId().equals(boatId)) {
            throw new IllegalArgumentException("Photo does not belong to the specified boat");
        }

        // Delete from Cloudinary
        cloudinaryService.deleteImage(photo.getPublicId());

        // Delete from database
        boatPhotoRepository.delete(photo);

        // Reorder remaining photos
        reorderPhotosAfterDeletion(boatId, photo.getOrdem());

        log.info("Deleted photo ID: {} from boat ID: {}", photoId, boatId);
    }

    /**
     * Updates the display order of photos for a boat.
     *
     * @param boatId the ID of the boat
     * @param orderUpdate DTO containing photo IDs in desired order
     */
    @Transactional
    public void updatePhotoOrder(Long boatId, PhotoOrderUpdateDTO orderUpdate) {
        List<Long> photoIds = orderUpdate.getPhotoIdsInOrder();

        // Verify all photos belong to the boat
        List<BoatPhoto> boatPhotos = boatPhotoRepository.findByBoatIdOrderByOrdemAsc(boatId);
        Map<Long, BoatPhoto> photoMap = boatPhotos.stream()
                .collect(Collectors.toMap(BoatPhoto::getId, p -> p));

        for (Long photoId : photoIds) {
            if (!photoMap.containsKey(photoId)) {
                throw new IllegalArgumentException("Photo ID " + photoId + " does not belong to boat ID " + boatId);
            }
        }

        // Update order
        for (int i = 0; i < photoIds.size(); i++) {
            BoatPhoto photo = photoMap.get(photoIds.get(i));
            photo.setOrdem(i);
        }

        boatPhotoRepository.saveAll(boatPhotos);
        log.info("Updated photo order for boat ID: {}", boatId);
    }

    /**
     * Gets all photos for a specific boat.
     *
     * @param boatId the ID of the boat
     * @return list of photo DTOs
     */
    @Transactional(readOnly = true)
    public List<BoatPhotoResponseDTO> getBoatPhotos(Long boatId) {
        return boatPhotoRepository.findByBoatIdOrderByOrdemAsc(boatId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Deletes all photos for a boat (useful when deleting the boat).
     *
     * @param boatId the ID of the boat
     * @throws IOException if deletion from Cloudinary fails
     */
    @Transactional
    public void deleteAllBoatPhotos(Long boatId) throws IOException {
        List<BoatPhoto> photos = boatPhotoRepository.findByBoatIdOrderByOrdemAsc(boatId);

        // Delete from Cloudinary
        List<String> publicIds = photos.stream()
                .map(BoatPhoto::getPublicId)
                .collect(Collectors.toList());

        if (!publicIds.isEmpty()) {
            cloudinaryService.deleteImages(publicIds);
        }

        // Delete from database
        boatPhotoRepository.deleteByBoatId(boatId);

        log.info("Deleted all {} photos for boat ID: {}", photos.size(), boatId);
    }

    /**
     * Reorders photos after a deletion to maintain sequential order.
     */
    private void reorderPhotosAfterDeletion(Long boatId, Integer deletedOrder) {
        List<BoatPhoto> remainingPhotos = boatPhotoRepository.findByBoatIdOrderByOrdemAsc(boatId);

        // Decrease order for photos that were after the deleted one
        for (BoatPhoto photo : remainingPhotos) {
            if (photo.getOrdem() > deletedOrder) {
                photo.setOrdem(photo.getOrdem() - 1);
            }
        }

        boatPhotoRepository.saveAll(remainingPhotos);
    }

    /**
     * Rolls back uploaded images from Cloudinary in case of failure.
     */
    private void rollbackUploads(List<CloudinaryUploadResult> uploadResults) {
        log.warn("Rolling back {} uploaded images due to failure", uploadResults.size());

        List<String> publicIds = uploadResults.stream()
                .map(CloudinaryUploadResult::getPublicId)
                .collect(Collectors.toList());

        try {
            cloudinaryService.deleteImages(publicIds);
            log.info("Rollback completed: {} images deleted", publicIds.size());
        } catch (IOException e) {
            log.error("Failed to delete images during rollback. Manual cleanup required for public IDs: {}",
                    publicIds, e);
        }
    }

    /**
     * Converts BoatPhoto entity to response DTO.
     */
    private BoatPhotoResponseDTO convertToResponseDTO(BoatPhoto photo) {
        return new BoatPhotoResponseDTO(
                photo.getId(),
                photo.getPhotoUrl(),
                photo.getPublicId(),
                photo.getFileName(),
                photo.getOrdem(),
                photo.getBoat().getId()
        );
    }
}