package app.model.dto;

import app.model.entity.BorrowRecord;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowRecordDto {

    private UUID id;
    private UUID userId;
    private String username;
    private UUID bookId;
    private String bookTitle;
    private String bookCoverUrl;
    private String authorName;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private String status;          // BORROWED, RETURNED, LATE
    private boolean overdue;
    private long daysUntilDue;      // negative if overdue

    public static BorrowRecordDto from(BorrowRecord record) {
        boolean overdue = record.isOverdue();
        long daysUntilDue = ChronoUnit.DAYS.between(
                LocalDate.now(),
                record.getDueDate().toLocalDate()
        );

        return BorrowRecordDto.builder()
                .id(record.getId())
                .userId(record.getUser().getId())
                .username(record.getUser().getUsername())
                .bookId(record.getBook().getId())
                .bookTitle(record.getBook().getTitle())
                .bookCoverUrl(record.getBook().getCoverImageUrl())
                .authorName(record.getBook().getAuthor() != null
                        ? record.getBook().getAuthor().getName()
                        : null)
                .borrowDate(record.getBorrowDate())
                .dueDate(record.getDueDate())
                .returnDate(record.getReturnDate())
                .status(overdue ? "LATE" : record.getStatus().name())
                .overdue(overdue)
                .daysUntilDue(daysUntilDue)
                .build();
    }
}