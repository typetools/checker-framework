import org.checkerframework.checker.nullness.qual.*;

@org.checkerframework.framework.qual.DefaultQualifier(NonNull.class)
public class DefaultFlow {

  void test() {

    @Nullable String reader = null;
    if (reader == null) {
      return;
    }

    reader.startsWith("hello");
  }

  void tesVariableInitialization() {
    @Nullable Object elts = null;
    assert elts != null : "@AssumeAssertion(nullness)";
    @NonNull Object elem = elts;
  }
}
