import javax.validation.constraints.NotNull;
import org.checkerframework.checker.nullness.qual.*;

class Issue308 {
    @NonNull Object nonnull = new Object();
    @Nullable Object nullable;

    @NotNull(message = "hi") Object notnull1 = new Object();

    @NotNull(groups = {Object.class}) Object notnull2 = new Object();

    void foo() {
        nonnull = notnull1;
        notnull2 = nonnull;
        notnull1 = notnull2;

        nullable = notnull1;
        nullable = notnull2;
    }
}
