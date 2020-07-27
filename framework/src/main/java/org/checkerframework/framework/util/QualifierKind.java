package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.Set;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.javacutil.BugInCF;

/**
 * Represents a kind of qualifier, which is an annotation class. If two qualifiers use the same
 * annotation class, then they have the same qualifier kind. Two qualifiers can have the same "kind"
 * of qualifier but not be the same qualifier; an example is {@code @IndexFor("a")} and
 * {@code @IndexFor("b")}.
 *
 * <p>A {@code QualifierKind} holds information about the relationship between itself and other
 * {@link QualifierKind}s.
 *
 * <p>Exactly one qualifier kind is created for each annotation class.
 *
 * <p>QualifierKinds are like a enum in that there are immutable after initialization and only a
 * finite number per type system exist.
 */
@AnnotatedFor("nullness")
public @Interned class QualifierKind implements Comparable<QualifierKind> {

    /** The canonical name of the annotation class of this. */
    private final @Interned String name;

    /** The annotation class for this. */
    private final Class<? extends Annotation> clazz;

    /** The top of the hierarchy to which this belongs. */
    // Set while creating the QualifierKindHierarchy.
    @MonotonicNonNull QualifierKind top;

    /** The bottom of the hierarchy to which this belongs. */
    // Set while creating the QualifierKindHierarchy.
    @MonotonicNonNull QualifierKind bottom;

    /** The polymorphic qualifier of the hierarchy to which this belongs. */
    // Set while creating the QualifierKindHierarchy.
    @Nullable QualifierKind poly;

    /** Whether or not the annotation class of this has annotation elements. */
    private final boolean hasElements;

    /**
     * All the qualifier kinds that are a strict super qualifier kind of this. Does not include this
     * qualifier kind itself.
     */
    // Set while creating the QualifierKindHierarchy.
    @MonotonicNonNull Set<QualifierKind> strictSuperTypes;

    /**
     * Creates a {@link QualifierKind} for the given annotation class.
     *
     * @param clazz annotation class for a qualifier
     */
    QualifierKind(Class<? extends Annotation> clazz) {
        this.clazz = clazz;
        this.hasElements = clazz.getDeclaredMethods().length != 0;
        this.name = QualifierKindHierarchy.annotationClassName(clazz).intern();
        this.poly = null;
    }

    /**
     * Returns the canonical name of the annotation class of this.
     *
     * @return the canonical name of the annotation class of this
     */
    public @Interned String getName() {
        return name;
    }

    /**
     * Returns the annotation class for this.
     *
     * @return the annotation class for this
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return clazz;
    }

    /**
     * Returns the top qualifier kind of the hierarchy to which this qualifier kind belongs.
     *
     * @return the top qualifier kind of the hierarchy to which this qualifier kind belongs
     */
    public QualifierKind getTop() {
        if (top == null) {
            throw new BugInCF("Top is null for QualifierKind %s.", name);
        }
        return top;
    }

    /**
     * Returns whether or not this is the top qualifier of its hierarchy.
     *
     * @return true if this is the top qualifier of its hierarchy
     */
    public boolean isTop() {
        return this.top == this;
    }

    /**
     * Returns the bottom qualifier kind of the hierarchy to which this qualifier kind belongs.
     *
     * @return the bottom qualifier kind of the hierarchy to which this qualifier kind belongs
     */
    public QualifierKind getBottom() {
        if (bottom == null) {
            throw new BugInCF("Bottom is null for QualifierKind %s.", name);
        }
        return bottom;
    }

    /**
     * Returns whether or not this is the bottom qualifier of its hierarchy.
     *
     * @return true if this is the bottom qualifier of its hierarchy
     */
    public boolean isBottom() {
        return this.bottom == this;
    }

    /**
     * Returns the polymorphic qualifier kind of the hierarchy to which this qualifier kind belongs
     * or null if one does not exist.
     *
     * @return the polymorphic qualifier kind of the hierarchy to which this qualifier kind belongs
     *     or null if one does not exist
     */
    public @Nullable QualifierKind getPolymorphic() {
        return poly;
    }

    /**
     * Returns whether or not this is polymorphic.
     *
     * @return true if this is polymorphic
     */
    public boolean isPoly() {
        return this.poly == this;
    }

    /**
     * Returns whether or not the annotation class this qualifier kind represents has annotation
     * elements.
     *
     * @return true if the annotation class this qualifier kind represents has elements
     */
    public boolean hasElements() {
        return hasElements;
    }

    /**
     * All the qualifier kinds that are a strict super qualifier of this qualifier. Does not include
     * this qualifier kind itself.
     *
     * @return all the qualifier kinds that are a strict super qualifier of this qualifier
     */
    public Set<QualifierKind> getStrictSuperTypes() {
        assert strictSuperTypes != null
                : "@AssumeAssertion(nullness): strictSuperTypes must be nonnull.";
        return strictSuperTypes;
    }

    /**
     * Returns whether or not this and {@code other} are in the same hierarchy.
     *
     * @param other a qualifier kind
     * @return true if this and {@code other} are in the same hierarchy
     */
    public boolean isInSameHierarchyAs(QualifierKind other) {
        return this.top == other.top;
    }

    /**
     * Returns whether or not this qualifier kind is a subtype of or equal to {@code superQualKind}.
     *
     * @param superQualKind other qualifier kind
     * @return true if this qualifier kind is a subtype of or equal to {@code superQualKind}
     */
    public boolean isSubtype(QualifierKind superQualKind) {
        assert strictSuperTypes != null
                : "@AssumeAssertion(nullness): strictSuperTypes must be nonnull.";
        return this == superQualKind || strictSuperTypes.contains(superQualKind);
    }

    @Override
    public int compareTo(QualifierKind o) {
        return this.name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return clazz.getSimpleName();
    }
}
