import java.text.MessageFormat;
import java.util.Date;

public class I18nFormat {

  void test() {

    MessageFormat.format(
        "{0} {1, number} {2, time} {3, date} {4, choice, 0#zero}",
        "S", 1, new Date(), new Date(), 0);
    MessageFormat.format("{0, number}{1, number}", 1, 2);
    MessageFormat.format("{0, number}{0}", 1);

    // :: warning: (i18nformat.excess.arguments)
    MessageFormat.format("'{0, number}'", new Date(12));

    // :: warning: (i18nformat.missing.arguments)
    MessageFormat.format("''{0, time, short}''{1}{2, time} {33, number}{44444}'{''''", 0);

    // :: warning: (i18nformat.missing.arguments)
    MessageFormat.format("{0, number}{1, number}", 1);

    // :: warning: (i18nformat.argument.unused)
    MessageFormat.format("{1, number}", 1, 1);

    // :: warning: (i18nformat.excess.arguments)
    MessageFormat.format("{0, number}", 1, new Date());

    // :: warning: (i18nformat.indirect.arguments)
    MessageFormat.format("{0, number}", new Object[2]);

    MessageFormat.format("{0}", "S");
    MessageFormat.format("{0}", 1);
    MessageFormat.format("{0}", new Date());

    // :: error: (argument.type.incompatible)
    MessageFormat.format("{0, number}", "S");
    MessageFormat.format("{0, number}", 1);
    // :: error: (argument.type.incompatible)
    MessageFormat.format("{0, number}", new Date());

    // :: error: (argument.type.incompatible)
    MessageFormat.format("{0, time}", "S");
    MessageFormat.format("{0, time}", 1);
    MessageFormat.format("{0, time}", new Date());

    // :: error: (argument.type.incompatible)
    MessageFormat.format("{0, date}", "S");
    MessageFormat.format("{0, date}", 1);
    MessageFormat.format("{0, date}", new Date());
  }
}
