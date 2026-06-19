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

@Controller
public class BorrowRecordController {

    private final BorrowRecordService borrowService;
    private final CurrentUserHelper currentUser;

    public BorrowRecordController(BorrowRecordService borrowService, CurrentUserHelper currentUser) {
        this.borrowService = borrowService;
        this.currentUser = currentUser;
    }

    @GetMapping("/my-borrows")
    public ModelAndView myBorrows(HttpSession session) {
        UserDto userDto = currentUser.get(session);
        UUID userId = userDto == null ? null : userDto.getId();
        if (userId == null) {
            return new ModelAndView("redirect:/login");
        }

        List<BorrowRecordDto> borrows = borrowService.getMyBorrows(userId);
        long activeCount = borrowService.getActiveBorrowCount(userId);
        long overdueCount = borrowService.getOverdueCount(userId);

        ModelAndView mav = new ModelAndView("my-borrows");
        mav.addObject("borrows", borrows);
        mav.addObject("activeCount", activeCount);
        mav.addObject("overdueCount", overdueCount);
        return mav;
    }

    @PostMapping("/books/{bookId}/borrow")
    public String borrowBook(@PathVariable UUID bookId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        UUID userId = currentUser.getUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            BorrowRecordDto borrow = borrowService.borrowBook(userId, bookId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "📚 '%s' borrowed! Due %s."
                            .formatted(borrow.getBookTitle(),
                                    borrow.getDueDate().toLocalDate()));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/books";
    }

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
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/my-borrows";
    }
}