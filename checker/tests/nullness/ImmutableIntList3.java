// Temporary.  Delete before merging.

import org.checkerframework.checker.nullness.qual.Nullable;

public class ImmutableIntList3 {

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ImmutableIntList3) {
            return true;
        } else {
            return false;
        }
    }
}
