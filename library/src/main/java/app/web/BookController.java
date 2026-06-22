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

/**
 * Admin-only endpoints for managing book records in the catalog.
 *
 * Access control:
 *  - Every endpoint checks currentUser.isAdmin() before processing.
 *  - Non-admins get an AccessDeniedException → handled by GlobalExceptionHandler → 403 page.
 *
 * All actions use POST-Redirect-Get (PRG) pattern:
 *  - Form submission happens via POST.
 *  - On success or failure, the user is redirected to /books (no resubmission on refresh).
 *  - Flash messages (success/error) are passed via RedirectAttributes.
 *
 * Note: unlike AuthorController, this one does NOT pre-check for related records before delete.
 * Book deletion is allowed even if borrow records exist — the BorrowRecord.book relation
 * uses @ManyToOne, and the foreign key behavior is configured in the entity (typically null on cascade,
 * or you can change to RESTRICT if you want to block delete). Be aware of this trade-off.
 */
@Controller
@RequestMapping("/admin/books")
public class BookController {

    private final BookService bookService;
    private final AuthorService authorService;
    private final CurrentUserHelper currentUser;

    public BookController(BookService bookService,
                          AuthorService authorService,
                          CurrentUserHelper currentUser) {
        this.bookService = bookService;
        this.authorService = authorService;
        this.currentUser = currentUser;
    }

    /**
     * Creates a new book from the modal form on /books.
     *
     * The request DTO includes an authorId — BookService.createBook() resolves this
     * to an Author entity and links it to the new book. If the authorId doesn't exist,
     * the service throws an exception (caught by GlobalExceptionHandler).
     */
    @PostMapping("/create")
    public ModelAndView createBook(@Valid @ModelAttribute("bookCreateRequest") BookCreateRequest request,
                                   BindingResult bindingResult,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        // Admin gate: deny non-admins immediately rather than letting them see partial state.
        if (!currentUser.isAdmin(session)) {
            throw new AccessDeniedException("Admin access required");
        }

        // Bean validation failure → bounce back with a generic error.
        // Specific field errors are shown via the form binding (Thymeleaf re-renders with errors).
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the errors in the form");
            return new ModelAndView("redirect:/books");
        }

        bookService.createBook(request);
        redirectAttributes.addFlashAttribute("successMessage", "Book added successfully!");
        return new ModelAndView("redirect:/books");
    }

    /**
     * Updates an existing book (title, ISBN, year, inventory, author link, cover URL).
     * Path variable is the book's UUID.
     */
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

        // BookService.updateBook throws BookNotFoundException if the ID doesn't exist,
        // which GlobalExceptionHandler turns into a 404 page.
        bookService.updateBook(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Book updated successfully!");
        return new ModelAndView("redirect:/books");
    }

    /**
     * Deletes a book by ID.
     *
     * Trade-off note: this method does NOT check for related BorrowRecords.
     * If active borrows reference this book, behavior depends on the FK configuration:
     *  - Default (no constraint): orphans remain in BorrowRecord with a null book.
     *  - RESTRICT constraint: DB throws an error → wrapped as a generic 500.
     *  - CASCADE: borrow records are deleted along with the book (loses history).
     *
     * If you want to prevent this, add a check here similar to AuthorController.deleteAuthor.
     */
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