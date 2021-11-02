// Test case for https://tinyurl.com/cfissue/3622

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class Issue3622 {

    public class ImmutableIntList1 {

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof ImmutableIntList1) {
                return true;
            } else {
                return obj instanceof List;
            }
        }
    }

    public class ImmutableIntList2 {

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ImmutableIntList2;
        }
    }

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

    public class ImmutableIntList4 {

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ImmutableIntList4 ? true : obj instanceof List;
        }
    }

    public class ImmutableIntList5 {

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ImmutableIntList5
                    ? obj instanceof ImmutableIntList5
                    : obj instanceof ImmutableIntList5;
        }
    }

    public class ImmutableIntList6 {

        @Override
        public boolean equals(@Nullable Object obj) {
            return true ? obj instanceof ImmutableIntList6 : obj instanceof ImmutableIntList6;
        }
    }

    public class ImmutableIntList7 {
        @Override
        public boolean equals(@Nullable Object obj) {
            // :: error:  (contracts.conditional.postcondition.not.satisfied)
            return (obj instanceof ImmutableIntList7) ? true : !(obj instanceof List);
        }
    }

    public class ImmutableIntList8 {

        @Override
        // The ternary expression has the condition of literal `true`, so the false-expression is
        // unreachable. However the store in the unreachable false-branch (where `obj` is @Nullable)
        // is propagated to the merge point, which causes the false positive.
        // TODO: prune the dead branch like https://github.com/typetools/checker-framework/pull/3389
        @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
        public boolean equals(@Nullable Object obj) {
            return true ? obj instanceof ImmutableIntList8 : false;
        }
    }

    public class ImmutableIntList9 {

        @Override
        // The false expression of the tenary expression is literal `false`. In this case only the
        // else-store after `false` should be propagated to the else-store of the merge point.
        // TODO: adapt the way of store propagation for boolean variables. i.e. for `true`, only
        // then-store is propagated; and for `false`, only else-store is propagated.
        @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ImmutableIntList9 ? true : false;
        }
    }

    public class ImmutableIntList10 {

        @Override
        // The false positive is because in the Nullness analysis the values of boolean variables
        // are not stored, therefore the relation between boolean variable `b` and `obj` is not
        // known
        @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
        public boolean equals(@Nullable Object obj) {
            boolean b;
            if (obj instanceof ImmutableIntList10) {
                b = true;
            } else {
                b = false;
            }
            return b;
        }
    }
}
