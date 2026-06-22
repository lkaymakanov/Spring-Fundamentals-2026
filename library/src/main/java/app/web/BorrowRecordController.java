package app.web;

import app.component.CurrentUserHelper;
import app.model.dto.BorrowRecordDto;
import app.model.dto.UserDto;
import app.service.BorrowRecordService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * User-facing endpoints for the borrow/return flow.
 *
 * Endpoints:
 *  - GET  /my-borrows: view current and past borrows
 *  - POST /books/{bookId}/borrow: borrow a book
 *  - POST /my-borrows/{recordId}/return: return a borrowed book
 *
 * Access control:
 *  - All endpoints require a logged-in user. Anonymous users are redirected to /login.
 *  - No admin gate — any user can borrow/return their own books.
 *  - Ownership checks happen inside the service layer
 *    (BorrowRecordService.returnBook verifies the user owns the record).
 *
 * Error handling:
 *  - Service exceptions (BookNotAvailableException, BorrowLimitException, etc.)
 *    are caught here and converted to flash messages, not propagated to the error page.
 *  - This is intentional: borrow errors are user-actionable, not server errors.
 */
@Controller
public class BorrowRecordController {

    private final BorrowRecordService borrowService;
    private final CurrentUserHelper currentUser;

    public BorrowRecordController(BorrowRecordService borrowService, CurrentUserHelper currentUser) {
        this.borrowService = borrowService;
        this.currentUser = currentUser;
    }

    /**
     * Displays the "My Borrows" page.
     * Shows all borrow records (active + returned) plus summary stats (active count, overdue count).
     */
    @GetMapping("/my-borrows")
    public ModelAndView myBorrows(HttpSession session) {
        // Extract the user ID from the session.
        // Note: this uses a different helper path than the other endpoints — see the helpers note below.
        UserDto userDto = currentUser.get(session);
        UUID userId = userDto == null ? null : userDto.getId();
        if (userId == null) {
            return new ModelAndView("redirect:/login");
        }

        // Pull both the records and the summary stats in one shot.
        // Service handles DTO mapping — controller never sees raw entities.
        List<BorrowRecordDto> borrows = borrowService.getMyBorrows(userId);
        long activeCount = borrowService.getActiveBorrowCount(userId);
        long overdueCount = borrowService.getOverdueCount(userId);

        ModelAndView mav = new ModelAndView("my-borrows");
        mav.addObject("borrows", borrows);
        mav.addObject("activeCount", activeCount);
        mav.addObject("overdueCount", overdueCount);
        return mav;
    }

    /**
     * Handles the "Borrow" button on a book card.
     *
     * Service layer enforces:
     *  - copiesAvailable > 0 (BookNotAvailableException)
     *  - user active count < 5 (BorrowLimitException)
     *  - no duplicate borrow of the same book (DuplicateBorrowException)
     *
     * On success, the user sees a confirmation with the book title and due date.
     * On failure, the service's exception message is shown to the user
     * (messages are written to be user-friendly, e.g. "Sorry, no copies of 'X' are available").
     */
    @PostMapping("/books/{bookId}/borrow")
    public String borrowBook(@PathVariable UUID bookId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        UUID userId = currentUser.getUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            // The service returns the DTO so we can include the book title and due date in the success message.
            BorrowRecordDto borrow = borrowService.borrowBook(userId, bookId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "📚 '%s' borrowed! Due %s."
                            .formatted(borrow.getBookTitle(),
                                    borrow.getDueDate().toLocalDate()));
        } catch (RuntimeException e) {
            // Catch service exceptions (BookNotAvailableException, BorrowLimitException, DuplicateBorrowException).
            // Messages are pre-written to be user-readable, so we pass them straight through.
            // Other RuntimeExceptions (DB errors, etc.) would also land here — they show their raw message,
            // which is fine for a learning project but you'd want more specific handling in production.
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/books";
    }

    /**
     * Handles the "Return" button on a borrow row.
     *
     * Service layer enforces:
     *  - record exists (BorrowNotFoundException)
     *  - user owns the record (AccessDeniedException)
     *  - book was not already returned (IllegalStateException)
     *
     * On success, inventory is incremented and the record status changes to RETURNED.
     */
    @PostMapping("/my-borrows/{recordId}/return")
    public String returnBook(@PathVariable UUID recordId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        UserDto userDto = currentUser.get(session);
        UUID userId = userDto == null ? null : userDto.getId();
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            BorrowRecordDto borrow = borrowService.returnBook(userId, recordId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ '%s' returned successfully!"
                            .formatted(borrow.getBookTitle()));
        } catch (RuntimeException e) {
            // Same pattern as borrow: surface service messages directly to the user.
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/my-borrows";
    }
}