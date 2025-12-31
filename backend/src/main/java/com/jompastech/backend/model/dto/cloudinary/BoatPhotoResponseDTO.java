package com.jompastech.backend.model.dto.cloudinary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoatPhotoResponseDTO {
    private Long id;
    private String photoUrl;
    private String publicId;
    private String fileName;
    private Integer ordem;
    private Long boatId;
}