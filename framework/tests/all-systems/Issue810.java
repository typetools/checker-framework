// Test case for Issue 810
// https://github.com/typetools/checker-framework/issues/810
// @skip-test

class C {
    java.util.Map m = new java.util.HashMap();
    java.util.Set n = m.keySet();
}
