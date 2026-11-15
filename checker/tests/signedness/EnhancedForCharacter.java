import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.SignedPositive;

// Unboxing a Character and widening it to long yields a @SignedPositive value.
public class EnhancedForCharacter {

  void array(Character[] array) {
    for (long x : array) {
      @SignedPositive long y = x;
      @Signed long z = x;
    }
  }

  void plain(Character c) {
    long x = c;
    @SignedPositive long y = x;
  }
}
