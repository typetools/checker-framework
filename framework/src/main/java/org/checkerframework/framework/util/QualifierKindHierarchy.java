package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.PluginUtil;
import org.checkerframework.javacutil.UserError;

/** NO AnnotationMirrors allowed in this class. */
public class QualifierKindHierarchy {
    public static @Interned class QualifierKind implements Comparable<QualifierKind> {
        private final @Interned String name;
        private final Class<? extends Annotation> clazz;
        private boolean isPoly;
        /**
         * All qualifiers that are a super qualifier of this qualifier, except for this qualifier
         * itself.
         */
        private Set<QualifierKind> superTypes;

        private QualifierKind top;
        private QualifierKind bottom;

        private boolean hasElements;

        QualifierKind(Class<? extends Annotation> clazz) {
            this.clazz = clazz;
            this.hasElements = clazz.getDeclaredMethods().length != 0;
            this.name = clazz.getCanonicalName().intern();
            isPoly = false;
            superTypes = null;
            top = null;
            bottom = null;
        }

        public String getName() {
            return name;
        }

        public Class<? extends Annotation> getAnnotationClass() {
            return clazz;
        }

        public boolean isPoly() {
            return isPoly;
        }

        public boolean isTop() {
            return this.top == this;
        }

        public boolean isBottom() {
            return this.bottom == this;
        }

        public Set<QualifierKind> getSuperTypes() {
            return superTypes;
        }

        public QualifierKind getTop() {
            return top;
        }

        public QualifierKind getBottom() {
            return bottom;
        }

        public boolean hasElements() {
            return hasElements;
        }

        public boolean areInSameHierarchy(QualifierKind other) {
            return this.top == other.top;
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

        public boolean isSubtype(QualifierKind superQual) {
            return this == superQual || superTypes.contains(superQual);
        }
    }
    /**
     * A mapping from interned, fully-qualified class name of a qualifier to the QualifierKind
     * object representing that class.
     */
    private final Map<@Interned String, QualifierKind> qualifierKindMap;

    /** Top -> Poly */
    private final Map<QualifierKind, QualifierKind> polyMap;

    private final Set<QualifierKind> tops;
    private final Set<QualifierKind> bottoms;
    private final Map<QualifierClassPair, QualifierKind> lubs;
    private final Map<QualifierClassPair, QualifierKind> glbs;

    public QualifierKindHierarchy(Collection<Class<? extends Annotation>> qualifierClasses) {
        this.qualifierKindMap = createQualifierKinds(qualifierClasses);
        Map<QualifierKind, Set<QualifierKind>> directSuperMap = initializeDirectSuperTypes();
        this.tops = initializeTops(directSuperMap);
        this.bottoms = initializeBottoms(directSuperMap);
        this.polyMap = initializePolymorphicQualifiers();
        initializeQualifierKinds(directSuperMap);
        this.lubs = initializeLubs();
        this.glbs = initializeGlbs();

        verify(directSuperMap);
        //                printLubs();
        //        printIsSubtype();
    }

    private void printLubs() {
        for (Entry<QualifierClassPair, QualifierKind> entry : lubs.entrySet()) {
            if (entry.getValue().hasElements()) {
                System.out.println(entry.getKey() + " " + entry.getValue());
            }
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

    public void verify(Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        for (QualifierKind qualifierKind : qualifierKindMap.values()) {
            boolean isPoly = qualifierKind.isPoly;
            boolean hasSubtype = directSuperMap.containsKey(qualifierKind);
            if (isPoly && hasSubtype) {
                // This is currently not supported. At some point we might add
                // polymorphic qualifiers with upper and lower bounds.
                throw new BugInCF(
                        "AnnotatedTypeFactory: "
                                + qualifierKind
                                + " is polymorphic and specifies super qualifiers. "
                                + "Remove the @org.checkerframework.framework.qual.SubtypeOf or @org.checkerframework.framework.qual.PolymorphicQualifier annotation from it.");
            } else if (!isPoly && !hasSubtype) {
                throw new BugInCF(
                        "AnnotatedTypeFactory: %s does not specify its super qualifiers.%n"
                                + "Add an @org.checkerframework.framework.qual.SubtypeOf annotation to it,%n"
                                + "or if it is an alias, exclude it from `createSupportedTypeQualifiers()`.%n",
                        qualifierKind);
            } else if (isPoly) {
                if (qualifierKind.top == null && tops.size() == 1) {
                    qualifierKind.top = tops.iterator().next();
                } else if (qualifierKind.top == null) {
                    throw new BugInCF(
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
     * qualifierClassMap. (This method does not initialize all fields in the {@link QualifierKind}.)
     *
     * @param qualifierClasses a collection of classes of annotations that are type qualifiers
     * @return
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
     * Iterates for all the qualifiers and adds all polymorphic qualifiers to polymorphicQualifiers.
     * Also sets {@link QualifierKind#isPoly} to true and {@link QualifierKind#top} to top if the
     * meta-annotation {@link PolymorphicQualifier} specifies a top.
     *
     * <p>Requires that tops has been initialized.
     *
     * @throws UserError if the meta-annotation {@link PolymorphicQualifier} specifies an annotation
     *     that is not in the hierarchy.
     * @return
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
     * Initializes directSuperMap.
     *
     * @throws UserError if the {@link SubtypeOf} meta-annotation refers to a class that is not a
     *     qualifier
     * @return
     */
    protected Map<QualifierKind, Set<QualifierKind>> initializeDirectSuperTypes() {
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
                                "%s @Subtype meta-annotation refers to a qualifier, %s, that isn't in the hierarchy.",
                                qualifierKind, superName);
                    }
                    directSupers.add(superQualifier);
                }
            }
        }
        return directSuperMap;
    }

    /**
     * Initializes tops. (Requires directSuperMap to be initialized.)
     *
     * @param directSuperMap
     */
    protected Set<QualifierKind> initializeTops(
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
     * Initializes bottoms.*
     *
     * @param directSuperMap
     * @return
     */
    protected Set<QualifierKind> initializeBottoms(
            Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
        Set<QualifierKind> bottoms = new TreeSet<>(directSuperMap.keySet());
        for (Set<QualifierKind> superKinds : directSuperMap.values()) {
            bottoms.removeAll(superKinds);
        }
        return bottoms;
    }

    /**
     * Initializes {@link QualifierKind#superTypes}, {@link QualifierKind#top} and {@link
     * QualifierKind#bottom}. (Requires directSuperMap, tops, bottoms, and polymorphicQualifiers to
     * be initialized.)
     *
     * @throws UserError if a qualifier isn't a subtype of one of the top qualifiers or if multiple
     *     tops or bottoms are found for the same hierarchy.
     * @param directSuperMap
     */
    protected void initializeQualifierKinds(Map<QualifierKind, Set<QualifierKind>> directSuperMap) {
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

    private Map<QualifierClassPair, QualifierKind> initializeLubs() {
        Map<QualifierClassPair, QualifierKind> lubs = new HashMap<>();
        for (QualifierKind qual1 : qualifierKindMap.values()) {
            for (QualifierKind qual2 : qualifierKindMap.values()) {
                if (qual1.top != qual2.top) {
                    continue;
                }
                QualifierKind lub = findLub(qual1, qual2);
                QualifierClassPair pair = new QualifierClassPair(qual1, qual2);
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

    /** Remove all supertypes of elements contained in the set. */
    private Set<QualifierKind> findLowestQualifiers(Set<QualifierKind> qualifierKinds) {
        Set<QualifierKind> lowestQualifiers = new TreeSet<>(qualifierKinds);
        for (QualifierKind a1 : qualifierKinds) {
            lowestQualifiers.removeIf(a2 -> a1 != a2 && a1.isSubtype(a2));
        }
        return lowestQualifiers;
    }

    private Map<QualifierClassPair, QualifierKind> initializeGlbs() {
        Map<QualifierClassPair, QualifierKind> glbs = new HashMap<>();
        for (QualifierKind qual1 : qualifierKindMap.values()) {
            for (QualifierKind qual2 : qualifierKindMap.values()) {
                if (qual1.top != qual2.top) {
                    continue;
                }
                QualifierKind glb = findGlb(qual1, qual2);
                QualifierClassPair pair = new QualifierClassPair(qual1, qual2);
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

    /** Remove all supertypes of elements contained in the set. */
    private Set<QualifierKind> findHighestQualifiers(Set<QualifierKind> qualifierKinds) {
        Set<QualifierKind> lowestQualifiers = new TreeSet<>(qualifierKinds);
        for (QualifierKind a1 : qualifierKinds) {
            lowestQualifiers.removeIf(a2 -> a1 != a2 && a2.isSubtype(a1));
        }
        return lowestQualifiers;
    }

    private static class QualifierClassPair {
        private final QualifierKind qual1;
        private final QualifierKind qual2;

        public QualifierClassPair(QualifierKind qual1, QualifierKind qual2) {
            // Order the pair.
            if (qual1.compareTo(qual2) > 0) {
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

            QualifierClassPair that = (QualifierClassPair) o;
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
    }

    public Set<QualifierKind> getTops() {
        return tops;
    }

    public Set<QualifierKind> getBottoms() {
        return bottoms;
    }

    public QualifierKind leastUpperBound(QualifierKind q1, QualifierKind q2) {
        return lubs.get(new QualifierClassPair(q1, q2));
    }

    public QualifierKind greatestLowerBound(QualifierKind q1, QualifierKind q2) {
        return glbs.get(new QualifierClassPair(q1, q2));
    }

    public Map<String, QualifierKind> getQualifierKindMap() {
        return qualifierKindMap;
    }

    public Map<QualifierKind, QualifierKind> getPolyMap() {
        return polyMap;
    }
}
