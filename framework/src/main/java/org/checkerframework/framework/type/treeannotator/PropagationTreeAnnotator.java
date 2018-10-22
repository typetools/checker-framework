package org.checkerframework.framework.type.treeannotator;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import java.util.Collection;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.Pair;

/**
 * {@link PropagationTreeAnnotator} adds qualifiers to types where the resulting type is a function
 * of an input type, e.g. the result of a binary operation is a LUB of the type of expressions in
 * the binary operation.
 *
 * <p>{@link PropagationTreeAnnotator} is generally ran first by {@link ListTreeAnnotator} since the
 * trees it handles are not usually targets of @implicit for.
 *
 * <p>{@link PropagationTreeAnnotator} does not traverse trees deeply by default.
 *
 * @see TreeAnnotator
 */
public class PropagationTreeAnnotator extends TreeAnnotator {

    private final QualifierHierarchy qualHierarchy;

    /**
     * Creates a {@link org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator}
     * from the given checker, using that checker's type hierarchy.
     */
    public PropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
        this.qualHierarchy = atypeFactory.getQualifierHierarchy();
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
        assert type.getKind() == TypeKind.ARRAY
                : "PropagationTreeAnnotator.visitNewArray: should be an array type";

        AnnotatedTypeMirror componentType = ((AnnotatedArrayType) type).getComponentType();

        Collection<? extends AnnotationMirror> prev = null;
        if (tree.getInitializers() != null && !tree.getInitializers().isEmpty()) {
            // We have initializers, either with or without an array type.

            for (ExpressionTree init : tree.getInitializers()) {
                AnnotatedTypeMirror initType = atypeFactory.getAnnotatedType(init);
                // initType might be a typeVariable, so use effectiveAnnotations.
                Collection<AnnotationMirror> annos = initType.getEffectiveAnnotations();

                prev = (prev == null) ? annos : qualHierarchy.leastUpperBounds(prev, annos);
            }
        } else {
            prev = componentType.getAnnotations();
        }

        assert prev != null
                : "PropagationTreeAnnotator.visitNewArray: violated assumption about qualifiers";

        Pair<Tree, AnnotatedTypeMirror> context =
                atypeFactory.getVisitorState().getAssignmentContext();
        Collection<? extends AnnotationMirror> post;

        if (context != null
                && context.second != null
                && context.second instanceof AnnotatedArrayType) {
            AnnotatedTypeMirror contextComponentType =
                    ((AnnotatedArrayType) context.second).getComponentType();
            // Only compare the qualifiers that existed in the array type
            // Defaulting wasn't performed yet, so prev might have fewer qualifiers than
            // contextComponentType, which would cause a failure.
            // TODO: better solution?
            boolean prevIsSubtype = true;
            for (AnnotationMirror am : prev) {
                if (contextComponentType.isAnnotatedInHierarchy(am)
                        && !this.qualHierarchy.isSubtype(
                                am, contextComponentType.getAnnotationInHierarchy(am))) {
                    prevIsSubtype = false;
                }
            }
            // TODO: checking conformance of component kinds is a basic sanity check
            // It fails for array initializer expressions. Those should be handled nicer.
            if (contextComponentType.getKind() == componentType.getKind()
                    && (prev.isEmpty()
                            || (!contextComponentType.getAnnotations().isEmpty()
                                    && prevIsSubtype))) {
                post = contextComponentType.getAnnotations();
            } else {
                // The type of the array initializers is incompatible with the
                // context type!
                // Somebody else will complain.
                post = prev;
            }
        } else {
            // No context is available - simply use what we have.
            post = prev;
        }
        componentType.addMissingAnnotations(post);

        return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
        if (hasPrimaryAnnotationInAllHierarchies(type)) {
            // If the type already has a primary annotation in all hierarchies, then the
            // propagated annotations won't be applied.  So don't compute them.
            return null;
        }
        AnnotatedTypeMirror rhs = atypeFactory.getAnnotatedType(node.getExpression());
        AnnotatedTypeMirror lhs = atypeFactory.getAnnotatedType(node.getVariable());
        Set<? extends AnnotationMirror> lubs =
                qualHierarchy.leastUpperBounds(
                        rhs.getEffectiveAnnotations(), lhs.getEffectiveAnnotations());
        type.addMissingAnnotations(lubs);
        return null;
    }

    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
        if (hasPrimaryAnnotationInAllHierarchies(type)) {
            // If the type already has a primary annotation in all hierarchies, then the
            // propagated annotations won't be applied.  So don't compute them.
            // Also, calling getAnnotatedType on the left and right operands is potentially
            // expensive.
            return null;
        }

        AnnotatedTypeMirror a = atypeFactory.getAnnotatedType(node.getLeftOperand());
        AnnotatedTypeMirror b = atypeFactory.getAnnotatedType(node.getRightOperand());
        Set<? extends AnnotationMirror> lubs =
                qualHierarchy.leastUpperBounds(
                        a.getEffectiveAnnotations(), b.getEffectiveAnnotations());
        type.addMissingAnnotations(lubs);
        return null;
    }

    @Override
    public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
        if (hasPrimaryAnnotationInAllHierarchies(type)) {
            // If the type already has a primary annotation in all hierarchies, then the
            // propagated annotations won't be applied.  So don't compute them.
            return null;
        }

        AnnotatedTypeMirror exp = atypeFactory.getAnnotatedType(node.getExpression());
        type.addMissingAnnotations(exp.getAnnotations());
        return null;
    }

    /*
     * TODO: would this make sense in general?
    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, AnnotatedTypeMirror type) {
        if (!type.isAnnotated()) {
            AnnotatedTypeMirror a = typeFactory.getAnnotatedType(node.getTrueExpression());
            AnnotatedTypeMirror b = typeFactory.getAnnotatedType(node.getFalseExpression());
            Set<AnnotationMirror> lubs = qualHierarchy.leastUpperBounds(a.getEffectiveAnnotations(), b.getEffectiveAnnotations());
            type.replaceAnnotations(lubs);
        }
        return super.visitConditionalExpression(node, type);
    }*/

    @Override
    public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
        if (hasPrimaryAnnotationInAllHierarchies(type)) {
            // If the type is already has a primary annotation in all hierarchies, then the
            // propagated annotations won't be applied.  So don't compute them.
            return null;
        }

        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node.getExpression());
        if (type.getKind() == TypeKind.TYPEVAR) {
            if (exprType.getKind() == TypeKind.TYPEVAR) {
                // If both types are type variables, take the direct annotations.
                type.addMissingAnnotations(exprType.getAnnotations());
            }
            // else do nothing.
        } else {
            // Use effective annotations from the expression, to get upper bound
            // of type variables.
            type.addMissingAnnotations(exprType.getEffectiveAnnotations());
        }
        return null;
    }

    private boolean hasPrimaryAnnotationInAllHierarchies(AnnotatedTypeMirror type) {
        boolean annotated = true;
        for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
            if (type.getEffectiveAnnotationInHierarchy(top) == null) {
                annotated = false;
            }
        }
        return annotated;
    }
}
