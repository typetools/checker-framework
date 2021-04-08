import org.checkerframework.checker.testchecker.wholeprograminference.qual.Parent;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling2;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Top;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.WholeProgramInferenceBottom;

public class Anonymous {
  public static int field1; // parent
  public static int field2; // sib2

  public Anonymous() {
    field1 = getSibling1();
  }

  void testPublicInference() {
    // :: warning: (argument.type.incompatible)
    expectsSibling2(field2);
    // :: warning: (argument.type.incompatible)
    expectsParent(field1);
    // :: warning: (argument.type.incompatible)
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
