import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/** Test case for issue 53: https://github.com/typetools/checker-framework/issues/53 */
public class EnsuresNonNullIfTest2 {

  private @Nullable Long id;

  public @org.checkerframework.dataflow.qual.Pure @Nullable Long getId() {
    return id;
  }

  @EnsuresNonNullIf(result = true, expression = "getId()")
  public boolean hasId2() {
    return getId() != null;
  }

  @EnsuresNonNullIf(result = true, expression = "id")
  public boolean hasId11() {
    return id != null;
  }

  @EnsuresNonNullIf(result = true, expression = "id")
  public boolean hasId12() {
    return this.id != null;
  }

  @EnsuresNonNullIf(result = true, expression = "this.id")
  public boolean hasId13() {
    return id != null;
  }

  @EnsuresNonNullIf(result = true, expression = "this.id")
  public boolean hasId14() {
    return this.id != null;
  }

  void client() {
    if (hasId11()) {
      id.toString();
    }
    if (hasId12()) {
      id.toString();
    }
    if (hasId13()) {
      id.toString();
    }
    if (hasId14()) {
      id.toString();
    }
    // :: error: (dereference.of.nullable)
    id.toString();
  }

  // Expressions referring to enclosing classes should be resolved.
  class Inner {
    @EnsuresNonNullIf(result = true, expression = "getId()")
    public boolean innerHasGetIdMethod() {
      return getId() != null;
    }

    @EnsuresNonNullIf(result = true, expression = "id")
    public boolean innerHasIdField() {
      return id != null;
    }
  }
}
