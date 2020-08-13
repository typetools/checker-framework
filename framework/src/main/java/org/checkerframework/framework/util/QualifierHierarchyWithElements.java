package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

/**
 * A {@link QualifierHierarchy} where qualifiers may be represented by annotations with elements.
 *
 * <p>For cases where the annotations have no elements, the {@link
 * org.checkerframework.framework.qual.SubtypeOf} meta-annotation is used.
 *
 * <p>QualifierHierarchyWithElements uses a {@link QualifierKindHierarchy} to model the
 * relationships between qualifiers. Subclasses can override {@link
 * #createQualifierKindHierarchy(Collection)} to return a subclass of QualifierKindHierarchy.
 */
@AnnotatedFor("nullness")
public abstract class QualifierHierarchyWithElements implements QualifierHierarchy {

    /** {@link org.checkerframework.javacutil.ElementUtils} */
    protected Elements elements;

    /** {@link QualifierKindHierarchy}. */
    protected final QualifierKindHierarchy qualifierKindHierarchy;

    /** A mapping from top QualifierKinds to their corresponding AnnotationMirror. */
    protected final Map<QualifierKind, AnnotationMirror> topsMap;

    /** The set of top annotation mirrors. */
    protected final Set<AnnotationMirror> tops;

    /** A mapping from bottom QualifierKinds to their corresponding AnnotationMirror. */
    protected final Map<QualifierKind, AnnotationMirror> bottomsMap;

    /** The set of bottom annotation mirrors. */
    protected final Set<AnnotationMirror> bottoms;

    /** A mapping from polymorphic QualifierKinds to their corresponding AnnotationMirror. */
    protected final Map<QualifierKind, AnnotationMirror> polysMap;

    /**
     * Creates a QualifierHierarchy from the given classes.
     *
     * @param qualifierClasses class of annotations that are the qualifiers
     * @param elements element utils
     */
    protected QualifierHierarchyWithElements(
            Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
        this.elements = elements;
        this.qualifierKindHierarchy = createQualifierKindHierarchy(qualifierClasses);

        this.topsMap = Collections.unmodifiableMap(createTopsMap());
        Set<AnnotationMirror> tops = AnnotationUtils.createAnnotationSet();
        tops.addAll(topsMap.values());
        this.tops = Collections.unmodifiableSet(tops);

        this.bottomsMap = Collections.unmodifiableMap(createBottomsMap());
        Set<AnnotationMirror> bottoms = AnnotationUtils.createAnnotationSet();
        bottoms.addAll(bottomsMap.values());
        this.bottoms = Collections.unmodifiableSet(bottoms);

        this.polysMap = Collections.unmodifiableMap(createPolysMap());
    }

    /**
     * Create the {@link QualifierKindHierarchy}. (Subclasses may override to return a subclass of
     * QualifierKindHierarchy.)
     *
     * @param qualifierClasses class of annotations that are the qualifiers
     * @return the newly created qualifier kind hierarchy
     */
    protected QualifierKindHierarchy createQualifierKindHierarchy(
            @UnderInitialization QualifierHierarchyWithElements this,
            Collection<Class<? extends Annotation>> qualifierClasses) {
        return new DefaultQualifierKindHierarchy(qualifierClasses);
    }

    /**
     * Creates a mapping from QualifierKind to AnnotationMirror, where the QualifierKind is top and
     * the AnnotationMirror is top in their respective hierarchies.
     *
     * <p>This implementation works if the top annotation has no elements, or it has elements,
     * provides a default, and that default is the top. Otherwise, subclasses must override this.
     *
     * @return a mapping from top QualifierKind to top AnnotationMirror
     */
    @RequiresNonNull({"this.qualifierKindHierarchy", "this.elements"})
    protected Map<QualifierKind, AnnotationMirror> createTopsMap(
            @UnderInitialization QualifierHierarchyWithElements this) {
        Map<QualifierKind, AnnotationMirror> topsMap = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.getTops()) {
            topsMap.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
        }
        return topsMap;
    }

    /**
     * Creates a mapping from QualifierKind to AnnotationMirror, where the QualifierKind is bottom
     * and the AnnotationMirror is bottom in their respective hierarchies.
     *
     * <p>This implementation works if the bottom annotation has no elements, or it has elements,
     * provides a default, and that default is the bottom. Otherwise, subclasses must override this.
     *
     * @return a mapping from bottom QualifierKind to bottom AnnotationMirror
     */
    @RequiresNonNull({"this.qualifierKindHierarchy", "this.elements"})
    protected Map<QualifierKind, AnnotationMirror> createBottomsMap(
            @UnderInitialization QualifierHierarchyWithElements this) {
        Map<QualifierKind, AnnotationMirror> bottomsMap = new TreeMap<>();
        for (QualifierKind kind : qualifierKindHierarchy.getBottoms()) {
            bottomsMap.put(kind, AnnotationBuilder.fromClass(elements, kind.getAnnotationClass()));
        }
        return bottomsMap;
    }

    /**
     * Creates a mapping from QualifierKind to AnnotationMirror, where the QualifierKind is a kind
     * of polymorphic qualifier and the AnnotationMirror is the polymorphic qualifier, in their
     * respective hierarchies.
     *
     * <p>This implementation works if the polymorphic annotation has no elements, or it has
     * elements, provides a default, and that default is the polymorphic annotation. Otherwise,
     * subclasses must override this.
     *
     * @return a mapping from polymorphic QualifierKind to polymorphic AnnotationMirror
     */
    @RequiresNonNull({"this.qualifierKindHierarchy", "this.elements"})
    protected Map<QualifierKind, AnnotationMirror> createPolysMap(
            @UnderInitialization QualifierHierarchyWithElements this) {
        Map<QualifierKind, AnnotationMirror> polyMap = new TreeMap<>();
        for (QualifierKind top : qualifierKindHierarchy.getTops()) {
            QualifierKind polyKind = top.getPolymorphic();
            if (polyKind != null) {
                polyMap.put(
                        polyKind,
                        AnnotationBuilder.fromClass(elements, polyKind.getAnnotationClass()));
            }
        }
        return polyMap;
    }

    /**
     * Returns the qualifier kind for the given annotation.
     *
     * @param anno annotation mirror
     * @return the qualifier kind for the given annotation
     */
    protected QualifierKind getQualifierKind(AnnotationMirror anno) {
        String name = AnnotationUtils.annotationName(anno);
        return getQualifierKind(name);
    }

    /**
     * Returns the qualifier kind for the annotation with the fully qualified name {@code name}.
     *
     * @param name fully qualified annotation name
     * @return the qualifier kind for the annotation with {@code name}
     */
    protected QualifierKind getQualifierKind(String name) {
        QualifierKind kind = qualifierKindHierarchy.getQualifierKind(name);
        if (kind == null) {
            throw new BugInCF("QualifierKind not in hierarchy: %s", name);
        }
        return kind;
    }

    @Override
    public Set<? extends AnnotationMirror> getTopAnnotations() {
        return tops;
    }

    @Override
    // All tops are a key for topsMap.
    @SuppressWarnings("nullness:return.type.incompatible")
    public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        return topsMap.get(kind.getTop());
    }

    @Override
    public Set<? extends AnnotationMirror> getBottomAnnotations() {
        return bottoms;
    }

    @Override
    public @Nullable AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        return polysMap.get(kind);
    }

    @Override
    public boolean isPolymorphicQualifier(AnnotationMirror qualifier) {
        return getQualifierKind(qualifier).isPoly();
    }

    @Override
    // All bottoms are keys for bottomsMap.
    @SuppressWarnings("nullness:return.type.incompatible")
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
        QualifierKind kind = getQualifierKind(start);
        return bottomsMap.get(kind.getBottom());
    }
}
