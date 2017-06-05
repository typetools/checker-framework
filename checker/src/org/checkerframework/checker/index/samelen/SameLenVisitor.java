package org.checkerframework.checker.index.samelen;
/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
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

    // This variable is used to store the name of the variable being assigned to on
    // the left hand side of an assignment/pseudo-assignment, so that that variable
    // can later be included in the SameLen type. This pattern is a bit uncomfortable,
    // but there is not a clear other way to save this information.
    private String lastVar = null;

    protected void commonAssignmentCheck(
            Tree varTree, ExpressionTree valueExp, /*@CompilerMessageKey*/ String errorKey) {
        if (varTree.getKind() == Tree.Kind.VARIABLE) {
            VariableTree vTree = (VariableTree) varTree;
            lastVar = vTree.getName().toString();
        } else {
            lastVar = null;
        }
        super.commonAssignmentCheck(varTree, valueExp, errorKey);
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
                List<String> names = new ArrayList<>();
                names.add(rec.toString());
                if (lastVar != null) {
                    names.add(lastVar);
                }
                AnnotationMirror newSameLen = atypeFactory.getCombinedSameLen(arraysInAnno, names);
                valueType.replaceAnnotation(newSameLen);
            }
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
    }
}
