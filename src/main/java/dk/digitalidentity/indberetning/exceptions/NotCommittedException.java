package dk.digitalidentity.indberetning.exceptions;

public class NotCommittedException extends RuntimeException {
    public NotCommittedException(String message) {
        super(message);
    }
}
