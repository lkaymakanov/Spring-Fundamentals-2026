package app.web;

import app.component.CurrentUserHelper;
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

        // Only fetch authors if admin (for the modals)
        if (currentUser.isAdmin(session)) {
            modelAndView.addObject("authors", authorService.getAllAuthors());
            modelAndView.addObject("bookCreateRequest", BookCreateRequest.builder().build());
            modelAndView.addObject("bookEditRequest", BookEditRequest.builder().build());
        }

        return modelAndView;
    }
}