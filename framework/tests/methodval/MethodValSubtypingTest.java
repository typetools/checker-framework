import org.checkerframework.common.reflection.qual.MethodVal;

public class MethodValSubtypingTest {
  @MethodVal(className = "class", methodName = "method", params = 0) Object classMethod0 = null;

  @MethodVal(className = "class", methodName = "method", params = 0) Object classMethod0Dup = null;

  @MethodVal(
      className = {"class", "class2"},
      methodName = {"method", "method2"},
      params = {0, 1})
  Object classClass2Method0 = null;

  @MethodVal(
      className = {"class2", "class"},
      methodName = {"method", "method2"},
      params = {0, 1})
  Object class2classMethod0 = null;

  Object unknown = null;

  void methodValSubtyping() {
    classMethod0 = classMethod0Dup;
    // :: error: (assignment.type.incompatible)
    classMethod0 = classClass2Method0;
    // :: error: (assignment.type.incompatible)
    classClass2Method0 = class2classMethod0;
    classClass2Method0 = classMethod0;
  }

  void bottomMethodVal() {
    classMethod0 = null;
    classClass2Method0 = null;
  }

  void unknownMethodVal1() {
    unknown = class2classMethod0;
  }

  void unknownMethodVal2() {
    // :: error: (assignment.type.incompatible)
    class2classMethod0 = unknown;
  }

  @MethodVal(
      className = {"aclass", "aclass", "aclass"},
      methodName = {"amethod", "amethod", "amethod"},
      params = {0, 1, 2})
  Object triple = null;

  @MethodVal(
      className = {"aclass", "aclass", "aclass"},
      methodName = {"amethod", "amethod", "amethod"},
      params = {2, 1, 0})
  Object tripleAgain = null;

  @MethodVal(
      className = {"aclass"},
      methodName = {"amethod"},
      params = {2})
  Object one = null;

  void test() {
    tripleAgain = triple;
    // :: error: (assignment.type.incompatible)
    one = triple;
    triple = one;
  }
}
