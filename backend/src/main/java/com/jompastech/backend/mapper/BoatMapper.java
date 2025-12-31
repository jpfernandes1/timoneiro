package com.jompastech.backend.mapper;

import com.jompastech.backend.model.dto.BoatRequestDTO;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatPhoto;
import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        uses = {AddressMapper.class}
)
public interface BoatMapper {

    // RequestDTO -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "address", ignore = true) //  Doesn't map directly, will be handled by service
    @Mapping(target = "photos", ignore = true) // Will be handled separately
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
    @Mapping(source = "photos", target = "photos", qualifiedByName = "extractPhotoUrls")
    BoatResponseDTO toResponseDTO(Boat boat);

    // Auxiliary method for extracting URLs from photos sorted by order.
    @Named("extractPhotoUrls")
    static List<String> extractPhotoUrls(List<BoatPhoto> photos) {
        if (photos == null || photos.isEmpty()) {
            return List.of();
        }

        return photos.stream()
                .sorted(Comparator.comparing(BoatPhoto::getOrdem))
                .map(BoatPhoto::getPhotoUrl)
                .collect(Collectors.toList());
    }

    // Optional auxiliary method: take the first photo (useful for thumbnails)
    @Named("firstPhotoUrl")
    static String firstPhotoUrl(List<BoatPhoto> photos) {
        if (photos == null || photos.isEmpty()) {
            return null;
        }

        return photos.stream()
                .sorted(Comparator.comparing(BoatPhoto::getOrdem))
                .map(BoatPhoto::getPhotoUrl)
                .findFirst()
                .orElse(null);
    }
}