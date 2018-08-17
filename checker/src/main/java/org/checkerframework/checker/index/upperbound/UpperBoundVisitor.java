package org.checkerframework.checker.index.upperbound;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.Subsequence;
import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.samelen.SameLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.ThisReference;
import org.checkerframework.dataflow.analysis.FlowExpressions.ValueLiteral;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/** Warns about array accesses that could be too high. */
public class UpperBoundVisitor extends BaseTypeVisitor<UpperBoundAnnotatedTypeFactory> {

    private static final @CompilerMessageKey String UPPER_BOUND = "array.access.unsafe.high";
    private static final @CompilerMessageKey String UPPER_BOUND_CONST =
            "array.access.unsafe.high.constant";
    private static final @CompilerMessageKey String UPPER_BOUND_RANGE =
            "array.access.unsafe.high.range";
    private static final @CompilerMessageKey String TO_NOT_LTEL = "to.not.ltel";
    private static final @CompilerMessageKey String NOT_FINAL = "not.final";
    private static final @CompilerMessageKey String HSS = "which.subsequence";

    public UpperBoundVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * When the visitor reaches an array access, it needs to check a couple of things. First, it
     * checks if the index has been assigned a reasonable UpperBound type: only an index with type
     * LTLengthOf(arr) is safe to access arr. If that fails, it checks if the access is still safe.
     * To do so, it checks if the Value Checker knows the minimum length of arr by querying the
     * Value Annotated Type Factory. If the minimum length of the array is known, the visitor can
     * check if the index is less than that minimum length. If so, then the access is still safe.
     * Otherwise, report a potential unsafe access.
     */
    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void type) {
        ExpressionTree indexTree = tree.getIndex();
        ExpressionTree arrTree = tree.getExpression();
        visitAccess(indexTree, arrTree);
        return super.visitArrayAccess(tree, type);
    }

    /** Warns about LTLengthOf annotations with arguments whose lengths do not match. */
    @Override
    public Void visitAnnotation(AnnotationTree node, Void p) {
        AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(node);
        if (AnnotationUtils.areSameByClass(anno, LTLengthOf.class)) {
            List<? extends ExpressionTree> args = node.getArguments();
            if (args.size() == 2) {
                // If offsets are provided, there must be the same number of them as there are
                // arrays.
                List<String> sequences =
                        AnnotationUtils.getElementValueArray(anno, "value", String.class, true);
                List<String> offsets =
                        AnnotationUtils.getElementValueArray(anno, "offset", String.class, true);
                if (sequences.size() != offsets.size() && offsets.size() > 0) {
                    checker.report(
                            Result.failure(
                                    "different.length.sequences.offsets",
                                    sequences.size(),
                                    offsets.size()),
                            node);
                    return null;
                }
            }
        } else if (AnnotationUtils.areSameByClass(anno, HasSubsequence.class)) {
            // Check that the arguments to a HasSubsequence annotation are valid flow expressions,
            // and issue an error if one of them is not.

            String seq = AnnotationUtils.getElementValue(anno, "value", String.class, true);
            String from = AnnotationUtils.getElementValue(anno, "from", String.class, true);
            String to = AnnotationUtils.getElementValue(anno, "to", String.class, true);

            // check that each expression is parseable in this context
            ClassTree enclosingClass = TreeUtils.enclosingClass(getCurrentPath());
            FlowExpressionContext context =
                    FlowExpressionContext.buildContextForClassDeclaration(enclosingClass, checker);
            checkEffectivelyFinalAndParsable(seq, context, node);
            checkEffectivelyFinalAndParsable(from, context, node);
            checkEffectivelyFinalAndParsable(to, context, node);
        }
        return super.visitAnnotation(node, p);
    }

    /**
     * Determines if the Java expression named by s is effectively final at the current program
     * location.
     */
    private void checkEffectivelyFinalAndParsable(
            String s, FlowExpressionContext context, Tree error) {
        Receiver rec;
        try {
            rec = FlowExpressionParseUtil.parse(s, context, getCurrentPath(), false);
        } catch (FlowExpressionParseException e) {
            checker.report(e.getResult(), error);
            return;
        }
        Element element = null;
        if (rec instanceof LocalVariable) {
            element = ((LocalVariable) rec).getElement();
        } else if (rec instanceof FieldAccess) {
            element = ((FieldAccess) rec).getField();
        } else if (rec instanceof ThisReference || rec instanceof ValueLiteral) {
            return;
        }
        if (element == null || !ElementUtils.isEffectivelyFinal(element)) {
            checker.report(Result.failure(NOT_FINAL, rec), error);
        }
    }

    /**
     * Checks if this array access is legal. Uses the common assignment check and a simple MinLen
     * check of its own. The MinLen check is needed because the common assignment check always
     * returns false when the upper bound qualifier is @UpperBoundUnknown.
     */
    private void visitAccess(ExpressionTree indexTree, ExpressionTree arrTree) {

        String arrName = FlowExpressions.internalReprOf(this.atypeFactory, arrTree).toString();
        LessThanLengthOf lhsQual = (LessThanLengthOf) UBQualifier.createUBQualifier(arrName, "0");
        if (relaxedCommonAssignmentCheck(lhsQual, indexTree) || checkMinLen(indexTree, arrTree)) {
            return;
        } // else issue errors.

        // We can issue three different errors:
        // 1. If the index is a compile-time constant, issue an error that describes the array type.
        // 2. If the index is a compile-time range and has no upperbound qualifier,
        //    issue an error that names the upperbound of the range and the array's type.
        // 3. If neither of the above, issue an error that names the upper bound type.

        AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(indexTree);
        UBQualifier qualifier = UBQualifier.createUBQualifier(indexType, atypeFactory.UNKNOWN);
        ValueAnnotatedTypeFactory valueFactory = atypeFactory.getValueAnnotatedTypeFactory();
        Long valMax = IndexUtil.getMaxValue(indexTree, valueFactory);

        if (IndexUtil.getExactValue(indexTree, valueFactory) != null) {
            // Note that valMax is equal to the exact value in this case.
            checker.report(
                    Result.failure(
                            UPPER_BOUND_CONST,
                            valMax,
                            valueFactory.getAnnotatedType(arrTree).toString(),
                            valMax + 1,
                            valMax + 1),
                    indexTree);
        } else if (valMax != null && qualifier.isUnknown()) {

            checker.report(
                    Result.failure(
                            UPPER_BOUND_RANGE,
                            valueFactory.getAnnotatedType(indexTree).toString(),
                            valueFactory.getAnnotatedType(arrTree).toString(),
                            arrName,
                            arrName,
                            valMax + 1),
                    indexTree);
        } else {
            checker.report(
                    Result.failure(UPPER_BOUND, indexType.toString(), arrName, arrName, arrName),
                    indexTree);
        }
    }

    @Override
    protected void commonAssignmentCheck(
            Tree varTree, ExpressionTree valueTree, @CompilerMessageKey String errorKey) {

        // check that when an assignment to a variable b declared as @HasSubsequence(a, from, to)
        // occurs, to <= a.length, i.e. to is @LTEqLengthOf(a).

        Subsequence subSeq = Subsequence.getSubsequenceFromTree(varTree, atypeFactory);
        if (subSeq != null) {
            AnnotationMirror anm;
            try {
                anm =
                        atypeFactory.getAnnotationMirrorFromJavaExpressionString(
                                subSeq.to, varTree, getCurrentPath());
            } catch (FlowExpressionParseException e) {
                anm = null;
            }

            boolean ltelCheckFailed = true;
            if (anm != null) {
                UBQualifier qual = UBQualifier.createUBQualifier(anm);
                ltelCheckFailed = !qual.isLessThanOrEqualTo(subSeq.array);
            }

            if (ltelCheckFailed) {
                // issue an error
                checker.report(
                        Result.failure(
                                TO_NOT_LTEL,
                                subSeq.to,
                                subSeq.array,
                                anm == null ? "@UpperBoundUnknown" : anm,
                                subSeq.array,
                                subSeq.array,
                                subSeq.array),
                        valueTree);
            } else {
                checker.report(
                        Result.warning(
                                HSS,
                                subSeq.array,
                                subSeq.from,
                                subSeq.from,
                                subSeq.to,
                                subSeq.to,
                                subSeq.array,
                                subSeq.array),
                        valueTree);
            }
        }

        super.commonAssignmentCheck(varTree, valueTree, errorKey);
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            ExpressionTree valueTree,
            @CompilerMessageKey String errorKey) {
        if (!relaxedCommonAssignment(varType, valueTree)) {
            super.commonAssignmentCheck(varType, valueTree, errorKey);
        } else if (checker.hasOption("showchecks")) {
            // Print the success message because super isn't called.
            long valuePos = positions.getStartPosition(root, valueTree);
            AnnotatedTypeMirror valueType = atypeFactory.getAnnotatedType(valueTree);
            System.out.printf(
                    " %s (line %3d): %s %s%n     actual: %s %s%n   expected: %s %s%n",
                    "success: actual is subtype of expected",
                    (root.getLineMap() != null ? root.getLineMap().getLineNumber(valuePos) : -1),
                    valueTree.getKind(),
                    valueTree,
                    valueType.getKind(),
                    valueType.toString(),
                    varType.getKind(),
                    varType.toString());
        }
    }

    /**
     * Returns whether the assignment is legal based on the relaxed assignment rules.
     *
     * <p>The relaxed assignment rules is the following: Assuming the varType (left-hand side) is
     * less than the length of some array given some offset
     *
     * <p>1. If both the offset and the value expression (rhs) are ints known at compile time, and
     * if the min length of the array is greater than offset + value, then the assignment is legal.
     * (This method returns true.)
     *
     * <p>2. If the value expression (rhs) is less than the length of an array that is the same
     * length as the array in the varType, and if the offsets are equal, then the assignment is
     * legal. (This method returns true.)
     *
     * <p>3. Otherwise the assignment is only legal if the usual assignment rules are true, so this
     * method returns false.
     *
     * <p>If the varType is less than the length of multiple arrays, then the this method only
     * returns true if the relaxed rules above apply for each array.
     *
     * <p>If the varType is an array type and the value expression is an array initializer, then the
     * above rules are applied for expression in the initializer where the varType is the component
     * type of the array.
     */
    private boolean relaxedCommonAssignment(AnnotatedTypeMirror varType, ExpressionTree valueExp) {
        List<? extends ExpressionTree> expressions;
        if (valueExp.getKind() == Kind.NEW_ARRAY && varType.getKind() == TypeKind.ARRAY) {
            expressions = ((NewArrayTree) valueExp).getInitializers();
            if (expressions == null || expressions.isEmpty()) {
                return false;
            }
            // The qualifier we need for an array is in the component type, not varType.
            AnnotatedTypeMirror componentType = ((AnnotatedArrayType) varType).getComponentType();
            UBQualifier qualifier =
                    UBQualifier.createUBQualifier(componentType, atypeFactory.UNKNOWN);
            if (!qualifier.isLessThanLengthQualifier()) {
                return false;
            }
            for (ExpressionTree expressionTree : expressions) {
                if (!relaxedCommonAssignmentCheck((LessThanLengthOf) qualifier, expressionTree)) {
                    return false;
                }
            }
            return true;
        }

        UBQualifier qualifier = UBQualifier.createUBQualifier(varType, atypeFactory.UNKNOWN);
        return qualifier.isLessThanLengthQualifier()
                && relaxedCommonAssignmentCheck((LessThanLengthOf) qualifier, valueExp);
    }

    /**
     * Fetches a receiver from a String using the passed type factory. Returns null if there is a
     * parse exception. This wraps GenericAnnotatedTypeFactory#getReceiverFromJavaExpressionString.
     */
    static Receiver getReceiverFromJavaExpressionString(
            String s, UpperBoundAnnotatedTypeFactory atypeFactory, TreePath currentPath) {
        Receiver rec;
        try {
            rec = atypeFactory.getReceiverFromJavaExpressionString(s, currentPath);
        } catch (FlowExpressionParseException e) {
            rec = null;
        }
        return rec;
    }

    /**
     * Given a Java expression, returns the additive inverse, as a String. Assumes that
     * FlowExpressions do not contain multiplication.
     */
    private String negateString(String s, FlowExpressionContext context) {
        return Subsequence.negateString(s, getCurrentPath(), context);
    }

    /*
     *  Queries the Value Checker to determine if the maximum possible value of indexTree
     *  is less than the minimum possible length of arrTree, and returns true if so.
     */
    private boolean checkMinLen(ExpressionTree indexTree, ExpressionTree arrTree) {
        int minLen = IndexUtil.getMinLen(arrTree, atypeFactory.getValueAnnotatedTypeFactory());
        Long valMax = IndexUtil.getMaxValue(indexTree, atypeFactory.getValueAnnotatedTypeFactory());
        if (valMax != null && valMax < minLen) {
            return true;
        }
        return false;
    }

    /**
     * Implements the actual check for the relaxed common assignment check. For what is permitted,
     * see {@link #relaxedCommonAssignment}.
     */
    private boolean relaxedCommonAssignmentCheck(
            LessThanLengthOf varLtlQual, ExpressionTree valueExp) {

        AnnotatedTypeMirror expType = atypeFactory.getAnnotatedType(valueExp);
        UBQualifier expQual = UBQualifier.createUBQualifier(expType, atypeFactory.UNKNOWN);

        UBQualifier lessThanQual = atypeFactory.fromLessThan(valueExp, getCurrentPath());
        if (lessThanQual != null) {
            expQual = expQual.glb(lessThanQual);
        }

        UBQualifier lessThanOrEqualQual =
                atypeFactory.fromLessThanOrEqual(valueExp, getCurrentPath());
        if (lessThanOrEqualQual != null) {
            expQual = expQual.glb(lessThanOrEqualQual);
        }
        if (expQual.isSubtype(varLtlQual)) {
            return true;
        }

        // Take advantage of information available on a HasSubsequence(a, from, to) annotation
        // on the lhs qualifier (varLtlQual):
        // this allows us to show that iff varLtlQual includes LTL(b),
        // b has HSS, and expQual includes LTL(a, -from), then the LTL(b) can be removed from
        // varLtlQual.

        UBQualifier newLHS = processSubsequenceForLHS(varLtlQual, expQual);
        if (newLHS.isUnknown()) {
            return true;
        } else {
            varLtlQual = (LessThanLengthOf) newLHS;
        }

        Long value = IndexUtil.getMaxValue(valueExp, atypeFactory.getValueAnnotatedTypeFactory());

        if (value == null && !expQual.isLessThanLengthQualifier()) {
            return false;
        }

        SameLenAnnotatedTypeFactory sameLenFactory = atypeFactory.getSameLenAnnotatedTypeFactory();
        ValueAnnotatedTypeFactory valueAnnotatedTypeFactory =
                atypeFactory.getValueAnnotatedTypeFactory();
        checkloop:
        for (String sequenceName : varLtlQual.getSequences()) {

            List<String> sameLenSequences =
                    sameLenFactory.getSameLensFromString(sequenceName, valueExp, getCurrentPath());
            if (testSameLen(expQual, varLtlQual, sameLenSequences, sequenceName)) {
                continue;
            }

            int minlen =
                    valueAnnotatedTypeFactory.getMinLenFromString(
                            sequenceName, valueExp, getCurrentPath());
            if (testMinLen(value, minlen, sequenceName, varLtlQual)) {
                continue;
            }
            for (String sequence : sameLenSequences) {
                int minlenSL =
                        valueAnnotatedTypeFactory.getMinLenFromString(
                                sequence, valueExp, getCurrentPath());
                if (testMinLen(value, minlenSL, sequenceName, varLtlQual)) {
                    continue checkloop;
                }
            }

            return false;
        }

        return true;
    }

    /* Returns the new value of the left hand side after processing the arrays named in the lhs.
     * Iff varLtlQual includes LTL(lhsSeq),
     * lhsSeq has HSS, and expQual includes LTL(a, -from), then the LTL(lhsSeq) will be removed from varLtlQual
     */
    private UBQualifier processSubsequenceForLHS(LessThanLengthOf varLtlQual, UBQualifier expQual) {
        UBQualifier newLHS = varLtlQual;
        for (String lhsSeq : varLtlQual.getSequences()) {
            // check is lhsSeq is an actual LTL
            if (varLtlQual.hasSequenceWithOffset(lhsSeq, 0)) {

                Receiver rec =
                        getReceiverFromJavaExpressionString(lhsSeq, atypeFactory, getCurrentPath());
                FlowExpressionContext context = Subsequence.getContextFromReceiver(rec, checker);
                Subsequence subSeq =
                        Subsequence.getSubsequenceFromReceiver(
                                rec, atypeFactory, getCurrentPath(), context);

                if (subSeq != null) {
                    String from = subSeq.from;
                    String a = subSeq.array;

                    if (expQual.hasSequenceWithOffset(a, negateString(from, context))) {
                        // This cast is safe because LTLs cannot contain duplicates.
                        // Note that this updates newLHS on each iteration from its old value,
                        // so even if there are multiple HSS arrays the result will be correct.
                        newLHS = ((LessThanLengthOf) newLHS).removeOffset(lhsSeq, 0);
                    }
                }
            }
        }
        return newLHS;
    }

    /**
     * Tests whether replacing any of the arrays in sameLenArrays with arrayName makes expQual
     * equivalent to varQual.
     */
    private boolean testSameLen(
            UBQualifier expQual,
            LessThanLengthOf varQual,
            List<String> sameLenArrays,
            String arrayName) {

        if (!expQual.isLessThanLengthQualifier()) {
            return false;
        }

        for (String sameLenArrayName : sameLenArrays) {
            // Check whether replacing the value for any of the current type's offset results
            // in the type we're trying to match.
            if (varQual.isValidReplacement(
                    arrayName, sameLenArrayName, (LessThanLengthOf) expQual)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests a constant value (value) against the minlen (minlens) of an array (arrayName) with a
     * qualifier (varQual).
     */
    private boolean testMinLen(Long value, int minLen, String arrayName, LessThanLengthOf varQual) {
        if (value == null) {
            return false;
        }
        return varQual.isValuePlusOffsetLessThanMinLen(arrayName, value, minLen);
    }
}
