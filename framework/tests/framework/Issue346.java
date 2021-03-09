// Test case for Issue 346:
// https://github.com/typetools/checker-framework/issues/346

// :: error: (type.checking.not.run)
class Before {}

// :: error: (type.checking.not.run)
class Context {
    // :: error: cannot find symbol
    Unknown f;
}
