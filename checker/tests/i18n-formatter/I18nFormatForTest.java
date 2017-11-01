import java.text.MessageFormat;
import java.util.Date;
import org.checkerframework.checker.i18nformatter.qual.I18nFormatFor;

public class I18nFormatForTest {

    static class A {
        public void methodA(@I18nFormatFor("#2") String format, Object... args) {}
    }

    public static void main(String[] args) {

        A a1 = new A();

        // :: error: (i18nformat.string.invalid)
        a1.methodA("{0, number", new Date(12));

        // :: warning: (i18nformat.excess.arguments)
        a1.methodA("'{0{}", 1);
        a1.methodA("{0}", "A");

        // :: error: (i18nformat.string.invalid)
        a(1, 1.2, "{0, number", 1.2, new Date(12));
        a(1, 1.2, "{0, number}{1}", 1.2, 1, "A");
        // :: warning: (i18nformat.missing.arguments)
        a(1, 1.2, "{0, number}{1}", 1.2, 1);
        // :: warning: (i18nformat.excess.arguments)
        a(1, 1.2, "{0, number}{1}", 1.2, 1, "A", 2);
        b("{0, number}{1}", 1, "A");

        // :: error: (i18nformat.string.invalid)
        b("{0, number", new Date(12));
        b("{0, number}{1}", 1, "A");
        b("{0}", "a string");
        // :: error: (argument.type.incompatible)
        b("{0, number}", "a string");

        // :: error: (i18nformat.invalid.formatfor)
        c("{0, number}{1}", 1, "A");

        // :: error: (i18nformat.invalid.formatfor)
        e(1, 2);

        f("{0}", 2);

        // :: error: (i18nformat.invalid.formatfor)
        h("{0}", "a string");

        // :: error: (i18nformat.invalid.formatfor)
        i("{0}", "a string");

        j("{0}");
        // :: error: (argument.type.incompatible)
        j("{0, number}");
    }

    // Normal use
    static void b(@I18nFormatFor("#2") String f, Object... args) {
        MessageFormat.format(f, args);
    }

    // @II18nFormatFor can be annotated anywhere
    static void a(
            int dummy1,
            double dummy2,
            @I18nFormatFor("#5") String f,
            Object dummy3,
            Object... args2) {
        MessageFormat.format(f, args2);
    }

    // Invalid index
    static void c(@I18nFormatFor("#-1") String f, Object... args) {
        MessageFormat.format("{0}", "A");
    }

    // @I18nFormatFor needs to be annotated to a string.
    static void e(@I18nFormatFor("#2") int f, Object... args) {}

    // The parameter type is not necessary to an array of objects
    static void f(@I18nFormatFor("#2") String f, int args) {
        MessageFormat.format(f, args);
    }

    // Invalid formatfor argument
    static void h(@I18nFormatFor("2") String f, String args) {
        MessageFormat.format(f, args);
    }

    // We don't support this form of argument. You need to specify the parameter index.
    static void i(@I18nFormatFor("arg") String f, Object... arg) {
        MessageFormat.format(f, arg);
    }

    // This is also a valid thing to do.
    static void j(@I18nFormatFor("#1") String f) {
        MessageFormat.format(f, f);
    }
}
