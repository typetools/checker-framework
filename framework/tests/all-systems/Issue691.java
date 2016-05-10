// Test case for Issue 691:
// https://github.com/typetools/checker-framework/issues/691

interface MyInterface<T> {
}
@SuppressWarnings("")
class Issue285<T> implements MyInterface<T> {
    MyInterface<?> mi = new Issue285<>();
}
