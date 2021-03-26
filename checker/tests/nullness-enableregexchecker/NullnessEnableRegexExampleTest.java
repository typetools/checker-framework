import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;

class NullnessEnableRegexExampleTest {
  private static final Pattern PATTERN = Pattern.compile("^.(.*).$");

  String doSomethingOdd(String input) {
    Matcher matcher = PATTERN.matcher(input);
    if (!matcher.matches()) {
      return "";
    }

    // The regex checker concludes that this call is legal, therefore its return type
    // can be @NonNull.
    @NonNull String a = matcher.group(1);

    // This call is not legal because the number of groups in the pattern is 1, therefore
    // its return type cannot be @NonNull.
    // :: error: (assignment.type.incompatible) :: error: (group.count.invalid)
    @NonNull String b = matcher.group(2);
    return a + b;
  }
}
