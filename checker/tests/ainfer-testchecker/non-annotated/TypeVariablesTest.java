import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class TypeVariablesTest<T1 extends @AinferParent Object, T2 extends @AinferParent Object> {

  // This method's parameter type should not be updated by the whole-program inference.
  // Even though there is only one call to foo with argument of type @AinferBottom,
  // the method has in its signature that the parameter is a subtype of @AinferParent,
  // therefore no annotation should be added.
  public static <A extends @AinferParent Object, B extends @AinferParent Object>
      TypeVariablesTest<A, B> foo(A a, B b) {
    return null;
  }

  public static <A extends @AinferParent Object, B extends A> void typeVarWithTypeVarUB(A a, B b) {}

  void test1() {
    @SuppressWarnings("cast.unsafe")
    @AinferParent String s = (@AinferParent String) "";
    foo(getAinferSibling1(), getAinferSibling2());
    typeVarWithTypeVarUB(getAinferSibling1(), getAinferSibling2());
  }

  static @AinferSibling1 int getAinferSibling1() {
    return 0;
  }

  static @AinferSibling2 int getAinferSibling2() {
    return 0;
  }
}
