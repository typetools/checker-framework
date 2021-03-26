import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public class AssertIfNonNullTest {

  Long id;

  public AssertIfNonNullTest(Long id) {
    this.id = id;
  }

  @AssertNonNullIfNonNull("id")
  @Pure
  public @Nullable Long getId() {
    return id;
  }
}
