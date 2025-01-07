import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.framework.testchecker.lib.VarargsMethods;

public class VarargsRe {
  // VarargsMethods is declared in
  // framework/tests/src/org/checkerframework/framework/testchecker/lib.
  // All the methods return the length of the vararg.
  public void use0() {
    @IntVal(0) int i1 = VarargsMethods.test0();
    @IntVal(1) int i2 = VarargsMethods.test0(-1);
    @IntVal(5) int i3 = VarargsMethods.test0(0, "sldfj", 0, 234, 234);
  }

  public void use1() {
    @IntVal(0) int i1 = VarargsMethods.test1("13");
    @IntVal(1) int i2 = VarargsMethods.test1("13", -1);
    @IntVal(5) int i3 = VarargsMethods.test1("13", 0, "sldfj", 0, 234, 234);
  }

  public void use2() {
    @IntVal(0) int i1 = VarargsMethods.test2("", "");
    @IntVal(1) int i2 = VarargsMethods.test2("", "", -1);
    @IntVal(5) int i3 = VarargsMethods.test2("", "", 0, "sldfj", 0, 234, 234);
  }
}
