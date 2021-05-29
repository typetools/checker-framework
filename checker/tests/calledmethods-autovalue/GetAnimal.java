import com.google.auto.value.AutoValue;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Adapted from the standard AutoValue example code:
 * https://github.com/google/auto/blob/master/value/userguide/builders.md
 */
@AutoValue
abstract class GetAnimal {
  abstract String getName();

  abstract @Nullable String getHabitat();

  abstract int getNumberOfLegs();

  abstract boolean isHasArms();

  static Builder builder() {
    return new AutoValue_GetAnimal.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setName(String value);

    abstract Builder setNumberOfLegs(int value);

    abstract Builder setHabitat(String value);

    abstract Builder setHasArms(boolean b);

    abstract GetAnimal build();
  }

  public static void buildSomethingWrong() {
    Builder b = builder();
    b.setName("Frank");
    // :: error: finalizer.invocation
    b.build();
  }

  public static void buildSomethingRight() {
    Builder b = builder();
    b.setName("Frank");
    b.setNumberOfLegs(4);
    b.setHasArms(true);
    b.build();
  }

  public static void buildSomethingRightIncludeOptional() {
    Builder b = builder();
    b.setName("Frank");
    b.setNumberOfLegs(4);
    b.setHabitat("jungle");
    b.setHasArms(true);
    b.build();
  }

  public static void buildSomethingWrongFluent() {
    // :: error: finalizer.invocation
    builder().setName("Frank").build();
  }

  public static void buildSomethingRightFluent() {
    builder().setName("Jim").setNumberOfLegs(7).setHasArms(false).build();
  }
}
