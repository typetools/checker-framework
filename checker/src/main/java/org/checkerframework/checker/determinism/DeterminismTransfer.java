package org.checkerframework.checker.determinism;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Transfer function for the determinism type-system.
 *
 * <p>Performs type refinement from {@code @OrderNonDet} to {@code @Det} for:
 *
 * <ul>
 *   <li>The receiver of List.sort.
 *   <li>The first argument of Arrays.sort.
 *   <li>The first argument of Arrays.parallelSort.
 *   <li>The first argument of Collections.sort.
 *   <li>The first argument of Collections.shuffle.
 * </ul>
 */
public class DeterminismTransfer extends CFTransfer {
    public DeterminismTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);
        DeterminismAnnotatedTypeFactory factory =
                (DeterminismAnnotatedTypeFactory) analysis.getTypeFactory();

        // Type refinement for List sort
        Node receiver = n.getTarget().getReceiver();
        // TODO: I am confused about this test.  In a static method invocation (like Arrays.sort),
        // the receiver should be null (shouldn't it?).  But this returns immediately if the
        // receiver is null, which suggests that it can't handle Arrays.sort.  What am I missing?
        if (receiver == null || TypesUtils.getTypeElement(receiver.getType()) == null) {
            return result;
        }
        // TODO: What is this the underlying type for?  It's the receiver, and it would be good for
        // the variable name to reflect that.  The current name is confusing.
        TypeMirror underlyingType = TypesUtils.getTypeElement(receiver.getType()).asType();

        // TODO: why is this variable needed?  I think it would be cleaner to abstract out the next
        // 4 or so lines as a isListSort() method.
        boolean isList = factory.isList(underlyingType);
        if (isList) {
            String methName = getMethodName(n, receiver);
            if (methName.equals("sort") && receiver.getType().getAnnotationMirrors().size() > 0) {
                // Check if receiver has OrderNonDet annotation
                AnnotationMirror receiverAnno =
                        receiver.getType().getAnnotationMirrors().iterator().next();
                if (receiverAnno != null
                        && AnnotationUtils.areSame(receiverAnno, factory.ORDERNONDET)) {
                    // TODO: the next three lines are repeated 5 times in this method.  Either put
                    // just one copy of it at the end, or abstract in into a method.
                    FlowExpressions.Receiver sortReceiver =
                            FlowExpressions.internalReprOf(factory, n.getTarget().getReceiver());
                    result.getThenStore().insertValue(sortReceiver, factory.DET);
                    result.getElseStore().insertValue(sortReceiver, factory.DET);
                }
            }
        }

        // Type refinement for Arrays sort
        boolean isArrays = factory.isArrays(underlyingType);
        if (isArrays) {
            String methName = getMethodName(n, receiver);
            if ((methName.equals("sort") || methName.equals("parallelSort"))) {
                AnnotatedTypeMirror firstArg =
                        factory.getAnnotatedType(n.getTree().getArguments().get(0));
                AnnotationMirror firstArgAnno = firstArg.getAnnotations().iterator().next();
                // Check if receiver has first argument annotation
                if (firstArgAnno != null
                        && AnnotationUtils.areSame(firstArgAnno, factory.ORDERNONDET)) {
                    boolean typeRefine = true;
                    List<Node> otherArgs = n.getArguments();
                    for (int i = 1; i < n.getArguments().size(); i++) {
                        AnnotatedTypeMirror otherArgType =
                                factory.getAnnotatedType(n.getTree().getArguments().get(i));
                        if (!otherArgType.hasAnnotation(factory.DET)) {
                            typeRefine = false;
                            break;
                        }
                    }
                    if (typeRefine) {
                        FlowExpressions.Receiver firtArgRep =
                                FlowExpressions.internalReprOf(factory, n.getArgument(0));
                        result.getThenStore().insertValue(firtArgRep, factory.DET);
                        result.getElseStore().insertValue(firtArgRep, factory.DET);
                    }
                }
            }
        }

        // Type refinement for Collections
        boolean isCollections = factory.isCollections(underlyingType);
        if (isCollections) {
            String methName = getMethodName(n, receiver);
            // refinement for sort
            if (methName.equals("sort")) {
                AnnotatedTypeMirror firstArg =
                        factory.getAnnotatedType(n.getTree().getArguments().get(0));
                AnnotationMirror firstArgAnno = firstArg.getAnnotations().iterator().next();
                // Check if receiver has first argument annotation
                if (firstArgAnno != null
                        && AnnotationUtils.areSame(firstArgAnno, factory.ORDERNONDET)) {
                    FlowExpressions.Receiver firtArgRep =
                            FlowExpressions.internalReprOf(factory, n.getArgument(0));
                    result.getThenStore().insertValue(firtArgRep, factory.DET);
                    result.getElseStore().insertValue(firtArgRep, factory.DET);
                }
            }

            // refinement for shuffle
            if (methName.equals("shuffle")) {
                AnnotatedTypeMirror firstArg =
                        factory.getAnnotatedType(n.getTree().getArguments().get(0));
                AnnotationMirror firstArgAnno = firstArg.getAnnotations().iterator().next();
                // Check if receiver has first argument annotation
                FlowExpressions.Receiver firtArgRep =
                        FlowExpressions.internalReprOf(factory, n.getArgument(0));
                result.getThenStore().insertValue(firtArgRep, factory.NONDET);
                result.getElseStore().insertValue(firtArgRep, factory.NONDET);
            }
        }
        return result;
    }

    /**
     * Extracts just the method name from MethodInvocationNode.
     *
     * @param n MethodInvocationNode
     * @param receiver Node
     * @return String method name
     */
    // TODO: Any use of toString or of string manipulation is a code smell.  This method should be
    // rewritten to use accessors.  Or, have a static variable for the methods of interest, and use
    // equals() on the methods.
    // TODO: Why isn't this method private?
    String getMethodName(MethodInvocationNode n, Node receiver) {
        String methodName = n.toString();
        String methodnameWithoutReceiver = methodName.substring(receiver.toString().length());
        int startIndex = methodnameWithoutReceiver.indexOf(".");
        int endIndex = methodnameWithoutReceiver.indexOf("(");
        String methName = methodnameWithoutReceiver.substring(startIndex + 1, endIndex);
        return methName;
    }
}
