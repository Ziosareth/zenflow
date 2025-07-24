package it.zenflow.mapper;

import it.zenflow.dto.UserDTO;
import it.zenflow.model.rbac.Role;
import it.zenflow.model.rbac.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the UserMapper implementation.
 */
@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    private UserDTO userDTO;
    private User user;
    private Set<Role> roles;

    @BeforeEach
    public void setup() {
        // Create test roles
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");

        roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);

        // Create test DTO
        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setTenant("test-tenant");
        userDTO.setEnabled(true);

        // Create test entity
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123"); // Sensitive field
        user.setEnabled(true);
        user.setPasswordChangeRequired(false); // Security-related field
        user.setResetToken("token123"); // Security-related field
        user.setResetTokenExpiry(LocalDateTime.now().plusDays(1)); // Security-related field
        user.setTenant("test-tenant");
        user.setRoles(roles); // Security-related field
    }

    @Test
    public void testToEntity() {
        // Act
        User result = userMapper.toEntity(userDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userDTO.getId());
        assertThat(result.getUsername()).isEqualTo(userDTO.getUsername());
        assertThat(result.getEmail()).isEqualTo(userDTO.getEmail());
        assertThat(result.getTenant()).isEqualTo(userDTO.getTenant());
        assertThat(result.isEnabled()).isEqualTo(userDTO.isEnabled());
        
        // Verify ignored fields are not set
        assertThat(result.getPassword()).isNull();
        assertThat(result.isPasswordChangeRequired()).isFalse();
        assertThat(result.getResetToken()).isNull();
        assertThat(result.getResetTokenExpiry()).isNull();
        assertThat(result.getRoles()).isEmpty();
    }

    @Test
    public void testToDto() {
        // Act
        UserDTO result = userMapper.toDto(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getTenant()).isEqualTo(user.getTenant());
        assertThat(result.isEnabled()).isEqualTo(user.isEnabled());
        
        // Verify sensitive information is not included in the DTO
        // Note: This is implicit since UserDTO doesn't have these fields
    }

    @Test
    public void testToDto_WithNullValues() {
        // Arrange
        User userWithNulls = new User();
        userWithNulls.setId(2L);
        userWithNulls.setUsername("nulluser");
        // Other fields are null

        // Act
        UserDTO result = userMapper.toDto(userWithNulls);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userWithNulls.getId());
        assertThat(result.getUsername()).isEqualTo(userWithNulls.getUsername());
        assertThat(result.getEmail()).isNull();
        assertThat(result.getTenant()).isNull();
        assertThat(result.isEnabled()).isTrue(); // Default value for enabled in User class is true
    }
}