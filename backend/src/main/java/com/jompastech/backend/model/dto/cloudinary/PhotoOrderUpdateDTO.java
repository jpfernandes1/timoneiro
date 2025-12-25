package com.jompastech.backend.model.dto.cloudinary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoOrderUpdateDTO {
    private List<Long> photoIdsInOrder; // List of photo IDs in the desired order
}