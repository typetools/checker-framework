// See checker/tests/nullness/InferNullType.java for test that verifies correct Nullness Checker
// errors
public class InferNullType {

  <T extends Object> T toInfer(T input) {
    return input;
  }

  <T> T toInfer2(T input) {
    return input;
  }

  <T, S extends T> T toInfer3(T input, S p2) {
    return input;
  }

  <T extends Number, S extends T> T toInfer4(T input, S p2) {
    return input;
  }

  void x() {
    @SuppressWarnings("nullness:type.argument.type.incompatible")
    Object m = toInfer(null);
    Object m2 = toInfer2(null);

    Object m3 = toInfer3(null, null);
    Object m4 = toInfer3(1, null);
    Object m5 = toInfer3(null, 1);

    @SuppressWarnings("nullness:type.argument.type.incompatible")
    Object m6 = toInfer4(null, null);
    @SuppressWarnings("nullness:type.argument.type.incompatible")
    Object m7 = toInfer4(1, null);
    @SuppressWarnings("nullness:type.argument.type.incompatible")
    Object m8 = toInfer4(null, 1);
  }
}
