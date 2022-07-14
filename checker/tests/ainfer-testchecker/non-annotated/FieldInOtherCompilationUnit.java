import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;

import java.util.GregorianCalendar;

public class FieldInOtherCompilationUnit {

    static @Sibling1 int myTime;

    static void test() {
        new GregorianCalendar() {
            public void newMethod() {
                this.time = myTime;
            }
        };
    }
}
