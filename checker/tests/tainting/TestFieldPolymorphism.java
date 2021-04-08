import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

@HasQualifierParameter(Tainted.class)
public class TestFieldPolymorphism {
  @PolyTainted String field;

  @PolyTainted TestFieldPolymorphism(@PolyTainted String s) {
    this.field = s;
  }

  @PolyTainted TestFieldPolymorphism testConstructor(@PolyTainted String s) {
    return new TestFieldPolymorphism(s);
  }

  void testSetter1(@PolyTainted TestFieldPolymorphism this, @PolyTainted String s) {
    this.field = s;
  }

  void testSetter2(@PolyTainted TestFieldPolymorphism this, @Untainted String s) {
    this.field = s;
  }

  void testSetter3(@PolyTainted TestFieldPolymorphism this, @Tainted String s) {
    // :: error: (assignment.type.incompatible)
    this.field = s;
  }

  @PolyTainted String testGetter1(@PolyTainted TestFieldPolymorphism this) {
    return this.field;
  }

  @Untainted String testGetter2(@PolyTainted TestFieldPolymorphism this) {
    // :: error: (return.type.incompatible)
    return this.field;
  }

  static @Untainted String testInstantiateUntaintedGetter(@Untainted TestFieldPolymorphism c) {
    return c.field;
  }

  static void testInstantiateUntaintedSetter(
      @Untainted TestFieldPolymorphism c, @Tainted String s) {
    // :: error: (assignment.type.incompatible)
    c.field = s;
  }

  static @Untainted String testInstantiateTaintedGetter(@Tainted TestFieldPolymorphism c) {
    // :: error: (return.type.incompatible)
    return c.field;
  }

  static void testInstantiateTaintedSetter(@Tainted TestFieldPolymorphism c, @Tainted String s) {
    c.field = s;
  }
}
