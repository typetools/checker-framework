import org.checkerframework.checker.fenum.qual.Fenum;

// Boxing and unboxing keep the fenum qualifier of the converted value, even though the declared
// return types of intValue() and valueOf() are @FenumUnqualified.
public class BoxingKeepsFenum {
  void unboxArrayElement(@Fenum("A") Integer[] a) {
    for (int x : a) {
      @Fenum("A") int y = x;
    }
    int q = a[0];
    @Fenum("A") int r = q;
  }

  void unbox(@Fenum("A") Integer i) {
    int j = i;
    @Fenum("A") int k = j;
  }

  void box(@Fenum("A") int i) {
    Integer b = i;
    @Fenum("A") Integer c = b;
  }
}
