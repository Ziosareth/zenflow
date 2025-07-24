package it.zenflow.mapper;

import it.zenflow.dto.UserDTO;
import it.zenflow.model.rbac.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for the User entity and its DTO.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Converts a UserDTO to a User entity.
     * Ignores sensitive fields like password and security-related fields.
     */
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "passwordChangeRequired", ignore = true)
    @Mapping(target = "resetToken", ignore = true)
    @Mapping(target = "resetTokenExpiry", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User toEntity(UserDTO dto);

    /**
     * Converts a User entity to a UserDTO.
     * Sensitive information like password is not included in the DTO.
     */
    UserDTO toDto(User user);
}