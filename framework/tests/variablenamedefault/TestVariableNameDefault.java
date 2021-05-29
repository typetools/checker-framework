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

    // :: error: (assignment)
    m = top;

    m = middle;
    m = middlevar;
    m = mymiddle;
    // :: error: (assignment)
    m = notmiddle;
    // :: error: (assignment)
    m = notmiddlevar;
    m = namedbottombutnot;

    m = bottom;
    m = bottomvar;
    m = mybottom;
    // :: error: (assignment)
    m = notbottom;
    // :: error: (assignment)
    m = notbottomvar;
    m = namedmiddlebutnot;

    @VariableNameDefaultBottom int b;

    // :: error: (assignment)
    b = top;

    // :: error: (assignment)
    b = middle;
    // :: error: (assignment)
    b = middlevar;
    // :: error: (assignment)
    b = mymiddle;
    // :: error: (assignment)
    b = notmiddle;
    // :: error: (assignment)
    b = notmiddlevar;
    // :: error: (assignment)
    b = namedbottombutnot;

    b = bottom;
    b = bottomvar;
    b = mybottom;
    // :: error: (assignment)
    b = notbottom;
    // :: error: (assignment)
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
    // :: error: (assignment)
    m = notmiddle;
    m = bottom;
    // :: error: (assignment)
    m = notbottom;

    @VariableNameDefaultBottom int b;

    // :: error: (assignment)
    b = middle;
    // :: error: (assignment)
    b = notmiddle;
    b = bottom;
    // :: error: (assignment)
    b = notbottom;
  }

  int middlemethod() {
    // :: error: (return)
    return 0;
  }

  int mymiddlemethod() {
    // :: error: (return)
    return 0;
  }

  int notmiddlemethod() {
    return 0;
  }

  int mynotmiddlemethod() {
    return 0;
  }

  int bottommethod() {
    // :: error: (return)
    return 0;
  }

  int mybottommethod() {
    // :: error: (return)
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
    // :: error: (assignment)
    m = notmiddlemethod();
    // :: error: (assignment)
    m = mynotmiddlemethod();

    m = bottommethod();
    m = mybottommethod();
    // :: error: (assignment)
    m = notbottommethod();
    // :: error: (assignment)
    m = mynotbottommethod();

    @VariableNameDefaultBottom int b;

    // :: error: (assignment)
    b = middlemethod();
    // :: error: (assignment)
    b = mymiddlemethod();
    // :: error: (assignment)
    b = notmiddlemethod();
    // :: error: (assignment)
    b = mynotmiddlemethod();

    b = bottommethod();
    b = mybottommethod();
    // :: error: (assignment)
    b = notbottommethod();
    // :: error: (assignment)
    b = mynotbottommethod();
  }
}
