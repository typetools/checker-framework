package org.checkerframework.common.value;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.StringsPlume;

/**
 * An abstraction that can be either a range or a list of values that could come from an {@link
 * ArrayLen} or {@link IntVal}. This abstraction reduces the number of cases that {@link
 * ValueTreeAnnotator#handleInitializers(List, AnnotatedTypeMirror.AnnotatedArrayType)} and {@link
 * ValueTreeAnnotator#handleDimensions(List, AnnotatedTypeMirror.AnnotatedArrayType)} must handle.
 *
 * <p>Tracks Ints in the list, and creates ArrayLen or ArrayLenRange annotations, because it's meant
 * to be used to reason about ArrayLen and ArrayLenRange values.
 */
class RangeOrListOfValues {
  private Range range;
  private List<Integer> values;
  private boolean isRange;

  public RangeOrListOfValues(List<Integer> values) {
    this.values = new ArrayList<>();
    isRange = false;
    addAll(values);
  }

  public RangeOrListOfValues(Range range) {
    this.range = range;
    isRange = true;
  }

  public void add(Range otherRange) {
    if (isRange) {
      range = range.union(otherRange);
    } else {
      convertToRange();
      add(otherRange);
    }
  }

  /**
   * If this is not a range, adds all members of newValues to the list. Otherwise, extends the range
   * as appropriate based on the max and min of newValues. If adding newValues to a non-range would
   * cause the list to become too large, converts this into a range.
   *
   * <p>If reading from an {@link org.checkerframework.common.value.qual.IntRange} annotation,
   * {@link #convertLongsToInts(List)} should be called before calling this method.
   *
   * @param newValues values to add
   */
  public void addAll(List<Integer> newValues) {
    if (isRange) {
      range = range.union(Range.create(newValues));
    } else {
      for (Integer i : newValues) {
        if (!values.contains(i)) {
          values.add(i);
        }
      }
      if (values.size() > ValueAnnotatedTypeFactory.MAX_VALUES) {
        convertToRange();
      }
    }
  }

  /**
   * Produces the most precise annotation that captures the information stored in this
   * RangeOrListofValues. The result is either a {@link ArrayLen} or a {@link ArrayLenRange}.
   *
   * @param atypeFactory the type factory
   * @return an annotation correspending to this RangeOrListofValues
   */
  public AnnotationMirror createAnnotation(ValueAnnotatedTypeFactory atypeFactory) {
    if (isRange) {
      return atypeFactory.createArrayLenRangeAnnotation(range);
    } else {
      return atypeFactory.createArrayLenAnnotation(values);
    }
  }

  /**
   * Converts a Long to an Integer by clipping it to the int range.
   *
   * @param l a Long integer
   * @return the value clipped to the Integer range
   */
  private static Integer convertLongToInt(Long l) {
    if (l > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else if (l < Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    } else {
      return l.intValue();
    }
  }

  /**
   * To be called before addAll. Converts Longs to Integers by clipping them to the int range; meant
   * to be used with ArrayLenRange (which only handles Ints).
   *
   * @param newValues a list of Long integers
   * @return a list of Integers
   */
  public static List<Integer> convertLongsToInts(List<Long> newValues) {
    return CollectionsPlume.mapList(RangeOrListOfValues::convertLongToInt, newValues);
  }

  /**
   * Transforms this into a range. Fails if there are no values in the list. Has no effect if this
   * is already a range.
   */
  public void convertToRange() {
    if (!isRange) {
      isRange = true;
      range = Range.create(values);
      values = null;
    }
  }

  @Override
  public String toString() {
    if (isRange) {
      return range.toString();
    } else {
      if (values.isEmpty()) {
        return "[]";
      }
      String res = "[";
      res += StringsPlume.join(", ", values);
      res += "]";
      return res;
    }
  }
}
