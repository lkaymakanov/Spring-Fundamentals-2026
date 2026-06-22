package app.web;

import app.component.CurrentUserHelper;
import app.model.dto.AuthorCreateRequest;
import app.model.dto.AuthorEditRequest;
import app.model.dto.BookCreateRequest;
import app.model.dto.BookEditRequest;
import app.model.entity.Author;
import app.model.entity.Book;
import app.service.AuthorService;
import app.service.BookService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public-facing read-only pages for browsing the catalog.
 *
 * Endpoints:
 *  - GET /books:    paginated/searchable book list
 *  - GET /authors:  paginated/searchable author list
 *
 * Differences from admin controllers:
 *  - No @RequestMapping class-level prefix (these are public URLs, not /admin/...).
 *  - GET-only — no mutations. All create/edit/delete go through the admin controllers.
 *  - Both pages support admins differently: extra data is added to the model
 *    (e.g. the list of authors for the "Add Book" dropdown) and admin-only UI is rendered via th:if.
 */
@Controller
public class BrowseController {

    private final BookService bookService;
    private final AuthorService authorService;
    private final CurrentUserHelper currentUser;

    public BrowseController(BookService bookService,
                            AuthorService authorService,
                            CurrentUserHelper currentUser) {
        this.bookService = bookService;
        this.authorService = authorService;
        this.currentUser = currentUser;
    }

    /**
     * Displays the book catalog.
     *
     * Query params:
     *  - search (optional): case-insensitive substring search on book titles.
     *    Empty/blank search → shows all books.
     *
     * Model attributes:
     *  - books: list of Book entities (or search-filtered list)
     *  - search: the current search term (for repopulating the input field)
     *  - isAdmin, isLogged: drive role-based UI in the template
     *  - authors + bookCreateRequest + bookEditRequest: only added for admins
     *    (the create/edit modals need the author list for the dropdown)
     */
    @GetMapping("/books")
    public ModelAndView browseBooks(@RequestParam(value = "search", required = false) String search,
                                    HttpSession session) {
        ModelAndView modelAndView = new ModelAndView("books");

        // Branch on search: empty/blank search shows everything, otherwise filter by title.
        // Repository's findByTitleContainingIgnoreCase handles case-insensitivity.
        List<Book> books;
        if (search != null && !search.isBlank()) {
            books = bookService.searchByTitle(search);
        } else {
            books = bookService.getAllBooks();
        }
        modelAndView.addObject("books", books);
        modelAndView.addObject("search", search);
        modelAndView.addObject("isAdmin", currentUser.isAdmin(session));
        modelAndView.addObject("isLogged", currentUser.isLogged(session));

        // Only fetch extra data needed by the admin modals (create/edit book dropdowns).
        // For regular users, this is wasted work — skip it to keep the page lightweight.
        if (currentUser.isAdmin(session)) {
            modelAndView.addObject("authors", authorService.getAllAuthors());
            // Pre-build empty DTOs so the modal forms have a th:object to bind to.
            modelAndView.addObject("bookCreateRequest", BookCreateRequest.builder().build());
            modelAndView.addObject("bookEditRequest", BookEditRequest.builder().build());
        }

        return modelAndView;
    }

    /**
     * Displays the author catalog.
     *
     * Query params:
     *  - search (optional): case-insensitive substring search on author names.
     *
     * Model attributes:
     *  - authors: list of Author entities (or search-filtered list)
     *  - search: the current search term
     *  - isAdmin: drives the visibility of create/edit/delete buttons
     *  - authorCreateRequest + authorEditRequest: only added for admins (modal form binding)
     *  - bookCounts: map of authorId → number of books they have, used by the UI to show
     *    "you can't delete this author" warnings and to power the delete guard in the controller.
     */
    @GetMapping("/authors")
    public ModelAndView browseAuthors(@RequestParam(value = "search", required = false) String search,
                                      HttpSession session) {
        ModelAndView modelAndView = new ModelAndView("author-management");

        List<Author> authors;
        if (search != null && !search.isBlank()) {
            authors = authorService.searchByName(search);
        } else {
            authors = authorService.getAllAuthors();
        }
        modelAndView.addObject("authors", authors);
        modelAndView.addObject("search", search);
        modelAndView.addObject("isAdmin", currentUser.isAdmin(session));

        if (currentUser.isAdmin(session)) {
            // Pre-build empty DTOs so the modal forms have a th:object to bind to.
            modelAndView.addObject("authorCreateRequest", AuthorCreateRequest.builder().build());
            modelAndView.addObject("authorEditRequest", AuthorEditRequest.builder().build());
        }

        // Build a map of authorId → book count for the UI.
        // Why? The template needs to show "Author X has 3 books" next to each row
        // and the AuthorController uses this same data to block delete on non-empty authors.
        //
        // Performance note: this is O(n) over all books. For a small library it's fine,
        // but at scale you'd add a countByAuthorId() method to BookRepository and call it
        // only for the authors being displayed (with batch lookup, not N+1 queries).
        Map<UUID, Long> bookCounts = new HashMap<>();
        bookService.getAllBooks().forEach(book -> {
            if (book.getAuthor() != null) {
                bookCounts.merge(book.getAuthor().getId(), 1L, Long::sum);
            }
        });
        modelAndView.addObject("bookCounts", bookCounts);

        return modelAndView;
    }
}