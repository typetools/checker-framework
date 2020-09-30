import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class CallWithinBuilder {

    public abstract ImmutableList<String> names();

    static Builder builder() {
        return new AutoValue_CallWithinBuilder.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract ImmutableList.Builder<String> namesBuilder();

        public Builder addName(String name) {
            namesBuilder().add(name);
            return this;
        }

        public Builder addNames(Collection<String> names) {
            for (String n : names) {
                addName(n);
            }
            return this;
        }

        abstract CallWithinBuilder build();
    }
}
