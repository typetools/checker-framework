import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.common.reflection.qual.MethodVal;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.StringVal;

public class MethodValInferenceTest {
  boolean flag = true;

  public void testGetMethodParamLen(
      Class<?> @ArrayLen(2) [] classArray2, Class<?>[] classArrayUnknown) throws Exception {
    @StringVal("someMethod") String str = "someMethod";
    @ClassVal("java.lang.Object") Class<?> c = Object.class;

    @MethodVal(className = "java.lang.Object", methodName = "getA", params = 0) Method m1 = c.getMethod("getA", new Class[] {});

    @MethodVal(className = "java.lang.Object", methodName = "getA", params = 0) Method m2 = c.getMethod("getA", (Class[]) null);

    @MethodVal(className = "java.lang.Object", methodName = "someMethod", params = 1) Method m3 = c.getMethod("someMethod", new Class[] {Integer.class});

    @MethodVal(className = "java.lang.Object", methodName = "equals", params = 1) Method m4 = c.getMethod("equals", Object.class);

    @MethodVal(className = "java.lang.Object", methodName = "someMethod", params = 1) Method m5 = c.getMethod(str, int.class);

    @MethodVal(className = "java.lang.Object", methodName = "getB", params = 0) Method m6 = c.getMethod("getB", new Class[0]);

    @MethodVal(className = "java.lang.Object", methodName = "someMethod", params = 1) Method m7 = c.getMethod(str, new Class[] {int.class});

    @MethodVal(className = "java.lang.Object", methodName = "someMethod", params = 2) Method m8 = c.getMethod(str, new Class[] {Integer.class, Integer.class});

    @MethodVal(className = "java.lang.Object", methodName = "someMethod", params = 2) Method m10 = c.getMethod(str, int.class, int.class);

    @MethodVal(className = "java.lang.Object", methodName = "someMethod", params = 2) Method m11 = c.getMethod(str, classArray2);

    @MethodVal(className = "java.lang.Object", methodName = "someMethod", params = -1) Method m12 = c.getMethod(str, classArrayUnknown);
  }

  public void testGetMethodMultiClassAndMethodNames(
      @ClassVal({"java.lang.Object", "java.lang.String"}) Class<?> twoClasses,
      @ClassVal({"java.lang.Object"}) Class<?> oneClass,
      @StringVal({"method1"}) String oneName,
      @StringVal({"method1", "method2"}) String twoNames,
      Class<?> @ArrayLen(2) [] classArray2,
      Class<?>[] classArrayUnknown)
      throws Exception {

    @MethodVal(
        className = {"java.lang.Object"},
        methodName = {"method1"},
        params = -1)
    Method m1 = oneClass.getMethod(oneName, classArrayUnknown);
    @MethodVal(
        className = {"java.lang.Object", "java.lang.Object"},
        methodName = {"method1", "method2"},
        params = {-1, -1})
    Method m2 = oneClass.getMethod(twoNames, classArrayUnknown);
    @MethodVal(
        className = {"java.lang.Object", "java.lang.String"},
        methodName = {"method1", "method1"},
        params = {-1, -1})
    Method m3 = twoClasses.getMethod(oneName, classArrayUnknown);
    @MethodVal(
        className = {
          "java.lang.Object",
          "java.lang.String",
          "java.lang.Object",
          "java.lang.String"
        },
        methodName = {"method1", "method2", "method2", "method1"},
        params = {-1, -1, -1, -1})
    Method m4 = twoClasses.getMethod(twoNames, classArrayUnknown);
  }

  @ClassBound("java.lang.Object") Class<?> classBound = Object.class;

  public void testGetConstructorClassBound() throws Exception {
    @MethodVal(className = "java.lang.Object", methodName = "getA", params = 0) Method m1 = classBound.getMethod("getA", new Class[] {});
  }

  public void testGetConstructorClassBoundFail() throws Exception {
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 0)
    // :: error: (assignment.type.incompatible)
    Constructor<?> con1 = classBound.getConstructor(new Class[] {}); // Should be @UnknownMethod
  }

  public void testGetConstructorParamLen(
      Class<?> @ArrayLen(2) [] classArray2, Class<?>[] classArrayUnknown) throws Exception {
    @ClassVal("java.lang.Object") Class<?> c = Object.class;
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 0) Constructor<?> con1 = c.getConstructor(new Class[] {});
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 0) Constructor<?> con2 = c.getConstructor((Class[]) null);
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 1) Constructor<?> con3 = c.getConstructor(new Class[] {Integer.class});
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 1) Constructor<?> con4 = c.getConstructor(Object.class);
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 1) Constructor<?> con5 = c.getConstructor(int.class);
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 0) Constructor<?> con6 = c.getConstructor(new Class[0]);
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 1) Constructor<?> con7 = c.getConstructor(new Class[] {int.class});
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 2) Constructor<?> con8 = c.getConstructor(new Class[] {Integer.class, Integer.class});
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 2) Constructor<?> con9 = c.getConstructor(int.class, int.class);
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = 2) Constructor<?> con10 = c.getConstructor(classArray2);
    @MethodVal(className = "java.lang.Object", methodName = "<init>", params = -1) Constructor<?> con11 = c.getConstructor(classArrayUnknown);
  }
}
