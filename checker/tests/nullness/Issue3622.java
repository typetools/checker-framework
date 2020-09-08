// Test case for https://tinyurl.com/cfissue/3622

// @skip-test until the issue is fixed

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3622 {

    // These currently pass (no warnings)

    public class ImmutableIntList1 {

        @Override
        public boolean equals(@Nullable Object obj2) {
            if (obj2 instanceof ImmutableIntList2) {
                return true;
            } else {
                return obj2 instanceof List;
            }
        }
    }

    public class ImmutableIntList2 {

        @Override
        public boolean equals(@Nullable Object obj3) {
            return obj3 instanceof ImmutableIntList3;
        }
    }

    // These currently fail (false positive warnings)

    public class ImmutableIntList5 {

        @Override
        public boolean equals(@Nullable Object obj5) {
            return true ? obj5 instanceof ImmutableIntList5 : obj5 instanceof ImmutableIntList5;
        }
    }

    public class ImmutableIntList6 {

        @Override
        public boolean equals(@Nullable Object obj6) {
            return true ? obj6 instanceof ImmutableIntList6 : false;
        }
    }

    public class ImmutableIntList7 {

        @Override
        public boolean equals(@Nullable Object obj7) {
            return obj7 instanceof ImmutableIntList7 ? true : false;
        }
    }

    public class ImmutableIntList8 {

        @Override
        public boolean equals(@Nullable Object obj8) {
            return obj8 instanceof ImmutableIntList8 ? true : obj8 instanceof List;
        }
    }

    public class ImmutableIntList9 {

        @Override
        public boolean equals(@Nullable Object obj9) {
            return obj9 instanceof ImmutableIntList9
                    ? obj9 instanceof ImmutableIntList9
                    : obj9 instanceof ImmutableIntList9;
        }
    }
}
