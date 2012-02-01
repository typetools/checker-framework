package checkers.regex;

import checkers.regex.quals.PolyRegex;
import checkers.regex.quals.Regex;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.util.TreeUtils;

/**
 * Adds {@link Regex} to the type of tree, in two cases:
 *
 * <ol>
 *
 * <li value="1">a {@code String} literal that is a valid regular expression</li>
 *
 * <li value="2">a {@code String} concatenation tree of two valid regular
 * expression values.</li>
 *
 * </ol>
 *
 * Also, adds {@link PolyRegex} to the type of concatenation of a Regex and a
 * PolyRegex {@code String} or two PolyRegex {@code String}s.
 */
public class RegexAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<RegexChecker> {

    public RegexAnnotatedTypeFactory(RegexChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public TreeAnnotator createTreeAnnotator(RegexChecker checker) {
        return new RegexTreeAnnotator(checker);
    }

    private class RegexTreeAnnotator extends TreeAnnotator {

        public RegexTreeAnnotator(BaseTypeChecker checker) {
            super(checker, RegexAnnotatedTypeFactory.this);
        }

        /**
         * Case 1: valid regular expression String literal
         */
        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotated()
                && tree.getKind() == Tree.Kind.STRING_LITERAL
                && RegexUtil.isRegex((String)((LiteralTree)tree).getValue())) {
                type.addAnnotation(Regex.class);
            }
            return super.visitLiteral(tree, type);
        }

        /**
         * Case 2: concatenation of two regular expression String literals,
         * concatenation of two PolyRegex Strings and concatenation of a Regex
         * and PolyRegex String.
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotated()
                && TreeUtils.isStringConcatenation(tree)) {
                AnnotatedTypeMirror lExpr = getAnnotatedType(tree.getLeftOperand());
                AnnotatedTypeMirror rExpr = getAnnotatedType(tree.getRightOperand());
                
                boolean lExprRE = lExpr.hasAnnotation(Regex.class);
                boolean rExprRE = rExpr.hasAnnotation(Regex.class);
                boolean lExprPoly = lExpr.hasAnnotation(PolyRegex.class);
                boolean rExprPoly = rExpr.hasAnnotation(PolyRegex.class);
                
                if (lExprRE && rExprRE)
                    type.addAnnotation(Regex.class);
                else if (lExprPoly && rExprPoly
                        || lExprPoly && rExprRE
                        || lExprRE && rExprPoly)
                    type.addAnnotation(PolyRegex.class);
            }
            return super.visitBinary(tree, type);
        }
    }
}
