// @below-java21-jdk-skip-test

// Test that a type pattern whose type is a non-leftmost bound of a type variable or intersection
// type matches every non-null selector value, so the switch has no MatchException path along which
// a resource leaks.  Such a pattern does not match a null record component, however, so a record
// pattern that nests it has a remainder, and the switch can throw MatchException.

import java.io.FileInputStream;
import java.io.IOException;

public class IntersectionBoundSwitchCoverage {

  static class Base {}

  interface Marker {}

  <T extends Base & Marker> int typePattern(T value) throws IOException {
    FileInputStream s = new FileInputStream("x");
    int r =
        switch (value) {
          case Marker m -> 1;
        };
    s.close();
    return r;
  }

  <T extends Base & Marker, U extends T> int nestedTypeVariable(U value) throws IOException {
    FileInputStream s = new FileInputStream("x");
    int r =
        switch (value) {
          case Marker m -> 1;
        };
    s.close();
    return r;
  }

  record Box<X>(X x) {}

  // `Marker m` is not unconditional for T, whose erasure is Base, so it does not match the null
  // component of `new Box<T>(null)`.  The switch throws MatchException for that value.  (It also
  // throws MatchException if the accessor throws an exception.)
  <T extends Base & Marker> int recordPattern(Box<T> box) throws IOException {
    // :: error: [required.method.not.called]
    FileInputStream s = new FileInputStream("x");
    int r =
        switch (box) {
          case Box<T>(Marker m) -> 1;
        };
    s.close();
    return r;
  }

  sealed interface Shape permits Circle, Square {}

  static final class Circle extends Base implements Shape {}

  static final class Square extends Base implements Shape {}

  // No single bound is covered by the last case, so the MatchException path remains.
  <T extends Base & Shape> int notCovered(T value) throws IOException {
    // :: error: [required.method.not.called]
    FileInputStream s = new FileInputStream("x");
    int r =
        switch (value) {
          case Circle c -> 1;
          case Square q -> 2;
        };
    s.close();
    return r;
  }
}
