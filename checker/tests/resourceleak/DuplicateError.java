import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

public class DuplicateError {
  void m(List<?> values) {
    @SuppressWarnings("lambda.param")
    List<String> stringVals = DECollectionsPlume.mapList((Object o) -> (String) o, values);
  }
}

class DECollectionsPlume {
  public static <
          @KeyForBottom FROM extends @Nullable @UnknownKeyFor Object,
          @KeyForBottom TO extends @Nullable @UnknownKeyFor Object>
      List<TO> mapList(Function<? super FROM, ? extends TO> f, Iterable<FROM> iterable) {
    return null;
  }
}
