package org.checkerframework.checker.index.samelen;
/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.PolySameLen;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class SameLenVisitor extends BaseTypeVisitor<SameLenAnnotatedTypeFactory> {
    public SameLenVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Modifies the common assignment checks to ensure that SameLen annotations are always merged.
     * The check is not relaxed in any way.
     */
    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            /*@CompilerMessageKey*/ String errorKey) {
        if (valueType.getKind() == TypeKind.ARRAY
                && TreeUtils.isExpressionTree(valueTree)
                && !(valueType.hasAnnotation(PolySameLen.class)
                        && varType.hasAnnotation(PolySameLen.class))) {

            AnnotationMirror am = valueType.getAnnotation(SameLen.class);
            List<String> arraysInAnno =
                    am == null
                            ? new ArrayList<String>()
                            : IndexUtil.getValueOfAnnotationWithStringArgument(am);

            Receiver rec = FlowExpressions.internalReprOf(atypeFactory, (ExpressionTree) valueTree);
            if (rec != null && SameLenAnnotatedTypeFactory.isReceiverToStringParsable(rec)) {
                List<String> itself = Collections.singletonList(rec.toString());
                AnnotationMirror newSameLen = atypeFactory.getCombinedSameLen(arraysInAnno, itself);
                valueType.replaceAnnotation(newSameLen);
            }
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
    }
}
