import com.google.auto.value.AutoValue;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class NonBuildName {

    public abstract String name();

    static Builder builder() {
        return new AutoValue_NonBuildName.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder setName(String name);

        abstract NonBuildName makeIt();
    }

    static void correct() {
        Builder b = builder();
        b.setName("Phil");
        b.makeIt();
    }

    static void wrong() {
        Builder b = builder();
        // :: error: finalizer.invocation.invalid
        b.makeIt();
    }
}
