package com.jompastech.backend.mapper;

import com.jompastech.backend.model.dto.BoatRequestDTO;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {AddressMapper.class}
)
public interface BoatMapper {

    // RequestDTO -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "address", ignore = true) //  Doesn't map directly, will be hanled by service
    @Mapping(target = "photos", source = "photos")
    @Mapping(target = "length", source = "length")
    @Mapping(target = "speed", source = "speed")
    @Mapping(target = "fabrication", source = "fabrication")
    @Mapping(target = "amenities", source = "amenities")
    @Mapping(target = "pricePerHour", source = "pricePerHour")
    Boat toEntity(BoatRequestDTO dto);

    // Entity → ResponseDTO
    @Mapping(source = "address.city", target = "city")
    @Mapping(source = "address.state", target = "state")
    @Mapping(source = "address.marina", target = "marina")
    @Mapping(source = "owner.name", target = "ownerName")
    @Mapping(source = "owner.id", target = "ownerId")
    @Mapping(source = "photos", target = "photos")
    BoatResponseDTO toResponseDTO(Boat boat);

    // Auxiliar Method to get the first photo
    @Named("firstPhoto")
    static String firstPhoto(List<String> photos) {
        return (photos != null && !photos.isEmpty()) ? photos.get(0) : null;
    }
}