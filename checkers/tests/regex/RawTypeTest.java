import checkers.regex.quals.*;

class RawTypeTest {

    class MyList<X extends @Regex String> {
        X f;
    }

    /* TODO: implement annotations on wildcard bounds
    interface I1 {
        public void m(MyList<? extends @Regex String> l);
    }

    class C1 implements I1 {
        public void m(MyList par) {
            @Regex String xxx = par.f;
        }
    }*/

    interface I2 {
        public void m(MyList<@Regex String> l);
    }

    class C2 implements I2 {
        public void m(MyList<@Regex String> l) {}
    }

    class C3 implements I2 {
        //:: error: (override.param.invalid) :: error: (generic.argument.invalid)
        public void m(MyList<String> l) {}
    }
}