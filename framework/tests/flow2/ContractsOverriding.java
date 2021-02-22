import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.testchecker.util.EnsuresOdd;
import org.checkerframework.framework.testchecker.util.EnsuresOddIf;
import org.checkerframework.framework.testchecker.util.Odd;
import org.checkerframework.framework.testchecker.util.RequiresOdd;

public class ContractsOverriding {
    static class Super {
        String f, g;

        void m1() {}

        void m2() {}

        @RequiresOdd("g")
        void m3() {}

        @RequiresOdd("g")
        void m3b() {}

        @RequiresOdd("f")
        void m4() {}
    }

    static class Sub extends Super {
        String g;

        @Override
        @RequiresOdd("f")
        // :: error: (contracts.precondition.override.invalid)
        void m1() {}

        @Override
        @RequiresQualifier(expression = "f", qualifier = Odd.class)
        // :: error: (contracts.precondition.override.invalid)
        void m2() {}

        @Override
        // g is a different field than in the supertype
        @RequiresOdd("g")
        // :: error: (contracts.precondition.override.invalid)
        void m3() {}

        @Override
        @RequiresOdd("super.g")
        void m3b() {}

        @Override
        // use different string to refer to 'f' and RequiresQualifier instead
        // of RequiresOdd
        @RequiresQualifier(expression = "this.f", qualifier = Odd.class)
        void m4() {}
    }

    static class Sub2 extends Super2 {
        String g;

        @Override
        // :: error: (contracts.postcondition.not.satisfied)
        void m1() {}

        @Override
        // :: error: (contracts.postcondition.not.satisfied)
        void m2() {}

        @Override
        @EnsuresOdd("g")
        // :: error: (contracts.postcondition.override.invalid)
        void m3() {
            g = odd;
        }

        @Override
        @EnsuresOdd("f")
        void m4() {
            super.m4();
        }
    }

    static class Super2 {
        String f, g;
        @Odd String odd;

        @EnsuresOdd("f")
        void m1() {
            f = odd;
        }

        @EnsuresQualifier(expression = "f", qualifier = Odd.class)
        void m2() {
            f = odd;
        }

        @EnsuresOdd("g")
        void m3() {
            g = odd;
        }

        @EnsuresQualifier(expression = "this.f", qualifier = Odd.class)
        void m4() {
            f = odd;
        }
    }

    static class Sub3 extends Super3 {
        String g;

        @Override
        boolean m1() {
            // :: error: (contracts.conditional.postcondition.not.satisfied)
            return true;
        }

        @Override
        boolean m2() {
            // :: error: (contracts.conditional.postcondition.not.satisfied)
            return true;
        }

        @Override
        @EnsuresOddIf(expression = "g", result = true)
        // :: error: (contracts.conditional.postcondition.true.override.invalid)
        boolean m3() {
            g = odd;
            return true;
        }

        @Override
        @EnsuresOddIf(expression = "f", result = true)
        boolean m4() {
            return super.m4();
        }

        @Override
        // invalid result
        @EnsuresOddIf(expression = "f", result = false)
        boolean m5() {
            f = odd;
            return true;
        }

        @EnsuresOddIf(expression = "f", result = false)
        boolean m6() {
            f = odd;
            return true;
        }

        @EnsuresQualifierIf(expression = "this.f", qualifier = Odd.class, result = true)
        boolean m7() {
            f = odd;
            return true;
        }
    }

    static class Super3 {
        String f, g;
        @Odd String odd;

        @EnsuresOddIf(expression = "f", result = true)
        boolean m1() {
            f = odd;
            return true;
        }

        @EnsuresQualifierIf(expression = "f", qualifier = Odd.class, result = true)
        boolean m2() {
            f = odd;
            return true;
        }

        @EnsuresOddIf(expression = "g", result = true)
        boolean m3() {
            g = odd;
            return true;
        }

        @EnsuresQualifierIf(expression = "this.f", qualifier = Odd.class, result = true)
        boolean m4() {
            f = odd;
            return true;
        }

        @EnsuresQualifierIf(expression = "this.f", qualifier = Odd.class, result = true)
        boolean m5() {
            f = odd;
            return true;
        }
    }
}
