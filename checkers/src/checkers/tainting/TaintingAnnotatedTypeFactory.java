package checkers.tainting;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.*;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.util.AnnotationUtils;

public class TaintingAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<TaintingChecker> {

    public TaintingAnnotatedTypeFactory(TaintingChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }

    protected TreeAnnotator createTreeAnnotator(TaintingChecker checker) {
        return new TaintingTreeAnnotator(checker);
    }

    private class TaintingTreeAnnotator extends TreeAnnotator {
        private AnnotationMirror UNTAINTED;

        public TaintingTreeAnnotator(BaseTypeChecker checker) {
            super(checker);
            AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
            UNTAINTED = annoFactory.fromClass(Untainted.class);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror p) {
            if (node.getKind() == Tree.Kind.PLUS) {
                AnnotatedTypeMirror lExpr = getAnnotatedType(node.getLeftOperand());
                AnnotatedTypeMirror rExpr = getAnnotatedType(node.getRightOperand());
                if (lExpr.hasAnnotation(UNTAINTED) && rExpr.hasAnnotation(UNTAINTED))
                    p.addAnnotation(UNTAINTED);
            }
            return super.visitBinary(node, p);
        }
    }
}
