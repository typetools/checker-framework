import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;

public class CopyOfArray {
    protected void makeCopy(Object[] args, int i) {
        Object[] copyExact1 = Arrays.copyOf(args, args.length);
        @Nullable Object[] copyExact2 = Arrays.copyOf(args, args.length);

        // :: error: (assignment.type.incompatible)
        Object[] copyInexact1 = Arrays.copyOf(args, i);
        @Nullable Object[] copyInexact2 = Arrays.copyOf(args, i);
    }
}
