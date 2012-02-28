package checkers.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;

import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;
import checkers.quals.PolymorphicQualifier;
import checkers.source.SourceChecker;
import checkers.types.QualifierHierarchy;

//It's functional, but requires optimization and better documentation
//
/**
 * Represents the type qualifier hierarchy of a type system.
 *
 * This class is immutable and can be only created through {@link MultiGraphFactory}.
 * 
 * A QualifierHierarchy that supports multiple separate subtype hierarchies.
 */
public class MultiGraphQualifierHierarchy extends QualifierHierarchy {

    /**
     * Factory used to create an instance of {@link GraphQualifierHierarchy}.
     * A factory can be used to create at most one {@link GraphQualifierHierarchy}.
     *
     * To create a hierarchy, a client may do so in three steps:
     * 1. add qualifiers using {@link #addQualifier(AnnotationMirror)};
     * 2. add subtype relations using {@link #addSubtype(AnnotationMirror, AnnotationMirror)}
     * 3. build the hierarchy and gets using {@link #build()}.
     *
     * Notice that {@link #addSubtype(AnnotationMirror, AnnotationMirror)} adds
     * the two qualifiers to the hierarchy if they are not already in.
     *
     * Also, once the client builds a hierarchy through {@link #build()},
     * no further modifications are allowed nor can it making a new instance.
     *
     * Clients build the hierarchy using {@link #addQualifier(AnnotationMirror)}
     * and {@link #addSubtype(AnnotationMirror, AnnotationMirror)}, then get
     * the instance with calling {@link #build()}
     */
    public static class MultiGraphFactory {
        /** map: qualifier --> supertypesMap of the qualifier */
        // supertypesMap is immutable once GraphQualifierHierarchy is built
        protected final Map<AnnotationMirror, Set<AnnotationMirror>> supertypes;

        AnnotationMirror polyQualifier;

        private final SourceChecker checker;
        
        public MultiGraphFactory(SourceChecker checker) {
            supertypes = AnnotationUtils.createAnnotationMap();
            this.checker = checker;
        }

        /**
         * Adds the passed qualifier to the hierarchy.  Clients need to specify
         * its super qualifiers in subsequent calls to
         * {@link #addSubtype(AnnotationMirror, AnnotationMirror)}.
         */
        public void addQualifier(AnnotationMirror qual) {
            assertNotBuilt();
            if (supertypes.containsKey(qual))
                return;
            supertypes.put(qual, AnnotationUtils.createAnnotationSet());

            if (isPolymorphic(qual))
                this.polyQualifier = qual;
        }

        private boolean isPolymorphic(AnnotationMirror qual) {
            if (qual == null)
                return false;
            Element qualElt = qual.getAnnotationType().asElement();
            return qualElt.getAnnotation(PolymorphicQualifier.class) != null;
        }

        /**
         * Adds a subtype relationship between the two type qualifiers.
         *
         * @param sub   the sub type qualifier
         * @param sup   the super type qualifier
         */
        public void addSubtype(AnnotationMirror sub, AnnotationMirror sup) {
            assertNotBuilt();
            addQualifier(sub);
            addQualifier(sup);
            supertypes.get(sub).add(sup);
        }

        /**
         * Returns an instance of {@link GraphQualifierHierarchy} that
         * represents the hierarchy built so far
         */
        public QualifierHierarchy build() {
            assertNotBuilt();
            addPolyRelations();
            QualifierHierarchy result = createQualifierHierarchy();
            wasBuilt = true;
            return result;
        }

    	protected QualifierHierarchy createQualifierHierarchy() {
    		return new MultiGraphQualifierHierarchy(this);
    	}

        private boolean wasBuilt = false;

        protected void assertNotBuilt() {
            if (wasBuilt) {
                checker.errorAbort("GraphQualifierHierarchy.Factory was already built. Method build can only be called once.");
            }
        }

        /**
         * Add the relationships for polymorphic qualifiers.
         *
         * A polymorphic qualifier needs to be (take {@link PolyNull} for example)
         * 1. a subtype of the root qualifier (e.g. {@link Nullable})
         * 2. a supertype of all the bottom qualifiers  (e.g. {@link NonNull})
         */
        protected void addPolyRelations() {
            if (polyQualifier == null)
                return;

            // find its supertypesMap
            if (supertypes.get(polyQualifier).isEmpty()) {
                for (AnnotationMirror root : findRoots(checker, supertypes, polyQualifier)) {
                	addSubtype(polyQualifier, root);
                }
            }

            Set<AnnotationMirror> bottoms = findBottoms(supertypes, polyQualifier);
            for (AnnotationMirror bottom : bottoms) {
                addSubtype(bottom, polyQualifier);
            }
        }
    }

    protected final Map<AnnotationMirror, Set<AnnotationMirror>> supertypesGraph;

    /** immutable map: qualifier --> supertypesMap of the qualifier**/
    // Contains all supertypes, not just the direct supertypes of the qualifier
    protected final Map<AnnotationMirror, Set<AnnotationMirror>> supertypesMap;

    /** the root of all the qualifiers **/
    protected final Set<AnnotationMirror> roots;
    protected final Set<AnnotationMirror> bottoms;

    protected MultiGraphQualifierHierarchy(MultiGraphFactory f) {
        super(f.checker);
        // no need for copying as f.supertypes has no mutable references to it
        this.supertypesGraph = f.supertypes;
        this.supertypesMap = buildFullMap(f.supertypes);

    	this.roots = findRoots(checker, this.supertypesMap, f.polyQualifier);
        this.bottoms = findBottoms(this.supertypesMap, f.polyQualifier);
    }

	protected MultiGraphQualifierHierarchy(MultiGraphQualifierHierarchy h) {
        super(h.checker);
        this.supertypesGraph = h.supertypesGraph;
        this.supertypesMap = h.supertypesMap;
        this.lubs = h.lubs;
        this.glbs = h.glbs;
		this.roots = h.roots;
		this.bottoms = h.bottoms;
	}

	@Override
	public String toString() {
	    // TODO: it would be easier to debug if the graph and map were sorted by the key.
	    // Simply creating a TreeMap here doesn't work, because AnnotationMirrors are not comparable.
	    return "Supertypes Graph: " + supertypesGraph.toString() +
	            "\nSupertypes Map: " + supertypesMap.toString() +
	            "\nRoots: " + roots +
	            "\nBottoms: " + bottoms;
	}

	@Override
    public Set<AnnotationMirror> getRootAnnotations() {
        return this.roots;
    }

    @Override
    public AnnotationMirror getRootAnnotation(AnnotationMirror start) {
    	for (AnnotationMirror root : roots) {
    		if (AnnotationUtils.areSameIgnoringValues(start, root) ||
    		        isSubtype(start, root)) {
    			return root;
    		}
    	}
    	checker.errorAbort("Did not find the root corresponding to qualifier " + start +
    	        " all roots: " + roots);
        return null;
    }

    @Override
    public AnnotationMirror getBottomAnnotation(AnnotationMirror start) {
    	for (AnnotationMirror bot : bottoms) {
    		if (AnnotationUtils.areSameIgnoringValues(start, bot) ||
    		        isSubtype(bot, start)) {
    			return bot;
    		}
    	}
    	checker.errorAbort("Did not find the bottom corresponding to qualifier " + start +
    	        " all bottoms: " + bottoms);
        return null;
    }

    @Override
    public Set<AnnotationMirror> getBottomAnnotations() {
        return this.bottoms;
    }

    @Override
    public boolean isSubtype(Collection<AnnotationMirror> rhs, Collection<AnnotationMirror> lhs) {
        if (lhs.isEmpty() || rhs.isEmpty()) {
            checker.errorAbort("QualifierHierarchy: Empty annotations in lhs: " + lhs + " or rhs: " + rhs);
        }
        if (lhs.size() != rhs.size()) {
            // checker.errorAbort("QualifierHierarchy: mismatched number of annotations in lhs: " + lhs + " and rhs: " + rhs);
            return false;
        }
        int valid = 0;
        for (AnnotationMirror lhsAnno : lhs) {
            for (AnnotationMirror rhsAnno : rhs) {
                if (getRootAnnotation(lhsAnno) == getRootAnnotation(rhsAnno) &&
                		isSubtype(rhsAnno, lhsAnno)) {
                	++valid;
                }
            }
        }
        return lhs.size()==valid;
    }


    private Set<Name> typeQualifiers = null;

    @Override
    public Set<Name> getTypeQualifiers() {
        if (typeQualifiers != null)
            return typeQualifiers;
        Set<Name> names = new HashSet<Name>();
        for (AnnotationMirror anno: supertypesMap.keySet())
            names.add(AnnotationUtils.annotationName(anno));
        typeQualifiers = names;
        return typeQualifiers;
    }


    // For caching results of lubs
    private Map<AnnotationPair, AnnotationMirror> lubs = null;

    @Override
    public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
        if (isSubtype(a1, a2)) {
            return a2;
        } else if (isSubtype(a2, a1)) {
            return a1;
        } else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
            return getRootAnnotation(a1);
        }
        if (lubs == null) {
            lubs = calculateLubs();
        }
        AnnotationPair pair = new AnnotationPair(a1, a2);
        return lubs.get(pair);
    }


    // For caching results of glbs
    private Map<AnnotationPair, AnnotationMirror> glbs = null;

    @Override
    public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
        if (AnnotationUtils.areSameIgnoringValues(a1, a2))
            return AnnotationUtils.areSame(a1, a2) ? a1 : getBottomAnnotation(a1);
        if (glbs == null) {
            glbs = calculateGlbs();
        }
        AnnotationPair pair = new AnnotationPair(a1, a2);
        return glbs.get(pair);
    }

    /**
     * Most qualifiers have no value fields.  However, two annotations with
     * values are subtype of each other only if they have the same values.
     * i.e. I(m) is a subtype of I(n) iff m = n
     *
     * When client specifies an annotation, a1, to be a subtype of annotation
     * with values, a2, then a1 is a subtype of all instances of a2 regardless
     * of a2 values.  i.e. IGJBottom is a subtype of all instances of
     * {@code @I}.
     *
     */
    @Override
    public boolean isSubtype(AnnotationMirror anno1, AnnotationMirror anno2) {
        if (AnnotationUtils.areSameIgnoringValues(anno1, anno2))
            return AnnotationUtils.areSame(anno1, anno2);
        /* TODO: this optimization leads to recursion
        for (AnnotationMirror root : roots) {
        	System.out.println("Looking at root: " + root + " and " + anno1);
        	// We cannot use getRootAnnotation, as that would use subtyping and recurse
        	if (isSubtype(anno1, root) && AnnotationUtils.areSame(root, anno2))
        		return true;
        }*/
        checkAnnoInGraph(anno1);
        checkAnnoInGraph(anno2);
        return this.supertypesMap.get(anno1).contains(anno2);
    }

    private final void checkAnnoInGraph(AnnotationMirror a) {
        if (supertypesMap.containsKey(a))
            return;

        if (a == null) {
            checker.errorAbort("MultiGraphQualifierHierarchy found an unqualified type.  Please ensure that " +
                    "your implicit rules cover all cases and/or " +
                    "use a @DefaulQualifierInHierarchy annotation.");
        } else {
            checker.errorAbort("MultiGraphQualifierHierarchy found the unrecognized qualifier: " + a +
            		". Please ensure that the qualifier is correctly included in the subtype hierarchy.");
        }
    }

    protected static Set<AnnotationMirror>
    findRoots(SourceChecker checker, Map<AnnotationMirror, Set<AnnotationMirror>> supertypes, AnnotationMirror ignore) {
        Set<AnnotationMirror> possibleRoots = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : supertypes.keySet()) {
            if (supertypes.get(anno).isEmpty())
                possibleRoots.add(anno);
        }

        if (ignore != null)
            possibleRoots.remove(ignore);

        return possibleRoots;
    }

    /**
     * Infer the bottoms of the subtype hierarchy.  Simple finds the qualifiers
     * that are not supertypes of other qualifiers.
     *
     * @param ignore
     *      a qualifier that cannot be a bottom candidate, like polymorphic
     *      qualifier
     */
    protected static Set<AnnotationMirror>
    findBottoms(Map<AnnotationMirror, Set<AnnotationMirror>> supertypes, AnnotationMirror ignore) {
        Set<AnnotationMirror> bottoms = AnnotationUtils.createAnnotationSet();
        bottoms.addAll(supertypes.keySet());
        for (Set<AnnotationMirror> supers : supertypes.values()) {
            bottoms.removeAll(supers);
        }
        if (ignore != null)
            bottoms.remove(ignore);
        return bottoms;
    }

    protected static Map<AnnotationMirror, Set<AnnotationMirror>>
    buildFullMap(Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
        Map<AnnotationMirror, Set<AnnotationMirror>> fullMap = AnnotationUtils.createAnnotationMap();
        for (AnnotationMirror anno : supertypes.keySet()) {
            findAllSupers(anno, supertypes, fullMap);
        }
        return fullMap;
    }

    private Map<AnnotationPair, AnnotationMirror>  calculateLubs() {
        Map<AnnotationPair, AnnotationMirror> newlubs = new HashMap<AnnotationPair, AnnotationMirror>();
        for (AnnotationMirror a1 : supertypesGraph.keySet()) {
            for (AnnotationMirror a2 : supertypesGraph.keySet()) {
                if (AnnotationUtils.areSameIgnoringValues(a1, a2))
                    continue;
                if (!AnnotationUtils.areSame(getRootAnnotation(a1), getRootAnnotation(a2)))
                    continue;
                AnnotationPair pair = new AnnotationPair(a1, a2);
                if (newlubs.containsKey(pair))
                    continue;
                AnnotationMirror lub = findLub(a1, a2);
                newlubs.put(pair, lub);
            }
        }
        return newlubs;
    }

    private AnnotationMirror findLub(AnnotationMirror a1, AnnotationMirror a2) {
        if (isSubtype(a1, a2))
            return a2;
        if (isSubtype(a2, a1))
            return a1;

        assert getRootAnnotation(a1) == getRootAnnotation(a2) :
            "MultiGraphQualifierHierarchy.findLub: this method may only be called " +
                "with qualifiers from the same hierarchy. Found a1: " + a1 + " [root: " + getRootAnnotation(a1) +
                "], a2: " + a2 + " [root: " + getRootAnnotation(a2) + "]";

        Set<AnnotationMirror> outset = new HashSet<AnnotationMirror>();
        for (AnnotationMirror a1Super : findSmallestTypes(supertypesMap.get(a1))) {
            // TODO: we take the first of the smallest supertypes, maybe we would
            // get a different LUB if we used a different one?
            AnnotationMirror a1Lub = findLub(a1Super, a2);
            if (a1Lub != null) {
                outset.add(a1Lub);
            }
            if (a1Lub==null && a1Super==null) {
                // null is also used for Unqualified! If two qualifiers are separate
                // subtypes of unqualifed, this might happen.
                // I ran into this when KeyFor <: Unqualified and Covariant <: Unqualified.
                // I think it would be much nicer if Unqualified would not be optimized away...
                // TODO This never seems to happen...
                outset.add(null);
            }
        }
        if (outset.size()==1) {
            return outset.iterator().next();
        }
        if (outset.size()>1) {
            outset = findSmallestTypes(outset);
            // TODO: more than one, incomparable supertypes. Just pick the first one.
            // if (outset.size()>1) { System.out.println("Still more than one LUB!"); }
            return outset.iterator().next();
        }

        checker.errorAbort("GraphQualifierHierarchy could not determine LUB for " + a1 + " and " + a2 +
                                 ". Please ensure that the checker knows about all type qualifiers.");
        return null;
    }

    // remove all supertypes of elements contained in the set
    private Set<AnnotationMirror> findSmallestTypes(Set<AnnotationMirror> inset) {
        Set<AnnotationMirror> outset = new HashSet<AnnotationMirror>(inset);

        for( AnnotationMirror a1 : inset ) {
            Iterator<AnnotationMirror> outit = outset.iterator();
            while( outit.hasNext() ) {
                AnnotationMirror a2 = outit.next();
                if( a1!=a2 && isSubtype(a1, a2) ) {
                    outit.remove();
                }
            }
        }
        return outset;
    }

    /**
     * Finds all the super qualifiers for an qualifier
     *
     * @param anno
     * @param supertypesMap
     * @return
     */
    private static Set<AnnotationMirror>
    findAllSupers(AnnotationMirror anno,
            Map<AnnotationMirror, Set<AnnotationMirror>> supertypes,
            Map<AnnotationMirror, Set<AnnotationMirror>> allSupersSoFar) {
        Set<AnnotationMirror> supers = AnnotationUtils.createAnnotationSet();
        if (allSupersSoFar.containsKey(anno))
            return Collections.unmodifiableSet(allSupersSoFar.get(anno));

        for (AnnotationMirror superAnno : supertypes.get(anno)) {
            supers.add(superAnno);
            supers.addAll(findAllSupers(superAnno, supertypes, allSupersSoFar));
        }
        allSupersSoFar.put(anno, Collections.unmodifiableSet(supers));
        return supers;
    }


    private Map<AnnotationPair, AnnotationMirror>  calculateGlbs() {
        Map<AnnotationPair, AnnotationMirror> newglbs = new HashMap<AnnotationPair, AnnotationMirror>();
        for (AnnotationMirror a1 : supertypesGraph.keySet()) {
            for (AnnotationMirror a2 : supertypesGraph.keySet()) {
                if (AnnotationUtils.areSameIgnoringValues(a1, a2))
                    continue;
                if (!AnnotationUtils.areSame(getRootAnnotation(a1), getRootAnnotation(a2)))
                    continue;
                AnnotationPair pair = new AnnotationPair(a1, a2);
                if (newglbs.containsKey(pair))
                    continue;
                AnnotationMirror glb = findGlb(a1, a2);
                newglbs.put(pair, glb);
            }
        }
        return newglbs;
    }

    private AnnotationMirror findGlb(AnnotationMirror a1, AnnotationMirror a2) {
        if (isSubtype(a1, a2))
            return a1;
        if (isSubtype(a2, a1))
            return a2;

        assert getRootAnnotation(a1) == getRootAnnotation(a2) :
            "MultiGraphQualifierHierarchy.findGlb: this method may only be called " +
                "with qualifiers from the same hierarchy. Found a1: " + a1 + " [root: " + getRootAnnotation(a1) +
                "], a2: " + a2 + " [root: " + getRootAnnotation(a2) + "]";

        Set<AnnotationMirror> outset = new HashSet<AnnotationMirror>();
        for (AnnotationMirror a1Sub : supertypesGraph.keySet()) {
            if (isSubtype(a1Sub, a1) && !a1Sub.equals(a1)) {
                AnnotationMirror a1lb = findGlb(a1Sub, a2);
                if (a1lb != null)
                    outset.add(a1lb);
            }
        }
        if (outset.size()==1) {
            return outset.iterator().next();
        }
        if (outset.size()>1) {
            outset = findGreatestTypes(outset);
            // TODO: more than one, incomparable subtypes. Pick the first one.
            // if (outset.size()>1) { System.out.println("Still more than one GLB!"); }
            return outset.iterator().next();
        }

        checker.errorAbort("GraphQualifierHierarchy could not determine GLB for " + a1 + " and " + a2 +
                ". Please ensure that the checker knows about all type qualifiers.");
        return null;
    }

    // remove all subtypes of elements contained in the set
    private Set<AnnotationMirror> findGreatestTypes(Set<AnnotationMirror> inset) {
        Set<AnnotationMirror> outset = new HashSet<AnnotationMirror>(inset);

        for( AnnotationMirror a1 : inset ) {
            Iterator<AnnotationMirror> outit = outset.iterator();
            while( outit.hasNext() ) {
                AnnotationMirror a2 = outit.next();
                if( a1!=a2 && isSubtype(a2, a1) ) {
                    outit.remove();
                }
            }
        }
        return outset;
    }


    private static class AnnotationPair {
        public final AnnotationMirror a1;
        public final AnnotationMirror a2;
        private int hashCode = -1;

        public AnnotationPair(AnnotationMirror a1, AnnotationMirror a2) {
            this.a1 = a1;
            this.a2 = a2;
        }

        @Override
        public int hashCode() {
            if (hashCode == -1) {
                hashCode = 31;
                if (a1 != null)
                    hashCode += 17 * AnnotationUtils.annotationName(a1).toString().hashCode();
                if (a2 != null)
                    hashCode += 17 * AnnotationUtils.annotationName(a2).toString().hashCode();
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof AnnotationPair))
                return false;
            AnnotationPair other = (AnnotationPair)o;
            if (AnnotationUtils.areSameIgnoringValues(a1, other.a1)
                    && AnnotationUtils.areSameIgnoringValues(a2, other.a2))
                return true;
            if (AnnotationUtils.areSameIgnoringValues(a2, other.a1)
                    && AnnotationUtils.areSameIgnoringValues(a1, other.a2))
                return true;
            return false;
        }

        @Override
        public String toString() {
            return "AnnotationPair(" + a1 + ", " + a2 + ")";
        }
    }
}
