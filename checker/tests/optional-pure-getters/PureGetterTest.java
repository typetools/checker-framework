import java.util.Optional;

class PureGetterTest {

  @SuppressWarnings("optional.field")
  Optional<String> field;

  Optional<String> getOptional() {
    return null;
  }

  Optional<String> otherOptional() {
    return null;
  }

  void sideEffect() {}

  void foo() {
    if (field.isPresent()) {
      field.get();
    }
    if (field.isPresent()) {
      sideEffect();
      // :: error: method.invocation
      field.get();
    }
    if (field.isPresent()) {
      getOptional();
      field.get();
    }
    if (field.isPresent()) {
      otherOptional();
      // :: error: method.invocation
      field.get();
    }

    if (getOptional().isPresent()) {
      getOptional().get();
    }
    if (getOptional().isPresent()) {
      sideEffect();
      getOptional().get();
    }
    if (getOptional().isPresent()) {
      getOptional();
      getOptional().get();
    }
    if (getOptional().isPresent()) {
      otherOptional();
      getOptional().get();
    }

    if (otherOptional().isPresent()) {
      // :: error: method.invocation
      otherOptional().get();
    }
    if (otherOptional().isPresent()) {
      sideEffect();
      // :: error: method.invocation
      otherOptional().get();
    }
    if (otherOptional().isPresent()) {
      getOptional();
      // :: error: method.invocation
      otherOptional().get();
    }
    if (otherOptional().isPresent()) {
      otherOptional();
      // :: error: method.invocation
      otherOptional().get();
    }
  }

  void bar(Optional<String> os) {
    os.get();
  }
}
