import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.formatter.qual.FormatMethod;

public class FormatMethodAndFormat {

    // This method contains both @FormatMethod and @Format.  Both constrain the arguments.
    @FormatMethod
    void log(@Format(ConversionCategory.INT) String format, Object... args) {
        System.out.printf(format, args);
    }

    void client() {
        // Without the @Format annotation (with just the @FormatMethod anno), this would be legal.
        // :: error: (argument.type.incompatible)
        log("%s %s", "hello", "goodbye");
    }
}
