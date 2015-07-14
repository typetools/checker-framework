package org.checkerframework.common.fieldtypeinference;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotatedTypes;

import com.sun.source.tree.CompilationUnitTree;

/**
 * The ATF of a Checker must be a subtype of this class for it to perform
 * intra-class inference for private fields.
 *
 * @author pbsf
 *
 */
public class FieldTypeInferenceAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** Maps a Receiver representing a private field to an ATM that contains the
    LUB of all AMs they have been assigned to. **/
    private final Map<Receiver, AnnotatedTypeMirror> assignedPrivateFields =
            new HashMap<Receiver, AnnotatedTypeMirror>();

    public FieldTypeInferenceAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public void setRoot(CompilationUnitTree root) {
        super.setRoot(root);
        assignedPrivateFields.clear();
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new FieldTypeInferenceTransfer(analysis);
    }

    /**
     * Updates the ATM mapped by the parameter <tt>r</tt> in the map
     * <tt>assignedPrivateFields</tt> to be the LUB between the parameter
     * <tt>rhsATM</tt> and the current ATM mapped by <tt>r</tt> in this map.
     * <p>
     * If <tt>r</tt> is not a key in the map, it is added to the map
     * mapping to <tt>rhsATM</tt>.
     * @param r
     *      A receiver representing a private field.
     * @param fieldDeclType
     *      The declared type of the private field.
     * @param rhsATM
     *      An ATM assigned to the private field in <tt>r</tt>.
     */
    public void addAssignedField(Receiver r, AnnotatedTypeMirror fieldDeclType,
            AnnotatedTypeMirror rhsATM) {
        // This is ONLY refining the primary annotations (and not the
        // Java types/type arguments) of the private field. That may change if
        // the implementation of AnnotatedTypes.leastUpperBound changes.
        AnnotatedTypeMirror previousLUB = assignedPrivateFields.get(r);
        if (previousLUB != null) { // If a previous LUB exists in the map
            AnnotatedTypeMirror newLUB = AnnotatedTypes.leastUpperBound(
                    getProcessingEnv(), this, previousLUB, rhsATM);
            previousLUB.clearAnnotations();
            previousLUB.addAnnotations(newLUB.getAnnotations());
        } else {
            fieldDeclType.clearAnnotations();
            fieldDeclType.addAnnotations(rhsATM.getAnnotations());
            assignedPrivateFields.put(r, fieldDeclType);
        }
    }

    public Set<Receiver> getAssignedPrivateFieldsKeySet() {
        return assignedPrivateFields.keySet();
    }

    public AnnotatedTypeMirror getAssignedPrivateFieldATM(Receiver r) {
        return assignedPrivateFields.get(r);
    }

}
