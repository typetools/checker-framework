package org.checkerframework.checker.index.upperbound;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
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

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            ExpressionTree valueExp,
            /*@CompilerMessageKey*/ String errorKey) {

        System.out.println("-----");
        System.out.println(valueExp);
        System.out.println(valueExp.getKind());

        // Slightly relaxes the usual assignment rules by allowing assignments where the right
        // hand side is a value known at compile time and the type of the left hand side is
        // annotated with LT*LengthOf("a").  If the min length of a is in the correct
        // relationship with the value on the right hand side, then the assignment is legal.
        List<Integer> rhsValues = new ArrayList<>();
        UBQualifier qualifier = UBQualifier.createUBQualifier(varType, atypeFactory.UNKNOWN);
        Integer rhsValue = atypeFactory.valMaxFromExpressionTree(valueExp);
        if (rhsValue != null) {
            rhsValues.add(rhsValue);
        } else if (valueExp.getKind() == Tree.Kind.NEW_ARRAY) {
            // Check for a new array initializer; if there is one, check each of its elements.
            NewArrayTree newArrayTree = (NewArrayTree) valueExp;
            System.out.println(newArrayTree.getInitializers());
            if (newArrayTree.getInitializers() != null) {
                for (ExpressionTree exp : newArrayTree.getInitializers()) {
                    Integer val = atypeFactory.valMaxFromExpressionTree(exp);
                    System.out.println(exp);
                    System.out.println(val);
                    if (val == null) {
                        super.commonAssignmentCheck(varType, valueExp, errorKey);
                        return;
                    } else {
                        rhsValues.add(val);
                    }
                }
            } else {
                System.out.println("getInit is null");
                super.commonAssignmentCheck(varType, valueExp, errorKey);
                return;
            }
        } else {
            System.out.println("not an int, not an array");
            super.commonAssignmentCheck(varType, valueExp, errorKey);
            return;
        }

        System.out.println(qualifier);
        if (qualifier.isUnknownOrBottom()) {
            System.out.println("not an interesting qualifier");
            super.commonAssignmentCheck(varType, valueExp, errorKey);
            return;
        }

        LessThanLengthOf ltl = (LessThanLengthOf) qualifier;

        for (String arrayName : ltl.getArrays()) {
            int minLen =
                    atypeFactory
                            .getMinLenAnnotatedTypeFactory()
                            .getMinLenFromString(arrayName, valueExp, getCurrentPath());
            boolean minLenOk = true;
            for (Integer value : rhsValues) {
                minLenOk = ltl.isValuePlusOffsetLessThanMinLen(arrayName, value, minLen);
            }
            if (!minLenOk) { //|| rhsValues.size() == 0) {
                System.out.println("All minlens weren't okay");
                super.commonAssignmentCheck(varType, valueExp, errorKey);
                return;
            }
        }
    }
}
