package checkers.tainting;

import com.sun.source.tree.*;

import checkers.basetype.BaseTypeChecker;
import checkers.tainting.quals.Untainted;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.util.TreeUtils;

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
        this.postInit();
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

        public TaintingTreeAnnotator(BaseTypeChecker checker) {
            super(checker, TaintingAnnotatedTypeFactory.this);
        }

        /**
         * Handles String concatenation.  The result of concatenating
         * untainted strings is also untainted String.
         *
         * Note: Flow infers the rule for '+=' from here
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isStringConcatenation(tree)) {
                AnnotatedTypeMirror lExpr = getAnnotatedType(tree.getLeftOperand());
                AnnotatedTypeMirror rExpr = getAnnotatedType(tree.getRightOperand());
                if (lExpr.hasAnnotation(Untainted.class) && rExpr.hasAnnotation(Untainted.class))
                    type.addAnnotation(Untainted.class);
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
//                    type.addAnnotation(Untainted.class);
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
