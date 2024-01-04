import java.util.Objects;
import java.util.Optional;

/**
 * Test case for rule #4: "It's generally a bad idea to create an Optional for the specific purpose
 * of chaining from it to get a value."
 */
public class Marks4 {

  String getDefault() {
    return "Fozzy Bear";
  }

  String process_bad(String s) {
    // :: warning: (introduce.eliminate)
    return Optional.ofNullable(s).orElseGet(this::getDefault);
  }

  String process_bad2(String s) {
    // :: warning: (introduce.eliminate)
    return Optional.<String>empty().orElseGet(this::getDefault);
  }

  String process_bad3(String s) {
    // :: warning: (introduce.eliminate)
    return Optional.of(s).orElseGet(this::getDefault);
  }

  String process_good(String s) {
    return (s != null) ? s : getDefault();
  }

  String m1(String s) {
    // :: warning: (introduce.eliminate)
    return Optional.ofNullable(s).orElseGet(this::getDefault) + "hello";
  }

  boolean m2(String s) {
    // :: warning: (introduce.eliminate)
    return Objects.equals("hello", Optional.ofNullable(s).orElseGet(this::getDefault));
  }

  boolean m3(String s) {
    // :: warning: (introduce.eliminate)
    return "hello" == Optional.ofNullable(s).orElseGet(this::getDefault);
  }

  String m4(String s) {
    // :: warning: (introduce.eliminate)
    return Optional.ofNullable(s).map(Object::toString).orElseGet(this::getDefault);
  }

  String m5(String s) {
    return Optional.ofNullable(s)
        .map(Object::toString)
        .map(Object::toString)
        // :: warning: (introduce.eliminate)
        .orElseGet(this::getDefault);
  }
}
