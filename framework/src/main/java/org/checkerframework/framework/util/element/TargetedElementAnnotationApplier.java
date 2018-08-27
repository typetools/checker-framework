package org.checkerframework.framework.util.element;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.TargetType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.PluginUtil;

/**
 * TargetedElementAnnotationApplier filters annotations for an element into 3 groups. TARGETED
 * annotations are those we wish to apply in this ElementAnnotationApplier. VALID annotations are
 * those that are valid on the current element (or its enclosure, see getRawTypeAttributes) but
 * should not be applied to the given type. Invalid annotations are those that should NEVER appear
 * for the given element. Invalid annotations are reported as errors by default in the handleInvalid
 * method. See method extractAndApply. Please read getRawTypeAttributes for an idea of what types of
 * annotations may be encountered by this ElementAnnotationApplier.
 *
 * <p>Note: Subtypes of this class likely want to implement the handleTargeted and handleValid
 * methods though they have default empty implementations for brevity.
 */
abstract class TargetedElementAnnotationApplier {
    /**
     * Three annotation types that may be encountered when calling getRawTypeAttributes. see sift().
     */
    static enum TargetClass {
        TARGETED,
        VALID,
        INVALID
    }

    /** The type to which we wish to apply annotations. */
    protected final AnnotatedTypeMirror type;

    /** An Element that type represents. */
    protected final Element element;

    /**
     * @return the TargetTypes that identify annotations we wish to apply with this object. Any
     *     annotations that have these target types will be passed to handleTargeted.
     */
    protected abstract TargetType[] annotatedTargets();

    /**
     * @return the TargetTypes that identify annotations that are valid but we wish to ignore. Any
     *     annotations that have these target types will be passed to handleValid, providing they
     *     aren't also in annotatedTargets.
     */
    protected abstract TargetType[] validTargets();

    /**
     * Annotations on elements are represented as Attribute.TypeCompounds ( a subtype of
     * AnnotationMirror) that are usually accessed through a getRawTypeAttributes method on the
     * element.
     *
     * <p>In Java 8 and later these annotations are generally contained by elements to which they
     * apply. However, in earlier versions of Java many of these annotations are handled by either
     * the enclosing method, e.g. parameters and method type parameters, or enclosing class, e.g.
     * class type parameters. Therefore, many annotations are addressed by first getting all
     * annotations on a method or class and the picking out only the ones we wish to target (see
     * extractAndApply).
     *
     * @return the annotations that we MAY wish to apply to the given type
     */
    protected abstract Iterable<Attribute.TypeCompound> getRawTypeAttributes();

    /**
     * Tests element/type fields to ensure that this TargetedElementAnnotationApplier is valid for
     * this element/type pair.
     *
     * @return true if the type/element members are handled by this class false otherwise
     */
    protected abstract boolean isAccepted();

    /**
     * @param type the type to annotate
     * @param element an element identifying type
     */
    TargetedElementAnnotationApplier(final AnnotatedTypeMirror type, final Element element) {
        this.type = type;
        this.element = element;
    }

    /**
     * This method should apply all annotations that are handled by this object.
     *
     * @param targeted the list of annotations that were returned by getRawTypeAttributes and had a
     *     TargetType contained by annotatedTargets
     */
    protected abstract void handleTargeted(List<Attribute.TypeCompound> targeted);

    /**
     * The default implementation of this method does nothing.
     *
     * @param valid the list of annotations that were returned by getRawTypeAttributes and had a
     *     TargetType contained by valid and NOT annotatedTargets
     */
    protected void handleValid(List<Attribute.TypeCompound> valid) {}

    /**
     * @param invalid the list of annotations that were returned by getRawTypeAttributes and were
     *     not handled by handleTargeted or handleValid
     */
    protected void handleInvalid(List<Attribute.TypeCompound> invalid) {
        List<Attribute.TypeCompound> remaining = new ArrayList<>(invalid.size());
        for (Attribute.TypeCompound tc : invalid) {
            if (tc.getAnnotationType().getKind() != TypeKind.ERROR) {
                // Filter out annotations that have an error type. javac will
                // already have raised an error for them.
                remaining.add(tc);
            }
        }
        if (!remaining.isEmpty()) {
            throw new BugInCF(
                    this.getClass().getName()
                            + ".handleInvalid: "
                            + "Invalid variable and element passed to extractAndApply; type: "
                            + type
                            + ","
                            + " element: "
                            + element
                            + " (kind: "
                            + element.getKind()
                            + "), invalid annotations: "
                            + PluginUtil.join(", ", remaining)
                            + "\n"
                            + "Targeted annotations: "
                            + PluginUtil.join(", ", annotatedTargets())
                            + "; Valid annotations: "
                            + PluginUtil.join(", ", validTargets()));
        }
    }

    /**
     * Separate the input annotations into a Map of TargetClass (TARGETED, VALID, INVALID) to the
     * annotations that fall into each of those categories.
     *
     * @param typeCompounds annotations to sift through, should be those returned by
     *     getRawTypeAttributes
     * @return a {@literal Map<TargetClass &rArr; Annotations>.}
     */
    protected Map<TargetClass, List<Attribute.TypeCompound>> sift(
            final Iterable<Attribute.TypeCompound> typeCompounds) {

        final Map<TargetClass, List<Attribute.TypeCompound>> targetClassToCompound =
                new EnumMap<>(TargetClass.class);
        for (TargetClass targetClass : TargetClass.values()) {
            targetClassToCompound.put(targetClass, new ArrayList<>());
        }

        for (final Attribute.TypeCompound typeCompound : typeCompounds) {
            final TargetType typeCompoundTarget = typeCompound.position.type;
            final List<Attribute.TypeCompound> destList;

            if (ElementAnnotationUtil.contains(typeCompoundTarget, annotatedTargets())) {
                destList = targetClassToCompound.get(TargetClass.TARGETED);

            } else if (ElementAnnotationUtil.contains(typeCompoundTarget, validTargets())) {
                destList = targetClassToCompound.get(TargetClass.VALID);

            } else {
                destList = targetClassToCompound.get(TargetClass.INVALID);
            }

            destList.add(typeCompound);
        }

        return targetClassToCompound;
    }

    /**
     * Reads the list of annotations that apply to this element (see getRawTypeAttributes). Sifts
     * them into three groups (TARGETED, INVALID, VALID) and then calls the appropriate handle
     * method on them. The handleTargeted method should apply all annotations that are handled by
     * this object.
     *
     * <p>This method will throw a runtime exception if isAccepted returns false.
     */
    public void extractAndApply() {
        if (!isAccepted()) {
            throw new BugInCF(
                    "LocalVariableExtractor.extractAndApply: "
                            + "Invalid variable and element passed to "
                            + this.getClass().getName()
                            + "::extractAndApply ("
                            + type
                            + ", "
                            + element);
        }

        final Map<TargetClass, List<Attribute.TypeCompound>> targetClassToAnno =
                sift(getRawTypeAttributes());

        handleInvalid(targetClassToAnno.get(TargetClass.INVALID));
        handleValid(targetClassToAnno.get(TargetClass.VALID));
        handleTargeted(targetClassToAnno.get(TargetClass.TARGETED));
    }
}
