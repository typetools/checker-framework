import java.util.Formatter;
import org.checkerframework.checker.formatter.qual.FormatMethod;

public class ConversionNull2 {
    void foo(Formatter f1, MyFormatter f2) {
        f1.format("%d %c", 0, null);
        f2.format("%d %c", 0, null);
    }
}

class MyFormatter {
    @FormatMethod
    public MyFormatter format(String format, Object... args) {
        return null;
    }
}
