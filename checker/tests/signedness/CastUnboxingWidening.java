import org.checkerframework.checker.signedness.qual.SignedPositive;

// A cast to a primitive type unboxes an operand whose type is a type variable bounded by a boxed
// primitive, and then widens it.  Widening a char to a long yields a @SignedPositive value.
public class CastUnboxingWidening {

  void unboxAndWiden(Character c) {
    @SignedPositive long l = (long) c;
  }

  <T extends Character> void unboxAndWidenGeneric(T c) {
    @SignedPositive long l = (long) c;
  }

  <T extends Character & Comparable<Character>> void unboxAndWidenIntersection(T c) {
    @SignedPositive long l = (long) c;
  }
}
