package org.checkerframework.checker.regex;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.regex.Pattern;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.regex.qual.PartialRegex;
import org.checkerframework.checker.regex.qual.PolyRegex;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.checker.regex.qual.RegexBottom;
import org.checkerframework.checker.regex.qual.UnknownRegex;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Adds {@link Regex} to the type of tree, in the following cases:
 *
 * <ol>
 *   <li value="1">a {@code String} or {@code char} literal that is a valid regular expression
 *   <li value="2">concatenation of two valid regular expression values (either {@code String} or
 *       {@code char}) or two partial regular expression values that make a valid regular expression
 *       when concatenated.
 *   <li value="3">for calls to Pattern.compile, change the group count value of the return type to
 *       be the same as the parameter. For calls to the asRegex methods of the classes in
 *       asRegexClasses, the returned {@code @Regex String} gets the same group count as the second
 *       argument to the call to asRegex.
 *       <!--<li value="4">initialization of a char array that when converted to a String
 * is a valid regular expression.</li>-->
 * </ol>
 *
 * Provides a basic analysis of concatenation of partial regular expressions to determine if a valid
 * regular expression is produced by concatenating non-regular expression Strings. Do do this,
 * {@link PartialRegex} is added to the type of tree in the following cases:
 *
 * <ol>
 *   <li value="1">a String literal that is not a valid regular expression.
 *   <li value="2">concatenation of two partial regex Strings that doesn't result in a regex String
 *       or a partial regex and regex String.
 * </ol>
 *
 * Also, adds {@link PolyRegex} to the type of String/char concatenation of a Regex and a PolyRegex
 * or two PolyRegexs.
 */
public class RegexAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The @{@link Regex} annotation. */
    protected final AnnotationMirror REGEX = AnnotationBuilder.fromClass(elements, Regex.class);
    /** The @{@link RegexBottom} annotation. */
    protected final AnnotationMirror REGEXBOTTOM =
            AnnotationBuilder.fromClass(elements, RegexBottom.class);
    /** The @{@link PartialRegex} annotation. */
    protected final AnnotationMirror PARTIALREGEX =
            AnnotationBuilder.fromClass(elements, PartialRegex.class);
    /** The @{@link PolyRegex} annotation. */
    protected final AnnotationMirror POLYREGEX =
            AnnotationBuilder.fromClass(elements, PolyRegex.class);

    /** The method that returns the value element of a {@code @Regex} annotation. */
    protected final ExecutableElement regexValueElement =
            TreeUtils.getMethod(
                    org.checkerframework.checker.regex.qual.Regex.class.getName(),
                    "value",
                    0,
                    processingEnv);

    /**
     * The value method of the PartialRegex qualifier.
     *
     * @see org.checkerframework.checker.regex.qual.PartialRegex
     */
    private final ExecutableElement partialRegexValue =
            TreeUtils.getMethod(
                    org.checkerframework.checker.regex.qual.PartialRegex.class.getName(),
                    "value",
                    0,
                    processingEnv);

    /**
     * The Pattern.compile method.
     *
     * @see java.util.regex.Pattern#compile(String)
     */
    private final ExecutableElement patternCompile =
            TreeUtils.getMethod(
                    java.util.regex.Pattern.class.getName(), "compile", 1, processingEnv);

    // TODO use? private TypeMirror[] legalReferenceTypes;

    public RegexAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        /*
        legalReferenceTypes = new TypeMirror[] {
            getTypeMirror("java.lang.CharSequence"),
            getTypeMirror("java.lang.Character"),
            getTypeMirror("java.util.regex.Pattern"),
            getTypeMirror("java.util.regex.MatchResult") };
         */

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(
                Regex.class, PartialRegex.class,
                RegexBottom.class, UnknownRegex.class);
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new RegexTransfer((CFAnalysis) analysis);
    }

    /** Returns a new Regex annotation with the given group count. */
    /*package-scope*/ AnnotationMirror createRegexAnnotation(int groupCount) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Regex.class);
        if (groupCount > 0) {
            builder.setValue("value", groupCount);
        }
        return builder.build();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new RegexQualifierHierarchy(factory, REGEXBOTTOM);
    }

    /**
     * A custom qualifier hierarchy for the Regex Checker. This makes a regex annotation a subtype
     * of all regex annotations with lower group count values. For example, {@code @Regex(3)} is a
     * subtype of {@code @Regex(1)}. All regex annotations are subtypes of {@code @Regex}, which has
     * a default value of 0.
     */
    private final class RegexQualifierHierarchy extends GraphQualifierHierarchy {

        public RegexQualifierHierarchy(MultiGraphFactory f, AnnotationMirror bottom) {
            super(f, bottom);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameByName(subAnno, REGEX)
                    && AnnotationUtils.areSameByName(superAnno, REGEX)) {
                int rhsValue = getRegexValue(subAnno);
                int lhsValue = getRegexValue(superAnno);
                return lhsValue <= rhsValue;
            }
            // TODO: subtyping between PartialRegex?
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameByName(superAnno, REGEX)) {
                superAnno = REGEX;
            }
            if (AnnotationUtils.areSameByName(subAnno, REGEX)) {
                subAnno = REGEX;
            }
            if (AnnotationUtils.areSameByName(superAnno, PARTIALREGEX)) {
                superAnno = PARTIALREGEX;
            }
            if (AnnotationUtils.areSameByName(subAnno, PARTIALREGEX)) {
                subAnno = PARTIALREGEX;
            }
            return super.isSubtype(subAnno, superAnno);
        }

        /** Gets the value out of a regex annotation. */
        private int getRegexValue(AnnotationMirror anno) {
            return (Integer)
                    AnnotationUtils.getElementValuesWithDefaults(anno)
                            .get(regexValueElement)
                            .getValue();
        }
    }

    /**
     * Returns the group count value of the given annotation or 0 if there's a problem getting the
     * group count value.
     */
    public int getGroupCount(AnnotationMirror anno) {
        AnnotationValue groupCountValue =
                AnnotationUtils.getElementValuesWithDefaults(anno).get(regexValueElement);
        // If group count value is null then there's no Regex annotation
        // on the parameter so set the group count to 0. This would happen
        // if a non-regex string is passed to Pattern.compile but warnings
        // are suppressed.
        return (groupCountValue == null) ? 0 : (Integer) groupCountValue.getValue();
    }

    /** Returns the number of groups in the given regex String. */
    public static int getGroupCount(@Regex String regexp) {
        return Pattern.compile(regexp).matcher("").groupCount();
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        // Don't call super.createTreeAnnotator because the PropagationTreeAnnotator types binary
        // expressions as lub.
        return new ListTreeAnnotator(
                new LiteralTreeAnnotator(this).addStandardLiteralQualifiers(),
                new RegexTreeAnnotator(this),
                new RegexPropagationTreeAnnotator(this));
    }

    private static class RegexPropagationTreeAnnotator extends PropagationTreeAnnotator {

        public RegexPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            // Don't call super method which will try to create a LUB
            // Even when it is not yet valid: i.e. between a @PolyRegex and a @Regex
            return null;
        }
    }

    private class RegexTreeAnnotator extends TreeAnnotator {

        public RegexTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * Case 1: valid regular expression String or char literal. Adds PartialRegex annotation to
         * String literals that are not valid regular expressions.
         */
        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotatedInHierarchy(REGEX)) {
                String regex = null;
                if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    regex = (String) tree.getValue();
                } else if (tree.getKind() == Tree.Kind.CHAR_LITERAL) {
                    regex = Character.toString((Character) tree.getValue());
                }
                if (regex != null) {
                    if (RegexUtil.isRegex(regex)) {
                        int groupCount = getGroupCount(regex);
                        type.addAnnotation(createRegexAnnotation(groupCount));
                    } else {
                        type.addAnnotation(createPartialRegexAnnotation(regex));
                    }
                }
            }
            return super.visitLiteral(tree, type);
        }

        /**
         * Case 2: concatenation of Regex or PolyRegex String/char literals. Also handles
         * concatenation of partial regular expressions.
         */
        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotatedInHierarchy(REGEX) && TreeUtils.isStringConcatenation(tree)) {
                AnnotatedTypeMirror lExpr = getAnnotatedType(tree.getLeftOperand());
                AnnotatedTypeMirror rExpr = getAnnotatedType(tree.getRightOperand());

                Integer lGroupCount = getMinimumRegexCount(lExpr);
                Integer rGroupCount = getMinimumRegexCount(rExpr);
                boolean lExprRE = lGroupCount != null;
                boolean rExprRE = rGroupCount != null;
                boolean lExprPart = lExpr.hasAnnotation(PartialRegex.class);
                boolean rExprPart = rExpr.hasAnnotation(PartialRegex.class);
                boolean lExprPoly = lExpr.hasAnnotation(PolyRegex.class);
                boolean rExprPoly = rExpr.hasAnnotation(PolyRegex.class);

                if (lExprRE && rExprRE) {
                    // Remove current @Regex annotation...
                    type.removeAnnotationInHierarchy(REGEX);
                    // ...and add a new one with the correct group count value.
                    type.addAnnotation(createRegexAnnotation(lGroupCount + rGroupCount));
                } else if ((lExprPoly && rExprPoly)
                        || (lExprPoly && rExprRE)
                        || (lExprRE && rExprPoly)) {
                    type.addAnnotation(PolyRegex.class);
                } else if (lExprPart && rExprPart) {
                    String lRegex = getPartialRegexValue(lExpr);
                    String rRegex = getPartialRegexValue(rExpr);
                    String concat = lRegex + rRegex;
                    if (RegexUtil.isRegex(concat)) {
                        int groupCount = getGroupCount(concat);
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

        /** Case 2: Also handle compound String concatenation. */
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            if (TreeUtils.isStringCompoundConcatenation(node)) {
                AnnotatedTypeMirror rhs = getAnnotatedType(node.getExpression());
                AnnotatedTypeMirror lhs = getAnnotatedType(node.getVariable());

                final Integer lhsRegexCount = getMinimumRegexCount(lhs);
                final Integer rhsRegexCount = getMinimumRegexCount(rhs);

                if (lhsRegexCount != null && rhsRegexCount != null) {
                    int lCount = getGroupCount(lhs.getAnnotation(Regex.class));
                    int rCount = getGroupCount(rhs.getAnnotation(Regex.class));
                    type.removeAnnotationInHierarchy(REGEX);
                    type.addAnnotation(createRegexAnnotation(lCount + rCount));
                }
            }
            return null; // super.visitCompoundAssignment(node, type);
        }

        /**
         * Case 3: For a call to Pattern.compile, add an annotation to the return type that has the
         * same group count value as the parameter. For calls to {@code asRegex(String, int)} change
         * the return type to have the same group count as the value of the second argument.
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            // TODO: Also get this to work with 2 argument Pattern.compile.
            if (TreeUtils.isMethodInvocation(tree, patternCompile, processingEnv)) {
                ExpressionTree arg0 = tree.getArguments().get(0);

                final AnnotatedTypeMirror argType = getAnnotatedType(arg0);
                Integer regexCount = getMinimumRegexCount(argType);
                AnnotationMirror bottomAnno =
                        getAnnotatedType(arg0).getAnnotation(RegexBottom.class);

                if (regexCount != null) {
                    // Remove current @Regex annotation...
                    // ...and add a new one with the correct group count value.
                    type.replaceAnnotation(createRegexAnnotation(regexCount));
                } else if (bottomAnno != null) {
                    type.replaceAnnotation(
                            AnnotationBuilder.fromClass(elements, RegexBottom.class));
                }
            }
            return super.visitMethodInvocation(tree, type);
        }

        /** Returns a new PartialRegex annotation with the given partial regular expression. */
        private AnnotationMirror createPartialRegexAnnotation(String partial) {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, PartialRegex.class);
            builder.setValue("value", partial);
            return builder.build();
        }

        /** Returns the value of a PartialRegex annotation. */
        private String getPartialRegexValue(AnnotatedTypeMirror type) {
            return (String)
                    AnnotationUtils.getElementValuesWithDefaults(
                                    type.getAnnotation(PartialRegex.class))
                            .get(partialRegexValue)
                            .getValue();
        }

        /**
         * Returns the value of the Regex annotation on the given type or NULL if there is no Regex
         * annotation. If type is a TYPEVAR, WILDCARD, or INTERSECTION type, visit first their
         * primary annotation then visit their upper bounds to get the Regex annotation. The method
         * gets "minimum" regex count because, depending on the bounds of a typevar or wildcard, the
         * actual type may have more than the upper bound's count.
         *
         * @param type type that may carry a Regex annotation
         * @return the Integer value of the Regex annotation (0 if no value exists)
         */
        private Integer getMinimumRegexCount(final AnnotatedTypeMirror type) {
            final AnnotationMirror primaryRegexAnno = type.getAnnotation(Regex.class);
            if (primaryRegexAnno == null) {
                switch (type.getKind()) {
                    case TYPEVAR:
                        return getMinimumRegexCount(((AnnotatedTypeVariable) type).getUpperBound());

                    case WILDCARD:
                        return getMinimumRegexCount(
                                ((AnnotatedWildcardType) type).getExtendsBound());

                    case INTERSECTION:
                        Integer maxBound = null;
                        for (final AnnotatedTypeMirror bound :
                                ((AnnotatedIntersectionType) type).directSuperTypes()) {
                            Integer boundRegexNum = getMinimumRegexCount(bound);
                            if (boundRegexNum != null) {
                                if (maxBound == null || boundRegexNum > maxBound) {
                                    maxBound = boundRegexNum;
                                }
                            }
                        }
                        return maxBound;
                    default:
                        // Nothing to do for other cases.
                }

                return null;
            }

            return getGroupCount(primaryRegexAnno);
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
