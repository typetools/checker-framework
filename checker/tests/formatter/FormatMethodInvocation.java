import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.Locale;

public class FormatMethodInvocation {
  public static void main() {
    Formatter f = new Formatter();
    f.format("%d", 1337);
    f.format(Locale.GERMAN, "%d", 1337);
    // :: error: (argument)
    f.format("%f", 1337);
    // :: error: (argument)
    f.format(Locale.GERMAN, "%f", 1337);
    f.close();

    String.format("%d", 1337);
    String.format(Locale.GERMAN, "%d", 1337);
    // :: error: (argument)
    String.format("%f", 1337);
    // :: error: (argument)
    String.format(Locale.GERMAN, "%f", 1337);

    PrintWriter pw = new PrintWriter(new ByteArrayOutputStream());
    pw.format("%d", 1337);
    pw.format(Locale.GERMAN, "%d", 1337);
    pw.printf("%d", 1337);
    pw.printf(Locale.GERMAN, "%d", 1337);
    // :: error: (argument)
    pw.format("%f", 1337);
    // :: error: (argument)
    pw.format(Locale.GERMAN, "%f", 1337);
    // :: error: (argument)
    pw.printf("%f", 1337);
    // :: error: (argument)
    pw.printf(Locale.GERMAN, "%f", 1337);
    pw.close();

    PrintStream ps = System.out;
    ps.format("%d", 1337);
    ps.format(Locale.GERMAN, "%d", 1337);
    ps.printf("%d", 1337);
    ps.printf(Locale.GERMAN, "%d", 1337);
    // :: error: (argument)
    ps.format("%f", 1337);
    // :: error: (argument)
    ps.format(Locale.GERMAN, "%f", 1337);
    // :: error: (argument)
    ps.printf("%f", 1337);
    // :: error: (argument)
    ps.printf(Locale.GERMAN, "%f", 1337);
    ps.close();

    Console c = System.console();
    c.format("%d", 1337);
    c.printf("%d", 1337);
    // :: error: (argument)
    c.format("%f", 1337);
    // :: error: (argument)
    c.printf("%f", 1337);

    myFormatMethod("%d", 1337);
    // :: error: (argument)
    myFormatMethod("%f", 1337);
  }

  @com.google.errorprone.annotations.FormatMethod
  static void myFormatMethod(String fmt, Object... args) {
    System.out.printf(fmt, args);
  }
}
