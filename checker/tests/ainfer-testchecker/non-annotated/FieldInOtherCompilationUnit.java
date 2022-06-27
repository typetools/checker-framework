import java.util.GregorianCalendar;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;

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
