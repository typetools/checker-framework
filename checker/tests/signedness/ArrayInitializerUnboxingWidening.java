import org.checkerframework.checker.signedness.qual.SignedPositive;
import org.checkerframework.checker.signedness.qual.Unsigned;

// An array initializer that is unboxed to its own primitive type and then widened to the component
// type.  Widening a char to an int yields a @SignedPositive value.
public class ArrayInitializerUnboxingWidening {

  void unboxAndWiden(Character c) {
    @SignedPositive int[] fromBoxed = new int[] {c, c};
  }

  void unboxSameKind(Character c) {
    @Unsigned char[] chars = new char[] {c, c};
  }

  <T extends Character> void unboxAndWidenGeneric(T c) {
    @SignedPositive int[] fromBoxed = new int[] {c, c};
  }

  <T extends Character> void unboxSameKindGeneric(T c) {
    @Unsigned char[] chars = new char[] {c, c};
  }
}
