import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class EnumTypeArgs {

  enum MyEnum {
    CONST1,
    CONST2,
  }

  void method(@Untainted MyEnum e1, @Tainted MyEnum e2) {
    e1.compareTo(e2);
  }
}
