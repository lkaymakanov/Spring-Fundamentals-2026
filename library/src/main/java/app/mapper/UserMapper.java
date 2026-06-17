package app.mapper;

import app.model.dto.UserDto;
import app.model.entity.User;

public class UserMapper {

    public static UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}
