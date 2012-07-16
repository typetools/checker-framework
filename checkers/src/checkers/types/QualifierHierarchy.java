package checkers.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;

import checkers.source.SourceChecker;
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

    /* The checker to use for error reporting.
     * The field should be final, 
     */
    protected final SourceChecker checker;

    protected QualifierHierarchy(SourceChecker c) {
        this.checker = c;
    }

    // **********************************************************************
    // Getter methods about this hierarchy
    // **********************************************************************

    /**
     * @return  the top (ultimate super) type qualifiers in the type system
     */
    public abstract Set<AnnotationMirror> getTopAnnotations();

    /**
     * Return the top qualifier for the given qualifier, that is, the qualifier
     * that is a supertype of start but no further supertypes exist. 
     */
    public abstract AnnotationMirror getTopAnnotation(AnnotationMirror start);

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
     * hierarchy.
     * TODO: What is the relation to {@link checkers.basetype.BaseTypeChecker#getSupportedTypeQualifiers()}?
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
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
     * 
     * @return  the least restrictive qualifiers for both types
     */
    public abstract AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2);

    /**
     * Returns the greatest lower bound for the qualifiers a1 and a2.
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
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
     * @return the least upper bound of annos1 and annos2
     */
    public Set<AnnotationMirror>
    leastUpperBounds(Collection<AnnotationMirror> annos1, Collection<AnnotationMirror> annos2) {
        if (annos1.size() == 1 && annos2.size() == 1) {
            AnnotationMirror a1 = annos1.iterator().next();
            AnnotationMirror a2 = annos2.iterator().next();
            return Collections.singleton(leastUpperBound(a1, a2));
        }

        assert annos1.size() == annos2.size() && annos1.size()!=0 :
            "QualifierHierarchy.leastUpperBounds: tried to determine LUB with empty sets or sets of different sizes!\n" +
                    "    Set 1: " + annos1 + " Set 2: " + annos2;

        Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror a1 : annos1) {
            for (AnnotationMirror a2 : annos2) {
                AnnotationMirror lub = leastUpperBound(a1, a2);
                if (lub!=null) {
                    result.add(lub);
                }
            }
        }

        assert result.size() == annos1.size() : "QualifierHierarchy.leastUpperBounds: resulting set has incorrect number of annotations!\n" +
                "    Set 1: " + annos1 + " Set 2: " + annos2 + " LUB: " + result;

        return result;
    }

    /**
     * Returns the type qualifiers that are the greatest lower bound of
     * the qualifiers in annos1 and annos2.
     *
     * The two qualifiers have to be from the same qualifier hierarchy. Otherwise,
     * null will be returned.
     * 
     * @param annos1 First collection of qualifiers
     * @param annos2 Second collection of qualifiers
     * @return Greatest lower bound of the two collections of qualifiers
     */
    public Set<AnnotationMirror>
    greatestLowerBounds(Collection<AnnotationMirror> annos1, Collection<AnnotationMirror> annos2) {
        if (annos1.size() == 1 && annos2.size() == 1) {
            AnnotationMirror a1 = annos1.iterator().next();
            AnnotationMirror a2 = annos2.iterator().next();
            return Collections.singleton(greatestLowerBound(a1, a2));
        }

        assert annos1.size() == annos2.size() && !annos1.isEmpty() :
            "QualifierHierarchy.greatestLowerBounds: tried to determine GLB with empty sets or sets of different sizes!\n" +
                    "    Set 1: " + annos1 + " Set 2: " + annos2;

        Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror a1 : annos1) {
            for (AnnotationMirror a2 : annos2) {
                AnnotationMirror glb = greatestLowerBound(a1, a2);
                if (glb!=null) {
                    result.add(glb);
                }
            }
        }

        assert result.size() == annos1.size() : "QualifierHierarchy.greatestLowerBounds: resulting set has incorrect number of annotations!\n" +
                "    Set 1: " + annos1 + " Set 2: " + annos2 + " LUB: " + result;

        return result;
    }

}
