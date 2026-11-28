import java.util.Arrays;
import org.checkerframework.checker.index.qual.*;

public class BinarySearchTest {

  private final long @SameLen("iNameKeys") [] iTransitions;
  private final String @SameLen("iTransitions") [] iNameKeys;

  private BinarySearchTest(
      long @SameLen("iNameKeys") [] transitions, String @SameLen("iTransitions") [] nameKeys) {
    // The parameter types name the fields, whose values here are their default values, not the
    // values that this constructor assigns.  The types cannot name the parameters instead (as in
    // `@SameLen("#2")`), because then neither field could be assigned first.
    // :: error: [reassignment.invalidates.type]
    iTransitions = transitions;
    // TODO: `nameKeys` has the same length as `iTransitions`, so the declared type of
    // `transitions` still holds.
    // :: error: [reassignment.invalidates.type]
    iNameKeys = nameKeys;
  }

  public String getNameKey(long instant) {
    long[] transitions = iTransitions;
    int i = Arrays.binarySearch(transitions, instant);
    if (i >= 0) {
      return iNameKeys[i];
    }
    i = ~i;
    if (i > 0) {
      return iNameKeys[i - 1];
    }
    return "";
  }

  public String getNameKey2(long instant) {
    long[] transitions = iTransitions;
    int i = Arrays.binarySearch(transitions, instant);
    if (i >= 0) {
      return iNameKeys[i];
    }
    i = ~i;
    if (i < iNameKeys.length) {
      return iNameKeys[i];
    }
    return "";
  }
}
