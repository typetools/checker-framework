package org.checkerframework.common.fieldtypeinference;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * The ATF of a Checker must be a subtype of this class for it to perform
 * intra-class inference for private fields.
 *
 * @author pbsf
 *
 */
public class FieldTypeInferenceAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** Maps a Receiver representing a private field to an ATM that contains the
    LUB of all AMs they have been assigned to. */
    private final Map<Receiver, AnnotatedTypeMirror> assignedPrivateFields =
            new HashMap<Receiver, AnnotatedTypeMirror>();

    public FieldTypeInferenceAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
    }

    public Map<Receiver, AnnotatedTypeMirror> getAssignedPrivateFields() {
        return assignedPrivateFields;
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new FieldTypeInferenceTransfer(analysis);
    }
}