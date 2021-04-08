import org.checkerframework.framework.testchecker.variablenamedefault.quals.*;

public class TestVariableNameDefault {

  int top;

  int middle;
  int middlevar;
  int mymiddle;
  int notmiddle;
  int notmiddlevar;
  @VariableNameDefaultMiddle int namedbottombutnot;

  int bottom;
  int bottomvar;
  int mybottom;
  int notbottom;
  int notbottomvar;
  @VariableNameDefaultBottom int namedmiddlebutnot;

  void testFields() {

    @VariableNameDefaultTop int t;

    t = top;

    t = middle;
    t = middlevar;
    t = mymiddle;
    t = notmiddle;
    t = notmiddlevar;
    t = namedbottombutnot;

    t = bottom;
    t = bottomvar;
    t = mybottom;
    t = notbottom;
    t = notbottomvar;
    t = namedmiddlebutnot;

    @VariableNameDefaultMiddle int m;

    // :: error: (assignment.type.incompatible)
    m = top;

    m = middle;
    m = middlevar;
    m = mymiddle;
    // :: error: (assignment.type.incompatible)
    m = notmiddle;
    // :: error: (assignment.type.incompatible)
    m = notmiddlevar;
    m = namedbottombutnot;

    m = bottom;
    m = bottomvar;
    m = mybottom;
    // :: error: (assignment.type.incompatible)
    m = notbottom;
    // :: error: (assignment.type.incompatible)
    m = notbottomvar;
    m = namedmiddlebutnot;

    @VariableNameDefaultBottom int b;

    // :: error: (assignment.type.incompatible)
    b = top;

    // :: error: (assignment.type.incompatible)
    b = middle;
    // :: error: (assignment.type.incompatible)
    b = middlevar;
    // :: error: (assignment.type.incompatible)
    b = mymiddle;
    // :: error: (assignment.type.incompatible)
    b = notmiddle;
    // :: error: (assignment.type.incompatible)
    b = notmiddlevar;
    // :: error: (assignment.type.incompatible)
    b = namedbottombutnot;

    b = bottom;
    b = bottomvar;
    b = mybottom;
    // :: error: (assignment.type.incompatible)
    b = notbottom;
    // :: error: (assignment.type.incompatible)
    b = notbottomvar;
    b = namedmiddlebutnot;
  }

  void testFormals(int middle, int notmiddle, int bottom, int notbottom) {

    @VariableNameDefaultTop int t;

    t = middle;
    t = notmiddle;
    t = bottom;
    t = notbottom;

    @VariableNameDefaultMiddle int m;

    m = middle;
    // :: error: (assignment.type.incompatible)
    m = notmiddle;
    m = bottom;
    // :: error: (assignment.type.incompatible)
    m = notbottom;

    @VariableNameDefaultBottom int b;

    // :: error: (assignment.type.incompatible)
    b = middle;
    // :: error: (assignment.type.incompatible)
    b = notmiddle;
    b = bottom;
    // :: error: (assignment.type.incompatible)
    b = notbottom;
  }

  int middlemethod() {
    // :: error: (return.type.incompatible)
    return 0;
  }

  int mymiddlemethod() {
    // :: error: (return.type.incompatible)
    return 0;
  }

  int notmiddlemethod() {
    return 0;
  }

  int mynotmiddlemethod() {
    return 0;
  }

  int bottommethod() {
    // :: error: (return.type.incompatible)
    return 0;
  }

  int mybottommethod() {
    // :: error: (return.type.incompatible)
    return 0;
  }

  int notbottommethod() {
    return 0;
  }

  int mynotbottommethod() {
    return 0;
  }

  void testMethods() {

    @VariableNameDefaultTop int t;

    t = middlemethod();
    t = mymiddlemethod();
    t = notmiddlemethod();
    t = mynotmiddlemethod();

    t = bottommethod();
    t = mybottommethod();
    t = notbottommethod();
    t = mynotbottommethod();

    @VariableNameDefaultMiddle int m;

    m = middlemethod();
    m = mymiddlemethod();
    // :: error: (assignment.type.incompatible)
    m = notmiddlemethod();
    // :: error: (assignment.type.incompatible)
    m = mynotmiddlemethod();

    m = bottommethod();
    m = mybottommethod();
    // :: error: (assignment.type.incompatible)
    m = notbottommethod();
    // :: error: (assignment.type.incompatible)
    m = mynotbottommethod();

    @VariableNameDefaultBottom int b;

    // :: error: (assignment.type.incompatible)
    b = middlemethod();
    // :: error: (assignment.type.incompatible)
    b = mymiddlemethod();
    // :: error: (assignment.type.incompatible)
    b = notmiddlemethod();
    // :: error: (assignment.type.incompatible)
    b = mynotmiddlemethod();

    b = bottommethod();
    b = mybottommethod();
    // :: error: (assignment.type.incompatible)
    b = notbottommethod();
    // :: error: (assignment.type.incompatible)
    b = mynotbottommethod();
  }
}
