package org.checkerframework.checker.units;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.UnknownUnits;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * A helper class for UnitsRelations, providing numerous methods which help process Annotations and
 * Annotated Types representing various units.
 */
public class UnitsRelationsTools {

  /**
   * Creates an AnnotationMirror representing a unit defined by annoClass, with the specific Prefix
   * p.
   *
   * @param env the Checker Processing Environment, provided as a parameter in init() of a
   *     UnitsRelations implementation
   * @param annoClass the fully-qualified name of an Annotation representing a Unit (eg m.class for
   *     meters)
   * @param p a Prefix value
   * @return an AnnotationMirror of the Unit with the Prefix p, or null if it cannot be constructed
   */
  public static @Nullable AnnotationMirror buildAnnoMirrorWithSpecificPrefix(
      final ProcessingEnvironment env,
      final @FullyQualifiedName CharSequence annoClass,
      final Prefix p) {
    AnnotationBuilder builder = new AnnotationBuilder(env, annoClass);
    builder.setValue("value", p);
    return builder.build();
  }

  /**
   * Creates an AnnotationMirror representing a unit defined by annoClass, with no prefix.
   *
   * @param env checker Processing Environment, provided as a parameter in init() of a
   *     UnitsRelations implementation
   * @param annoClass the getElementValueClassname of an Annotation representing a Unit (eg m.class
   *     for meters)
   * @return an AnnotationMirror of the Unit with no prefix, or null if it cannot be constructed
   */
  public static @Nullable AnnotationMirror buildAnnoMirrorWithNoPrefix(
      final ProcessingEnvironment env, final @FullyQualifiedName CharSequence annoClass) {
    return AnnotationBuilder.fromName(env.getElementUtils(), annoClass);
  }

  /**
   * Retrieves the SI Prefix of an Annotated Type.
   *
   * @param annoType an AnnotatedTypeMirror representing a Units Annotated Type
   * @return a Prefix value (including Prefix.one), or null if it has none
   */
  public static @Nullable Prefix getPrefix(final AnnotatedTypeMirror annoType) {
    Prefix result = null;

    // go through each Annotation of an Annotated Type, find the prefix and return it
    for (AnnotationMirror mirror : annoType.getAnnotations()) {
      // try to get a prefix
      result = getPrefix(mirror);
      // if it is not null, then return the retrieved prefix immediately
      if (result != null) {
        return result;
      }
    }

    // if it can't find any prefix at all, then return null
    return result;
  }

  /**
   * Retrieves the SI Prefix of an Annotation.
   *
   * @param unitsAnnotation an AnnotationMirror representing a Units Annotation
   * @return a Prefix value (including Prefix.one), or null if it has none
   */
  public static @Nullable Prefix getPrefix(final AnnotationMirror unitsAnnotation) {
    AnnotationValue annotationValue = getAnnotationMirrorPrefix(unitsAnnotation);

    // if this Annotation has no prefix, return null
    if (hasNoPrefix(annotationValue)) {
      return null;
    }

    // if the Annotation has a value, then detect and match the string name of the prefix, and
    // return the matching Prefix
    String prefixString = annotationValue.getValue().toString();
    for (Prefix prefix : Prefix.values()) {
      if (prefixString.equals(prefix.toString())) {
        return prefix;
      }
    }

    // if none of the strings match, then return null
    return null;
  }

  /**
   * Checks to see if an Annotated Type has no prefix.
   *
   * @param annoType an AnnotatedTypeMirror representing a Units Annotated Type
   * @return true if it has no prefix, false otherwise
   */
  public static boolean hasNoPrefix(final AnnotatedTypeMirror annoType) {
    for (AnnotationMirror mirror : annoType.getAnnotations()) {
      // if any Annotation has a prefix, return false
      if (!hasNoPrefix(mirror)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks to see if an Annotation has no prefix (ie, no value element).
   *
   * @param unitsAnnotation an AnnotationMirror representing a Units Annotation
   * @return true if it has no prefix, false otherwise
   */
  public static boolean hasNoPrefix(final AnnotationMirror unitsAnnotation) {
    AnnotationValue annotationValue = getAnnotationMirrorPrefix(unitsAnnotation);
    return hasNoPrefix(annotationValue);
  }

  private static boolean hasNoPrefix(final AnnotationValue annotationValue) {
    // Annotation has no element value (ie no SI prefix)
    if (annotationValue == null) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Given an Annotation, returns the prefix (eg kilo) as an AnnotationValue if there is any,
   * otherwise returns null.
   */
  private static @Nullable AnnotationValue getAnnotationMirrorPrefix(
      final AnnotationMirror unitsAnnotation) {
    Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
        unitsAnnotation.getElementValues();

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        elementValues.entrySet()) {
      if (entry.getKey().getSimpleName().contentEquals("value")) {
        return entry.getValue();
      }
    }

    return null;
  }

  /**
   * Removes the prefix value from an Annotation, by constructing and returning a copy of its base
   * SI unit's Annotation.
   *
   * @param elements the Element Utilities from a checker's processing environment, typically
   *     obtained by calling env.getElementUtils() in init() of a Units Relations implementation
   * @param unitsAnnotation an AnnotationMirror representing a Units Annotation
   * @return the base SI Unit's AnnotationMirror, or null if the base SI Unit cannot be constructed
   */
  public static @Nullable AnnotationMirror removePrefix(
      final Elements elements, final AnnotationMirror unitsAnnotation) {
    if (hasNoPrefix(unitsAnnotation)) {
      // Optimization, though the else case would also work.
      return unitsAnnotation;
    } else {
      String unitsAnnoName = AnnotationUtils.annotationName(unitsAnnotation);
      // In the Units Checker, the only annotation value is the prefix value.  Therefore,
      // fromName (which creates an annotation with no values) is acceptable.
      // TODO: refine sensitivity of removal for extension units, in case extension
      // Annotations have more than just Prefix in its values.
      return AnnotationBuilder.fromName(elements, unitsAnnoName);
    }
  }

  /**
   * Removes the Prefix value from an Annotated Type, by constructing and returning a copy of the
   * Annotated Type without the prefix.
   *
   * @param elements the Element Utilities from a checker's processing environment, typically
   *     obtained by calling env.getElementUtils() in init() of a Units Relations implementation
   * @param annoType an AnnotatedTypeMirror representing a Units Annotated Type
   * @return a copy of the Annotated Type without the prefix
   */
  public static AnnotatedTypeMirror removePrefix(
      final Elements elements, final AnnotatedTypeMirror annoType) {
    // deep copy the Annotated Type Mirror without any of the Annotations
    AnnotatedTypeMirror result = annoType.deepCopy(false);

    // get all of the original Annotations in the Annotated Type
    Set<AnnotationMirror> annos = annoType.getAnnotations();

    // loop through all the Annotations to see if they use Prefix.one, remove Prefix.one if it
    // does
    for (AnnotationMirror anno : annos) {
      // try to clean the Annotation Mirror of the Prefix
      AnnotationMirror cleanedMirror = removePrefix(elements, anno);
      // if successful, add the cleaned annotation to the deep copy
      if (cleanedMirror != null) {
        result.addAnnotation(cleanedMirror);
      }
      // if unsuccessful, add the original annotation
      else {
        result.addAnnotation(anno);
      }
    }

    return result;
  }

  /**
   * Checks to see if a particular Annotated Type has no units, such as scalar constants in
   * calculations.
   *
   * <p>Any number that isn't assigned a unit will automatically get the Annotation UnknownUnits.
   * eg: int x = 5; // x has @UnknownUnits
   *
   * @param annoType an AnnotatedTypeMirror representing a Units Annotated Type
   * @return true if the Type has no units, false otherwise
   */
  public static boolean hasNoUnits(final AnnotatedTypeMirror annoType) {
    return (annoType.getAnnotation(UnknownUnits.class) != null);
  }

  /**
   * Checks to see if a particular Annotated Type has a specific unit (represented by its
   * Annotation).
   *
   * @param annoType an AnnotatedTypeMirror representing a Units Annotated Type
   * @param unitsAnnotation an AnnotationMirror representing a Units Annotation of a specific unit
   * @return true if the Type has the specific unit, false otherwise
   */
  public static boolean hasSpecificUnit(
      final AnnotatedTypeMirror annoType, final AnnotationMirror unitsAnnotation) {
    return AnnotationUtils.containsSame(annoType.getAnnotations(), unitsAnnotation);
  }

  /**
   * Checks to see if a particular Annotated Type has a particular base unit (represented by its
   * Annotation).
   *
   * @param annoType an AnnotatedTypeMirror representing a Units Annotated Type
   * @param unitsAnnotation an AnnotationMirror representing a Units Annotation of the base unit
   * @return true if the Type has the specific unit, false otherwise
   */
  public static boolean hasSpecificUnitIgnoringPrefix(
      final AnnotatedTypeMirror annoType, final AnnotationMirror unitsAnnotation) {
    return AnnotationUtils.containsSameByName(annoType.getAnnotations(), unitsAnnotation);
  }

  /**
   * Creates an AnnotationMirror representing a unit defined by annoClass, with the specific Prefix
   * p.
   *
   * <p>This interface is intended only for subclasses of UnitsRelations; other clients should use
   * {@link #buildAnnoMirrorWithSpecificPrefix(ProcessingEnvironment, CharSequence, Prefix)}
   *
   * @param env the Checker Processing Environment, provided as a parameter in init() of a
   *     UnitsRelations implementation
   * @param annoClass the Class of an Annotation representing a Unit (eg m.class for meters)
   * @param p a Prefix value
   * @return an AnnotationMirror of the Unit with the Prefix p, or null if it cannot be constructed
   */
  public static @Nullable AnnotationMirror buildAnnoMirrorWithSpecificPrefix(
      final ProcessingEnvironment env,
      final Class<? extends Annotation> annoClass,
      final Prefix p) {
    AnnotationBuilder builder = new AnnotationBuilder(env, annoClass);
    builder.setValue("value", p);
    return builder.build();
  }

  /**
   * Creates an AnnotationMirror representing a unit defined by annoClass, with the default Prefix
   * of {@code Prefix.one}.
   *
   * <p>This interface is intended only for subclasses of UnitsRelations; other clients should not
   * use it.
   *
   * @param env the Checker Processing Environment, provided as a parameter in init() of a
   *     UnitsRelations implementation
   * @param annoClass the Class of an Annotation representing a Unit (eg m.class for meters)
   * @return an AnnotationMirror of the Unit with Prefix.one, or null if it cannot be constructed
   */
  public static @Nullable AnnotationMirror buildAnnoMirrorWithDefaultPrefix(
      final ProcessingEnvironment env, final Class<? extends Annotation> annoClass) {
    return buildAnnoMirrorWithSpecificPrefix(env, annoClass, Prefix.one);
  }

  /**
   * Creates an AnnotationMirror representing a unit defined by annoClass, with no prefix.
   *
   * <p>This interface is intended only for subclasses of UnitsRelations; other clients should use
   * {@link #buildAnnoMirrorWithNoPrefix(ProcessingEnvironment, CharSequence)}.
   *
   * @param env checker Processing Environment, provided as a parameter in init() of a
   *     UnitsRelations implementation
   * @param annoClass the Class of an Annotation representing a Unit (eg m.class for meters)
   * @return an AnnotationMirror of the Unit with no prefix, or null if it cannot be constructed
   */
  static @Nullable AnnotationMirror buildAnnoMirrorWithNoPrefix(
      final ProcessingEnvironment env, final Class<? extends Annotation> annoClass) {
    return AnnotationBuilder.fromClass(env.getElementUtils(), annoClass);
  }
}
