package java.net;
import checkers.javari.quals.*;

import java.io.IOException;

public class UnknownHostException extends IOException {
    private static final long serialVersionUID = -4639126076052875403L;

    public UnknownHostException(String host) {
        throw new RuntimeException("skeleton method");
    }

    public UnknownHostException() {
        throw new RuntimeException("skeleton method");
    }
}
