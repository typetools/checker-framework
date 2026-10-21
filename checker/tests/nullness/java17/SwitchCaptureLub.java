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
        switch (i) {
          case 2 -> c1;
          default -> c2;
        });
  }

  <T> void conditionalArms(
      boolean b,
      Comparator<List<T>> c1,
      Comparator<Object> c2,
      List<? super Comparator<? super List<T>>> sink) {
    sink.add(b ? c1 : c2);
  }
}
