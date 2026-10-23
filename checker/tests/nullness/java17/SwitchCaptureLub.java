// @below-java17-jdk-skip-test
// Test case for https://github.com/typetools/checker-framework/issues/6872

import java.util.Comparator;
import java.util.List;

public class SwitchCaptureLub {
  <T> void switchArms(
      int i,
      Comparator<List<T>> c1,
      Comparator<Object> c2,
      List<? super Comparator<? super List<T>>> sink) {
    sink.add(
        // TODO: This is a false positive.  The lub of the switch arms is a fresh capture of
        // ? super Comparator<? super List<T>>, rather than the capture that is the parameter type.
        // :: error: (argument)
        switch (i) {
          case 2 -> c1;
          default -> c2;
        });
  }
}
