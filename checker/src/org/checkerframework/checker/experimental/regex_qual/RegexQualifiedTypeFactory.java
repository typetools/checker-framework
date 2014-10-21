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
import org.checkerframework.qualframework.util.ExtendedTypeMirror;
import org.checkerframework.qualframework.util.QualifierContext;

import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
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
//        partialRegexValue = TreeUtils.getMethod("org.checkerframework.checker.regex.qual.PartialRegex", "value", 0, this.context.getProcessingEnvironment());
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
    public QualifiedTypeMirror<Regex> postTypeVarSubstitution(QualifiedParameterDeclaration<Regex> varDecl,
            QualifiedTypeVariable<Regex> varUse, QualifiedTypeMirror<Regex> value) {
        if (varUse.getQualifier() == Regex.TOP) {
            return value;
        } else {
            return super.postTypeVarSubstitution(varDecl, varUse, value);
        }
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
                    Regex regexQual = null;
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

//            @Override
//            public QualifiedTypeMirror<Regex> visitCompoundAssignment(CompoundAssignmentTree tree,
//                    ExtendedTypeMirror type) {
//
//                QualifiedTypeMirror<Regex> result = super.visitCompoundAssignment(tree, type);
//
//                if (TreeUtils.isStringCompoundConcatenation(tree)) {
//                    QualifiedTypeMirror<Regex> lExpr = getQualifiedType(tree.getVariable());
//                    QualifiedTypeMirror<Regex> rExpr = getQualifiedType(tree.getExpression());
//
//                }
//
//
//                return result;
//            }

//            /**
//             * Case 2: Also handle compound String concatenation.
//             */
//            @Override
//            public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
//                if (TreeUtils.isStringCompoundConcatenation(node)) {
//                    AnnotatedTypeMirror rhs = getAnnotatedType(node.getExpression());
//                    AnnotatedTypeMirror lhs = getAnnotatedType(node.getVariable());
//                    if (lhs.hasAnnotation(org.checkerframework.checker.regex.qual.Regex.class) && rhs.hasAnnotation(org.checkerframework.checker.regex.qual.Regex.class)) {
//                        int lCount = getGroupCount(lhs.getAnnotation(org.checkerframework.checker.regex.qual.Regex.class));
//                        int rCount = getGroupCount(rhs.getAnnotation(org.checkerframework.checker.regex.qual.Regex.class));
//                        type.removeAnnotationInHierarchy(REGEX);
//                        type.addAnnotation(createRegexAnnotation(lCount + rCount));
//                    }
//                }
//                return null; // super.visitCompoundAssignment(node, type);
//            }

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

                if (TreeUtils.isStringConcatenation(tree)) {
                    QualifiedTypeMirror<Regex> lExpr = getQualifiedType(tree.getLeftOperand());
                    QualifiedTypeMirror<Regex> rExpr = getQualifiedType(tree.getRightOperand());

                    Regex lRegex = lExpr.getQualifier();
                    Regex rRegex = rExpr.getQualifier();

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
}
