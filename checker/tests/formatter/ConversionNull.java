import java.util.Date;
import java.util.Formatter;

public class ConversionNull {
  public static void main(String... p) {
    Formatter f = new Formatter();

    f.format("%d %s", 0, null);
    f.format("%s", (Object) null);

    f.format("%d %c", 0, null);
    f.format("%c", (Character) null);
    f.format("%c", (Object) null);

    f.format("%d %d", 0, null);
    f.format("%d", (Integer) null);
    f.format("%d", (Object) null);

    f.format("%d %f", 0, null);
    f.format("%f", (Float) null);
    f.format("%f", (Object) null);

    f.format("%d %tD", 0, null);
    f.format("%tD", (Date) null);
    f.format("%tD", (Object) null);

    System.out.println(f.toString());
    f.close();
  }
}
