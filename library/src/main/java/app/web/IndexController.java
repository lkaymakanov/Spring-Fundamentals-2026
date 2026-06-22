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

/**
 * Handles unauthenticated flows: landing page, login, register, logout,
 * and the post-login home/dashboard.
 *
 * Endpoints:
 *  - GET  /         : landing page (redirects to /home if logged in, else to /login)
 *  - GET  /login    : login form
 *  - POST /login    : submit credentials
 *  - GET  /register : registration form
 *  - POST /register : create new account
 *  - GET  /home     : post-login dashboard
 *  - GET  /logout   : invalidate session
 *
 * Authentication model:
 *  - We use a simple session-based approach: user_id is stored in HttpSession on login.
 *  - No Spring Security filter chain — just a custom SessionInterceptor that gates protected routes.
 *  - The CurrentUserHelper component centralizes session → user info lookups.
 */
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

    /** Landing page — public, no auth required. */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Renders the login form.
     * Pre-builds an empty UserLoginRequest so Thymeleaf's th:object binding works.
     */
    @GetMapping("/login")
    public ModelAndView getLoginPage() {
        UserLoginRequest userLoginRequest = UserLoginRequest.builder().build();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        modelAndView.addObject("userLoginRequest", userLoginRequest);

        return modelAndView;
    }

    /**
     * Processes a login attempt.
     *
     * Flow:
     *  1. Bean validation (blank fields, length limits) → re-render form on failure.
     *  2. userService.login() → throws InvalidCredentialsException on bad credentials
     *     (handled by GlobalExceptionHandler, which redirects to /login with a flash error).
     *  3. On success, store user_id in the session and redirect to /home.
     *
     * Note: HttpServletResponse is injected but unused here — it's kept for future
     * enhancements (e.g. setting cookies, custom headers). Safe to remove if not needed.
     */
    @PostMapping("/login")
    public ModelAndView login(@Valid UserLoginRequest userLoginRequest,
                              BindingResult bindingResult,
                              HttpSession httpSession,
                              HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            // Re-render the form. Field-level errors are shown by Thymeleaf via th:errors.
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("login");
            return modelAndView;
        }

        // Service throws InvalidCredentialsException on bad credentials — GlobalExceptionHandler catches it.
        UserDto user = userService.login(userLoginRequest);

        // Store the user ID in the session. All authenticated controllers read this back via CurrentUserHelper.
        httpSession.setAttribute("user_id", user.getId());

        return new ModelAndView("redirect:/home");
    }

    /**
     * Renders the registration form.
     * Pre-builds an empty UserRegisterRequest so the form has a th:object to bind to.
     */
    @GetMapping("/register")
    public ModelAndView getRegisterPage() {
        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder().build();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register");
        modelAndView.addObject("userRegisterRequest", userRegisterRequest);

        return modelAndView;
    }

    /**
     * Creates a new user account.
     *
     * Flow:
     *  1. Bean validation → re-render form on failure.
     *  2. userService.register() → throws UsernameAlreadyExistsException on conflict
     *     (handled by GlobalExceptionHandler, redirects to /register with a flash error).
     *  3. On success, redirect to /login (the user can now sign in with their new account).
     */
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

    

    /**
     * Renders the post-login dashboard.
     *
     * Shows:
     *  - User info (username, role)
     *  - All books in the catalog (so the user can browse and borrow from the dashboard too)
     *  - Borrow stats: active count + overdue count (so the user sees their own activity at a glance)
     *  - isAdmin flag (drives the visibility of admin-only sections in the template)
     *
     * Auth: relies on the SessionInterceptor to block unauthenticated access.
     * Direct cast (UUID) httpSession.getAttribute("user_id") is safe here because the interceptor
     * has already verified the user is logged in.
     */
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

    /**
     * Logs the user out by invalidating the session.
     * After invalidation, any subsequent request is treated as anonymous
     * (the session interceptor will redirect them to /login).
     */
    @GetMapping("/logout")
    public ModelAndView getLogoutPage(HttpSession httpSession) {
        httpSession.invalidate();
        return new ModelAndView("redirect:/");
    }
}