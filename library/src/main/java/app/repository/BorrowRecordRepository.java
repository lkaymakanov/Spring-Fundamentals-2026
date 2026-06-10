package app.repository;

import app.model.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, UUID> {

    List<BorrowRecord> findByUserId(UUID userId);
    List<BorrowRecord> findByBookId(UUID bookId);
}