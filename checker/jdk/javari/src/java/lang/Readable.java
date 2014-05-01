package java.lang;
import org.checkerframework.checker.javari.qual.*;

import java.io.IOException;

public interface Readable {

    public int read(java.nio.CharBuffer cb) throws IOException;
}
