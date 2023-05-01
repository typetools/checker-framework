// This test ensures that @EnsuresCalledMethods annotations are not inferred by the normal
// WPI postcondition annotation inference algorithm (i.e., that it is disabled). With the
// usual WPI postcondition annotation inference algorithm, this test case would produce a
// spurious (but technically correct) error.

public class UnwantedECMInference {

  class Bar {
    Object field;

    void doStuff() {
      field.toString();
    }
  }

  class Baz extends Bar {
    void doStuff() {
      // This method does not call toString(), so an @EnsuresCalledMethods("toString") annotation on
      // either this method or on the overridden method is an error!
      field.hashCode();
    }
  }
}
