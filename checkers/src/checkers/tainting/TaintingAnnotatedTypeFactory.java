package checkers.tainting;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.*;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.util.AnnotationUtils;
import checkers.util.TypesUtils;

/**
 * Adds implicit and default {@code Untainted} annotation, only if the user
 * does not explicitly insert them.
 * <p/>
 *
 * This factory will add the {@link Untainted} annotation to a type if the
 * input is
 *
 * <ol>
 * <li value="1">a string literal (Note: Handled by Unqualified meta-annotation)
 * <li value="2">a string concatenation where both operands are untainted
 * </ol>
 *
 */
public class TaintingAnnotatedTypeFactory
  extends BasicAnnotatedTypeFactory<TaintingChecker> {

    public TaintingAnnotatedTypeFactory(TaintingChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }

    /*
     * Override createTreeAnnotator to customize type based on tree kind
     * and content.
     */
    protected TreeAnnotator createTreeAnnotator(TaintingChecker checker) {
        return new TaintingTreeAnnotator(checker);
    }

    /*
     * To insert an annotation for a kind of a tree, FOO, override
     * the appropriate visitFOO method and modify the passed
     * AnnotationTypeMirror
     */
    private class TaintingTreeAnnotator extends TreeAnnotator {
        private AnnotationMirror UNTAINTED;

        public TaintingTreeAnnotator(BaseTypeChecker checker) {
            super(checker);
            AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
            UNTAINTED = annoFactory.fromClass(Untainted.class);
        }

        /**
         * Handles String concatenation.  The result of concatenating
         * untainted strings is also untainted String.
         *
         * Note: Flow infers the rule for '+=' from here
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            if (tree.getKind() == Tree.Kind.PLUS
                && TypesUtils.isDeclaredOfName(type.getUnderlyingType(), "java.lang.String")) {
                AnnotatedTypeMirror lExpr = getAnnotatedType(tree.getLeftOperand());
                AnnotatedTypeMirror rExpr = getAnnotatedType(tree.getRightOperand());
                if (lExpr.hasAnnotation(UNTAINTED) && rExpr.hasAnnotation(UNTAINTED))
                    type.addAnnotation(UNTAINTED);
            }
            return super.visitBinary(tree, type);
        }

        // For now, assume all string literals are untainted
//        /**
//         * Performs extra checks for string literal to determine its taintness
//         */
//        @Override
//        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
//            if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
//                LiteralTree sl = (LiteralTree)tree;
//                String str = (String)sl.getValue();
//
//                if (isUntaintedValue(str)) {
//                    type.addAnnotation(UNTAINTED);
//                }
//            }
//            return super.visitLiteral(tree, type);
//        }
//
//        private boolean isUntaintedValue(String s) {
//            throw new RuntimeException("Not implemented yet");
//        }

    }
}
