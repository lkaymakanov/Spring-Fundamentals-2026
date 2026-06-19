package app.web;

import app.component.CurrentUserHelper;
import app.model.dto.UserDto;
import app.model.dto.UserLoginRequest;
import app.model.dto.UserRegisterRequest;
import app.model.entity.Book;
import app.service.BookService;
import app.service.BorrowRecordService;
import app.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
public class IndexController {

    private final UserService userService;
    private final BookService bookService;
    private final BorrowRecordService borrowService;
    private final CurrentUserHelper currentUserHelper;

    public IndexController(UserService userService,
                           BookService bookService,
                           BorrowRecordService borrowService,
                           CurrentUserHelper currentUserHelper) {
        this.bookService = bookService;
        this.borrowService = borrowService;
        this.userService = userService;
        this.currentUserHelper = currentUserHelper;
    }

    @GetMapping("/")
    public String index() {

        return "index";
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage() {
        UserLoginRequest userLoginRequest = UserLoginRequest.builder().build();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        modelAndView.addObject("userLoginRequest", userLoginRequest);

        return modelAndView;
    }

    @PostMapping("/login")
    public ModelAndView login(@Valid UserLoginRequest userLoginRequest,
                              BindingResult bindingResult,
                              HttpSession httpSession,
                              HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("login");
            return modelAndView;
        }

        UserDto user = userService.login(userLoginRequest);
        httpSession.setAttribute("user_id", user.getId());

        return new ModelAndView("redirect:/home");
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage() {
        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder().build();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register");
        modelAndView.addObject("userRegisterRequest", userRegisterRequest);

        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView registerUser(@Valid UserRegisterRequest userRegisterRequest,
                                     BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("register");
            return modelAndView;
        }

        userService.register(userRegisterRequest);

        return new ModelAndView("redirect:/login");
    }

    /*@GetMapping("/home")
    public ModelAndView getHomePage(HttpSession httpSession) {

        UUID userUUID = (UUID) httpSession.getAttribute("user_id");

        UserDto user = userService.getById(userUUID);

        ModelAndView modelAndView = new ModelAndView("home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }*/

    @GetMapping("/home")
    public ModelAndView getHomePage(HttpSession httpSession) {

        UUID userUUID = (UUID) httpSession.getAttribute("user_id");

        UserDto user = userService.getById(userUUID);

        List<Book> allBooks = bookService.getAllBooks();




        ModelAndView modelAndView = new ModelAndView("home");
        modelAndView.addObject("user", user);
        modelAndView.addObject("books", allBooks);
        modelAndView.addObject("borrowedCount", borrowService.getActiveBorrowCount(userUUID));
        modelAndView.addObject("dueSoonCount", borrowService.getOverdueCount(userUUID));
        modelAndView.addObject("isAdmin", currentUserHelper.isAdmin(httpSession));
        return modelAndView;
    }

    @GetMapping("/logout")
    public ModelAndView getLogoutPage(HttpSession httpSession) {
        httpSession.invalidate();
        return new ModelAndView("redirect:/");
    }
}
