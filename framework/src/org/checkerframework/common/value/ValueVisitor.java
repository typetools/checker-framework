package org.checkerframework.common.value;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.framework.source.Result;

import java.util.List;

import javax.lang.model.element.Element;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

/**
 * @author plvines
 *
 *         Visitor for the Constant Value type-system.
 *
 */
public class ValueVisitor extends BaseTypeVisitor<ValueAnnotatedTypeFactory> {

    public ValueVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected ValueAnnotatedTypeFactory createTypeFactory() {
        return new ValueAnnotatedTypeFactory(checker);
    }

    /**
     * Issues a warning if any constant-value annotation has &gt; MAX_VALUES number of values provided.
     * Works together with ValueAnnotatedTypeFactory.ValueTypeAnnotator.replaceWithUnknownValIfTooManyValues
     * which treats the value as @UnknownVal in this case.
     */
    @Override
    public Void visitAnnotation(AnnotationTree node, Void p) {
        List<? extends ExpressionTree> args = node.getArguments();

        if (args.isEmpty()) {
            // Nothing to do if there are no annotation arguments.
            return super.visitAnnotation(node, p);
        }

        Element element = TreeInfo.symbol((JCTree) node.getAnnotationType());
        if (!(element.toString().equals(ArrayLen.class.getName())
                || element.toString().equals(BoolVal.class.getName())
                || element.toString().equals(DoubleVal.class.getName())
                || element.toString().equals(IntVal.class.getName()) || element
                .toString().equals(StringVal.class.getName()))) {
            return super.visitAnnotation(node, p);
        }

        if (node.getArguments().size() > 0
                && node.getArguments().get(0).getKind() == Kind.ASSIGNMENT) {
            AssignmentTree argument = (AssignmentTree) node.getArguments().get(
                    0);
            if (argument.getExpression().getKind() == Tree.Kind.NEW_ARRAY) {
                int numArgs = ((NewArrayTree) argument.getExpression())
                        .getInitializers().size();

                if (numArgs > ValueAnnotatedTypeFactory.MAX_VALUES) {
                    checker.report(Result.warning("too.many.values.given",
                            ValueAnnotatedTypeFactory.MAX_VALUES), node);
                    return null;
                }
            }

        }
        return super.visitAnnotation(node, p);
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        if (node.getExpression().getKind() == Kind.NULL_LITERAL) {
            return null;
        }
        return super.visitTypeCast(node, p);
    }

}
