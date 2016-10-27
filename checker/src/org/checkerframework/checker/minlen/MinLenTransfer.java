package org.checkerframework.checker.minlen;

import com.sun.source.tree.Tree;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.minlen.qual.MinLen;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class MinLenTransfer extends CFAbstractTransfer<MinLenValue, MinLenStore, MinLenTransfer> {

    protected MinLenAnalysis analysis;
    protected static MinLenAnnotatedTypeFactory atypeFactory;
    protected final ProcessingEnvironment env;
    protected final ExecutableElement listAdd;
    protected final ExecutableElement listAdd2;
    protected final ExecutableElement listToArray;
    protected final ExecutableElement listToArray1;
    protected final ExecutableElement arrayAsList;

    public MinLenTransfer(MinLenAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (MinLenAnnotatedTypeFactory) analysis.getTypeFactory();
        this.env = MinLenAnnotatedTypeFactory.env;
        this.listAdd = TreeUtils.getMethod("java.util.List", "add", 1, env);
        this.listAdd2 = TreeUtils.getMethod("java.util.List", "add", 2, env);
        this.listToArray = TreeUtils.getMethod("java.util.List", "toArray", 0, env);
        this.listToArray1 = TreeUtils.getMethod("java.util.List", "toArray", 1, env);
        this.arrayAsList = TreeUtils.getMethod("java.util.Arrays", "asList", 1, env);
    }

    @Override
    public TransferResult<MinLenValue, MinLenStore> visitMethodInvocation(
            MethodInvocationNode node, TransferInput<MinLenValue, MinLenStore> in) {
        TransferResult<MinLenValue, MinLenStore> result = super.visitMethodInvocation(node, in);
        Receiver rec =
                FlowExpressions.internalReprOf(
                        analysis.getTypeFactory(), node.getTarget().getReceiver());
        MinLenStore store = result.getRegularStore();
        if (TreeUtils.isMethodInvocation(node.getTree(), listAdd, env)
                || TreeUtils.isMethodInvocation(node.getTree(), listAdd2, env)) {
            if (node.getTarget().getReceiver().getTree() == null) {
                return result;
            }
            AnnotatedTypeMirror ATM =
                    atypeFactory.getAnnotatedType(node.getTarget().getReceiver().getTree());
            AnnotationMirror anno = ATM.getAnnotation(MinLen.class);
            int value = MinLenAnnotatedTypeFactory.getMinLenValue(anno);
            AnnotationMirror AM = atypeFactory.createMinLen(value + 1);
            store.clearValue(rec);
            store.insertValue(rec, AM);
            RegularTransferResult<MinLenValue, MinLenStore> newResult =
                    new RegularTransferResult<MinLenValue, MinLenStore>(
                            result.getResultValue(), store);
            return newResult;
        } else if (TreeUtils.isMethodInvocation(node.getTree(), listToArray, env)
                || TreeUtils.isMethodInvocation(node.getTree(), listToArray1, env)) {
            if (node.getTarget().getReceiver().getTree() == null) {
                return result;
            }
            AnnotatedTypeMirror ATM =
                    atypeFactory.getAnnotatedType(node.getTarget().getReceiver().getTree());
            AnnotationMirror anno = ATM.getAnnotation(MinLen.class);
            int value = MinLenAnnotatedTypeFactory.getMinLenValue(anno);
            AnnotationMirror AM = atypeFactory.createMinLen(value + 1);
            result.setResultValue(analysis.createSingleAnnotationValue(AM, node.getType()));
            return result;
        } else if (TreeUtils.isMethodInvocation(node.getTree(), arrayAsList, env)) {
            Node arg = node.getArgument(0);
            int value = 0;
            if (arg instanceof ArrayCreationNode) {
                ArrayCreationNode aNode = (ArrayCreationNode) arg;
                List<Node> args = aNode.getInitializers();
                // if there is only one argument which is an array that subclasses an object array (so Not primitive component)
                // then the size of that array is the size of the array.asList
                // otherwise it is treated as varargs and the resulting list's size is the number of arguments
                if (args.size() == 1
                        && args.get(0).getType().getKind().equals(TypeKind.ARRAY)
                        && !((ArrayType) args.get(0).getType())
                                .getComponentType()
                                .getKind()
                                .isPrimitive()) {
                    if (args.get(0).getTree() == null) {
                        return result;
                    }
                    AnnotatedTypeMirror ATM = atypeFactory.getAnnotatedType(args.get(0).getTree());
                    AnnotationMirror anno = ATM.getAnnotation(MinLen.class);
                    value = MinLenAnnotatedTypeFactory.getMinLenValue(anno);
                } else {
                    value = args.size();
                }
            }
            AnnotationMirror AM = atypeFactory.createMinLen(value);
            result.setResultValue(analysis.createSingleAnnotationValue(AM, node.getType()));
        }

        return result;
    }
}
