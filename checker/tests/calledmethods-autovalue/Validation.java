import com.google.auto.value.AutoValue;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class Validation {

  public abstract String name();

  static Builder builder() {
    return new AutoValue_Validation.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setName(String name);

    abstract Validation autoBuild();

    public Validation build(@CalledMethods("setName") Builder this) {
      Validation v = autoBuild();
      if (v.name().length() < 5) {
        throw new RuntimeException("name too short!");
      }
      return v;
    }
  }

  static void correct() {
    Builder b = builder();
    b.setName("Phil");
    b.build();
  }

  static void wrong() {
    Builder b = builder();
    // :: error: finalizer.invocation
    b.build();
  }
}
