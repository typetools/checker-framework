// A test case from the CF's all-systems tests that fails with the MC checker unless an
// implicit upper bound is made explicit.

class CFAbstractValue<V extends CFAbstractValue<V>> {}

class CFAbstractAnalysis<V extends CFAbstractValue<V>> {}

class GenericAnnotatedTypeFactory<
    Value extends CFAbstractValue<Value>, FlowAnalysis extends CFAbstractAnalysis<Value>> {

  protected FlowAnalysis createFlowAnalysis() {
    FlowAnalysis result = invokeConstructorFor();
    return result;
  }

  // The difference between this version of this test and the all-systems version is the "extends
  // Object" on
  // the next line.
  public static <T extends Object> T invokeConstructorFor() {
    return null;
  }
}
