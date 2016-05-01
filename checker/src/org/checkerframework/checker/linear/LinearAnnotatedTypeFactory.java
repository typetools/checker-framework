package org.checkerframework.checker.linear;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.checkerframework.checker.linear.qual.Linear;
import org.checkerframework.checker.linear.qual.Unusable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

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
public class LinearAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    private final AnnotationMirror LINEAR, UNUSABLE;

    public LinearAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        LINEAR = AnnotationUtils.fromClass(elements, Linear.class);
        UNUSABLE = AnnotationUtils.fromClass(elements, Unusable.class);

        this.postInit();
    }

    /**
     * Case 1: type of class declaration
     */
    @Override
    public void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        if (!type.isAnnotatedInHierarchy(LINEAR) && elt.getKind().isClass()) {
            type.addAnnotation(UNUSABLE);
        }
        super.annotateImplicit(elt, type);
    }

    // TODO: Re-enable flow with the new org.checkerframework.dataflow framework.

    /**
     * Performs flow-sensitive analysis to mark reference types {@code Linear}
     * as {@code Unusable} once they are used up.
     *
     * A {code Linear} type is "used up" once the reference is mentioned, as
     * an {@link IdentifierTree}.
     *
     */
    /*
    private class LinearFlow extends DefaultFlow<DefaultFlowState> {
        public LinearFlow(BaseTypeChecker checker, CompilationUnitTree root,
                Set<AnnotationMirror> annotations, AnnotatedTypeFactory factory) {
            super(checker, root, annotations, factory);
        }

        /**
         * Case 2: add {@code Unusable} to node type, if it is {@code Linear}.
         *
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
         *
        private void markAsUnusableIfLinear(ExpressionTree node) {
            if (!LinearVisitor.isLocalVarOrParam(node)) {
                return;
                }

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
    */
}
