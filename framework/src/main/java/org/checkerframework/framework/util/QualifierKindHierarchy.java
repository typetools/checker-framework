package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TypeSystemError;

/**
 * This class holds information about the subtyping relationships between kinds of qualifiers. A
 * "kind" of qualifier is its annotation class and is represented by the {@link QualifierKind}
 * class. If a type system has more than one hierarchy, information about all hierarchies are stored
 * in this class.
 *
 * <p>The qualifier kind subtyping relationship may be an over-approximation of the qualifier
 * subtyping relationship, for qualifiers that have elements/arguments. In other words, if a
 * qualifier kind is a subtype of another qualifier kind, then qualifiers of those kinds may or may
 * not be subtypes, depending on the value of any elements of the qualifiers.
 *
 * <p>By default, the subtyping information and information about polymorphic qualifiers is read
 * from meta-annotations on the annotation classes. This information is used to infer further
 * information such as top and bottom qualifiers. Subclasses can override the following methods to
 * change this behavior:
 *
 * <ul>
 *   <li>{@link #createQualifierKinds(Collection)}
 *   <li>{@link #createDirectSuperMap()}
 *   <li>{@link #specifyBottom(Map, Class)}
 *   <li>{@link #initializePolymorphicQualifiers()}
 *   <li>{@link #initializeQualifierKindFields(Map)}
 *   <li>{@link #createLubsMap()}
 *   <li>{@link #createGlbsMap()}
 * </ul>
 *
 * This class is used by {@link SimpleQualifierHierarchy} and {@link ComplexQualifierHierarchy} to
 * implement methods that compare {@link javax.lang.model.element.AnnotationMirror}s, such as {@link
 * org.checkerframework.framework.type.QualifierHierarchy#isSubtype(AnnotationMirror,
 * AnnotationMirror)}.
 */
public class QualifierKindHierarchy {

    /**
     * Represents a kind of qualifier, which is an annotation class. If two qualifiers use the same
     * annotation class, then they have the same qualifier kind. Two qualifiers can have the same
     * "kind" of qualifier but not be the same qualifier; an example is {@code @IndexFor("a")} and
     * {@code @IndexFor("b")}.
     *
     * <p>Exactly one qualifier kind is created for each annotation class.
     *
     * <p>A {@code QualifierKind} holds information about the relationship between itself and other
     * {@link QualifierKind}s.
     */
    // The private non-final fields of this class are set while creating the QualifierKindHierarchy.
    public static @Interned class QualifierKind implements Comparable<QualifierKind> {

        /** The canonical name of the annotation class of this. */
        private final @Interned String name;

        /** The annotation class for this. */
        private final Class<? extends Annotation> clazz;

        /** Whether or not this has elements. */
        private final boolean hasElements;

        /** Whether or not this is polymorphic. */
        private boolean isPoly;

        /**
         * All the qualifier kinds that are a strict super qualifier kind of this. Does not include
         * this qualifier kind itself.
         */
        private Set<QualifierKind> strictSuperTypes;

        /** The top of the hierarchy to which this belongs. */
        private QualifierKind top;

        /** The bottom of the hierarchy to which this belongs. */
        private QualifierKind bottom;

        /**
         * Creates a {@link QualifierKind} for the given annotation class.
         *
         * @param clazz annotation class for a qualifier
         */
        private QualifierKind(Class<? extends Annotation> clazz) {
            this.clazz = clazz;
            this.hasElements = clazz.getDeclaredMethods().length != 0;
            this.name = clazz.getCanonicalName().intern();
            // These non-final fields are set by QualifierKindHierarchy.
            // Give them dummy values here, because the Java compiler requires it.
            isPoly = false;
            strictSuperTypes = null;
            top = null;
            bottom = null;
        }

        /**
         * Returns the canonical name of the annotation class of this.
         *
         * @return the canonical name of the annotation class of this
         */
        public String getName() {
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
         * Returns whether or not this is polymorphic.
         *
         * @return true if this is polymorphic
         */
        public boolean isPoly() {
            return isPoly;
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
         * Returns whether or not this is the bottom qualifier of its hierarchy.
         *
         * @return true if this is the bottom qualifier of its hierarchy
         */
        public boolean isBottom() {
            return this.bottom == this;
        }

        /**
         * All the qualifier kinds that are a strict super qualifier of this qualifier. Does not
         * include this qualifier kind itself.
         *
         * @return all the qualifier kinds that are a strict super qualifier of this qualifier
         */
        public Set<QualifierKind> getStrictSuperTypes() {
            return strictSuperTypes;
        }

        /**
         * Returns the top qualifier kind of the hierarchy to which this qualifier kind belongs.
         *
         * @return the top qualifier kind of the hierarchy to which this qualifier kind belongs
         */
        public QualifierKind getTop() {
            return top;
        }

        /**
         * Returns the bottom qualifier kind of the hierarchy to which this qualifier kind belongs.
         *
         * @return the bottom qualifier kind of the hierarchy to which this qualifier kind belongs
         */
        public QualifierKind getBottom() {
            return bottom;
        }

        /**
         * Returns whether or not the annotation class this qualifier kind represents has elements.
         *
         * @return true if the annotation class this qualifier kind represents has elements
         */
        public boolean hasElements() {
            return hasElements;
        }

        /**
         * Returns whether or not this and {@code other} are in the same hierarchy.
         *
         * @param other a qualifier kind
         * @return true if this and {@code other} are in the same hierarchy
         */
        public boolean areInSameHierarchy(QualifierKind other) {
            return this.top == other.top;
        }

        /**
         * Returns whether or not this qualifier kind is a subtype of or equal to {@code
         * superQualKind}.
         *
         * @param superQualKind other qualifier kind
         * @return true if this qualifier kind is a subtype of or equal to {@code superQualKind}
         */
        public boolean isSubtype(QualifierKind superQualKind) {
            return this == superQualKind || strictSuperTypes.contains(superQualKind);
        }

        @Override
        public int compareTo(QualifierKind o) {
            return this.name.compareTo(o.name);
        }

        @Override
        public String toString() {
            return name.substring(name.lastIndexOf(".") + 1);
        }
    }

    // End of QualifierKind class, start of QualifierKindHierarchy fields.

    /**
     * A mapping from canonical name of a qualifier class to the QualifierKind object representing
     * that class.
     */
    private final NavigableMap<@Interned String, QualifierKind> nameToQualifierKind;

    /**
     * A mapping from a top qualifier kind to the polymorphic qualifier kind in the same hierarchy.
     */
    private final Map<QualifierKind, QualifierKind> topToPoly;

    /** All the qualifier kinds that are the top qualifier in their hierarchy. */
    private final Set<QualifierKind> tops;

    /** All the qualifier kinds that are the bottom qualifier in their hierarchy. */
    private final Set<QualifierKind> bottoms;

    /**
     * Holds the lub of qualifier kinds. {@code lubs.get(kind1).get(kind2)} returns the lub of kind1
     * and kind2.
     */
    private final Map<QualifierKind, Map<QualifierKind, QualifierKind>> lubs;

    /**
     * Holds the glb of qualifier kinds. {@code lubs.get(kind1).get(kind2)} returns the glb of kind1
     * and kind2.
     */
    private final Map<QualifierKind, Map<QualifierKind, QualifierKind>> glbs;

    /**
     * Returns the qualifier kinds that are the top qualifier in their hierarchies.
     *
     * @return the qualifier kinds that are the top qualifier in their hierarchies
     */
    public Set<QualifierKind> getTops() {
        return tops;
    }

    /**
     * Returns the qualifier kinds that are the bottom qualifier in their hierarchies.
     *
     * @return the qualifier kinds that are the bottom qualifier in their hierarchies
     */
    public Set<QualifierKind> getBottoms() {
        return bottoms;
    }

    /**
     * Returns the least upper bound of {@code q1} and {@code q2}. Ignores elements/arguments (as
     * QualifierKind always does).
     *
     * @param q1 a qualifier kind
     * @param q2 a qualifier kind
     * @return the least upper bound of {@code q1} and {@code q2}
     */
    public QualifierKind leastUpperBound(QualifierKind q1, QualifierKind q2) {
        return lubs.get(q1).get(q2);
    }

    /**
     * Returns the greatest lower bound of {@code q1} and {@code q2}. Ignores elements/arguments (as
     * QualifierKind always does).
     *
     * @param q1 a qualifier kind
     * @param q2 a qualifier kind
     * @return the greatest lower bound of {@code q1} and {@code q2}
     */
    public QualifierKind greatestLowerBound(QualifierKind q1, QualifierKind q2) {
        return glbs.get(q1).get(q2);
    }

    /**
     * Returns a collection of all {@link QualifierKind}s sorted in ascending order.
     *
     * @return a collection of all {@link QualifierKind}s sorted in ascending order
     */
    public Collection<QualifierKind> allQualifierKinds() {
        return nameToQualifierKind.values();
    }

    /**
     * Returns the {@link QualifierKind} for the given annotation class name or null if one does not
     * exist.
     *
     * @param name canonical name of an annotation class
     * @return the {@link QualifierKind} for the given annotation class name or null if one does not
     *     exist
     */
    public @Nullable QualifierKind getQualifierKind(String name) {
        return nameToQualifierKind.get(name);
    }

    /**
     * The mapping from a top qualifier kind to the polymorphic qualifier kind in the same
     * hierarchy.
     *
     * @return the mapping from a top qualifier kind to the polymorphic qualifier kind in the same
     *     hierarchy
     */
    public Map<QualifierKind, QualifierKind> getTopToPoly() {
        return topToPoly;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// <editor-fold desc="Initialize hierarchy">
    /// Methods that initialize the hierarchy; only called at object initialization.
    ///

    /**
     * Creates a {@link QualifierKindHierarchy}. Also, creates and initializes all its qualifier
     * kinds.
     *
     * @param qualifierClasses all the classes of qualifiers supported by this hierarchy
     */
    public QualifierKindHierarchy(Collection<Class<? extends Annotation>> qualifierClasses) {
        this.nameToQualifierKind = createQualifierKinds(qualifierClasses);
        Map<QualifierKind, Set<QualifierKind>> directSuperMap = createDirectSuperMap();
        specifyBottom(directSuperMap, null);
        this.tops = createTopsSet(directSuperMap);
        this.bottoms = createBottomsSet(directSuperMap);
        this.topToPoly = initializePolymorphicQualifiers();
        initializeQualifierKindFields(directSuperMap);
        this.lubs = createLubsMap();
        this.glbs = createGlbsMap();

        verifyHierarchy(directSuperMap);
    }

    /**
     * Verifies that the {@link QualifierKindHierarchy} is a valid hierarchy.
     *
     * @param directSuperMap mapping from qualifier to its direct supertypes; used to verify that a
     *     polymorphic annotation does not have a {@link SubtypeOf} meta-annotation
     * @throws TypeSystemError if the hierarchy isn't valid
     */
    protected void verifyHierarchy(Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        for (QualifierKind qualifierKind : nameToQualifierKind.values()) {
            boolean isPoly = qualifierKind.isPoly;
            boolean hasSubtypeOfAnno = directSuperMap.containsKey(qualifierKind);
            if (isPoly && hasSubtypeOfAnno) {
                // Polymorphic qualifiers with upper and lower bounds are currently not supported.
                throw new TypeSystemError(
                        "AnnotatedTypeFactory: "
                                + qualifierKind
                                + " is polymorphic and specifies super qualifiers.%n"
                                + "Remove the @PolymorphicQualifier or @SubtypeOf annotation from it.");
            } else if (!isPoly && !hasSubtypeOfAnno) {
                throw new TypeSystemError(
                        "AnnotatedTypeFactory: %s does not specify its super qualifiers.%n"
                                + "Add an @SubtypeOf or @PolymorphicQualifier annotation to it,%n"
                                + "or if it is an alias, exclude it from `createSupportedTypeQualifiers()`.%n",
                        qualifierKind);
            } else if (isPoly) {
                if (qualifierKind.top == null) {
                    throw new TypeSystemError(
                            "PolymorphicQualifier, %s, has to specify a type hierarchy in its @PolymorphicQualifier meta-annotation, if more than one exists; top types: [%s].",
                            qualifierKind, SystemUtil.join(", ", tops));
                } else if (!tops.contains(qualifierKind.top)) {
                    throw new TypeSystemError(
                            "Polymorphic qualifier %s has invalid top %s. Top qualifiers: %s",
                            qualifierKind, qualifierKind.top, SystemUtil.join(", ", tops));
                }
            }
        }

        if (bottoms.size() != tops.size()) {
            throw new TypeSystemError(
                    "Number of tops not equal to number of bottoms: Tops: [%s] Bottoms: [%s]",
                    SystemUtil.join(", ", tops), SystemUtil.join(", ", bottoms));
        }
    }

    /**
     * Creates all QualifierKind objects for the given qualifier classes and adds them to
     * qualifierClassMap. This method does not initialize all fields in the {@link QualifierKind};
     * that is done by {@link #initializeQualifierKindFields(Map)}.
     *
     * @param qualifierClasses classes of annotations that are type qualifiers
     * @return a mapping from the canonical name of an annotation class to {@link QualifierKind}
     */
    protected NavigableMap<@Interned String, QualifierKind> createQualifierKinds(
            Collection<Class<? extends Annotation>> qualifierClasses) {
        TreeMap<@Interned String, QualifierKind> nameToQualifierKind = new TreeMap<>();
        for (Class<? extends Annotation> clazz : qualifierClasses) {
            @SuppressWarnings("interning") // uniqueness is tested immediately below
            @Interned QualifierKind qualifierKind = new QualifierKind(clazz);
            if (nameToQualifierKind.containsKey(qualifierKind.name)) {
                throw new TypeSystemError("Duplicate QualifierKind " + qualifierKind.name);
            }
            nameToQualifierKind.put(qualifierKind.name, qualifierKind);
        }
        return Collections.unmodifiableNavigableMap(nameToQualifierKind);
    }

    /**
     * Creates a mapping from a {@link QualifierKind} to a set of its direct super qualifier kinds.
     * The direct super qualifier kinds do not contain the qualifier itself. This mapping is used by
     * {@link #createBottomsSet(Map)}, {@link #createTopsSet(Map)}, and {@link
     * #initializeQualifierKindFields(Map)}.
     *
     * <p>This implementation uses the {@link SubtypeOf} meta-annotation. Subclasses may override
     * this method to create the direct super map some other way.
     *
     * <p>Note that this method is called from the constructor when only {@link
     * #nameToQualifierKind} has been initialized. This method is not static, so it can be
     * overridden by subclasses.
     *
     * @return a mapping from each {@link QualifierKind} to a set of its direct super qualifiers
     */
    protected Map<QualifierKind, Set<QualifierKind>> createDirectSuperMap() {
        Map<QualifierKind, Set<QualifierKind>> directSuperMap = new TreeMap<>();
        for (QualifierKind qualifierKind : nameToQualifierKind.values()) {
            SubtypeOf subtypeOfMetaAnno = qualifierKind.clazz.getAnnotation(SubtypeOf.class);
            if (subtypeOfMetaAnno != null) {
                Set<QualifierKind> directSupers = new TreeSet<>();
                directSuperMap.put(qualifierKind, directSupers);
                for (Class<? extends Annotation> superClazz : subtypeOfMetaAnno.value()) {
                    String superName = superClazz.getCanonicalName();
                    QualifierKind superQualifier = nameToQualifierKind.get(superName);
                    if (superQualifier == null) {
                        throw new TypeSystemError(
                                "%s @Subtype argument %s isn't in the hierarchy. Qualifiers: [%s]",
                                qualifierKind,
                                superName,
                                SystemUtil.join(", ", nameToQualifierKind.values()));
                    }
                    directSupers.add(superQualifier);
                }
            }
        }
        return directSuperMap;
    }

    /**
     * Set {@code bottom} to the given class and modify {@code directSuperMap} to add all leaves to
     * its super qualifier kinds. Leaves are qualifier kinds that are not super qualifier kinds of
     * another qualifier kind.
     *
     * <p>For some type systems, qualifiers may be added at run time, so the {@link SubtypeOf}
     * meta-annotation on the bottom qualifier class cannot specify all other qualifiers. For those
     * type systems, override this method and call super with the bottom class. For example,
     *
     * <pre>
     * {@code @Override
     *  protected void specifyBottom(Map<QualifierKind, Set<QualifierKind>> directSuperMap, Class<? extends Annotation> bottom) {
     *      super.specifyBottom(directSuperMap, UnitsBottom.class);
     *  }
     * }</pre>
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifiers; side-effected by this method
     * @param bottom the class of the bottom qualifier or {@code null}; if {@code null}, this method
     *     has no effect
     */
    protected void specifyBottom(
            Map<QualifierKind, Set<QualifierKind>> directSuperMap,
            @Nullable Class<? extends Annotation> bottom) {
        if (bottom == null) {
            return;
        }

        QualifierKind bottomKind = getQualifierKind(bottom.getCanonicalName());
        if (bottomKind == null) {
            throw new TypeSystemError(
                    "QualifierKindHierarchy#specifyBottom: the given bottom class, %s, is not in the hierarchy.",
                    bottom.getCanonicalName());
        }
        Set<QualifierKind> currentLeaves = new TreeSet<>(allQualifierKinds());
        currentLeaves.remove(bottomKind);
        directSuperMap.forEach(
                (sub, supers) -> {
                    currentLeaves.removeAll(supers);
                });
        Set<QualifierKind> bottomDirectSuperQuals = directSuperMap.get(bottomKind);
        bottomDirectSuperQuals.addAll(currentLeaves);
    }

    /**
     * Creates the set of top {@link QualifierKind}s by searching {@code directSuperMap} for
     * qualifier kinds without any direct super qualifier kinds.
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifier kinds; created by {@link #createDirectSuperMap()}
     * @return the set of top {@link QualifierKind}s
     */
    // Subclasses should override createDirectSuperMap to change the tops and not this method,
    // because other methods expect the directSuperMap to be complete.
    private Set<QualifierKind> createTopsSet(
            Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        Set<QualifierKind> tops = new TreeSet<>();
        directSuperMap.forEach(
                (qualifierKind, superQuals) -> {
                    if (superQuals.isEmpty()) {
                        tops.add(qualifierKind);
                    }
                });
        return tops;
    }

    /**
     * Creates the set of bottom {@link QualifierKind}s by searching {@code directSuperMap} for
     * qualifiers that are not a direct super qualifier kind of another qualifier kind.
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifier kinds; created by {@link #createDirectSuperMap()}
     * @return the set of bottom {@link QualifierKind}s
     */
    // Subclasses should override createDirectSuperMap or specifyBottom to change the bottoms and
    // not this method, because other methods expect the directSuperMap to be complete.
    private Set<QualifierKind> createBottomsSet(
            Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        Set<QualifierKind> bottoms = new HashSet<>(directSuperMap.keySet());
        for (Set<QualifierKind> superKinds : directSuperMap.values()) {
            bottoms.removeAll(superKinds);
        }
        return bottoms;
    }

    /**
     * Iterates over all the qualifier kinds and adds all polymorphic qualifier kinds to
     * polymorphicQualifiers. Also sets {@link QualifierKind#isPoly} to true and {@link
     * QualifierKind#top} to top for the polymophic qualifiers.
     *
     * <p>Requires that tops has been initialized.
     *
     * @return a mapping from a top qualifier to the polymorphic qualifier in the heirarchy
     */
    protected Map<QualifierKind, QualifierKind> initializePolymorphicQualifiers() {
        Map<QualifierKind, QualifierKind> topToPoly = new TreeMap<>();
        for (QualifierKind qualifierKind : nameToQualifierKind.values()) {
            Class<? extends Annotation> clazz = qualifierKind.getAnnotationClass();
            PolymorphicQualifier polyMetaAnno = clazz.getAnnotation(PolymorphicQualifier.class);
            if (polyMetaAnno == null) {
                continue;
            }
            qualifierKind.isPoly = true;
            String topName = polyMetaAnno.value().getCanonicalName();
            if (nameToQualifierKind.containsKey(topName)) {
                qualifierKind.top = nameToQualifierKind.get(topName);
            } else if (topName.equals(Annotation.class.getCanonicalName())) {
                // Annotation.class is the default value of PolymorphicQualifier. If it is used,
                // then there must be exactly one top.
                if (tops.size() == 1) {
                    qualifierKind.top = tops.iterator().next();
                } else {
                    throw new TypeSystemError(
                            "Polymorphic qualifier %s did not specify a top annotation class. Tops: [%s]",
                            qualifierKind, SystemUtil.join(", ", tops));
                }
            } else {
                throw new TypeSystemError(
                        "Polymorphic qualifier %s's top, %s, is not a qualifier.",
                        qualifierKind, topName);
            }
            qualifierKind.strictSuperTypes = Collections.singleton(qualifierKind.top);
            topToPoly.put(qualifierKind.top, qualifierKind);
        }
        return topToPoly;
    }

    /**
     * For each qualifier kind in {@code directSuperMap}, initializes {@link
     * QualifierKind#strictSuperTypes}, {@link QualifierKind#top} and {@link QualifierKind#bottom}.
     * (Requires tops, bottoms, and polymorphicQualifiers to be initialized.)
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifier kinds; created by {@link #createDirectSuperMap()}
     */
    protected void initializeQualifierKindFields(
            Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        for (QualifierKind qualifierKind : nameToQualifierKind.values()) {
            if (!qualifierKind.isPoly) {
                qualifierKind.strictSuperTypes = findAllTheSupers(qualifierKind, directSuperMap);
            }
        }
        for (QualifierKind qualifierKind : nameToQualifierKind.values()) {
            for (QualifierKind top : tops) {
                if (qualifierKind.isSubtype(top)) {
                    if (qualifierKind.top == null) {
                        qualifierKind.top = top;
                    } else if (qualifierKind.top != top) {
                        throw new TypeSystemError(
                                "Multiple tops found for qualifier %s. Tops: %s and %s.",
                                qualifierKind, top, qualifierKind.top);
                    }
                }
            }
            if (qualifierKind.top == null) {
                throw new TypeSystemError(
                        "Qualifier %s isn't a subtype of any top. tops = %s", qualifierKind, tops);
            }
        }
        for (QualifierKind qualifierKind : nameToQualifierKind.values()) {
            for (QualifierKind bot : bottoms) {
                if (bot.top != qualifierKind.top) {
                    continue;
                }
                if (qualifierKind.bottom == null) {
                    qualifierKind.bottom = bot;
                } else if (qualifierKind.top != bot) {
                    throw new TypeSystemError(
                            "Multiple bottoms found for qualifier %s. Bottoms: %s and %s.",
                            qualifierKind, bot, qualifierKind.bottom);
                }
                if (qualifierKind.isPoly) {
                    bot.strictSuperTypes.add(qualifierKind);
                }
            }
        }
    }

    /**
     * Returns the set of all qualifier kinds that are a strict supertype of {@code qualifierKind}.
     *
     * @param qualifierKind the qualifier kind whose super types should be returned
     * @param directSuperMap directSuperMap
     * @return the set of all qualifier kinds that are a strict supertype of {@code qualifierKind}
     */
    private Set<QualifierKind> findAllTheSupers(
            QualifierKind qualifierKind, Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        Queue<QualifierKind> queue = new ArrayDeque<>(directSuperMap.get(qualifierKind));
        Set<QualifierKind> allSupers = new TreeSet<>(directSuperMap.get(qualifierKind));
        Set<QualifierKind> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            QualifierKind superQual = queue.remove();
            if (superQual == qualifierKind) {
                throw new TypeSystemError("Cycle in hierarchy: %s", qualifierKind);
            }
            if (visited.contains(superQual)) {
                continue;
            }
            visited.add(superQual);
            if (superQual.isPoly) {
                continue;
            }
            queue.addAll(directSuperMap.get(superQual));
            allSupers.addAll(directSuperMap.get(superQual));
        }
        return allSupers;
    }

    /**
     * Creates the lub of qualifier kinds. {@code lubs.get(kind1).get(kind2)} returns the lub of
     * kind1 and kind2.
     *
     * @return a mapping of lubs
     */
    protected Map<QualifierKind, Map<QualifierKind, QualifierKind>> createLubsMap() {
        Map<QualifierKind, Map<QualifierKind, QualifierKind>> lubs = new HashMap<>();
        for (QualifierKind qual1 : nameToQualifierKind.values()) {
            for (QualifierKind qual2 : nameToQualifierKind.values()) {
                if (qual1.top != qual2.top) {
                    continue;
                }
                QualifierKind lub = findLub(qual1, qual2);
                addToMapOfMap(lubs, qual1, qual2, lub, "lub");
                addToMapOfMap(lubs, qual2, qual1, lub, "lub");
            }
        }
        return lubs;
    }

    /**
     * Returns the least upper bound of {@code qual1} and {@code qual2}.
     *
     * @param qual1 a qualifier kind
     * @param qual2 a qualifier kind
     * @return the least upper bound of {@code qual1} and {@code qual2}
     */
    private QualifierKind findLub(QualifierKind qual1, QualifierKind qual2) {
        if (qual1 == qual2) {
            return qual1;
        } else if (qual1.isSubtype(qual2)) {
            return qual2;
        } else if (qual2.isSubtype(qual1)) {
            return qual1;
        }
        Set<QualifierKind> allSuperTypes = new TreeSet<>(qual1.strictSuperTypes);
        allSuperTypes.retainAll(qual2.strictSuperTypes);
        Set<QualifierKind> lubs = findLowestQualifiers(allSuperTypes);
        if (lubs.size() != 1) {
            throw new TypeSystemError(
                    "Not exactly 1 lub for %s and %s. Found lubs: [%s].",
                    qual1, qual2, SystemUtil.join(", ", lubs));
        }
        QualifierKind lub = lubs.iterator().next();
        if (lub.isPoly && !qual1.isPoly && !qual2.isPoly) {
            throw new TypeSystemError(
                    "Lub can't be poly: lub: %s, qual1: %s, qual2: %s.", lub, qual1, qual2);
        }
        return lub;
    }

    /**
     * Returns the lowest qualifiers in the passed set.
     *
     * @param qualifierKinds a set of qualifiers
     * @return the lowest qualifiers in the passed set
     */
    protected final Set<QualifierKind> findLowestQualifiers(Set<QualifierKind> qualifierKinds) {
        Set<QualifierKind> lowestQualifiers = new TreeSet<>(qualifierKinds);
        for (QualifierKind a1 : qualifierKinds) {
            lowestQualifiers.removeIf(a2 -> a1 != a2 && a1.isSubtype(a2));
        }
        return lowestQualifiers;
    }

    /**
     * Creates the glb of qualifier kinds. {@code lubs.get(kind1).get(kind2)} returns the glb of
     * kind1 and kind2.
     *
     * @return a mapping of glb
     */
    private Map<QualifierKind, Map<QualifierKind, QualifierKind>> createGlbsMap() {
        Map<QualifierKind, Map<QualifierKind, QualifierKind>> glbs = new TreeMap<>();
        for (QualifierKind qual1 : nameToQualifierKind.values()) {
            for (QualifierKind qual2 : nameToQualifierKind.values()) {
                if (qual1.top != qual2.top) {
                    continue;
                }
                QualifierKind glb = findGlb(qual1, qual2);
                addToMapOfMap(glbs, qual1, qual2, glb, "glb");
                addToMapOfMap(glbs, qual2, qual1, glb, "glb");
            }
        }
        return glbs;
    }

    /**
     * Returns the greatest lower bound of {@code qual1} and {@code qual2}.
     *
     * @param qual1 a qualifier kind
     * @param qual2 a qualifier kind
     * @return the greatest lower bound of {@code qual1} and {@code qual2}
     */
    private QualifierKind findGlb(QualifierKind qual1, QualifierKind qual2) {
        if (qual1 == qual2) {
            return qual1;
        } else if (qual1.isSubtype(qual2)) {
            return qual1;
        } else if (qual2.isSubtype(qual1)) {
            return qual2;
        }
        Set<QualifierKind> allSubTypes = new TreeSet<>();
        for (QualifierKind qualifierKind : nameToQualifierKind.values()) {
            if (qualifierKind.isSubtype(qual1) && qualifierKind.isSubtype(qual2)) {
                allSubTypes.add(qualifierKind);
            }
        }
        Set<QualifierKind> glbs = findHighestQualifiers(allSubTypes);
        if (glbs.size() != 1) {
            throw new TypeSystemError(
                    "Not exactly 1 glb for %s and %s. Found glbs: [%s].",
                    qual1, qual2, SystemUtil.join(", ", glbs));
        }
        QualifierKind glb = glbs.iterator().next();
        if (glb.isPoly && !qual1.isPoly && !qual2.isPoly) {
            throw new TypeSystemError(
                    "GLB can't be poly: lub: %s, qual1: %s, qual2: %s.", glb, qual1, qual2);
        }
        return glb;
    }

    /**
     * Returns the highest qualifiers in the passed set.
     *
     * @param qualifierKinds a set of qualifiers
     * @return the highest qualifiers in the passed set
     */
    protected final Set<QualifierKind> findHighestQualifiers(Set<QualifierKind> qualifierKinds) {
        Set<QualifierKind> lowestQualifiers = new TreeSet<>(qualifierKinds);
        for (QualifierKind a1 : qualifierKinds) {
            lowestQualifiers.removeIf(a2 -> a1 != a2 && a2.isSubtype(a1));
        }
        return lowestQualifiers;
    }

    /**
     * Add Key: qual1, Value: (Key: qual2, Value: value) to {@code map}. If already in map, throw an
     * exception if value is different.
     *
     * @param map mapping
     * @param qual1 the first qualifier kind
     * @param qual2 the second qualifier kind
     * @param value the value to add
     * @param error "lub" or "glb"; used only for error messages
     */
    private static void addToMapOfMap(
            Map<QualifierKind, Map<QualifierKind, QualifierKind>> map,
            QualifierKind qual1,
            QualifierKind qual2,
            QualifierKind value,
            String error) {
        Map<QualifierKind, QualifierKind> qual1Map =
                map.computeIfAbsent(qual1, k -> new HashMap<>());
        QualifierKind existingValue = qual1Map.get(qual2);
        if (existingValue == null) {
            qual1Map.put(qual2, value);
        } else {
            if (existingValue != value) {
                throw new TypeSystemError(
                        "Multiple %s for qualifiers %s and %s. Found map %s and %s",
                        error, qual1, qual2, value, existingValue);
            }
        }
    }
    // </editor-fold> End of methods that initialize hierarchy

}
