import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class CompoundTypeTest {
  // The default type for fields is @AinferDefaultType.
  Object[] field;

  void assign() {
    field = getCompoundType();
  }

  void test() {
    // :: warning: (argument)
    expectsCompoundType(field);
  }

  void expectsCompoundType(@AinferSibling1 Object @AinferSibling2 [] obj) {}

  @AinferSibling1 Object @AinferSibling2 [] getCompoundType() {
    @SuppressWarnings("cast.unsafe")
    @AinferSibling1 Object @AinferSibling2 [] out = (@AinferSibling1 Object @AinferSibling2 []) new Object[1];
    return out;
  }
}
