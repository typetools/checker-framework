import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;

public class FormatBasic {
  public static void main(String... p) {
    Formatter f = new Formatter();

    f.format("String");
    f.format("String %20% %n");
    f.format("%% %s", "str");
    f.format("%4$2s %3$2s %2$2s %1$2s", "a", "b", "c", "d");
    f.format("e = %+10.4f", Math.E);
    f.format("Amount gained or lost since last statement: $ %(,.2f", -6217.58);
    f.format("Local time: %tT", Calendar.getInstance());
    f.format("Unable to open file '%1$s': %2$s", "food", "No such file or directory");
    f.format("Duke's Birthday: %1$tm %1$te,%1$tY", new GregorianCalendar(1995, Calendar.MAY, 23));
    f.format("Duke's Birthday: %tm %<te,%<tY", new Date());
    f.format("Duke's Birthday: %2$tm %<te,%<tY (it's the %dth)", 123, new Date());

    String s = "%+s%";
    // :: error: (format.string)
    f.format(s, "illegal");
    // :: error: (format.string)
    f.format("%+s%", "illegal");
    // :: error: (format.string)
    f.format("Wrong < indexing: %1$tm %<te,%<$tY", new Date());
    // :: error: (format.string)
    f.format("%t", new Date());
    // :: error: (argument)
    f.format("%Td", (int) 231);

    f.close();
  }
}
