package app.model.dto;

import app.model.entity.UserRole;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private UserRole role;
    private boolean active ;
    private LocalDateTime createdAt;

}