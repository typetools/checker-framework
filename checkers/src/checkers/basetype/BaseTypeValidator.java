package checkers.basetype;

/*>>>
import checkers.compilermsgs.quals.CompilerMessageKey;
*/

import checkers.source.Result;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.types.visitors.AnnotatedTypeScanner;

import javacutils.AnnotationUtils;
import javacutils.Pair;
import javacutils.TreeUtils;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * A visitor to validate the types in a tree.
 */
// TODO: add a TypeValidator interface
public class BaseTypeValidator extends AnnotatedTypeScanner<Void, Tree> {
    protected boolean isValid = true;

    protected final BaseTypeChecker<?> checker;
    protected final BaseTypeVisitor<?, ?> visitor;
    protected final AnnotatedTypeFactory atypeFactory;

    // TODO: clean up coupling between components
    public BaseTypeValidator(BaseTypeChecker<?> checker,
            BaseTypeVisitor<?, ?> visitor,
            AnnotatedTypeFactory atypeFactory) {
        this.checker = checker;
        this.visitor = visitor;
        this.atypeFactory = atypeFactory;
    }

    public boolean isValid(AnnotatedTypeMirror type, Tree tree) {
        this.isValid = true;
        visit(type, tree);
        return this.isValid;
    }

    protected void reportValidityResult(
            final /*@CompilerMessageKey*/ String errorType,
            final AnnotatedTypeMirror type, final Tree p) {
        checker.report(Result.failure(errorType, type.getAnnotations(),
                        type.toString()), p);
        isValid = false;
    }

    protected void reportError(final AnnotatedTypeMirror type, final Tree p) {
        reportValidityResult("type.invalid", type, p);
    }

    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
        if (checker.shouldSkipUses(type.getUnderlyingType().asElement()))
            return super.visitDeclared(type, tree);

        {
            // Ensure that type use is a subtype of the element type
            // isValidUse determines the erasure of the types.
            AnnotatedDeclaredType elemType = (AnnotatedDeclaredType) atypeFactory
                    .getAnnotatedType(type.getUnderlyingType().asElement());

            if (!visitor.isValidUse(elemType, type)) {
                reportError(type, tree);
            }
        }

        // System.out.println("Type: " + type);
        // System.out.println("Tree: " + tree);
        // System.out.println("Tree kind: " + tree.getKind());

        /*
         * Try to reconstruct the ParameterizedTypeTree from the given tree.
         * TODO: there has to be a nicer way to do this...
         */
        Pair<ParameterizedTypeTree, AnnotatedDeclaredType> p = extractParameterizedTypeTree(tree, type);
        ParameterizedTypeTree typeargtree = p.first;
        type = p.second;

        if (typeargtree != null) {
            // We have a ParameterizedTypeTree -> visit it.

            visitParameterizedType(type, typeargtree);

            /*
             * Instead of calling super with the unchanged "tree", adapt the
             * second argument to be the corresponding type argument tree. This
             * ensures that the first and second parameter to this method always
             * correspond. visitDeclared is the only method that had this
             * problem.
             */
            List<? extends AnnotatedTypeMirror> tatypes = type.getTypeArguments();

            if (tatypes == null)
                return null;

            // May be zero for a "diamond" (inferred type args in constructor
            // invocation).
            int numTypeArgs = typeargtree.getTypeArguments().size();
            if (numTypeArgs != 0) {
                // TODO: this should be an equality, but in
                // http://buffalo.cs.washington.edu:8080/job/jdk6-daikon-typecheck/2061/console
                // it failed with:
                // daikon/Debug.java; message: size mismatch for type arguments:
                // @NonNull Object and Class<?>
                // but I didn't manage to reduce it to a test case.
                assert tatypes.size() <= numTypeArgs : "size mismatch for type arguments: " +
                        type + " and " + typeargtree;

                for (int i = 0; i < tatypes.size(); ++i) {
                    scan(tatypes.get(i), typeargtree.getTypeArguments().get(i));
                }
            }

            return null;

            // Don't call the super version, because it creates a mismatch
            // between
            // the first and second parameters.
            // return super.visitDeclared(type, tree);
        }

        return super.visitDeclared(type, tree);
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
                Pair<ParameterizedTypeTree, AnnotatedDeclaredType> p = extractParameterizedTypeTree(
                        undtr, type);
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
            System.err.printf("TypeValidator.visitDeclared unhandled tree: %s of kind %s\n",
                            tree, tree.getKind());
        }

        return Pair.of(typeargtree, type);
    }

    @Override
    public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
        if (checker.shouldSkipUses(type.getUnderlyingType().toString()))
            return super.visitPrimitive(type, tree);

        if (!visitor.isValidUse(type)) {
            reportError(type, tree);
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

        if (comp != null &&
                comp.getKind() == TypeKind.DECLARED &&
                checker.shouldSkipUses(((AnnotatedDeclaredType) comp)
                        .getUnderlyingType().asElement())) {
            return super.visitArray(type, tree);
        }

        if (!visitor.isValidUse(type)) {
            reportError(type, tree);
        }

        return super.visitArray(type, tree);
    }

    /**
     * Checks that the annotations on the type arguments supplied to a type or a
     * method invocation are within the bounds of the type variables as
     * declared, and issues the "generic.argument.invalid" error if they are
     * not.
     *
     * This method used to be visitParameterizedType, which incorrectly handles
     * the main annotation on generic types.
     */
    protected Void visitParameterizedType(AnnotatedDeclaredType type,
            ParameterizedTypeTree tree) {
        // System.out.printf("TypeValidator.visitParameterizedType: type: %s, tree: %s\n",
        // type, tree);

        if (TreeUtils.isDiamondTree(tree))
            return null;

        final TypeElement element = (TypeElement) type.getUnderlyingType().asElement();
        if (checker.shouldSkipUses(element))
            return null;

        List<AnnotatedTypeVariable> typevars = atypeFactory.typeVariablesFromUse(type, element);

        visitor.checkTypeArguments(tree, typevars, type.getTypeArguments(),
                tree.getTypeArguments());

        return null;
    }

    @Override
    public Void visitTypeVariable(AnnotatedTypeVariable type, Tree tree) {
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }

        // Keep in sync with visitWildcard
        Set<AnnotationMirror> onVar = type.getAnnotations();
        if (!onVar.isEmpty()) {
            // System.out.printf("BaseTypeVisitor.TypeValidator.visitTypeVariable(type: %s, tree: %s)%n",
            // type, tree);

            // TODO: the following check should not be necessary, once we are
            // able to
            // recurse on type parameters in AnnotatedTypes.isValidType (see
            // todo there).
            {
                // Check whether multiple qualifiers from the same hierarchy
                // appear.
                Set<AnnotationMirror> seenTops = AnnotationUtils.createAnnotationSet();
                for (AnnotationMirror aOnVar : onVar) {
                    AnnotationMirror top = checker.getQualifierHierarchy().getTopAnnotation(aOnVar);
                    if (seenTops.contains(top)) {
                        this.reportError(type, tree);
                    }
                    seenTops.add(top);
                }
            }

            // TODO: because of the way AnnotatedTypeMirror fixes up the bounds,
            // i.e. an annotation on the type variable always replaces a
            // corresponding
            // annotation in the bound, some of these checks are not actually
            // meaningful.
            /*if (type.getUpperBoundField() != null) {
                AnnotatedTypeMirror upper = type.getUpperBoundField();

                for (AnnotationMirror aOnVar : onVar) {
                    if (upper.isAnnotatedInHierarchy(aOnVar) &&
                            !checker.getQualifierHierarchy().isSubtype(aOnVar,
                                    upper.getAnnotationInHierarchy(aOnVar))) {
                        this.reportError(type, tree);
                    }
                }
                upper.replaceAnnotations(onVar);
            }*/

            if (type.getLowerBoundField() != null) {
                AnnotatedTypeMirror lower = type.getLowerBoundField();
                for (AnnotationMirror aOnVar : onVar) {
                    if (lower.isAnnotatedInHierarchy(aOnVar) &&
                            !checker.getQualifierHierarchy().isSubtype(
                                    lower.getAnnotationInHierarchy(aOnVar),
                                    aOnVar)) {
                        this.reportError(type, tree);
                    }
                }
                lower.replaceAnnotations(onVar);
            }
        }

        return super.visitTypeVariable(type, tree);
    }

    @Override
    public Void visitWildcard(AnnotatedWildcardType type, Tree tree) {
        if (visitedNodes.containsKey(type)) {
            return visitedNodes.get(type);
        }

        // Keep in sync with visitTypeVariable
        Set<AnnotationMirror> onVar = type.getAnnotations();
        if (!onVar.isEmpty()) {
            // System.out.printf("BaseTypeVisitor.TypeValidator.visitWildcard(type: %s, tree: %s)",
            // type, tree);

            // TODO: the following check should not be necessary, once we are
            // able to
            // recurse on type parameters in AnnotatedTypes.isValidType (see
            // todo there).
            {
                // Check whether multiple qualifiers from the same hierarchy
                // appear.
                Set<AnnotationMirror> seenTops = AnnotationUtils.createAnnotationSet();
                for (AnnotationMirror aOnVar : onVar) {
                    AnnotationMirror top = checker.getQualifierHierarchy().getTopAnnotation(aOnVar);
                    if (seenTops.contains(top)) {
                        this.reportError(type, tree);
                    }
                    seenTops.add(top);
                }
            }

            if (type.getExtendsBoundField() != null) {
                AnnotatedTypeMirror upper = type.getExtendsBoundField();
                for (AnnotationMirror aOnVar : onVar) {
                    if (upper.isAnnotatedInHierarchy(aOnVar) &&
                            !checker.getQualifierHierarchy().isSubtype(aOnVar,
                                    upper.getAnnotationInHierarchy(aOnVar))) {
                        this.reportError(type, tree);
                    }
                }
                upper.replaceAnnotations(onVar);
            }

            if (type.getSuperBoundField() != null) {
                AnnotatedTypeMirror lower = type.getSuperBoundField();
                for (AnnotationMirror aOnVar : onVar) {
                    if (lower.isAnnotatedInHierarchy(aOnVar) &&
                            !checker.getQualifierHierarchy().isSubtype(
                                    lower.getAnnotationInHierarchy(aOnVar),
                                    aOnVar)) {
                        this.reportError(type, tree);
                    }
                }
                lower.replaceAnnotations(onVar);
            }

        }
        return super.visitWildcard(type, tree);
    }
}