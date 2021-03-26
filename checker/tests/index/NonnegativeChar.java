import java.util.ArrayList;
import org.checkerframework.checker.index.qual.LowerBoundBottom;
import org.checkerframework.checker.index.qual.PolyLowerBound;

public class NonnegativeChar {
  void foreach(char[] array) {
    for (char value : array) {}
  }

  char constant() {
    return Character.MAX_VALUE;
  }

  char conversion(int i) {
    return (char) i;
  }

  public void takeList(ArrayList<Character> z) {}

  public void passList() {
    takeList(new ArrayList<Character>());
  }

  static class CustomList extends ArrayList<Character> {}

  public void passCustomList() {
    takeList(new CustomList());
  }

  public @LowerBoundBottom char bottomLB(@LowerBoundBottom char c) {
    return c;
  }

  public @PolyLowerBound char polyLB(@PolyLowerBound char c) {
    return c;
  }
}
