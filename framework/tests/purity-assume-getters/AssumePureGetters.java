// -AassumePureGetters assumes that every getter is side-effect-free and deterministic, including
// a getter with no purity annotation.  That is the case the option exists for:  an unannotated
// library, where writing stub files for every getter would be the alternative.

import org.checkerframework.dataflow.qual.Pure;

public class AssumePureGetters {

  int field = 0;

  /** A getter: an instance method with no formal parameters whose name starts with "get". */
  int getField() {
    return field;
  }

  /** Also a getter, by the name test. */
  boolean isEmpty() {
    return field == 0;
  }

  /** Not a getter: the name does not start with "get", "is", "not", or "has". */
  int readField() {
    return field;
  }

  /** Not a getter: "get" is not followed by an uppercase letter. */
  int getterForField() {
    return field;
  }

  /** Not a getter: it has a formal parameter. */
  int getFieldPlus(int i) {
    return field + i;
  }

  /** Not a getter: it is static. */
  static int getStaticField() {
    return 0;
  }

  @Pure
  int callGetters() {
    return getField() + (isEmpty() ? 1 : 0);
  }

  @Pure
  int callNonGetterByName() {
    // :: error: [purity.call]
    return readField();
  }

  @Pure
  int callNonGetterByCase() {
    // :: error: [purity.call]
    return getterForField();
  }

  @Pure
  int callNonGetterWithParameter() {
    // :: error: [purity.call]
    return getFieldPlus(1);
  }

  @Pure
  int callStaticGetter() {
    // :: error: [purity.call]
    return getStaticField();
  }

  /** The assumption is about the methods that a body calls, not about the body itself. */
  @Pure
  int bodyIsStillChecked() {
    // :: error: [purity.assign.field]
    field++;
    return getField();
  }
}
