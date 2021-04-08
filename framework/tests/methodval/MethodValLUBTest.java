import java.lang.reflect.Method;
import org.checkerframework.common.reflection.qual.MethodVal;

public class MethodValLUBTest {
  Object unknown = null;
  boolean flag = false;

  @MethodVal(className = "c1", methodName = "m1", params = 0) Object c1m10 = null;

  @MethodVal(className = "c2", methodName = "m2", params = 1) Object c2m21 = null;

  void basicLub() {
    if (flag) {
      unknown = c1m10;
    } else {
      unknown = c2m21;
    }
    @MethodVal(
        className = {"c1", "c2"},
        methodName = {"m1", "m2"},
        params = {0, 1})
    Object lub = unknown;
    // :: error: (assignment.type.incompatible)
    c1m10 = unknown;
    // :: error: (assignment.type.incompatible)
    c2m21 = unknown;
  }

  @MethodVal(className = "c1", methodName = "m1", params = 0) Object c1m10duplicate = null;

  void lubSameType() {
    if (flag) {
      unknown = c1m10;
    } else {
      unknown = c1m10duplicate;
    }
    @MethodVal(className = "c1", methodName = "m1", params = 0) Object lub = unknown;
  }

  @MethodVal(className = "c1", methodName = "m1", params = 1) Object c1m11 = null;

  void simalarSigLub() {
    if (flag) {
      unknown = c1m10;
    } else {
      unknown = c1m11;
    }
    @MethodVal(
        className = {"c1", "c1"},
        methodName = {"m1", "m1"},
        params = {0, 1})
    Object lub = unknown;
    // :: error: (assignment.type.incompatible)
    c1m10 = unknown;
    // :: error: (assignment.type.incompatible)
    c1m11 = unknown;
  }

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

  void setsLub() {
    if (flag) {
      unknown = classClass2Method0;
    } else {
      unknown = class2classMethod0;
    }
    @MethodVal(
        className = {"class2", "class", "class", "class2"},
        methodName = {"method", "method2", "method", "method2"},
        params = {0, 1, 0, 1})
    Object lub = unknown;
    // :: error: (assignment.type.incompatible)
    classClass2Method0 = unknown;
    // :: error: (assignment.type.incompatible)
    class2classMethod0 = unknown;
  }

  void inferedlubTest() throws Exception {
    Class<MethodValInferenceTest> c = MethodValInferenceTest.class;
    Method m;
    if (flag) {
      m = c.getMethod("getA", new Class[0]);
    } else {
      m = c.getMethod("getB", new Class[0]);
    }
    @MethodVal(
        className = {"MethodValInferenceTest", "MethodValInferenceTest"},
        methodName = {"getA", "getB"},
        params = {0, 0})
    Method lub = m;
  }
}
