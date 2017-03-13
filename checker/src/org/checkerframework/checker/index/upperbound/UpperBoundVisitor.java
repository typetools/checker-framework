package org.checkerframework.checker.index.upperbound;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree.Kind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
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

    private void visitAccess(ExpressionTree indexTree, ExpressionTree arrTree) {
        AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(indexTree);
        String arrName = FlowExpressions.internalReprOf(this.atypeFactory, arrTree).toString();

        UBQualifier qualifier = UBQualifier.createUBQualifier(indexType, atypeFactory.UNKNOWN);
        if (qualifier.isLessThanLengthOf(arrName)) {
            return;
        }

        AnnotationMirror sameLenAnno = atypeFactory.sameLenAnnotationFromExpressionTree(arrTree);
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
        Integer valMax = atypeFactory.valMaxFromExpressionTree(indexTree);
        int minLen = atypeFactory.minLenFromExpressionTree(arrTree);
        if (valMax != null && minLen != -1 && valMax < minLen) {
            return;
        }

        checker.report(
                Result.failure(UPPER_BOUND, indexType.toString(), arrName, arrName), indexTree);
    }

    /**
     * Slightly relaxes the usual assignment rules by allowing assignments where the right hand side
     * is a value known at compile time and the type of the left hand side is annotated with
     * LT*LengthOf("a"). If the min length of a is in the correct relationship with the value on the
     * right hand side, then the assignment is legal. Both constant integers and constant arrays of
     * integers are handled.
     */
    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            ExpressionTree valueExp,
            /*@CompilerMessageKey*/ String errorKey) {

        List<? extends ExpressionTree> expressions;
        if (valueExp.getKind() == Kind.NEW_ARRAY) {
            expressions = ((NewArrayTree) valueExp).getInitializers();
        } else {
            expressions = Collections.singletonList(valueExp);
        }

        if (expressions == null || expressions.isEmpty()) {
            super.commonAssignmentCheck(varType, valueExp, errorKey);
            return;
        }

        // Find the appropriate qualifier to try to conform to.
        UBQualifier qualifier;
        if (varType instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
            // The qualifier we need for an array is in the component type, not varType.
            AnnotatedTypeMirror componentType =
                    ((AnnotatedTypeMirror.AnnotatedArrayType) varType).getComponentType();
            qualifier = UBQualifier.createUBQualifier(componentType, atypeFactory.UNKNOWN);
        } else {
            qualifier = UBQualifier.createUBQualifier(varType, atypeFactory.UNKNOWN);
        }

        // If the qualifier is uninteresting or the type is unannotated, do nothing else.
        if (qualifier == null || !qualifier.isLessThanLengthQualifier()) {
            super.commonAssignmentCheck(varType, valueExp, errorKey);
            return;
        }
        LessThanLengthOf ltl = (LessThanLengthOf) qualifier;

        // Either a singleton list of the single integer on the right,
        // or a list containing all the values in a constant array.
        List<Integer> rhsValues = new ArrayList<>();

        boolean allValuesConstant = true;
        // All the values must be compile-time constants to check against the minlen.
        for (ExpressionTree exp : expressions) {
            Integer val = atypeFactory.valMaxFromExpressionTree(exp);
            if (val == null) {
                allValuesConstant = false;
            } else {
                rhsValues.add(val);
            }
        }

        boolean conforms = true;
        // Check whether the expressions are valid for each array listed. Set conforms to true if they aren't.
        for (String arrayName : ltl.getArrays()) {
            // do the samelen check first. Look up all arrays in the SL type and
            // check if it conforms to one. If so, skip this iteration.
            List<String> sameLenArrays =
                    atypeFactory
                            .getSameLenAnnotatedTypeFactory()
                            .getSameLensFromString(arrayName, valueExp, getCurrentPath());

            if (sameLenArrays.size() == 0) {
                if (!allValuesConstant) {
                    conforms = false;
                    break;
                }
            }

            // SameLen checks
            for (ExpressionTree exp : expressions) {
                AnnotatedTypeMirror currentType = atypeFactory.getAnnotatedType(exp);
                UBQualifier currentQual =
                        UBQualifier.createUBQualifier(currentType, atypeFactory.UNKNOWN);
                boolean matchesAny = false;
                if (currentQual.isLessThanLengthQualifier()) {
                    LessThanLengthOf currentLTL = (LessThanLengthOf) currentQual;
                    for (String sameLenArrayName : sameLenArrays) {
                        // Check whether replacing the value for any of the current type's offset results
                        // in the type we're trying to match. If so, set matchesAny to true and break.
                        if (ltl.isValidReplacement(arrayName, sameLenArrayName, currentLTL)) {
                            matchesAny = true;
                            break;
                        }
                    }
                }
                if (!matchesAny && !allValuesConstant) {
                    // If none match and there's no chance for the minlen check to succeed, fail.
                    conforms = false;
                    break;
                }
            }

            // MinLen Checks
            if (allValuesConstant) {
                int minLen =
                        atypeFactory
                                .getMinLenAnnotatedTypeFactory()
                                .getMinLenFromString(arrayName, valueExp, getCurrentPath());
                boolean minLenOk = true;
                for (Integer value : rhsValues) {
                    minLenOk =
                            minLenOk
                                    && ltl.isValuePlusOffsetLessThanMinLen(
                                            arrayName, value, minLen);
                }
                if (!minLenOk) {
                    conforms = false;
                }
            }
        }
        if (!conforms) {
            super.commonAssignmentCheck(varType, valueExp, errorKey);
            return;
        }
    }
}
