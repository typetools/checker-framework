import java.util.Date;
import java.util.Formatter;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;

public class FormatIndexing {
  public static void main(String... p) {
    Formatter f = new Formatter();

    // GENERAL and self intersections
    @Format({ConversionCategory.CHAR}) String ccc = "%1$c %<c %c";
    @Format({ConversionCategory.CHAR}) String c = "%c %<s";
    @Format({ConversionCategory.INT}) String i = "%d %<s";
    @Format({ConversionCategory.FLOAT}) String d = "%f %<s";
    @Format({ConversionCategory.TIME}) String t = "%TT %<s";

    // test CHAR_AND_INT
    @Format({ConversionCategory.CHAR_AND_INT}) String ici = "%1$d %1$c";
    f.format("%1$c %1$d", (int) 42);
    f.format("%1$c %1$d %1$d", (int) 42);
    // :: error: (argument.type.incompatible)
    f.format("%1$c %1$d", (long) 42);
    // :: error: (argument.type.incompatible)
    f.format("%1$c %1$d", 'c');

    // test INT_AND_TIME
    @Format({ConversionCategory.INT_AND_TIME}) String iit = "%1$tT %1$d";
    f.format("%1$tT %1$d", (long) 42);
    f.format("%1$d %1$tT %1$d", (long) 42);
    // :: error: (argument.type.incompatible)
    f.format("%1$tT %1$d", (int) 42);
    // :: error: (argument.type.incompatible)
    f.format("%1$tT %1$d", new Date());

    // test NULL
    @Format({ConversionCategory.NULL}) String cf = "%c %<f";
    @Format({ConversionCategory.NULL}) String ct = "%c %<TT";
    @Format({ConversionCategory.NULL}) String _if = "%d %<f";
    @Format({ConversionCategory.NULL}) String fc = "%f %<c";
    @Format({ConversionCategory.NULL}) String fi = "%f %<d";
    @Format({ConversionCategory.NULL}) String ft = "%f %<TT";
    @Format({ConversionCategory.NULL}) String tc = "%TT %<c";
    @Format({ConversionCategory.NULL, ConversionCategory.INT}) String tf = "%TT %<f %2$d";
    @Format({ConversionCategory.NULL}) String icf = "%d %<c %<f";
    @Format({ConversionCategory.NULL}) String itc = "%d %<TT %<c";
    // :: warning: (format.specifier.null)
    f.format(tf, null, 0);
    // :: warning: (format.specifier.null)
    f.format(tf, (Object[]) null);
    // :: error: (format.specifier.null)
    f.format(tf, 'c', 0);
    // :: warning: (format.specifier.null)
    f.format(tf, (Object) null, 0);

    // test UNUSED
    // :: warning: (format.argument.unused)
    f.format("%1$s %3$s", "Hello", "Missing", "World");
    // :: warning: (format.argument.unused) :: warning: (format.indirect.arguments)
    f.format("%1$s %3$s", new Object[5]);
    // :: warning: (format.argument.unused)
    f.format("%5$s", (Object[]) null);
    // :: warning: (format.argument.unused)
    f.format("%3$s", null, null, null);
    // :: warning: (format.argument.unused)
    f.format("%7$s %2$s %4$s %<s %7$s", 0, 0, 0, 0, 0, 0, 0);
    f.close();

    // test UNUSED and NULL
    // :: warning: (format.argument.unused) :: error: (format.specifier.null)
    f.format("%1$s %3$d %3$f", "Hello", "Missing", "World");
  }
}
