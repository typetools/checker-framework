import com.google.auto.value.AutoValue;

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Adapted from the standard AutoValue example code:
 * https://github.com/google/auto/blob/master/value/userguide/builders.md
 */
@AutoValue
abstract class AnimalNoSet {
    abstract String name();

    abstract @Nullable String habitat();

    abstract int numberOfLegs();

    public String getStr() {
        return "str";
    }

    static Builder builder() {
        return new AutoValue_AnimalNoSet.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder name(String value);

        abstract Builder numberOfLegs(int value);

        abstract Builder habitat(String value);

        abstract AnimalNoSet build();
    }

    public static void buildSomethingWrong() {
        Builder b = builder();
        b.name("Frank");
        // :: error: finalizer.invocation.invalid
        b.build();
    }

    public static void buildSomethingRight() {
        Builder b = builder();
        b.name("Frank");
        b.numberOfLegs(4);
        b.build();
    }

    public static void buildSomethingRightIncludeOptional() {
        Builder b = builder();
        b.name("Frank");
        b.numberOfLegs(4);
        b.habitat("jungle");
        b.build();
    }

    public static void buildSomethingWrongFluent() {
        // :: error: finalizer.invocation.invalid
        builder().name("Frank").build();
    }

    public static void buildSomethingRightFluent() {
        builder().name("Jim").numberOfLegs(7).build();
    }
}
