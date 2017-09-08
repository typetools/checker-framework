import java.util.Locale;
import org.checkerframework.checker.formatter.qual.FormatMethod;

public class FormatMethodAnnotation {

    public void example() {
        String ex1 = String.format(Locale.ENGLISH, "%s %d", "cost", 12);
        log("%d", 0);
    }

    @FormatMethod
    static void log(String format, Object... args) {
        String ex1 = String.format(Locale.ENGLISH, format, args);
    }
}
