// @below-java17-jdk-skip-test
// from https://docs.oracle.com/en/java/javase/15/language/sealed-classes-and-interfaces.html

public sealed class Figure
// The permits clause has been omitted
// as its permitted classes have been
// defined in the same file.
{}

final class Circle extends Figure {
  float radius;
}

non-sealed class Square extends Figure {
  float side;
}

sealed class Rectangle extends Figure {
  float length, width;
}

final class FilledRectangle extends Rectangle {
  int red, green, blue;
}
