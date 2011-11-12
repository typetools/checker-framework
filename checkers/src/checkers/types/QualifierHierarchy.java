package checkers.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;

import checkers.util.AnnotationUtils;

/**
 * Represents a type qualifier hierarchy.
 *
 * All method parameter annotations need to be type qualifiers recognized
 * within this hierarchy.
 *
 * This assumes that any particular annotated type in a program is annotated
 * with at least one qualifier from the hierarchy.
 */
public abstract class QualifierHierarchy {

    // **********************************************************************
    // Getter methods about this hierarchy
    // **********************************************************************

    /**
     * @return  the root (ultimate super) type qualifier in the hierarchy
     */
	public abstract Set<AnnotationMirror> getRootAnnotations();
	
	/**
	 * Return the root for the given qualifier, that is, the qualifier that is a
	 * supertype of start but no further supertypes exist. 
	 */
	public abstract AnnotationMirror getRootAnnotation(AnnotationMirror start);

	/**
	 * Return the bottom for the given qualifier, that is, the qualifier that is a
	 * subtype of start but no further subtypes exist. 
	 */	
	public abstract AnnotationMirror getBottomAnnotation(AnnotationMirror start);

    /**
     * @return the bottom type qualifier in the hierarchy
     */
    public abstract Set<AnnotationMirror> getBottomAnnotations();

    /**
     * Returns the names of all type qualifiers in this type qualifier
     * hierarchy
     *
     * @return the fully qualified name represented in this hierarchy
     */
    public abstract Set<Name> getTypeQualifiers();

    // **********************************************************************
    // Qualifier Hierarchy Queries
    // **********************************************************************

    /**
     * Tests whether anno1 is a sub-qualifier of anno2, according to the
     * type qualifier hierarchy.  This checks only the qualifiers, not the
     * Java type.
     *
     * @return true iff anno1 is a sub qualifier of anno2
     */
    public abstract boolean isSubtype(AnnotationMirror anno1, AnnotationMirror anno2);

    /**
     * Tests whether there is any annotation in lhs that is a super qualifier
     * of some annotation in rhs.
     * lhs and rhs contain only the annotations, not the Java type.
     *
     * @return true iff an annotation in lhs is a super of one in rhs
     **/
    // This method requires more revision.
    // The only case were rhs and lhs have more than one qualifier is in IGJ
    // where the type of 'this' is '@AssignsFields @I FOO'.  Subtyping for
    // this case, requires subtyping with respect to one qualifier only.
    public abstract boolean isSubtype(Collection<AnnotationMirror> rhs, Collection<AnnotationMirror> lhs);

    /**
     * Returns the  least upper bound for the qualifiers a1 and a2.
     *
     * Examples:
     * For NonNull, leastUpperBound('Nullable', 'NonNull') ==> Nullable
     * For IGJ,     leastUpperBound('Immutable', 'Mutable') ==> ReadOnly
     *
     * @return  the least restrictive qualifiers for both types
     */
    public abstract AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the greatest lower bound for the qualifiers a1 and a2.
     *
     * @param a1 First annotation
     * @param a2 Second annotation
     * @return Greatest lower bound of the two annotations
     */
    public abstract AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the type qualifiers that are the least upper bound of
     * the qualifiers in annos1 and annos2.
     * <p>
     *
     * This is necessary for determining the type of a conditional
     * expression (<tt>?:</tt>), where the type of the expression is the
     * least upper bound of the true and false clauses.
     * <p>
     *
     * The current implementation returns the intersection of annos1 and
     * annos2 along with the qualifier that is the least upper bound of all
     * other annotations.
     *
     * @return the least upper bound of annos1 and annos2
     */
    public Set<AnnotationMirror>
    leastUpperBound(Collection<AnnotationMirror> annos1, Collection<AnnotationMirror> annos2) {
        Collection<AnnotationMirror> as1 = annos1;
        Collection<AnnotationMirror> as2 = annos2;

        if (as1.size() == 1 && as2.size() == 1) {
            AnnotationMirror a1 = as1.iterator().next();
            AnnotationMirror a2 = as2.iterator().next();
            return Collections.singleton(leastUpperBound(a1, a2));
        }

        // Let's hope that the difference is simply two elements
        Set<AnnotationMirror> difference = difference(as1, as2);
        Set<AnnotationMirror> lub = AnnotationUtils.createAnnotationSet();
        lub.addAll(intersect(as1, as2));

        if (difference.isEmpty())
            return lub;
        AnnotationMirror lubOfDiff = difference.iterator().next();
        for (AnnotationMirror a : difference)
            lubOfDiff = leastUpperBound(lubOfDiff, a);
        lub.add(lubOfDiff);
        return lub;
    }


    /**
     * @return the intersection set of as1 and as2
     */
    protected Set<AnnotationMirror> intersect(Collection<AnnotationMirror> as1, Collection<AnnotationMirror> as2) {
        Set<AnnotationMirror> intersects = AnnotationUtils.createAnnotationSet();
        intersects.addAll(as1);
        intersects.retainAll(as2);
        return intersects;
    }

    /**
     * @return the elements belonging to exactly one of as1 or as2
     */
    protected Set<AnnotationMirror> difference(Collection<AnnotationMirror> as1, Collection<AnnotationMirror> as2) {
        Set<AnnotationMirror> difference = AnnotationUtils.createAnnotationSet();
        difference.addAll(as1);
        difference.addAll(as2);
        difference.removeAll(intersect(as1, as2));
        return difference;
    }
}
