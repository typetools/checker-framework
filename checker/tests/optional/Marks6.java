import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Test cases for Rule #6: "Avoid using Optional in fields, method parameters, and collections." */
public class Marks6 {

  // :: warning: (optional.field)
  Optional<String> optionalField = Optional.ofNullable(null);

  // :: warning: (optional.parameter)
  void optionalParameter(Optional<String> arg) {}

  Optional<String> okUses() {
    Optional<String> os = Optional.of("hello world");
    return os;
  }

  void illegalInstantiations() {
    // :: warning: (optional.as.element.type)
    List<Optional<String>> los = new ArrayList<>();
    // :: warning: (optional.as.element.type)
    List<Optional<String>> los2 = new ArrayList<Optional<String>>();
    // :: warning: (optional.as.element.type)
    Set<Optional<String>> sos = new HashSet<>();
  }
}
