import java.lang.reflect.Field;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferToIgnore;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;

/**
 * This file contains expected errors that should exist even after the jaif type inference occurs.
 */
public class ExpectedErrors {

  // Case where the declared type is a supertype of the refined type.
  private @AinferTop int privateDeclaredField;
  public @AinferTop int publicDeclaredField;

  // The type of both privateDeclaredField and publicDeclaredField are
  // not refined to @AinferBottom.
  void assignFieldsToAinferSibling1() {
    privateDeclaredField = getAinferSibling1();
    publicDeclaredField = getAinferSibling1();
  }

  void testFields() {
    // :: warning: (argument)
    expectsAinferSibling1(privateDeclaredField);
    // :: warning: (argument)
    expectsAinferSibling1(publicDeclaredField);
  }

  // Case where the declared type is a subtype of the refined type.
  private @AinferBottom int privateDeclaredField2;
  public @AinferBottom int publicDeclaredField2;

  // The refinement cannot happen and an assignemnt type incompatible error occurs.
  void assignFieldsToAinferTop() {
    // :: warning: (assignment)
    privateDeclaredField2 = getAinferTop();
    // :: warning: (assignment)
    publicDeclaredField2 = getAinferTop();
  }

  // No errors should be issued below:
  void assignFieldsToBot() {
    privateDeclaredField2 = getBottom();
    publicDeclaredField2 = getBottom();
  }

  // Testing that the types above were not widened.
  void testFields2() {
    expectsBottom(privateDeclaredField2);
    expectsBottom(publicDeclaredField2);
  }

  // LUB TEST
  // The default type for fields is @AinferTop.
  private static int lubPrivateField;
  public static int lubPublicField;

  void assignLubFieldsToAinferSibling1() {
    lubPrivateField = getAinferSibling1();
    lubPublicField = getAinferSibling1();
  }

  static {
    lubPrivateField = getAinferSibling2();
    lubPublicField = getAinferSibling2();
  }

  void testLUBFields1() {
    // :: warning: (argument)
    expectsAinferSibling1(lubPrivateField);
    // :: warning: (argument)
    expectsAinferSibling1(lubPublicField);
  }

  void testLUBFields2() {
    // :: warning: (argument)
    expectsAinferSibling2(lubPrivateField);
    // :: warning: (argument)
    expectsAinferSibling2(lubPublicField);
  }

  private static boolean bool = false;

  public static int lubTest() {
    if (bool) {
      return (@AinferSibling1 int) 0;
    } else {
      return (@AinferSibling2 int) 0;
    }
  }

  public @AinferSibling1 int getAinferSibling1Wrong() {
    int x = lubTest();
    // :: warning: (return)
    return x;
  }

  public @AinferSibling2 int getAinferSibling2Wrong() {
    int x = lubTest();
    // :: warning: (return)
    return x;
  }

  void expectsAinferSibling1(@AinferSibling1 int t) {}

  void expectsAinferSibling2(@AinferSibling2 int t) {}

  void expectsBottom(@AinferBottom int t) {}

  void expectsBottom(@AinferBottom String t) {}

  void expectsAinferTop(@AinferTop int t) {}

  void expectsParent(@AinferParent int t) {}

  static @AinferSibling1 int getAinferSibling1() {
    return 0;
  }

  static @AinferSibling2 int getAinferSibling2() {
    return 0;
  }

  @AinferBottom int getBottom() {
    return 0;
  }

  @AinferTop
  int getAinferTop() {
    return 0;
  }

  // Method Field.setBoolean != ExpectedErrors.setBoolean.
  // No refinement should happen.
  void test(Field f) throws Exception {
    f.setBoolean(null, false);
  }

  void setBoolean(Object o, boolean b) {
    // :: warning: (assignment)
    @AinferBottom Object bot = o;
  }

  public class SuppressWarningsTest {
    // Tests that whole-program inference in a @SuppressWarnings block is ignored.
    private int i;
    private int i2;

    @SuppressWarnings("all")
    public void suppressWarningsTest() {
      i = (@AinferSibling1 int) 0;
      i2 = getAinferSibling1();
    }

    public void suppressWarningsTest2() {
      SuppressWarningsInner.i = (@AinferSibling1 int) 0;
      SuppressWarningsInner.i2 = getAinferSibling1();
    }

    public void suppressWarningsValidation() {
      // :: warning: (argument)
      expectsAinferSibling1(i);
      // :: warning: (argument)
      expectsAinferSibling1(i2);
      // :: warning: (argument)
      expectsAinferSibling1(SuppressWarningsInner.i);
      // :: warning: (argument)
      expectsAinferSibling1(SuppressWarningsInner.i2);
      // :: warning: (argument)
      expectsAinferSibling1(suppressWarningsMethodReturn());

      suppressWarningsMethodParams(getAinferSibling1());
    }

    @SuppressWarnings("all")
    public int suppressWarningsMethodReturn() {
      return getAinferSibling1();
    }

    // It is problematic to automatically test whole-program inference for method params when
    // suppressing warnings.
    // Since we must use @SuppressWarnings() for the method, we won't be able to catch any error
    // inside the method body.  Verified manually that in the "annotated" folder param's type wasn't
    // updated.
    @SuppressWarnings("all")
    public void suppressWarningsMethodParams(int param) {}
  }

  @SuppressWarnings("all")
  static class SuppressWarningsInner {
    public static int i;
    public static int i2;
  }

  class NullTest {
    // The default type for fields is @AinferDefaultType.
    private String privateField;
    public String publicField;

    // The types of both fields are not refined to @AinferBottom, as whole-program
    // inference never performs refinement in the presence of the null literal.
    @SuppressWarnings("value")
    void assignFieldsToBottom() {
      privateField = null;
      publicField = null;
    }

    // Testing the refinement above.
    void testFields() {
      // :: warning: (argument)
      expectsBottom(privateField);
      // :: warning: (argument)
      expectsBottom(publicField);
    }
  }

  class IgnoreMetaAnnotationTest2 {
    @AinferToIgnore int field;
    @IgnoreInWholeProgramInference int field2;

    void foo() {
      field = getAinferSibling1();
      field2 = getAinferSibling1();
    }

    void test() {
      // :: warning: (argument)
      expectsAinferSibling1(field);
      // :: warning: (argument)
      expectsAinferSibling1(field2);
    }
  }

  class AssignParam {
    public void f(@AinferBottom Object param) {
      // :: warning: assignment
      param = ((@AinferTop Object) null);
    }
  }
}
