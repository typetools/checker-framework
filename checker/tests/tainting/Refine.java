import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

@HasQualifierParameter(Tainted.class)
public class Refine {
  void method(@Tainted Refine tainted, @Untainted Refine untainted) {
    // :: error: (assignment)
    @Tainted Refine local = untainted;
    // :: error: (assignment)
    @Untainted Refine untaintedLocal = local;
    @Untainted Refine untaintedLocal2 = untaintedLocal;
  }

  void methodNull() {
    @Tainted Refine local = null;
    @Untainted Refine untaintedLocal = local;
  }

  public static class SuperClass {
    @Untainted SuperClass() {}
  }

  @HasQualifierParameter(Tainted.class)
  public static class SubClass extends SuperClass {}

  static void method2(@Untainted SubClass subClass) {
    @Untainted SuperClass untainted1 = subClass;
    @Tainted SuperClass superClass = subClass;
    @Untainted SuperClass untainted2 = superClass;
  }
}
