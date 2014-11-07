package org.checkerframework.checker.experimental.regex_qual;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import org.checkerframework.checker.experimental.tainting_qual.Tainting;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.qualframework.base.AnnotationConverter;
import org.checkerframework.qualframework.base.Checker;
import org.checkerframework.qualframework.base.DefaultQualifiedTypeFactory;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedParameterDeclaration;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
import org.checkerframework.qualframework.base.TreeAnnotator;
import org.checkerframework.qualframework.base.dataflow.QualAnalysis;
import org.checkerframework.qualframework.base.dataflow.QualTransfer;
import org.checkerframework.qualframework.base.dataflow.QualValue;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;
import org.checkerframework.qualframework.util.QualifierContext;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by mcarthur on 6/3/14.
 */
public class RegexQualifiedTypeFactory extends DefaultQualifiedTypeFactory<Regex> {

    /**
     * The Pattern.compile method.
     *
     * @see java.util.regex.Pattern#compile(String)
     */
    private final ExecutableElement patternCompile;

    /**
//     * The value method of the PartialRegex qualifier.
//     *
//     * @see org.checkerframework.checker.regex.qual.PartialRegex
//     */
//    private final ExecutableElement partialRegexValue;

    public RegexQualifiedTypeFactory(QualifierContext<Regex> checker) {
        super(checker);

        patternCompile = TreeUtils.getMethod("java.util.regex.Pattern", "compile", 1, getContext().getProcessingEnvironment());
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
            @Override
            public QualifiedTypeMirror<Regex> visitLiteral(LiteralTree tree, ExtendedTypeMirror type) {
                QualifiedTypeMirror<Regex> result = super.visitLiteral(tree, type);

                String regexStr = null;
                if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    regexStr = (String) tree.getValue();
                } else if (tree.getKind() == Tree.Kind.CHAR_LITERAL) {
                    regexStr = Character.toString((Character) tree.getValue());
                } else if (tree.getKind() == Kind.NULL_LITERAL) {
                    return SetQualifierVisitor.apply(result, Regex.BOTTOM);
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

            @Override
            public QualifiedTypeMirror<Regex> visitCompoundAssignment(CompoundAssignmentTree tree,
                    ExtendedTypeMirror type) {

                QualifiedTypeMirror<Regex> result = super.visitCompoundAssignment(tree, type);
                Regex lRegex = getQualifiedType(tree.getExpression()).getQualifier();
                Regex rRegex = getQualifiedType(tree.getVariable()).getQualifier();

                return handleBinaryOperation(tree, lRegex, rRegex, result);
            }

            @Override
            public QualifiedTypeMirror<Regex> visitMethodInvocation(MethodInvocationTree tree, ExtendedTypeMirror type) {
                // TODO: Also get this to work with 2 argument Pattern.compile.

                QualifiedTypeMirror<Regex> result = super.visitMethodInvocation(tree, type);

                if (TreeUtils.isMethodInvocation(tree, patternCompile,
                        getContext().getProcessingEnvironment())) {

                    ExpressionTree arg0 = tree.getArguments().get(0);
                    Regex qual = getQualifiedType(arg0).getQualifier();
                    result = SetQualifierVisitor.apply(result, qual);
                }
                return result;
            }

            /**
             * Case 2: concatenation of Regex or PolyRegex String/char literals.
             * Also handles concatenation of partial regular expressions.
             */
            @Override
            public QualifiedTypeMirror<Regex> visitBinary(BinaryTree tree, ExtendedTypeMirror type) {

                QualifiedTypeMirror<Regex> result = super.visitBinary(tree, type);
                Regex lRegex = getQualifiedType(tree.getLeftOperand()).getQualifier();
                Regex rRegex = getQualifiedType(tree.getRightOperand()).getQualifier();

                return handleBinaryOperation(tree, lRegex, rRegex, result);
            }

            private QualifiedTypeMirror<Regex> handleBinaryOperation(Tree tree, Regex lRegex,
                    Regex rRegex, QualifiedTypeMirror<Regex> result) {
                if (TreeUtils.isStringConcatenation(tree)
                        || (tree instanceof CompoundAssignmentTree
                            && TreeUtils.isStringCompoundConcatenation((CompoundAssignmentTree)tree))) {

                    Regex regex = null;
                    if (lRegex instanceof Regex.RegexVal && rRegex instanceof Regex.RegexVal) {
                        int resultCount = ((Regex.RegexVal) lRegex).getCount() + ((Regex.RegexVal) rRegex).getCount();
                        regex = new Regex.RegexVal(resultCount);
                    } else if (lRegex instanceof Regex.PartialRegex && rRegex instanceof Regex.PartialRegex) {
                        String concat = ((Regex.PartialRegex) lRegex).getPartialValue() + ((Regex.PartialRegex) rRegex).getPartialValue();
                        if (isRegex(concat)) {
                            int groupCount = getGroupCount(concat);
                            regex = new Regex.RegexVal(groupCount);
                        } else {
                            regex = new Regex.PartialRegex(concat);
                        }
                    } else if (lRegex instanceof Regex.RegexVal && rRegex instanceof Regex.PartialRegex) {
                        String concat = "e" + ((Regex.PartialRegex) rRegex).getPartialValue();
                        regex = new Regex.PartialRegex(concat);
                    } else if (rRegex instanceof Regex.RegexVal && lRegex instanceof Regex.PartialRegex ) {
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
            /*@org.checkerframework.checker.experimental.regex_qual.qual.Regex*/ String regex) {

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

    @Override
    public QualAnalysis<Regex> createFlowAnalysis(List<Pair<VariableElement, QualValue<Regex>>> fieldValues) {
        return new QualAnalysis<Regex>(this.getContext()) {
            @Override
            public QualTransfer<Regex> createTransferFunction() {
                return new RegexQualTransfer(this);
            }
        };
    }
}
