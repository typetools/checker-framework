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

  String process_good(String s) {
    return (s != null) ? s : getDefault();
  }
}
