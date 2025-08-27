package com.jompastech.backend.mapper;

import com.jompastech.backend.model.dto.UserRequestDTO;
import com.jompastech.backend.model.dto.UserResponseDTO;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.security.dto.AuthResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    // Basic convertion DTO → Entity
    User toEntity(UserRequestDTO dto);

    // Basic convertion Entity → ResponseDTO
    UserResponseDTO toResponseDTO(User user);

    // For update operations (PUT);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntityFromRequest(UserRequestDTO dto, @MappingTarget User entity);

    // This converts User + token into → AuthResponseDTO
    default AuthResponseDTO toAuthResponseDTO(User user, String token) {
        if (user == null || token == null) {
            return null;
        }

        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        return response;
    }
}
