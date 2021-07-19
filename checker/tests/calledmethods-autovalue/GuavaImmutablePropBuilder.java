import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class GuavaImmutablePropBuilder {

    public abstract ImmutableList<String> names();

    static Builder builder() {
        return new AutoValue_GuavaImmutablePropBuilder.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract ImmutableList.Builder<String> namesBuilder();

        abstract GuavaImmutablePropBuilder build();
    }

    public static void buildSomething() {
        // don't need to explicitly set the names
        builder().build();
    }
}
