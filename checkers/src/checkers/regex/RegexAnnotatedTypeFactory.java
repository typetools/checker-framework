package checkers.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import checkers.basetype.BaseTypeChecker;
import checkers.regex.quals.PolyRegex;
import checkers.regex.quals.Regex;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
import checkers.util.AnnotationUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * Adds {@link Regex} to the type of tree, in the following cases:
 *
 * <ol>
 *
 * <li value="1">a {@code String} or {@code char} literal that is a valid
 * regular expression</li>
 *
 * <li value="2">concatenation of two valid regular expression values
 * (either {@code String} or {@code char}.)</li>
 * 
 * <li value="3">for calls to Pattern.compile changes the group count value
 * of the return type to be the same as the parameter. For calls to the asRegex
 * methods of the classes in asRegexClasses these asRegex methods will return a
 * {@code @Regex String} with the same group count as the second argument to the
 * call to asRegex.</li>
 * 
 * <!--<li value="4">initialization of a char array that when converted to a String
 * is a valid regular expression.</li>-->
 *
 * </ol>
 *
 * Also, adds {@link PolyRegex} to the type of String/char concatenation of
 * a Regex and a PolyRegex or two PolyRegexs.
 */
public class RegexAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<RegexChecker> {

    /**
     * The Pattern.compile method.
     * 
     * @see java.util.regex.Pattern#compile(String)
     */
    private final ExecutableElement patternCompile;

    /**
     * Class names that contain an {@code asRegex(String, int)} method. These
     * asRegex methods will return a {@code @Regex String} with the same group
     * count as the second parameter to the asRegex call.
     * 
     * @see RegexUtil#asRegex(String, int)
     */
    private final String[] asRegexClasses = new String[] {
            "checkers.regex.RegexUtil", "plume.RegexUtil", "daikon.util.RegexUtil" };

    /**
     * A list of all of the ExecutableElements for the class names in
     * asRegexClasses.
     * 
     * @see #asRegexClasses
     * @see RegexUtil#asRegex(String, int)
     */
    private final List<ExecutableElement> asRegexes;

    public RegexAnnotatedTypeFactory(RegexChecker checker,
            CompilationUnitTree root) {
        super(checker, root);

        patternCompile = TreeUtils.getMethod("java.util.regex.Pattern", "compile", 1, env);
        asRegexes = new ArrayList<ExecutableElement>();
        for (String clazz : asRegexClasses) {
            try {
                asRegexes.add(TreeUtils.getMethod(clazz, "asRegex", 2, env));
            } catch (Exception e) {
                // The class couldn't be loaded so it must not be on the
                // classpath, just skip it.
                continue;
            }
        }
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
         * Case 1: valid regular expression String or char literal
         */
        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotated()) {
                String regex = null;
                if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    regex = (String) tree.getValue();
                } else if (tree.getKind() == Tree.Kind.CHAR_LITERAL) {
                    regex = Character.toString((Character) tree.getValue());
                }
                if (regex != null) {
                    if(RegexUtil.isRegex(regex)) {
                        int groupCount = Pattern.compile(regex).matcher("").groupCount();
                        type.addAnnotation(createRegexAnnotation(groupCount));
                    }
                }
            }
            return super.visitLiteral(tree, type);
        }

        /**
         * Case 2: concatenation of Regex or PolyRegex String/char literals
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

                if (lExprRE && rExprRE) {
                    int lGroupCount = checker.getGroupCount(lExpr.getAnnotation(Regex.class));
                    int rGroupCount = checker.getGroupCount(rExpr.getAnnotation(Regex.class));
                    // Remove current @Regex annotation...
                    type.removeAnnotation(Regex.class);
                    // ...and add a new one with the correct group count value.
                    type.addAnnotation(createRegexAnnotation(lGroupCount + rGroupCount));
                } else if (lExprPoly && rExprPoly
                        || lExprPoly && rExprRE
                        || lExprRE && rExprPoly)
                    type.addAnnotation(PolyRegex.class);
            }
            return super.visitBinary(tree, type);
        }

        /**
         * Case 2: Also handle compound String concatenation.
         */
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            if (TreeUtils.isStringCompoundConcatenation(node)) {
                AnnotatedTypeMirror rhs = getAnnotatedType(node.getExpression());
                AnnotatedTypeMirror lhs = getAnnotatedType(node.getVariable());
                if (lhs.hasAnnotation(Regex.class) && rhs.hasAnnotation(Regex.class)) {
                    int lCount = checker.getGroupCount(lhs.getAnnotation(Regex.class));
                    int rCount = checker.getGroupCount(rhs.getAnnotation(Regex.class));
                    type.removeAnnotation(Regex.class);
                    type.addAnnotation(createRegexAnnotation(lCount + rCount));
                }
            }
            return super.visitCompoundAssignment(node, type);
        }

        /**
         * Case 3: For a call to Pattern.compile, add an annotation to the
         * return type that has the same group count value as the parameter.
         * For calls to {@code asRegex(String, int)} change the return type to
         * have the same group count as the value of the second argument.
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            // TODO: Also get this to work with 2 argument Pattern.compile.
            if (TreeUtils.isMethodInvocation(tree, patternCompile, env)) {
                AnnotationMirror anno = getAnnotatedType(tree.getArguments().get(0)).getAnnotation(Regex.class);
                int groupCount = checker.getGroupCount(anno);
                // Remove current @Regex annotation...
                type.removeAnnotation(Regex.class);
                // ...and add a new one with the correct group count value.
                type.addAnnotation(createRegexAnnotation(groupCount));
            } else if (isAsRegex(tree)) {
                ExpressionTree groupArg = tree.getArguments().get(1);
                if (groupArg.getKind() == Kind.INT_LITERAL) {
                    LiteralTree literal = (LiteralTree) groupArg;
                    int paramGroups = (Integer) literal.getValue();
                    type.removeAnnotation(Regex.class);
                    type.addAnnotation(createRegexAnnotation(paramGroups));
                }
            }
            return super.visitMethodInvocation(tree, type);
        }

        /**
         * Returns true if the given MethodInvocationTree represents a call to
         * an asRegex method in one of the classes in asRegexClasses. 
         */
        private boolean isAsRegex(MethodInvocationTree tree) {
            for (ExecutableElement asRegex : asRegexes) {
                if (TreeUtils.isMethodInvocation(tree, asRegex, env)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns a new Regex annotation with the given group count.
         */
        private AnnotationMirror createRegexAnnotation(int groupCount) {
            AnnotationUtils.AnnotationBuilder builder =
                new AnnotationUtils.AnnotationBuilder(env, Regex.class.getCanonicalName());
            builder.setValue("value", groupCount);
            return builder.build();
        }

//         This won't work correctly until flow sensitivity is supported by the
//         the regex checker. For example:
//
//         char @Regex [] arr = {'r', 'e'};
//         arr[0] = '('; // type is still "char @Regex []", but this is no longer correct
//
//         There are associated tests in tests/regex/Simple.java:testCharArrays
//         that can be uncommented when this is uncommented.
//        /**
//         * Case 4: a char array that as a String is a valid regular expression.
//         */
//        @Override
//        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
//            boolean isCharArray = ((ArrayType) type.getUnderlyingType())
//                    .getComponentType().getKind() == TypeKind.CHAR;
//            if (isCharArray && tree.getInitializers() != null) {
//                List<? extends ExpressionTree> initializers = tree.getInitializers();
//                StringBuilder charArray = new StringBuilder();
//                boolean allLiterals = true;
//                for (int i = 0; allLiterals && i < initializers.size(); i++) {
//                    ExpressionTree e = initializers.get(i);
//                    if (e.getKind() == Tree.Kind.CHAR_LITERAL) {
//                        charArray.append(((LiteralTree) e).getValue());
//                    } else if (getAnnotatedType(e).hasAnnotation(Regex.class)) {
//                        // if there's an @Regex char in the array then substitute
//                        // it with a .
//                        charArray.append('.');
//                    } else {
//                        allLiterals = false;
//                    }
//                }
//                if (allLiterals && RegexUtil.isRegex(charArray.toString())) {
//                    type.addAnnotation(Regex.class);
//                }
//            }
//            return super.visitNewArray(tree, type);
//        }
    }
}
