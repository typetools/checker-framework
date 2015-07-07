package org.checkerframework.common.fieldtypeinference;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;

import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * 
 * This Transfer Function performs type inference for private fields in a class.
 * <p>
 * To enable type inference for private fields, the Checker's ATF must be a subtype of
 * <tt>org.checkerframework.common.fieldtypeinference.FieldTypeInferenceAnnotatedTypeFactory</tt>.
 *
 * @author pbsf
 */

public class FieldTypeInferenceTransfer extends CFTransfer {

    private final Map<Receiver, AnnotatedTypeMirror> assignedPrivateFields;
    private final FieldTypeInferenceAnnotatedTypeFactory factory;

    public FieldTypeInferenceTransfer(CFAbstractAnalysis<CFValue, CFStore,
            CFTransfer> analysis) {
        super(analysis);
        assert (analysis.getTypeFactory() instanceof FieldTypeInferenceAnnotatedTypeFactory);
        factory = (FieldTypeInferenceAnnotatedTypeFactory) analysis.getTypeFactory();
        assignedPrivateFields = factory.getAssignedPrivateFields();
    }

    @Override
    public CFStore initialStore(UnderlyingAST underlyingAST,
            List<LocalVariableNode> parameters) {
        CFStore initialStore = super.initialStore(underlyingAST, parameters);
        for (Receiver r : assignedPrivateFields.keySet()) {
            for (AnnotationMirror am : assignedPrivateFields.get(r).
                    getAnnotations()) {
                initialStore.insertValue(r, am);
            }
        }
        return initialStore;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(AssignmentNode n,
            TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(n, in);
        assert result instanceof RegularTransferResult;
        Node rhs = n.getExpression();
        Receiver expr = FlowExpressions.internalReprOf(analysis.getTypeFactory(),
                n.getTarget());

        // If this is an assignment to a field of 'this' and this field is
        // private, then populate the map in the ATF.
        if (!expr.containsUnknown() && expr instanceof FieldAccess &&
                ((FieldAccess) expr).getField().getModifiers().
                contains(Modifier.PRIVATE)) {
            addAssignedField(expr, factory.getAnnotatedType(rhs.getTree()));
        }
        return result;
    }

    /**
     * Update the ATM mapped by the parameter <tt>r</tt> in the map
     * <tt>ClassValAnnotatedTypeFactory.analysis.assignedPrivateFields</tt> to be the LUB
     * of the parameter <tt>atm</tt> and the current ATM mapped by <tt>r</tt>
     * in this map.
     * <p>
     * If <tt>r</tt> is not a key in the map, it is added to the map
     * mapping to <tt>atm</tt>.
     * @param r
     *      A receiver representing a private field.
     * @param atm
     *      An ATM assigned to the private field in <tt>r</tt>.
     */
    public void addAssignedField(Receiver r, AnnotatedTypeMirror atm) {
        Map<Receiver, AnnotatedTypeMirror> assignedPrivateFields = factory.getAssignedPrivateFields();
        if (assignedPrivateFields.containsKey(r)) {
            Set<? extends AnnotationMirror> lub = analysis.getTypeFactory().
                    getQualifierHierarchy().leastUpperBounds(atm.getAnnotations(),
                    assignedPrivateFields.get(r).getAnnotations());
            // Would atm.replaceAnnotations work instead of these 2 lines below?
            // My concern is that in a scenario where the ATM has two annotations
            // where one is a subtype of the other, I must guarantee that only
            // the one which is the LUB of both will be in the ATM,
            // and the other should be removed. Or is the CF smart enough to
            // handle that in case both are present? - pbsf
            atm.clearAnnotations();
            atm.addAnnotations(lub);
        }
        assignedPrivateFields.put(r, atm);
    }

}
