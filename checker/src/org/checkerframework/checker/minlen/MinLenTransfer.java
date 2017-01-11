package org.checkerframework.checker.minlen;

import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexRefinementInfo;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.Unknown;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.TreeUtils;

public class MinLenTransfer extends IndexAbstractTransfer<MinLenStore, MinLenTransfer> {

    protected MinLenAnalysis analysis;
    protected MinLenAnnotatedTypeFactory atypeFactory;
    protected final ExecutableElement listAdd;
    protected final ExecutableElement listAdd2;
    protected final ExecutableElement listToArray;
    protected final ExecutableElement listToArray1;
    protected final ExecutableElement arrayAsList;

    private QualifierHierarchy qualifierHierarchy;

    public MinLenTransfer(MinLenAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (MinLenAnnotatedTypeFactory) analysis.getTypeFactory();
        qualifierHierarchy = atypeFactory.getQualifierHierarchy();
        ProcessingEnvironment env = atypeFactory.getProcessingEnv();
        this.listAdd = TreeUtils.getMethod("java.util.List", "add", 1, env);
        this.listAdd2 = TreeUtils.getMethod("java.util.List", "add", 2, env);
        this.listToArray = TreeUtils.getMethod("java.util.List", "toArray", 0, env);
        this.listToArray1 = TreeUtils.getMethod("java.util.List", "toArray", 1, env);
        this.arrayAsList = TreeUtils.getMethod("java.util.Arrays", "asList", 1, env);
    }

    @Override
    public TransferResult<CFValue, MinLenStore> visitMethodInvocation(
            MethodInvocationNode node, TransferInput<CFValue, MinLenStore> in) {
        ProcessingEnvironment env = atypeFactory.getProcessingEnv();
        TransferResult<CFValue, MinLenStore> result = super.visitMethodInvocation(node, in);

        String methodName = node.getTarget().getMethod().toString();
        boolean add = methodName.startsWith("add(");
        boolean asList = methodName.contains("asList(");
        boolean toArray = methodName.startsWith("toArray(");
        if (!(add || asList || toArray)) {
            return result;
        }
        if (TreeUtils.isMethodInvocation(node.getTree(), listAdd, env)
                || TreeUtils.isMethodInvocation(node.getTree(), listAdd2, env)) {
            Receiver rec =
                    FlowExpressions.internalReprOf(
                            analysis.getTypeFactory(), node.getTarget().getReceiver());
            if (node.getTarget().getReceiver().getTree() == null || rec instanceof Unknown) {
                return result;
            }
            Integer value = atypeFactory.getMinLenValue(node.getTarget().getReceiver().getTree());
            if (value == null) {
                return result;
            }
            AnnotationMirror AM = atypeFactory.createMinLen(value + 1);
            Set<AnnotationMirror> set = new HashSet<>();
            set.add(AM);
            CFValue minlen = new CFValue(analysis, set, node.getTarget().getReceiver().getType());
            if (MinLenStore.canInsertReceiver(rec)) {
                if (result.containsTwoStores()) {
                    result.getThenStore().replaceValue(rec, minlen);
                    result.getElseStore().replaceValue(rec, minlen);
                } else {
                    result.getRegularStore().replaceValue(rec, minlen);
                }
            }
            return result;
        } else if (TreeUtils.isMethodInvocation(node.getTree(), listToArray, env)
                || TreeUtils.isMethodInvocation(node.getTree(), listToArray1, env)) {
            if (node.getTarget().getReceiver().getTree() == null) {
                return result;
            }
            Integer value = atypeFactory.getMinLenValue(node.getTarget().getReceiver().getTree());
            if (value == null) {
                return result;
            }
            AnnotationMirror AM = atypeFactory.createMinLen(value + 1);
            result.setResultValue(analysis.createSingleAnnotationValue(AM, node.getType()));
            return result;
        } else if (TreeUtils.isMethodInvocation(node.getTree(), arrayAsList, env)) {
            Node arg = node.getArgument(0);
            Integer value = 0;
            if (arg instanceof ArrayCreationNode) {
                ArrayCreationNode aNode = (ArrayCreationNode) arg;
                List<Node> args = aNode.getInitializers();
                // if there is only one argument arg; and if arg is an array of T (T[] arg); and if T is not a primitive
                // then array.asList(arg).size() == arg.length
                // otherwise it is treated as varargs and array.asList(arg).size() == the number of arguments
                if (args.size() == 1
                        && args.get(0).getType().getKind().equals(TypeKind.ARRAY)
                        && !((ArrayType) args.get(0).getType())
                                .getComponentType()
                                .getKind()
                                .isPrimitive()) {
                    if (args.get(0).getTree() == null) {
                        return result;
                    }
                    value = atypeFactory.getMinLenValue(args.get(0).getTree());
                    if (value == null) {
                        return result;
                    }
                } else {
                    value = args.size();
                }
            }
            AnnotationMirror AM = atypeFactory.createMinLen(value);
            result.setResultValue(analysis.createSingleAnnotationValue(AM, node.getType()));
        }

        return result;
    }

    @Override
    public TransferResult<CFValue, MinLenStore> visitArrayAccess(
            ArrayAccessNode node, TransferInput<CFValue, MinLenStore> in) {
        TransferResult<CFValue, MinLenStore> result = super.visitArrayAccess(node, in);
        AnnotatedTypeMirror valueType = atypeFactory.valueTypeFromTree(node.getArray().getTree());

        if (valueType.hasAnnotation(ArrayLen.class)) {
            // In this case, refine the MinLen to match the ArrayLen.
            AnnotationMirror arrayLenAnm = valueType.getAnnotation(ArrayLen.class);
            MinLenStore store = in.getRegularStore();
            int minlen = Collections.min(ValueAnnotatedTypeFactory.getArrayLength(arrayLenAnm));
            Receiver rec =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getArray());
            store.insertValue(rec, atypeFactory.createMinLen(minlen));
        }

        return result;
    }

    @Override
    protected TransferResult<CFValue, MinLenStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, MinLenStore> res,
            Node firstNode,
            Node secondNode,
            CFValue firstValue,
            CFValue secondValue,
            boolean notEqualTo) {
        TransferResult<CFValue, MinLenStore> result =
                super.strengthenAnnotationOfEqualTo(
                        res, firstNode, secondNode, firstValue, secondValue, notEqualTo);
        IndexRefinementInfo<MinLenStore> rfi =
                new IndexRefinementInfo<>(result, analysis, firstNode, secondNode);

        MinLenStore equalsStore = notEqualTo ? rfi.elseStore : rfi.thenStore;
        MinLenStore notEqualsStore = notEqualTo ? rfi.thenStore : rfi.elseStore;

        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, equalsStore);
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, equalsStore);

        // Types in the not equal branch should only be refined if a length is being compared
        // to zero.
        // This special case occurs because zero is a hard bound on the bottom
        // of the array (i.e. no array can be smaller than zero), so in this
        // case the MinLen of the array is one.
        refineNotEqual(rfi.right, rfi.rightType, rfi.left, rfi.leftType, notEqualsStore);
        refineNotEqual(rfi.left, rfi.leftType, rfi.right, rfi.rightType, notEqualsStore);

        return rfi.newResult;
    }

    private Receiver getReceiverForFiNodeOrNull(Node node) {
        if (node instanceof FieldAccessNode) {
            Receiver rec =
                    FlowExpressions.internalReprOf(
                            analysis.getTypeFactory(), ((FieldAccessNode) node).getReceiver());
            return rec;
        }
        return null;
    }

    private Integer getNewMinLenForRefinement(
            Node fiNode, Node nonFiNode, Set<AnnotationMirror> leftType) {
        FieldAccessNode fi = null;
        Tree tree = null;
        Set<AnnotationMirror> type = null;
        // Only the length matters. This will miss an expression which
        // include an array length (like "a.length + 1"), but that's okay
        // for now.
        // FIXME: Joe: List support will be needed here too.

        if (fiNode instanceof FieldAccessNode) {
            fi = (FieldAccessNode) fiNode;
            tree = nonFiNode.getTree();
            type = leftType;
        } else {
            return null;
        }

        if (fi == null || tree == null || type == null) {
            return null;
        }
        if (fi.getFieldName().equals("length")
                && fi.getReceiver().getType().getKind() == TypeKind.ARRAY) {
            // At this point, MinLen needs to invoke the constant value checker
            // to find out if it knows anything about what the length is being
            // compared to.

            AnnotatedTypeMirror valueType = atypeFactory.valueTypeFromTree(tree);

            if (valueType == null) {
                return null;
            }

            Integer newMinLen = atypeFactory.getMinLenFromValueType(valueType);

            return newMinLen;
        }
        return null;
    }

    private void refineNotEqual(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            MinLenStore store) {

        Receiver rec = getReceiverForFiNodeOrNull(left);
        Integer newMinLen = getNewMinLenForRefinement(left, right, leftType);

        if (newMinLen != null && newMinLen == 0 && rec != null) {
            store.insertValue(rec, atypeFactory.createMinLen(1));
        }
    }

    @Override
    protected void refineGT(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            MinLenStore store) {

        Receiver rec = getReceiverForFiNodeOrNull(left);
        Integer newMinLen = getNewMinLenForRefinement(left, right, leftType);
        if (rec != null && newMinLen != null) {
            store.insertValue(rec, atypeFactory.createMinLen(newMinLen + 1));
        }
    }

    @Override
    protected void refineGTE(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            MinLenStore store) {
        Receiver rec = getReceiverForFiNodeOrNull(left);
        Integer newMinLen = getNewMinLenForRefinement(left, right, leftType);
        if (rec != null && newMinLen != null) {
            store.insertValue(rec, atypeFactory.createMinLen(newMinLen));
        }
    }
}
