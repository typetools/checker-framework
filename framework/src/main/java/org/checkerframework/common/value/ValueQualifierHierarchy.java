package org.checkerframework.common.value;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.plumelib.util.CollectionsPlume;
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
   * @param atypeFactory a ValueAnnotatedTypeFactory
   * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
   * @deprecated use {@link #ValueQualifierHierarchy(Collection, ValueAnnotatedTypeFactory)} which
   *     has the arguments in the other order
   */
  @Deprecated // 2023-05-23
  ValueQualifierHierarchy(
      ValueAnnotatedTypeFactory atypeFactory,
      Collection<Class<? extends Annotation>> qualifierClasses) {
    this(qualifierClasses, atypeFactory);
  }

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
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME:
        // Intersection of value lists
        List<String> otherValues = atypeFactory.getStringValues(otherAnno);
        values.retainAll(otherValues);
        break;
      case ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
        // Retain strings of correct lengths
        List<Integer> otherLengths = atypeFactory.getArrayLength(otherAnno);
        ArrayList<String> result = new ArrayList<>(values.size());
        for (String s : values) {
          if (otherLengths.contains(s.length())) {
            result.add(s);
          }
        }
        values = result;
        break;
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
        // Retain strings of lengths from a range
        Range otherRange = atypeFactory.getRange(otherAnno);
        ArrayList<String> range = new ArrayList<>(values.size());
        for (String s : values) {
          if (otherRange.contains(s.length())) {
            range.add(s);
          }
        }
        values = range;
        break;
      case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME:
        List<@Regex String> matchesRegexes =
            AnnotationUtils.getElementValueArray(
                otherAnno, atypeFactory.matchesRegexValueElement, String.class);
        // Retain the @StringVal values such that one of the regexes matches it.
        values = RegexUtil.matchesSomeRegex(values, matchesRegexes);
        break;
      case ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME:
        List<@Regex String> doesNotMatchRegexes =
            AnnotationUtils.getElementValueArray(
                otherAnno, atypeFactory.doesNotMatchRegexValueElement, String.class);
        // Retain the @StringVal values such that none of the regexes matches it.
        values = RegexUtil.matchesNoRegex(values, doesNotMatchRegexes);
        break;
      default:
        return atypeFactory.BOTTOMVAL;
    }

    return atypeFactory.createStringAnnotation(values);
  }

  @Override
  public AnnotationMirror greatestLowerBoundQualifiers(AnnotationMirror a1, AnnotationMirror a2) {
    if (isSubtypeQualifiers(a1, a2)) {
      return a1;
    } else if (isSubtypeQualifiers(a2, a1)) {
      return a2;
    } else {

      // Implementation of GLB where one of the annotations is StringVal is needed for
      // length-based refinement of constant string values. Other cases of length-based
      // refinement are handled by subtype check.
      if (AnnotationUtils.areSameByName(a1, ValueAnnotatedTypeFactory.STRINGVAL_NAME)) {
        return glbOfStringVal(a1, a2);
      } else if (AnnotationUtils.areSameByName(a2, ValueAnnotatedTypeFactory.STRINGVAL_NAME)) {
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
    // If both bounds of the new range are bigger than the old range, then returned range
    // should use the lower bound of the new range and a MAX_VALUE.
    if ((newRange.from >= oldRange.from && newRange.to >= oldRange.to)) {
      long max = lubRange.to;
      if (max < Byte.MAX_VALUE) {
        max = Byte.MAX_VALUE;
      } else if (max < Short.MAX_VALUE) {
        max = Short.MAX_VALUE;
      } else if (max < Integer.MAX_VALUE) {
        max = Integer.MAX_VALUE;
      } else {
        max = Long.MAX_VALUE;
      }
      return Range.create(newRange.from, max);
    }

    // If both bounds of the old range are bigger than the new range, then returned range
    // should use a MIN_VALUE and the upper bound of the new range.
    if ((newRange.from <= oldRange.from && newRange.to <= oldRange.to)) {
      long min = lubRange.from;
      if (min > Byte.MIN_VALUE) {
        min = Byte.MIN_VALUE;
      } else if (min > Short.MIN_VALUE) {
        min = Short.MIN_VALUE;
      } else if (min > Integer.MIN_VALUE) {
        min = Integer.MIN_VALUE;
      } else {
        min = Long.MIN_VALUE;
      }
      return Range.create(min, newRange.to);
    }

    if (lubRange.isWithin(Byte.MIN_VALUE + 1, Byte.MAX_VALUE)
        || lubRange.isWithin(Byte.MIN_VALUE, Byte.MAX_VALUE - 1)) {
      return Range.BYTE_EVERYTHING;
    } else if (lubRange.isWithin(Short.MIN_VALUE + 1, Short.MAX_VALUE)
        || lubRange.isWithin(Short.MIN_VALUE, Short.MAX_VALUE - 1)) {
      return Range.SHORT_EVERYTHING;
    } else if (lubRange.isWithin(Long.MIN_VALUE + 1, Long.MAX_VALUE)
        || lubRange.isWithin(Long.MIN_VALUE, Long.MAX_VALUE - 1)) {
      return Range.INT_EVERYTHING;
    } else {
      return Range.EVERYTHING;
    }
  }

  /**
   * Determines the least upper bound of a1 and a2, which contains the union of their sets of
   * possible values.
   *
   * @return the least upper bound of a1 and a2
   */
  @Override
  public @Nullable AnnotationMirror leastUpperBoundQualifiers(
      AnnotationMirror a1, AnnotationMirror a2) {
    if (!AnnotationUtils.areSameByName(getTopAnnotation(a1), getTopAnnotation(a2))) {
      // The annotations are in different hierarchies
      return null;
    }

    a1 = atypeFactory.convertSpecialIntRangeToStandardIntRange(a1);
    a2 = atypeFactory.convertSpecialIntRangeToStandardIntRange(a2);

    if (isSubtypeQualifiers(a1, a2)) {
      return a2;
    } else if (isSubtypeQualifiers(a2, a1)) {
      return a1;
    }
    String qualName1 = AnnotationUtils.annotationName(a1);
    String qualName2 = AnnotationUtils.annotationName(a2);

    if (qualName1.equals(qualName2)) {
      // If both are the same type, determine the type and merge
      switch (qualName1) {
        case ValueAnnotatedTypeFactory.INTRANGE_NAME:
          // special handling for IntRange
          Range intrange1 = atypeFactory.getRange(a1);
          Range intrange2 = atypeFactory.getRange(a2);
          return atypeFactory.createIntRangeAnnotation(intrange1.union(intrange2));
        case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
          // special handling for ArrayLenRange
          Range range1 = atypeFactory.getRange(a1);
          Range range2 = atypeFactory.getRange(a2);
          return atypeFactory.createArrayLenRangeAnnotation(range1.union(range2));
        case ValueAnnotatedTypeFactory.INTVAL_NAME:
          List<Long> longs = atypeFactory.getIntValues(a1);
          CollectionsPlume.adjoinAll(longs, atypeFactory.getIntValues(a2));
          return atypeFactory.createIntValAnnotation(longs);
        case ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
          List<Integer> arrayLens = atypeFactory.getArrayLength(a1);
          CollectionsPlume.adjoinAll(arrayLens, atypeFactory.getArrayLength(a2));
          return atypeFactory.createArrayLenAnnotation(arrayLens);
        case ValueAnnotatedTypeFactory.STRINGVAL_NAME:
          List<String> strings = atypeFactory.getStringValues(a1);
          CollectionsPlume.adjoinAll(strings, atypeFactory.getStringValues(a2));
          return atypeFactory.createStringAnnotation(strings);
        case ValueAnnotatedTypeFactory.BOOLVAL_NAME:
          List<Boolean> bools = atypeFactory.getBooleanValues(a1);
          CollectionsPlume.adjoinAll(bools, atypeFactory.getBooleanValues(a2));
          return atypeFactory.createBooleanAnnotation(bools);
        case ValueAnnotatedTypeFactory.DOUBLEVAL_NAME:
          List<Double> doubles = atypeFactory.getDoubleValues(a1);
          CollectionsPlume.adjoinAll(doubles, atypeFactory.getDoubleValues(a2));
          return atypeFactory.createDoubleAnnotation(doubles);
        case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME:
          List<@Regex String> regexes = atypeFactory.getMatchesRegexValues(a1);
          CollectionsPlume.adjoinAll(regexes, atypeFactory.getMatchesRegexValues(a2));
          return atypeFactory.createMatchesRegexAnnotation(regexes);
        case ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME:
          // The LUB is the intersection of the sets.
          List<@Regex String> regexes1 = atypeFactory.getDoesNotMatchRegexValues(a1);
          List<@Regex String> regexes2 = atypeFactory.getDoesNotMatchRegexValues(a2);
          regexes1.retainAll(regexes2);
          return atypeFactory.createDoesNotMatchRegexAnnotation(regexes1);
        default:
          throw new TypeSystemError("default case: %s %s %s%n", qualName1, a1, a2);
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
      case ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
        arrayLenAnno = a1;
        break;
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
        arrayLenRangeAnno = a1;
        break;
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME:
        stringValAnno = a1;
        break;
      case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME:
        matchesRegexAnno = a1;
        break;
      case ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME:
        doesNotMatchRegexAnno = a1;
        break;
      case ValueAnnotatedTypeFactory.INTVAL_NAME:
        intValAnno = a1;
        break;
      case ValueAnnotatedTypeFactory.INTRANGE_NAME:
        intRangeAnno = a1;
        break;
      case ValueAnnotatedTypeFactory.DOUBLEVAL_NAME:
        doubleValAnno = a1;
        break;
      default:
        // Do nothing
    }

    switch (qualName2) {
      case ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
        arrayLenAnno = a2;
        break;
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
        arrayLenRangeAnno = a2;
        break;
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME:
        stringValAnno = a2;
        break;
      case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME:
        matchesRegexAnno = a2;
        break;
      case ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME:
        doesNotMatchRegexAnno = a2;
        break;
      case ValueAnnotatedTypeFactory.INTVAL_NAME:
        intValAnno = a2;
        break;
      case ValueAnnotatedTypeFactory.INTRANGE_NAME:
        intRangeAnno = a2;
        break;
      case ValueAnnotatedTypeFactory.DOUBLEVAL_NAME:
        doubleValAnno = a2;
        break;
      default:
        // Do nothing
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
    String subQualName = AnnotationUtils.annotationName(subAnno);
    if (subQualName.equals(ValueAnnotatedTypeFactory.UNKNOWN_NAME)) {
      superAnno = atypeFactory.convertToUnknown(superAnno);
    }
    String superQualName = AnnotationUtils.annotationName(superAnno);
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
      case ValueAnnotatedTypeFactory.INTVAL_NAME + ValueAnnotatedTypeFactory.DOUBLEVAL_NAME:
        List<Double> superValues = atypeFactory.getDoubleValues(superAnno);
        List<Double> subValues =
            atypeFactory.convertLongListToDoubleList(atypeFactory.getIntValues(subAnno));
        return superValues.containsAll(subValues);
      case ValueAnnotatedTypeFactory.INTVAL_NAME + ValueAnnotatedTypeFactory.INTRANGE_NAME:
      case ValueAnnotatedTypeFactory.ARRAYLEN_NAME + ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
        Range superRange = atypeFactory.getRange(superAnno);
        List<Long> subLongValues = atypeFactory.getArrayLenOrIntValue(subAnno);
        Range subLongRange = Range.create(subLongValues);
        return superRange.contains(subLongRange);
      case ValueAnnotatedTypeFactory.INTRANGE_NAME + ValueAnnotatedTypeFactory.DOUBLEVAL_NAME:
        Range subRange = atypeFactory.getRange(subAnno);
        if (subRange.isWiderThan(ValueAnnotatedTypeFactory.MAX_VALUES)) {
          return false;
        }
        List<Double> superDoubleValues = atypeFactory.getDoubleValues(superAnno);
        List<Double> subDoubleValues = ValueCheckerUtils.getValuesFromRange(subRange, Double.class);
        return superDoubleValues.containsAll(subDoubleValues);
      case ValueAnnotatedTypeFactory.INTRANGE_NAME + ValueAnnotatedTypeFactory.INTVAL_NAME:
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME + ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
        Range subRange2 = atypeFactory.getRange(subAnno);
        if (subRange2.isWiderThan(ValueAnnotatedTypeFactory.MAX_VALUES)) {
          return false;
        }
        List<Long> superValues2 = atypeFactory.getArrayLenOrIntValue(superAnno);
        List<Long> subValues2 = ValueCheckerUtils.getValuesFromRange(subRange2, Long.class);
        return superValues2.containsAll(subValues2);
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME + ValueAnnotatedTypeFactory.STRINGVAL_NAME:
      case ValueAnnotatedTypeFactory.ARRAYLEN_NAME + ValueAnnotatedTypeFactory.STRINGVAL_NAME:

        // Allow @ArrayLen(0) to be converted to @StringVal("")
        List<String> superStringValues = atypeFactory.getStringValues(superAnno);
        return superStringValues.contains("") && atypeFactory.getMaxLenValue(subAnno) == 0;
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME + ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME:
        {
          List<String> strings = atypeFactory.getStringValues(subAnno);
          List<String> regexes =
              AnnotationUtils.getElementValueArray(
                  superAnno, atypeFactory.matchesRegexValueElement, String.class);
          return RegexUtil.everyStringMatchesSomeRegex(strings, regexes);
        }
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME
          + ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME:
        {
          List<String> strings = atypeFactory.getStringValues(subAnno);
          List<String> regexes =
              AnnotationUtils.getElementValueArray(
                  superAnno, atypeFactory.doesNotMatchRegexValueElement, String.class);
          return RegexUtil.noStringMatchesAnyRegex(strings, regexes);
        }
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME + ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
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
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME + ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
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
      default:
        return false;
    }
  }
}
