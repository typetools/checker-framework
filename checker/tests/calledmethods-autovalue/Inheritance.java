import com.google.auto.value.AutoValue;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.common.returnsreceiver.qual.This;

public class Inheritance {
    static interface Props {
        String name();

        String somethingElse();

        abstract class Builder<B extends Builder<B>> {
            public abstract @This B name(String value);
        }
    }

    @AutoValue
    abstract static class PropHolder implements Props {
        abstract int size();

        @Override
        public String somethingElse() {
            return "hi";
        }

        static Builder builder() {
            return new AutoValue_Inheritance_PropHolder.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder extends Props.Builder<Builder> {
            public abstract Builder size(int value);

            public abstract PropHolder build();
        }
    }

    static void correct() {
        PropHolder.Builder b = PropHolder.builder();
        b.name("Manu").size(1).build();
    }

    static void wrong() {
        PropHolder.Builder b = PropHolder.builder();
        // :: error: finalizer.invocation.invalid
        b.size(1).build();
    }
}
