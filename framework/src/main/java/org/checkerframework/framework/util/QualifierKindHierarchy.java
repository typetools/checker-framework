package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.PluginUtil;
import org.checkerframework.javacutil.UserError;

/**
 * This class holds information about the relationships between annotation classes, stored as {@link
 * QualifierKind}s.
 *
 * <p>By default, the subtyping information and information about polymorphic qualifiers is read
 * from meta-annotations on the annotation classes. This information is used to infer further
 * information such as top and bottom qualifiers. Subclasses can override the following methods to
 * change this behavior:
 *
 * <ul>
 *   <li>{ @link #createQualifierKinds(Collection)}
 *   <li>{@link #createDirectSuperMap()}
 *   <li>{@link #specifyBottom(Map, Class)}
 *   <li>{@link #initializePolymorphicQualifiers()}
 *   <li>{@link #initializeQualifierKindFields(Map)}
 *   <li>{@link #createLubsMap()}
 *   <li>{@link #createGlbsMap()}
 * </ul>
 *
 * This class is used by {@link SimpleHierarchy} and {@link ComplexHierarchy} classes to implement
 * methods that compare {@link javax.lang.model.element.AnnotationMirror}s, such as {@link
 * org.checkerframework.framework.type.QualifierHierarchy#isSubtype(AnnotationMirror,
 * AnnotationMirror)}.
 */
public class QualifierKindHierarchy {

    /**
     * A class that represent a particular class of annotation that is a qualifier. It holds
     * information about the relationship between itself and other {@link QualifierKind}s.
     *
     * <p>Exactly one qualifier kind is created for each annotation class.
     */
    // The private non-final fields of this class are set while creating the QualifierKindHierarchy.
    public static @Interned class QualifierKind implements Comparable<QualifierKind> {

        /** The canonical name of the annotation class of this kind of qualifier. */
        private final @Interned String name;

        /** The annotation class for this kind of qualifier. */
        private final Class<? extends Annotation> clazz;

        /** Whether or not this kind of qualifier has elements. */
        private final boolean hasElements;

        /** Whether or not this is a polymorphic kind of qualifier. */
        private boolean isPoly;

        /**
         * All the qualifier kinds that are a super qualifier of this qualifier, except for this
         * qualifier itself.
         */
        private Set<QualifierKind> superTypes;

        /**
         * The qualifier kind that is the top of the hierarchy to which this qualifier kind belongs.
         */
        private QualifierKind top;
        /**
         * The qualifier kind that is the bottom of the hierarchy to which this qualifier kind
         * belongs.
         */
        private QualifierKind bottom;

        /**
         * Creates a {@link QualifierKind} for the give annotation class.
         *
         * @param clazz annotation class that this qualifier kind represents
         */
        private QualifierKind(Class<? extends Annotation> clazz) {
            this.clazz = clazz;
            this.hasElements = clazz.getDeclaredMethods().length != 0;
            this.name = clazz.getCanonicalName().intern();
            // The non-final fields in this class are set by QualifierKindHierarchy.
            isPoly = false;
            superTypes = null;
            top = null;
            bottom = null;
        }

        /**
         * Returns the canonical name of the annotation class of this kind of qualifier.
         *
         * @return the canonical name of the annotation class of this kind of qualifier
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the annotation class for this kind of qualifier.
         *
         * @return the annotation class for this kind of qualifier
         */
        public Class<? extends Annotation> getAnnotationClass() {
            return clazz;
        }

        /**
         * Whether or not this is a polymorphic qualifier kind.
         *
         * @return true if this is a polymorphic qualifier kind
         */
        public boolean isPoly() {
            return isPoly;
        }

        /**
         * Whether or not this qualifier is the top qualifier of its hierarchy.
         *
         * @return true if this qualifier kind is the top qualifier of its hierarchy
         */
        public boolean isTop() {
            return this.top == this;
        }

        /**
         * Whether or not this qualifier is the bottom qualifier of its hierarchy.
         *
         * @return true if this qualifier kind is the bottom qualifier of its hierarchy
         */
        public boolean isBottom() {
            return this.bottom == this;
        }

        /**
         * All the qualifier kinds that are a super qualifier of this qualifier, except for this
         * qualifier itself.
         *
         * @return all the qualifier kinds that are a super qualifier of this qualifier, except for
         *     this qualifier itself
         */
        public Set<QualifierKind> getSuperTypes() {
            return superTypes;
        }

        /**
         * Return the top qualifier kind of the hierarchy to which this qualifier kind belongs.
         *
         * @return the top qualifier kind of the hierarchy to which this qualifier kind belongs
         */
        public QualifierKind getTop() {
            return top;
        }

        /**
         * Return the bottom qualifier kind of the hierarchy to which this qualifier kind belongs.
         *
         * @return the bottom qualifier kind of the hierarchy to which this qualifier kind belongs
         */
        public QualifierKind getBottom() {
            return bottom;
        }

        /**
         * Whether or not the annotation class this qualifier kind represents has elements.
         *
         * @return true if the annotation class this qualifier kind represents has elements
         */
        public boolean hasElements() {
            return hasElements;
        }

        /**
         * Whether or not this qualifier kind and {@code other} are in the same hierarchy.
         *
         * @param other an other qualifier kind
         * @return true if this qualifier kind and {@code other} are in the same hierarchy
         */
        public boolean areInSameHierarchy(QualifierKind other) {
            return this.top == other.top;
        }

        /**
         * Whether or not this qualifier is a subtype of {@code superQual}.
         *
         * @param superQual other qualifier
         * @return true if this qualifier is a subtype of {@code superQual}.
         */
        public boolean isSubtype(QualifierKind superQual) {
            return this == superQual || superTypes.contains(superQual);
        }

        @Override
        public int compareTo(QualifierKind o) {
            return this.name.compareTo(o.name);
        }

        @Override
        public String toString() {
            String[] split = name.split("\\.");
            if (split.length == 0) {
                return name;
            }
            return split[split.length - 1];
        }
    }

    /**
     * A mapping from interned, fully-qualified class name of a qualifier to the QualifierKind
     * object representing that class.
     */
    private final Map<@Interned String, QualifierKind> qualifierKindMap;

    /**
     * A mapping from a top qualifier kind to the polymorphic qualifier kind in the same hierarchy.
     */
    private final Map<QualifierKind, QualifierKind> polyMap;

    /** All the qualifier kinds that are the top qualifier in their hierarchy. */
    private final Set<QualifierKind> tops;

    /** All the qualifier kinds that are the bottom qualifier in their hierarchy. */
    private final Set<QualifierKind> bottoms;

    /** A mapping from a pair of qualifier kinds to their lub. */
    private final Map<QualifierKindPair, QualifierKind> lubs;

    /** A mapping from a pair of qualifier kinds to their glb. */
    private final Map<QualifierKindPair, QualifierKind> glbs;

    /**
     * Creates a {@link QualifierKindHierarchy}. Also, creates and initializes all the qualifier
     * kinds for the hierarchy.
     *
     * @param qualifierClasses all the classes of qualifiers supported by this hierarchy
     */
    public QualifierKindHierarchy(Collection<Class<? extends Annotation>> qualifierClasses) {
        this.qualifierKindMap = createQualifierKinds(qualifierClasses);
        Map<QualifierKind, Set<QualifierKind>> directSuperMap = createDirectSuperMap();
        specifyBottom(directSuperMap, null);
        this.tops = createTopsSet(directSuperMap);
        this.bottoms = createBottomsSet(directSuperMap);
        this.polyMap = initializePolymorphicQualifiers();
        initializeQualifierKindFields(directSuperMap);
        this.lubs = createLubsMap();
        this.glbs = createGlbsMap();

        verifyHierarchy(directSuperMap);
        // printLubs();
        // printIsSubtype();
    }

    private void printLubs() {
        for (Entry<QualifierKindPair, QualifierKind> entry : lubs.entrySet()) {
            System.out.printf(
                    "LUB(%s, %s): %s%n",
                    entry.getKey().qual1, entry.getKey().qual2, entry.getValue());
        }
    }

    private void printLubsWithElements() {
        for (Entry<QualifierKindPair, QualifierKind> entry : lubs.entrySet()) {
            if (entry.getValue().hasElements) {
                System.out.printf(
                        "LUB(%s, %s): %s%n",
                        entry.getKey().qual1, entry.getKey().qual2, entry.getValue());
            }
        }
    }

    private void printGlbs() {
        for (Entry<QualifierKindPair, QualifierKind> entry : glbs.entrySet()) {
            System.out.printf(
                    "GLB(%s, %s): %s%n",
                    entry.getKey().qual1, entry.getKey().qual2, entry.getValue());
        }
    }

    private void printIsSubtype() {
        for (QualifierKind subKind : qualifierKindMap.values()) {
            for (QualifierKind superKind : qualifierKindMap.values()) {
                if (subKind.isSubtype(superKind)
                        && superKind.hasElements()
                        && subKind.hasElements()) {
                    System.out.printf("Sub: %s Super: %s%n", subKind, superKind);
                }
            }
        }
    }

    /**
     * Verifies that the {@link QualifierKindHierarchy} is a valid hierarchy.
     *
     * @param directSuperMap mapping from qualifier kind to its direct super types; used to verify
     *     that a polymorphic annotation does not have a {@link SubtypeOf} meta-annotation
     * @throws UserError if the heirarchy isn't valid
     */
    protected void verifyHierarchy(Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        for (QualifierKind qualifierKind : qualifierKindMap.values()) {
            boolean isPoly = qualifierKind.isPoly;
            boolean hasSubtype = directSuperMap.containsKey(qualifierKind);
            if (isPoly && hasSubtype) {
                // This is currently not supported. At some point we might add
                // polymorphic qualifiers with upper and lower bounds.
                throw new UserError(
                        "AnnotatedTypeFactory: "
                                + qualifierKind
                                + " is polymorphic and specifies super qualifiers. "
                                + "Remove the @org.checkerframework.framework.qual.SubtypeOf or @org.checkerframework.framework.qual.PolymorphicQualifier annotation from it.");
            } else if (!isPoly && !hasSubtype) {
                throw new UserError(
                        "AnnotatedTypeFactory: %s does not specify its super qualifiers.%n"
                                + "Add an @org.checkerframework.framework.qual.SubtypeOf annotation to it,%n"
                                + "or if it is an alias, exclude it from `createSupportedTypeQualifiers()`.%n",
                        qualifierKind);
            } else if (isPoly) {
                if (qualifierKind.top == null && tops.size() == 1) {
                    qualifierKind.top = tops.iterator().next();
                } else if (qualifierKind.top == null) {
                    throw new UserError(
                            "PolymorphicQualifier, %s,  has to specify type hierarchy, if more than one exist; top types: [%s] ",
                            qualifierKind, PluginUtil.join(", ", tops));
                } else if (!tops.contains(qualifierKind.top)) {
                    throw new UserError(
                            "Polymorphic qualifier, %s, specified %s, instead of a top qualifier in a hierarchy. Top qualifiers: %s",
                            qualifierKind, qualifierKind.top, PluginUtil.join(", ", tops));
                }
            }
        }

        if (bottoms.size() != tops.size()) {
            throw new UserError(
                    "Number of tops not equal to number of bottoms: Tops: [%s] Bottoms: [%s]",
                    PluginUtil.join(", ", tops), PluginUtil.join(", ", bottoms));
        }
    }

    /**
     * Creates all QualifierKind objects for the given {@code qualifierClasses} and adds them to
     * qualifierClassMap. (This method does not initialize all fields in the {@link QualifierKind}
     * that is done by {@link #initializeQualifierKindFields(Map)}.)
     *
     * @param qualifierClasses a collection of classes of annotations that are type qualifiers
     * @return a mapping from annotation name to {@link QualifierKind}
     */
    protected Map<@Interned String, QualifierKind> createQualifierKinds(
            Collection<Class<? extends Annotation>> qualifierClasses) {
        Map<@Interned String, QualifierKind> qualifierKindMap = new TreeMap<>();
        for (Class<? extends Annotation> clazz : qualifierClasses) {
            // If another QualifierKind exists with the same class, an exception will be thrown
            // below.
            @SuppressWarnings("interning")
            @Interned QualifierKind qualifierKind = new QualifierKind(clazz);
            if (qualifierKindMap.containsKey(qualifierKind.name)) {
                throw new UserError("");
            }
            qualifierKindMap.put(qualifierKind.name, qualifierKind);
        }
        return Collections.unmodifiableMap(qualifierKindMap);
    }

    /**
     * Creates a mapping from a {@link QualifierKind} to a set of its direct super qualifiers using
     * the {@link SubtypeOf} meta-annotation. The direct super qualifiers must not contain the
     * qualifier itself. This mapping is used to by {@link #createBottomsSet(Map)}, {@link
     * #createTopsSet(Map)}, and {@link #initializeQualifierKindFields(Map)}.
     *
     * <p>Subclasses may override this method to create the direct super map some other way.
     *
     * <p>Note that this method is called from the constructor and only {@link #qualifierKindMap}
     * has been initialized.
     *
     * @throws UserError if the {@link SubtypeOf} meta-annotation refers to a class that is not a
     *     qualifier
     * @return a mapping from a {@link QualifierKind} to a set of its direct super qualifiers
     */
    protected Map<QualifierKind, Set<QualifierKind>> createDirectSuperMap() {
        Map<QualifierKind, Set<QualifierKind>> directSuperMap = new TreeMap<>();
        for (QualifierKind qualifierKind : qualifierKindMap.values()) {
            SubtypeOf subtypeOfMetaAnno = qualifierKind.clazz.getAnnotation(SubtypeOf.class);
            if (subtypeOfMetaAnno != null) {
                Class<? extends Annotation>[] superQualifiers = subtypeOfMetaAnno.value();
                Set<QualifierKind> directSupers = new TreeSet<>();
                directSuperMap.put(qualifierKind, directSupers);
                for (Class<? extends Annotation> superClazz : superQualifiers) {
                    String superName = superClazz.getCanonicalName();
                    QualifierKind superQualifier = qualifierKindMap.get(superName);
                    if (superQualifier == null) {
                        throw new UserError(
                                "%s @Subtype meta-annotation refers to a qualifier, %s, that isn't in the hierarchy. Qualifiers: [%s]",
                                qualifierKind,
                                superName,
                                PluginUtil.join(", ", qualifierKindMap.values()));
                    }
                    directSupers.add(superQualifier);
                }
            }
        }
        return directSuperMap;
    }

    /**
     * Explicitly set bottom to the given class.
     *
     * <p>For some type systems, qualifiers may be added at runtime, so the {@link SubtypeOf}
     * meta-annotation on the bottom qualifier class cannot specify all other qualifiers. For those
     * type systems, override this method and call super with the bottom class. For example,
     *
     * <pre>
     * @Override
     * protected void specifyBottom(Map<QualifierKind, Set<QualifierKind>> directSuperMap, Class<? extends Annotation> bottom) {
     *     super.specifyBottom(directSuperMap, UnitsBottom.class);
     * }
     * </pre>
     *
     * If this method is not overridden, it has no effect.
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifiers
     * @param bottom the class of the bottom qualifier
     */
    protected void specifyBottom(
            Map<QualifierKind, Set<QualifierKind>> directSuperMap,
            Class<? extends Annotation> bottom) {
        if (bottom != null) {
            String name = bottom.getCanonicalName();
            QualifierKind bottomKind = getQualifierKindMap().get(name);
            Set<QualifierKind> superTypes = directSuperMap.get(bottomKind);
            superTypes.addAll(directSuperMap.keySet());
            superTypes.remove(bottomKind);
        }
    }

    /**
     * Creates the set of top {@link QualifierKind}s by searching {@code directSuperMap} for
     * qualifiers without any direct super qualifiers.
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifiers; create by {@link #createDirectSuperMap()}
     * @return the set of top {@link QualifierKind}s
     */
    // Subclasses should override createDirectSuperMap to change the tops and not this method,
    // because other methods expect the directSuperMap to be complete.
    private Set<QualifierKind> createTopsSet(
            Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        Set<QualifierKind> tops = new TreeSet<>();
        for (Entry<QualifierKind, Set<QualifierKind>> entry : directSuperMap.entrySet()) {
            QualifierKind qualifierKind = entry.getKey();
            if (entry.getValue().size() == 0) {
                tops.add(qualifierKind);
                qualifierKind.top = qualifierKind;
            }
        }
        return tops;
    }

    /**
     * Creates the set of bottom {@link QualifierKind}s by search {@code directSuperMap} for
     * qualifiers that are not a direct super qualifier of another qualifier.
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifiers; create by {@link #createDirectSuperMap()}
     * @return the set of bottom {@link QualifierKind}s
     */
    // Subclasses should override createDirectSuperMap or specifyBottom to change the bottoms and
    // not this method, because other methods expect the directSuperMap to be complete.
    private Set<QualifierKind> createBottomsSet(
            Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        // Bottom starts with all qualifiers
        Set<QualifierKind> bottoms = new TreeSet<>(directSuperMap.keySet());
        for (Set<QualifierKind> superKinds : directSuperMap.values()) {
            // Remove any qualifier that is a direct super qualifier of another qualifier
            bottoms.removeAll(superKinds);
        }
        return bottoms;
    }

    /**
     * Iterates over all the qualifier kinds and adds all polymorphic qualifiers to
     * polymorphicQualifiers. Also sets {@link QualifierKind#isPoly} to true and {@link
     * QualifierKind#top} to top if the meta-annotation {@link PolymorphicQualifier} specifies a
     * top.
     *
     * <p>Requires that tops has been initialized.
     *
     * @throws UserError if the meta-annotation {@link PolymorphicQualifier} specifies an annotation
     *     that is not in the hierarchy.
     * @return a mapping from a top qualifier to the polymorphic qualifier in the heirarchy
     */
    protected Map<QualifierKind, QualifierKind> initializePolymorphicQualifiers() {
        Map<QualifierKind, QualifierKind> polyMap = new TreeMap<>();
        for (QualifierKind qualifierKind : qualifierKindMap.values()) {
            Class<? extends Annotation> clazz = qualifierKind.getAnnotationClass();
            PolymorphicQualifier polyMetAnno = clazz.getAnnotation(PolymorphicQualifier.class);
            if (polyMetAnno != null) {
                qualifierKind.isPoly = true;
                String topName = polyMetAnno.value().getCanonicalName();
                if (qualifierKindMap.containsKey(topName)) {
                    qualifierKind.top = qualifierKindMap.get(topName);
                } else if (topName.equals(PolymorphicQualifier.class.getCanonicalName())) {
                    if (tops.size() == 1) {
                        qualifierKind.top = tops.iterator().next();
                    } else {
                        throw new UserError(
                                "The polymorphic qualifier, %s, did not specify a top annotation class. Tops: [%s]",
                                qualifierKind, PluginUtil.join(", ", tops));
                    }
                } else {
                    throw new UserError(
                            "The polymorphic qualifier, %s, specified a top annotation class that is not a supported qualifier. Found: %s.",
                            qualifierKind, topName);
                }
                polyMap.put(qualifierKind.top, qualifierKind);
            }
        }
        return polyMap;
    }

    /**
     * Initializes {@link QualifierKind#superTypes}, {@link QualifierKind#top} and {@link
     * QualifierKind#bottom}. (Requires tops, bottoms, and polymorphicQualifiers to be initialized.)
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifiers; create by {@link #createDirectSuperMap()}
     * @throws UserError if a qualifier isn't a subtype of one of the top qualifiers or if multiple
     *     tops or bottoms are found for the same hierarchy.
     */
    protected void initializeQualifierKindFields(
            Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        for (QualifierKind qualifierKind : qualifierKindMap.values()) {
            if (qualifierKind.isPoly) {
                qualifierKind.superTypes = new TreeSet<>();
                qualifierKind.superTypes.add(qualifierKind.top);
            } else {
                qualifierKind.superTypes = findAllTheSupers(qualifierKind, directSuperMap);
            }
        }
        for (QualifierKind qualifierKind : qualifierKindMap.values()) {
            for (QualifierKind top : tops) {
                if (qualifierKind.isSubtype(top)) {
                    if (qualifierKind.top != null && qualifierKind.top != top) {
                        throw new UserError(
                                "Multiple tops found for qualifier %s. Tops: %s and %s.",
                                qualifierKind, top, qualifierKind.top);
                    }
                    qualifierKind.top = top;
                }
            }
        }
        for (QualifierKind qualifierKind : qualifierKindMap.values()) {
            for (QualifierKind bot : bottoms) {
                if (bot.top == qualifierKind.top) {
                    if (qualifierKind.bottom != null && qualifierKind.top != bot) {
                        throw new UserError(
                                "Multiple bottoms found for qualifier %s. Tops: %s and %s.",
                                qualifierKind, bot, qualifierKind.bottom);
                    }
                    qualifierKind.bottom = bot;
                    if (qualifierKind.isPoly) {
                        bot.superTypes.add(qualifierKind);
                    }
                }
            }
            if (qualifierKind.top == null) {
                throw new UserError("Qualifier isn't in hierarchy: %s", qualifierKind);
            }
        }
    }

    /**
     * Returns the set of all qualifiers that are a supertype of {@code qualifierKind}.
     *
     * @param qualifierKind the qualifier whose super types should be returned
     * @param directSuperMap
     * @return the set of all qualifiers that are a supertype of {@code qualifierKind}
     * @throws UserError if there is a cycle in the hierarchy
     */
    private Set<QualifierKind> findAllTheSupers(
            QualifierKind qualifierKind, Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        Queue<QualifierKind> queue = new ArrayDeque<>(directSuperMap.get(qualifierKind));
        Set<QualifierKind> allSupers = new TreeSet<>(directSuperMap.get(qualifierKind));
        while (!queue.isEmpty()) {
            QualifierKind superQual = queue.remove();
            if (superQual == qualifierKind) {
                throw new UserError("Cycle in hierarchy: %s", qualifierKind);
            }
            queue.addAll(directSuperMap.get(superQual));
            allSupers.addAll(directSuperMap.get(superQual));
        }
        return allSupers;
    }

    /**
     * Creates a mapping from {@link QualifierKindPair} to the least upper bound of both
     * QualifierKinds.
     *
     * @return a mapping from {@link QualifierKindPair} to their lub.
     */
    protected Map<QualifierKindPair, QualifierKind> createLubsMap() {
        Map<QualifierKindPair, QualifierKind> lubs = new TreeMap<>();
        for (QualifierKind qual1 : qualifierKindMap.values()) {
            for (QualifierKind qual2 : qualifierKindMap.values()) {
                if (qual1.top != qual2.top) {
                    continue;
                }
                QualifierKind lub = findLub(qual1, qual2);
                QualifierKindPair pair = new QualifierKindPair(qual1, qual2);
                QualifierKind otherLub = lubs.get(pair);
                if (otherLub != null) {
                    if (otherLub != lub) {
                        throw new BugInCF(
                                "Multiple lubs for qualifiers %s and %s. Found lubs %s and %s",
                                qual1, qual2, lub, otherLub);
                    }
                } else {
                    lubs.put(pair, lub);
                }
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
        Set<QualifierKind> allSuperTypes = new TreeSet<>(qual1.superTypes);
        allSuperTypes.retainAll(qual2.superTypes);
        Set<QualifierKind> lubs = findLowestQualifiers(allSuperTypes);
        if (lubs.size() != 1) {
            throw new BugInCF(
                    "Not exactly 1 lub for %s and %s. Found lubs: [%s].",
                    qual1, qual2, PluginUtil.join(", ", lubs));
        }
        QualifierKind lub = lubs.iterator().next();
        if (lub.isPoly && !qual1.isPoly && !qual2.isPoly) {
            return lub.top;
        }
        return lub;
    }

    /**
     * Returns the lowest qualifiers in the passed set.
     *
     * @param qualifierKinds the passed set
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
     * Creates a mapping from {@link QualifierKindPair} to the greatest lower bound of both
     * QualifierKinds.
     *
     * @return a mapping from {@link QualifierKindPair} to their glb.
     */
    private Map<QualifierKindPair, QualifierKind> createGlbsMap() {
        Map<QualifierKindPair, QualifierKind> glbs = new TreeMap<>();
        for (QualifierKind qual1 : qualifierKindMap.values()) {
            for (QualifierKind qual2 : qualifierKindMap.values()) {
                if (qual1.top != qual2.top) {
                    continue;
                }
                QualifierKind glb = findGlb(qual1, qual2);
                QualifierKindPair pair = new QualifierKindPair(qual1, qual2);
                QualifierKind otherGlb = glbs.get(pair);
                if (otherGlb != null) {
                    if (otherGlb != glb) {
                        throw new BugInCF(
                                "Multiple glbs for qualifiers %s and %s. Found lubs %s and %s",
                                qual1, qual2, glb, otherGlb);
                    }
                } else {
                    glbs.put(pair, glb);
                }
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
        for (QualifierKind qualifierKind : qualifierKindMap.values()) {
            if (qualifierKind.isSubtype(qual1) && qualifierKind.isSubtype(qual2)) {
                allSubTypes.add(qualifierKind);
            }
        }
        Set<QualifierKind> glbs = findHighestQualifiers(allSubTypes);
        if (glbs.size() != 1) {
            throw new BugInCF(
                    "Not exactly 1 glb for %s and %s. Found glb: [%s].",
                    qual1, qual2, PluginUtil.join(", ", glbs));
        }
        QualifierKind lub = glbs.iterator().next();
        if (lub.isPoly && !qual1.isPoly && !qual2.isPoly) {
            return lub.bottom;
        }
        return lub;
    }

    /**
     * Returns the highest qualifiers in the passed set.
     *
     * @param qualifierKinds the passed set
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
     * Returns the least upper bound of {@code q1} and {@code q2}.
     *
     * @param q1 a qualifier kind
     * @param q2 a qualifier kind
     * @return the least upper bound of {@code q1} and {@code q2}
     */
    public QualifierKind leastUpperBound(QualifierKind q1, QualifierKind q2) {
        return lubs.get(new QualifierKindPair(q1, q2));
    }

    /**
     * Returns the greatest lower bound of {@code q1} and {@code q2}.
     *
     * @param q1 a qualifier kind
     * @param q2 a qualifier kind
     * @return the greatest lower bound of {@code q1} and {@code q2}
     */
    public QualifierKind greatestLowerBound(QualifierKind q1, QualifierKind q2) {
        return glbs.get(new QualifierKindPair(q1, q2));
    }

    /**
     * Returns the mapping from the fully-qualified class name of an annotation to its qualifier
     * kind.
     *
     * @return the mapping from the fully-qualified class name of an annotation to its qualifier
     *     kind
     */
    public Map<String, QualifierKind> getQualifierKindMap() {
        return qualifierKindMap;
    }

    /**
     * The mapping from a top qualifier kind to the polymorphic qualifier kind in the same
     * hierarchy.
     *
     * @return the mapping from a top qualifier kind to the polymorphic qualifier kind in the same
     *     hierarchy
     */
    public Map<QualifierKind, QualifierKind> getPolyMap() {
        return polyMap;
    }

    /**
     * A pair of {@link QualifierKind}s. new QualifierKindPair(q1, q2) is equal to new
     * QualifierKindPair(q2, q1).
     */
    protected static class QualifierKindPair implements Comparable<QualifierKindPair> {

        /** The first qualifier of the pair. */
        private final QualifierKind qual1;
        /** The second qualifier of the pair. */
        private final QualifierKind qual2;

        /**
         * Create a pair.
         *
         * @param qual1 a qualifier kind
         * @param qual2 a qualifier kind
         */
        public QualifierKindPair(QualifierKind qual1, QualifierKind qual2) {
            // Order the pair.
            if (qual1.compareTo(qual2) <= 0) {
                this.qual1 = qual1;
                this.qual2 = qual2;
            } else {
                this.qual1 = qual2;
                this.qual2 = qual1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            QualifierKindPair that = (QualifierKindPair) o;
            return qual1 == that.qual1 && qual2 == that.qual2;
        }

        @Override
        public int hashCode() {
            int result = qual1.hashCode();
            result = 31 * result + qual2.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "qual1=" + qual1 + ", qual2=" + qual2;
        }

        @Override
        public int compareTo(QualifierKindPair o) {
            if (this.qual1 == o.qual1) {
                return this.qual2.compareTo(o.qual2);
            }
            return this.qual1.compareTo(o.qual1);
        }
    }
}
