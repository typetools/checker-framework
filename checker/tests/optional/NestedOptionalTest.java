import java.util.Collections;
import java.util.Optional;

class NestedOptional {

  Object field;

  @SuppressWarnings("optional.parameter")
  // :: warning: (optional.nesting)
  Optional<String> bar(Optional<Optional<String>> optOptStr) {
    if (optOptStr.isPresent()) {
      return optOptStr.get();
    }
    return Optional.empty();
  }

  void foo() {
    // Explicitly providing a type annotation triggers the error
    // :: warning: (optional.nesting)
    var x = Optional.of(Optional.of("foo")); // I expect an error here.

    // :: warning: (optional.nesting)
    bar(Optional.of(Optional.of("bar")));

    // :: warning: (optional.nesting)
    field = Optional.of(Optional.of("baz"));

    // :: warning: (optional.collection)
    field = Optional.of(Collections.singleton("baz"));
  }
}
