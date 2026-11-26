import java.util.List;
import org.checkerframework.framework.testchecker.unboxingweakens.qual.UWStrong;

// An unboxing conversion changes @UWStrong to @UWTop, including the conversion of the element of
// an enhanced for loop.
public class EnhancedForUnboxingWeakens {
  void unboxing(@UWStrong Integer i) {
    int x = i;
    // :: error: (assignment)
    @UWStrong int y = x;
  }

  void enhancedForUnboxing(List<@UWStrong Integer> values) {
    for (int x : values) {
      // :: error: (assignment)
      @UWStrong int y = x;
    }
  }

  void enhancedForUnboxingArray(@UWStrong Integer[] values) {
    for (int x : values) {
      // :: error: (assignment)
      @UWStrong int y = x;
    }
  }

  <T extends @UWStrong Integer> void enhancedForUnboxingTypeVariable(List<T> values) {
    for (int x : values) {
      // :: error: (assignment)
      @UWStrong int y = x;
    }
  }

  void enhancedForNoUnboxing(List<@UWStrong Integer> values) {
    for (Integer x : values) {
      @UWStrong Integer y = x;
    }
  }
}
