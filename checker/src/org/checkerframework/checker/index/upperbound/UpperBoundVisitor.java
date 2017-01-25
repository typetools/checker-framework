package org.checkerframework.checker.index.upperbound;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import java.util.HashSet;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LTOMLengthOf;
import org.checkerframework.checker.index.qual.SameLen;
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
     * When the visitor reachs an array access, it needs to check a couple of things. First, it
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
        String arrName = FlowExpressions.internalReprOf(this.atypeFactory, arrTree).toString();
        AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(indexTree);

        AnnotationMirror sameLenAnno = atypeFactory.sameLenAnnotationFromExpressionTree(arrTree);
        // Is indexType LTL/LTOM of a set containing arrName?
        if (indexTypeContainsArray(arrName, indexType, sameLenAnno)) {
            // If so, this is safe - get out of here.
            return;
        }

        // Find max because it's important to determine whether the index is less than the
        // minimum length of the array. If it could be any of several values, only the max is of
        // interest.
        Integer valMax = atypeFactory.valMaxFromExpressionTree(indexTree);
        int minLen = atypeFactory.minLenFromExpressionTree(arrTree);
        if (valMax != null && minLen != -1 && valMax < minLen) {
            return;
        }

        // Unsafe, since neither the Upper bound or MinLen checks succeeded.
        checker.report(
                Result.failure(UPPER_BOUND, indexType.toString(), arrName, arrName), indexTree);
    }

    /**
     * Determines if the given string is a member of the LTL or LTOM annotation attached to ubType.
     * Requires a SameLen annotation as well, so that it can compare the set of SameLen annotations
     * attached to the array/list to the passed string.
     */
    private boolean indexTypeContainsArray(
            String array, AnnotatedTypeMirror ubType, AnnotationMirror sameLenAnno) {

        String[] arrayNamesFromUBAnno;
        if (ubType.hasAnnotation(LTLengthOf.class)) {
            arrayNamesFromUBAnno =
                    UpperBoundAnnotatedTypeFactory.getValue(ubType.getAnnotation(LTLengthOf.class));
        } else if (ubType.hasAnnotation(LTOMLengthOf.class)) {
            arrayNamesFromUBAnno =
                    UpperBoundAnnotatedTypeFactory.getValue(
                            ubType.getAnnotation(LTOMLengthOf.class));
        } else {
            return false;
        }

        HashSet<String> arrays = new HashSet<>();
        arrays.add(array);

        // Produce the full list of relevant names by checking the SameLen type.
        if (sameLenAnno != null && AnnotationUtils.areSameByClass(sameLenAnno, SameLen.class)) {
            if (AnnotationUtils.hasElementValue(sameLenAnno, "value")) {
                List<String> slNames =
                        AnnotationUtils.getElementValueArray(
                                sameLenAnno, "value", String.class, true);
                arrays.addAll(slNames);
            }
        }

        for (String st : arrayNamesFromUBAnno) {
            if (arrays.contains(st)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            ExpressionTree valueExp,
            /*@CompilerMessageKey*/ String errorKey) {

        // Slightly relaxes the usual assignment rules by allowing assignments where the right
        // hand side is a value known at compile time and the type of the left hand side is
        // annotated with LT*LengthOf("a").  If the min length of a is in the correct
        // relationship with the value on the right hand side, then the assignment is legal.
        Integer rhsValue = atypeFactory.valMaxFromExpressionTree(valueExp);
        if (rhsValue == null) {
            super.commonAssignmentCheck(varType, valueExp, errorKey);
            return;
        } else if (rhsValue == 0) {
            // 0 is an index of all arrays
            return;
        }
        AnnotationMirror upperBoundAnno = varType.getAnnotationInHierarchy(atypeFactory.UNKNOWN);

        boolean minLenMatches = true;
        String[] arrayNames = UpperBoundAnnotatedTypeFactory.getValue(upperBoundAnno);
        if (arrayNames == null) {
            super.commonAssignmentCheck(varType, valueExp, errorKey);
            return;
        }
        for (String arrayName : arrayNames) {
            int minLen =
                    atypeFactory
                            .getMinLenAnnotatedTypeFactory()
                            .getMinLenFromString(arrayName, valueExp, getCurrentPath());

            if (AnnotationUtils.areSameByClass(upperBoundAnno, LTLengthOf.class)) {
                if (!(minLen > rhsValue)) {
                    minLenMatches = false;
                }
            } else if (AnnotationUtils.areSameByClass(upperBoundAnno, LTEqLengthOf.class)) {
                if (!(minLen >= rhsValue)) {
                    minLenMatches = false;
                }
            } else if (AnnotationUtils.areSameByClass(upperBoundAnno, LTOMLengthOf.class)) {
                if (!(minLen - 1 > rhsValue)) {
                    minLenMatches = false;
                }
            }
        }
        if (!minLenMatches) {
            super.commonAssignmentCheck(varType, valueExp, errorKey);
        }
    }
}
