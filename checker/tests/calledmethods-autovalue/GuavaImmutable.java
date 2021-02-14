import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class GuavaImmutable {

    public abstract ImmutableList<String> names();

    static Builder builder() {
        return new AutoValue_GuavaImmutable.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder names(ImmutableList<String> value);

        abstract GuavaImmutable build();
    }

    public static void buildSomethingWrong() {
        // :: error: finalizer.invocation.invalid
        builder().build();
    }

    public static void buildSomethingRight() {
        builder().names(ImmutableList.of()).build();
    }
}
