package org.checkerframework.common.value;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.plumelib.util.CollectionsP;
import org.plumelib.util.RegexUtil;

/** The qualifier hierarchy for the Value type system. */
final class ValueQualifierHierarchy extends ElementQualifierHierarchy {

  // This shadows the same-named field in GenericAnnotatedTypeFactory, but has a more specific
  // type.
  /** The type factory to use. */
  @SuppressWarnings("HidingField")
  private final ValueAnnotatedTypeFactory atypeFactory;

  /**
   * Creates a ValueQualifierHierarchy from the given classes.
   *
   * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
   * @param atypeFactory the associated type factory
   */
  ValueQualifierHierarchy(
      Collection<Class<? extends Annotation>> qualifierClasses,
      ValueAnnotatedTypeFactory atypeFactory) {
    super(qualifierClasses, atypeFactory.getElementUtils(), atypeFactory);
    this.atypeFactory = atypeFactory;
  }

  /**
   * Computes greatest lower bound of a @StringVal annotation with another Value Checker annotation.
   *
   * @param stringValAnno annotation of type @StringVal
   * @param otherAnno annotation from the value checker hierarchy
   * @return greatest lower bound of {@code stringValAnno} and {@code otherAnno}
   */
  private AnnotationMirror glbOfStringVal(
      AnnotationMirror stringValAnno, AnnotationMirror otherAnno) {
    List<String> values = atypeFactory.getStringValues(stringValAnno);
    switch (AnnotationUtils.annotationName(otherAnno)) {
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME -> {
        // Intersection of value lists
        List<String> otherValues = atypeFactory.getStringValues(otherAnno);
        values.retainAll(otherValues);
      }
      case ValueAnnotatedTypeFactory.ARRAYLEN_NAME -> {
        // Retain strings of correct lengths
        List<Integer> otherLengths = atypeFactory.getArrayLength(otherAnno);
        ArrayList<String> result = new ArrayList<>(values.size());
        for (String s : values) {
          if (otherLengths.contains(s.length())) {
            result.add(s);
          }
        }
        values = result;
      }
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME -> {
        // Retain strings of lengths from a range
        Range otherRange = atypeFactory.getRange(otherAnno);
        ArrayList<String> range = new ArrayList<>(values.size());
        for (String s : values) {
          if (otherRange.contains(s.length())) {
            range.add(s);
          }
        }
        values = range;
      }
      case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME -> {
        List<@Regex String> matchesRegexes =
            AnnotationUtils.getElementValueArray(
                otherAnno, atypeFactory.matchesRegexValueElement, String.class);
        // Retain the @StringVal values such that one of the regexes matches it.
        values = RegexUtil.matchesSomeRegex(values, matchesRegexes);
      }
      case ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME -> {
        List<@Regex String> doesNotMatchRegexes =
            AnnotationUtils.getElementValueArray(
                otherAnno, atypeFactory.doesNotMatchRegexValueElement, String.class);
        // Retain the @StringVal values such that none of the regexes matches it.
        values = RegexUtil.matchesNoRegex(values, doesNotMatchRegexes);
      }
      default -> {
        return atypeFactory.BOTTOMVAL;
      }
    }

    return atypeFactory.createStringAnnotation(values);
  }

  @Override
  public AnnotationMirror greatestLowerBoundQualifiers(AnnotationMirror a1, AnnotationMirror a2) {
    // Converting and computing the qualifier names once and passing them to both
    // isSubtypeQualifiers() calls below avoids redoing that work in each direction. (`a1` and
    // `a2` themselves are intentionally left unconverted, since this method returns one of them
    // verbatim below.)
    AnnotationMirror converted1 = atypeFactory.convertSpecialIntRangeToStandardIntRange(a1);
    AnnotationMirror converted2 = atypeFactory.convertSpecialIntRangeToStandardIntRange(a2);
    String qualName1 = AnnotationUtils.annotationName(converted1);
    String qualName2 = AnnotationUtils.annotationName(converted2);

    if (isSubtypeQualifiers(converted1, qualName1, converted2, qualName2)) {
      return a1;
    } else if (isSubtypeQualifiers(converted2, qualName2, converted1, qualName1)) {
      return a2;
    } else {

      // Implementation of GLB where one of the annotations is StringVal is needed for
      // length-based refinement of constant string values. Other cases of length-based
      // refinement are handled by subtype check.
      if (qualName1.equals(ValueAnnotatedTypeFactory.STRINGVAL_NAME)) {
        return glbOfStringVal(a1, a2);
      } else if (qualName2.equals(ValueAnnotatedTypeFactory.STRINGVAL_NAME)) {
        return glbOfStringVal(a2, a1);
      }

      // Simply return BOTTOMVAL in other cases. Refine this if we discover use cases
      // that need a more precise GLB.
      return atypeFactory.BOTTOMVAL;
    }
  }

  @Override
  public int numberOfIterationsBeforeWidening() {
    return ValueAnnotatedTypeFactory.MAX_VALUES + 1;
  }

  @Override
  public AnnotationMirror widenedUpperBound(
      AnnotationMirror newQualifier, AnnotationMirror previousQualifier) {
    AnnotationMirror lub = leastUpperBoundQualifiers(newQualifier, previousQualifier);
    if (AnnotationUtils.areSameByName(lub, ValueAnnotatedTypeFactory.INTRANGE_NAME)) {
      Range lubRange = atypeFactory.getRange(lub);
      Range newRange = atypeFactory.getRange(newQualifier);
      Range oldRange = atypeFactory.getRange(previousQualifier);
      Range wubRange = widenedRange(newRange, oldRange, lubRange);
      return atypeFactory.createIntRangeAnnotation(wubRange);
    } else if (AnnotationUtils.areSameByName(lub, ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME)) {
      Range lubRange = atypeFactory.getRange(lub);
      Range newRange = atypeFactory.getRange(newQualifier);
      Range oldRange = atypeFactory.getRange(previousQualifier);
      Range wubRange = widenedRange(newRange, oldRange, lubRange);
      return atypeFactory.createArrayLenRangeAnnotation(wubRange);
    } else {
      return lub;
    }
  }

  /** Possible values for the bound of a widened range. */
  private static final TreeSet<Long> wideningValues = new TreeSet<>();

  static {
    for (long i :
        new long[] {
          // Wraparound for long values is deduplicated by TreeSet.
          Long.MIN_VALUE,
          Long.MAX_VALUE,
          Integer.MIN_VALUE,
          Integer.MAX_VALUE,
          Short.MIN_VALUE,
          Short.MAX_VALUE,
          Character.MIN_VALUE,
          Character.MAX_VALUE,
          Byte.MIN_VALUE,
          Byte.MAX_VALUE,
          0L
        }) {
      wideningValues.add(i - 2);
      wideningValues.add(i - 1);
      wideningValues.add(i);
      wideningValues.add(i + 1);
      wideningValues.add(i + 2);
    }
  }

  /**
   * Determine the widened range from other ranges.
   *
   * @param newRange the new range
   * @param oldRange the old range
   * @param lubRange the LUB range
   * @return the widened range
   */
  private Range widenedRange(Range newRange, Range oldRange, Range lubRange) {
    if (newRange == null || oldRange == null || lubRange.equals(oldRange)) {
      return lubRange;
    }

    long min;
    // Is the lower bound decreasing?
    if (newRange.from < oldRange.from) {
      // Non-null because wideningValues contains Long.MIN_VALUE.
      @NonNull Long floor = wideningValues.floor(newRange.from);
      min = floor;
    } else {
      min = oldRange.from;
    }

    long max;
    // Is the upper bound increasing?
    if (newRange.to > oldRange.to) {
      // Non-null because wideningValues contains Long.MAX_VALUE.
      @NonNull Long ceiling = wideningValues.ceiling(newRange.to);
      max = ceiling;
    } else {
      max = oldRange.to;
    }

    Range result = Range.create(min, max);
    return result;
  }

  /**
   * Determines the least upper bound of a1 and a2, which contains the union of their sets of
   * possible values.
   *
   * @return the least upper bound of a1 and a2
   */
  @Override
  @SuppressWarnings({
    "regex:assignment", // getMatchesRegexValues/getDoesNotMatchRegexValues return valid @Regex
    // strings
    "regex:type.arguments.not.inferred" // AnnotationUtils.getElementValueArray contains @Regex
    // strings
  })
  public @Nullable AnnotationMirror leastUpperBoundQualifiers(
      AnnotationMirror a1, AnnotationMirror a2) {
    if (!AnnotationUtils.areSameByName(getTopAnnotation(a1), getTopAnnotation(a2))) {
      // The annotations are in different hierarchies
      return null;
    }

    a1 = atypeFactory.convertSpecialIntRangeToStandardIntRange(a1);
    a2 = atypeFactory.convertSpecialIntRangeToStandardIntRange(a2);
    // Computing the qualifier names once and passing them to both isSubtypeQualifiers() calls
    // below (instead of calling the 2-argument overload, which would recompute them and redo the
    // conversion above) avoids redoing that work in each direction.
    String qualName1 = AnnotationUtils.annotationName(a1);
    String qualName2 = AnnotationUtils.annotationName(a2);

    if (qualName1.equals(qualName2)) {
      // For most same-named kinds, the LUB can be computed directly from each side's
      // value/range without calling isSubtypeQualifiers(): extract each side's value/range once
      // and reuse that same extraction both to detect that one is a subtype of the other (in
      // which case return that annotation unchanged, exactly as the isSubtypeQualifiers() calls
      // below would have) and, if not, to compute the union. This avoids extracting each side's
      // value/range three times (once in each direction of isSubtypeQualifiers, and again for
      // the union below). Returns null, falling back to the general subtype-based computation
      // below, for the one same-named kind (@DoesNotMatchRegex) this doesn't handle.
      AnnotationMirror merged = mergeSameKind(qualName1, a1, a2);
      if (merged != null) {
        return merged;
      }
    }

    if (isSubtypeQualifiers(a1, qualName1, a2, qualName2)) {
      return a2;
    } else if (isSubtypeQualifiers(a2, qualName2, a1, qualName1)) {
      return a1;
    }

    if (qualName1.equals(qualName2)) {
      // mergeSameKind() above handles every same-named kind except the two below and the
      // "top"/"bottom"/"poly" qualifiers (which the isSubtypeQualifiers() calls above already
      // resolved).
      switch (qualName1) {
        case ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME -> {
          // @DoesNotMatchRegex's subtyping direction is reversed from the other kinds (a
          // *smaller* set of excluded regexes is the supertype), so it can't use the
          // superset-check short-circuit that mergeBySupersetCheck() relies on.
          // The LUB is the intersection of the sets.
          List<@Regex String> regexes1 = atypeFactory.getDoesNotMatchRegexValues(a1);
          List<@Regex String> regexes2 = atypeFactory.getDoesNotMatchRegexValues(a2);
          regexes1.retainAll(regexes2);
          return atypeFactory.createDoesNotMatchRegexAnnotation(regexes1);
        }
        case ValueAnnotatedTypeFactory.BOOLVAL_NAME -> {
          // Unlike the other getXValues() methods, getBooleanValues() returns null (meaning
          // "top") for a well-formed annotation whose own value list already contains both
          // true and false, not only for a null argument -- so it can't be called unconditionally
          // up front the way mergeBySupersetCheck() does. That case isn't reachable here: the
          // isSubtypeQualifiers() calls above extract raw, non-collapsed value lists, so a side
          // whose own list already contains both booleans trivially contains the other side's
          // values, and isSubtypeQualifiers() already returned above in that case.
          List<Boolean> bools = atypeFactory.getBooleanValues(a1);
          CollectionsP.adjoinAll(bools, atypeFactory.getBooleanValues(a2));
          return atypeFactory.createBooleanAnnotation(bools);
        }
        default -> throw new TypeSystemError("default case: %s %s %s%n", qualName1, a1, a2);
      }
    }

    // Special handling for dealing with the lub of two annotations that are distinct but
    // convertible (e.g. a StringVal and a MatchesRegex, or an IntVal and an IntRange).
    // Each of these variables is an annotation of the given type, or is null if neither of
    // the arguments to leastUpperBound is of the given types.
    AnnotationMirror arrayLenAnno = null;
    AnnotationMirror arrayLenRangeAnno = null;
    AnnotationMirror stringValAnno = null;
    AnnotationMirror matchesRegexAnno = null;
    AnnotationMirror doesNotMatchRegexAnno = null;
    AnnotationMirror intValAnno = null;
    AnnotationMirror intRangeAnno = null;
    AnnotationMirror doubleValAnno = null;

    switch (qualName1) {
      case ValueAnnotatedTypeFactory.ARRAYLEN_NAME -> arrayLenAnno = a1;
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME -> arrayLenRangeAnno = a1;
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME -> stringValAnno = a1;
      case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME -> matchesRegexAnno = a1;
      case ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME -> doesNotMatchRegexAnno = a1;
      case ValueAnnotatedTypeFactory.INTVAL_NAME -> intValAnno = a1;
      case ValueAnnotatedTypeFactory.INTRANGE_NAME -> intRangeAnno = a1;
      case ValueAnnotatedTypeFactory.DOUBLEVAL_NAME -> doubleValAnno = a1;
      default -> {} // Do nothing
    }

    switch (qualName2) {
      case ValueAnnotatedTypeFactory.ARRAYLEN_NAME -> arrayLenAnno = a2;
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME -> arrayLenRangeAnno = a2;
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME -> stringValAnno = a2;
      case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME -> matchesRegexAnno = a2;
      case ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME -> doesNotMatchRegexAnno = a2;
      case ValueAnnotatedTypeFactory.INTVAL_NAME -> intValAnno = a2;
      case ValueAnnotatedTypeFactory.INTRANGE_NAME -> intRangeAnno = a2;
      case ValueAnnotatedTypeFactory.DOUBLEVAL_NAME -> doubleValAnno = a2;
      default -> {} // Do nothing
    }

    // Special handling for dealing with the lub of an ArrayLenRange and an ArrayLen,
    // a StringVal with one of them, or a StringVal and a MatchesRegex.
    // Each of these converts one annotation to the other, then makes a recursive call.
    if (arrayLenAnno != null && arrayLenRangeAnno != null) {
      return leastUpperBoundQualifiers(
          arrayLenRangeAnno, atypeFactory.convertArrayLenToArrayLenRange(arrayLenAnno));
    } else if (stringValAnno != null && arrayLenAnno != null) {
      return leastUpperBoundQualifiers(
          arrayLenAnno, atypeFactory.convertStringValToArrayLen(stringValAnno));
    } else if (stringValAnno != null && arrayLenRangeAnno != null) {
      return leastUpperBoundQualifiers(
          arrayLenRangeAnno, atypeFactory.convertStringValToArrayLenRange(stringValAnno));
    } else if (stringValAnno != null && matchesRegexAnno != null) {
      return leastUpperBoundQualifiers(
          matchesRegexAnno, atypeFactory.convertStringValToMatchesRegex(stringValAnno));
    }

    if (stringValAnno != null && doesNotMatchRegexAnno != null) {
      // The lub is either doesNotMatchRegexAnno or UNKNOWNVAL.
      List<String> stringVals = atypeFactory.getStringValues(stringValAnno);
      List<@Regex String> regexes =
          AnnotationUtils.getElementValueArray(
              doesNotMatchRegexAnno, atypeFactory.doesNotMatchRegexValueElement, String.class);
      if (RegexUtil.everyStringMatchesSomeRegex(stringVals, regexes)) {
        return atypeFactory.UNKNOWNVAL;
      }
      return doesNotMatchRegexAnno;
    }

    // Annotations are both in the same hierarchy, but they are not the same.
    // If a1 and a2 are not the same type of *Value annotation, they may still be mergeable
    // because some values can be implicitly cast as others. For example, if a1 and a2 are
    // both in {DoubleVal, IntVal} then they will be converted upwards: IntVal -> DoubleVal
    // to arrive at a common annotation type.

    if (doubleValAnno != null) {
      if (intRangeAnno != null) {
        intValAnno = atypeFactory.convertIntRangeToIntVal(intRangeAnno);
        if (AnnotationUtils.areSameByName(intValAnno, ValueAnnotatedTypeFactory.UNKNOWN_NAME)) {
          intValAnno = null;
        }
      }
      if (intValAnno != null) {
        // Convert intValAnno to a @DoubleVal AnnotationMirror
        AnnotationMirror doubleValAnno2 = atypeFactory.convertIntValToDoubleVal(intValAnno);
        return leastUpperBoundQualifiers(doubleValAnno, doubleValAnno2);
      }
      return atypeFactory.UNKNOWNVAL;
    }
    if (intRangeAnno != null && intValAnno != null) {
      // Convert intValAnno to an @IntRange AnnotationMirror
      AnnotationMirror intRangeAnno2 = atypeFactory.convertIntValToIntRange(intValAnno);
      return leastUpperBoundQualifiers(intRangeAnno, intRangeAnno2);
    }

    // In all other cases, the LUB is UnknownVal.
    return atypeFactory.UNKNOWNVAL;
  }

  /**
   * Computes the least upper bound of {@code a1} and {@code a2}, which have the same qualifier name
   * {@code qualName}, without calling {@link #isSubtypeQualifiers}. Used by {@link
   * #leastUpperBoundQualifiers} to avoid extracting each side's value/range multiple times.
   *
   * @param qualName the qualifier name of both {@code a1} and {@code a2}
   * @param a1 an annotation named {@code qualName}
   * @param a2 an annotation named {@code qualName}
   * @return the least upper bound of {@code a1} and {@code a2}, or null if {@code qualName} is a
   *     kind not handled by this method (in which case the caller should fall back to the general,
   *     isSubtypeQualifiers()-based computation)
   */
  @SuppressWarnings({
    "regex:assignment", // getMatchesRegexValues returns valid @Regex strings
    "regex:argument"
  })
  private @Nullable AnnotationMirror mergeSameKind(
      String qualName, AnnotationMirror a1, AnnotationMirror a2) {
    switch (qualName) {
      case ValueAnnotatedTypeFactory.INTRANGE_NAME -> {
        return mergeRanges(
            a1,
            atypeFactory.getRange(a1),
            a2,
            atypeFactory.getRange(a2),
            atypeFactory::createIntRangeAnnotation);
      }
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME -> {
        return mergeRanges(
            a1,
            atypeFactory.getRange(a1),
            a2,
            atypeFactory.getRange(a2),
            atypeFactory::createArrayLenRangeAnnotation);
      }
      case ValueAnnotatedTypeFactory.INTVAL_NAME -> {
        return mergeBySupersetCheck(
            a1,
            atypeFactory.getIntValues(a1),
            a2,
            atypeFactory.getIntValues(a2),
            atypeFactory::createIntValAnnotation);
      }
      case ValueAnnotatedTypeFactory.ARRAYLEN_NAME -> {
        return mergeBySupersetCheck(
            a1,
            atypeFactory.getArrayLength(a1),
            a2,
            atypeFactory.getArrayLength(a2),
            atypeFactory::createArrayLenAnnotation);
      }
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME -> {
        return mergeBySupersetCheck(
            a1,
            atypeFactory.getStringValues(a1),
            a2,
            atypeFactory.getStringValues(a2),
            atypeFactory::createStringAnnotation);
      }
      case ValueAnnotatedTypeFactory.DOUBLEVAL_NAME -> {
        return mergeBySupersetCheck(
            a1,
            atypeFactory.getDoubleValues(a1),
            a2,
            atypeFactory.getDoubleValues(a2),
            atypeFactory::createDoubleAnnotation);
      }
      case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME -> {
        return mergeBySupersetCheck(
            a1,
            atypeFactory.getMatchesRegexValues(a1),
            a2,
            atypeFactory.getMatchesRegexValues(a2),
            atypeFactory::createMatchesRegexAnnotation);
      }
      default -> {
        return null;
      }
    }
  }

  /**
   * If {@code values2} contains every element of {@code values1}, returns {@code a2} (unchanged);
   * else if {@code values1} contains every element of {@code values2}, returns {@code a1}
   * (unchanged); otherwise returns the annotation, built by {@code createAnnotation}, for the union
   * of {@code values1} and {@code values2}.
   *
   * @param <T> the type of the elements
   * @param a1 an annotation whose value element is {@code values1}
   * @param values1 {@code a1}'s values; this list is side-effected (values from {@code values2} may
   *     be added to it)
   * @param a2 an annotation whose value element is {@code values2}
   * @param values2 {@code a2}'s values
   * @param createAnnotation builds an annotation from a list of values
   * @return the least upper bound of {@code a1} and {@code a2}
   */
  private <T> AnnotationMirror mergeBySupersetCheck(
      AnnotationMirror a1,
      List<T> values1,
      AnnotationMirror a2,
      List<T> values2,
      Function<List<T>, AnnotationMirror> createAnnotation) {
    if (values2.containsAll(values1)) {
      return a2;
    } else if (values1.containsAll(values2)) {
      return a1;
    }
    CollectionsP.adjoinAll(values1, values2);
    return createAnnotation.apply(values1);
  }

  /**
   * If {@code range2} contains {@code range1}, returns {@code a2} (unchanged); else if {@code
   * range1} contains {@code range2}, returns {@code a1} (unchanged); otherwise returns the
   * annotation, built by {@code createAnnotation}, for the union of {@code range1} and {@code
   * range2}.
   *
   * @param a1 an annotation whose range is {@code range1}
   * @param range1 {@code a1}'s range
   * @param a2 an annotation whose range is {@code range2}
   * @param range2 {@code a2}'s range
   * @param createAnnotation builds an annotation from a range
   * @return the least upper bound of {@code a1} and {@code a2}
   */
  private AnnotationMirror mergeRanges(
      AnnotationMirror a1,
      Range range1,
      AnnotationMirror a2,
      Range range2,
      Function<Range, AnnotationMirror> createAnnotation) {
    if (range2.contains(range1)) {
      return a2;
    } else if (range1.contains(range2)) {
      return a1;
    }
    return createAnnotation.apply(range1.union(range2));
  }

  @Override
  public boolean isSubtypeShallow(
      AnnotationMirror subQualifier,
      TypeMirror subType,
      AnnotationMirror superQualifier,
      TypeMirror superType) {
    subQualifier = atypeFactory.convertSpecialIntRangeToStandardIntRange(subQualifier, subType);
    superQualifier =
        atypeFactory.convertSpecialIntRangeToStandardIntRange(superQualifier, superType);
    return super.isSubtypeShallow(subQualifier, subType, superQualifier, superType);
  }

  @Override
  public @Nullable AnnotationMirror leastUpperBoundShallow(
      AnnotationMirror qualifier1, TypeMirror tm1, AnnotationMirror qualifier2, TypeMirror tm2) {
    qualifier1 = atypeFactory.convertSpecialIntRangeToStandardIntRange(qualifier1, tm1);
    qualifier2 = atypeFactory.convertSpecialIntRangeToStandardIntRange(qualifier2, tm2);
    return super.leastUpperBoundShallow(qualifier1, tm1, qualifier2, tm2);
  }

  /**
   * Computes subtyping as per the subtyping in the qualifier hierarchy structure unless both
   * annotations are Value. In this case, subAnno is a subtype of superAnno iff superAnno contains
   * at least every element of subAnno.
   *
   * @return true if subAnno is a subtype of superAnno, false otherwise
   */
  @Override
  public boolean isSubtypeQualifiers(AnnotationMirror subAnno, AnnotationMirror superAnno) {
    subAnno = atypeFactory.convertSpecialIntRangeToStandardIntRange(subAnno);
    superAnno = atypeFactory.convertSpecialIntRangeToStandardIntRange(superAnno);
    return isSubtypeQualifiers(
        subAnno,
        AnnotationUtils.annotationName(subAnno),
        superAnno,
        AnnotationUtils.annotationName(superAnno));
  }

  /**
   * Same as {@link #isSubtypeQualifiers(AnnotationMirror, AnnotationMirror)}, but the caller has
   * already converted {@code subAnno} and {@code superAnno} via {@link
   * ValueAnnotatedTypeFactory#convertSpecialIntRangeToStandardIntRange(AnnotationMirror)} and
   * computed their qualifier names. {@link #leastUpperBoundQualifiers} and {@link
   * #greatestLowerBoundQualifiers} each need the result of {@code isSubtypeQualifiers} in both
   * directions, so calling this overload instead of the public one avoids doing that conversion and
   * name computation twice.
   *
   * @param subAnno a converted annotation mirror
   * @param subQualName the qualifier name of {@code subAnno}
   * @param superAnno a converted annotation mirror
   * @param superQualName the qualifier name of {@code superAnno}
   * @return true if subAnno is a subtype of superAnno, false otherwise
   */
  @SuppressWarnings(
      "regex:argument") // AnnotationUtils.getElementValueArray returns @Regex strings from regex
  // annotations
  private boolean isSubtypeQualifiers(
      AnnotationMirror subAnno,
      String subQualName,
      AnnotationMirror superAnno,
      String superQualName) {
    if (subQualName.equals(ValueAnnotatedTypeFactory.UNKNOWN_NAME)) {
      superAnno = atypeFactory.convertToUnknown(superAnno);
      superQualName = AnnotationUtils.annotationName(superAnno);
    }
    if (superQualName.equals(ValueAnnotatedTypeFactory.UNKNOWN_NAME)
        || subQualName.equals(ValueAnnotatedTypeFactory.BOTTOMVAL_NAME)) {
      return true;
    } else if (superQualName.equals(ValueAnnotatedTypeFactory.BOTTOMVAL_NAME)
        || subQualName.equals(ValueAnnotatedTypeFactory.UNKNOWN_NAME)) {
      return false;
    } else if (superQualName.equals(ValueAnnotatedTypeFactory.POLY_NAME)) {
      return subQualName.equals(ValueAnnotatedTypeFactory.POLY_NAME);
    } else if (subQualName.equals(ValueAnnotatedTypeFactory.POLY_NAME)) {
      return false;
    } else if (superQualName.equals(subQualName)) {
      // Same annotation name, so might be subtype
      if (subQualName.equals(ValueAnnotatedTypeFactory.INTRANGE_NAME)
          || subQualName.equals(ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME)) {
        // Special case for range-based annotations
        Range superRange = atypeFactory.getRange(superAnno);
        Range subRange = atypeFactory.getRange(subAnno);
        return superRange.contains(subRange);
      } else if (subQualName.equals(ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME)) {
        List<String> superValues =
            AnnotationUtils.getElementValueArray(
                superAnno, atypeFactory.doesNotMatchRegexValueElement, String.class);
        List<String> subValues =
            AnnotationUtils.getElementValueArray(
                subAnno, atypeFactory.doesNotMatchRegexValueElement, String.class);
        return subValues.containsAll(superValues);
      } else {
        // The annotations have the same name, which is one of:
        // ArrayLen, BoolVal, DoubleVal, EnumVal, StringVal, MatchesRegex.
        @SuppressWarnings("deprecation") // concrete annotation class is not known
        List<Object> superValues =
            AnnotationUtils.getElementValueArray(superAnno, "value", Object.class, false);
        @SuppressWarnings("deprecation") // concrete annotation class is not known
        List<Object> subValues =
            AnnotationUtils.getElementValueArray(subAnno, "value", Object.class, false);
        return superValues.containsAll(subValues);
      }
    }
    switch (subQualName + superQualName) {
      case ValueAnnotatedTypeFactory.INTVAL_NAME + ValueAnnotatedTypeFactory.DOUBLEVAL_NAME -> {
        List<Double> superValues = atypeFactory.getDoubleValues(superAnno);
        List<Double> subValues =
            atypeFactory.convertLongListToDoubleList(atypeFactory.getIntValues(subAnno));
        return superValues.containsAll(subValues);
      }
      case ValueAnnotatedTypeFactory.INTVAL_NAME + ValueAnnotatedTypeFactory.INTRANGE_NAME,
          ValueAnnotatedTypeFactory.ARRAYLEN_NAME
              + ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME -> {
        Range superRange = atypeFactory.getRange(superAnno);
        List<Long> subLongValues = atypeFactory.getArrayLenOrIntValue(subAnno);
        Range subLongRange = Range.create(subLongValues);
        return superRange.contains(subLongRange);
      }
      case ValueAnnotatedTypeFactory.INTRANGE_NAME + ValueAnnotatedTypeFactory.DOUBLEVAL_NAME -> {
        Range subRange = atypeFactory.getRange(subAnno);
        if (subRange.isWiderThan(ValueAnnotatedTypeFactory.MAX_VALUES)) {
          return false;
        }
        List<Double> superDoubleValues = atypeFactory.getDoubleValues(superAnno);
        List<Double> subDoubleValues = ValueCheckerUtils.getValuesFromRange(subRange, Double.class);
        return superDoubleValues.containsAll(subDoubleValues);
      }
      case ValueAnnotatedTypeFactory.INTRANGE_NAME + ValueAnnotatedTypeFactory.INTVAL_NAME,
          ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME
              + ValueAnnotatedTypeFactory.ARRAYLEN_NAME -> {
        Range subRange2 = atypeFactory.getRange(subAnno);
        if (subRange2.isWiderThan(ValueAnnotatedTypeFactory.MAX_VALUES)) {
          return false;
        }
        List<Long> superValues2 = atypeFactory.getArrayLenOrIntValue(superAnno);
        List<Long> subValues2 = ValueCheckerUtils.getValuesFromRange(subRange2, Long.class);
        return superValues2.containsAll(subValues2);
      }
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME + ValueAnnotatedTypeFactory.STRINGVAL_NAME,
          ValueAnnotatedTypeFactory.ARRAYLEN_NAME + ValueAnnotatedTypeFactory.STRINGVAL_NAME -> {
        // Allow @ArrayLen(0) to be converted to @StringVal("")
        List<String> superStringValues = atypeFactory.getStringValues(superAnno);
        return superStringValues.contains("") && atypeFactory.getMaxLenValue(subAnno) == 0;
      }
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME
              + ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME -> {
        List<String> strings = atypeFactory.getStringValues(subAnno);
        List<String> regexes =
            AnnotationUtils.getElementValueArray(
                superAnno, atypeFactory.matchesRegexValueElement, String.class);
        return RegexUtil.everyStringMatchesSomeRegex(strings, regexes);
      }
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME
              + ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME -> {
        List<String> strings = atypeFactory.getStringValues(subAnno);
        List<String> regexes =
            AnnotationUtils.getElementValueArray(
                superAnno, atypeFactory.doesNotMatchRegexValueElement, String.class);
        return RegexUtil.noStringMatchesAnyRegex(strings, regexes);
      }
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME + ValueAnnotatedTypeFactory.ARRAYLEN_NAME -> {
        // StringVal is a subtype of ArrayLen, if all the strings have one of the correct
        // lengths.
        List<Integer> superIntValues = atypeFactory.getArrayLength(superAnno);
        List<String> subStringValues = atypeFactory.getStringValues(subAnno);
        for (String value : subStringValues) {
          if (!superIntValues.contains(value.length())) {
            return false;
          }
        }
        return true;
      }
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME
              + ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME -> {
        // StringVal is a subtype of ArrayLenRange, if all the strings have a length in the
        // range.
        Range superRange2 = atypeFactory.getRange(superAnno);
        List<String> subValues3 = atypeFactory.getStringValues(subAnno);
        for (String value : subValues3) {
          if (!superRange2.contains(value.length())) {
            return false;
          }
        }
        return true;
      }
      default -> {
        return false;
      }
    }
  }
}
