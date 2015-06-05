// Test case for Issue 346:
// https://code.google.com/p/checker-framework/issues/detail?id=346

class Before {}

class Context {
    //:: error: cannot find symbol
    Unknown f;
}
