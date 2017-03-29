package org.checkerframework.checker.index.upperbound;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree.Kind;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.minlen.MinLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.index.samelen.SameLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.javacutil.AnnotationUtils;

/** Warns about array accesses that could be too high. */
public class UpperBoundVisitor extends BaseTypeVisitor<UpperBoundAnnotatedTypeFactory> {

    private static final String UPPER_BOUND = "array.access.unsafe.high";

    public UpperBoundVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * When the visitor reaches an array access, it needs to check a couple of things. First, it
     * checks if the index has been assigned a reasonable UpperBound type: only an index with type
     * LTLengthOf(arr) is safe to access arr. If that fails, it checks if the access is still safe.
     * To do so, it checks if the MinLen checker knows the minimum length of arr by querying the
     * MinLenATF. If the MinLen of the array is known, the visitor can check if the index is less
     * than the MinLen, using the Value Checker. If so then the access is still safe. Otherwise,
     * report a potential unsafe access.
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
            }
        }

        // Find max because it's important to determine whether the index is less than the
        // minimum length of the array. If it could be any of several values, only the max is of
        // interest.
        Long valMax = IndexUtil.getMaxValue(indexTree, atypeFactory.getValueAnnotatedTypeFactory());
        int minLen = IndexUtil.getMinLen(arrTree, atypeFactory.getMinLenAnnotatedTypeFactory());
        if (valMax != null && minLen != -1 && valMax < minLen) {
            return;
        }

        checker.report(
                Result.failure(UPPER_BOUND, indexType.toString(), arrName, arrName), indexTree);
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            ExpressionTree valueExp,
            /*@CompilerMessageKey*/ String errorKey) {
        if (!relaxedCommonAssignment(varType, valueExp)) {
            super.commonAssignmentCheck(varType, valueExp, errorKey);
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
        MinLenAnnotatedTypeFactory minLenFactory = atypeFactory.getMinLenAnnotatedTypeFactory();
        for (String arrayName : varLtlQual.getArrays()) {

            List<String> sameLenArrays =
                    sameLenFactory.getSameLensFromString(arrayName, valueExp, getCurrentPath());
            if (testSameLen(expQual, varLtlQual, sameLenArrays, arrayName)) {
                continue;
            }

            int minLen = minLenFactory.getMinLenFromString(arrayName, valueExp, getCurrentPath());
            if (testMinLen(value, minLen, arrayName, varLtlQual)) {
                continue;
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
     * Tests a constant value (value) against the minlen (minlen) of an array (arrayName) with a
     * qualifier (varQual).
     */
    private boolean testMinLen(Long value, int minLen, String arrayName, LessThanLengthOf varQual) {
        if (value == null) {
            return false;
        }
        return varQual.isValuePlusOffsetLessThanMinLen(arrayName, value.intValue(), minLen);
    }
}
