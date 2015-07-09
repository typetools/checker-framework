package org.checkerframework.common.fieldtypeinference;

import java.util.List;

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
 * {@link org.checkerframework.common.fieldtypeinference.FieldTypeInferenceAnnotatedTypeFactory}.
 *
 * @author pbsf
 */

public class FieldTypeInferenceTransfer extends CFTransfer {

    private final FieldTypeInferenceAnnotatedTypeFactory factory;

    public FieldTypeInferenceTransfer(CFAbstractAnalysis<CFValue, CFStore,
            CFTransfer> analysis) {
        super(analysis);
        assert (analysis.getTypeFactory() instanceof FieldTypeInferenceAnnotatedTypeFactory);
        factory = (FieldTypeInferenceAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    @Override
    public CFStore initialStore(UnderlyingAST underlyingAST,
            List<LocalVariableNode> parameters) {
        CFStore initialStore = super.initialStore(underlyingAST, parameters);
        for (Receiver r : factory.getAssignedPrivateFieldsKeySet()) {
            for (AnnotationMirror am : factory.getAssignedPrivateFieldATM(r).
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
            AnnotatedTypeMirror lhsDeclType = AnnotatedTypeMirror.createType(
                    n.getTarget().getType(), factory, true);
            factory.addAssignedField(expr, lhsDeclType,
                    factory.getAnnotatedType(rhs.getTree()));
        }
        return result;
    }

}
