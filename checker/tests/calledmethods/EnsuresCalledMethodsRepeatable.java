import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;

class EnsuresCalledMethodsRepeatable {

  @EnsuresCalledMethods(
      value = "#1",
      methods = {"toString"})
  @EnsuresCalledMethods(
      value = "#1",
      methods = {"hashCode"})
  void test(Object obj) {
    obj.toString();
    obj.hashCode();
  }
}
