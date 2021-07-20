import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.Format;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

public class ConversionBasic {
    public static void main(String... p) {
        Formatter f = new Formatter();

        // test GENERAL, there is nothing we can do wrong
        @Format({ConversionCategory.GENERAL}) String s = "%s";
        f.format("Suc-%s-ful", "cess");
        f.format("%b", 4);
        f.format("%B", 7.5);
        f.format("%h", new Date());
        f.format("%H", Integer.valueOf(4));
        f.format("%s", new ArrayList<Integer>());

        // test CHAR
        @Format({ConversionCategory.CHAR}) String c = "%c";
        f.format("%c", 'c');
        f.format("%c", (byte) 67);
        f.format("%c", (int) 67);
        f.format("%c", Character.valueOf('c'));
        f.format("%c", Byte.valueOf((byte) 67));
        f.format("%c", Short.valueOf((short) 67));
        f.format("%C", Integer.valueOf(67));
        // :: error: (argument.type.incompatible)
        f.format("%c", 7.5);
        // :: error: (argument.type.incompatible)
        f.format("%C", "Value");

        // test INT
        @Format({ConversionCategory.INT}) String i = "%d";
        f.format("%d", (byte) 67);
        f.format("%o", (short) 67);
        f.format("%x", (int) 67);
        f.format("%X", (long) 67);
        f.format("%d", Long.valueOf(67));
        f.format("%d", BigInteger.ONE);
        // :: error: (argument.type.incompatible)
        f.format("%d", 'c');
        // :: error: (argument.type.incompatible)
        f.format("%d", BigDecimal.ONE);

        // test FLOAT
        @Format({ConversionCategory.FLOAT}) String d = "%f";
        f.format("%e", (float) 67.1);
        f.format("%E", (double) 67.3);
        f.format("%f", Float.valueOf(42.5f));
        f.format("%g", Double.valueOf(42.5));
        f.format("%G", 67.87);
        f.format("%a", BigDecimal.ONE);
        // :: error: (argument.type.incompatible)
        f.format("%A", 1325);
        // :: error: (argument.type.incompatible)
        f.format("%a", BigInteger.ONE);

        // test TIME
        @Format({ConversionCategory.TIME}) String t = "%tT";
        f.format("%tD", new Date());
        f.format("%TM", (long) 32165456);
        f.format("%TD", Calendar.getInstance());
        // :: error: (argument.type.incompatible)
        f.format("%tD", 1321543512);
        // :: error: (argument.type.incompatible)
        f.format("%tD", new Object());

        System.out.println(f.toString());
        f.close();
    }
}
