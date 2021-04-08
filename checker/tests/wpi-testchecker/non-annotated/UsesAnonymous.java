import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling2;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Top;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.WholeProgramInferenceBottom;

public class UsesAnonymous {
  void method() {
    Anonymous a =
        new Anonymous() {
          int innerField;

          public void method2() {
            Anonymous.field1 = getSibling2();
            Anonymous.field2 = getSibling2();
            innerField = getSibling2();
          }

          void innerFieldTest() {
            // :: warning: (argument.type.incompatible)
            expectsSibling2(innerField);
          }

          @WholeProgramInferenceBottom int getBottom() {
            return (@WholeProgramInferenceBottom int) 0;
          }

          @Top int getTop() {
            return (@Top int) 0;
          }

          @Sibling2 int getSibling2() {
            return (@Sibling2 int) 0;
          }

          void expectsSibling2(@Sibling2 int t) {}
        };
  }
}
