import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;
import org.checkerframework.framework.qual.NoQualifierParameter;

@HasQualifierParameter(Tainted.class)
public class InheritQualifierParameter {}

class SubHasQualifierParameter extends InheritQualifierParameter {
  void test(@Untainted SubHasQualifierParameter arg) {
    // :: error: (assignment.type.incompatible)
    @Tainted SubHasQualifierParameter local = arg;
  }
}

@NoQualifierParameter(Tainted.class)
// :: error: (conflicting.qual.param)
class SubHasQualifierParameter1 extends InheritQualifierParameter {}

@NoQualifierParameter(Tainted.class)
class InheritNoQualifierParameter {}

class SubNoQualifierParameter extends InheritNoQualifierParameter {
  void test(@Untainted SubNoQualifierParameter arg) {
    @Tainted SubNoQualifierParameter local = arg;
  }
}

@HasQualifierParameter(Tainted.class)
// :: error: (conflicting.qual.param)
@Tainted class SubNoQualifierParameter1 extends InheritNoQualifierParameter {}
