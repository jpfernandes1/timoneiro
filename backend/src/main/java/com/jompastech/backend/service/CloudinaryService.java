package com.jompastech.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jompastech.backend.config.CloudinaryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.jompastech.backend.model.dto.cloudinary.CloudinaryUploadResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service class for handling Cloudinary image upload and management operations.
 * Provides methods for uploading single or multiple images to Cloudinary storage,
 * deleting images by their public ID, and performing image validation.
 *
 * @since 1.0
 */
@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Constructs a CloudinaryService with the provided configuration.
     * Initializes the Cloudinary client with configuration properties.
     *
     * @param cloudinaryConfig the Cloudinary configuration containing credentials
     *                         (cloud name, API key, API secret, and secure flag)
     */
    public CloudinaryService(CloudinaryConfig cloudinaryConfig) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryConfig.getCloudName(),
                "api_key", cloudinaryConfig.getApiKey(),
                "api_secret", cloudinaryConfig.getApiSecret(),
                "secure", cloudinaryConfig.isSecure()
        ));
    }

    /**
     * Uploads a single image file to Cloudinary storage.
     * Validates the file before upload and organizes it in the specified folder structure.
     *
     * @param file the image file to upload (must not be null or empty)
     * @return CloudinaryUploadResult containing the secure URL and public ID of the uploaded image
     * @throws IllegalArgumentException if the file is null or empty
     * @throws IOException if an I/O error occurs during the upload process
     * @throws RuntimeException if the upload result doesn't contain required data
     */
    public CloudinaryUploadResult uploadImage(MultipartFile file) throws IOException {
        validateImageFile(file);

        log.debug("Uploading image: {} ({} bytes, type: {})",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "timoneiro/boats",
                        "public_id", generatePublicId(file.getOriginalFilename()),
                        "overwrite", false,
                        "resource_type", "auto"
                )
        );

        String secureUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        if (secureUrl == null || publicId == null) {
            throw new RuntimeException("Cloudinary upload failed: No secure URL or public ID returned");
        }

        log.info("Image uploaded successfully. Public ID: {}, URL: {}", publicId, secureUrl);

        return new CloudinaryUploadResult(
                secureUrl,
                publicId,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
        );
    }

    /**
     * Uploads multiple image files to Cloudinary storage.
     * Processes each file individually and collects all successful uploads.
     * If any single upload fails, the exception is propagated immediately.
     *
     * @param files list of image files to upload
     * @return list of CloudinaryUploadResult for all successfully uploaded images
     * @throws IOException if an I/O error occurs during any upload
     */
    public List<CloudinaryUploadResult> uploadImages(List<MultipartFile> files) throws IOException {
        List<CloudinaryUploadResult> uploadResults = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    CloudinaryUploadResult result = uploadImage(file);
                    uploadResults.add(result);
                } catch (IOException e) {
                    log.error("Failed to upload image: {}", file.getOriginalFilename(), e);
                    throw new IOException("Failed to upload image: " + file.getOriginalFilename(), e);
                }
            }
        }

        return uploadResults;
    }

    /**
     * Deletes an image from Cloudinary storage using its public ID.
     *
     * @param publicId the public identifier of the image in Cloudinary
     * @throws IOException if an error occurs during the deletion process
     */
    public void deleteImage(String publicId) throws IOException {
        log.debug("Deleting image with public ID: {}", publicId);

        Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

        log.info("Image deletion result for public ID {}: {}", publicId, result.get("result"));
    }

    /**
     * Deletes multiple images from Cloudinary storage.
     *
     * @param publicIds list of public identifiers to delete
     * @throws IOException if an error occurs during any deletion
     */
    public void deleteImages(List<String> publicIds) throws IOException {
        for (String publicId : publicIds) {
            try {
                deleteImage(publicId);
            } catch (IOException e) {
                log.error("Failed to delete image with public ID: {}", publicId, e);
                // Continue trying to delete others, then throw exception
                throw new IOException("Failed to delete multiple images", e);
            }
        }
    }

    /**
     * Validates an image file for type and size constraints.
     *
     * @param file the image file to validate
     * @throws IllegalArgumentException if the file is null, empty, or fails validation
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be null or empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid file type. Only image files are allowed");
        }

        long maxSize = 10 * 1024 * 1024; // 10MB in bytes
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
    }

    /**
     * Generates a unique public ID for an image file.
     * Combines timestamp, original filename, and random component for uniqueness.
     *
     * @param originalFilename the original filename of the image
     * @return a unique public ID string
     */
    private String generatePublicId(String originalFilename) {
        String sanitizedFilename = originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_")
                : "image";

        return String.format("boat_%d_%s_%d",
                System.currentTimeMillis(),
                sanitizedFilename,
                (int) (Math.random() * 1000));
    }
}