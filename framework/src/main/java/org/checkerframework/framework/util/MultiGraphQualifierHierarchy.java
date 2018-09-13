package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.poly.QualifierPolymorphism;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

/**
 * Represents the type qualifier hierarchy of a type system that supports multiple separate subtype
 * hierarchies.
 *
 * <p>This class is immutable and can be only created through {@link MultiGraphFactory}.
 */
public class MultiGraphQualifierHierarchy extends QualifierHierarchy {

    /**
     * Factory used to create an instance of {@link GraphQualifierHierarchy}. A factory can be used
     * to create at most one {@link GraphQualifierHierarchy}.
     *
     * <p>To create a hierarchy, a client may do so in three steps:
     *
     * <ol>
     *   <li>add qualifiers using {@link #addQualifier(AnnotationMirror)};
     *   <li>add subtype relations using {@link #addSubtype(AnnotationMirror, AnnotationMirror)}
     *   <li>build the hierarchy and gets using {@link #build()}.
     * </ol>
     *
     * Notice that {@link #addSubtype(AnnotationMirror, AnnotationMirror)} adds the two qualifiers
     * to the hierarchy if they are not already in.
     *
     * <p>Also, once the client builds a hierarchy through {@link #build()}, no further
     * modifications are allowed nor can it making a new instance.
     *
     * <p>Clients build the hierarchy using {@link #addQualifier(AnnotationMirror)} and {@link
     * #addSubtype(AnnotationMirror, AnnotationMirror)}, then get the instance with calling {@link
     * #build()}
     */
    public static class MultiGraphFactory {
        /**
         * Map from qualifiers to the direct supertypes of the qualifier. Only the subtype relations
         * given by addSubtype are in this mapping, no transitive relationships. It is immutable
         * once GraphQualifierHierarchy is built. No polymorphic qualifiers are contained in this
         * map.
         */
        protected final Map<AnnotationMirror, Set<AnnotationMirror>> supertypes;

        /**
         * Map from qualifier hierarchy to the corresponding polymorphic qualifier. The key is: *
         * the argument to @PolymorphicQualifier (typically the top qualifier in the hierarchy), or
         * * "PolymorphicQualifier" if @PolymorphicQualifier is used without an argument, or * null,
         * for the PolyAll qualifier.
         */
        protected final Map<AnnotationMirror, AnnotationMirror> polyQualifiers;

        protected final AnnotatedTypeFactory atypeFactory;

        public MultiGraphFactory(AnnotatedTypeFactory atypeFactory) {
            this.supertypes = AnnotationUtils.createAnnotationMap();
            this.polyQualifiers = new HashMap<>();
            this.atypeFactory = atypeFactory;
        }

        /**
         * Adds the passed qualifier to the hierarchy. Clients need to specify its super qualifiers
         * in subsequent calls to {@link #addSubtype(AnnotationMirror, AnnotationMirror)}.
         */
        public void addQualifier(AnnotationMirror qual) {
            assertNotBuilt();
            if (AnnotationUtils.containsSame(supertypes.keySet(), qual)) {
                return;
            }

            Class<? extends Annotation> pqtopclass =
                    QualifierPolymorphism.getPolymorphicQualifierTop(qual);
            if (pqtopclass != null) {
                AnnotationMirror pqtop =
                        AnnotationBuilder.fromClass(atypeFactory.getElementUtils(), pqtopclass);
                if (QualifierPolymorphism.isPolyAll(qual)) {
                    // Use key null as marker for polyall
                    this.polyQualifiers.put(null, qual);
                } else {
                    // use given top (which might be PolymorphicQualifier) as key
                    this.polyQualifiers.put(pqtop, qual);
                }
            } else {
                supertypes.put(qual, AnnotationUtils.createAnnotationSet());
            }
        }

        /**
         * Adds a subtype relationship between the two type qualifiers. Assumes that both qualifiers
         * are part of the same qualifier hierarchy; callers should ensure this.
         *
         * @param sub the sub type qualifier
         * @param sup the super type qualifier
         */
        public void addSubtype(AnnotationMirror sub, AnnotationMirror sup) {
            assertNotBuilt();
            addQualifier(sub);
            addQualifier(sup);
            supertypes.get(sub).add(sup);
        }

        /**
         * Returns an instance of {@link GraphQualifierHierarchy} that represents the hierarchy
         * built so far.
         */
        public QualifierHierarchy build() {
            assertNotBuilt();
            QualifierHierarchy result = createQualifierHierarchy();
            wasBuilt = true;
            return result;
        }

        protected QualifierHierarchy createQualifierHierarchy() {
            return atypeFactory.createQualifierHierarchy(this);
        }

        /** True if the factory has already been built. */
        private boolean wasBuilt = false;

        /** Throw an exception if the factory was already built. */
        protected void assertNotBuilt() {
            if (wasBuilt) {
                throw new BugInCF(
                        "MultiGraphQualifierHierarchy.Factory was already built. Method build can only be called once.");
            }
        }
    }

    /**
     * The declared, direct supertypes for each qualifier, without added transitive relations.
     * Immutable after construction finishes. No polymorphic qualifiers are contained in this map.
     *
     * @see MultiGraphQualifierHierarchy.MultiGraphFactory#supertypes
     */
    protected final Map<AnnotationMirror, Set<AnnotationMirror>> supertypesGraph;

    /** The transitive closure of the supertypesGraph. Immutable after construction finishes. */
    protected final Map<AnnotationMirror, Set<AnnotationMirror>> supertypesMap;

    /** The top qualifiers of the individual type hierarchies. */
    protected final Set<AnnotationMirror> tops;

    /** The bottom qualifiers of the type hierarchies. TODO: clarify relation to tops. */
    protected final Set<AnnotationMirror> bottoms;

    /**
     * Reference to the special qualifier org.checkerframework.framework.qual.PolymorphicQualifier.
     * It is used as a key in polyQualifiers, if the qualifier hierarchy consists of a single top
     * and no specific qualifier was specified.
     */
    protected final AnnotationMirror polymorphicQualifier;

    /** @see MultiGraphQualifierHierarchy.MultiGraphFactory#polyQualifiers */
    protected final Map<AnnotationMirror, AnnotationMirror> polyQualifiers;

    /** All qualifiers, including polymorphic qualifiers. */
    private final Set<AnnotationMirror> typeQualifiers;

    public MultiGraphQualifierHierarchy(MultiGraphFactory f) {
        this(f, (Object[]) null);
    }

    // Allow a subclass to provide additional constructor parameters that
    // are simply passed back via a call to the "finish" method.
    public MultiGraphQualifierHierarchy(MultiGraphFactory f, Object... args) {
        super();
        // no need for copying as f.supertypes has no mutable references to it
        // TODO: also make the Set of supertypes immutable?
        this.supertypesGraph = Collections.unmodifiableMap(f.supertypes);

        // Calculate the transitive closure
        Map<AnnotationMirror, Set<AnnotationMirror>> fullMap = buildFullMap(f.supertypes);

        Set<AnnotationMirror> newtops = findTops(fullMap);
        Set<AnnotationMirror> newbottoms = findBottoms(fullMap);

        this.polymorphicQualifier =
                AnnotationBuilder.fromClass(
                        f.atypeFactory.getElementUtils(), PolymorphicQualifier.class);
        this.polyQualifiers = f.polyQualifiers;

        addPolyRelations(this, fullMap, this.polyQualifiers, newtops, newbottoms);

        finish(this, fullMap, this.polyQualifiers, newtops, newbottoms, args);

        this.tops = Collections.unmodifiableSet(newtops);
        this.bottoms = Collections.unmodifiableSet(newbottoms);
        // TODO: make polyQualifiers immutable also?

        this.supertypesMap = Collections.unmodifiableMap(fullMap);
        Set<AnnotationMirror> typeQualifiers = AnnotationUtils.createAnnotationSet();
        typeQualifiers.addAll(supertypesMap.keySet());
        this.typeQualifiers = Collections.unmodifiableSet(typeQualifiers);
        // System.out.println("MGH: " + this);
    }

    /**
     * Method to finalize the qualifier hierarchy before it becomes unmodifiable. The parameters
     * pass all fields and allow modification.
     */
    protected void finish(
            QualifierHierarchy qualHierarchy,
            Map<AnnotationMirror, Set<AnnotationMirror>> fullMap,
            Map<AnnotationMirror, AnnotationMirror> polyQualifiers,
            Set<AnnotationMirror> tops,
            Set<AnnotationMirror> bottoms,
            Object... args) {}

    @SideEffectFree
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Supertypes Graph: ");

        for (Entry<AnnotationMirror, Set<AnnotationMirror>> qual : supertypesGraph.entrySet()) {
            sb.append("\n\t");
            sb.append(qual.getKey());
            sb.append(" = ");
            sb.append(qual.getValue());
        }

        sb.append("\nSupertypes Map: ");

        for (Entry<AnnotationMirror, Set<AnnotationMirror>> qual : supertypesMap.entrySet()) {
            sb.append("\n\t");
            sb.append(qual.getKey());
            sb.append(" = [");

            Set<AnnotationMirror> supertypes = qual.getValue();

            if (supertypes.size() == 1) {
                // if there's only 1 supertype for this qual, then directly display that in the same
                // row
                sb.append(supertypes.iterator().next());
            } else {
                // otherwise, display each supertype in its own row
                for (Iterator<AnnotationMirror> iterator = supertypes.iterator();
                        iterator.hasNext(); ) {
                    // new line and tabbing
                    sb.append("\n\t\t");
                    // display the supertype
                    sb.append(iterator.next());
                    // add a comma delimiter if it isn't the last value
                    sb.append(iterator.hasNext() ? ", " : "");
                }
                sb.append("\n\t\t"); // new line and tab indentation for the trailing bracket
            }

            sb.append("]");
        }

        sb.append("\nTops: ");
        sb.append(tops);
        sb.append("\nBottoms: ");
        sb.append(bottoms);

        return sb.toString();
    }

    @Override
    public Set<? extends AnnotationMirror> getTopAnnotations() {
        return this.tops;
    }

    @Override
    public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
        for (AnnotationMirror top : tops) {
            if (AnnotationUtils.areSame(start, top) || isSubtype(start, top)) {
                return top;
            }
        }
        throw new BugInCF(
                "MultiGraphQualifierHierarchy: did not find the top corresponding to qualifier "
                        + start
                        + " all tops: "
                        + tops);
    }

    @Override
    public Set<? extends AnnotationMirror> getBottomAnnotations() {
        return this.bottoms;
    }

    @Override
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
        for (AnnotationMirror bot : bottoms) {
            if (AnnotationUtils.areSame(start, bot) || isSubtype(bot, start)) {
                return bot;
            }
        }
        throw new BugInCF(
                "MultiGraphQualifierHierarchy: did not find the bottom corresponding to qualifier "
                        + start
                        + "; all bottoms: "
                        + bottoms
                        + "; this: "
                        + this);
    }

    @Override
    public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
        AnnotationMirror top = getTopAnnotation(start);
        for (AnnotationMirror key : polyQualifiers.keySet()) {
            if (AnnotationUtils.areSame(key, top)) {
                return polyQualifiers.get(key);
            }
        }

        if (AnnotationUtils.containsSame(polyQualifiers.keySet(), polymorphicQualifier)) {
            return polyQualifiers.get(polymorphicQualifier);
        } else {
            // No polymorphic qualifier exists for that hierarchy.
            throw new BugInCF(
                    "MultiGraphQualifierHierarchy: did not find the polymorphic qualifier corresponding to qualifier "
                            + start
                            + "; all polymorphic qualifiers: "
                            + polyQualifiers
                            + "; this: "
                            + this);
        }
    }

    @Override
    public boolean isSubtype(
            Collection<? extends AnnotationMirror> rhs,
            Collection<? extends AnnotationMirror> lhs) {
        rhs = replacePolyAll(rhs);
        lhs = replacePolyAll(lhs);
        if (lhs.isEmpty() || rhs.isEmpty()) {
            throw new BugInCF(
                    "MultiGraphQualifierHierarchy: empty annotations in lhs: "
                            + lhs
                            + " or rhs: "
                            + rhs);
        }
        if (lhs.size() != rhs.size()) {
            throw new BugInCF(
                    "MultiGraphQualifierHierarchy: mismatched number of annotations in lhs: "
                            + lhs
                            + " and rhs: "
                            + rhs);
        }
        int valid = 0;
        for (AnnotationMirror lhsAnno : lhs) {
            for (AnnotationMirror rhsAnno : rhs) {
                if (AnnotationUtils.areSame(getTopAnnotation(lhsAnno), getTopAnnotation(rhsAnno))
                        && isSubtype(rhsAnno, lhsAnno)) {
                    ++valid;
                }
            }
        }
        return lhs.size() == valid;
    }

    @Override
    public boolean isSubtypeTypeVariable(
            Collection<? extends AnnotationMirror> subAnnos,
            Collection<? extends AnnotationMirror> superAnnos) {
        for (AnnotationMirror top : getTopAnnotations()) {
            AnnotationMirror rhsForTop = findAnnotationInHierarchy(subAnnos, top);
            AnnotationMirror lhsForTop = findAnnotationInHierarchy(superAnnos, top);
            if (!isSubtypeTypeVariable(rhsForTop, lhsForTop)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<? extends AnnotationMirror> getTypeQualifiers() {
        return typeQualifiers;
    }

    // For caching results of lubs
    private Map<AnnotationPair, AnnotationMirror> lubs = null;

    @Override
    public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
        if (!AnnotationUtils.areSameIgnoringValues(getTopAnnotation(a1), getTopAnnotation(a2))) {
            return null;
        } else if (isSubtype(a1, a2)) {
            return a2;
        } else if (isSubtype(a2, a1)) {
            return a1;
        } else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
            return getTopAnnotation(a1);
        }
        if (lubs == null) {
            lubs = calculateLubs();
        }
        AnnotationPair pair = new AnnotationPair(a1, a2);
        return lubs.get(pair);
    }

    @Override
    public AnnotationMirror leastUpperBoundTypeVariable(AnnotationMirror a1, AnnotationMirror a2) {
        if (a1 == null || a2 == null) {
            // [] is a supertype of any qualifier, and [] <: []
            return null;
        }
        return leastUpperBound(a1, a2);
    }

    /** A cache of the results of glb computations. Maps from a pair of annotations to their glb. */
    private Map<AnnotationPair, AnnotationMirror> glbs = null;

    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
            return AnnotationUtils.areSame(a1, a2) ? a1 : getBottomAnnotation(a1);
        }
        if (glbs == null) {
            glbs = calculateGlbs();
        }
        AnnotationPair pair = new AnnotationPair(a1, a2);
        return glbs.get(pair);
    }

    @Override
    public AnnotationMirror greatestLowerBoundTypeVariable(
            AnnotationMirror a1, AnnotationMirror a2) {
        if (a1 == null) {
            // [] is a supertype of any qualifier, and [] <: []
            return a2;
        }
        if (a2 == null) {
            // [] is a supertype of any qualifier, and [] <: []
            return a1;
        }
        return greatestLowerBound(a1, a2);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Most qualifiers have no value fields. However, two annotations with values are subtype of
     * each other only if they have the same values. i.e. I(m) is a subtype of I(n) iff m = n
     *
     * <p>When client specifies an annotation, a1, to be a subtype of annotation with values, a2,
     * then a1 is a subtype of all instances of a2 regardless of a2 values.
     *
     * @param subAnno the sub qualifier
     * @param superAnno the super qualifier
     */
    @Override
    public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        checkAnnoInGraph(subAnno);
        checkAnnoInGraph(superAnno);

        /* TODO: this optimization leads to recursion
        for (AnnotationMirror top : tops) {
            System.out.println("Looking at top: " + tops + " and " + anno1);
            // We cannot use getRootAnnotation, as that would use subtyping and recurse
            if (isSubtype(anno1, top) && AnnotationUtils.areSame(top, anno2)) {
            return true;
            }
        }*/
        if (AnnotationUtils.areSameIgnoringValues(subAnno, superAnno)) {
            return AnnotationUtils.areSame(subAnno, superAnno);
        }
        Set<AnnotationMirror> supermap1 = this.supertypesMap.get(subAnno);
        return AnnotationUtils.containsSame(supermap1, superAnno);
    }

    @Override
    public boolean isSubtypeTypeVariable(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        if (superAnno == null) {
            // [] is a supertype of any qualifier, and [] <: []
            return true;
        }
        if (subAnno == null) {
            // [] is a subtype of no qualifier (only [])
            return false;
        }
        return isSubtype(subAnno, superAnno);
    }

    private final void checkAnnoInGraph(AnnotationMirror a) {
        if (AnnotationUtils.containsSame(supertypesMap.keySet(), a)
                || AnnotationUtils.containsSame(polyQualifiers.values(), a)) {
            return;
        }

        if (a == null) {
            throw new BugInCF(
                    "MultiGraphQualifierHierarchy found an unqualified type.  Please ensure that "
                            + "your implicit rules cover all cases and/or "
                            + "use a @DefaultQualifierInHierarchy annotation.");
        } else {
            // System.out.println("MultiGraphQH: " + this);
            throw new BugInCF(
                    "MultiGraphQualifierHierarchy found the unrecognized qualifier: "
                            + a
                            + ". Please ensure that the qualifier is correctly included in the subtype hierarchy.");
        }
    }

    /**
     * Infer the tops of the subtype hierarchy. Simple finds the qualifiers that have no supertypes.
     */
    // Not static to allow adaptation in subclasses. Only parameters should be modified.
    protected Set<AnnotationMirror> findTops(
            Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
        Set<AnnotationMirror> possibleTops = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : supertypes.keySet()) {
            if (supertypes.get(anno).isEmpty()) {
                possibleTops.add(anno);
            }
        }
        return possibleTops;
    }

    /**
     * Infer the bottoms of the subtype hierarchy. Simple finds the qualifiers that are not
     * supertypes of other qualifiers.
     */
    // Not static to allow adaptation in subclasses. Only parameters should be modified.
    protected Set<AnnotationMirror> findBottoms(
            Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
        Set<AnnotationMirror> possibleBottoms = AnnotationUtils.createAnnotationSet();
        possibleBottoms.addAll(supertypes.keySet());
        for (Set<AnnotationMirror> supers : supertypes.values()) {
            possibleBottoms.removeAll(supers);
        }
        return possibleBottoms;
    }

    /** Computes the transitive closure of the given map and returns it. */
    /* The method gets all required parameters passed in and could be static. However,
     * we want to allow subclasses to adapt the behavior and therefore make it an instance method.
     */
    protected Map<AnnotationMirror, Set<AnnotationMirror>> buildFullMap(
            Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
        Map<AnnotationMirror, Set<AnnotationMirror>> fullMap =
                AnnotationUtils.createAnnotationMap();
        for (AnnotationMirror anno : supertypes.keySet()) {
            // this method directly modifies fullMap and is
            // ignoring the returned value
            findAllSupers(anno, supertypes, fullMap);
        }
        return fullMap;
    }

    /**
     * Add the relationships for polymorphic qualifiers.
     *
     * <p>A polymorphic qualifier, such as {@code PolyNull}, needs to be:
     *
     * <ol>
     *   <li>a subtype of the top qualifier (e.g. {@code Nullable})
     *   <li>a supertype of all the bottom qualifiers (e.g. {@code NonNull})
     * </ol>
     *
     * Field supertypesMap is not set yet when this method is called -- use fullMap instead.
     */
    // The method gets all required parameters passed in and could be static. However,
    // we want to allow subclasses to adapt the behavior and therefore make it an instance method.
    protected void addPolyRelations(
            QualifierHierarchy qualHierarchy,
            Map<AnnotationMirror, Set<AnnotationMirror>> fullMap,
            Map<AnnotationMirror, AnnotationMirror> polyQualifiers,
            Set<AnnotationMirror> tops,
            Set<AnnotationMirror> bottoms) {
        if (polyQualifiers.isEmpty()) {
            return;
        }

        for (Map.Entry<AnnotationMirror, AnnotationMirror> kv : polyQualifiers.entrySet()) {
            AnnotationMirror declTop = kv.getKey();
            AnnotationMirror polyQualifier = kv.getValue();
            if (declTop == null
                    || // PolyAll
                    AnnotationUtils.areSame(declTop, polymorphicQualifier)) {
                if (declTop == null
                        || // PolyAll
                        tops.size() == 1) { // un-ambigous single top
                    AnnotationUtils.updateMappingToImmutableSet(fullMap, polyQualifier, tops);
                    for (AnnotationMirror bottom : bottoms) {
                        // Add the polyqualifier as a supertype
                        // Need to copy over the set as it is unmodifiable.
                        AnnotationUtils.updateMappingToImmutableSet(
                                fullMap, bottom, Collections.singleton(polyQualifier));
                    }
                    if (declTop == null) { // PolyAll
                        // Make all other polymorphic qualifiers a subtype of PolyAll
                        for (Map.Entry<AnnotationMirror, AnnotationMirror> otherpolyKV :
                                polyQualifiers.entrySet()) {
                            AnnotationMirror otherTop = otherpolyKV.getKey();
                            AnnotationMirror otherPoly = otherpolyKV.getValue();
                            if (otherTop != null) {
                                AnnotationUtils.updateMappingToImmutableSet(
                                        fullMap, otherPoly, Collections.singleton(polyQualifier));
                            }
                        }
                    }
                } else {
                    throw new BugInCF(
                            "MultiGraphQualifierHierarchy.addPolyRelations: "
                                    + "incorrect or missing top qualifier given in polymorphic qualifier "
                                    + polyQualifier
                                    + "; declTop = "
                                    + declTop
                                    + "; possible top qualifiers: "
                                    + tops);
                }
            } else {
                // Ensure that it's really the top of the hierarchy
                Set<AnnotationMirror> declSupers = fullMap.get(declTop);
                AnnotationMirror polyTop = null;
                if (declSupers.isEmpty()) {
                    polyTop = declTop;
                } else {
                    for (AnnotationMirror ds : declSupers) {
                        if (AnnotationUtils.containsSameIgnoringValues(tops, ds)) {
                            polyTop = ds;
                        }
                    }
                }
                boolean found = (polyTop != null);
                if (found) {
                    AnnotationUtils.updateMappingToImmutableSet(
                            fullMap, polyQualifier, Collections.singleton(polyTop));
                } else {
                    throw new BugInCF(
                            "MultiGraphQualifierHierarchy.addPolyRelations: "
                                    + "incorrect top qualifier given in polymorphic qualifier: "
                                    + polyQualifier
                                    + " could not find: "
                                    + polyTop);
                }

                found = false;
                AnnotationMirror bottom = null;
                outer:
                for (AnnotationMirror btm : bottoms) {
                    for (AnnotationMirror btmsuper : fullMap.get(btm)) {
                        if (AnnotationUtils.areSameIgnoringValues(btmsuper, polyTop)) {
                            found = true;
                            bottom = btm;
                            break outer;
                        }
                    }
                }
                if (found) {
                    AnnotationUtils.updateMappingToImmutableSet(
                            fullMap, bottom, Collections.singleton(polyQualifier));
                } else {
                    // TODO: in a type system with a single qualifier this check will fail.
                    // throw new BugInCF("MultiGraphQualifierHierarchy.addPolyRelations:
                    // " +
                    //        "incorrect top qualifier given in polymorphic qualifier: "
                    //
                    //        + polyQualifier + " could not find bottom for: " + polyTop);
                }
            }
        }
    }

    private Map<AnnotationPair, AnnotationMirror> calculateLubs() {
        Map<AnnotationPair, AnnotationMirror> newlubs = new HashMap<>();
        for (AnnotationMirror a1 : typeQualifiers) {
            for (AnnotationMirror a2 : typeQualifiers) {
                if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
                    continue;
                }
                if (!AnnotationUtils.areSame(getTopAnnotation(a1), getTopAnnotation(a2))) {
                    continue;
                }
                AnnotationPair pair = new AnnotationPair(a1, a2);
                if (newlubs.containsKey(pair)) {
                    continue;
                }
                AnnotationMirror lub = findLub(a1, a2);
                newlubs.put(pair, lub);
            }
        }
        return newlubs;
    }

    /**
     * Finds and returns the Least Upper Bound (LUB) of two annotation mirrors a1 and a2 by
     * recursively climbing the qualifier hierarchy of a1 until one of them is a subtype of the
     * other, or returns null if no subtype relationships can be found.
     *
     * @param a1 first annotation mirror
     * @param a2 second annotation mirror
     * @return the LUB of a1 and a2, or null if none can be found
     */
    protected AnnotationMirror findLub(AnnotationMirror a1, AnnotationMirror a2) {
        if (isSubtype(a1, a2)) {
            return a2;
        }
        if (isSubtype(a2, a1)) {
            return a1;
        }

        assert getTopAnnotation(a1) == getTopAnnotation(a2)
                : "MultiGraphQualifierHierarchy.findLub: this method may only be called "
                        + "with qualifiers from the same hierarchy. Found a1: "
                        + a1
                        + " [top: "
                        + getTopAnnotation(a1)
                        + "], a2: "
                        + a2
                        + " [top: "
                        + getTopAnnotation(a2)
                        + "]";

        if (isPolymorphicQualifier(a1)) {
            return findLubWithPoly(a1, a2);
        } else if (isPolymorphicQualifier(a2)) {
            return findLubWithPoly(a2, a1);
        }

        Set<AnnotationMirror> outset = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror a1Super : supertypesGraph.get(a1)) {
            // TODO: we take the first of the smallest supertypes, maybe we would
            // get a different LUB if we used a different one?
            AnnotationMirror a1Lub = findLub(a1Super, a2);
            if (a1Lub != null) {
                outset.add(a1Lub);
            } else {
                throw new BugInCF(
                        "GraphQualifierHierarchy could not determine LUB for "
                                + a1
                                + " and "
                                + a2
                                + ". Please ensure that the checker knows about all type qualifiers.");
            }
        }
        return requireSingleton(outset, a1, a2, /*lub=*/ true);
    }

    private AnnotationMirror findLubWithPoly(AnnotationMirror poly, AnnotationMirror other) {
        AnnotationMirror bottom = getBottomAnnotation(other);
        if (AnnotationUtils.areSame(bottom, other)) {
            return poly;
        }

        return getTopAnnotation(poly);
    }

    /** Sees if a particular annotation mirror is a polymorphic qualifier. */
    private boolean isPolymorphicQualifier(AnnotationMirror qual) {
        return AnnotationUtils.containsSame(polyQualifiers.values(), qual);
    }

    /** Remove all supertypes of elements contained in the set. */
    private Set<AnnotationMirror> findSmallestTypes(Set<AnnotationMirror> inset) {
        Set<AnnotationMirror> outset = AnnotationUtils.createAnnotationSet();
        outset.addAll(inset);

        for (AnnotationMirror a1 : inset) {
            Iterator<AnnotationMirror> outit = outset.iterator();
            while (outit.hasNext()) {
                AnnotationMirror a2 = outit.next();
                if (a1 != a2 && isSubtype(a1, a2)) {
                    outit.remove();
                }
            }
        }
        return outset;
    }

    /** Finds all the super qualifiers for a qualifier. */
    private static Set<AnnotationMirror> findAllSupers(
            AnnotationMirror anno,
            Map<AnnotationMirror, Set<AnnotationMirror>> supertypes,
            Map<AnnotationMirror, Set<AnnotationMirror>> allSupersSoFar) {
        Set<AnnotationMirror> supers = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror superAnno : supertypes.get(anno)) {
            // add the current super to the superset
            supers.add(superAnno);
            // add all of current super's super into superset
            supers.addAll(findAllSupers(superAnno, supertypes, allSupersSoFar));
        }
        allSupersSoFar.put(anno, Collections.unmodifiableSet(supers));
        return supers;
    }

    /** Returns a map from each possible pair of annotations to their glb. */
    private Map<AnnotationPair, AnnotationMirror> calculateGlbs() {
        Map<AnnotationPair, AnnotationMirror> newglbs = new HashMap<>();
        for (AnnotationMirror a1 : typeQualifiers) {
            for (AnnotationMirror a2 : typeQualifiers) {
                if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
                    continue;
                }
                if (!AnnotationUtils.areSame(getTopAnnotation(a1), getTopAnnotation(a2))) {
                    continue;
                }
                AnnotationPair pair = new AnnotationPair(a1, a2);
                if (newglbs.containsKey(pair)) {
                    continue;
                }
                AnnotationMirror glb = findGlb(a1, a2);
                newglbs.put(pair, glb);
            }
        }
        return newglbs;
    }

    private AnnotationMirror findGlb(AnnotationMirror a1, AnnotationMirror a2) {
        if (isSubtype(a1, a2)) {
            return a1;
        }
        if (isSubtype(a2, a1)) {
            return a2;
        }

        assert getTopAnnotation(a1) == getTopAnnotation(a2)
                : "MultiGraphQualifierHierarchy.findGlb: this method may only be called "
                        + "with qualifiers from the same hierarchy. Found a1: "
                        + a1
                        + " [top: "
                        + getTopAnnotation(a1)
                        + "], a2: "
                        + a2
                        + " [top: "
                        + getTopAnnotation(a2)
                        + "]";

        if (isPolymorphicQualifier(a1)) {
            return findGlbWithPoly(a1, a2);
        } else if (isPolymorphicQualifier(a2)) {
            return findGlbWithPoly(a2, a1);
        }

        Set<AnnotationMirror> outset = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror a1Sub : supertypesGraph.keySet()) {
            if (isSubtype(a1Sub, a1) && !a1Sub.equals(a1)) {
                AnnotationMirror a1lb = findGlb(a1Sub, a2);
                if (a1lb != null) {
                    outset.add(a1lb);
                }
            }
        }
        return requireSingleton(outset, a1, a2, /*lub=*/ false);
    }

    private AnnotationMirror findGlbWithPoly(AnnotationMirror poly, AnnotationMirror other) {
        AnnotationMirror top = getTopAnnotation(other);
        if (AnnotationUtils.areSame(top, other)) {
            return poly;
        }

        return getBottomAnnotation(poly);
    }

    /** Remove all subtypes of elements contained in the set. */
    private Set<AnnotationMirror> findGreatestTypes(Set<AnnotationMirror> inset) {
        Set<AnnotationMirror> outset = AnnotationUtils.createAnnotationSet();
        outset.addAll(inset);

        for (AnnotationMirror a1 : inset) {
            Iterator<AnnotationMirror> outit = outset.iterator();
            while (outit.hasNext()) {
                AnnotationMirror a2 = outit.next();
                if (a1 != a2 && isSubtype(a2, a1)) {
                    outit.remove();
                }
            }
        }
        return outset;
    }

    /**
     * Require that outset is a singleton set, after polymorphic qualifiers have been removed. If
     * not, report a bug: the type hierarchy is not a lattice.
     *
     * @param outset the set of upper or lower bounds of a1 and a2 (depending on whether lub==true)
     * @param a1 the first annotation being lubbed or glbed
     * @param a2 the second annotation being lubbed or glbed
     * @param lub true if computing lub(a1, a2), false if computing glb(a1, a2)
     * @return the unique element of outset; issues an error if outset.size() != 1
     */
    private AnnotationMirror requireSingleton(
            Set<AnnotationMirror> outset, AnnotationMirror a1, AnnotationMirror a2, boolean lub) {
        if (outset.size() == 0) {
            throw new BugInCF(
                    "MultiGraphQualifierHierarchy could not determine "
                            + (lub ? "LUB" : "GLB")
                            + " for "
                            + a1
                            + " and "
                            + a2
                            + ". Please ensure that the checker knows about all type qualifiers.");
        } else if (outset.size() == 1) {
            return outset.iterator().next();
        } else {
            // outset.size() > 1

            outset = lub ? findSmallestTypes(outset) : findGreatestTypes(outset);

            AnnotationMirror result = null;
            for (AnnotationMirror anno : outset) {
                if (isPolymorphicQualifier(anno)) {
                    continue;
                } else if (result == null) {
                    result = anno;
                } else {
                    throw new BugInCF(
                            String.format(
                                    "Bug in checker implementation:  type hierarchy is not a lattice.%n"
                                            + "There is no unique "
                                            + (lub ? "lub" : "glb")
                                            + "(%s, %s).%n"
                                            + "Two incompatible candidates are: %s %s",
                                    a1,
                                    a2,
                                    result,
                                    anno));
                }
            }
            return result;
        }
    }

    /** Two annotations; used for caching the result of calls to lub and glb. */
    private static class AnnotationPair {
        /** The first annotation. */
        public final AnnotationMirror a1;
        /** The second annotation. */
        public final AnnotationMirror a2;
        /** The cached hashCode of this; -1 until computed. */
        private int hashCode = -1;

        /** Create a new AnnotationPair. */
        public AnnotationPair(AnnotationMirror a1, AnnotationMirror a2) {
            this.a1 = a1;
            this.a2 = a2;
        }

        @Pure
        @Override
        public int hashCode() {
            if (hashCode == -1) {
                hashCode = 31;
                if (a1 != null) {
                    hashCode += 17 * AnnotationUtils.annotationName(a1).hashCode();
                }
                if (a2 != null) {
                    hashCode += 17 * AnnotationUtils.annotationName(a2).hashCode();
                }
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof AnnotationPair)) {
                return false;
            }
            AnnotationPair other = (AnnotationPair) o;
            if (AnnotationUtils.areSameIgnoringValues(a1, other.a1)
                    && AnnotationUtils.areSameIgnoringValues(a2, other.a2)) {
                return true;
            }
            if (AnnotationUtils.areSameIgnoringValues(a2, other.a1)
                    && AnnotationUtils.areSameIgnoringValues(a1, other.a2)) {
                return true;
            }
            return false;
        }

        @SideEffectFree
        @Override
        public String toString() {
            return "AnnotationPair(" + a1 + ", " + a2 + ")";
        }
    }
}
