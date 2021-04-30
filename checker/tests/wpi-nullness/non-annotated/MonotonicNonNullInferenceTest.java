public class MonotonicNonNullInferenceTest {

  // :: warning: (initialization.static.field.uninitialized)
  static String staticString1;

  // :: warning: (assignment)
  static String staticString2 = null;

  static String staticString3;

  String instanceString1;

  // :: warning: (assignment)
  String instanceString2 = null;

  String instanceString3;

  static {
    // :: warning: (assignment)
    staticString3 = null;
  }

  // :: warning: (initialization.fields.uninitialized)
  MonotonicNonNullInferenceTest() {
    String instanceString3 = "hello";
  }

  static void m1(String arg) {
    staticString1 = arg;
    staticString2 = arg;
    staticString3 = arg;
  }

  void m2(String arg) {
    instanceString1 = arg;
    instanceString2 = arg;
    instanceString3 = arg;
  }
}
