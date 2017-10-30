import java.util.Date;
import java.util.Formatter;

public class ConversionNull {
    public static void main(String... p) {
        Formatter f = new Formatter();

        f.format("%d %s", 0, null);
        f.format("%s", (Object) null);

        // :: error: (argument.type.incompatible)
        f.format("%d %c", 0, null);
        f.format("%c", (Character) null);
        // :: error: (argument.type.incompatible)
        f.format("%c", (Object) null);

        // :: error: (argument.type.incompatible)
        f.format("%d %d", 0, null);
        f.format("%d", (Integer) null);
        // :: error: (argument.type.incompatible)
        f.format("%d", (Object) null);

        // :: error: (argument.type.incompatible)
        f.format("%d %f", 0, null);
        f.format("%f", (Float) null);
        // :: error: (argument.type.incompatible)
        f.format("%f", (Object) null);

        // :: error: (argument.type.incompatible)
        f.format("%d %tD", 0, null);
        f.format("%tD", (Date) null);
        // :: error: (argument.type.incompatible)
        f.format("%tD", (Object) null);

        System.out.println(f.toString());
        f.close();
    }
}
