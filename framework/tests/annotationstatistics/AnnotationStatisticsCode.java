import java.util.List;

// AnnotationStatistics issues no diagnostics.  This test checks that it does not crash.
public class AnnotationStatisticsCode {

  @Deprecated int f;

  @SuppressWarnings("unchecked")
  List<String> m(Object o) {
    return (List<String>) o;
  }
}
