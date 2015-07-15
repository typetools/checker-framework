package org.checkerframework.common.fieldtypeinference;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.type.TypeMirror;

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
    LUB of all AMs that have been assigned to it. **/
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
     * @param lhsTypeMirror
     *      The declared type of the private field. lhsTypeMirror is
     *      side-effected and must not be modified.
     * @param rhsATM
     *      An ATM assigned to the private field in <tt>r</tt>.
     */
    public void addAssignedField(Receiver r, TypeMirror lhsTypeMirror,
            AnnotatedTypeMirror newATM) {
        // This is ONLY refining the primary annotations (and not the
        // Java types/type arguments) of the private field. That may change if
        // the implementation of AnnotatedTypes.leastUpperBound changes.
        AnnotatedTypeMirror curATM = assignedPrivateFields.get(r);
        if (curATM == null) {
            // A copy of the type of the LHS is created below.
            // Modifying this new type will not modify the
            // original type of the LHS. lhsTypeMirror must not be modified.
            curATM = AnnotatedTypeMirror.createType(lhsTypeMirror, this, true);
            assignedPrivateFields.put(r, curATM);
        } else {
            newATM = AnnotatedTypes.leastUpperBound(getProcessingEnv(), this,
                     curATM, newATM);
        }
        curATM.clearAnnotations();
        curATM.addAnnotations(newATM.getAnnotations());
    }

    public Set<Receiver> getAssignedPrivateFieldsKeySet() {
        return assignedPrivateFields.keySet();
    }

    public AnnotatedTypeMirror getAssignedPrivateFieldATM(Receiver r) {
        return assignedPrivateFields.get(r);
    }

}
