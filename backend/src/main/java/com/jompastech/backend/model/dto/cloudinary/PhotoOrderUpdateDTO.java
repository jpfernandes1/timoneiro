package com.jompastech.backend.model.dto.cloudinary;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoOrderUpdateDTO {

    @NotEmpty(message = "The list of photo IDs cannot be empty.")
    private List<Long> photoIdsInOrder; // List of photo IDs in the desired order
}