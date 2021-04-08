import java.util.Date;
import java.util.Formatter;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.checker.formatter.util.FormatUtil;
import org.junit.Assert;

public class FlowFormatter {
  public static String callUnqual(String u) {
    return u;
  }

  public static void main(String... p) {
    Formatter f = new Formatter();

    String unqual = System.lineSeparator();
    String qual = "%s %d %f";
    String wrong = "%$s";
    callUnqual("%s");
    callUnqual(qual);
    callUnqual(wrong);
    callUnqual(null);
    // :: error: (format.string.invalid)
    f.format(null);
    @Format({ConversionCategory.GENERAL}) String nullAssign = null;
    // :: error: (format.string.invalid)
    f.format(nullAssign, "string");
    if (false) {
      nullAssign = "%s";
    }
    f.format(nullAssign, "string");
    // :: error: (assignment.type.incompatible)
    @Format({ConversionCategory.GENERAL}) String err0 = unqual;
    // :: error: (assignment.type.incompatible)
    @Format({ConversionCategory.GENERAL}) String err2 = "%$s";
    @Format({ConversionCategory.GENERAL}) String ok = "%s";

    String u = "%s" + " %" + "d";
    String v = FormatUtil.asFormat(u, ConversionCategory.GENERAL, ConversionCategory.INT);
    f.format(u, "String", 1337);
    // :: error: (argument.type.incompatible)
    f.format(u, "String", 7.4);

    try {
      String l = FormatUtil.asFormat(u, ConversionCategory.FLOAT, ConversionCategory.INT);
      Assert.fail("Expected Exception");
    } catch (Error e) {
    }

    String a = "Success: %s %d %f";
    f.format(a, "String", 1337, 7.5);

    String b = "Fail: %d";
    // :: error: (argument.type.incompatible)
    f.format(b, "Wrong");

    @Format({
      ConversionCategory.GENERAL,
      ConversionCategory.INT,
      ConversionCategory.FLOAT,
      ConversionCategory.CHAR
    })
    String s = "Success: %s %d %f %c";
    f.format(s, "OK", 42, 3.14, 'c');

    @Format({ConversionCategory.GENERAL, ConversionCategory.INT, ConversionCategory.FLOAT}) String t = "Fail: %s %d %f";
    // :: error: (argument.type.incompatible)
    f.format(t, "OK", "Wrong", 3.14);

    call(f, "Success: %tM");
    // :: error: (argument.type.incompatible)
    call(f, "Fail: %d");

    System.out.println(f.toString());
    f.close();
  }

  public static void call(Formatter f, @Format({ConversionCategory.TIME}) String s) {
    f.format(s, new Date());
    // :: error: (argument.type.incompatible)
    f.format(s, "Wrong");
  }
}
