import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;

public class AndExample {

  @SuppressWarnings("index") // forward reference to field iYearInfoCache
  private static final @IndexOrHigh("iYearInfoCache") int CACHE_SIZE = 1 << 10;

  private static final @IndexFor("iYearInfoCache") int CACHE_MASK = CACHE_SIZE - 1;

  private static final String[] iYearInfoCache = new String[CACHE_SIZE];

  private String getYearInfo(int year) {
    return iYearInfoCache[year & CACHE_MASK];
  }
}
