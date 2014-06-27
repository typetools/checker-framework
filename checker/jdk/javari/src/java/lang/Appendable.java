package java.lang;
import org.checkerframework.checker.javari.qual.*;

import java.io.IOException;

public interface Appendable {

    Appendable append(@ReadOnly CharSequence csq) throws IOException;
    Appendable append(@ReadOnly CharSequence csq, int start, int end) throws IOException;
    Appendable append(char c) throws IOException;
}
