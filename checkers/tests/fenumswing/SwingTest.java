import checkers.fenum.quals.SwingBoxOrientation;
import checkers.fenum.quals.SwingHorizontalOrientation;
import checkers.fenum.quals.SwingVerticalOrientation;
import checkers.fenum.quals.SwingCompassDirection;

public class SwingTest {

  static @SwingVerticalOrientation int BOTTOM;
  static @SwingCompassDirection int NORTH;

  static @SwingHorizontalOrientation int CENTER;
  static @SwingHorizontalOrientation int LEFT;


  static void m(@SwingVerticalOrientation int box) {}

  public static void main(String[] args) {
    // ok
    m(BOTTOM);

    //:: error: (argument.type.incompatible)
    m(5);

    //:: error: (argument.type.incompatible)
    m(NORTH);
  }

  @SuppressWarnings("swingverticalorientation")
  static void ignoreAll() {
    m(NORTH);

    @SwingVerticalOrientation int b = 5;
  }

  @SuppressWarnings("fenum:argument.type.incompatible")
  static void ignoreOne() {
    m(NORTH);

    //:: error: (assignment.type.incompatible)
    @SwingVerticalOrientation int b = 5;
  }

  void testNull() {
    // This enum should only be used on ints, but I wanted to
    // test how an Object enum and null interact.
    @SwingVerticalOrientation Object box = null;
  }

  @SwingVerticalOrientation Object testNullb() {
      return null;
  }

  @SwingVerticalOrientation Object testNullc() {
      Object o = null;
      return o;
  }

  @SwingVerticalOrientation int testInference0() {
    //:: error: (assignment.type.incompatible)
    @SwingVerticalOrientation int boxint = 5;
    int box = boxint;
    return box;
  }

  Object testInference1() {
      Object o = new String();
      return o;
  }

  @SwingVerticalOrientation int testInference2() {
      int i = BOTTOM;
      return i;
  }

  @SwingVerticalOrientation Object testInference3() {
    //:: error: (assignment.type.incompatible)
    @SwingVerticalOrientation Object boxobj = new Object();
    Object obox = boxobj;
    return obox;
  }

  int testInference4() {
    int aint = 5;
    return aint;
  }

  Object testInference5() {
      Object o = null;
      if( 5==4 ) {
        o = new Object();
      }
      return o;
  }

  Object testInference5b() {
      Object o = null;
      if( 5==4 ) {
        o = new Object();
      } else {}
      // the empty else branch actually covers a different code path!
      return o;
  }

  @SwingHorizontalOrientation int testInference5c() {
      int o;
      if( 5==4 ) {
        o = CENTER;
      } else {
        o = LEFT;
      }
      return o;
  }

  int testInference6() {
    int last = 0;
    last += 1;
    return last;
  }

  @SwingBoxOrientation Object testInference7() {
      Object o = new @SwingVerticalOrientation Object();
      if( 5==4 ) {
        o = new @SwingHorizontalOrientation Object();
      } else {
      //   o = new @SwingVerticalOrientation Object();
      }
      return o;
  }

  @SwingBoxOrientation Object testInference7b() {
    Object o;
    if( 5==4 ) {
      o = new @SwingHorizontalOrientation Object();
    } else {
      o = new @SwingVerticalOrientation Object();
    }
    return o;
  }

  @SwingBoxOrientation Object testInference7c() {
    Object o = null;
    if( 5==4 ) {
      o = new @SwingHorizontalOrientation Object();
    } else {
      o = new @SwingVerticalOrientation Object();
    }
    return o;
  }

  int s1 = 0;
  int c = 3;

  void testInference8() {
    int s2 = s1;
    s1 = (s2 &= c);
  }

  void testInference8b() {
    //:: error: (assignment.type.incompatible)
    @SwingHorizontalOrientation int s2 = 5;
    //:: error: (compoundassign.type.incompatible)
    s2 += 1;

    //:: error: (assignment.type.incompatible)
    s1 = (s2 += s2);

    //:: error: (assignment.type.incompatible)
    @SwingHorizontalOrientation String str = "abc";
    // yes, somebody in the Swing API really wrote this.
    str += null;
  }

  boolean flag;

  Object testInference9() {
      Object o = null;
      while(flag) {
        if( 5==4 ) {
          o = new @SwingHorizontalOrientation Object();
        } else {
          o = new @SwingVerticalOrientation Object();
          // note that this break makes a difference!
          break;
        }
      }
      //:: error: (return.type.incompatible)
      return o;
  }

  @SwingBoxOrientation Object testInference9b() {
      Object o = null;
      while(flag) {
        if( 5==4 ) {
          o = new @SwingHorizontalOrientation Object();
        } else {
          o = new @SwingVerticalOrientation Object();
          // note that this break makes a difference!
          break;
        }
      }
      return o;
  }

  @SwingHorizontalOrientation Object testInference9c() {
      Object o = null;
      while(flag) {
        if( 5==4 ) {
          o = new @SwingHorizontalOrientation Object();
        } else {
          o = new @SwingVerticalOrientation Object();
          // note that this break makes a difference!
          break;
        }
      }
      //:: error: (return.type.incompatible)
      return o;
  }

  @SwingVerticalOrientation Object testInference9d() {
      Object o = null;
      while(flag) {
        if( 5==4 ) {
          o = new @SwingHorizontalOrientation Object();
        } else {
          o = new @SwingVerticalOrientation Object();
          // note that this break makes a difference!
          break;
        }
      }
      //:: error: (return.type.incompatible)
      return o;
  }

  @SwingBoxOrientation Object testInference9e() {
    Object o = null;
    while(flag) {
      if( 5==4 ) {
        o = new @SwingHorizontalOrientation Object();
      } else {
        o = new @SwingVerticalOrientation Object();
      }
    }
    return o;
  }

  /* TODO: Flow inference does not handle dead branches correctly.
   * The return statement is only reachable with i being unqualified.
   * However, the else-branch does not initialize the variable, leaving it
   * at the @FenumTop initial value.
  int testInferenceThrow() {
      int i;
      if( 5==4 ) {
        i = 5;
      } else {
        throw new RuntimeException("bla");
      }
      return i;
  }
  */

  @SwingVerticalOrientation Object testDefaulting0() {
    @checkers.quals.DefaultQualifier("SwingVerticalOrientation")
    Object o = new String();
    return o;
  }
}
