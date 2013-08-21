package java.lang;

import checkers.nullness.quals.*;

public class Error extends Throwable {
    static final long serialVersionUID = 4980196508277280342L;

    public Error() {
	super();
    }

    public Error(@Nullable String message) {
	super(message);
    }

    public Error(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public Error(@Nullable Throwable cause) {
        super(cause);
    }
}
