import com.google.auto.value.AutoValue;

/**
 * Adapted from the standard AutoValue example code:
 * https://github.com/google/auto/blob/master/value/userguide/builders.md
 */
@AutoValue
abstract class AnimalSimple {
  abstract String name();

  abstract int numberOfLegs();

  static Builder builder() {
    return new AutoValue_AnimalSimple.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setName(String value);

    abstract Builder setNumberOfLegs(int value);

    abstract AnimalSimple build();
  }

  public static void buildSomethingWrong() {
    Builder b = builder();
    b.setName("Frank");
    b.build();
  }

  public static void buildSomethingRight() {
    Builder b = builder();
    b.setName("Frank");
    b.setNumberOfLegs(4);
    b.build();
  }

  public static void buildSomethingWrongFluent() {
    builder().setName("Frank").build();
  }

  public static void buildSomethingRightFluent() {
    builder().setName("Jim").setNumberOfLegs(7).build();
  }

  public static void buildSomethingRightFluentWithLocal() {
    Builder b = builder();
    b.setName("Jim").setNumberOfLegs(7);
    b.build();
  }
}
