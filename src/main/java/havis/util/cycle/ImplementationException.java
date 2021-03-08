package havis.util.cycle;

public class ImplementationException extends Exception {

	private static final long serialVersionUID = 1L;

	public ImplementationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ImplementationException(String message) {
		super(message);
	}

	public ImplementationException(Throwable cause) {
		super(cause);
	}
}
