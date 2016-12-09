package org.checkerframework.checker.lowerbound;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.lowerbound.qual.*;
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
            checker.report(Result.warning(LOWER_BOUND, indexType.toString(), arrName), index);
        }

        return super.visitArrayAccess(tree, type);
    }
}
