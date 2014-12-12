package org.checkerframework.framework.type;

import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.type.visitor.AtmComboVisitor;
import org.checkerframework.framework.type.visitor.VisitHistory;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TypesUtils;

import javax.lang.model.element.AnnotationMirror;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.checkerframework.framework.type.AnnotatedTypeMirror.*;
/**
 * A visitor used to compare two type mirrors for "structural" equality.  Structural equality
 * implies that, for two objects, all fields are also structurally equal and for primitives
 * their values are equal.  One reason this class is necessary is that at the moment
 * we compare wildcards and type variables for "equality".  This occurs because we do not
 * employ capture conversion.
 *
 * See also DefaultTypeHierarchy, and VisitHistory
 */
public class StructuralEqualityComparer extends AbstractAtmComboVisitor<Boolean, VisitHistory> {

    //TODO: REMOVE THIS OVERRIDE WHEN inferTypeArgs NO LONGER GENERATES INCOMPARABLE TYPES
    //TODO: THE PROBLEM IS THIS CLASS SHOULD FAIL WHEN INCOMPARABLE TYPES ARE COMPARED BUT
    //TODO: TO CURRENTLY SUPPORT THE BUGGY inferTypeArgs WE FALL BACK TO the RawnessComparer
    //TODO: WHICH IS CLOSE TO THE OLD TypeHierarchy behavior
    private AbstractAtmComboVisitor<Boolean, VisitHistory> fallback;


    //explain this one
    private AnnotationMirror currentTop = null;

    public StructuralEqualityComparer() {
        this(null);
    }

    public StructuralEqualityComparer(final AbstractAtmComboVisitor<Boolean, VisitHistory> fallback) {
        this.fallback = fallback;
    }

    @Override
    protected Boolean defaultAction(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, VisitHistory visitHistory) {
        //TODO: REMOVE THIS OVERRIDE WHEN inferTypeArgs NO LONGER GENERATES INCOMPARABLE TYPES
        //TODO: THe rawness comparer is close to the old implementation of TypeHierarchy
        if(fallback != null) {
            return fallback.visit(type1, type2, visitHistory);
        }

        return super.defaultAction(type1, type2, visitHistory);
    }

    /**
     * Returns true if type1 and type2 are structurally equivalent. With one exception,
     * type1.getClass().equals(type2.getClass()) must be true.  However, because the Checker Framework
     * sometimes "infers" Typevars to be Wildcards, we allow the combination Wildcard,Typevar.  In this
     * case, the two types are "equal" if their bounds are.
     * @param type1
     * @param type2
     * @return true if type1 and type2 are equal
     */
    public boolean areEqual(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2 ) {
        return AtmCombo.accept(type1, type2, new VisitHistory(this), this);
    }

    /**
     * The same as areEqual(type1, type2) except now a visited is passed along in order to avoid
     * infinite recursion on recursive bounds.  This method is only used internally to EqualityComparer.
     * @return
     */
    public boolean areEqual(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2,
                            final VisitHistory visited ) {
        if (type1 == null) {
            return type2 == null;
        }

        if (type2 == null) {
            return false;
        }

        return AtmCombo.accept(type1, type2, visited, this);
    }

    public boolean areEqualInHierarchy(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2,
                                       final AnnotationMirror top) {
        boolean areEqual;
        AnnotationMirror prevTop = currentTop;
        currentTop = top;
        try {
            areEqual = areEqual(type1, type2);
        } finally {
            currentTop = prevTop;
        }

        return areEqual;
    }

    /**
     * Return true if type1 and type2 have the same set of annotations.
     */
    protected boolean arePrimeAnnosEqual(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        if (currentTop != null) {
            return AnnotationUtils.areSame(
                    type1.getAnnotationInHierarchy(currentTop),
                    type2.getAnnotationInHierarchy(currentTop));
        } //else

        return AnnotationUtils.areSame(type1.getAnnotations(), type2.getAnnotations());
    }


    /**
     * Compare each type in types1 and types2 pairwise and return true if they are all equal.  This method
     * throws an exceptions if types1.size() != types2.size()
     * @param types1
     * @param types2
     * @param visited A store of what types have already been visited
     * @return true if for each pair (t1 = types1.get(i); t2 = types2.get(i)), areEqual(t1,t2)
     */
    protected boolean areAllEqual(final Collection<? extends AnnotatedTypeMirror> types1,
                                  final Collection<? extends AnnotatedTypeMirror> types2,
                                  final VisitHistory visited ) {
        if( types1.size() != types2.size()) {
            ErrorReporter.errorAbort(
                "Mismatching collection sizes:\n"
              + PluginUtil.join(",", types1) + "\n"
              + PluginUtil.join(",", types2)
            );
        }

        final Iterator<? extends AnnotatedTypeMirror> types1Iter = types1.iterator();
        final Iterator<? extends AnnotatedTypeMirror> types2Iter = types2.iterator();
        while(types1Iter.hasNext()) {
            final AnnotatedTypeMirror type1 = types1Iter.next();
            final AnnotatedTypeMirror type2 = types2Iter.next();
            if( !checkOrAreEqual(type1, type2, visited) ) {
                return false;
            }
        }

        return true;
    }

    /**
     * First check visited to see if type1 and type2 have been compared once already.
     * If so return true; otherwise compare them and add them to visited
     * @param type1
     * @param type2
     * @param visited
     * @return
     */
    protected boolean checkOrAreEqual(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2,
                                      final VisitHistory visited ) {
        if(visited.contains(type1, type2)) {
            return true;
        }

        final Boolean result = areEqual(type1, type2, visited);
        visited.add(type1, type2);

        return result;
    }

    /**
     * Called for every combination in which !type1.getClass().equals(type2.getClass()) except for
     * Wildcard_Typevar.
     * @return error message explaining the two types' classes are not the same
     */
    @Override
    protected String defaultErrorMessage(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, VisitHistory visited) {
        return "AnnotatedTypeMirror classes aren't equal.\n"
             + "type1 = " + type1.getClass().getSimpleName() + "( " + type1 + " )\n"
             + "type2 = " + type2.getClass().getSimpleName() + "( " + type2 + " )\n"
             + "visitHistory = " + visited;
    }

    /**
     * Two arrays are equal if:
     *   1) Their sets of primary annotations are equal
     *   2) Their component types are equal
     */
    @Override
    public Boolean visitArray_Array(final AnnotatedArrayType type1, final AnnotatedArrayType type2,
                                     final VisitHistory visited) {
        if(!arePrimeAnnosEqual(type1, type2)) {
            return false;
        }

        return areEqual(type1.getComponentType(), type2.getComponentType(), visited);
    }

    /**
     * Two declared types are equal if:
     *   1) The types are of the same class/interfaces
     *   2) Their sets of primary annotations are equal
     *   3) Their sets of type arguments are equal or one type is raw
     */
    @Override
    public Boolean visitDeclared_Declared(final AnnotatedDeclaredType type1, final AnnotatedDeclaredType type2,
                                          final VisitHistory visited) {
        if(visited.contains(type1,type2)) {
            return true;
        }

        if(!arePrimeAnnosEqual(type1, type2)) {
            return false;
        }
        visited.add(type1, type2);
        return visitTypeArgs(type1, type2, visited);
    }

    /**
     * A helper class for visitDeclared_Declared.  There are subtypes of DefaultTypeHierarchy that
     * need to customize the handling of type arguments. This method provides a convenient extension
     * point.
     */
    protected Boolean visitTypeArgs(final AnnotatedDeclaredType type1, final AnnotatedDeclaredType type2,
                                 final VisitHistory visited) {

        //TODO: ANYTHING WITH RAW TYPES? SHOULD WE HANDLE THEM LIKE DefaultTypeHierarchy, i.e. use ignoreRawTypes
        final List<? extends AnnotatedTypeMirror> type1Args = type1.getTypeArguments();
        final List<? extends AnnotatedTypeMirror> type2Args = type2.getTypeArguments();

        //TODO: IN THE ORIGINAL TYPE_HIERARCHY WE ALWAYS RETURN TRUE IF ONE OF THE LISTS IS EMPTY
        //TODO: WE SHOULD NEVER GET HERE UNLESS type's declared class and type2's declared class are equal
        //TODO: but potentially this would return true if say we compared (Object, List<String>)
        if( type1Args.isEmpty() || type2Args.isEmpty() ) {
            return true;
        }

        if (type1Args.size() > 0) {
            if(!areAllEqual(type1Args, type2Args, visited)) {
                return false;
            }
        }

        return true;
    }

    /**
     * //TODO: SHOULD PRIMARY ANNOTATIONS OVERRIDE INDIVIDUAL BOUND ANNOTATIONS?
     * //TODO: IF SO THEN WE SHOULD REMOVE THE arePrimeAnnosEqual AND FIX AnnotatedIntersectionType
     * Two intersection types are equal if:
     *   1) Their sets of primary annotations are equal
     *   2) Their sets of bounds (the types being intersected) are equal
     */
    @Override
    public Boolean visitIntersection_Intersection(final AnnotatedIntersectionType type1, final AnnotatedIntersectionType type2,
                                                  final VisitHistory visited) {
        if(!arePrimeAnnosEqual(type1, type2)) {
            return false;
        }

        visited.add(type1, type2);
        return areAllEqual(type1.directSuperTypes(), type2.directSuperTypes(), visited);
    }

    /**
     * Two null types are equal if:
     *   1) Their sets of primary annotations are equal
     */
    @Override
    public Boolean visitNull_Null(final AnnotatedNullType type1, final AnnotatedNullType type2,
                                  final VisitHistory visited) {
        return arePrimeAnnosEqual(type1, type2);
    }

    /**
     * Two primitive types are equal if:
     *   1) Their sets of primary annotations are equal
     */
    @Override
    public Boolean visitPrimitive_Primitive(final AnnotatedPrimitiveType type1, final AnnotatedPrimitiveType type2,
                                            final VisitHistory visited) {
        return arePrimeAnnosEqual(type1, type2);
    }

    /**
     * Two type variables are equal if:
     *   1) Their bounds are equal
     *
     * Note:  Primary annotations will be taken into account when the bounds are retrieved
     */
    @Override
    public Boolean visitTypevar_Typevar(final AnnotatedTypeVariable type1, final AnnotatedTypeVariable type2,
                                        final VisitHistory visited) {
        if(visited.contains(type1, type2)) {
            return true;
        }

        visited.add(type1, type2);
        return areEqual(type1.getUpperBound(), type2.getUpperBound(), visited)
            && areEqual(type1.getLowerBound(), type2.getLowerBound(), visited);
    }

    /**
     * TODO: IDENTIFY TESTS THAT LEAD TO RECURSIVE BOUNDED WILDCARDS, PERHAPS THE RIGHT THING IS TO
     * TODO: MOVE THE CODE THAT IDENTIFIES REFERENCES TO THE SAME WILDCARD TYPE HERE.
     * TODO  WOULD WE EVER WANT TO HAVE A REFERENCE TO THE SAME WILDCARD WITH DIFFERENT ANNOTATIONS?
     * Two wildcards are equal if:
     *   1) Their bounds are equal
     *
     * Note:  Primary annotations will be taken into account when the bounds are retrieved
     */
    @Override
    public Boolean visitWildcard_Wildcard(final AnnotatedWildcardType type1, final AnnotatedWildcardType type2,
                                          final VisitHistory visited) {
        if(visited.contains(type1, type2)) {
            return true;
        }

        visited.add(type1, type2);
        return areEqual(type1.getExtendsBound(), type2.getExtendsBound(), visited)
            && areEqual(type1.getSuperBound(),   type2.getSuperBound(),   visited);
    }

    //since we don't do a boxing conversion between primitive and declared types in some cases
    //we must compare primitives with their boxed counterparts
    @Override
    public Boolean visitDeclared_Primitive(AnnotatedDeclaredType type1, AnnotatedPrimitiveType type2, VisitHistory visitHistory) {
        if(!TypesUtils.isBoxOf(type1.getUnderlyingType(), type2.getUnderlyingType())) {
            defaultErrorMessage(type1, type2, visitHistory);
        }

        return arePrimeAnnosEqual(type1, type2);
    }

    @Override
    public Boolean visitPrimitive_Declared(AnnotatedPrimitiveType type1, AnnotatedDeclaredType type2,
                                           VisitHistory visitHistory) {
        if(!TypesUtils.isBoxOf(type2.getUnderlyingType(), type1.getUnderlyingType())) {
            defaultErrorMessage(type1, type2, visitHistory);
        }

        return arePrimeAnnosEqual(type1, type2);
    }

    //The following methods are because we use WILDCARDS instead of TYPEVARS for capture converted wildcards
    //TODO: REMOVE THE METHOD BELOW WHEN CAPTURE CONVERSION IS IMPLEMENTED
    /**
     * Since the Checker Framework doesn't engage in capture conversion, and since sometimes type variables
     * are "inferred" to be wildcards, this method allows the comparison of a wildcard to a type variable even
     * though they should never truly be equal.
     *
     * A wildcard is equal tyo a type variable if:
     *   1) The wildcard's bounds are equal to the type variable's bounds
     */
    @Override
    public Boolean visitWildcard_Typevar(final AnnotatedWildcardType type1, final AnnotatedTypeVariable type2,
                                         final VisitHistory visited) {
        if (visited.contains(type1, type2) ) {
            return true;
        }

        visited.add(type1, type2);
        return areEqual(type1.getExtendsBound(), type2.getUpperBound(), visited)
            && areEqual(type1.getSuperBound(),   type2.getLowerBound(), visited);
    }
}
