package app.web;

import app.component.CurrentUserHelper;
import app.exception.UserNotFoundException;
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

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final CurrentUserHelper currentUser;

    public ProfileController(UserService userService, CurrentUserHelper currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ModelAndView profilePage(HttpSession session) {
        UUID userId = currentUser.getUserId(session);
        if (userId == null) {
            return new ModelAndView("redirect:/login");
        }

        UserDto user = userService.getById(userId);

        ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", user);
        mav.addObject("changePasswordRequest", ChangePasswordRequest.builder().build());
        return mav;
    }

    @PostMapping("/change-password")
    public ModelAndView changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
                                       BindingResult bindingResult,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        UUID userId = currentUser.getUserId(session);
        if (userId == null) {
            return new ModelAndView("redirect:/login");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the errors in the form");
            return new ModelAndView("redirect:/profile");
        }

        try {
            userService.changePassword(userId, request);
            redirectAttributes.addFlashAttribute("successMessage", "🔒 Password changed successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return new ModelAndView("redirect:/profile");
    }
}