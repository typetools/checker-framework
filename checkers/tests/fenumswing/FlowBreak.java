import checkers.fenum.quals.SwingBoxOrientation;
import checkers.fenum.quals.SwingHorizontalOrientation;
import checkers.fenum.quals.SwingVerticalOrientation;
import checkers.fenum.quals.SwingCompassDirection;

//TODO: this test illustrates a problem with Flow inference and control flow.

/**
 * @skip-test
 */
public class FlowBreak {
  static @SwingHorizontalOrientation Object CENTER;
  static @SwingHorizontalOrientation Object LEFT;

  boolean flag;

  @SwingHorizontalOrientation Object testInference() {
      Object o;
      // initially o is @FenumTop
      o = null;
      // o is @Bottom
      while(flag) {
        if( flag ) {
          o = CENTER;
          // o is @SwingHorizontalOrientation
        } else {
          o = new @SwingVerticalOrientation Object();
          // o is @SwingVerticalOrientation
          break;
        }
        // We can only come here from the then-branch, the else-branch is dead.
        // Therefore, we only take the annotations at the end of
        // the then-branch and ignore the results of the else-branch.
        // Therefore, o is @SwingHorizontalOrientation and the
        // following is valid:
        // TODO @SwingHorizontalOrientation Object pla = o;
      }
      // Here we have to merge three paths:
      // 1. The entry to the loop, if the condition is false [@Bottom]
      // 2. The normal end of the loop body [@SwingHorizontalOrientation]
      // 3. The path from the break to here [@SwingVerticalOrientation]
      // Currently, the third path is ignored and we do not get this error message.
      //:: error: (return.type.incompatible)
      return o;
  }
}
