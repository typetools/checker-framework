package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.qualframework.base.TypeMirrorConverter;

/**
 * Created by mcarthur on 10/23/14.
 */
public class QualTransferInput<Q> extends TransferInput<QualValue<Q>, QualStore<Q>> {

    private final TypeMirrorConverter<Q> converter;
    private final QualAnalysis<Q> qualAnalysis;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public QualTransferInput(QualAnalysis<Q> qualAnalysis, Node n, Analysis analysis, QualStore<Q> store, TypeMirrorConverter<Q> converter) {
        super(n, analysis, store);
        this.qualAnalysis = qualAnalysis;
        this.converter = converter;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public QualTransferInput(QualAnalysis<Q> qualAnalysis, Node n, Analysis analysis, QualStore<Q> store1, QualStore<Q> store2, TypeMirrorConverter<Q> converter) {
        super(n, analysis, store1, store2);
        this.qualAnalysis = qualAnalysis;
        this.converter = converter;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public QualValue<Q> getValueOfSubNode(Node n) {
        CFAbstractAnalysis<CFValue,?,?> hack = (CFAbstractAnalysis<CFValue,?,?>)(CFAbstractAnalysis)analysis;
        return qualAnalysis.createAbstractValue(converter.getQualifiedType(hack.getValue(n).getType()));
    }
}