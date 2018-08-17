import java.util.ArrayList;
import java.util.Formatter;

public class VarargsFormatter {
    public static void main(String... p) {
        Formatter f = new Formatter();

        // vararg as parameter
        // :: warning: non-varargs call of varargs method with inexact argument type for last
        // parameter; :: warning: (format.indirect.arguments)
        f.format("Nothing", null); // equivalent to (Object[])null
        // :: warning: (format.indirect.arguments)
        f.format("Nothing", (Object[]) null);
        // :: warning: (format.indirect.arguments)
        f.format("%s", (Object[]) null);
        // :: warning: (format.indirect.arguments)
        f.format("%s %d %x", (Object[]) null);
        // :: warning: non-varargs call of varargs method with inexact argument type for last
        // parameter; :: warning: (format.indirect.arguments)
        f.format("%s %d %x", null); // equivalent to (Object[])null
        // :: warning: (format.indirect.arguments)
        f.format("%d", new Object[1]);
        // :: warning: (format.indirect.arguments)
        f.format("%s", new Object[2]);
        // :: warning: (format.indirect.arguments)
        f.format("%s %s", new Object[0]);
        // :: warning: (format.indirect.arguments)
        f.format("Empty", new Object[0]);
        // :: warning: (format.indirect.arguments)
        f.format("Empty", new Object[5]);
        f.format("%s", new ArrayList<Object>());
        f.format("%d %s", 132, new Object[2]);
        f.format("%s %d", new Object[2], 123);
        // :: error: (format.missing.arguments)
        f.format("%d %s %s", 132, new Object[2]);
        // :: error: (argument.type.incompatible)
        f.format("%d %d", new Object[2], 123);
        // :: error: (format.specifier.null) :: warning: (format.indirect.arguments)
        f.format("%d %<f", new Object[1]);

        // too many arguments
        // :: warning: (format.excess.arguments)
        f.format("", 213);
        // :: warning: (format.excess.arguments)
        f.format("%d", 232, 132);
        // :: warning: (format.excess.arguments)
        f.format("%s", "a", "b");
        // :: warning: (format.excess.arguments)
        f.format("%d %s", 123, "a", 123);

        // too few arguments
        // :: error: (format.missing.arguments)
        f.format("%s");
        // :: error: (format.missing.arguments)
        f.format("%d %s", 545);
        // :: error: (format.missing.arguments)
        f.format("%s %c %c", 'c', 'c');

        f.close();
    }
}
