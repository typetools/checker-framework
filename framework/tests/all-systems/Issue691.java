// Test case for Issue 691:
// https://github.com/typetools/checker-framework/issues/691

interface MyInterface<T> {
}
@SuppressWarnings("")
// This code causes greatestLowerBound in the qualifier hierarchy to be executed, which results
// in a crash if the default implementation isn't correct for a given checker.
class Issue285<T> implements MyInterface<T> {
    MyInterface<?> mi = new Issue285<>();
}
