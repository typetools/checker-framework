// @test
// @summary Reproduce incorrect interaction between @NotOnlyInitialized and @UnderInitialization
// @compile
import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class Parent {
  @NonNull String parentField;

  Parent() {
    parentField = "parent";
  }
}
