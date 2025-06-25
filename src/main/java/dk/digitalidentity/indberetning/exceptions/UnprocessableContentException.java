package dk.digitalidentity.indberetning.exceptions;

public class UnprocessableContentException extends Exception {
	public UnprocessableContentException(String errMsg) {
		super(errMsg);
	}

	public UnprocessableContentException(String message, Throwable cause) {
		super(message, cause);
	}
}
