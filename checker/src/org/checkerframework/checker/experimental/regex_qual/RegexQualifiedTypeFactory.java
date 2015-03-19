package org.checkerframework.checker.experimental.regex_qual;

import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.qualframework.base.AnnotationConverter;
import org.checkerframework.qualframework.base.DefaultQualifiedTypeFactory;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedWildcardType;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
import org.checkerframework.qualframework.base.TreeAnnotator;
import org.checkerframework.qualframework.base.dataflow.QualAnalysis;
import org.checkerframework.qualframework.base.dataflow.QualTransfer;
import org.checkerframework.qualframework.base.dataflow.QualValue;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;
import org.checkerframework.qualframework.util.QualifierContext;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * The QualifiedTypeFactory for the Regex-Qual type system.
 *
 * Note: Polymorphic qualifiers are not supported automatically by the qual system.
 * Instead, only the most basic and required polymorphic methods are manually
 * supported by visitMethodInvocation.
 *
 * see org.checkerframework.checker.regex.RegexAnnotatedTypeFactory
 *
 */
public class RegexQualifiedTypeFactory extends DefaultQualifiedTypeFactory<Regex> {

    /**
     * The Pattern.compile method.
     *
     * @see java.util.regex.Pattern#compile(String)
     */
    private final ExecutableElement patternCompile;
    /**
     * The Pattern.matcher method.
     *
     * @see java.util.regex.Pattern#matcher(CharSequence)
     */
    private final ExecutableElement patternMatcher;

    public RegexQualifiedTypeFactory(QualifierContext<Regex> checker) {
        super(checker);

        patternCompile = TreeUtils.getMethod("java.util.regex.Pattern", "compile", 1, getContext().getProcessingEnvironment());
        patternMatcher = TreeUtils.getMethod("java.util.regex.Pattern", "matcher", 1, getContext().getProcessingEnvironment());
    }

    @Override
    protected QualifierHierarchy<Regex> createQualifierHierarchy() {
        return new RegexQualifierHierarchy();
    }

    @Override
    protected AnnotationConverter<Regex> createAnnotationConverter() {
        return new RegexAnnotationConverter();
    }

    @Override
    protected TreeAnnotator<Regex> createTreeAnnotator() {
        return new TreeAnnotator<Regex>() {

            /**
             * Create a Regex qualifier based on the contents of string and char literals.
             * Null literals are Regex.BOTTOM.
             */
            @Override
            public QualifiedTypeMirror<Regex> visitLiteral(LiteralTree tree, ExtendedTypeMirror type) {
                QualifiedTypeMirror<Regex> result = super.visitLiteral(tree, type);

                if (tree.getKind() == Tree.Kind.NULL_LITERAL) {
                    return SetQualifierVisitor.apply(result, Regex.BOTTOM);
                }

                String regexStr = null;
                if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    regexStr = (String) tree.getValue();
                } else if (tree.getKind() == Tree.Kind.CHAR_LITERAL) {
                    regexStr = Character.toString((Character) tree.getValue());
                }

                if (regexStr != null) {
                    Regex regexQual;
                    if (isRegex(regexStr)) {
                        int groupCount = getGroupCount(regexStr);
                        regexQual = new Regex.RegexVal(groupCount);
                    } else {
                        regexQual = new Regex.PartialRegex(regexStr);
                    }
                    result = SetQualifierVisitor.apply(result, regexQual);
                }

                return result;
            }

            /**
             * Handle string compound assignment.
             */
            @Override
            public QualifiedTypeMirror<Regex> visitCompoundAssignment(CompoundAssignmentTree tree,
                    ExtendedTypeMirror type) {

                QualifiedTypeMirror<Regex> result = super.visitCompoundAssignment(tree, type);
                Regex lRegex = getEffectiveQualifier(getQualifiedType(tree.getExpression()));
                Regex rRegex = getEffectiveQualifier(getQualifiedType(tree.getVariable()));

                return handleBinaryOperation(tree, lRegex, rRegex, result);
            }

            /**
             * Add polymorphism to the Pattern.compile and Pattern.matcher methods.
             */
            @Override
            public QualifiedTypeMirror<Regex> visitMethodInvocation(MethodInvocationTree tree, ExtendedTypeMirror type) {

                // TODO: Also get this to work with 2 argument Pattern.compile.
                QualifiedTypeMirror<Regex> result = super.visitMethodInvocation(tree, type);

                if (TreeUtils.isMethodInvocation(tree, patternCompile,
                        getContext().getProcessingEnvironment())) {

                    ExpressionTree arg0 = tree.getArguments().get(0);
                    Regex qual = getEffectiveQualifier(getQualifiedType(arg0));
                    result = SetQualifierVisitor.apply(result, qual);
                } else if (TreeUtils.isMethodInvocation(tree, patternMatcher,
                        getContext().getProcessingEnvironment())) {

                    Regex qual = getEffectiveQualifier(getReceiverType(tree));
                    result = SetQualifierVisitor.apply(result, qual);
                }
                return result;
            }

            /**
             * Handle concatenation of Regex or PolyRegex String/char literals.
             * Also handles concatenation of partial regular expressions.
             */
            @Override
            public QualifiedTypeMirror<Regex> visitBinary(BinaryTree tree, ExtendedTypeMirror type) {

                QualifiedTypeMirror<Regex> result = super.visitBinary(tree, type);
                Regex lRegex = getEffectiveQualifier(getQualifiedType(tree.getLeftOperand()));
                Regex rRegex = getEffectiveQualifier(getQualifiedType(tree.getRightOperand()));

                return handleBinaryOperation(tree, lRegex, rRegex, result);
            }

            /**
             * Returns the QualifiedTypeMirror that is the result of the binary operation represented by tree.
             * Handles concatenation of Regex and PolyRegex qualifiers.
             *
             * @param tree A BinaryTree or a CompoundAssignmentTree
             * @param lRegex The qualifier of the left hand side of the expression
             * @param rRegex The qualifier of the right hand side of the expression
             * @param result The current QualifiedTypeMirror result
             * @return result if operation is not a string concatenation or compound assignment. Otherwise
             *          a copy of result with the new qualifier applied is returned.
             */
            private QualifiedTypeMirror<Regex> handleBinaryOperation(Tree tree, Regex lRegex,
                    Regex rRegex, QualifiedTypeMirror<Regex> result) {
                if (TreeUtils.isStringConcatenation(tree)
                        || (tree instanceof CompoundAssignmentTree
                            && TreeUtils.isStringCompoundConcatenation((CompoundAssignmentTree)tree))) {

                    Regex regex = null;
                    if (lRegex.isRegexVal() && rRegex.isRegexVal()) {
                        int resultCount = ((Regex.RegexVal) lRegex).getCount() + ((Regex.RegexVal) rRegex).getCount();
                        regex = new Regex.RegexVal(resultCount);

                    } else if (lRegex.isPartialRegex() && rRegex.isPartialRegex()) {
                        String concat = ((Regex.PartialRegex) lRegex).getPartialValue() + ((Regex.PartialRegex) rRegex).getPartialValue();
                        if (isRegex(concat)) {
                            int groupCount = getGroupCount(concat);
                            regex = new Regex.RegexVal(groupCount);
                        } else {
                            regex = new Regex.PartialRegex(concat);
                        }

                    } else if (lRegex.isRegexVal() && rRegex.isPartialRegex()) {
                        String concat = "e" + ((Regex.PartialRegex) rRegex).getPartialValue();
                        regex = new Regex.PartialRegex(concat);
                    } else if (rRegex.isRegexVal() && lRegex.isPartialRegex()) {
                        String concat = ((Regex.PartialRegex) lRegex).getPartialValue() + "e";
                        regex = new Regex.PartialRegex(concat);
                    }

                    if (regex != null) {
                        result = SetQualifierVisitor.apply(result, regex);
                    }
                }
                return result;
            }

        };
    }

    /**
     * Returns the number of groups in the given regex String.
     */
    public static int getGroupCount(
            /*@org.checkerframework.checker.regex.qual.Regex*/ String regex) {

        return Pattern.compile(regex).matcher("").groupCount();
    }

    /** This method is a copy of RegexUtil.isValidRegex.
     * We cannot directly use RegexUtil, because it uses type annotations
     * which cannot be used in IDEs (yet).
     */
    /*@SuppressWarnings("purity")*/ // the checker cannot prove that the method is pure, but it is
    /*@org.checkerframework.dataflow.qual.Pure*/
    private static boolean isRegex(String s) {
        try {
            Pattern.compile(s);
        } catch (PatternSyntaxException e) {
            return false;
        }
        return true;
    }

    /**
     * Configure dataflow to use the RegexQualifiedTransfer.
     */
    @Override
    public QualAnalysis<Regex> createFlowAnalysis(List<Pair<VariableElement, QualValue<Regex>>> fieldValues) {
        return new QualAnalysis<Regex>(this.getContext()) {
            @Override
            public QualTransfer<Regex> createTransferFunction() {
                return new RegexQualifiedTransfer(this);
            }
        };
    }

    public Regex getEffectiveQualifier(QualifiedTypeMirror<Regex> mirror) {
        switch (mirror.getKind()) {
            case TYPEVAR:
                return this.getQualifiedTypeParameterBounds(
                        ((QualifiedTypeVariable<Regex>) mirror).
                                getDeclaration().getUnderlyingType()).getUpperBound().getQualifier();
            case WILDCARD:
                return ((QualifiedWildcardType<Regex>)mirror).getExtendsBound().getQualifier();

            default:
                return mirror.getQualifier();
        }
    }
}
