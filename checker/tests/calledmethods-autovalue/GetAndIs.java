import com.google.auto.value.AutoValue;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class GetAndIs {
  abstract String get();

  abstract boolean is();

  static Builder builder() {
    return new AutoValue_GetAndIs.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setGet(String value);

    abstract Builder setIs(boolean value);

    abstract GetAndIs build();
  }

  public static void buildSomethingWrong() {
    Builder b = builder();
    b.setGet("Frank");
    // :: error: finalizer.invocation.invalid
    b.build();
  }

  public static void buildSomethingRight() {
    Builder b = builder();
    b.setGet("Frank");
    b.setIs(false);
    b.build();
  }

  public static void buildSomethingWrongFluent() {
    // :: error: finalizer.invocation.invalid
    builder().setGet("Frank").build();
  }

  public static void buildSomethingRightFluent() {
    builder().setGet("Jim").setIs(true).build();
  }
}
