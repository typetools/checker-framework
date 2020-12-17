import org.checkerframework.checker.nullness.qual.*;

@org.checkerframework.framework.qual.DefaultQualifier(
        org.checkerframework.checker.nullness.qual.NonNull.class)
public class ThisTest {

    public String field;

    public ThisTest(String field) {
        this.field = field;
    }

    void doNothing() {}

    class InnerClass {
        public void accessOuterThis() {
            ThisTest.this.doNothing();
            String s = ThisTest.this.field;
        }
    }
}
