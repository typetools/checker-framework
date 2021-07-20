import com.google.auto.value.AutoValue;

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class IsPreserved {

    abstract String name();

    abstract String getAddress();

    abstract boolean isPresent();

    static Builder builder() {
        return new AutoValue_IsPreserved.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder name(String val);

        abstract Builder getAddress(String val);

        abstract Builder isPresent(boolean value);

        abstract IsPreserved build();
    }

    public static void buildSomethingRight() {
        Builder b = builder();
        b.name("Frank");
        b.getAddress("something");
        b.isPresent(true);
        b.build();
    }

    public static void buildSomethingRightFluent() {
        builder().name("Bill").getAddress("something").isPresent(false).build();
    }
}
