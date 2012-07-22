package checkers.linear;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import com.sun.source.tree.*;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.DefaultFlow;
import checkers.flow.DefaultFlowState;
import checkers.flow.Flow;
import checkers.linear.quals.*;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.util.AnnotationUtils;
import checkers.util.TreeUtils;

/**
 * Adds {@link Unusable} qualifier to a type if it represents:
 *
 * <ol>
 * <li value="1">Class declaration tree/element.  Such a construct usually
 * requires the top qualifier.</li>
 *
 * <li value="2">{@code Linear} reference once it is "used up"</li>
 * </ol>
 *
 */
public class LinearAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<LinearChecker> {

    public LinearAnnotatedTypeFactory(LinearChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
        this.postInit();
    }

    /**
     * Case 1: type of class declaration
     */
    @Override
    public void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        if (!type.isAnnotated() && elt.getKind().isClass()) {
            type.addAnnotation(Unusable.class);
        }
        super.annotateImplicit(elt, type);
    }

    @Override
    public Flow createFlow(LinearChecker checker, CompilationUnitTree tree,
            Set<AnnotationMirror> flowQuals) {
        return new LinearFlow(checker, tree, flowQuals, this);
    }

    /**
     * Performs flow-sensitive analysis to mark reference types {@code Linear}
     * as {@code Unusable} once they are used up.
     *
     * A {code Linear} type is "used up" once the reference is mentioned, as
     * an {@link IdentifierTree}.
     *
     */
    private static class LinearFlow extends DefaultFlow<DefaultFlowState> {
        private final AnnotationMirror LINEAR, UNUSABLE;

        public LinearFlow(BaseTypeChecker checker, CompilationUnitTree root,
                Set<AnnotationMirror> annotations, AnnotatedTypeFactory factory) {
            super(checker, root, annotations, factory);

            AnnotationUtils annoFactory = AnnotationUtils.getInstance(checker.getProcessingEnvironment());
            LINEAR = annoFactory.fromClass(Linear.class);
            UNUSABLE = annoFactory.fromClass(Unusable.class);
        }

        /**
         * Case 2: add {@code Unusable} to node type, if it is {@code Linear}.
         */
        @Override
        public Void visitIdentifier(IdentifierTree node, Void p) {
            super.visitIdentifier(node, p);
            markAsUnusableIfLinear(node);
            return null;
        }

        /**
         * If the node is of type {@code Linear}, then transit its type
         * into an {@code Unusable} type.
         *
         * The method should be called on every instance of a tree
         * that causes the reference to be "used up".
         */
        private void markAsUnusableIfLinear(ExpressionTree node) {
            if (!LinearVisitor.isLocalVarOrParam(node))
                return;

            Element elem = TreeUtils.elementFromUse(node);
            assert elem != null;
            if (this.flowState.vars.contains(elem)) {
                int idx = this.flowState.vars.indexOf(elem);
                if (this.flowState.annos.get(LINEAR, idx)) {
                    this.flowState.annos.set(UNUSABLE, idx);
                    this.flowState.annos.clear(LINEAR, idx);
                }
            }
        }

    }
}
