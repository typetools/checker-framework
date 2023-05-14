import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.SubstringIndexFor;

public class SubstringIndexForIrrelevant {

  @SuppressWarnings(
      "substringindex:return" // https://github.com/kelloggm/checker-framework/issues/206, 207, 208
  )
  public static @LTEqLengthOf("#1") @SubstringIndexFor(value = "#1", offset = "#2.length - 1") int
      indexOf(boolean[] array, boolean[] target) {
    return -1;
  }
}
