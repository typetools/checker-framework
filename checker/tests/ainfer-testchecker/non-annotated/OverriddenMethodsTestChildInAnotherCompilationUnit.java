import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class OverriddenMethodsTestChildInAnotherCompilationUnit
    extends OverriddenMethodsTest.OverriddenMethodsTestParent {
  public void callthud(@AinferSibling1 Object obj1, @AinferSibling2 Object obj2) {
    thud(obj1, obj2);
  }
}
