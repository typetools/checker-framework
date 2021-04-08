import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

@HasQualifierParameter(Tainted.class)
public class InnerHasQualifierParameter {

  @HasQualifierParameter(Tainted.class)
  interface TestInterface {
    public void testMethod();
  }

  public void test() {
    TestInterface test =
        new TestInterface() {
          public void testMethod() {}
        };
  }
}
