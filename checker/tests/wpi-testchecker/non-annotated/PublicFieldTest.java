import org.checkerframework.checker.testchecker.wholeprograminference.qual.Parent;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling2;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Top;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.WholeProgramInferenceBottom;

public class PublicFieldTest {
  public static int field1; // parent
  public static int field2; // sib2

  public PublicFieldTest() {
    field1 = getSibling1();
  }

  void testPublicInference() {
    // :: warning: (argument)
    expectsSibling2(field2);
    // :: warning: (argument)
    expectsParent(field1);
    // :: warning: (argument)
    expectsParent(field2);
  }

  void expectsBottom(@WholeProgramInferenceBottom int t) {}

  void expectsSibling1(@Sibling1 int t) {}

  void expectsSibling2(@Sibling2 int t) {}

  void expectsTop(@Top int t) {}

  void expectsParent(@Parent int t) {}

  @Sibling1 int getSibling1() {
    return (@Sibling1 int) 0;
  }
}

class AnotherClass {

  int innerField;

  public AnotherClass() {
    PublicFieldTest.field1 = getSibling2();
    PublicFieldTest.field2 = getSibling2();
    innerField = getSibling2();
  }

  void innerFieldTest() {
    // :: warning: (argument)
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
}
