// Test case for Issue 285:
// https://code.google.com/p/checker-framework/issues/detail?id=285
class Issue285 {
    void f() {
        for (String s : new String[] {"s"}) {} 
    }
}
