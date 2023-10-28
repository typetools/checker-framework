import java.util.Optional;
import org.checkerframework.checker.optional.qual.*;

public class RequiresPresentTest {

  // :: warning: (optional.field)
  Optional<String> field1;
  // :: warning: (optional.field)
  Optional<String> field2;

  @RequiresPresent("field1")
  void method1() {
    field1.get().length(); // OK, field1 is known to be present (non-empty)
    this.field1.get().length(); // OK, field1 is known to be present (non-emmpty)
    // :: error: (method.invocation)
    field2.get().length(); // error, might throw NoSuchElementException
  }

  // TODO: write additional test cases.
}
