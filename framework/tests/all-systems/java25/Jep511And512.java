// Example from https://openjdk.org/jeps/512.
// @below-java25-jdk-skip-test

// None of the WPI formats supports the new Java 25 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
import module java.base;

String greeting() {
  return "Hello, World!";
}

void main() {
  IO.println(greeting());
  String name = IO.readln("Please enter your name: ");
  IO.print("Pleased to meet you, ");
  IO.println(name);
  var authors = List.of("James", "Bill", "Guy", "Alex", "Dan", "Gavin");
  for (var author : authors) {
    IO.println(name + ": " + name.length());
  }
}
