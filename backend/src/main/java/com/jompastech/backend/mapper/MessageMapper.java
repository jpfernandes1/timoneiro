package com.jompastech.backend.mapper;

import com.jompastech.backend.model.dto.MessageResponseDTO;
import com.jompastech.backend.model.dto.basicDTO.UserBasicDTO;
import com.jompastech.backend.model.entity.Message;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting Message entities to DTOs.
 *
 * Separates data transformation logic from business logic
 * following Single Responsibility Principle.
 */
@Component
public class MessageMapper {

    /**
     * Converts Message entity to MessageResponseDTO.
     *
     * Transforms entity data into API-friendly format including
     * nested user information and context identifiers.
     *
     * @param message the Message entity to convert
     * @return MessageResponseDTO with all necessary data for client
     */
    public MessageResponseDTO toDTO(Message message) {
        var userBasicDTO = new UserBasicDTO(
                message.getUser().getId(),
                message.getUser().getName(),
                message.getUser().getEmail()
        );

        return new MessageResponseDTO(
                message.getId(),
                message.getContent(),
                message.getSentAt(),
                userBasicDTO,
                message.getBooking() != null ? message.getBooking().getId() : null,
                message.getBoat() != null ? message.getBoat().getId() : null
        );
    }
}