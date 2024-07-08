import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class VarargsObjectTest {

  static void m1Varargs(Object... args) {}

  static void m1ArrArgs(Object[] args) {}

  static void m2Varargs(Object... args) {}

  static void m2ArrArgs(Object[] args) {}

  static void m3Varargs(Object... args) {}

  static void m3ArrArgs(Object[] args) {}

  static @AinferSibling1 Object @AinferParent [] p_s1_array;
  static @AinferSibling1 Object @AinferSibling1 [] s1_s1_array;

  static void client() {
    m1Varargs(s1_s1_array);
    m1ArrArgs(s1_s1_array);

    m2Varargs(p_s1_array);
    m2ArrArgs(p_s1_array);

    m3Varargs(s1_s1_array);
    m3ArrArgs(s1_s1_array);
    m3Varargs(p_s1_array);
    m3ArrArgs(p_s1_array);
  }
}
