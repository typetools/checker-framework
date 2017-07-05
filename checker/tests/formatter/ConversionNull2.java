import java.util.Formatter;
import org.checkerframework.checker.formatter.qual.FormatMethod;

public class ConversionNull2 {
    static void main(String... p) {
        Formatter f1 = new Formatter();
        MyFormatter f2 = new MyFormatter();

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
