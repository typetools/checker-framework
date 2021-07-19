import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling2;

public class OverriddenMethodsTestChildInAnotherCompilationUnit
    extends OverriddenMethodsTestParent {
  public void callthud(@Sibling1 Object obj1, @Sibling2 Object obj2) {
    thud(obj1, obj2);
  }
}
