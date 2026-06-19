package app.exception;

public class BorrowLimitException extends RuntimeException {
    public BorrowLimitException(String message) {
        super(message);
    }
}