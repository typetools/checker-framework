package org.checkerframework.framework.util;

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypeSystemError;
import org.plumelib.util.StringsPlume;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This is the default implementation of {@link QualifierKindHierarchy}.
 *
 * <p>By default, the subtyping information and information about polymorphic qualifiers is read
 * from meta-annotations on the annotation classes. This information is used to infer further
 * information such as top and bottom qualifiers. Subclasses can override the following methods to
 * change this behavior:
 *
 * <ul>
 *   <li>{@link #createQualifierKinds(Collection)}
 *   <li>{@link #createDirectSuperMap()}
 *   <li>{@link #initializePolymorphicQualifiers()}
 *   <li>{@link #initializeQualifierKindFields(Map)}
 *   <li>{@link #createLubsMap()}
 *   <li>{@link #createGlbsMap()}
 * </ul>
 *
 * {@link DefaultQualifierKindHierarchy.DefaultQualifierKind} is the implementation used for {@link
 * QualifierKind} by this class.
 */
@AnnotatedFor("nullness")
public class DefaultQualifierKindHierarchy implements QualifierKindHierarchy {

    /**
     * A mapping from canonical name of a qualifier class to the QualifierKind object representing
     * that class.
     */
    protected final Map<@Interned @CanonicalName String, DefaultQualifierKind> nameToQualifierKind;

    /**
     * A list of all {@link QualifierKind}s for this DefaultQualifierKindHierarchy, sorted in
     * ascending order.
     */
    protected final List<DefaultQualifierKind> qualifierKinds;

    /** All the qualifier kinds that are the top qualifier in their hierarchy. */
    private final Set<DefaultQualifierKind> tops;

    /** All the qualifier kinds that are the bottom qualifier in their hierarchy. */
    private final Set<DefaultQualifierKind> bottoms;

    /**
     * Holds the lub of qualifier kinds. {@code lubs.get(kind1).get(kind2)} returns the lub of kind1
     * and kind2.
     */
    private final Map<QualifierKind, Map<QualifierKind, QualifierKind>> lubs;

    /**
     * Holds the glb of qualifier kinds. {@code glbs.get(kind1).get(kind2)} returns the glb of kind1
     * and kind2.
     */
    private final Map<QualifierKind, Map<QualifierKind, QualifierKind>> glbs;

    @Override
    public Set<? extends QualifierKind> getTops() {
        return tops;
    }

    @Override
    public Set<? extends QualifierKind> getBottoms() {
        return bottoms;
    }

    @Override
    public @Nullable QualifierKind leastUpperBound(QualifierKind q1, QualifierKind q2) {
        @SuppressWarnings(
                "nullness:dereference.of.nullable") // All QualifierKinds are keys in lubs.
        QualifierKind result = lubs.get(q1).get(q2);
        return result;
    }

    @Override
    public @Nullable QualifierKind greatestLowerBound(QualifierKind q1, QualifierKind q2) {
        @SuppressWarnings(
                "nullness:dereference.of.nullable") // All QualifierKinds are keys in glbs.
        QualifierKind result = glbs.get(q1).get(q2);
        return result;
    }

    @Override
    public List<? extends QualifierKind> allQualifierKinds() {
        return qualifierKinds;
    }

    @Override
    public @Nullable QualifierKind getQualifierKind(@CanonicalName String name) {
        return nameToQualifierKind.get(name);
    }

    /**
     * Creates a {@link DefaultQualifierKindHierarchy}. Also, creates and initializes all its
     * qualifier kinds.
     *
     * @param qualifierClasses all the classes of qualifiers supported by this hierarchy
     */
    public DefaultQualifierKindHierarchy(Collection<Class<? extends Annotation>> qualifierClasses) {
        this(qualifierClasses, null, null);
    }

    /**
     * Creates a {@link DefaultQualifierKindHierarchy}. Also, creates and initializes all its
     * qualifier kinds.
     *
     * <p>For some type systems, qualifiers may be added at run time, so the {@link SubtypeOf}
     * meta-annotation on the bottom qualifier class cannot specify all other qualifiers. For those
     * type systems, use this constructor. Otherwise, use {@link
     * #DefaultQualifierKindHierarchy(Collection)}.
     *
     * @param qualifierClasses all the classes of qualifiers supported by this hierarchy
     * @param bottom the bottom qualifier of this hierarchy
     */
    public DefaultQualifierKindHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses,
            Class<? extends Annotation> bottom) {
        this(qualifierClasses, bottom, null);
    }

    /**
     * Private constructor that sets the bottom qualifier if {@code bottom} is nonnull.
     *
     * @param qualifierClasses all the classes of qualifiers supported by this hierarchy
     * @param bottom the bottom qualifier of this hierarchy or null if bottom can be inferred from
     *     the meta-annotations
     * @param voidParam void parameter to differentiate from {@link
     *     #DefaultQualifierKindHierarchy(Collection, Class)}
     */
    private DefaultQualifierKindHierarchy(
            Collection<Class<? extends Annotation>> qualifierClasses,
            @Nullable Class<? extends Annotation> bottom,
            @SuppressWarnings("UnusedVariable") Void voidParam) {
        this.nameToQualifierKind = createQualifierKinds(qualifierClasses);
        this.qualifierKinds = new ArrayList<>(nameToQualifierKind.values());
        Collections.sort(qualifierKinds);

        Map<DefaultQualifierKind, Set<DefaultQualifierKind>> directSuperMap =
                createDirectSuperMap();
        if (bottom != null) {
            setBottom(bottom, directSuperMap);
        }
        this.tops = createTopsSet(directSuperMap);
        this.bottoms = createBottomsSet(directSuperMap);
        initializePolymorphicQualifiers();
        initializeQualifierKindFields(directSuperMap);
        this.lubs = createLubsMap();
        this.glbs = createGlbsMap();

        verifyHierarchy(directSuperMap);
    }

    /**
     * Verifies that the {@link DefaultQualifierKindHierarchy} is a valid hierarchy.
     *
     * @param directSuperMap mapping from qualifier to its direct supertypes; used to verify that a
     *     polymorphic annotation does not have a {@link SubtypeOf} meta-annotation
     * @throws TypeSystemError if the hierarchy isn't valid
     */
    @RequiresNonNull({"this.qualifierKinds", "this.tops", "this.bottoms"})
    protected void verifyHierarchy(
            @UnderInitialization DefaultQualifierKindHierarchy this,
            Map<DefaultQualifierKind, Set<DefaultQualifierKind>> directSuperMap) {
        for (DefaultQualifierKind qualifierKind : qualifierKinds) {
            boolean isPoly = qualifierKind.isPoly();
            boolean hasSubtypeOfAnno = directSuperMap.containsKey(qualifierKind);
            if (isPoly && hasSubtypeOfAnno) {
                // Polymorphic qualifiers with upper and lower bounds are currently not supported.
                throw new TypeSystemError(
                        "AnnotatedTypeFactory: "
                                + qualifierKind
                                + " is polymorphic and specifies super qualifiers.%nRemove the"
                                + " @PolymorphicQualifier or @SubtypeOf annotation from it.");
            } else if (!isPoly && !hasSubtypeOfAnno) {
                throw new TypeSystemError(
                        "AnnotatedTypeFactory: %s does not specify its super qualifiers.%nAdd an"
                            + " @SubtypeOf or @PolymorphicQualifier annotation to it,%nor if it is"
                            + " an alias, exclude it from `createSupportedTypeQualifiers()`.",
                        qualifierKind);
            } else if (isPoly) {
                if (qualifierKind.top == null) {
                    throw new TypeSystemError(
                            "PolymorphicQualifier, %s, has to specify a type hierarchy in its"
                                + " @PolymorphicQualifier meta-annotation, if more than one exists;"
                                + " top types: [%s].",
                            qualifierKind, StringsPlume.join(", ", tops));
                } else if (!tops.contains(qualifierKind.top)) {
                    throw new TypeSystemError(
                            "Polymorphic qualifier %s has invalid top %s. Top qualifiers: %s",
                            qualifierKind, qualifierKind.top, StringsPlume.join(", ", tops));
                }
            }
        }

        if (bottoms.size() != tops.size()) {
            throw new TypeSystemError(
                    "Number of tops not equal to number of bottoms: Tops: [%s] Bottoms: [%s]",
                    StringsPlume.join(", ", tops), StringsPlume.join(", ", bottoms));
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
    protected Map<@Interned @CanonicalName String, DefaultQualifierKind> createQualifierKinds(
            @UnderInitialization DefaultQualifierKindHierarchy this,
            Collection<Class<? extends Annotation>> qualifierClasses) {
        TreeMap<@Interned @CanonicalName String, DefaultQualifierKind> nameToQualifierKind =
                new TreeMap<>();
        for (Class<? extends Annotation> clazz : qualifierClasses) {
            @SuppressWarnings("interning") // uniqueness is tested immediately below
            @Interned DefaultQualifierKind qualifierKind = new DefaultQualifierKind(clazz);
            if (nameToQualifierKind.containsKey(qualifierKind.getName())) {
                throw new TypeSystemError("Duplicate QualifierKind " + qualifierKind.getName());
            }
            nameToQualifierKind.put(qualifierKind.getName(), qualifierKind);
        }
        return Collections.unmodifiableMap(nameToQualifierKind);
    }

    /**
     * Creates a mapping from a {@link QualifierKind} to a set of its direct super qualifier kinds.
     * The direct super qualifier kinds do not contain the qualifier itself. This mapping is used to
     * create the bottom set, to create the top set, and by {@link
     * #initializeQualifierKindFields(Map)}.
     *
     * <p>This implementation uses the {@link SubtypeOf} meta-annotation. Subclasses may override
     * this method to create the direct super map some other way.
     *
     * <p>Note that this method is called from the constructor when {@link #nameToQualifierKind} and
     * {@link #qualifierKinds} are the only fields that have nonnull values. This method is not
     * static, so it can be overridden by subclasses.
     *
     * @return a mapping from each {@link QualifierKind} to a set of its direct super qualifiers
     */
    @RequiresNonNull({"this.nameToQualifierKind", "this.qualifierKinds"})
    protected Map<DefaultQualifierKind, Set<DefaultQualifierKind>> createDirectSuperMap(
            @UnderInitialization DefaultQualifierKindHierarchy this) {
        Map<DefaultQualifierKind, Set<DefaultQualifierKind>> directSuperMap = new TreeMap<>();
        for (DefaultQualifierKind qualifierKind : qualifierKinds) {
            SubtypeOf subtypeOfMetaAnno =
                    qualifierKind.getAnnotationClass().getAnnotation(SubtypeOf.class);
            if (subtypeOfMetaAnno == null) {
                // qualifierKind has no @SubtypeOf: it must be top or polymorphic
                continue;
            }
            Set<DefaultQualifierKind> directSupers = new TreeSet<>();
            for (Class<? extends Annotation> superClazz : subtypeOfMetaAnno.value()) {
                String superName = QualifierKindHierarchy.annotationClassName(superClazz);
                DefaultQualifierKind superQualifier = nameToQualifierKind.get(superName);
                if (superQualifier == null) {
                    throw new TypeSystemError(
                            "%s @Subtype argument %s isn't in the hierarchy. Qualifiers: [%s]",
                            qualifierKind, superName, StringsPlume.join(", ", qualifierKinds));
                }
                directSupers.add(superQualifier);
            }
            directSuperMap.put(qualifierKind, directSupers);
        }
        return directSuperMap;
    }

    /**
     * This method sets bottom to the given class and modifies {@code directSuperMap} to add all
     * leaves to its super qualifier kinds. Leaves are qualifier kinds that are not super qualifier
     * kinds of another qualifier kind and are not polymorphic.
     *
     * @param bottom the class of the bottom qualifier in the hierarchy
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifiers; side-effected by this method
     */
    @RequiresNonNull({"this.nameToQualifierKind", "this.qualifierKinds"})
    private void setBottom(
            @UnderInitialization DefaultQualifierKindHierarchy this,
            Class<? extends Annotation> bottom,
            Map<DefaultQualifierKind, Set<DefaultQualifierKind>> directSuperMap) {
        DefaultQualifierKind bottomKind =
                nameToQualifierKind.get(QualifierKindHierarchy.annotationClassName(bottom));
        if (bottomKind == null) {
            throw new TypeSystemError(
                    "QualifierKindHierarchy#setBottom: %s is not in the hierarchy",
                    bottom.getCanonicalName());
        }

        Set<DefaultQualifierKind> leaves = new TreeSet<>(qualifierKinds);
        leaves.remove(bottomKind);
        directSuperMap.forEach((sub, supers) -> leaves.removeAll(supers));
        Set<DefaultQualifierKind> bottomDirectSuperQuals = directSuperMap.get(bottomKind);
        if (bottomDirectSuperQuals == null) {
            directSuperMap.put(bottomKind, leaves);
        } else {
            bottomDirectSuperQuals.addAll(leaves);
        }
    }

    /**
     * Creates the set of top {@link QualifierKind}s by searching {@code directSuperMap} for
     * qualifier kinds without any direct super qualifier kinds.
     *
     * <p>Subclasses should override {@link #createDirectSuperMap} to change the tops and not this
     * method, because other methods expect the directSuperMap to be complete.
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifier kinds; created by {@link #createDirectSuperMap()}
     * @return the set of top {@link QualifierKind}s
     */
    private Set<DefaultQualifierKind> createTopsSet(
            @UnderInitialization DefaultQualifierKindHierarchy this,
            Map<DefaultQualifierKind, Set<DefaultQualifierKind>> directSuperMap) {
        Set<DefaultQualifierKind> tops = new TreeSet<>();
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
     * <p>Subclasses should override {@link #createDirectSuperMap} or {@link #setBottom} to change
     * the bottoms and not this method, because other methods expect the directSuperMap to be
     * complete.
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifier kinds; created by {@link #createDirectSuperMap()}
     * @return the set of bottom {@link QualifierKind}s
     */
    private Set<DefaultQualifierKind> createBottomsSet(
            @UnderInitialization DefaultQualifierKindHierarchy this,
            Map<DefaultQualifierKind, Set<DefaultQualifierKind>> directSuperMap) {
        Set<DefaultQualifierKind> bottoms = new HashSet<>(directSuperMap.keySet());
        for (Set<DefaultQualifierKind> superKinds : directSuperMap.values()) {
            bottoms.removeAll(superKinds);
        }
        return bottoms;
    }

    /**
     * Iterates over all the qualifier kinds and adds all polymorphic qualifier kinds to
     * polymorphicQualifiers. Also sets {@link DefaultQualifierKind#poly} and {@link
     * DefaultQualifierKind#top} for the polymorphic qualifiers, and sets {@link
     * DefaultQualifierKind#poly} for the top qualifiers.
     *
     * <p>Requires that tops has been initialized.
     */
    @RequiresNonNull({"this.nameToQualifierKind", "this.qualifierKinds", "this.tops"})
    protected void initializePolymorphicQualifiers(
            @UnderInitialization DefaultQualifierKindHierarchy this) {
        for (DefaultQualifierKind qualifierKind : qualifierKinds) {
            Class<? extends Annotation> clazz = qualifierKind.getAnnotationClass();
            PolymorphicQualifier polyMetaAnno = clazz.getAnnotation(PolymorphicQualifier.class);
            if (polyMetaAnno == null) {
                continue;
            }
            qualifierKind.poly = qualifierKind;
            String topName = QualifierKindHierarchy.annotationClassName(polyMetaAnno.value());
            if (nameToQualifierKind.containsKey(topName)) {
                qualifierKind.top = nameToQualifierKind.get(topName);
            } else if (topName.equals(Annotation.class.getCanonicalName())) {
                // Annotation.class is the default value of PolymorphicQualifier. If it is used,
                // then there must be exactly one top.
                if (tops.size() == 1) {
                    qualifierKind.top = tops.iterator().next();
                } else {
                    throw new TypeSystemError(
                            "Polymorphic qualifier %s did not specify a top annotation class. Tops:"
                                    + " [%s]",
                            qualifierKind, StringsPlume.join(", ", tops));
                }
            } else {
                throw new TypeSystemError(
                        "Polymorphic qualifier %s's top, %s, is not a qualifier.",
                        qualifierKind, topName);
            }
            qualifierKind.strictSuperTypes = Collections.singleton(qualifierKind.top);
            qualifierKind.top.poly = qualifierKind;
        }
    }

    /**
     * For each qualifier kind in {@code directSuperMap}, initializes {@link
     * DefaultQualifierKind#strictSuperTypes}, {@link DefaultQualifierKind#top}, {@link
     * DefaultQualifierKind#bottom}, and {@link DefaultQualifierKind#poly}.
     *
     * <p>Requires tops, bottoms, and polymorphicQualifiers to be initialized.
     *
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifier kinds; created by {@link #createDirectSuperMap()}
     */
    @RequiresNonNull({"this.qualifierKinds", "this.tops", "this.bottoms"})
    protected void initializeQualifierKindFields(
            @UnderInitialization DefaultQualifierKindHierarchy this,
            Map<DefaultQualifierKind, Set<DefaultQualifierKind>> directSuperMap) {
        for (DefaultQualifierKind qualifierKind : directSuperMap.keySet()) {
            if (!qualifierKind.isPoly()) {
                qualifierKind.strictSuperTypes = findAllTheSupers(qualifierKind, directSuperMap);
            }
        }
        for (DefaultQualifierKind qualifierKind : qualifierKinds) {
            for (DefaultQualifierKind top : tops) {
                if (qualifierKind.isSubtypeOf(top)) {
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
            qualifierKind.poly = qualifierKind.top.poly;
        }
        for (DefaultQualifierKind qualifierKind : qualifierKinds) {
            for (DefaultQualifierKind bot : bottoms) {
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
                if (qualifierKind.isPoly()) {
                    assert bot.strictSuperTypes != null
                            : "@AssumeAssertion(nullness): strictSuperTypes should be nonnull.";
                    bot.strictSuperTypes.add(qualifierKind);
                }
            }
            if (qualifierKind.bottom == null) {
                throw new TypeSystemError(
                        "Cannot find a bottom qualifier for %s. bottoms = %s",
                        qualifierKind, bottoms);
            }
        }
    }

    /**
     * Returns the set of all qualifier kinds that are a strict supertype of {@code qualifierKind}.
     *
     * @param qualifierKind the qualifier kind whose super types should be returned
     * @param directSuperMap a mapping from a {@link QualifierKind} to a set of its direct super
     *     qualifier kinds; created by {@link #createDirectSuperMap()}
     * @return the set of all qualifier kinds that are a strict supertype of {@code qualifierKind}
     */
    private Set<QualifierKind> findAllTheSupers(
            @UnderInitialization DefaultQualifierKindHierarchy this,
            @KeyFor("#2") QualifierKind qualifierKind,
            Map<DefaultQualifierKind, Set<DefaultQualifierKind>> directSuperMap) {

        Set<QualifierKind> allSupers = new TreeSet<>(directSuperMap.get(qualifierKind));

        // Visit every super qualifier kind and add its super qualifier kinds to allSupers.
        Queue<DefaultQualifierKind> toVisit = new ArrayDeque<>(directSuperMap.get(qualifierKind));
        Set<DefaultQualifierKind> visited = new HashSet<>();
        while (!toVisit.isEmpty()) {
            DefaultQualifierKind superQualKind = toVisit.remove();
            if (superQualKind == qualifierKind) {
                throw new TypeSystemError("Cycle in hierarchy: %s", qualifierKind);
            }

            if (!visited.add(superQualKind) || superQualKind.isPoly()) {
                continue;
            }

            Set<DefaultQualifierKind> superSuperQuals = directSuperMap.get(superQualKind);
            if (superSuperQuals == null) {
                throw new TypeSystemError(superQualKind + " is not a key in the directSuperMap");
            }
            toVisit.addAll(superSuperQuals);
            allSupers.addAll(superSuperQuals);
        }
        return allSupers;
    }

    /**
     * Creates the lub of qualifier kinds. {@code lubs.get(kind1).get(kind2)} returns the lub of
     * kind1 and kind2.
     *
     * @return a mapping of lubs
     */
    @RequiresNonNull("this.qualifierKinds")
    protected Map<QualifierKind, Map<QualifierKind, QualifierKind>> createLubsMap(
            @UnderInitialization DefaultQualifierKindHierarchy this) {
        Map<QualifierKind, Map<QualifierKind, QualifierKind>> lubs = new HashMap<>();
        for (QualifierKind qual1 : qualifierKinds) {
            for (QualifierKind qual2 : qualifierKinds) {
                if (qual1.getTop() != qual2.getTop()) {
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
    private QualifierKind findLub(
            @UnderInitialization DefaultQualifierKindHierarchy this,
            QualifierKind qual1,
            QualifierKind qual2) {
        if (qual1 == qual2) {
            return qual1;
        } else if (qual1.isSubtypeOf(qual2)) {
            return qual2;
        } else if (qual2.isSubtypeOf(qual1)) {
            return qual1;
        }
        Set<QualifierKind> allSuperTypes = new TreeSet<>(qual1.getStrictSuperTypes());
        Set<? extends QualifierKind> qual2StrictSuperTypes = qual2.getStrictSuperTypes();
        allSuperTypes.retainAll(qual2StrictSuperTypes);
        Set<? extends QualifierKind> lubs = findLowestQualifiers(allSuperTypes);
        if (lubs.size() != 1) {
            throw new TypeSystemError(
                    "lub(%s, %s) should have size 1: [%s]",
                    qual1, qual2, StringsPlume.join(", ", lubs));
        }
        QualifierKind lub = lubs.iterator().next();
        if (lub.isPoly() && !qual1.isPoly() && !qual2.isPoly()) {
            throw new TypeSystemError("lub(%s, %s) can't be poly: %s", qual1, qual2, lub);
        }
        return lub;
    }

    /**
     * Returns the lowest qualifiers in the passed set.
     *
     * @param qualifierKinds a set of qualifiers
     * @return the lowest qualifiers in the passed set
     */
    protected static Set<QualifierKind> findLowestQualifiers(Set<QualifierKind> qualifierKinds) {
        Set<QualifierKind> lowestQualifiers = new TreeSet<>(qualifierKinds);
        for (QualifierKind a1 : qualifierKinds) {
            lowestQualifiers.removeIf(a2 -> a1 != a2 && a1.isSubtypeOf(a2));
        }
        return lowestQualifiers;
    }

    /**
     * Creates the glb of qualifier kinds. {@code glbs.get(kind1).get(kind2)} returns the glb of
     * kind1 and kind2.
     *
     * @return a mapping of glb
     */
    @RequiresNonNull("this.qualifierKinds")
    protected Map<QualifierKind, Map<QualifierKind, QualifierKind>> createGlbsMap(
            @UnderInitialization DefaultQualifierKindHierarchy this) {
        Map<QualifierKind, Map<QualifierKind, QualifierKind>> glbs = new TreeMap<>();
        for (QualifierKind qual1 : qualifierKinds) {
            for (QualifierKind qual2 : qualifierKinds) {
                if (qual1.getTop() != qual2.getTop()) {
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
    @RequiresNonNull("this.qualifierKinds")
    private QualifierKind findGlb(
            @UnderInitialization DefaultQualifierKindHierarchy this,
            QualifierKind qual1,
            QualifierKind qual2) {
        if (qual1 == qual2) {
            return qual1;
        } else if (qual1.isSubtypeOf(qual2)) {
            return qual1;
        } else if (qual2.isSubtypeOf(qual1)) {
            return qual2;
        }
        Set<QualifierKind> allSubTypes = new TreeSet<>();
        for (QualifierKind qualifierKind : qualifierKinds) {
            if (qualifierKind.isSubtypeOf(qual1) && qualifierKind.isSubtypeOf(qual2)) {
                allSubTypes.add(qualifierKind);
            }
        }
        Set<QualifierKind> glbs = findHighestQualifiers(allSubTypes);
        if (glbs.size() != 1) {
            throw new TypeSystemError(
                    "glb(%s, %s) should have size 1: [%s]",
                    qual1, qual2, StringsPlume.join(", ", glbs));
        }
        QualifierKind glb = glbs.iterator().next();
        if (glb.isPoly() && !qual1.isPoly() && !qual2.isPoly()) {
            throw new TypeSystemError("glb(%s, %s) can't be poly: %s", qual1, qual2, glb);
        }
        return glb;
    }

    /**
     * Returns the highest qualifiers in the passed set.
     *
     * @param qualifierKinds a set of qualifiers
     * @return the highest qualifiers in the passed set
     */
    protected static Set<QualifierKind> findHighestQualifiers(Set<QualifierKind> qualifierKinds) {
        Set<QualifierKind> highestQualifiers = new TreeSet<>(qualifierKinds);
        for (QualifierKind a1 : qualifierKinds) {
            highestQualifiers.removeIf(a2 -> a1 != a2 && a2.isSubtypeOf(a1));
        }
        return highestQualifiers;
    }

    /**
     * Add Key: qual1, Value: (Key: qual2, Value: value) to {@code map}. If already in map, throw an
     * exception if value is different.
     *
     * @param map mapping to side-effect
     * @param qual1 the first qualifier kind
     * @param qual2 the second qualifier kind
     * @param value the value to add
     * @param operationName "lub" or "glb"; used only for error messages
     */
    private static void addToMapOfMap(
            Map<QualifierKind, Map<QualifierKind, QualifierKind>> map,
            QualifierKind qual1,
            QualifierKind qual2,
            QualifierKind value,
            String operationName) {
        Map<QualifierKind, QualifierKind> qual1Map =
                map.computeIfAbsent(qual1, k -> new HashMap<>());
        QualifierKind existingValue = qual1Map.get(qual2);
        if (existingValue == null) {
            qual1Map.put(qual2, value);
        } else {
            if (existingValue != value) {
                throw new TypeSystemError(
                        "Multiple %ss for qualifiers %s and %s. Found map %s and %s",
                        operationName, qual1, qual2, value, existingValue);
            }
        }
    }

    /**
     * The default implementation of {@link QualifierKind}.
     *
     * <p>The fields in this class that refer to {@link QualifierKind}s are set when creating the
     * {@link DefaultQualifierKindHierarchy}. So the getter methods for these fields should not be
     * called until after the {@code DefaultQualifierKindHierarchy} is created.
     */
    @AnnotatedFor("nullness")
    public @Interned static class DefaultQualifierKind implements QualifierKind {

        /** The canonical name of the annotation class of this. */
        private final @Interned @CanonicalName String name;

        /** The annotation class for this. */
        private final Class<? extends Annotation> clazz;

        /** True if the annotation class of this has annotation elements/arguments. */
        private final boolean hasElements;

        /** The top of the hierarchy to which this belongs. */
        // Set while creating the QualifierKindHierarchy.
        protected @MonotonicNonNull DefaultQualifierKind top;

        /** The bottom of the hierarchy to which this belongs. */
        // Set while creating the QualifierKindHierarchy.
        protected @MonotonicNonNull DefaultQualifierKind bottom;

        /** The polymorphic qualifier of the hierarchy to which this belongs. */
        // Set while creating the QualifierKindHierarchy.
        protected @Nullable DefaultQualifierKind poly;

        /**
         * All the qualifier kinds that are a strict super qualifier kind of this. Does not include
         * this qualifier kind itself.
         */
        // Set while creating the QualifierKindHierarchy.
        protected @MonotonicNonNull Set<QualifierKind> strictSuperTypes;

        /**
         * Creates a {@link DefaultQualifierKind} for the given annotation class.
         *
         * @param clazz annotation class for a qualifier
         */
        DefaultQualifierKind(Class<? extends Annotation> clazz) {
            this.clazz = clazz;
            this.hasElements = clazz.getDeclaredMethods().length != 0;
            this.name = QualifierKindHierarchy.annotationClassName(clazz).intern();
            this.poly = null;
        }

        @Override
        public @Interned @CanonicalName String getName() {
            return name;
        }

        @Override
        public Class<? extends Annotation> getAnnotationClass() {
            return clazz;
        }

        @Override
        public QualifierKind getTop() {
            if (top == null) {
                throw new BugInCF(
                        "DefaultQualifierKindHierarchy#getTop: Top is null for QualifierKind %s."
                                + " Don't call this method during initialization of"
                                + " DefaultQualifierKindHierarchy.",
                        name);
            }
            return top;
        }

        @Override
        public boolean isTop() {
            return this.top == this;
        }

        @Override
        public QualifierKind getBottom() {
            if (bottom == null) {
                throw new BugInCF(
                        "DefaultQualifierKind#getBottom:Bottom is null for QualifierKind %s. Don't"
                                + " call this method during initialization of"
                                + " DefaultQualifierKindHierarchy.",
                        name);
            }
            return bottom;
        }

        @Override
        public boolean isBottom() {
            return this.bottom == this;
        }

        @Override
        public @Nullable QualifierKind getPolymorphic() {
            return poly;
        }

        @Pure
        @Override
        public boolean isPoly() {
            return this.poly == this;
        }

        @Override
        public boolean hasElements() {
            return hasElements;
        }

        @Override
        public Set<? extends QualifierKind> getStrictSuperTypes() {
            if (strictSuperTypes == null) {
                throw new BugInCF(
                        "DefaultQualifierKind#getStrictSuperTypes: strictSuperTypes was null. Don't"
                                + " call this method during initialization of"
                                + " DefaultQualifierKindHierarchy.");
            }
            return strictSuperTypes;
        }

        @Override
        public boolean isInSameHierarchyAs(QualifierKind other) {
            return this.top == other.getTop();
        }

        @Override
        public boolean isSubtypeOf(QualifierKind superQualKind) {
            if (strictSuperTypes == null) {
                throw new BugInCF(
                        "DefaultQualifierKind#isSubtypeOf: strictSuperTypes was null. Don't call"
                                + " this method during initialization of"
                                + " DefaultQualifierKindHierarchy.");
            }
            return this == superQualKind || strictSuperTypes.contains(superQualKind);
        }

        @Override
        public String toString() {
            return clazz.getSimpleName();
        }
    }
}
