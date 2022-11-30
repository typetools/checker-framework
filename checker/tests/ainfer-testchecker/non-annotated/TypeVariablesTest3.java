import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class TypeVariablesTest3<@AinferSibling1 T extends @AinferSibling1 Object> {
  public @AinferSibling2 T sibling2;
  public @AinferSibling1 T sibling1;

  public T tField;

  void foo(T param) {
    // :: warning: (assignment)
    param = sibling2;
  }

  void baz(T param) {
    param = sibling1;
  }

  void bar(@AinferSibling2 T param) {
    // :: warning: (assignment)
    tField = param;
  }
}
