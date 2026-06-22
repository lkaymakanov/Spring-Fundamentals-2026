package app.web;

import app.component.CurrentUserHelper;
import app.exception.AccessDeniedException;
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

import java.util.UUID;

/**
 * Admin-only endpoints for managing author records in the catalog.
 *
 * Access control:
 *  - Every endpoint checks currentUser.isAdmin() before processing.
 *  - Non-admins get an AccessDeniedException → handled by GlobalExceptionHandler → 403 page.
 *
 * All actions use POST-Redirect-Get (PRG) pattern:
 *  - Form submission happens via POST.
 *  - On success or failure, the user is redirected to /authors (no resubmission on refresh).
 *  - Flash messages (success/error) are passed via RedirectAttributes.
 *
 * The actual page rendering (list, edit modals) is handled by the public BrowseController
 * at /authors — this controller only handles mutations.
 */
@Controller
@RequestMapping("/admin/authors")
public class AuthorController {

    private final AuthorService authorService;
    private final BookService bookService;
    private final CurrentUserHelper currentUser;

    public AuthorController(AuthorService authorService,
                            BookService bookService,
                            CurrentUserHelper currentUser) {
        this.authorService = authorService;
        this.bookService = bookService;
        this.currentUser = currentUser;
    }

    /**
     * Creates a new author from the modal form on /authors.
     *
     * Flow:
     *  1. Admin check (else 403).
     *  2. Bean validation on the request DTO (name required, length limits, etc.).
     *  3. Delegate to AuthorService for persistence.
     *  4. Redirect to /authors with success banner.
     */
    @PostMapping("/create")
    public ModelAndView createAuthor(@Valid @ModelAttribute("authorCreateRequest") AuthorCreateRequest request,
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
            return new ModelAndView("redirect:/authors");
        }

        authorService.createAuthor(request);
        redirectAttributes.addFlashAttribute("successMessage", "Author added successfully!");
        return new ModelAndView("redirect:/authors");
    }

    /**
     * Updates an existing author's name and biography.
     * Path variable is the author's UUID.
     */
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

        // AuthorService.updateAuthor throws AuthorNotFoundException if the ID doesn't exist,
        // which GlobalExceptionHandler turns into a 404 page.
        authorService.updateAuthor(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Author updated successfully!");
        return new ModelAndView("redirect:/authors");
    }

    /**
     * Deletes an author, but only if they have no books in the catalog.
     *
     * Why the pre-check?
     *  - Authors are referenced by Book entities. Deleting an author with books
     *    would leave books with null author pointers (or fail with a foreign key violation).
     *  - Better UX: tell the admin exactly why the delete was blocked
     *    ("author has 3 book(s)") rather than showing a generic DB error.
     */
    @PostMapping("/delete/{id}")
    public ModelAndView deleteAuthor(@PathVariable UUID id,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!currentUser.isAdmin(session)) {
            throw new AccessDeniedException("Admin access required");
        }

        // Count books that reference this author.
        // Stream + filter is O(n) over all books — fine for a small library,
        // but for a large catalog you'd add a countByAuthorId() method to BookRepository.
        Author author = authorService.getById(id);
        long bookCount = bookService.getAllBooks().stream()
                .filter(b -> b.getAuthor() != null && b.getAuthor().getId().equals(id))
                .count();

        if (bookCount > 0) {
            // Show the actual count in the error so the admin knows what to do next.
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete author — they have " + bookCount + " book(s) in the catalog.");
            return new ModelAndView("redirect:/authors");
        }

        authorService.deleteAuthor(id);
        redirectAttributes.addFlashAttribute("successMessage", "Author deleted successfully!");
        return new ModelAndView("redirect:/authors");
    }
}