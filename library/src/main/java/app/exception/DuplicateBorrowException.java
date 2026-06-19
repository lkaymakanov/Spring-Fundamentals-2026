package app.exception;

public class DuplicateBorrowException extends RuntimeException {
    public DuplicateBorrowException(String message) {
        super(message);
    }
}