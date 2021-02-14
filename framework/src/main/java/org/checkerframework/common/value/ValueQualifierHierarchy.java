package org.checkerframework.common.value;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/** The qualifier hierarchy for the Value type system. */
final class ValueQualifierHierarchy extends ElementQualifierHierarchy {

    /** The type factory to use. */
    private final ValueAnnotatedTypeFactory atypeFactory;

    /**
     * Creates a ValueQualifierHierarchy from the given classes.
     *
     * @param atypeFactory ValueAnnotatedTypeFactory
     * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
     */
    ValueQualifierHierarchy(
            ValueAnnotatedTypeFactory atypeFactory,
            Collection<Class<? extends Annotation>> qualifierClasses) {
        super(qualifierClasses, atypeFactory.getElementUtils());
        this.atypeFactory = atypeFactory;
    }

    /**
     * Computes greatest lower bound of a @StringVal annotation with another Value Checker
     * annotation.
     *
     * @param stringValAnno annotation of type @StringVal
     * @param otherAnno annotation from the value checker hierarchy
     * @return greatest lower bound of {@code stringValAnno} and {@code otherAnno}
     */
    private AnnotationMirror glbOfStringVal(
            AnnotationMirror stringValAnno, AnnotationMirror otherAnno) {
        List<String> values = ValueAnnotatedTypeFactory.getStringValues(stringValAnno);
        switch (AnnotationUtils.annotationName(otherAnno)) {
            case ValueAnnotatedTypeFactory.STRINGVAL_NAME:
                // Intersection of value lists
                List<String> otherValues = ValueAnnotatedTypeFactory.getStringValues(otherAnno);
                values.retainAll(otherValues);
                break;
            case ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
                // Retain strings of correct lengths
                List<Integer> otherLengths = ValueAnnotatedTypeFactory.getArrayLength(otherAnno);
                ArrayList<String> result = new ArrayList<>();
                for (String s : values) {
                    if (otherLengths.contains(s.length())) {
                        result.add(s);
                    }
                }
                values = result;
                break;
            case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
                // Retain strings of lengths from a range
                Range otherRange = ValueAnnotatedTypeFactory.getRange(otherAnno);
                ArrayList<String> range = new ArrayList<>();
                for (String s : values) {
                    if (otherRange.contains(s.length())) {
                        range.add(s);
                    }
                }
                values = range;
                break;
            case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME:
                List<@Regex String> regexes = ValueAnnotatedTypeFactory.getStringValues(otherAnno);
                values =
                        values.stream()
                                .filter(value -> regexes.stream().anyMatch(value::matches))
                                .collect(Collectors.toList());
                break;
            default:
                return atypeFactory.BOTTOMVAL;
        }

        return atypeFactory.createStringAnnotation(values);
    }

    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        if (isSubtype(a1, a2)) {
            return a1;
        } else if (isSubtype(a2, a1)) {
            return a2;
        } else {

            // Implementation of GLB where one of the annotations is StringVal is needed for
            // length-based refinement of constant string values. Other cases of length-based
            // refinement are handled by subtype check.
            if (AnnotationUtils.areSameByName(a1, ValueAnnotatedTypeFactory.STRINGVAL_NAME)) {
                return glbOfStringVal(a1, a2);
            } else if (AnnotationUtils.areSameByName(
                    a2, ValueAnnotatedTypeFactory.STRINGVAL_NAME)) {
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
        AnnotationMirror lub = leastUpperBound(newQualifier, previousQualifier);
        if (AnnotationUtils.areSameByName(lub, ValueAnnotatedTypeFactory.INTRANGE_NAME)) {
            Range lubRange = ValueAnnotatedTypeFactory.getRange(lub);
            Range newRange = ValueAnnotatedTypeFactory.getRange(newQualifier);
            Range oldRange = ValueAnnotatedTypeFactory.getRange(previousQualifier);
            Range wubRange = widenedRange(newRange, oldRange, lubRange);
            return atypeFactory.createIntRangeAnnotation(wubRange);
        } else if (AnnotationUtils.areSameByName(
                lub, ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME)) {
            Range lubRange = ValueAnnotatedTypeFactory.getRange(lub);
            Range newRange = ValueAnnotatedTypeFactory.getRange(newQualifier);
            Range oldRange = ValueAnnotatedTypeFactory.getRange(previousQualifier);
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
    public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
        if (!AnnotationUtils.areSameByName(getTopAnnotation(a1), getTopAnnotation(a2))) {
            // The annotations are in different hierarchies
            return null;
        }

        a1 = atypeFactory.convertSpecialIntRangeToStandardIntRange(a1);
        a2 = atypeFactory.convertSpecialIntRangeToStandardIntRange(a2);

        if (isSubtype(a1, a2)) {
            return a2;
        } else if (isSubtype(a2, a1)) {
            return a1;
        }
        String qual1 = AnnotationUtils.annotationName(a1);
        String qual2 = AnnotationUtils.annotationName(a2);

        if (qual1.equals(qual2)) {
            // If both are the same type, determine the type and merge
            switch (qual1) {
                case ValueAnnotatedTypeFactory.INTRANGE_NAME:
                    // special handling for IntRange
                    Range intrange1 = ValueAnnotatedTypeFactory.getRange(a1);
                    Range intrange2 = ValueAnnotatedTypeFactory.getRange(a2);
                    return atypeFactory.createIntRangeAnnotation(intrange1.union(intrange2));
                case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
                    // special handling for ArrayLenRange
                    Range range1 = ValueAnnotatedTypeFactory.getRange(a1);
                    Range range2 = ValueAnnotatedTypeFactory.getRange(a2);
                    return atypeFactory.createArrayLenRangeAnnotation(range1.union(range2));
                case ValueAnnotatedTypeFactory.INTVAL_NAME:
                    List<Long> a1Values = ValueAnnotatedTypeFactory.getIntValues(a1);
                    List<Long> a2Values = ValueAnnotatedTypeFactory.getIntValues(a2);
                    List<Long> newValues = new ArrayList<>();
                    newValues.addAll(a1Values);
                    newValues.addAll(a2Values);
                    return atypeFactory.createIntValAnnotation(newValues);
                case ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
                    List<Integer> al1Values = ValueAnnotatedTypeFactory.getArrayLength(a1);
                    List<Integer> al2Values = ValueAnnotatedTypeFactory.getArrayLength(a2);
                    List<Integer> newValuesAL = new ArrayList<>();
                    newValuesAL.addAll(al1Values);
                    newValuesAL.addAll(al2Values);
                    return atypeFactory.createArrayLenAnnotation(newValuesAL);
                case ValueAnnotatedTypeFactory.STRINGVAL_NAME:
                    List<String> string1Values = ValueAnnotatedTypeFactory.getStringValues(a1);
                    List<String> string2Values = ValueAnnotatedTypeFactory.getStringValues(a2);
                    List<String> newStringValues = new ArrayList<>();
                    newStringValues.addAll(string1Values);
                    newStringValues.addAll(string2Values);
                    return atypeFactory.createStringAnnotation(newStringValues);
                default:
                    List<Object> object1Values =
                            AnnotationUtils.getElementValueArray(a1, "value", Object.class, true);
                    List<Object> object2Values =
                            AnnotationUtils.getElementValueArray(a2, "value", Object.class, true);
                    TreeSet<Object> newObjectValues = new TreeSet<>();
                    newObjectValues.addAll(object1Values);
                    newObjectValues.addAll(object2Values);

                    if (newObjectValues.isEmpty()) {
                        return atypeFactory.BOTTOMVAL;
                    }
                    if (newObjectValues.size() > ValueAnnotatedTypeFactory.MAX_VALUES) {
                        return atypeFactory.UNKNOWNVAL;
                    }
                    AnnotationBuilder builder =
                            new AnnotationBuilder(
                                    atypeFactory.getProcessingEnv(),
                                    AnnotationUtils.annotationName(a1));
                    List<Object> valuesList = new ArrayList<>(newObjectValues);
                    builder.setValue("value", valuesList);
                    return builder.build();
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
        AnnotationMirror intValAnno = null;
        AnnotationMirror intRangeAnno = null;
        AnnotationMirror doubleValAnno = null;

        switch (qual1) {
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

        switch (qual2) {
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
        if (arrayLenAnno != null && arrayLenRangeAnno != null) {
            return leastUpperBound(
                    arrayLenRangeAnno, atypeFactory.convertArrayLenToArrayLenRange(arrayLenAnno));
        } else if (stringValAnno != null && arrayLenAnno != null) {
            return leastUpperBound(
                    arrayLenAnno, atypeFactory.convertStringValToArrayLen(stringValAnno));
        } else if (stringValAnno != null && arrayLenRangeAnno != null) {
            return leastUpperBound(
                    arrayLenRangeAnno, atypeFactory.convertStringValToArrayLenRange(stringValAnno));
        } else if (stringValAnno != null && matchesRegexAnno != null) {
            return leastUpperBound(
                    matchesRegexAnno, atypeFactory.convertStringValToMatchesRegex(stringValAnno));
        }

        // Annotations are both in the same hierarchy, but they are not the same.
        // If a1 and a2 are not the same type of *Value annotation, they may still be mergeable
        // because some values can be implicitly cast as others. For example, if a1 and a2 are
        // both in {DoubleVal, IntVal} then they will be converted upwards: IntVal -> DoubleVal
        // to arrive at a common annotation type.

        if (doubleValAnno != null) {
            if (intRangeAnno != null) {
                intValAnno = atypeFactory.convertIntRangeToIntVal(intRangeAnno);
                if (AnnotationUtils.areSameByName(
                        intValAnno, ValueAnnotatedTypeFactory.UNKNOWN_NAME)) {
                    intValAnno = null;
                }
            }
            if (intValAnno != null) {
                // Convert intValAnno to a @DoubleVal AnnotationMirror
                AnnotationMirror doubleValAnno2 = atypeFactory.convertIntValToDoubleVal(intValAnno);
                return leastUpperBound(doubleValAnno, doubleValAnno2);
            }
            return atypeFactory.UNKNOWNVAL;
        }
        if (intRangeAnno != null && intValAnno != null) {
            // Convert intValAnno to an @IntRange AnnotationMirror
            AnnotationMirror intRangeAnno2 = atypeFactory.convertIntValToIntRange(intValAnno);
            return leastUpperBound(intRangeAnno, intRangeAnno2);
        }

        // In all other cases, the LUB is UnknownVal.
        return atypeFactory.UNKNOWNVAL;
    }

    /**
     * Computes subtyping as per the subtyping in the qualifier hierarchy structure unless both
     * annotations are Value. In this case, subAnno is a subtype of superAnno iff superAnno contains
     * at least every element of subAnno.
     *
     * @return true if subAnno is a subtype of superAnno, false otherwise
     */
    @Override
    public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        subAnno = atypeFactory.convertSpecialIntRangeToStandardIntRange(subAnno);
        superAnno = atypeFactory.convertSpecialIntRangeToStandardIntRange(superAnno);
        String subQual = AnnotationUtils.annotationName(subAnno);
        if (subQual.equals(ValueAnnotatedTypeFactory.UNKNOWN_NAME)) {
            superAnno = atypeFactory.convertToUnknown(superAnno);
        }
        String superQual = AnnotationUtils.annotationName(superAnno);
        if (superQual.equals(ValueAnnotatedTypeFactory.UNKNOWN_NAME)
                || subQual.equals(ValueAnnotatedTypeFactory.BOTTOMVAL_NAME)) {
            return true;
        } else if (superQual.equals(ValueAnnotatedTypeFactory.BOTTOMVAL_NAME)
                || subQual.equals(ValueAnnotatedTypeFactory.UNKNOWN_NAME)) {
            return false;
        } else if (superQual.equals(ValueAnnotatedTypeFactory.POLY_NAME)) {
            return subQual.equals(ValueAnnotatedTypeFactory.POLY_NAME);
        } else if (subQual.equals(ValueAnnotatedTypeFactory.POLY_NAME)) {
            return false;
        } else if (superQual.equals(subQual)) {
            // Same type, so might be subtype
            if (subQual.equals(ValueAnnotatedTypeFactory.INTRANGE_NAME)
                    || subQual.equals(ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME)) {
                // Special case for range-based annotations
                Range superRange = ValueAnnotatedTypeFactory.getRange(superAnno);
                Range subRange = ValueAnnotatedTypeFactory.getRange(subAnno);
                return superRange.contains(subRange);
            } else {
                List<Object> superValues =
                        AnnotationUtils.getElementValueArray(
                                superAnno, "value", Object.class, true);
                List<Object> subValues =
                        AnnotationUtils.getElementValueArray(subAnno, "value", Object.class, true);
                return superValues.containsAll(subValues);
            }
        }
        switch (superQual + subQual) {
            case ValueAnnotatedTypeFactory.DOUBLEVAL_NAME + ValueAnnotatedTypeFactory.INTVAL_NAME:
                List<Double> superValues = ValueAnnotatedTypeFactory.getDoubleValues(superAnno);
                List<Double> subValues =
                        atypeFactory.convertLongListToDoubleList(
                                ValueAnnotatedTypeFactory.getIntValues(subAnno));
                return superValues.containsAll(subValues);
            case ValueAnnotatedTypeFactory.INTRANGE_NAME + ValueAnnotatedTypeFactory.INTVAL_NAME:
            case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME
                    + ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
                Range superRange = ValueAnnotatedTypeFactory.getRange(superAnno);
                List<Long> subLongValues = atypeFactory.getArrayLenOrIntValue(subAnno);
                Range subLongRange = Range.create(subLongValues);
                return superRange.contains(subLongRange);
            case ValueAnnotatedTypeFactory.DOUBLEVAL_NAME + ValueAnnotatedTypeFactory.INTRANGE_NAME:
                Range subRange = ValueAnnotatedTypeFactory.getRange(subAnno);
                if (subRange.isWiderThan(ValueAnnotatedTypeFactory.MAX_VALUES)) {
                    return false;
                }
                List<Double> superDoubleValues =
                        ValueAnnotatedTypeFactory.getDoubleValues(superAnno);
                List<Double> subDoubleValues =
                        ValueCheckerUtils.getValuesFromRange(subRange, Double.class);
                return superDoubleValues.containsAll(subDoubleValues);
            case ValueAnnotatedTypeFactory.INTVAL_NAME + ValueAnnotatedTypeFactory.INTRANGE_NAME:
            case ValueAnnotatedTypeFactory.ARRAYLEN_NAME
                    + ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
                Range subRange2 = ValueAnnotatedTypeFactory.getRange(subAnno);
                if (subRange2.isWiderThan(ValueAnnotatedTypeFactory.MAX_VALUES)) {
                    return false;
                }
                List<Long> superValues2 = atypeFactory.getArrayLenOrIntValue(superAnno);
                List<Long> subValues2 = ValueCheckerUtils.getValuesFromRange(subRange2, Long.class);
                return superValues2.containsAll(subValues2);
            case ValueAnnotatedTypeFactory.STRINGVAL_NAME
                    + ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
            case ValueAnnotatedTypeFactory.STRINGVAL_NAME + ValueAnnotatedTypeFactory.ARRAYLEN_NAME:

                // Allow @ArrayLen(0) to be converted to @StringVal("")
                List<String> superStringValues =
                        ValueAnnotatedTypeFactory.getStringValues(superAnno);
                return superStringValues.contains("") && atypeFactory.getMaxLenValue(subAnno) == 0;
            case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME
                    + ValueAnnotatedTypeFactory.STRINGVAL_NAME:
                List<String> strings = ValueAnnotatedTypeFactory.getStringValues(subAnno);
                List<String> regexes = ValueAnnotatedTypeFactory.getStringValues(superAnno);
                return strings.stream()
                        .allMatch(string -> regexes.stream().anyMatch(string::matches));
            case ValueAnnotatedTypeFactory.ARRAYLEN_NAME + ValueAnnotatedTypeFactory.STRINGVAL_NAME:
                // StringVal is a subtype of ArrayLen, if all the strings have one of the
                // correct
                // lengths
                List<Integer> superIntValues = ValueAnnotatedTypeFactory.getArrayLength(superAnno);
                List<String> subStringValues = ValueAnnotatedTypeFactory.getStringValues(subAnno);
                for (String value : subStringValues) {
                    if (!superIntValues.contains(value.length())) {
                        return false;
                    }
                }
                return true;
            case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME
                    + ValueAnnotatedTypeFactory.STRINGVAL_NAME:
                // StringVal is a subtype of ArrayLenRange, if all the strings have a length in
                // the
                // range.
                Range superRange2 = ValueAnnotatedTypeFactory.getRange(superAnno);
                List<String> subValues3 = ValueAnnotatedTypeFactory.getStringValues(subAnno);
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
