package regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.regex.qual.Regex;

/** Designed to test whether or not a bounds range of generics actually works. */
public class GenericsBoundsRange<@Regex(3) T extends @Regex(1) String> {
  public T t;

  public GenericsBoundsRange(T t) {
    Matcher matcher = Pattern.compile(t).matcher("some str");
    if (matcher.matches()) {
      matcher.group(0);
      matcher.group(1);

      // T has at least 1 group so the above 2 group calls are good
      // however, T MAY or MAY NOT have 2 or 3 groups, so issue an error

      // :: error: (group.count)
      matcher.group(2);

      // :: error: (group.count)
      matcher.group(3);

      // T definitely does not have 4 groups, issue an error

      // :: error: (group.count)
      matcher.group(4);
    }
  }

  // Bounds used to not actually be bounds but instead exactly the lower bound
  // so line below would fail because the argument could only be Regex(0).  So this
  // tests BaseTypeValidator.checkTypeArguments range checking.
  public void method(GenericsBoundsRange<@Regex(2) String> gbr) {}
}
