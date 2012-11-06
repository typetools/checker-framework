import checkers.quals.EnsuresAnnotation;
import checkers.quals.RequiresAnnotation;
import tests.util.EnsuresOdd;
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
    
    static class Sub2 extends Super2 {
        String g;

        @Override
        //:: error: (contracts.postcondition.override.invalid)
        void m1() {
        }

        @Override
        //:: error: (contracts.postcondition.override.invalid)
        void m2() {
        }

        @Override
        @EnsuresOdd("g")
        //:: error: (contracts.postcondition.override.invalid)
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

        @EnsuresAnnotation(expression = "f", annotation = Odd.class)
        void m2() {
            f = odd;
        }

        @EnsuresOdd("g")
        void m3() {
            g = odd;
        }
        
        @EnsuresAnnotation(expression = "this.f", annotation = Odd.class)
        void m4() {
            f = odd;
        }
    }
}
