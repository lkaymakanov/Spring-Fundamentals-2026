package app.web;

import app.component.CurrentUserHelper;
import app.model.dto.AuthorCreateRequest;
import app.model.dto.AuthorEditRequest;
import app.model.entity.Author;
import app.model.entity.Book;
import app.service.AuthorService;
import app.service.BookService;
import app.model.dto.BookCreateRequest;
import app.model.dto.BookEditRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
public class BrowseController {

    private final BookService bookService;
    private final AuthorService authorService;
    private final CurrentUserHelper currentUser;

    public BrowseController(BookService bookService, AuthorService authorService, CurrentUserHelper currentUser) {
        this.bookService = bookService;
        this.authorService = authorService;
        this.currentUser = currentUser;
    }

    @GetMapping("/books")
    public ModelAndView browseBooks(@RequestParam(value = "search", required = false) String search,
                                    HttpSession session) {
        ModelAndView modelAndView = new ModelAndView("books");

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

        // Only fetch authors if admin (for the modals)
        if (currentUser.isAdmin(session)) {
            modelAndView.addObject("authors", authorService.getAllAuthors());
            modelAndView.addObject("bookCreateRequest", BookCreateRequest.builder().build());
            modelAndView.addObject("bookEditRequest", BookEditRequest.builder().build());
        }

        return modelAndView;
    }


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
            modelAndView.addObject("authorCreateRequest", AuthorCreateRequest.builder().build());
            modelAndView.addObject("authorEditRequest", AuthorEditRequest.builder().build());

            // Pass book counts per author for the "delete guard" message
            java.util.Map<UUID, Long> bookCounts = new java.util.HashMap<>();
            bookService.getAllBooks().forEach(book -> {
                if (book.getAuthor() != null) {
                    bookCounts.merge(book.getAuthor().getId(), 1L, Long::sum);
                }
            });
            modelAndView.addObject("bookCounts", bookCounts);
        }

        return modelAndView;
    }
}