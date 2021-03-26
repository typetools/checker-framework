import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;

public class StringValNull {

  public static void main(String[] args) {
    @StringVal("itsValue") String nbleString = null;
    @StringVal("itsValue") String nnString = "itsValue";

    System.out.println(toString1(nbleString));
    System.out.println(toString2(nbleString));

    System.out.println(toString1(nnString));
    System.out.println(toString2(nnString));
    // System.out.println(toString3(nnString));

    @IntVal(22) Integer nbleInteger = null;
    @IntVal(22) Integer nnInteger = 22;

    System.out.println(toString4(nbleInteger));
    System.out.println(toString5(nbleInteger));

    System.out.println(toString4(nnInteger));
    System.out.println(toString5(nnInteger));
    System.out.println(toString6(nnInteger));
  }

  static @StringVal("arg=itsValue") String toString1(@Nullable @StringVal("itsValue") String arg) {
    // :: error: (return.type.incompatible)
    return "arg=" + arg;
  }

  static @StringVal({"arg=itsValue", "arg=null"}) String toString2(
      @Nullable @StringVal("itsValue") String arg) {
    return "arg=" + arg;
  }

  /* static @StringVal("arg=itsValue") String toString3(@StringVal("itsValue") String arg) {
      return "arg=" + arg;
  } */

  static @StringVal("arg=22") String toString4(@Nullable @IntVal(22) Integer arg) {
    // :: error: (return.type.incompatible)
    return "arg=" + arg;
  }

  static @StringVal({"arg=22", "arg=null"}) String toString5(@Nullable @IntVal(22) Integer arg) {
    return "arg=" + arg;
  }

  static @StringVal("arg=22") String toString6(@IntVal(22) int arg) {
    return "arg=" + arg;
  }

  final @StringVal("hello") String s = null;

  @StringVal("hello") String s2 = null;

  void method2(StringValNull obj) {
    // :: error: (assignment.type.incompatible)
    @StringVal("hello") String l1 = "" + obj.s;
    // :: error: (assignment.type.incompatible)
    @StringVal("hello") String l2 = "" + obj.s2;
  }
}
