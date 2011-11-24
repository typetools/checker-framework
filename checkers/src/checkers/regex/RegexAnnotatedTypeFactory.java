package checkers.regex;

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
 * expression values.
 *
 * </ol>
 *
 * Adds {@link Regex} to the type of each {@code String} literal that is
 * a syntactically valid regular expression.
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
         * Case 2: concatenation of two regular expression Strings literals
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotated()
                && TreeUtils.isStringConcatenation(tree)) {
                AnnotatedTypeMirror lExpr = getAnnotatedType(tree.getLeftOperand());
                AnnotatedTypeMirror rExpr = getAnnotatedType(tree.getRightOperand());
                if (lExpr.hasAnnotation(Regex.class)
                        && rExpr.hasAnnotation(Regex.class))
                    type.addAnnotation(Regex.class);
            }
            return super.visitBinary(tree, type);
        }
    }
}
