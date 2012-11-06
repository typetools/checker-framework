import checkers.quals.RequiresAnnotation;
import tests.util.Odd;
import tests.util.RequiresOdd;

class ContractsOverriding {
    static class Super {
        String f, g;

        void m1() {
        }

        void m2() {
        }

        @RequiresOdd("g")
        void m3() {
        }
        
        @RequiresOdd("g")
        void m3b() {
        }

        @RequiresOdd("f")
        void m4() {
        }
    }

    static class Sub extends Super {
        String g;

        @Override
        @RequiresOdd("f")
        //:: error: (contracts.precondition.override.invalid)
        void m1() {
        }

        @Override
        @RequiresAnnotation(expression = "f", annotation = Odd.class)
        //:: error: (contracts.precondition.override.invalid)
        void m2() {
        }

        @Override
        // g is a different field than in the supertype
        @RequiresOdd("g")
        //:: error: (contracts.precondition.override.invalid)
        void m3() {
        }
        
        @Override
        @RequiresOdd("super.g")
        void m3b() {
        }

        @Override
        // use different string to refer to 'f' and RequiresAnnotation instead
        // of RequiresOdd
        @RequiresAnnotation(expression = "this.f", annotation = Odd.class)
        void m4() {
        }
    }
}
