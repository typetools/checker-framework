import com.google.auto.value.AutoValue;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class BuilderGetter {

  public abstract String name();

  static Builder builder() {
    return new AutoValue_BuilderGetter.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setName(String name);

    abstract String name();

    abstract BuilderGetter build();
  }

  static void correct() {
    Builder b = builder();
    b.setName("Phil");
    b.build();
  }

  static void wrong() {
    Builder b = builder();
    b.name();
    // :: error: finalizer.invocation.invalid
    b.build();
  }
}
