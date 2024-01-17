import java.util.Formattable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.checkerframework.common.value.qual.ArrayLen;

public class IndexIssue6046 {

  public interface Record extends Comparable<Record>, Formattable {}

  public interface Result<R extends Record> extends List<R>, Formattable {}

  @SuppressWarnings("unchecked")
  public static <K, V extends Record, R extends Record>
      Collector<R, ?, Map<K, Result<V>>> intoResultGroups(
          Function<? super R, ? extends K> keyMapper) {

    return Collectors.groupingBy(
        keyMapper,
        LinkedHashMap::new,
        Collector.<R, Result<V>[], Result<V>>of(
            // :: error:  (array.access.unsafe.high.constant)
            () -> new Result[1], (x, r) -> {}, (r1, r2) -> r1, r -> r[0]));
  }

  @SuppressWarnings("unchecked")
  public static <K, V extends Record, R extends Record>
      Collector<R, ?, Map<K, Result<V>>> intoResultGroups2(
          Function<? super R, ? extends K> keyMapper) {

    return Collectors.groupingBy(
        keyMapper,
        LinkedHashMap::new,
        Collector.<R, Result<V> @ArrayLen(1) [], Result<V>>of(
            () -> new Result[1], (x, r) -> {}, (r1, r2) -> r1, r -> r[0]));
  }

  public static <R extends Record> Result<R> result(R record) {
    throw new RuntimeException();
  }
}
