public class UnwantedECMInference {

  class Bar {
    Object field;

    void doStuff() {
      field.toString();
    }
  }

  class Baz extends Bar {
    @Override
    void doStuff() {
      // This method does not call toString(), so an @EnsuresCalledMethods("toString") annotation on
      // either this method or on the overridden method is an error!
      field.hashCode();
    }
  }
}
