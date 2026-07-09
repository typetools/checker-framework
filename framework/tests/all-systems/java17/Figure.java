// @below-java17-jdk-skip-test
// @infer-jaifs-skip-test
// from https://docs.oracle.com/en/java/javase/15/language/sealed-classes-and-interfaces.html

public sealed class Figure
// The permits clause has been omitted
// as its permitted classes have been
// defined in the same file.
{}

@SuppressWarnings({
  "initializedfields:contracts.postcondition",
  "unneeded.suppression" // The suppression is needed when the InitializedFields Checker is run
  // without the Value Checker.
})
final class Circle extends Figure {
  float radius;
}

@SuppressWarnings({
  "initializedfields:contracts.postcondition",
  "unneeded.suppression" // The suppression is needed when the InitializedFields Checker is run
  // without the Value Checker.
})
non-sealed class Square extends Figure {
  float side;
}

@SuppressWarnings({
  "initializedfields:contracts.postcondition",
  "unneeded.suppression" // The suppression is needed when the InitializedFields Checker is run
  // without the Value Checker.
})
sealed class Rectangle extends Figure {
  float length, width;
}

@SuppressWarnings({
  "initializedfields:contracts.postcondition",
  "unneeded.suppression" // The suppression is needed when the InitializedFields Checker is run
  // without the Value Checker.
})
final class FilledRectangle extends Rectangle {
  int red, green, blue, sealed;
}
