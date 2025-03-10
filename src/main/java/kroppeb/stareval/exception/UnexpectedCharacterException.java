package kroppeb.stareval.exception;

public class UnexpectedCharacterException extends ParseException {

	private static final long serialVersionUID = -3886198952263126846L;

    public UnexpectedCharacterException(char expected, char actual, int index) {
		this("Expected to read '" + expected + "' but got '" + actual + "' at index " + index);
	}

	public UnexpectedCharacterException(char actual, int index) {
		this("Read an unexpected character '" + actual + "' at index " + index);
	}

	public UnexpectedCharacterException(String expected, char actual, int index) {
		this("Expected to read " + expected + " but got '" + actual + "' at index " + index);
	}

	private UnexpectedCharacterException(String message) {
		super(message);
	}
}
