package org.checkerframework.common.value;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypeKindUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Performs pre-processing on annotations written by users, replacing illegal annotations by legal
 * ones.
 */
class ValueTypeAnnotator extends TypeAnnotator {

  /** The type factory to use. Shadows the field from the superclass with a more specific type. */
  @SuppressWarnings("HidingField")
  protected final ValueAnnotatedTypeFactory typeFactory;

  /**
   * Construct a new ValueTypeAnnotator.
   *
   * @param typeFactory the type factory to use
   */
  protected ValueTypeAnnotator(ValueAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
    this.typeFactory = typeFactory;
  }

  @Override
  protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
    replaceWithNewAnnoInSpecialCases(type);
    return super.scan(type, aVoid);
  }

  /**
   * This method performs pre-processing on annotations written by users.
   *
   * <p>If any *Val annotation has &gt; MAX_VALUES number of values provided, replaces the
   * annotation by @IntRange for integral types, @ArrayLenRange for arrays, @ArrayLen
   * or @ArrayLenRange for strings, and @UnknownVal for all other types. Works together with {@link
   * ValueVisitor#visitAnnotation(com.sun.source.tree.AnnotationTree, Void)} which issues warnings
   * to users in these cases.
   *
   * <p>If any @IntRange or @ArrayLenRange annotation has incorrect parameters, e.g. the value
   * "from" is greater than the value "to", replaces the annotation by {@code @BottomVal}. The
   * {@link ValueVisitor#visitAnnotation(com.sun.source.tree.AnnotationTree, Void)} raises an error
   * to users if the annotation was user-written.
   *
   * <p>If any @ArrayLen annotation has a negative number, replaces the annotation by {@code
   * BottomVal}. The {@link ValueVisitor#visitAnnotation(com.sun.source.tree.AnnotationTree, Void)}
   * raises an error to users if the annotation was user-written.
   *
   * <p>If a user only writes one side of an {@code IntRange} annotation, this method also computes
   * an appropriate default based on the underlying type for the other side of the range. For
   * instance, if the user writes {@code @IntRange(from = 1) short x;} then this method will
   * translate the annotation to {@code @IntRange(from = 1, to = Short.MAX_VALUE}.
   */
  private void replaceWithNewAnnoInSpecialCases(AnnotatedTypeMirror atm) {
    AnnotationMirror anno = atm.getAnnotationInHierarchy(typeFactory.UNKNOWNVAL);
    if (anno == null || anno.getElementValues().isEmpty()) {
      return;
    }

    if (AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.INTVAL_NAME)) {
      List<Long> values = typeFactory.getIntValues(anno);
      if (values.size() > ValueAnnotatedTypeFactory.MAX_VALUES) {
        atm.replaceAnnotation(typeFactory.createIntRangeAnnotation(Range.create(values)));
      }
    } else if (AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.ARRAYLEN_NAME)) {
      List<Integer> values = typeFactory.getArrayLength(anno);
      if (values.isEmpty()) {
        atm.replaceAnnotation(typeFactory.BOTTOMVAL);
      } else if (Collections.min(values) < 0) {
        atm.replaceAnnotation(typeFactory.BOTTOMVAL);
      } else if (values.size() > ValueAnnotatedTypeFactory.MAX_VALUES) {
        atm.replaceAnnotation(typeFactory.createArrayLenRangeAnnotation(Range.create(values)));
      }
    } else if (AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.INTRANGE_NAME)) {
      TypeMirror underlyingType = atm.getUnderlyingType();
      // If the underlying type is neither a primitive integral type nor boxed integral type,
      // return without making changes. TypesUtils.isIntegralPrimitiveOrBoxed fails if passed
      // a non-primitive type that is not a declared type, so it cannot be called directly.
      if (!TypeKindUtils.isIntegral(underlyingType.getKind())
          && (underlyingType.getKind() != TypeKind.DECLARED
              || !TypesUtils.isIntegralPrimitiveOrBoxed(underlyingType))) {
        return;
      }

      // Compute appropriate defaults for integral ranges.
      long from = typeFactory.getFromValueFromIntRange(atm);
      long to = typeFactory.getToValueFromIntRange(atm);

      if (from > to) {
        // `from > to` either indicates a user error when writing an annotation or an error in the
        // checker's implementation. `-from` should always be <= to. ValueVisitor#validateType will
        // issue an error.
        atm.replaceAnnotation(typeFactory.BOTTOMVAL);
      } else {
        // Always do a replacement of the annotation here so that the defaults calculated above are
        // correctly added to the annotation (assuming the annotation is well-formed).
        atm.replaceAnnotation(typeFactory.createIntRangeAnnotation(from, to));
      }
    } else if (AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME)) {
      int from = typeFactory.getArrayLenRangeFromValue(anno);
      int to = typeFactory.getArrayLenRangeToValue(anno);
      if (from > to) {
        // `from > to` either indicates a user error when writing an annotation or an error in the
        // checker's implementation `-from` should always be <= to. ValueVisitor#validateType will
        // issue an error.
        atm.replaceAnnotation(typeFactory.BOTTOMVAL);
      } else if (from < 0) {
        // No array can have a length less than 0. Any time the type includes a from
        // less than zero, it must indicate imprecision in the checker.
        atm.replaceAnnotation(typeFactory.createArrayLenRangeAnnotation(0, to));
      }
    } else if (AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.STRINGVAL_NAME)) {
      // The annotation is StringVal. If there are too many elements,
      // ArrayLen or ArrayLenRange is used.
      List<String> values = typeFactory.getStringValues(anno);

      if (values.size() > ValueAnnotatedTypeFactory.MAX_VALUES) {
        List<Integer> lengths = ValueCheckerUtils.getLengthsForStringValues(values);
        atm.replaceAnnotation(typeFactory.createArrayLenAnnotation(lengths));
      }

    } else if (AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME)) {
      // If the annotation contains an invalid regex, replace it with bottom. ValueVisitor
      // will issue a warning where the annotation was written.
      List<String> regexes =
          AnnotationUtils.getElementValueArray(
              anno, typeFactory.matchesRegexValueElement, String.class);
      for (String regex : regexes) {
        try {
          Pattern.compile(regex);
        } catch (PatternSyntaxException pse) {
          atm.replaceAnnotation(typeFactory.BOTTOMVAL);
          break;
        }
      }
    } else {
      // In here the annotation is @*Val where (*) is not Int, String but other types
      // (Double, etc).
      // Therefore we extract its values in a generic way to check its size.
      List<Object> values =
          AnnotationUtils.getElementValueArray(anno, "value", Object.class, false);
      if (values.size() > ValueAnnotatedTypeFactory.MAX_VALUES) {
        atm.replaceAnnotation(typeFactory.UNKNOWNVAL);
      }
    }
  }
}
