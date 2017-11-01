// Test case for Issue 346:
// https://github.com/typetools/checker-framework/issues/346

class Before {}

class Context {
    // :: error: cannot find symbol
    Unknown f;
}
