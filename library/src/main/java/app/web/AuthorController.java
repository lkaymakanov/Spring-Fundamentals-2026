package app.web;

import app.component.CurrentUserHelper;
import app.exception.AccessDeniedException;
import app.exception.AuthorNotFoundException;
import app.model.dto.AuthorCreateRequest;
import app.model.dto.AuthorEditRequest;
import app.model.entity.Author;
import app.service.AuthorService;
import app.service.BookService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/authors")
public class AuthorController {

    private final AuthorService authorService;
    private final BookService bookService;
    private final CurrentUserHelper currentUser;

    public AuthorController(AuthorService authorService, BookService bookService, CurrentUserHelper currentUser) {
        this.authorService = authorService;
        this.bookService = bookService;
        this.currentUser = currentUser;
    }

    @PostMapping("/create")
    public ModelAndView createAuthor(@Valid @ModelAttribute("authorCreateRequest") AuthorCreateRequest request,
                                     BindingResult bindingResult,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!currentUser.isAdmin(session)) {
            throw new AccessDeniedException("Admin access required");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the errors in the form");
            return new ModelAndView("redirect:/authors");
        }

        authorService.createAuthor(request);
        redirectAttributes.addFlashAttribute("successMessage", "Author added successfully!");
        return new ModelAndView("redirect:/authors");
    }

    @PostMapping("/edit/{id}")
    public ModelAndView editAuthor(@PathVariable UUID id,
                                   @Valid @ModelAttribute("authorEditRequest") AuthorEditRequest request,
                                   BindingResult bindingResult,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (!currentUser.isAdmin(session)) {
            throw new AccessDeniedException("Admin access required");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the errors in the form");
            return new ModelAndView("redirect:/authors");
        }

        authorService.updateAuthor(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Author updated successfully!");
        return new ModelAndView("redirect:/authors");
    }

    @PostMapping("/delete/{id}")
    public ModelAndView deleteAuthor(@PathVariable UUID id,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!currentUser.isAdmin(session)) {
            throw new AccessDeniedException("Admin access required");
        }

        // Check if author has books — prevent deletion if so
        Author author = authorService.getById(id);
        long bookCount = bookService.getAllBooks().stream()
                .filter(b -> b.getAuthor() != null && b.getAuthor().getId().equals(id))
                .count();
        if (bookCount > 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete author — they have " + bookCount + " book(s) in the catalog.");
            return new ModelAndView("redirect:/authors");
        }

        authorService.deleteAuthor(id);
        redirectAttributes.addFlashAttribute("successMessage", "Author deleted successfully!");
        return new ModelAndView("redirect:/authors");
    }
}