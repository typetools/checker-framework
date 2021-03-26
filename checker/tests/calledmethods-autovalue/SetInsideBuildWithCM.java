import com.google.auto.value.AutoValue;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class SetInsideBuildWithCM {
  public abstract String name();

  public abstract int size();

  static Builder builder() {
    return new AutoValue_SetInsideBuildWithCM.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder setName(String name);

    abstract Builder setSize(int value);

    abstract SetInsideBuildWithCM autoBuild();

    public SetInsideBuildWithCM build() {
      return this.autoBuild();
    }
  }

  public static void buildSomethingCorrect() {
    Builder b = builder();
    b.setName("Frank");
    b.setSize(2);
    b.build();
  }

  public static void buildSomethingWrong() {
    Builder b = builder();
    // :: error: finalizer.invocation.invalid
    b.build();
  }
}
