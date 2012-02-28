package checkers.regex;

import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
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
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * Adds {@link Regex} to the type of tree, in the following cases:
 *
 * <ol>
 *
 * <li value="1">a {@code String} or (@code char} literal that is a valid
 * regular expression</li>
 *
 * <li value="2">concatenation tree of two valid regular expression values
 * (either {@code String} or {@code char}.)</li>
 * 
 * <li value="3">for calls to Pattern.compile changes the group count value
 * of the return type to be the same as the parameter.</li>
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
  
    private final ExecutableElement patternCompile;
    private final ExecutableElement regexValue;

    public RegexAnnotatedTypeFactory(RegexChecker checker,
            CompilationUnitTree root) {
        super(checker, root);

        patternCompile = TreeUtils.getMethod("java.util.regex.Pattern", "compile", 1, env);
        regexValue = TreeUtils.getMethod("checkers.regex.quals.Regex", "value", 0, env);
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
                        AnnotationUtils.AnnotationBuilder builder =
                            new AnnotationUtils.AnnotationBuilder(env, Regex.class.getCanonicalName());
                        builder.setValue("value", groupCount);
                        type.addAnnotation(builder.build());
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

                if (lExprRE && rExprRE)
                    type.addAnnotation(Regex.class);
                else if (lExprPoly && rExprPoly
                        || lExprPoly && rExprRE
                        || lExprRE && rExprPoly)
                    type.addAnnotation(PolyRegex.class);
            }
            return super.visitBinary(tree, type);
        }

        /**
         * Case 3: For a call to Pattern.compile, add an annotation to the
         * return type that has the same group count value as the parameter.
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            // TODO: Also get this to work with 2 argument Pattern.compile.
            if (TreeUtils.isMethodInvocation(tree, patternCompile, env)) {
                AnnotationMirror anno = getAnnotatedType(tree.getArguments().get(0)).getAnnotation(Regex.class);
                AnnotationValue groupCountValue = AnnotationUtils.getElementValuesWithDefaults(anno).get(regexValue);
                // If group count value is null then there's no Regex annotation
                // on the parameter so set the group count to 0. This would happen
                // if a non-regex string is passed to Pattern.compile but warnings
                // are suppressed.
                int groupCount = groupCountValue == null ? 0 : (Integer) groupCountValue.getValue();
                
                AnnotationUtils.AnnotationBuilder builder =
                    new AnnotationUtils.AnnotationBuilder(env, Regex.class.getCanonicalName());
                builder.setValue("value", groupCount);
                // Remove current @Regex annotation...
                type.removeAnnotation(Regex.class);
                // ...and add a new one with the correct value.
                type.addAnnotation(builder.build());
            }
            return super.visitMethodInvocation(tree, type);
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
