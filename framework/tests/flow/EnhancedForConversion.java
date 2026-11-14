import java.util.List;
import org.checkerframework.framework.testchecker.util.*;

// The loop variable gets the element type of the iterable expression, even when assignment
// conversion unboxes or widens the element.
public class EnhancedForConversion {
  @Odd Integer[] field;

  @Odd Integer[] method() {
    throw new Error();
  }

  void unboxList(List<@Odd Integer> list) {
    for (int x : list) {
      @Odd int y = x;
    }
  }

  void unboxAndWidenList(List<@Odd Integer> list) {
    for (long x : list) {
      @Odd long y = x;
    }
  }

  void unboxWildcardList(List<? extends @Odd Integer> list) {
    for (int x : list) {
      @Odd int y = x;
    }
  }

  void unboxAndWidenWildcardList(List<? extends @Odd Integer> list) {
    for (long x : list) {
      @Odd long y = x;
    }
  }

  <T extends @Odd Integer> void unboxTypeVariableList(List<T> list) {
    for (int x : list) {
      @Odd int y = x;
    }
  }

  void unboxArray(@Odd Integer[] array) {
    for (int x : array) {
      @Odd int y = x;
    }
  }

  void unboxAndWidenArray(@Odd Integer[] array) {
    for (long x : array) {
      @Odd long y = x;
    }
  }

  void widenArray(@Odd int[] array) {
    for (long x : array) {
      @Odd long y = x;
    }
  }

  void unboxArrayField() {
    for (int x : field) {
      @Odd int y = x;
    }
    for (int x : this.field) {
      @Odd int y = x;
    }
  }

  void unboxArrayMethod() {
    for (int x : method()) {
      @Odd int y = x;
    }
  }
}
