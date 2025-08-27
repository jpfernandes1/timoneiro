package com.jompastech.backend.mapper;

import com.jompastech.backend.model.dto.BoatRequestDTO;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface BoatMapper {

    // RequestDTO -> Entity
    @Mapping(source = "addressId", target = "address.id")
    @Mapping(source = "ownerId", target = "owner.id")
    Boat toEntity(BoatRequestDTO dto);

    // Basic convertion Entity → public ResponseDTO
    @Mapping(source = "address.city", target = "city")
    @Mapping(source = "address.state", target = "state")
    @Mapping(source = "owner.name", target = "ownerName")
    BoatResponseDTO toResponseDTO(Boat boat);

}
