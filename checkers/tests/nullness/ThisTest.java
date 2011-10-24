import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")
class ThisTest {

    public String field;

    public ThisTest(String field) {
        this.field = field;
    }

    void doNothing() {
    }

    class InnerClass {
        public void accessOuterThis() {
            ThisTest.this.doNothing();
            String s = ThisTest.this.field;
        }
    }

}
