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
import org.checkerframework.checker.minlen.qual.MinLen;
import org.checkerframework.checker.minlen.qual.MinLenBottom;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.Unknown;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

public class MinLenTransfer extends CFAbstractTransfer<CFValue, MinLenStore, MinLenTransfer> {

    protected MinLenAnalysis analysis;
    protected static MinLenAnnotatedTypeFactory atypeFactory;
    protected final ExecutableElement listAdd;
    protected final ExecutableElement listAdd2;
    protected final ExecutableElement listToArray;
    protected final ExecutableElement listToArray1;
    protected final ExecutableElement arrayAsList;
    protected static ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    private QualifierHierarchy qualifierHierarchy;

    public MinLenTransfer(MinLenAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (MinLenAnnotatedTypeFactory) analysis.getTypeFactory();
        valueAnnotatedTypeFactory = atypeFactory.getValueAnnotatedTypeFactory();
        qualifierHierarchy = atypeFactory.getQualifierHierarchy();
        ProcessingEnvironment env = atypeFactory.getProcessingEnv();
        this.listAdd = TreeUtils.getMethod("java.util.List", "add", 1, env);
        this.listAdd2 = TreeUtils.getMethod("java.util.List", "add", 2, env);
        this.listToArray = TreeUtils.getMethod("java.util.List", "toArray", 0, env);
        this.listToArray1 = TreeUtils.getMethod("java.util.List", "toArray", 1, env);
        this.arrayAsList = TreeUtils.getMethod("java.util.Arrays", "asList", 1, env);
    }

    @Override
    public TransferResult<CFValue, MinLenStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, MinLenStore> in) {
        TransferResult<CFValue, MinLenStore> result = super.visitAssignment(node, in);

        // When an array is created using another array's length as the dimension, transfer
        // that array's MinLen annotation to the new array.

        if (node.getTarget().getType().getKind() == TypeKind.ARRAY) {
            // An array is being assigned.
            if (node.getExpression() instanceof ArrayCreationNode) {
                // If a new array is being created.
                if (((ArrayCreationNode) node.getExpression()).getDimensions().size() > 0) {
                    Node lengthNode = ((ArrayCreationNode) node.getExpression()).getDimension(0);
                    if (lengthNode instanceof FieldAccessNode) {
                        if (((FieldAccessNode) lengthNode).getReceiver().getType().getKind()
                                        == TypeKind.ARRAY
                                && ((FieldAccessNode) lengthNode).getFieldName().equals("length")) {
                            // Finally, confirmation that a new array has been created using another array's length.
                            AnnotationMirror otherMinLen =
                                    atypeFactory.getAnnotationMirror(
                                            ((FieldAccessNode) lengthNode).getReceiver().getTree(),
                                            MinLen.class);
                            Receiver rec =
                                    FlowExpressions.internalReprOf(
                                            analysis.getTypeFactory(), node.getTarget());
                            MinLenStore store = result.getRegularStore();
                            store.insertValue(rec, otherMinLen);
                        }
                    }
                }
            }
        }

        return result;
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
            AnnotatedTypeMirror ATM =
                    atypeFactory.getAnnotatedType(node.getTarget().getReceiver().getTree());
            AnnotationMirror anno = ATM.getAnnotation(MinLen.class);
            if (anno == null || AnnotationUtils.areSameByClass(anno, MinLenBottom.class)) {
                return result;
            }
            int value = MinLenAnnotatedTypeFactory.getMinLenValue(anno);
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

    @Override
    public TransferResult<CFValue, MinLenStore> visitArrayAccess(
            ArrayAccessNode node, TransferInput<CFValue, MinLenStore> in) {
        TransferResult<CFValue, MinLenStore> result = super.visitArrayAccess(node, in);

        if (valueAnnotatedTypeFactory
                .getAnnotatedType(node.getArray().getTree())
                .hasAnnotation(ArrayLen.class)) {
            // In this case, refine the MinLen to match the ArrayLen.
            AnnotationMirror arrayLenAnm =
                    valueAnnotatedTypeFactory
                            .getAnnotatedType(node.getArray().getTree())
                            .getAnnotation(ArrayLen.class);
            MinLenStore store = in.getRegularStore();
            int minlen = Collections.min(ValueAnnotatedTypeFactory.getArrayLength(arrayLenAnm));
            Receiver rec =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getArray());
            store.insertValue(rec, atypeFactory.createMinLen(minlen));
        }

        return result;
    }

    /**
     * This struct contains all of the information that the refinement functions need. It's called
     * by each node function (i.e. greater than node, less than node, etc.) and then the results are
     * passed to the refinement function in whatever order is appropriate for that node. Its
     * constructor contains all of its logic. I originally wrote this for LowerBoundTransfer but I'm
     * duplicating it here since I need it again...maybe it should live elsewhere and be shared? I
     * don't know where though.
     */
    private class RefinementInfo {
        public Node left, right;
        public Set<AnnotationMirror> leftType, rightType;
        public MinLenStore thenStore, elseStore;
        public ConditionalTransferResult<CFValue, MinLenStore> newResult;

        public RefinementInfo(
                TransferResult<CFValue, MinLenStore> result,
                TransferInput<CFValue, MinLenStore> in,
                Node r,
                Node l) {
            right = r;
            left = l;

            rightType = in.getValueOfSubNode(right).getAnnotations();
            leftType = in.getValueOfSubNode(left).getAnnotations();

            thenStore = result.getRegularStore();
            elseStore = thenStore.copy();

            newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
        }
    }

    // So I actually just ended up copying these from Lower Bound Transfer too.
    // The only parts that are actually different are the definitions of
    // refineGT and refineGTE, and the handling of equals and not equals. The
    // code for the visitGreaterThan, visitLessThan, etc., are all identical to
    // their LBC counterparts.

    @Override
    public TransferResult<CFValue, MinLenStore> visitGreaterThan(
            GreaterThanNode node, TransferInput<CFValue, MinLenStore> in) {
        TransferResult<CFValue, MinLenStore> result = super.visitGreaterThan(node, in);
        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch, which is the inverse of the then branch.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, MinLenStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode node, TransferInput<CFValue, MinLenStore> in) {
        TransferResult<CFValue, MinLenStore> result = super.visitGreaterThanOrEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, MinLenStore> visitLessThanOrEqual(
            LessThanOrEqualNode node, TransferInput<CFValue, MinLenStore> in) {
        TransferResult<CFValue, MinLenStore> result = super.visitLessThanOrEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch. A <= is just a flipped >=.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, MinLenStore> visitLessThan(
            LessThanNode node, TransferInput<CFValue, MinLenStore> in) {
        TransferResult<CFValue, MinLenStore> result = super.visitLessThan(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch. A < is just a flipped >.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, MinLenStore> visitEqualTo(
            EqualToNode node, TransferInput<CFValue, MinLenStore> in) {
        TransferResult<CFValue, MinLenStore> result = super.visitEqualTo(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        refineEq(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // The else branch should only be refined if a length is being compared
        // to zero. The following code block implements this special case.
        // This special case occurs because zero is a hard bound on the bottom
        // of the array (i.e. no array can be smaller than zero), so in this
        // case the MinLen of the array is one.
        refineZeroEquality(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);
        refineZeroEquality(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, MinLenStore> visitNotEqual(
            NotEqualNode node, TransferInput<CFValue, MinLenStore> in) {
        TransferResult<CFValue, MinLenStore> result = super.visitNotEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);

        refineEq(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        // The then branch should only be refined if a length is being compared
        // to zero. The following code block implements this special case.
        // This special case occurs because zero is a hard bound on the bottom
        // of the array (i.e. no array can be smaller than zero), so in this
        // case the MinLen of the array is one.
        refineZeroEquality(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);
        refineZeroEquality(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        return rfi.newResult;
    }

    private void refineEq(
            Node left,
            Set<AnnotationMirror> leftTypeSet,
            Node right,
            Set<AnnotationMirror> rightTypeSet,
            MinLenStore store) {

        AnnotationMirror rightType =
                qualifierHierarchy.findAnnotationInHierarchy(rightTypeSet, atypeFactory.MIN_LEN_0);
        AnnotationMirror leftType =
                qualifierHierarchy.findAnnotationInHierarchy(leftTypeSet, atypeFactory.MIN_LEN_0);

        if (leftType == null || rightType == null) {
            return;
        }

        AnnotationMirror newType = qualifierHierarchy.greatestLowerBound(leftType, rightType);

        Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
        Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);

        store.insertValue(rightRec, newType);
        store.insertValue(leftRec, newType);
    }

    private void refineZeroEquality(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            MinLenStore store) {
        FieldAccessNode fi = null;
        Tree tree = null;
        Receiver rec = null;
        Set<AnnotationMirror> type = null;
        // Only the length matters. This will miss an expression which
        // include an array length (like "a.length + 1"), but that's okay
        // for now.
        // FIXME: Joe: List support will be needed here too.

        if (left instanceof FieldAccessNode) {
            fi = (FieldAccessNode) left;
            tree = right.getTree();
            rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), fi.getReceiver());
            type = leftType;
        } else {
            return;
        }

        if (fi == null || tree == null || rec == null || type == null) {
            return;
        }
        if (fi.getFieldName().equals("length")
                && fi.getReceiver().getType().getKind() == TypeKind.ARRAY) {
            // At this point, MinLen needs to invoke the constant value checker
            // to find out if it knows anything about what the length is being
            // compared to.

            AnnotatedTypeMirror valueType = atypeFactory.valueTypeFromTree(tree);

            if (valueType == null) {
                return;
            }

            Integer newMinLen = atypeFactory.getMinLenFromValueType(valueType);

            if (newMinLen == null) {
                return;
            }

            // This has to be a comparison against zero; otherwise, refineGTE will
            // have the same behavior as this function.
            if (newMinLen != 0) {
                return;
            }

            AnnotationMirror anno = AnnotationUtils.getAnnotationByClass(type, MinLen.class);
            if (!AnnotationUtils.hasElementValue(anno, "value")) {
                return;
            }

            Integer currentMinLen =
                    AnnotationUtils.getElementValue(anno, "value", Integer.class, true);

            if (1 > currentMinLen) {
                store.insertValue(rec, atypeFactory.createMinLen(1));
                return;
            }

            return;
        }
    }

    private void refineGT(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            MinLenStore store) {
        FieldAccessNode fi = null;
        Tree tree = null;
        Receiver rec = null;
        Set<AnnotationMirror> type = null;
        // Only length matters. This will miss an expression which
        // include an array length (like "a.length + 1"), but that's okay
        // for now.
        // FIXME: Joe: List support will be needed here too.

        if (left instanceof FieldAccessNode) {
            fi = (FieldAccessNode) left;
            tree = right.getTree();
            rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), fi.getReceiver());
            type = leftType;
        } else {
            return;
        }

        if (fi == null || tree == null || rec == null || type == null) {
            return;
        }

        if (fi.getFieldName().equals("length")
                && fi.getReceiver().getType().getKind() == TypeKind.ARRAY) {
            // At this point, MinLen needs to invoke the constant value checker
            // to find out if it knows anything about what the length is being
            // compared to.

            AnnotatedTypeMirror valueType = atypeFactory.valueTypeFromTree(tree);

            if (valueType == null) {
                return;
            }

            Integer newMinLen = atypeFactory.getMinLenFromValueType(valueType);

            if (newMinLen == null) {
                return;
            }

            AnnotationMirror anno = AnnotationUtils.getAnnotationByClass(type, MinLen.class);
            if (!AnnotationUtils.hasElementValue(anno, "value")) {
                return;
            }

            Integer currentMinLen =
                    AnnotationUtils.getElementValue(anno, "value", Integer.class, true);

            if (newMinLen + 1 > currentMinLen) {
                store.insertValue(rec, atypeFactory.createMinLen(newMinLen + 1));
                return;
            }

            return;
        }
    }

    private void refineGTE(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            MinLenStore store) {
        FieldAccessNode fi = null;
        Tree tree = null;
        Receiver rec = null;
        Set<AnnotationMirror> type = null;
        // Only length matters. This will miss an expression which
        // include an array length (like "a.length + 1"), but that's okay
        // for now.
        // FIXME: Joe: List support will be needed here too.
        if (left instanceof FieldAccessNode) {
            fi = (FieldAccessNode) left;
            tree = right.getTree();
            rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), fi.getReceiver());
            type = leftType;
        } else if (right instanceof FieldAccessNode) {
            fi = (FieldAccessNode) right;
            tree = left.getTree();
            rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), fi.getReceiver());
            type = rightType;
        } else {
            return;
        }

        if (fi == null || tree == null || rec == null || type == null) {
            return;
        }

        if (fi.getFieldName().equals("length")
                && fi.getReceiver().getType().getKind() == TypeKind.ARRAY) {
            // At this point, MinLen needs to invoke the constant value checker
            // to find out if it knows anything about what the length is being
            // compared to.

            AnnotatedTypeMirror valueType = atypeFactory.valueTypeFromTree(tree);

            if (valueType == null) {
                return;
            }

            Integer newMinLen = atypeFactory.getMinLenFromValueType(valueType);

            if (newMinLen == null) {
                return;
            }

            AnnotationMirror anno = AnnotationUtils.getAnnotationByClass(type, MinLen.class);
            if (!AnnotationUtils.hasElementValue(anno, "value")) {
                return;
            }
            Integer currentMinLen =
                    AnnotationUtils.getElementValue(anno, "value", Integer.class, true);
            if (newMinLen > currentMinLen) {
                store.insertValue(rec, atypeFactory.createMinLen(newMinLen));
            }
        }
    }
}
