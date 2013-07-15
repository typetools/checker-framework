package checkers.regex;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import javacutils.AnnotationUtils;
import javacutils.TreeUtils;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.CFStore;
import checkers.flow.CFValue;
import checkers.regex.quals.PartialRegex;
import checkers.regex.quals.PolyRegex;
import checkers.regex.quals.Regex;
import checkers.regex.quals.RegexBottom;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.TreeAnnotator;
import checkers.util.AnnotationBuilder;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * Adds {@link Regex} to the type of tree, in the following cases:
 *
 * <ol>
 *
 * <li value="1">a {@code String} or {@code char} literal that is a valid
 * regular expression</li>
 *
 * <li value="2">concatenation of two valid regular expression values
 * (either {@code String} or {@code char}) or two partial regular expression
 * values that make a valid regular expression when concatenated.</li>
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
 * Provides a basic analysis of concatenation of partial regular expressions
 * to determine if a valid regular expression is produced by concatenating
 * non-regular expression Strings. Do do this, {@link PartialRegex} is added
 * to the type of tree in the following cases:
 *
 * <ol>
 *
 * <li value="1">a String literal that is not a valid regular expression.</li>
 *
 * <li value="2">concatenation of two partial regex Strings that doesn't result
 * in a regex String or a partial regex and regex String.</li>
 *
 * </ol>
 *
 * Also, adds {@link PolyRegex} to the type of String/char concatenation of
 * a Regex and a PolyRegex or two PolyRegexs.
 */
public class RegexAnnotatedTypeFactory extends AbstractBasicAnnotatedTypeFactory<RegexChecker, CFValue, CFStore, RegexTransfer, RegexAnalysis> {

    /**
     * The Pattern.compile method.
     *
     * @see java.util.regex.Pattern#compile(String)
     */
    private final ExecutableElement patternCompile;

    /**
     * The value method of the PartialRegex qualifier.
     *
     * @see checkers.regex.quals.PartialRegex
     */
    private final ExecutableElement partialRegexValue;

    /**
     * Class names that contain an {@code asRegex(String, int)} method. These
     * asRegex methods will return a {@code @Regex String} with the same group
     * count as the second parameter to the asRegex call.
     *
     * @see RegexUtil#asRegex(String, int)
     */
    /*package-scope*/ static final String[] regexUtilClasses = new String[] {
            "checkers.regex.RegexUtil",
            "plume.RegexUtil",
            "daikon.util.RegexUtil" };

    /** The {@code @Regex} annotation. */
    private final AnnotationMirror REGEX;

    public RegexAnnotatedTypeFactory(RegexChecker checker,
            CompilationUnitTree root) {
        super(checker, root);

        patternCompile = TreeUtils.getMethod("java.util.regex.Pattern", "compile", 1, processingEnv);
        partialRegexValue = TreeUtils.getMethod("checkers.regex.quals.PartialRegex", "value", 0, processingEnv);
        REGEX = AnnotationUtils.fromClass(elements, Regex.class);
        this.postInit();
    }

    @Override
    public TreeAnnotator createTreeAnnotator(RegexChecker checker) {
        return new RegexTreeAnnotator(checker);
    }

    /**
     * Returns a new Regex annotation with the given group count.
     */
    /*package-scope*/ AnnotationMirror createRegexAnnotation(int groupCount) {
        AnnotationBuilder builder =
            new AnnotationBuilder(processingEnv, Regex.class.getCanonicalName());
        builder.setValue("value", groupCount);
        return builder.build();
    }

    /** This method is a copy of RegexUtil.isRegex.
     * We cannot directly use RegexUtil, because it uses type annotations
     * which cannot be used in IDEs (yet).
     */
    /*@SuppressWarnings("purity")*/ // the checker cannot prove that the method is pure, but it is
    /*@dataflow.quals.Pure*/
    private static boolean isRegex(String s) {
        try {
            Pattern.compile(s);
        } catch (PatternSyntaxException e) {
            return false;
        }
        return true;
    }

    private class RegexTreeAnnotator extends TreeAnnotator {

        public RegexTreeAnnotator(BaseTypeChecker<?> checker) {
            super(checker, RegexAnnotatedTypeFactory.this);
        }

        /**
         * Case 1: valid regular expression String or char literal.
         * Adds PartialRegex annotation to String literals that are not valid
         * regular expressions.
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
                    if (isRegex(regex)) {
                        int groupCount = checker.getGroupCount(regex);
                        type.addAnnotation(createRegexAnnotation(groupCount));
                    } else {
                        type.addAnnotation(createPartialRegexAnnotation(regex));
                    }
                }
            }
            return super.visitLiteral(tree, type);
        }

        /**
         * Case 2: concatenation of Regex or PolyRegex String/char literals.
         * Also handles concatenation of partial regular expressions.
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotated()
                && TreeUtils.isStringConcatenation(tree)) {
                AnnotatedTypeMirror lExpr = getAnnotatedType(tree.getLeftOperand());
                AnnotatedTypeMirror rExpr = getAnnotatedType(tree.getRightOperand());

                boolean lExprRE = lExpr.hasAnnotation(Regex.class);
                boolean rExprRE = rExpr.hasAnnotation(Regex.class);
                boolean lExprPart = lExpr.hasAnnotation(PartialRegex.class);
                boolean rExprPart = rExpr.hasAnnotation(PartialRegex.class);
                boolean lExprPoly = lExpr.hasAnnotation(PolyRegex.class);
                boolean rExprPoly = rExpr.hasAnnotation(PolyRegex.class);

                if (lExprRE && rExprRE) {
                    int lGroupCount = checker.getGroupCount(lExpr.getAnnotation(Regex.class));
                    int rGroupCount = checker.getGroupCount(rExpr.getAnnotation(Regex.class));
                    // Remove current @Regex annotation...
                    type.removeAnnotationInHierarchy(REGEX);
                    // ...and add a new one with the correct group count value.
                    type.addAnnotation(createRegexAnnotation(lGroupCount + rGroupCount));
                } else if (lExprPoly && rExprPoly
                        || lExprPoly && rExprRE
                        || lExprRE && rExprPoly) {
                    type.addAnnotation(PolyRegex.class);
                } else if (lExprPart && rExprPart) {
                    String lRegex = getPartialRegexValue(lExpr);
                    String rRegex = getPartialRegexValue(rExpr);
                    String concat = lRegex + rRegex;
                    if (isRegex(concat)) {
                        int groupCount = checker.getGroupCount(concat);
                        type.addAnnotation(createRegexAnnotation(groupCount));
                    } else {
                        type.addAnnotation(createPartialRegexAnnotation(concat));
                    }
                } else if (lExprRE && rExprPart) {
                    String rRegex = getPartialRegexValue(rExpr);
                    String concat = "e" + rRegex;
                    type.addAnnotation(createPartialRegexAnnotation(concat));
                } else if (lExprPart && rExprRE) {
                    String lRegex = getPartialRegexValue(lExpr);
                    String concat = lRegex + "e";
                    type.addAnnotation(createPartialRegexAnnotation(concat));
                }
            }
            return null; // super.visitBinary(tree, type);
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
                    type.removeAnnotationInHierarchy(REGEX);
                    type.addAnnotation(createRegexAnnotation(lCount + rCount));
                }
            }
            return null; // super.visitCompoundAssignment(node, type);
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
            if (TreeUtils.isMethodInvocation(tree, patternCompile, processingEnv)) {
                ExpressionTree arg0 = tree.getArguments().get(0);
                AnnotationMirror regexAnno = getAnnotatedType(arg0).getAnnotation(Regex.class);
                AnnotationMirror bottomAnno = getAnnotatedType(arg0).getAnnotation(RegexBottom.class);
                if (regexAnno != null) {
                    int groupCount = checker.getGroupCount(regexAnno);
                    // Remove current @Regex annotation...
                    // ...and add a new one with the correct group count value.
                    type.replaceAnnotation(createRegexAnnotation(groupCount));
                } else if (bottomAnno != null) {
                    type.replaceAnnotation(AnnotationUtils.fromClass(elements, RegexBottom.class));
                }
            }
            return super.visitMethodInvocation(tree, type);
        }

        /**
         * Returns a new PartialRegex annotation with the given partial regular
         * expression.
         */
        private AnnotationMirror createPartialRegexAnnotation(String partial) {
            AnnotationBuilder builder =
                new AnnotationBuilder(processingEnv, PartialRegex.class.getCanonicalName());
            builder.setValue("value", partial);
            return builder.build();
        }

        /**
         * Returns the value of a PartialRegex annotation.
         */
        private String getPartialRegexValue(AnnotatedTypeMirror type) {
            return (String) AnnotationUtils.getElementValuesWithDefaults(type.getAnnotation(PartialRegex.class)).get(partialRegexValue).getValue();
        }

//         This won't work correctly until flow sensitivity is supported by the
//         the Regex Checker. For example:
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
