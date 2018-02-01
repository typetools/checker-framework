package org.checkerframework.checker.index.upperbound;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree.Kind;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.index.samelen.SameLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.javacutil.AnnotationUtils;

/** Warns about array accesses that could be too high. */
public class UpperBoundVisitor extends BaseTypeVisitor<UpperBoundAnnotatedTypeFactory> {

    private static final String UPPER_BOUND = "array.access.unsafe.high";
    private static final String UPPER_BOUND_CONST = "array.access.unsafe.high.constant";
    private static final String UPPER_BOUND_RANGE = "array.access.unsafe.high.range";

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

    /**
     * Checks if this array access is either using a variable that is less than the length of the
     * array, or using a constant less than the array's minlen. Issues an error if neither is true.
     */
    private void visitAccess(ExpressionTree indexTree, ExpressionTree arrTree) {
        AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(indexTree);
        String arrName = FlowExpressions.internalReprOf(this.atypeFactory, arrTree).toString();

        UBQualifier qualifier = UBQualifier.createUBQualifier(indexType, atypeFactory.UNKNOWN);
        if (qualifier.isLessThanLengthOf(arrName)) {
            return;
        }

        // Find max because it's important to determine whether the index is less than the
        // minimum length of the array. If it could be any of several values, only the max is of
        // interest.
        Long valMax = IndexUtil.getMaxValue(indexTree, atypeFactory.getValueAnnotatedTypeFactory());

        AnnotationMirror sameLenAnno = atypeFactory.sameLenAnnotationFromTree(arrTree);
        // Produce the full list of relevant names by checking the SameLen type.
        if (sameLenAnno != null && AnnotationUtils.areSameByClass(sameLenAnno, SameLen.class)) {
            if (AnnotationUtils.hasElementValue(sameLenAnno, "value")) {
                List<String> slNames =
                        AnnotationUtils.getElementValueArray(
                                sameLenAnno, "value", String.class, true);
                if (qualifier.isLessThanLengthOfAny(slNames)) {
                    return;
                }
                for (String slName : slNames) {
                    // Check if any of the arrays have a minlen that is greater than the
                    // known constant value.
                    int minlenSL =
                            atypeFactory
                                    .getValueAnnotatedTypeFactory()
                                    .getMinLenFromString(slName, arrTree, getCurrentPath());
                    if (valMax != null && valMax < minlenSL) {
                        return;
                    }
                }
            }
        }

        // Check against the minlen of the array itself.
        int minLen = IndexUtil.getMinLen(arrTree, atypeFactory.getValueAnnotatedTypeFactory());
        if (valMax != null && valMax < minLen) {
            return;
        }

        // We can issue three different errors:
        // 1. If the index is a compile-time constant, issue an error that describes the array type.
        // 2. If the index is a compile-time range and has no upperbound qualifier,
        //    issue an error that names the upperbound of the range and the array's type.
        // 3. If neither of the above, issue an error that names the upper bound type.

        if (IndexUtil.getExactValue(indexTree, atypeFactory.getValueAnnotatedTypeFactory())
                != null) {
            // Note that valMax is equal to the exact value in this case.
            checker.report(
                    Result.failure(
                            UPPER_BOUND_CONST,
                            valMax,
                            atypeFactory
                                    .getValueAnnotatedTypeFactory()
                                    .getAnnotatedType(arrTree)
                                    .toString(),
                            valMax + 1,
                            valMax + 1),
                    indexTree);
        } else if (valMax != null && qualifier.isUnknown()) {

            checker.report(
                    Result.failure(
                            UPPER_BOUND_RANGE,
                            atypeFactory
                                    .getValueAnnotatedTypeFactory()
                                    .getAnnotatedType(indexTree)
                                    .toString(),
                            atypeFactory
                                    .getValueAnnotatedTypeFactory()
                                    .getAnnotatedType(arrTree)
                                    .toString(),
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
     * <p>If the varType is an array type and the value express is an array initializer, then the
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
     * Implements the actual check for the relaxed common assignment check. For what is permitted,
     * see {@link #relaxedCommonAssignment}.
     */
    private boolean relaxedCommonAssignmentCheck(
            LessThanLengthOf varLtlQual, ExpressionTree valueExp) {

        AnnotatedTypeMirror expType = atypeFactory.getAnnotatedType(valueExp);
        UBQualifier expQual = UBQualifier.createUBQualifier(expType, atypeFactory.UNKNOWN);

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
