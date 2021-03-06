package org.checkerframework.checker.regex;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.regex.qual.EnhancedRegex;
import org.checkerframework.checker.regex.qual.PartialRegex;
import org.checkerframework.checker.regex.qual.PolyRegex;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.checker.regex.qual.RegexBottom;
import org.checkerframework.checker.regex.qual.UnknownRegex;
import org.checkerframework.checker.regex.util.RegexUtil;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.util.NodeUtils;
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
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Adds {@link EnhancedRegex} or {@link Regex} to the type of tree, in the following cases:
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
    /** The @{@link UnknownRegex} annotation. */
    protected final AnnotationMirror UNKNOWNREGEX =
            AnnotationBuilder.fromClass(elements, UnknownRegex.class);
    /** The @{@link EnhancedRegex} annotation. */
    protected final AnnotationMirror ENHANCEDREGEX =
            AnnotationBuilder.fromClass(elements, EnhancedRegex.class);

    /** The method that returns the value element of a {@code @Regex} annotation. */
    protected final ExecutableElement regexValueElement =
            TreeUtils.getMethod(
                    "org.checkerframework.checker.regex.qual.Regex", "value", 0, processingEnv);

    /** The method that returns the value element of a {@code @EnhancedRegex} annotation. */
    protected final ExecutableElement enhancedRegexValueElement =
            TreeUtils.getMethod(
                    "org.checkerframework.checker.regex.qual.EnhancedRegex",
                    "value",
                    0,
                    processingEnv);

    /** The {@code Matcher.group} method. */
    protected final ExecutableElement group =
            TreeUtils.getMethod(
                    java.util.regex.Matcher.class.getCanonicalName(),
                    "group",
                    processingEnv,
                    "int");

    /**
     * The value method of the PartialRegex qualifier.
     *
     * @see org.checkerframework.checker.regex.qual.PartialRegex
     */
    private final ExecutableElement partialRegexValue =
            TreeUtils.getMethod(
                    "org.checkerframework.checker.regex.qual.PartialRegex",
                    "value",
                    0,
                    processingEnv);

    /**
     * The Pattern.compile method.
     *
     * @see java.util.regex.Pattern#compile(String)
     */
    private final ExecutableElement patternCompile =
            TreeUtils.getMethod("java.util.regex.Pattern", "compile", 1, processingEnv);

    /**
     * Create a new RegexAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    public RegexAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

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

    /**
     * Returns a new Enhanced Regex annotation with the given group count.
     *
     * @param nonNullGroups list of groups that are definitely non-null and total number of groups
     * @return an EnhancedRegex annotation
     */
    AnnotationMirror createEnhancedRegexAnnotation(List<Integer> nonNullGroups) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, EnhancedRegex.class);
        if (nonNullGroups.size() > 1) {
            builder.setValue("value", nonNullGroups);
        }
        return builder.build();
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new RegexQualifierHierarchy(this.getSupportedTypeQualifiers(), elements);
    }

    /**
     * Returns true if the method invocation is Matcher.group(int).
     *
     * @param n the method invocation node to check
     * @return whether the method invocation node is {@code Matcher.group(int)}
     */
    public boolean isMatcherGroup(Node n) {
        return NodeUtils.isMethodInvocation(n, group, getProcessingEnv());
    }

    /**
     * A custom qualifier hierarchy for the Regex Checker. This makes a regex annotation a subtype
     * of all regex annotations with lower group count values. For example, {@code @Regex(3)} is a
     * subtype of {@code @Regex(1)}. All regex annotations are subtypes of {@code @Regex}, which has
     * a default value of 0.
     */
    private final class RegexQualifierHierarchy extends MostlyNoElementQualifierHierarchy {

        /** Qualifier kind for the @{@link Regex} annotation. */
        private final QualifierKind REGEX_KIND;
        /** Qualifier kind for the @{@link PartialRegex} annotation. */
        private final QualifierKind PARTIALREGEX_KIND;
        /** Qualifier kind for the @{@link EnhancedRegex} annotation. */
        private final QualifierKind ENHANCEDREGEX_KIND;
        /**
         * Creates a RegexQualifierHierarchy from the given classes.
         *
         * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
         * @param elements element utils
         */
        private RegexQualifierHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
            REGEX_KIND = getQualifierKind(REGEX);
            PARTIALREGEX_KIND = getQualifierKind(PARTIALREGEX);
            ENHANCEDREGEX_KIND = getQualifierKind(ENHANCEDREGEX);
        }

        @Override
        protected boolean isSubtypeWithElements(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            if (subKind == REGEX_KIND && superKind == REGEX_KIND) {
                int rhsValue = getRegexValue(subAnno);
                int lhsValue = getRegexValue(superAnno);
                return lhsValue <= rhsValue;
            } else if (subKind == PARTIALREGEX_KIND && superKind == PARTIALREGEX_KIND) {
                return AnnotationUtils.areSame(subAnno, superAnno);
            } else if (subKind == ENHANCEDREGEX_KIND && superKind == ENHANCEDREGEX_KIND) {
                List<Integer> rhsValue = getEnhancedRegexValue(subAnno);
                List<Integer> lhsValue = getEnhancedRegexValue(superAnno);
                int rhsGroupCount = rhsValue.get(rhsValue.size() - 1);
                int lhsGroupCount = lhsValue.get(lhsValue.size() - 1);
                // remove the group count (the last element in the value array) because it is not
                // exactly a non null group (if it is, it will occur twice in the array).
                rhsValue.remove((Integer) rhsGroupCount);
                lhsValue.remove((Integer) lhsGroupCount);
                if (rhsGroupCount == lhsGroupCount) {
                    // if number of groups is same and rhs is more specific (specifies more non null
                    // groups) then it is a subtype of lhs.
                    return rhsValue.containsAll(lhsValue);
                } else if (rhsGroupCount > lhsGroupCount) {
                    // if rhs specifies more groups than lhs and all the non null groups in rhs are
                    // also non null in lhs then rhs is a subtype of lhs.
                    return rhsValue.containsAll(lhsValue);
                } else {
                    // if lhs specifies more groups than rhs then it is more specific and thus rhs
                    // is not a subtype of lhs.
                    return false;
                }
            } else if (subKind == ENHANCEDREGEX_KIND && superKind == REGEX_KIND) {
                List<Integer> rhsValue = getEnhancedRegexValue(subAnno);
                int rhsGroupCount = rhsValue.get(rhsValue.size() - 1);
                int lhsGroupCount = getRegexValue(superAnno);
                // if the number of groups specified in the EnhancedRegex annotation is not greater
                // than the number of groups present in the Regex annotation then EnhancedRegex is
                // more specific and thus a subtype of Regex.
                return lhsGroupCount <= rhsGroupCount;
            }
            throw new BugInCF("Unexpected qualifiers: %s %s", subAnno, superAnno);
        }

        @Override
        protected AnnotationMirror leastUpperBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind lubKind) {
            if (qualifierKind1 == REGEX_KIND && qualifierKind2 == REGEX_KIND) {
                int value1 = getRegexValue(a1);
                int value2 = getRegexValue(a2);
                if (value1 < value2) {
                    return a1;
                } else {
                    return a2;
                }
            } else if (qualifierKind1 == PARTIALREGEX_KIND && qualifierKind2 == PARTIALREGEX_KIND) {
                if (AnnotationUtils.areSame(a1, a2)) {
                    return a1;
                } else {
                    return UNKNOWNREGEX;
                }
            } else if (qualifierKind1 == ENHANCEDREGEX_KIND
                    && qualifierKind2 == ENHANCEDREGEX_KIND) {
                List<Integer> value1 = getEnhancedRegexValue(a1);
                List<Integer> value2 = getEnhancedRegexValue(a2);
                int groupCount1 = value1.get(value1.size() - 1);
                int groupCount2 = value2.get(value2.size() - 1);
                value1.remove((Integer) groupCount1);
                value2.remove((Integer) groupCount2);
                if (groupCount1 == groupCount2) {
                    // number of groups in both the annotations are same.
                    if (value1.containsAll(value2)) {
                        // every group specified non null in first annotation is specified non null
                        // in the second one. So the second is more general, and thus the upperbound
                        // of the first.
                        return a2;
                    } else if (value2.containsAll(value1)) {
                        return a1;
                    } else {
                        // if both specify some group that is not present in the other, then lub
                        // is Regex.
                        return REGEX;
                    }
                } else if (groupCount1 > groupCount2) {
                    // since first specified more groups than the second, either second is the lub
                    // or regex is the lub.
                    if (value1.containsAll(value2)) {
                        return a2;
                    } else {
                        return REGEX;
                    }
                } else {
                    if (value2.containsAll(value1)) {
                        return a1;
                    } else {
                        return REGEX;
                    }
                }
            } else if (qualifierKind1 == PARTIALREGEX_KIND
                    || qualifierKind1 == REGEX_KIND
                    || qualifierKind1 == ENHANCEDREGEX_KIND) {
                return a1;
            } else if (qualifierKind2 == PARTIALREGEX_KIND
                    || qualifierKind2 == REGEX_KIND
                    || qualifierKind2 == ENHANCEDREGEX_KIND) {
                return a2;
            }
            throw new BugInCF("Unexpected qualifiers: %s %s", a1, a2);
        }

        @Override
        protected AnnotationMirror greatestLowerBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind glbKind) {
            if (qualifierKind1 == REGEX_KIND && qualifierKind2 == REGEX_KIND) {
                int value1 = getRegexValue(a1);
                int value2 = getRegexValue(a2);
                if (value1 > value2) {
                    return a1;
                } else {
                    return a2;
                }
            } else if (qualifierKind1 == PARTIALREGEX_KIND && qualifierKind2 == PARTIALREGEX_KIND) {
                if (AnnotationUtils.areSame(a1, a2)) {
                    return a1;
                } else {
                    return REGEXBOTTOM;
                }
            } else if (qualifierKind1 == ENHANCEDREGEX_KIND
                    && qualifierKind2 == ENHANCEDREGEX_KIND) {
                List<Integer> value1 = getEnhancedRegexValue(a1);
                List<Integer> value2 = getEnhancedRegexValue(a2);
                int groupCount1 = value1.get(value1.size() - 1);
                int groupCount2 = value2.get(value2.size() - 1);
                value1.remove((Integer) groupCount1);
                value2.remove((Integer) groupCount2);
                // Similar reasoning as in lub.
                if (groupCount1 == groupCount2) {
                    if (value1.containsAll(value2)) {
                        return a1;
                    } else if (value2.containsAll(value1)) {
                        return a2;
                    } else {
                        return REGEXBOTTOM;
                    }
                } else if (groupCount1 > groupCount2) {
                    if (value1.containsAll(value2)) {
                        return a1;
                    } else {
                        return REGEXBOTTOM;
                    }
                } else {
                    if (value2.containsAll(value1)) {
                        return a2;
                    } else {
                        return REGEXBOTTOM;
                    }
                }
            } else if (qualifierKind1 == PARTIALREGEX_KIND
                    || qualifierKind1 == REGEX_KIND
                    || qualifierKind1 == ENHANCEDREGEX_KIND) {
                return a1;
            } else if (qualifierKind2 == PARTIALREGEX_KIND
                    || qualifierKind2 == REGEX_KIND
                    || qualifierKind2 == ENHANCEDREGEX_KIND) {
                return a2;
            }
            throw new BugInCF("Unexpected qualifiers: %s %s", a1, a2);
        }

        /** Gets the value out of a regex annotation. */
        private int getRegexValue(AnnotationMirror anno) {
            return (Integer)
                    AnnotationUtils.getElementValuesWithDefaults(anno)
                            .get(regexValueElement)
                            .getValue();
        }

        /**
         * Gets the list of non-null groups out of an enhanced regex annotation.
         *
         * @param anno the annotation to extract the list from
         * @return the extracted list of non-null groups
         */
        private List<Integer> getEnhancedRegexValue(AnnotationMirror anno) {
            return AnnotationUtils.getElementValueArray(anno, "value", Integer.class, true);
        }
    }

    /**
     * Returns the group count value of the given annotation or 0 if there's a problem getting the
     * group count value.
     *
     * @param anno the annotation to extract group count from
     * @return the number of capturing groups as per the annotation
     */
    public int getGroupCount(AnnotationMirror anno) {
        if (anno.getAnnotationType().asElement().getSimpleName().contentEquals("Regex")) {
            AnnotationValue groupCountValue =
                    AnnotationUtils.getElementValuesWithDefaults(anno).get(regexValueElement);
            // If group count value is null then there's no Regex annotation
            // on the parameter so set the group count to 0. This would happen
            // if a non-regex string is passed to Pattern.compile but warnings
            // are suppressed.
            if (groupCountValue != null) {
                Object value = groupCountValue.getValue();
                if (value instanceof Integer) return (Integer) value;
            }
        } else if (anno.getAnnotationType()
                .asElement()
                .getSimpleName()
                .contentEquals("EnhancedRegex")) {
            List<Integer> nonNullGroups =
                    AnnotationUtils.getElementValueArray(anno, "value", Integer.class, false);
            if (nonNullGroups.size() > 1) return nonNullGroups.get(nonNullGroups.size() - 1);
            else throw new BugInCF("Size of the value array is less than 2.");
        }
        return 0;
    }

    /**
     * Returns the list of non-null groups from the EnhancedRegex annotation. Returns the default if
     * the annotation is not present.
     *
     * @param anno the annotation from which the list is to be extracted
     * @return the extracted list of non-null groups
     */
    public List<Integer> getNonNullGroups(AnnotationMirror anno) {
        return AnnotationUtils.getElementValueArray(anno, "value", Integer.class, true);
    }

    /** Returns the number of groups in the given regex String. */
    public static int getGroupCount(@Regex String regexp) {
        return Pattern.compile(regexp).matcher("").groupCount();
    }

    /**
     * Returns the list of groups that are non-null in the given regex String.
     *
     * @param regexp the string to analyse
     * @return a list of non-null groups and the number of groups
     */
    public static List<Integer> getNonNullGroups(@Regex String regexp) {
        // TODO check and ask for missing cases or alternate ways.
        List<Integer> nonNullGroups = new ArrayList<>();
        int n = getGroupCount(regexp);
        for (int i = 0; i <= n; i++) {
            nonNullGroups.add(i);
        }
        ArrayDeque<Integer> openingIndices = new ArrayDeque<>();
        int group = 0;
        boolean squareBracketOpen = false;
        boolean escaped = false;
        boolean notCapturing = false;
        for (int i = 0; i < regexp.length(); i++) {
            if (!escaped && !squareBracketOpen && regexp.charAt(i) == '(') {
                if (i != regexp.length() - 1 && regexp.charAt(i + 1) == '?') {
                    notCapturing = true;
                    continue;
                }
                group += 1;
                if (group > n) throw new BugInCF("Encountered more groups than there actually are");
                if (i != 0 && regexp.charAt(i - 1) == '|') {
                    nonNullGroups.remove((Integer) group);
                }
                openingIndices.push(group);
            } else if (!escaped && !squareBracketOpen && regexp.charAt(i) == ')') {
                if (notCapturing) {
                    notCapturing = false;
                    continue;
                }
                int popped = openingIndices.pop();
                if (i != regexp.length() - 1
                        && "*?|".contains(Character.toString(regexp.charAt(i + 1)))) {
                    nonNullGroups.remove((Integer) popped);
                }
            } else if (!escaped && !squareBracketOpen && regexp.charAt(i) == '[') {
                squareBracketOpen = true;
            } else if (squareBracketOpen && regexp.charAt(i) == ']') {
                squareBracketOpen = false;
            }
            if (!escaped && regexp.charAt(i) == '\\') {
                escaped = true;
            } else if (escaped) {
                escaped = false;
            }
        }
        nonNullGroups.add(n);
        Collections.sort(nonNullGroups);
        return nonNullGroups;
    }

    @Override
    public Set<AnnotationMirror> getWidenedAnnotations(
            Set<AnnotationMirror> annos, TypeKind typeKind, TypeKind widenedTypeKind) {
        return Collections.singleton(UNKNOWNREGEX);
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
                        List<Integer> nonNullGroups = getNonNullGroups(regex);
                        // type.addAnnotation(createRegexAnnotation(groupCount));
                        type.addAnnotation(createEnhancedRegexAnnotation(nonNullGroups));
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
                List<Integer> lNonNullGroups = getMinimumNonNullGroups(lExpr);
                List<Integer> rNonNullGroups = getMinimumNonNullGroups(rExpr);
                boolean lExprRE = lGroupCount != null;
                boolean rExprRE = rGroupCount != null;
                boolean lExprERE = lNonNullGroups != null;
                boolean rExprERE = rNonNullGroups != null;
                boolean lExprPart = lExpr.hasAnnotation(PartialRegex.class);
                boolean rExprPart = rExpr.hasAnnotation(PartialRegex.class);
                boolean lExprPoly = lExpr.hasAnnotation(PolyRegex.class);
                boolean rExprPoly = rExpr.hasAnnotation(PolyRegex.class);

                if (lExprERE && rExprERE) {
                    type.removeAnnotationInHierarchy(ENHANCEDREGEX);
                    List<Integer> concatNonNullGroups = new ArrayList<>();
                    lGroupCount = lNonNullGroups.get(lNonNullGroups.size() - 1);
                    rGroupCount = rNonNullGroups.get(rNonNullGroups.size() - 1);
                    int groupCount = lGroupCount + rGroupCount;
                    concatNonNullGroups.add(0);
                    lNonNullGroups.remove((Integer) 0);
                    rNonNullGroups.remove((Integer) 0);
                    lNonNullGroups.remove(Integer.valueOf(lGroupCount));
                    rNonNullGroups.remove(Integer.valueOf(rGroupCount));
                    concatNonNullGroups.addAll(lNonNullGroups);
                    for (int r : rNonNullGroups) {
                        concatNonNullGroups.add(r + lGroupCount);
                    }
                    concatNonNullGroups.add(groupCount);
                    type.addAnnotation(createEnhancedRegexAnnotation(concatNonNullGroups));
                } else if (lExprRE && rExprRE) {
                    // Remove current @Regex annotation...
                    type.removeAnnotationInHierarchy(REGEX);
                    // ...and add a new one with the correct group count value.
                    type.addAnnotation(createRegexAnnotation(lGroupCount + rGroupCount));
                } else if ((lExprPoly && rExprPoly)
                        || (lExprPoly && rExprRE)
                        || (lExprRE && rExprPoly)
                        || (lExprPoly && rExprERE)
                        || (lExprERE && rExprPoly)) {
                    type.addAnnotation(PolyRegex.class);
                } else if (lExprPart && rExprPart) {
                    String lRegex = getPartialRegexValue(lExpr);
                    String rRegex = getPartialRegexValue(rExpr);
                    String concat = lRegex + rRegex;
                    if (RegexUtil.isRegex(concat)) {
                        List<Integer> nonNullGroups = getNonNullGroups(concat);
                        type.addAnnotation(createEnhancedRegexAnnotation(nonNullGroups));
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
                final List<Integer> lhsNonNullGroups = getMinimumNonNullGroups(lhs);
                final List<Integer> rhsNonNullGroups = getMinimumNonNullGroups(rhs);

                if (lhsNonNullGroups != null && rhsNonNullGroups != null) {
                    type.removeAnnotationInHierarchy(ENHANCEDREGEX);
                    List<Integer> lNonNullGroups =
                            getNonNullGroups(lhs.getAnnotation(EnhancedRegex.class));
                    List<Integer> rNonNullGroups =
                            getNonNullGroups(rhs.getAnnotation(EnhancedRegex.class));
                    List<Integer> concatNonNullGroups = new ArrayList<>();
                    int lGroupCount = lNonNullGroups.get(lNonNullGroups.size() - 1);
                    int rGroupCount = rNonNullGroups.get(rNonNullGroups.size() - 1);
                    int groupCount = lGroupCount + rGroupCount;
                    concatNonNullGroups.add(0);
                    lNonNullGroups.remove((Integer) 0);
                    rNonNullGroups.remove((Integer) 0);
                    lNonNullGroups.remove(Integer.valueOf(lGroupCount));
                    rNonNullGroups.remove(Integer.valueOf(rGroupCount));
                    concatNonNullGroups.addAll(lNonNullGroups);
                    for (int r : rNonNullGroups) {
                        concatNonNullGroups.add(r + lGroupCount);
                    }
                    concatNonNullGroups.add(groupCount);
                    type.addAnnotation(createEnhancedRegexAnnotation(concatNonNullGroups));
                } else if (lhsRegexCount != null && rhsRegexCount != null) {
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
                List<Integer> nonNullGroups = getMinimumNonNullGroups(argType);
                AnnotationMirror bottomAnno =
                        getAnnotatedType(arg0).getAnnotation(RegexBottom.class);

                if (regexCount != null) {
                    // Remove current @Regex annotation...
                    // ...and add a new one with the correct group count value.
                    if (nonNullGroups != null)
                        type.replaceAnnotation(createEnhancedRegexAnnotation(nonNullGroups));
                    else type.replaceAnnotation(createRegexAnnotation(regexCount));
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
            AnnotationMirror primaryRegexAnno = type.getAnnotation(Regex.class);
            if (primaryRegexAnno == null) {
                primaryRegexAnno = type.getAnnotation(EnhancedRegex.class);
                if (primaryRegexAnno == null) {
                    switch (type.getKind()) {
                        case TYPEVAR:
                            return getMinimumRegexCount(
                                    ((AnnotatedTypeVariable) type).getUpperBound());

                        case WILDCARD:
                            return getMinimumRegexCount(
                                    ((AnnotatedWildcardType) type).getExtendsBound());

                        case INTERSECTION:
                            Integer maxBound = null;
                            for (final AnnotatedTypeMirror bound :
                                    ((AnnotatedIntersectionType) type).getBounds()) {
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
                } else {
                    return getGroupCount(primaryRegexAnno);
                }
                return null;
            }

            return getGroupCount(primaryRegexAnno);
        }

        /**
         * Minimum non-null groups from the annotation.
         *
         * @param type the annotation to extract the non-null groups from
         * @return the list of non-null groups
         */
        private List<Integer> getMinimumNonNullGroups(final AnnotatedTypeMirror type) {
            AnnotationMirror anno = type.getAnnotation(EnhancedRegex.class);
            if (anno == null) {
                switch (type.getKind()) {
                    case TYPEVAR:
                        return getMinimumNonNullGroups(
                                ((AnnotatedTypeVariable) type).getUpperBound());

                    case WILDCARD:
                        return getMinimumNonNullGroups(
                                ((AnnotatedWildcardType) type).getExtendsBound());

                    case INTERSECTION:
                        List<Integer> maxNonNull = null;
                        for (final AnnotatedTypeMirror bound :
                                ((AnnotatedIntersectionType) type).getBounds()) {
                            List<Integer> boundNonNullGroups = getMinimumNonNullGroups(bound);
                            if (maxNonNull == null || boundNonNullGroups.containsAll(maxNonNull)) {
                                maxNonNull = boundNonNullGroups;
                            }
                        }
                        return maxNonNull;
                    default:
                }
                return null;
            }
            return AnnotationUtils.getElementValueArray(anno, "value", Integer.class, false);
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
