package org.checkerframework.checker.index.upperbound;

import com.sun.source.util.TreePath;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.upperbound.UpperBoundUtil.SideEffectKind;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.ElementUtils;

/**
 * At every possible side effect, let T be all the types (for method formals and returns of any
 * method in the enclosing class, and anywhere in the store) whose expression might have its value
 * affected by the side effect.
 *
 * <p>If the side effect is a reassignment to a local variable, arr1, these are only expressions
 * that include any of:
 *
 * <ul>
 *   <li>arr1
 * </ul>
 *
 * <p>If the side effect is a reassignment to an array field, arr1, these are expressions that
 * include any of:
 *
 * <ul>
 *   <li>arr1
 *   <li>a method call
 * </ul>
 *
 * <p>If the side effect is a reassignment to a non-array reference (non-primitive) field or a call
 * to a non-side-effect-free method, these are expressions that include any of:
 *
 * <ul>
 *   <li>a non-final field whose type is not an array
 *   <li>a method call
 * </ul>
 *
 * <p>If the side effect is a reassignment to a primitive field, no expressions are affected.
 *
 * <p>Let V be all the variables with a type in T. In particular, for a reassignment “arr1 = …”, V
 * includes every int variable with type LT[E]L("...arr1...").
 *
 * <p>For every variable v in V: If v is in the refinement store, then unrefine the int variable.
 * (No need to check the final unrefined type; it will be handled by the next rule if it still has
 * type @LT[E]L).).
 *
 * <p>For every type t in T: If t is not a refined type (not from the store), then it is a
 * user-written type such as @LT[E]L("...arr1...") or a possible alias. The type-checker issues an
 * error, stating that this is an illegal assignment. The type-checker suggests that the user should
 * either make the array variable final or (if the possible reassignment was a method call) annotate
 * the method as @SideEffectFree.
 */
public class UpperBoundStore extends CFAbstractStore<CFValue, UpperBoundStore> {

    public UpperBoundStore(UpperBoundAnalysis analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    public UpperBoundStore(CFAbstractStore<CFValue, UpperBoundStore> other) {
        super(other);
    }

    @Override
    public void updateForMethodCall(
            MethodInvocationNode n, AnnotatedTypeFactory factory, CFValue val) {
        super.updateForMethodCall(n, factory, val);

        ExecutableElement elt = n.getTarget().getMethod();

        if (!isSideEffectFree(factory, elt)) {
            // Should include all method calls and non-array references, but not the name of the method.
            clearFromStore(null, n, UpperBoundUtil.SideEffectKind.SIDE_EFFECTING_METHOD_CALL);
        }
    }

    @Override
    public void updateForAssignment(Node n, CFValue val) {
        // Do reassignment things here.
        super.updateForAssignment(n, val);

        // This code determines the list of dependencies in types that are to be invalidated
        SideEffectKind sideEffectKind;
        if (n instanceof LocalVariableNode
                && n.getType().getKind() == TypeKind.ARRAY
                && !ElementUtils.isEffectivelyFinal(((LocalVariableNode) n).getElement())) {
            sideEffectKind = UpperBoundUtil.SideEffectKind.LOCAL_VAR_REASSIGNMENT;
        } else if (n instanceof FieldAccessNode
                && !ElementUtils.isEffectivelyFinal(((FieldAccessNode) n).getElement())) {
            if (n.getType().getKind() == TypeKind.ARRAY) {
                sideEffectKind = UpperBoundUtil.SideEffectKind.ARRAY_FIELD_REASSIGNMENT;
            } else if (!n.getType().getKind().isPrimitive()) {
                sideEffectKind = UpperBoundUtil.SideEffectKind.NON_ARRAY_FIELD_REASSIGNMENT;
            } else {
                return; // No side effect to check
            }
        } else {
            return; // No side effect to check
        }

        FlowExpressions.Receiver rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), n);
        clearFromStore(rec, n, sideEffectKind);
    }

    /**
     * Clears receivers from the store whose current value may be effected by the side effect.
     *
     * @param reassignedVar reassigned variable or null
     * @param n current node
     * @param sideEffect kind of side effect
     */
    private void clearFromStore(Receiver reassignedVar, Node n, SideEffectKind sideEffect) {
        TreePath path = analysis.getTypeFactory().getPath(n.getTree());
        if (path == null) {
            System.out.println("NPE thrown while getting path for this node:");
            System.out.println(n);
            System.out.println("with tree:");
            System.out.println(n.getTree());

            System.out.println("relevant receiver:");
            System.out.println(reassignedVar);

            System.exit(1);
        }

        Set<Entry<? extends Receiver, CFValue>> receiverAnnotationEntry = new HashSet<>();
        receiverAnnotationEntry.addAll(localVariableValues.entrySet());
        receiverAnnotationEntry.addAll(methodValues.entrySet());
        receiverAnnotationEntry.addAll(classValues.entrySet());
        receiverAnnotationEntry.addAll(fieldValues.entrySet());
        receiverAnnotationEntry.addAll(arrayValues.entrySet());

        for (Entry<? extends Receiver, CFValue> entry : receiverAnnotationEntry) {
            Receiver r = entry.getKey();
            for (Receiver dependent :
                    UpperBoundUtil.getDependentReceivers(
                            entry.getValue().getAnnotations(),
                            path,
                            (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory())) {
                if (UpperBoundUtil.isSideEffected(dependent, reassignedVar, sideEffect)
                        != UpperBoundUtil.SideEffectError.NO_ERROR) {
                    this.clearValue(r);
                }
            }
        }
    }
}
