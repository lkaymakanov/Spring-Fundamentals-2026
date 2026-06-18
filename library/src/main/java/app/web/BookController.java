package app.web;

import app.component.CurrentUserHelper;
import app.exception.AccessDeniedException;
import app.model.dto.BookCreateRequest;
import app.model.dto.BookEditRequest;
import app.service.AuthorService;
import app.service.BookService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/books")
public class BookController {

    private final BookService bookService;
    private final AuthorService authorService;
    private final CurrentUserHelper currentUser;

    public BookController(BookService bookService, AuthorService authorService, CurrentUserHelper currentUser) {
        this.bookService = bookService;
        this.authorService = authorService;
        this.currentUser = currentUser;
    }

    @PostMapping("/create")
    public ModelAndView createBook(@Valid @ModelAttribute("bookCreateRequest") BookCreateRequest request,
                                   BindingResult bindingResult,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (!currentUser.isAdmin(session)) {
            throw new AccessDeniedException("Admin access required");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the errors in the form");
            return new ModelAndView("redirect:/books");
        }

        bookService.createBook(request);
        redirectAttributes.addFlashAttribute("successMessage", "Book added successfully!");
        return new ModelAndView("redirect:/books");
    }

    @PostMapping("/edit/{id}")
    public ModelAndView editBook(@PathVariable UUID id,
                                 @Valid @ModelAttribute("bookEditRequest") BookEditRequest request,
                                 BindingResult bindingResult,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!currentUser.isAdmin(session)) {
            throw new AccessDeniedException("Admin access required");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the errors in the form");
            return new ModelAndView("redirect:/books");
        }

        bookService.updateBook(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Book updated successfully!");
        return new ModelAndView("redirect:/books");
    }

    @PostMapping("/delete/{id}")
    public ModelAndView deleteBook(@PathVariable UUID id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (!currentUser.isAdmin(session)) {
            throw new AccessDeniedException("Admin access required");
        }
        bookService.deleteBook(id);
        redirectAttributes.addFlashAttribute("successMessage", "Book deleted successfully!");
        return new ModelAndView("redirect:/books");
    }
}