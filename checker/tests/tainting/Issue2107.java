import org.checkerframework.checker.tainting.qual.PolyTainted;

public abstract class Issue2107 {

  abstract @PolyTainted int method(@PolyTainted Issue2107 this);

  @PolyTainted int method2(@PolyTainted Issue2107 this) {
    return this.method();
  }

  @PolyTainted int method3(@PolyTainted Issue2107 this) {
    return method();
  }
}
