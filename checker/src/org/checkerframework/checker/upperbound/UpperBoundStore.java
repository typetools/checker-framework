package org.checkerframework.checker.upperbound;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.upperbound.qual.LTEqLengthOf;
import org.checkerframework.checker.upperbound.qual.LTLengthOf;
import org.checkerframework.checker.upperbound.qual.LTOMLengthOf;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;

public class UpperBoundStore extends CFAbstractStore<CFValue, UpperBoundStore> {

    protected UpperBoundStore(UpperBoundStore other) {
        super(other);
    }

    public UpperBoundStore(
            CFAbstractAnalysis<CFValue, UpperBoundStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    // If something is removed from a list it reduces the minlen of anything that could be an alias of the list by 1.
    // If a list is cleared, anything that could be an alias of this list goes to UpperBoundUnknown.
    @Override
    public void updateForMethodCall(
            MethodInvocationNode miNode, AnnotatedTypeFactory atypeFactory, CFValue cfValue) {
        Receiver caller =
                FlowExpressions.internalReprOf(atypeFactory, miNode.getTarget().getReceiver());
        UpperBoundAnnotatedTypeFactory factory = (UpperBoundAnnotatedTypeFactory) atypeFactory;
        boolean remove = factory.isListRemove(miNode.getTarget().getMethod());
        boolean clear = factory.isListClear(miNode.getTarget().getMethod());
        boolean add = factory.isListAdd(miNode.getTarget().getMethod());
        Map<Receiver, CFValue> replace = new HashMap<Receiver, CFValue>();
        if (clear) {
            for (FlowExpressions.LocalVariable rec : localVariableValues.keySet()) {
                applyClear(rec, replace, atypeFactory);
            }
            for (FieldAccess rec : fieldValues.keySet()) {
                applyClear(rec, replace, atypeFactory);
            }
        }
        if (remove) {
            for (FlowExpressions.LocalVariable rec : localVariableValues.keySet()) {
                applyRemove(rec, replace, atypeFactory);
            }
            for (FieldAccess rec : fieldValues.keySet()) {
                applyRemove(rec, replace, atypeFactory);
            }
        }
        if (add) {
            for (FlowExpressions.LocalVariable rec : localVariableValues.keySet()) {
                applyAdd(rec, replace, atypeFactory, caller);
            }
            for (FieldAccess rec : fieldValues.keySet()) {
                applyAdd(rec, replace, atypeFactory, caller);
            }
        }
        for (Receiver rec : replace.keySet()) {
            replaceValue(rec, replace.get(rec));
        }

        super.updateForMethodCall(miNode, atypeFactory, cfValue);
    }

    private void applyClear(
            Receiver rec, Map<Receiver, CFValue> replace, AnnotatedTypeFactory atypeFactory) {

        UpperBoundAnnotatedTypeFactory factory = (UpperBoundAnnotatedTypeFactory) atypeFactory;
        CFValue val = analysis.createSingleAnnotationValue(factory.UNKNOWN, rec.getType());
        replace.put(rec, val);
    }

    private void applyRemove(
            Receiver rec, Map<Receiver, CFValue> replace, AnnotatedTypeFactory atypeFactory) {

        UpperBoundAnnotatedTypeFactory factory = (UpperBoundAnnotatedTypeFactory) atypeFactory;
        CFValue value = this.getValue(rec);
        Set<AnnotationMirror> atm = value.getAnnotations();

        if (AnnotationUtils.containsSameByClass(atm, LTOMLengthOf.class)) {
            AnnotationMirror anno = AnnotationUtils.getAnnotationByClass(atm, LTOMLengthOf.class);
            AnnotationMirror newAnno =
                    factory.createLTLengthOfAnnotation(UpperBoundUtils.getValue(anno));
            CFValue val =
                    analysis.createSingleAnnotationValue(
                            factory.getQualifierHierarchy().leastUpperBound(newAnno, anno),
                            rec.getType());
            replace.put(rec, val);
        } else if (AnnotationUtils.containsSameByClass(atm, LTLengthOf.class)) {
            AnnotationMirror anno = AnnotationUtils.getAnnotationByClass(atm, LTLengthOf.class);
            AnnotationMirror newAnno =
                    factory.createLTEqLengthOfAnnotation(UpperBoundUtils.getValue(anno));
            CFValue val =
                    analysis.createSingleAnnotationValue(
                            factory.getQualifierHierarchy().leastUpperBound(newAnno, anno),
                            rec.getType());
            replace.put(rec, val);
        } else if (AnnotationUtils.containsSameByClass(atm, LTEqLengthOf.class)) {
            CFValue val = analysis.createSingleAnnotationValue(factory.UNKNOWN, rec.getType());
            replace.put(rec, val);
        }
    }

    private void applyAdd(
            Receiver rec,
            Map<Receiver, CFValue> replace,
            AnnotatedTypeFactory atypeFactory,
            Receiver caller) {

        UpperBoundAnnotatedTypeFactory factory = (UpperBoundAnnotatedTypeFactory) atypeFactory;
        CFValue value = this.getValue(rec);
        Set<AnnotationMirror> atm = value.getAnnotations();
        if (AnnotationUtils.containsSameByClass(atm, LTEqLengthOf.class)) {
            AnnotationMirror anno = AnnotationUtils.getAnnotationByClass(atm, LTEqLengthOf.class);
            String[] vals = UpperBoundUtils.getValue(anno);
            if (vals.length != 1 || !vals[0].equals(caller.toString())) {
                return;
            }
            AnnotationMirror newAnno = factory.createLTLengthOfAnnotation(caller.toString());
            CFValue val =
                    analysis.createSingleAnnotationValue(
                            factory.getQualifierHierarchy().greatestLowerBound(newAnno, anno),
                            rec.getType());
            replace.put(rec, val);
        } else if (AnnotationUtils.containsSameByClass(atm, LTLengthOf.class)) {
            AnnotationMirror anno = AnnotationUtils.getAnnotationByClass(atm, LTLengthOf.class);
            String[] vals = UpperBoundUtils.getValue(anno);
            if (vals.length != 1 || !vals[0].equals(caller.toString())) {
                return;
            }
            AnnotationMirror newAnno = factory.createLTOMLengthOfAnnotation(caller.toString());
            CFValue val =
                    analysis.createSingleAnnotationValue(
                            factory.getQualifierHierarchy().greatestLowerBound(newAnno, anno),
                            rec.getType());
            replace.put(rec, val);
        }
    }

    @Override
    public String toString() {
        String res = "";
        for (LocalVariable k : this.localVariableValues.keySet()) {
            CFValue anno = localVariableValues.get(k);
            res += k.toString() + ": " + anno.toString();
            res += "\n";
        }
        return res;
    }
}
