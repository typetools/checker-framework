// Test case for Issue 691:
// https://github.com/typetools/checker-framework/issues/691

interface MyInterface<T> {}

// This code causes greatestLowerBound in the qualifier hierarchy to be executed, which results
// in a crash if the default implementation isn't correct for a given checker. A checker could
// issue a valid type checking error for this code, so suppress any warnings.
@SuppressWarnings("all")
public class Issue691<T> implements MyInterface<T> {
  MyInterface<?> mi = new Issue691<>();
}
