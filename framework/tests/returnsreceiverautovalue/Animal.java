import com.google.auto.value.AutoValue;

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

/**
 * Adapted from the standard AutoValue example code:
 * https://github.com/google/auto/blob/master/value/userguide/builders.md
 */
@AutoValue
abstract class Animal {
    abstract String name();

    abstract @Nullable String habitat();

    abstract int numberOfLegs();

    static Builder builder() {
        return new AutoValue_Animal.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder setName(String value);

        abstract Builder setNumberOfLegs(int value);

        abstract Builder setHabitat(String value);

        abstract Animal build();

        // wrapper methods to ensure @This annotations are getting added properly
        @This Builder wrapperSetName() {
            return setName("dummy");
        }

        @This Builder wrapperSetNumberOfLegs() {
            return setNumberOfLegs(3);
        }

        @This Builder wrapperSetHabitat() {
            return setHabitat("dummy");
        }
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

    public static void buildSomethingRightIncludeOptional() {
        Builder b = builder();
        b.setName("Frank");
        b.setNumberOfLegs(4);
        b.setHabitat("jungle");
        b.build();
    }

    public static void buildSomethingWrongFluent() {
        builder().setName("Frank").build();
    }

    public static void buildSomethingRightFluent() {
        builder().setName("Jim").setNumberOfLegs(7).build();
    }
}
