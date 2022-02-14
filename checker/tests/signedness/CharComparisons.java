import org.checkerframework.checker.signedness.qual.Unsigned;

public class CharComparisons {
  char c;
  @Unsigned byte b;

  void unsignedComparison(char c, @Unsigned byte b) {
    // :: error: (comparison.unsignedlhs)
    boolean res = c > b;
    // :: error: (comparison.unsignedlhs)
    res = c >= b;
    // :: error: (comparison.unsignedlhs)
    res = c < b;
    // :: error: (comparison.unsignedlhs)
    res = c <= b;
    res = c == b;
  }

  void unsignedComparisonFields() {
    // :: error: (comparison.unsignedlhs)
    boolean res = this.c > this.b;
    // :: error: (comparison.unsignedlhs)
    res = this.c >= this.b;
    // :: error: (comparison.unsignedlhs)
    res = this.c < this.b;
    // :: error: (comparison.unsignedlhs)
    res = this.c <= this.b;
    res = this.c == this.b;
  }
}
