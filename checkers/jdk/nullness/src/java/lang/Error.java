package java.lang;

import checkers.nullness.quals.Nullable;
import dataflow.quals.SideEffectFree;

public class Error extends Throwable {
    static final long serialVersionUID = 4980196508277280342L;

    @SideEffectFree public Error() {
	super();
    }

    @SideEffectFree public Error(@Nullable String message) {
	super(message);
    }

    @SideEffectFree public Error(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    @SideEffectFree public Error(@Nullable Throwable cause) {
        super(cause);
    }
}
