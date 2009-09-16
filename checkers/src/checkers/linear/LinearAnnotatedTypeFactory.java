package checkers.linear;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.Flow;
import checkers.linear.quals.*;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.util.AnnotationUtils;
import checkers.util.TreeUtils;

public class LinearAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<LinearChecker> {

    public LinearAnnotatedTypeFactory(LinearChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }

    public void annotateImplicit(Element elt, AnnotatedTypeMirror type) {
        if (type.getAnnotations().isEmpty() && elt.getKind().isClass()) {
            type.addAnnotation(Unusable.class);
        }
        super.annotateImplicit(elt, type);
    }

    public Flow createFlow(LinearChecker checker, CompilationUnitTree tree,
            Set<AnnotationMirror> flowQuals) {
        return new LinearFlow(checker, tree, flowQuals, this);
    }

    private static class LinearFlow extends Flow {
        private final AnnotationMirror LINEAR, UNUSABLE;

        public LinearFlow(BaseTypeChecker checker, CompilationUnitTree root,
                Set<AnnotationMirror> annotations, AnnotatedTypeFactory factory) {
            super(checker, root, annotations, factory);

            AnnotationUtils annoFactory = AnnotationUtils.getInstance(checker.getProcessingEnvironment());
            LINEAR = annoFactory.fromClass(Linear.class);
            UNUSABLE = annoFactory.fromClass(Unusable.class);
        }

        private void inferUnusableIfNeeded(ExpressionTree node) {
            if (!LinearVisitor.possibleUnusable(node))
                return;

            Element elem = TreeUtils.elementFromUse(node);
            assert elem != null;
            if (vars.contains(elem)) {
                int idx = vars.indexOf(elem);
                if (annos.get(LINEAR, idx)) {
                    annos.set(UNUSABLE, idx);
                    annos.clear(LINEAR, idx);
                }
            }
        }

        public Void visitIdentifier(IdentifierTree node, Void p) {
            super.visitIdentifier(node, p);
            inferUnusableIfNeeded(node);
            return null;
        }

        public Void visitMemberSelect(MemberSelectTree node, Void p) {
            inferUnusableIfNeeded(node);
            super.visitMemberSelect(node, p);
            return null;
        }
    }
}
