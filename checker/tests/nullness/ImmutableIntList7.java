// Temporary.  Delete before merging.

import org.checkerframework.checker.nullness.qual.Nullable;

public class ImmutableIntList7 {

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ImmutableIntList7 ? true : false;
    }
}
