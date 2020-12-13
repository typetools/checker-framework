import static org.checkerframework.checker.formatter.qual.ConversionCategory.NULL;

import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;

public class FormatNullCategory {

    static @Format(NULL) String n1 = "%1$f %1$c %n";

    static @Format(ConversionCategory.NULL) String n2 = "%1$f %1$c %n";

    public static void main(String[] args) {

        System.out.printf("%d %n", (Object) null);

        // Warning if call is legal, error if it is not.
        // :: warning: (format.specifier.null)
        System.out.printf(n1, (Object) null);
        // :: error: (format.specifier.null)
        System.out.printf(n1, 3);
        // :: warning: (format.specifier.null)
        System.out.printf(n2, (Object) null);
        // :: error: (format.specifier.null)
        System.out.printf(n2, 3);
    }
}
