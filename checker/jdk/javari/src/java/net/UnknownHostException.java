package java.net;
import org.checkerframework.checker.javari.qual.*;

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
