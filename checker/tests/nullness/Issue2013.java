import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.dataflow.qual.Pure;

// @skip-test
public class Issue2013 {
    static class Super {
        private @Nullable String name = null;

        @EnsuresNonNull("name()")
        // :: error: (contracts.postcondition.not.satisfied)
        void ensureNameNonNull() {
            name = "name";
        }

        @RequiresNonNull("name()")
        void requiresNameNonNull() {
            name().equals("name");
        }

        @Pure
        @Nullable String name() {
            return name;
        }
    }

    static class Sub extends Super {
        @Nullable String subname = null;

        @Override
        // :: error: (contracts.postcondition.not.satisfied)
        void ensureNameNonNull() {
            super.ensureNameNonNull();
            subname = "Sub";
        }

        public static boolean flag;

        @Override
        @RequiresNonNull("name()")
        void requiresNameNonNull() {
            if (flag) {
                name().toString();
            } else {
                super.requiresNameNonNull();
            }
        }

        @Override
        @Nullable String name() {
            return subname;
        }

        void use() {
            if (super.name() != null) {
                // :: error: (contracts.precondition.not.satisfied)
                requiresNameNonNull();
            }

            if (this.name() != null) {
                requiresNameNonNull();
            }

            if (super.name() != null) {
                // :: error: (contracts.precondition.not.satisfied)
                super.requiresNameNonNull();
            }

            if (this.name() != null) {
                super.requiresNameNonNull();
            }

            super.ensureNameNonNull();
            // :: error: (contracts.precondition.not.satisfied)
            requiresNameNonNull();

            super.ensureNameNonNull();
            // :: error: (contracts.precondition.not.satisfied)
            super.requiresNameNonNull();

            ensureNameNonNull();
            super.requiresNameNonNull();

            ensureNameNonNull();
            requiresNameNonNull();
        }
    }

    void method(Super superObj) {
        if (superObj.name() != null) {
            superObj.requiresNameNonNull();
        }

        superObj.ensureNameNonNull();
        superObj.requiresNameNonNull();
    }

    void method2(Sub subObj) {
        if (subObj.name() != null) {
            subObj.requiresNameNonNull();
        }

        if (subObj.name() != null) {
            subObj.requiresNameNonNull();
        }
    }
}
