package org.checkerframework.checker.nullness;

import org.checkerframework.checker.nullness.KeyForPropagator.PropagationDirection;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * For the following initializations we wish to propagate the annotations from the left-hand side
 * to the right-hand side or vice versa:
 *
 * 1. If a keySet is being saved to a newly declared set, we transfer the annotations from the
 * keySet to the lhs. e.g.,
 * <pre>{@code
 * //Previously, the user would be required to annotate the LHS's type argument with @KeyFor("m")
 * Set<String> keySet = m.keySet();
 * }</pre>
 *
 * 2. If a variable declaration contains type arguments with an @KeyFor annotation and it's initializer
 * is a new class tree with corresponding type arguments that have an @UknownKeyFor primary annotation
 * we transfer from the LHS to RHS.  e.g.,
 * <pre>{@code
 * //normally a user would have to write the @KeyFor("m") on both sides
 * List<@KeyFor("m") String> keys = new ArrayList<String>();
 * }</pre>
 *
 * 3. IMPORTANT NOTE:  The following case must be (and is) handled in KeyForAnnotatedTypeFactory.
 * In BaseTypeVisitor we check to make sure that the constructor called in a NewClassTree is actually
 * compatible with the annotations placed on the NewClassTree.  This requires that, prior to
 * this check we also propagate the annotations to this constructor in constructorFromUse so that
 * the constructor call matches the type given to the NewClassTree.
 * @see org.checkerframework.checker.nullness.KeyForAnnotatedTypeFactory#constructorFromUse(com.sun.source.tree.NewClassTree)
 *
 * Note propagation only occurs between two AnnotatedDeclaredTypes.  If either side is not an
 * AnnotatedDeclaredType then this class does nothing.
 *
 * IMPORTANT NOTE:
 */
public class KeyForPropagationTreeAnnotator extends TreeAnnotator {
    private final KeyForPropagator keyForPropagator;
    private final ExecutableElement keySetMethod;

    public KeyForPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory,
                                          KeyForPropagator propagationTreeAnnotator) {
        super(atypeFactory);
        this.keyForPropagator = propagationTreeAnnotator;
        keySetMethod = TreeUtils.getMethod("java.util.Map", "keySet", 0, atypeFactory.getProcessingEnv());
    }

    /**
     * @return true iff expression is a call to java.util.Map.KeySet
     */
    public boolean isCallToKeyset(ExpressionTree expression) {
        if (expression instanceof MethodInvocationTree) {
            return TreeUtils.isMethodInvocation(expression, keySetMethod, atypeFactory.getProcessingEnv());
        }
        return false;
    }


    /** Transfers annotations to the variableTree if the right side is a call to java.util.Map.KeySet. */
    @Override
    public Void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type) {
        super.visitVariable(variableTree, type);

        //This should only happen on map.keySet();
        if (type.getKind() == TypeKind.DECLARED) {
            final ExpressionTree initializer = variableTree.getInitializer();

            if (isCallToKeyset(initializer)) {
                final AnnotatedDeclaredType variableType = (AnnotatedDeclaredType) type;
                final AnnotatedTypeMirror initializerType = atypeFactory.getAnnotatedType(initializer);

                //array types and boxed primitives etc don't require propagation
                if (variableType.getKind() == TypeKind.DECLARED) {
                    keyForPropagator.propagate((AnnotatedDeclaredType) initializerType, variableType, PropagationDirection.TO_SUPERTYPE, atypeFactory);
                }

            }
        }

        return null;
    }

    /** Transfers annotations to type if the left hand side is a variable declaration. */
    @Override
    public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror type) {
        Pair<Tree, AnnotatedTypeMirror> context = atypeFactory.getVisitorState().getAssignmentContext();

        if (type.getKind() == TypeKind.DECLARED && context != null && context.first != null) {
            AnnotatedTypeMirror assignedTo = TypeArgInferenceUtil.assignedTo(atypeFactory, atypeFactory.getPath(node));

            if (assignedTo != null) {

                //array types and boxed primitives etc don't require propagation
                if (assignedTo.getKind() == TypeKind.DECLARED) {
                    final AnnotatedDeclaredType newClassType = (AnnotatedDeclaredType) type;
                    keyForPropagator.propagate(newClassType, (AnnotatedDeclaredType) assignedTo, PropagationDirection.TO_SUBTYPE, atypeFactory);
                }

            }
        }

        return super.visitNewClass(node, type);
    }
}
