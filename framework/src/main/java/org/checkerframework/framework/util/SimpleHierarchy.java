package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.PluginUtil;
import org.checkerframework.javacutil.UserError;

public class SimpleHierarchy extends QualifierHierarchy {
    /**
     * A mapping from interned, fully-qualified class name of a qualifier to the QualifierClass
     * object representing that class.
     */
    private final Map<@Interned String, QualifierClass> qualifierClassMap = new TreeMap<>();

    private final Set<QualifierClass> polymorphicQualifiers = new TreeSet<>();
    private final Map<QualifierClass, Set<QualifierClass>> directSuperMap = new TreeMap<>();
    private final Set<QualifierClass> tops = new TreeSet<>();
    private final Map<QualifierClass, AnnotationMirror> topAnnotations = new TreeMap<>();
    private final Set<QualifierClass> bottoms = new TreeSet<>();
    private final Map<QualifierClass, AnnotationMirror> bottomAnnotations = new TreeMap<>();
    private Elements elements;

    public SimpleHierarchy(Collection<Class<? extends Annotation>> qualifierClasses) {
        initialize(qualifierClasses);
    }

    private QualifierClass getQualifierClass(AnnotationMirror annotationMirror) {
        String name = AnnotationUtils.annotationName(annotationMirror);
        return qualifierClassMap.get(name);
    }

    private final Set<AnnotationMirror> topsAnnotationSet = AnnotationUtils.createAnnotationSet();
    private final Set<AnnotationMirror> bottomsAnnotationSet =
            AnnotationUtils.createAnnotationSet();

    @Override
    public Set<? extends AnnotationMirror> getTopAnnotations() {
        return topsAnnotationSet;
    }

    @Override
    public Set<? extends AnnotationMirror> getBottomAnnotations() {
        return bottomsAnnotationSet;
    }

    @Override
    public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
        QualifierClass qualifierClass = getQualifierClass(start);
        return topAnnotations.get(qualifierClass.top);
    }

    @Override
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
        QualifierClass qualifierClass = getQualifierClass(start);
        return topAnnotations.get(qualifierClass.top);
    }

    @Override
    public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        QualifierClass qualifierClass = getQualifierClass(start);
        return bottomAnnotations.get(qualifierClass.bottom);
    }

    @Override
    public Set<? extends AnnotationMirror> getTypeQualifiers() {
        return null;
    }

    @Override
    public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
        QualifierClass subQual = getQualifierClass(rhs);
        QualifierClass superQual = getQualifierClass(lhs);
        return subQual.isSubtype(superQual);
    }

    @Override
    public boolean isSubtype(
            Collection<? extends AnnotationMirror> rhs,
            Collection<? extends AnnotationMirror> lhs) {
        int valid = 0;
        for (AnnotationMirror subAnno : rhs) {
            QualifierClass subQual = getQualifierClass(subAnno);
            for (AnnotationMirror superAnno : lhs) {
                QualifierClass superQual = getQualifierClass(superAnno);
                if (subQual.top == superQual.top) {
                    valid++;
                    if (subQual.isSubtype(superQual)) {
                        valid++;
                    }
                }
            }
        }
        return valid == lhs.size() && valid == rhs.size();
    }

    @Override
    public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
        QualifierClass qual1 = getQualifierClass(a1);
        QualifierClass qual2 = getQualifierClass(a2);
        if (qual1 == qual2) {
            return a1;
        }
        QualifierClass lub = lubs.get(new QualifierClassPair(qual1, qual2));
    }

    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        return null;
    }

    @Override
    public boolean isSubtypeTypeVariable(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        return false;
    }

    @Override
    public boolean isSubtypeTypeVariable(
            Collection<? extends AnnotationMirror> subAnnos,
            Collection<? extends AnnotationMirror> superAnnos) {
        return false;
    }

    @Override
    public AnnotationMirror leastUpperBoundTypeVariable(AnnotationMirror a1, AnnotationMirror a2) {
        return null;
    }

    @Override
    public AnnotationMirror greatestLowerBoundTypeVariable(
            AnnotationMirror a1, AnnotationMirror a2) {
        return null;
    }

    @Interned private static class QualifierClass implements Comparable<QualifierClass> {
        private final @Interned String name;
        private final Class<? extends Annotation> clazz;
        private boolean isPoly;
        /**
         * All qualifiers that are a super qualifier of this qualifier, except for this qualifier
         * itself.
         */
        private Set<QualifierClass> superTypes;

        private QualifierClass top;
        private QualifierClass bottom;

        QualifierClass(Class<? extends Annotation> clazz, @Nullable AnnotationMirror anno) {
            this.clazz = clazz;
            this.name = clazz.getCanonicalName().intern();
            isPoly = false;
            superTypes = null;
            top = null;
            bottom = null;
            anno = anno;
        }

        public String getName() {
            return name;
        }

        public Class<? extends Annotation> getClazz() {
            return clazz;
        }

        public boolean isPoly() {
            return isPoly;
        }

        public Set<QualifierClass> getSuperTypes() {
            return superTypes;
        }

        public QualifierClass getTop() {
            return top;
        }

        @Override
        public int compareTo(QualifierClass o) {
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

        boolean isSubtype(QualifierClass superQual) {
            return this == superQual || superTypes.contains(superQual);
        }
    }

    public void initialize(Collection<Class<? extends Annotation>> qualifierClasses) {
        createQualifierClasses(qualifierClasses);
        initializePolymorphicQualifiers();
        initializeDirectSuperTypesAndTops();
        initializeBottomsAndSuperTypesAndTop();
        initializeTopAndBottomAnnotationMirrors();
        initializeLubs();

        for (QualifierClass qualifierClass : qualifierClassMap.values()) {
            boolean isPoly = qualifierClass.isPoly;
            boolean hasSubtype = directSuperMap.containsKey(qualifierClass);
            if (isPoly && hasSubtype) {
                // This is currently not supported. At some point we might add
                // polymorphic qualifiers with upper and lower bounds.
                throw new BugInCF(
                        "AnnotatedTypeFactory: "
                                + qualifierClass
                                + " is polymorphic and specifies super qualifiers. "
                                + "Remove the @org.checkerframework.framework.qual.SubtypeOf or @org.checkerframework.framework.qual.PolymorphicQualifier annotation from it.");
            } else if (!isPoly && !hasSubtype) {
                throw new BugInCF(
                        "AnnotatedTypeFactory: %s does not specify its super qualifiers.%n"
                                + "Add an @org.checkerframework.framework.qual.SubtypeOf annotation to it,%n"
                                + "or if it is an alias, exclude it from `createSupportedTypeQualifiers()`.%n",
                        qualifierClass);
            }
        }
        // Check polymorphic qualifiers are wellformed
        for (QualifierClass polyQual : polymorphicQualifiers) {
            if (polyQual.top == null && tops.size() == 1) {
                polyQual.top = tops.iterator().next();
            } else if (polyQual.top == null) {
                throw new BugInCF(
                        "PolymorphicQualifier, %s,  has to specify type hierarchy, if more than one exist; top types: [%s] ",
                        polyQual, PluginUtil.join(", ", tops));
            } else if (!tops.contains(polyQual.top)) {
                throw new UserError(
                        "Polymorphic qualifier, %s, specified %s, instead of a top qualifier in a hierarchy. Top qualifiers: %s",
                        polyQual, polyQual.top, PluginUtil.join(", ", tops));
            }
        }

        if (bottoms.size() != tops.size()) {
            throw new UserError(
                    "Number of tops not equal to number of bottoms: Tops: [%s] Bottoms: [%s]",
                    PluginUtil.join(", ", tops), PluginUtil.join(", ", bottoms));
        }
    }

    /**
     * Creates all QualifierClass objects for the given {@code qualifierClasses} and adds them to
     * qualifierClassMap. (This method does not initialize all fields in the {@link
     * QualifierClass}.)
     *
     * @param qualifierClasses a collection of classes of annotations that are type qualifiers
     */
    private void createQualifierClasses(Collection<Class<? extends Annotation>> qualifierClasses) {
        for (Class<? extends Annotation> clazz : qualifierClasses) {
            // TODO: fromClass should only be used on classes with
            AnnotationMirror anno = AnnotationBuilder.fromClass(elements, clazz);
            QualifierClass qualifierClass = new QualifierClass(clazz, anno);
            if (qualifierClassMap.containsKey(qualifierClass.name)) {
                throw new UserError("");
            }
            qualifierClassMap.put(qualifierClass.name, qualifierClass);
        }
    }

    /**
     * Iterates for all the qualifiers and adds all polymorphic qualifiers to polymorphicQualifiers.
     * Also sets {@link QualifierClass#isPoly} to true and {@link QualifierClass#top} to top it the
     * meta-annotation {@link PolymorphicQualifier} specifies a top.
     *
     * @throws UserError if the meta-annotation {@link PolymorphicQualifier} specifies an annotation
     *     that is not in the hierarchy.
     */
    private void initializePolymorphicQualifiers() {
        for (QualifierClass qualifierClass : qualifierClassMap.values()) {
            Class<? extends Annotation> clazz = qualifierClass.getClazz();
            PolymorphicQualifier polyMetaAnnotation =
                    clazz.getAnnotation(PolymorphicQualifier.class);
            if (polyMetaAnnotation != null) {
                qualifierClass.isPoly = true;
                polymorphicQualifiers.add(qualifierClass);
                String topName = polyMetaAnnotation.value().getCanonicalName();
                if (qualifierClassMap.containsKey(topName)) {
                    qualifierClass.top = qualifierClassMap.get(topName);
                } else if (!topName.equals(PolymorphicQualifier.class.getCanonicalName())) {
                    throw new UserError(
                            "PolymorphicQualifier has to specify type hierarchy, if more than one exist.");
                }
                // else qualifierClass.top is set after the top qualifier is found.
            }
        }
    }

    /**
     * Initializes directSuperMap and tops.
     *
     * @throws UserError if the {@link SubtypeOf} meta-annotation refers to a class that is not a
     *     qualifier
     */
    private void initializeDirectSuperTypesAndTops() {
        for (QualifierClass qualifierClass : qualifierClassMap.values()) {
            SubtypeOf subtypeOfMetaAnno = qualifierClass.clazz.getAnnotation(SubtypeOf.class);
            if (subtypeOfMetaAnno != null) {
                Class<? extends Annotation>[] superQualifiers = subtypeOfMetaAnno.value();
                if (superQualifiers.length == 0) {
                    tops.add(qualifierClass);
                    qualifierClass.top = qualifierClass;
                }

                Set<QualifierClass> directSupers = new TreeSet<>();
                directSuperMap.put(qualifierClass, directSupers);
                for (Class<? extends Annotation> superClazz : superQualifiers) {
                    String superName = superClazz.getCanonicalName();
                    QualifierClass superQualifier = qualifierClassMap.get(superName);
                    if (superQualifier == null) {
                        throw new UserError(
                                "%s @Subtype meta-annotation refers to a qualifier, %s, that isn't in the hierarchy.",
                                qualifierClass, superName);
                    }
                    directSupers.add(superQualifier);
                }
            }
        }
    }

    /**
     * Initializes bottoms and for qualifiers that are not polymorphic, {@link
     * QualifierClass#superTypes} and {@link QualifierClass#superTypes} are initialized.
     *
     * @throws UserError if a qualifier isn't a subtype of one of the top qualifiers.
     */
    private void initializeBottomsAndSuperTypesAndTop() {
        bottoms.addAll(directSuperMap.keySet());
        for (QualifierClass qualifierClass : directSuperMap.keySet()) {
            qualifierClass.superTypes =
                    Collections.unmodifiableSet(findAllTheSupers(qualifierClass));
            bottoms.removeAll(qualifierClass.superTypes);
            if (qualifierClass.superTypes.isEmpty()) {
                qualifierClass.top = qualifierClass;
            }
            for (QualifierClass top : tops) {
                if (qualifierClass.isSubtype(top)) {
                    qualifierClass.top = top;
                }
            }
            if (qualifierClass.top == null) {
                throw new UserError("Qualifier isn't in hierarchy: %s", qualifierClass);
            }
        }

        for (QualifierClass bot : bottoms) {
            for (QualifierClass qualifierClass : bot.superTypes) {
                qualifierClass.bottom = bot;
            }
        }
    }

    /**
     * Returns the set of all qualifiers that are a supertype of {@code qualifierClass}.
     *
     * @param qualifierClass the qualifier whose super types should be returned
     * @return the set of all qualifiers that are a supertype of {@code qualifierClass}
     * @throws UserError if there is a cycle in the hierarchy
     */
    private Set<QualifierClass> findAllTheSupers(QualifierClass qualifierClass) {
        Queue<QualifierClass> queue = new ArrayDeque<>(directSuperMap.get(qualifierClass));
        Set<QualifierClass> allSupers = new TreeSet<>(directSuperMap.get(qualifierClass));
        while (!queue.isEmpty()) {
            QualifierClass superQual = queue.remove();
            if (superQual == qualifierClass) {
                throw new UserError("Cycle in hierarchy: %s", qualifierClass);
            }
            queue.addAll(directSuperMap.get(superQual));
            allSupers.addAll(directSuperMap.get(superQual));
        }
        return allSupers;
    }

    private void initializeTopAndBottomAnnotationMirrors() {
        for (QualifierClass top : tops) {
            // TODO: this is assuming that fromClass issues an error if there are any elements with
            // out a default value.
            AnnotationMirror topAnno = AnnotationBuilder.fromClass(elements, top.getClazz());
            topsAnnotationSet.add(topAnno);
            topAnnotations.put(top, topAnno);
        }

        for (QualifierClass bottom : bottoms) {
            // TODO: this is assuming that fromClass issues an error if there are any elements with
            // out a default value.
            AnnotationMirror bottomAnno = AnnotationBuilder.fromClass(elements, bottom.getClazz());
            bottomsAnnotationSet.add(bottomAnno);
            bottomAnnotations.put(bottom, bottomAnno);
        }
    }

    Map<QualifierClassPair, QualifierClass> lubs = new HashMap<>();

    private void initializeLubs() {
        for (QualifierClass qual1 : qualifierClassMap.values()) {
            for (QualifierClass qual2 : qualifierClassMap.values()) {
                QualifierClass lub = findLub(qual1, qual2);
                QualifierClassPair pair = new QualifierClassPair(qual1, qual2);
                QualifierClass otherLub = lubs.get(pair);
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
    }

    private QualifierClass findLub(QualifierClass qual1, QualifierClass qual2) {
        if (qual1 == qual2) {
            return qual1;
        } else if (qual1.isSubtype(qual2)) {
            return qual2;
        } else if (qual2.isSubtype(qual1)) {
            return qual1;
        }
        Set<QualifierClass> allSuperTypes = new TreeSet<>(qual1.superTypes);
        allSuperTypes.retainAll(qual2.superTypes);
        Set<QualifierClass> lubs = findLowestQualifiers(allSuperTypes);
        if (lubs.size() != 1) {
            throw new BugInCF(
                    "Not exactly 1 lub for %s and %s. Found lubs: [%s].",
                    qual1, qual2, PluginUtil.join(", ", lubs));
        }
        return lubs.iterator().next();
    }

    /** Remove all supertypes of elements contained in the set. */
    private Set<QualifierClass> findLowestQualifiers(Set<QualifierClass> qualifierClasses) {
        Set<QualifierClass> lowestQualifiers = new TreeSet<>(qualifierClasses);
        for (QualifierClass a1 : qualifierClasses) {
            lowestQualifiers.removeIf(a2 -> a1 != a2 && a2.isSubtype(a1));
        }
        return lowestQualifiers;
    }

    private static class QualifierClassPair {
        private final QualifierClass qual1;
        private final QualifierClass qual2;

        public QualifierClassPair(QualifierClass qual1, QualifierClass qual2) {
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
    }
}
