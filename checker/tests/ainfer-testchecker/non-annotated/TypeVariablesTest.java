import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

// Whole-program inference infers `@AinferDefaultType` for the formal parameters of `foo` and
// `typeVarWithTypeVarUB`.  The type of each formal parameter is a use of a type variable, and a
// primary annotation on such a use overrides the type variable's bounds.  Therefore, whole-program
// inference writes `@AinferDefaultType` even though it is the default qualifier.
@SuppressWarnings("annotation.not.allowed.in.src") // `@AinferDefaultType` on type-variable uses
public class TypeVariablesTest<T1 extends @AinferParent Object, T2 extends @AinferParent Object> {

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
