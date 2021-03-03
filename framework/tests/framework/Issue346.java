// Test case for Issue 346:
// https://github.com/typetools/checker-framework/issues/346

// warning: Javac errored; type checking halted.
class Before {}

class Context {
    // :: error: cannot find symbol
    Unknown f;
}
