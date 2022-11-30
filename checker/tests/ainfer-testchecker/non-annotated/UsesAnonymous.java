import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;

public class UsesAnonymous {
  void method() {
    Anonymous a =
        new Anonymous() {
          int innerField;

          public void method2() {
            Anonymous.field1 = getAinferSibling2();
            Anonymous.field2 = getAinferSibling2();
            innerField = getAinferSibling2();
          }

          void innerFieldTest() {
            // :: warning: (argument)
            expectsAinferSibling2(innerField);
          }

          @AinferBottom int getBottom() {
            return (@AinferBottom int) 0;
          }

          @AinferTop int getAinferTop() {
            return (@AinferTop int) 0;
          }

          @AinferSibling2 int getAinferSibling2() {
            return (@AinferSibling2 int) 0;
          }

          void expectsAinferSibling2(@AinferSibling2 int t) {}
        };
  }
}
