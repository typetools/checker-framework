// Test case for https://tinyurl.com/cfissue/3622

// @skip-test until the issue is fixed

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3622 {

    public class ImmutableIntList1 {

        @Override
        public boolean equals(@Nullable Object obj1) {
            return obj1 instanceof ImmutableIntList1 ? true : obj1 instanceof List;
        }
    }

    public class ImmutableIntList2 {

        @Override
        public boolean equals(@Nullable Object obj2) {
            if (obj2 instanceof ImmutableIntList2) {
                return true;
            } else {
                return obj2 instanceof List;
            }
        }
    }

    public class ImmutableIntList3 {

        @Override
        public boolean equals(@Nullable Object obj3) {
            return obj3 instanceof ImmutableIntList3;
        }
    }
}
