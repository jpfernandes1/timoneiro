package com.jompastech.backend.model.dto.basicDTO;

import com.jompastech.backend.model.entity.User;
import lombok.Data;

/**
 * Basic DTO for user entity representation in API responses.
 *
 * <p>Contains minimal user information suitable for scenarios where
 * full user details are not required, such as chat messages,
 * comment authors, or listing owners.</p>
 *
 * <p><b>Security Note:</b> Exposes only non-sensitive user information.
 * Consider context-specific requirements before adding additional fields.</p>
 */
@Data
public class UserBasicDTO {

    /**
     * Unique identifier of the user.
     */
    private Long id;

    /**
     * User's full name for display purposes.
     */
    private String name;

    /**
     * User's email address.
     *
     * <p><b>Privacy Consideration:</b> Evaluate if email exposure
     * is necessary for each specific use case.</p>
     */
    private String email;

    /**
     * Constructs a UserBasicDTO from User entity.
     *
     * @param user the User entity to convert to DTO
     */
    public UserBasicDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
    }

    /**
     * All-args constructor for manual DTO creation.
     *
     * @param id the user identifier
     * @param name the user's name
     * @param email the user's email
     */
    public UserBasicDTO(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /**
     * Default constructor for serialization/deserialization.
     */
    public UserBasicDTO() {}
}