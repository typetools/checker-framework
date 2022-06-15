import org.checkerframework.checker.signedness.qual.Unsigned;

public class CharComparisons {
  char c;
  @Unsigned byte b;

  void unsignedComparison(char c, @Unsigned byte b) {
    // :: error: (comparison.unsignedrhs)
    boolean res = c > b;
    // :: error: (comparison.unsignedrhs)
    res = c >= b;
    // :: error: (comparison.unsignedrhs)
    res = c < b;
    // :: error: (comparison.unsignedrhs)
    res = c <= b;
    res = c == b;
  }

  void unsignedComparisonFields() {
    // :: error: (comparison.unsignedrhs)
    boolean res = this.c > this.b;
    // :: error: (comparison.unsignedrhs)
    res = this.c >= this.b;
    // :: error: (comparison.unsignedrhs)
    res = this.c < this.b;
    // :: error: (comparison.unsignedrhs)
    res = this.c <= this.b;
    res = this.c == this.b;
  }
}
