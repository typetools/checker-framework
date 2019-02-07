package org.checkerframework.checker.units;

import java.lang.annotation.Annotation;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;

/** Interface that is used to specify the relation between units. */
public interface UnitsRelations {
    /**
     * Initialize the object. Needs to be called before any other method.
     *
     * @param env the ProcessingEnvironment to use
     * @return a reference to "this"
     */
    UnitsRelations init(ProcessingEnvironment env);

    /**
     * Called for the multiplication of type lht and rht.
     *
     * @param lht left hand side in multiplication
     * @param rht right hand side in multiplication
     * @return the annotation to use for the result of the multiplication or null if no special
     *     relation is known
     */
    @Nullable AnnotationMirror multiplication(AnnotatedTypeMirror lht, AnnotatedTypeMirror rht);

    /**
     * Called for the division of type lht and rht.
     *
     * @param lht left hand side in division
     * @param rht right hand side in division
     * @return the annotation to use for the result of the division or null if no special relation
     *     is known
     */
    @Nullable AnnotationMirror division(AnnotatedTypeMirror lht, AnnotatedTypeMirror rht);

    /**
     * Creates an AnnotationMirror representing a unit defined by annoClass, with the specific
     * Prefix p.
     *
     * <p>This interface is intended only for subclasses of UnitsRelations; other clients should use
     * methods in UnitsRelationsTools.
     *
     * @param env the Checker Processing Environment, provided as a parameter in init() of a
     *     UnitsRelations implementation
     * @param annoClass the Class of an Annotation representing a Unit (eg m.class for meters)
     * @param p a Prefix value
     * @return an AnnotationMirror of the Unit with the Prefix p, or null if it cannot be
     *     constructed
     */
    static @Nullable AnnotationMirror buildAnnoMirrorWithSpecificPrefix(
            final ProcessingEnvironment env,
            final Class<? extends Annotation> annoClass,
            final Prefix p) {
        AnnotationBuilder builder = new AnnotationBuilder(env, annoClass.getCanonicalName());
        builder.setValue("value", p);
        return builder.build();
    }

    /**
     * Creates an AnnotationMirror representing a unit defined by annoClass, with the default Prefix
     * of {@code Prefix.one}.
     *
     * <p>This interface is intended only for subclasses of UnitsRelations; other clients should use
     * methods in UnitsRelationsTools.
     *
     * @param env the Checker Processing Environment, provided as a parameter in init() of a
     *     UnitsRelations implementation
     * @param annoClass the Class of an Annotation representing a Unit (eg m.class for meters)
     * @return an AnnotationMirror of the Unit with Prefix.one, or null if it cannot be constructed
     */
    static @Nullable AnnotationMirror buildAnnoMirrorWithDefaultPrefix(
            final ProcessingEnvironment env, final Class<? extends Annotation> annoClass) {
        return buildAnnoMirrorWithSpecificPrefix(env, annoClass, Prefix.one);
    }

    /**
     * Creates an AnnotationMirror representing a unit defined by annoClass, with no prefix.
     *
     * <p>This interface is intended only for subclasses of UnitsRelations; other clients should use
     * methods in UnitsRelationsTools.
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
