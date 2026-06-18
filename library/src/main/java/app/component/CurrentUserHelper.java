package app.component;


import app.model.dto.UserDto;
import app.model.entity.UserRole;
import app.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserHelper {

    private final UserService userService;

    public CurrentUserHelper(UserService userService) {
        this.userService = userService;
    }

    public UserDto get(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("user_id");
        if (userId == null) return null;
        try {
            return userService.getById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isAdmin(HttpSession session) {
        UserDto user = get(session);
        return user != null && user.getRole() == UserRole.ADMIN;
    }
}