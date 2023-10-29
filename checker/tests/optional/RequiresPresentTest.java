import java.util.Optional;
import org.checkerframework.checker.optional.qual.*;

public class RequiresPresentTest {

  // :: warning: (optional.field)
  Optional<String> field1 = Optional.of("abc");
  // :: warning: (optional.field)
  Optional<String> field2 = Optional.empty();

  @RequiresPresent("field1")
  void method1() {
    field1.get().length(); // OK, field1 is known to be present (non-empty)
    this.field1.get().length(); // OK, field1 is known to be present (non-emmpty)
    // :: error: (method.invocation)
    field2.get().length(); // error, might throw NoSuchElementException
  }

  @RequiresPresent("field1")
  void method2() {
    // OK, an indirect call to method1.
    method1();
  }

  void method3() {
    field1 = Optional.of("abc");
    method1(); // OK, satisfied method precondition.
    field1 = Optional.empty();
    // :: error: (contracts.precondition)
    method1(); // error, does not satisfy method precondition.
  }

  // :: warning: (optional.field)
  protected Optional<String> field;

  @RequiresPresent("field")
  public void requiresPresentField() {}

  public void clientFail(RequiresPresentTest arg1) {
    // :: error: (contracts.precondition)
    arg1.requiresPresentField();
  }

  public void clientOK(RequiresPresentTest arg2) {
    arg2.field = Optional.of("def");

    // this is legal.
    @Present Optional<String> optField = arg2.field;

    // OK, field is known to be present.
    arg2.requiresPresentField();
  }
}
