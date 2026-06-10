package app.repository;

import app.model.entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FineRepository extends JpaRepository<Fine, UUID> {

    Optional<Fine> findByBorrowRecordId(UUID borrowRecordId);
}