import java.util.List;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintingPolyFields {
  // :: error: (invalid.polymorphic.qualifier.use)
  @PolyTainted Integer x;
  // :: error: (invalid.polymorphic.qualifier.use)
  @PolyTainted List<@PolyTainted String> lst;
  // :: error: (invalid.polymorphic.qualifier.use)
  @PolyTainted String @PolyTainted [] str;
  // :: error: (invalid.polymorphic.qualifier.use)
  List<@PolyTainted String> lst1;
  // :: error: (invalid.polymorphic.qualifier.use)
  @PolyTainted String[] str1;
  // :: error: (invalid.polymorphic.qualifier.use)
  @PolyTainted List<String> lst2;
  // :: error: (invalid.polymorphic.qualifier.use)
  String @PolyTainted [] str2;
  // :: error: (invalid.polymorphic.qualifier.use)
  @PolyTainted int z;

  // Access of poly fields outside of the declaring class.
  static void test() {
    @Tainted TaintingPolyFields obj = new @Tainted TaintingPolyFields();
    // :: error: (assignment.type.incompatible)
    @Untainted Integer myX = obj.x;
    // :: error: (assignment.type.incompatible)
    @Untainted List<@Untainted String> myLst = obj.lst;
    // :: error: (assignment.type.incompatible)
    @Untainted String @Untainted [] myStr = obj.str;

    // :: warning: (cast.unsafe.constructor.invocation)
    @Untainted TaintingPolyFields obj1 = new @Untainted TaintingPolyFields();
    @Untainted Integer myX1 = obj1.x;
    TaintingPolyFields obj2 = new TaintingPolyFields();
    // :: error: (assignment.type.incompatible)
    @Untainted List<@Untainted String> myLst2 = obj2.lst;
  }

  static void polyTest(@PolyTainted TaintingPolyFields o) {
    @PolyTainted Integer f = o.x;
  }
}

class TypeParam<T> {
  T field;
}
