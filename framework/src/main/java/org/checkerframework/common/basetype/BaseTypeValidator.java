package org.checkerframework.common.basetype;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.poly.QualifierPolymorphism;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.*;

/** A visitor to validate the types in a tree. */
public class BaseTypeValidator extends AnnotatedTypeScanner<Void, Tree> implements TypeValidator {
    protected boolean isValid = true;

    protected final BaseTypeChecker checker;
    protected final BaseTypeVisitor<?> visitor;
    protected final AnnotatedTypeFactory atypeFactory;

    // TODO: clean up coupling between components
    public BaseTypeValidator(
            BaseTypeChecker checker,
            BaseTypeVisitor<?> visitor,
            AnnotatedTypeFactory atypeFactory) {
        this.checker = checker;
        this.visitor = visitor;
        this.atypeFactory = atypeFactory;
    }

    /**
     * The entry point to the type validator. Validate the type against the given tree. Neither this
     * method nor visit should be called directly by a visitor, only use {@link
     * BaseTypeVisitor#validateTypeOf(Tree)}.
     *
     * @param type the type to validate
     * @param tree the tree from which the type originated. If the tree is a method tree, validate
     *     its return type. If the tree is a variable tree, validate its field type.
     * @return true, iff the type is valid
     */
    @Override
    public boolean isValid(AnnotatedTypeMirror type, Tree tree) {
        Result result = isValidType(atypeFactory.getQualifierHierarchy(), type);
        if (result.isFailure()) {
            checker.report(result, tree);
            return false;
        }
        this.isValid = true;
        visit(type, tree);
        return this.isValid;
    }

    /**
     * Returns true if the given {@link AnnotatedTypeMirror} passed a set of well-formedness checks.
     * The method will never return false for valid types, but might not catch all invalid types.
     *
     * <p>Currently, the following is checked:
     *
     * <ol>
     *   <li>There should not be multiple annotations from the same hierarchy.
     *   <li>There should not be more annotations than the width of the qualifier hierarchy.
     *   <li>If the type is not a type variable, then the number of annotations should be the same
     *       as the width of the qualifier hierarchy.
     *   <li>These properties should also hold recursively for component types of arrays, as wells
     *       as bounds of type variables and wildcards.
     * </ol>
     */
    protected Result isValidType(QualifierHierarchy qualifierHierarchy, AnnotatedTypeMirror type) {
        SimpleAnnotatedTypeScanner<Result, Void> scanner =
                new SimpleAnnotatedTypeScanner<Result, Void>() {
                    @Override
                    protected Result defaultAction(AnnotatedTypeMirror type, Void aVoid) {
                        return isTopLevelValidType(qualifierHierarchy, type);
                    }

                    @Override
                    protected Result reduce(Result r1, Result r2) {
                        if (r1 == null) {
                            if (r2 == null) {
                                return Result.SUCCESS;
                            }
                            return r2;
                        } else if (r2 == null) {
                            return r1;
                        }
                        return r1.merge(r2);
                    }
                };
        return scanner.visit(type);
    }

    /** Checks every property listed in {@link #isValidType}, but only for the top level type. */
    protected Result isTopLevelValidType(
            QualifierHierarchy qualifierHierarchy, AnnotatedTypeMirror type) {
        // multiple annotations from the same hierarchy
        Set<AnnotationMirror> annotations = type.getAnnotations();
        Set<AnnotationMirror> seenTops = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : annotations) {
            if (QualifierPolymorphism.isPolyAll(anno)) {
                // ignore PolyAll when counting annotations
                continue;
            }
            AnnotationMirror top = qualifierHierarchy.getTopAnnotation(anno);
            if (AnnotationUtils.containsSame(seenTops, top)) {
                return Result.failure("type.invalid.conflicting.annos", annotations, type);
            }
            seenTops.add(top);
        }

        // treat types that have polyall like type variables
        boolean hasPolyAll = type.hasAnnotation(PolyAll.class);
        boolean canHaveEmptyAnnotationSet =
                QualifierHierarchy.canHaveEmptyAnnotationSet(type) || hasPolyAll;

        // wrong number of annotations
        if (!canHaveEmptyAnnotationSet && seenTops.size() < qualifierHierarchy.getWidth()) {
            return Result.failure("type.invalid.too.few.annotations", annotations, type);
        }
        return Result.SUCCESS;
    }

    protected void reportValidityResult(
            final @CompilerMessageKey String errorType,
            final AnnotatedTypeMirror type,
            final Tree p) {
        checker.report(Result.failure(errorType, type.getAnnotations(), type.toString()), p);
        isValid = false;
    }

    /**
     * Like {@link #reportValidityResult}, but the type is printed in the error message without
     * annotations. This method would print "annotation @NonNull is not permitted on type int",
     * whereas {@link #reportValidityResult} would print "annotation @NonNull is not permitted on
     * type @NonNull int".
     */
    protected void reportValidityResultOnUnannotatedType(
            final @CompilerMessageKey String errorType,
            final AnnotatedTypeMirror type,
            final Tree p) {
        // TODO: if underlying is a compound type such as List<@Palindrome String>, then it would be
        // nice to print it as "List" instead of as "List<@Palindrome String>".
        TypeMirror underlying = type.getUnderlyingType();
        checker.report(Result.failure(errorType, type.getAnnotations(), underlying.toString()), p);
        isValid = false;
    }

    /**
     * Most errors reported by this class are of the form type.invalid. This method reports when the
     * bounds of a wildcard or type variable don't make sense. Bounds make sense when the effective
     * annotations on the upper bound are supertypes of those on the lower bounds for all
     * hierarchies. To ensure that this subtlety is not lost on users, we report
     * "bound.type.incompatible" and print the bounds along with the invalid type rather than a
     * "type.invalid".
     */
    protected void reportInvalidBounds(final AnnotatedTypeMirror type, final Tree tree) {
        final String label;
        final AnnotatedTypeMirror upperBound;
        final AnnotatedTypeMirror lowerBound;

        switch (type.getKind()) {
            case TYPEVAR:
                label = "type parameter";
                upperBound = ((AnnotatedTypeVariable) type).getUpperBound();
                lowerBound = ((AnnotatedTypeVariable) type).getLowerBound();
                break;

            case WILDCARD:
                label = "wildcard";
                upperBound = ((AnnotatedWildcardType) type).getExtendsBound();
                lowerBound = ((AnnotatedWildcardType) type).getSuperBound();
                break;

            default:
                throw new BugInCF(
                        "Type is not bounded.\n" + "type=" + type + "\n" + "tree=" + tree);
        }

        checker.report(
                Result.failure(
                        "bound.type.incompatible",
                        label,
                        type.toString(),
                        upperBound.toString(true),
                        lowerBound.toString(true)),
                tree);
        isValid = false;
    }

    protected void reportInvalidType(final AnnotatedTypeMirror type, final Tree p) {
        reportValidityResult("type.invalid", type, p);
    }

    protected void reportInvalidAnnotationsOnUse(final AnnotatedTypeMirror type, final Tree p) {
        reportValidityResultOnUnannotatedType("type.invalid.annotations.on.use", type, p);
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }

        final boolean skipChecks = checker.shouldSkipUses(type.getUnderlyingType().asElement());

        if (!skipChecks) {
            // Ensure that type use is a subtype of the element type
            // isValidUse determines the erasure of the types.
            AnnotatedDeclaredType elemType =
                    (AnnotatedDeclaredType)
                            atypeFactory.getAnnotatedType(type.getUnderlyingType().asElement());

            if (!visitor.isValidUse(elemType, type, tree)) {
                reportInvalidAnnotationsOnUse(type, tree);
            }
        }

        /*
         * Try to reconstruct the ParameterizedTypeTree from the given tree.
         * TODO: there has to be a nicer way to do this...
         */
        Pair<ParameterizedTypeTree, AnnotatedDeclaredType> p =
                extractParameterizedTypeTree(tree, type);
        ParameterizedTypeTree typeArgTree = p.first;
        type = p.second;

        if (typeArgTree == null) {
            return super.visitDeclared(type, tree);
        } // else

        // We put this here because we don't want to put it in visitedNodes before calling
        // super (in the else branch) because that would cause the super implementation
        // to detect that we've already visited type and to immediately return
        visitedNodes.put(type, null);

        // We have a ParameterizedTypeTree -> visit it.

        visitParameterizedType(type, typeArgTree);

        /*
         * Instead of calling super with the unchanged "tree", adapt the
         * second argument to be the corresponding type argument tree. This
         * ensures that the first and second parameter to this method always
         * correspond. visitDeclared is the only method that had this
         * problem.
         */
        List<? extends AnnotatedTypeMirror> tatypes = type.getTypeArguments();

        if (tatypes == null) {
            return null;
        }

        // May be zero for a "diamond" (inferred type args in constructor
        // invocation).
        int numTypeArgs = typeArgTree.getTypeArguments().size();
        if (numTypeArgs != 0) {
            // TODO: this should be an equality, but in
            // http://buffalo.cs.washington.edu:8080/job/jdk6-daikon-typecheck/2061/console
            // it failed with:
            // daikon/Debug.java; message: size mismatch for type arguments:
            // @NonNull Object and Class<?>
            // but I didn't manage to reduce it to a test case.
            assert tatypes.size() <= numTypeArgs || skipChecks
                    : "size mismatch for type arguments: " + type + " and " + typeArgTree;

            for (int i = 0; i < tatypes.size(); ++i) {
                scan(tatypes.get(i), typeArgTree.getTypeArguments().get(i));
            }
        }

        // Don't call the super version, because it creates a mismatch
        // between the first and second parameters.
        // return super.visitDeclared(type, tree);

        return null;
    }

    private Pair<ParameterizedTypeTree, AnnotatedDeclaredType> extractParameterizedTypeTree(
            Tree tree, AnnotatedDeclaredType type) {
        ParameterizedTypeTree typeargtree = null;

        switch (tree.getKind()) {
            case VARIABLE:
                Tree lt = ((VariableTree) tree).getType();
                if (lt instanceof ParameterizedTypeTree) {
                    typeargtree = (ParameterizedTypeTree) lt;
                } else {
                    // System.out.println("Found a: " + lt);
                }
                break;
            case PARAMETERIZED_TYPE:
                typeargtree = (ParameterizedTypeTree) tree;
                break;
            case NEW_CLASS:
                NewClassTree nct = (NewClassTree) tree;
                ExpressionTree nctid = nct.getIdentifier();
                if (nctid.getKind() == Tree.Kind.PARAMETERIZED_TYPE) {
                    typeargtree = (ParameterizedTypeTree) nctid;
                    /*
                     * This is quite tricky... for anonymous class instantiations,
                     * the type at this point has no type arguments. By doing the
                     * following, we get the type arguments again.
                     */
                    type = (AnnotatedDeclaredType) atypeFactory.getAnnotatedType(typeargtree);
                }
                break;
            case ANNOTATED_TYPE:
                AnnotatedTypeTree tr = (AnnotatedTypeTree) tree;
                ExpressionTree undtr = tr.getUnderlyingType();
                if (undtr instanceof ParameterizedTypeTree) {
                    typeargtree = (ParameterizedTypeTree) undtr;
                } else if (undtr instanceof IdentifierTree) {
                    // @Something D -> Nothing to do
                } else {
                    // TODO: add more test cases to ensure that nested types are
                    // handled correctly,
                    // e.g. @Nullable() List<@Nullable Object>[][]
                    Pair<ParameterizedTypeTree, AnnotatedDeclaredType> p =
                            extractParameterizedTypeTree(undtr, type);
                    typeargtree = p.first;
                    type = p.second;
                }
                break;
            case IDENTIFIER:
            case ARRAY_TYPE:
            case NEW_ARRAY:
            case MEMBER_SELECT:
            case UNBOUNDED_WILDCARD:
            case EXTENDS_WILDCARD:
            case SUPER_WILDCARD:
            case TYPE_PARAMETER:
                // Nothing to do.
                // System.out.println("Found a: " + (tree instanceof
                // ParameterizedTypeTree));
                break;
            default:
                // the parameterized type is the result of some expression tree.
                // No need to do anything further.
                break;
                // System.err.printf("TypeValidator.visitDeclared unhandled tree: %s of kind %s\n",
                //                 tree, tree.getKind());
        }

        return Pair.of(typeargtree, type);
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        if (checker.shouldSkipUses(type.getUnderlyingType().toString())) {
            return super.visitPrimitive(type, tree);
        }

        if (!visitor.isValidUse(type, tree)) {
            reportInvalidAnnotationsOnUse(type, tree);
        }

        return super.visitPrimitive(type, tree);
    }

    @Override
    public Void visitArray(AnnotatedArrayType type, Tree tree) {
        // TODO: is there already or add a helper method
        // to determine the non-array component type
        AnnotatedTypeMirror comp = type;
        do {
            comp = ((AnnotatedArrayType) comp).getComponentType();
        } while (comp.getKind() == TypeKind.ARRAY);

        if (comp.getKind() == TypeKind.DECLARED
                && checker.shouldSkipUses(
                        ((AnnotatedDeclaredType) comp).getUnderlyingType().asElement())) {
            return super.visitArray(type, tree);
        }

        if (!visitor.isValidUse(type, tree)) {
            reportInvalidAnnotationsOnUse(type, tree);
        }

        return super.visitArray(type, tree);
    }

    /**
     * Checks that the annotations on the type arguments supplied to a type or a method invocation
     * are within the bounds of the type variables as declared, and issues the
     * "type.argument.type.incompatible" error if they are not.
     *
     * <p>This method used to be visitParameterizedType, which incorrectly handles the main
     * annotation on generic types.
     */
    protected Void visitParameterizedType(AnnotatedDeclaredType type, ParameterizedTypeTree tree) {
        // System.out.printf("TypeValidator.visitParameterizedType: type: %s, tree: %s\n",
        // type, tree);

        if (TreeUtils.isDiamondTree(tree)) {
            return null;
        }

        final TypeElement element = (TypeElement) type.getUnderlyingType().asElement();
        if (checker.shouldSkipUses(element)) {
            return null;
        }

        List<AnnotatedTypeParameterBounds> bounds =
                atypeFactory.typeVariablesFromUse(type, element);

        visitor.checkTypeArguments(tree, bounds, type.getTypeArguments(), tree.getTypeArguments());

        return null;
    }

    @Override
    public Void visitTypeVariable(AnnotatedTypeVariable type, Tree tree) {
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }

        if (type.isDeclaration() && !areBoundsValid(type.getUpperBound(), type.getLowerBound())) {
            reportInvalidBounds(type, tree);
        }

        return super.visitTypeVariable(type, tree);
    }

    @Override
    public Void visitWildcard(AnnotatedWildcardType type, Tree tree) {
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }

        if (!areBoundsValid(type.getExtendsBound(), type.getSuperBound())) {
            reportInvalidBounds(type, tree);
        }

        return super.visitWildcard(type, tree);
    }

    /**
     * @return true if the effective annotations on the upperBound are above those on the lowerBound
     */
    public boolean areBoundsValid(
            final AnnotatedTypeMirror upperBound, final AnnotatedTypeMirror lowerBound) {
        final QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();
        final Set<AnnotationMirror> upperBoundAnnos =
                AnnotatedTypes.findEffectiveAnnotations(qualifierHierarchy, upperBound);
        final Set<AnnotationMirror> lowerBoundAnnos =
                AnnotatedTypes.findEffectiveAnnotations(qualifierHierarchy, lowerBound);

        if (upperBoundAnnos.size() == lowerBoundAnnos.size()) {
            return qualifierHierarchy.isSubtype(lowerBoundAnnos, upperBoundAnnos);
        } // else
        //  When upperBoundAnnos.size() != lowerBoundAnnos.size() one of the two bound types will
        //  be reported as invalid.  Therefore, we do not do any other comparisons nor do we report
        //  a bound.type.incompatible

        return true;
    }
}
