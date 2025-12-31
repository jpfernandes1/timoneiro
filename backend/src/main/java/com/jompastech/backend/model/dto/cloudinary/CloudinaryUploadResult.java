package com.jompastech.backend.model.dto.cloudinary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the result of a Cloudinary image upload.
 * Contains both the public URL and the public ID required for future management.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudinaryUploadResult {

    /**
     * The secure (HTTPS) URL of the uploaded image.
     */
    private String url;

    /**
     * The public ID of the image in Cloudinary.
     * Required for deletion and updates.
     */
    private String publicId;

    /**
     * The original filename of the uploaded image.
     */
    private String fileName;

    /**
     * The size of the uploaded image in bytes.
     */
    private Long fileSize;

    /**
     * The MIME type of the uploaded image.
     */
    private String contentType;
}