package org.checkerframework.checker.index.lowerbound;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Implements the actual checks to make sure that array accesses aren't too low. Will issue a
 * warning if a variable that can't be proved to be either "NonNegative" (i.e. &ge; 0) or "Positive"
 * (i.e. &ge; 1) is used as an array index.
 */
public class LowerBoundVisitor extends BaseTypeVisitor<LowerBoundAnnotatedTypeFactory> {

    /* This is a key into the messages.properties file in the same
     * directory, which includes the actual text of the warning.
     */
    private static final @CompilerMessageKey String LOWER_BOUND = "array.access.unsafe.low";
    private static final @CompilerMessageKey String NEGATIVE_ARRAY = "array.length.negative";

    public LowerBoundVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void type) {
        ExpressionTree index = tree.getIndex();
        String arrName = tree.getExpression().toString();
        AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(index);
        if (!(indexType.hasAnnotation(NonNegative.class)
                || indexType.hasAnnotation(Positive.class))) {
            checker.report(Result.failure(LOWER_BOUND, indexType.toString(), arrName), index);
        }

        return super.visitArrayAccess(tree, type);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, Void type) {
        if (tree.getDimensions().size() > 0) {
            for (ExpressionTree dim : tree.getDimensions()) {
                AnnotatedTypeMirror dimType = atypeFactory.getAnnotatedType(dim);
                if (!(dimType.hasAnnotation(NonNegative.class)
                        || dimType.hasAnnotation(Positive.class))) {
                    checker.report(Result.failure(NEGATIVE_ARRAY, dimType.toString()), dim);
                }
            }
        }

        return super.visitNewArray(tree, type);
    }
}
