package app.web;

import app.component.CurrentUserHelper;
import app.model.dto.ChangePasswordRequest;
import app.model.dto.UserDto;
import app.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Handles the user profile page and password change flow.
 *
 * Endpoints:
 *  - GET  /profile:                  view profile info and change-password form
 *  - POST /profile/change-password:  submit new password (after validation)
 *
 * Access control:
 *  - Anonymous users are redirected to /login.
 *  - Users can only see/edit their own profile (no path variable for userId —
 *    the user is always derived from the session).
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final CurrentUserHelper currentUser;

    public ProfileController(UserService userService, CurrentUserHelper currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    /**
     * Renders the profile page.
     * Pre-builds an empty ChangePasswordRequest so the form has a th:object to bind to
     * (Thymeleaf requires a non-null model attribute for form binding).
     */
    @GetMapping
    public ModelAndView profilePage(HttpSession session) {
        UUID userId = currentUser.getUserId(session);
        // Anonymous users are redirected to login instead of throwing a 500
        // (defense in depth: the session interceptor should already handle this, but
        //  this guard makes the controller self-contained and easier to reason about).
        if (userId == null) {
            return new ModelAndView("redirect:/login");
        }

        UserDto user = userService.getById(userId);

        ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", user);
        mav.addObject("changePasswordRequest", ChangePasswordRequest.builder().build());
        return mav;
    }

    /**
     * Processes a password change request.
     *
     * Validation layers:
     *  1. Bean validation (@Valid on the DTO) — runs first via @ModelAttribute binding.
     *  2. Service-level business rules — caught with try/catch, surfaced as flash errors.
     *
     * Both cases redirect back to /profile with a flash message instead of rendering
     * the form again — the user sees feedback at the top of the page on every outcome.
     */
    @PostMapping("/change-password")
    public ModelAndView changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
                                       BindingResult bindingResult,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        UUID userId = currentUser.getUserId(session);
        if (userId == null) {
            return new ModelAndView("redirect:/login");
        }

        // Layer 1: bean validation failures (blank fields, too-short passwords).
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the errors in the form");
            return new ModelAndView("redirect:/profile");
        }

        // Layer 2: service-level business rules.
        // The service throws InvalidCredentialsException, InvalidPasswordException, etc.
        // with user-friendly messages that we pass straight through to the UI.
        try {
            userService.changePassword(userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "🔒 Password changed successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return new ModelAndView("redirect:/profile");
    }
}