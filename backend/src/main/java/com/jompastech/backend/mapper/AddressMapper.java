package com.jompastech.backend.mapper;

import com.jompastech.backend.model.dto.AddressFullResponseDTO;
import com.jompastech.backend.model.dto.AddressRequestDTO;
import com.jompastech.backend.model.dto.AddressResponseDTO;
import com.jompastech.backend.model.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    // RequestDTO -> Entity
    Address toEntity(AddressRequestDTO dto);

    // Basic convertion Entity → private ResponseDTO
    AddressFullResponseDTO toFullResponseDTO(Address entity);

    // Basic convertion Entity → public ResponseDTO
    AddressResponseDTO toPublicResponseDTO(Address entity);


}
