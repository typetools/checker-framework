// Test case for Issue 1507:
// https://github.com/typetools/checker-framework/issues/1507

import java.util.Locale;
import org.checkerframework.checker.formatter.qual.FormatMethod;

public class FormatMethodAnnotation {

    public void example() {
        String ex1 = String.format(Locale.ENGLISH, "%s %d", "cost", 12);
        log("%d", 0);
        log2(Locale.ENGLISH, "%d", 0);
    }

    @FormatMethod
    static void log(String format, Object... args) {
        String ex1 = String.format(format, args);
        String ex2 = String.format(Locale.ENGLISH, format, args);
    }

    @FormatMethod
    static void log2(Locale locale, String format, Object... args) {
        String ex1 = String.format(format, args);
        String ex2 = String.format(locale, format, args);
        String ex3 = String.format(Locale.ENGLISH, format, args);
    }
}
