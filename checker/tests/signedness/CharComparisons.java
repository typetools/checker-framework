import org.checkerframework.checker.signedness.qual.Unsigned;

public class CharComparisons {
  char c;
  @Unsigned byte b;

  void unsignedComparison(char c, @Unsigned byte b) {
    boolean res = c > b;
    res = c >= b;
    res = c < b;
    res = c <= b;
    res = c == b;
  }

  void unsignedComparisonFields() {
    boolean res = this.c > this.b;
    res = this.c >= this.b;
    res = this.c < this.b;
    res = this.c <= this.b;
    res = this.c == this.b;
  }
}
