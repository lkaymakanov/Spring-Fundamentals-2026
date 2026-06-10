package app.security;

//import app.model.dto.user.UserDto;
//import app.service.user.UserService;
import app.model.entity.User;
import app.model.entity.UserRole;
import app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.UUID;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    private static final Set<String> UNAUTHENTICATED_ENDPOINTS = Set.of("/", "/login", "/register", "/error");
    private static final Set<String> ADMIN_ENDPOINTS = Set.of("/users", "/reports");

    private final UserService userService;

    public SessionInterceptor(UserService userService) {
        this.userService = userService;
    }


    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String endpoint = request.getServletPath();
        //System.out.println("Entered in preHande: " + this.getClass());

        if (UNAUTHENTICATED_ENDPOINTS.contains(endpoint)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect("/login");
            return false;
        }

        UUID userId = (UUID) session.getAttribute("user_id");
        if (userId == null) {
            session.invalidate();
            response.sendRedirect("/login");
            return false;
        }

        User user = userService.getById(userId);

        if (!user.isActive()) {
            session.invalidate();
            response.sendRedirect("/login");
            return false;
        }

        if (ADMIN_ENDPOINTS.contains(endpoint) && !(user.getRole() == UserRole.ADMIN)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("You do not have permission to access this resource.");
            return false;
        }

        return true;
    }
}