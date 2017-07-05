import java.util.Formatter;
import org.checkerframework.checker.formatter.qual.FormatMethod;

public class ConversionNull2 {
    void foo(Formatter f1, MyFormatter f2) {
        //:: error: (argument.type.incompatible)
        f1.format("%d %c", 0, null);
        //:: error: (argument.type.incompatible)
        f2.format("%d %c", 0, null);
    }
}

class MyFormatter {
    @FormatMethod
    public MyFormatter format(String format, Object... args) {
        return null;
    }
}
