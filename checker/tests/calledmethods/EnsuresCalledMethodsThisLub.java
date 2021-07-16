import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;

class EnsuresCalledMethodsThisLub {

  @EnsuresCalledMethods(
      value = "#1",
      methods = {"toString", "equals"})
  void call1(Object obj) {
    obj.toString();
    obj.equals(null);
  }

  @EnsuresCalledMethods(
      value = "#1",
      methods = {"toString", "hashCode"})
  void call2(Object obj) {
    obj.toString();
    obj.hashCode();
  }

  void test(boolean b) {
    if (b) {
      call1(this);
    } else {
      call2(this);
    }
    @CalledMethods("toString") Object obj1 = this;
    // :: error: (assignment)
    @CalledMethods({"toString", "equals"}) Object obj2 = this;
  }

  void test_arg(Object arg, boolean b) {
    if (b) {
      call1(arg);
    } else {
      call2(arg);
    }
    @CalledMethods("toString") Object obj1 = arg;
    // :: error: (assignment)
    @CalledMethods({"toString", "equals"}) Object obj2 = arg;
  }
}
