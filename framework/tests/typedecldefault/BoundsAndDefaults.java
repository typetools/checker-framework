import org.checkerframework.framework.qual.*;
import org.checkerframework.framework.testchecker.typedecldefault.quals.*;

// @TypeDeclDefaultBottom is the default qualifier in hierarchy.
@SuppressWarnings("inconsistent.constructor.type")
public class BoundsAndDefaults {
  static @TypeDeclDefaultMiddle class MiddleClass {}

  @TypeDeclDefaultBottom MiddleClass method(@TypeDeclDefaultMiddle MiddleClass middle, MiddleClass noAnno) {
    noAnno = middle;
    // :: error: (return.type.incompatible)
    return noAnno;
  }

  // :: error: (type.invalid.annotations.on.use)
  void tops(@TypeDeclDefaultTop MiddleClass invalid) {
    @TypeDeclDefaultTop MiddleClass local = null;
  }

  @NoDefaultQualifierForUse(TypeDeclDefaultTop.class)
  static @TypeDeclDefaultMiddle class MiddleBoundClass {
    @TypeDeclDefaultMiddle MiddleBoundClass() {}
  }

  @TypeDeclDefaultBottom MiddleBoundClass method(@TypeDeclDefaultMiddle MiddleBoundClass middle, MiddleBoundClass noAnno) {
    // :: error: (assignment.type.incompatible)
    noAnno = middle;
    return noAnno;
  }
}
