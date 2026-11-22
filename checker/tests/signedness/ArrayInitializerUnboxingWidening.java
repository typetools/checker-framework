import org.checkerframework.checker.signedness.qual.SignedPositive;

// An array initializer that is unboxed to its own primitive type and then widened to the component
// type.  Widening a char to an int yields a @SignedPositive value.
public class ArrayInitializerUnboxingWidening {

  void unboxAndWiden(Character c) {
    @SignedPositive int[] fromBoxed = new int[] {c, c};
  }

  <T extends Character> void unboxAndWidenGeneric(T c) {
    @SignedPositive int[] fromBoxed = new int[] {c, c};
  }

  <T extends Character & Comparable<Character>> void unboxAndWidenIntersection(T c) {
    @SignedPositive int[] fromBoxed = new int[] {c, c};
  }
}
