package org.checkerframework.checker.index.samelen;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.PolySameLen;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

public class SameLenVisitor extends BaseTypeVisitor<SameLenAnnotatedTypeFactory> {
    public SameLenVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Merges SameLen annotations, then calls super.
     *
     * <p>{@inheritDoc}
     */
    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        if (IndexUtil.isSequenceType(valueType.getUnderlyingType())
                && TreeUtils.isExpressionTree(valueTree)
                // if both annotations are @PolySameLen, there is nothing to do
                && !(valueType.hasAnnotation(PolySameLen.class)
                        && varType.hasAnnotation(PolySameLen.class))) {

            JavaExpression rhs = JavaExpression.fromTree((ExpressionTree) valueTree);
            if (rhs != null && SameLenAnnotatedTypeFactory.mayAppearInSameLen(rhs)) {
                String rhsExpr = rhs.toString();
                AnnotationMirror sameLenAnno = valueType.getAnnotation(SameLen.class);
                Collection<String> exprs;
                if (sameLenAnno == null) {
                    exprs = Collections.singletonList(rhsExpr);
                } else {
                    exprs =
                            new TreeSet<>(
                                    AnnotationUtils.getElementValueArray(
                                            sameLenAnno,
                                            atypeFactory.sameLenValueElement,
                                            String.class));
                    exprs.add(rhsExpr);
                }
                AnnotationMirror newSameLen = atypeFactory.createSameLen(exprs);
                valueType.replaceAnnotation(newSameLen);
            }
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
    }
}
