package dk.digitalidentity.indberetning.exceptions;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressLookupRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 2473172765262663181L;

	private String message;
	private HttpStatusCode statusCode;
	private HttpHeaders headers;
	
	public AddressLookupRuntimeException(String message, HttpStatusCode statusCode, HttpHeaders headers) {
		super(message);
		this.message = message;
		this.statusCode = statusCode;
		this.headers = headers;
	}
}
