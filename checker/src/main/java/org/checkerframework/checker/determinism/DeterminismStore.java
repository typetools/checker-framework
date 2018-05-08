//package org.checkerframework.checker.determinism;
//
//import org.checkerframework.checker.initialization.InitializationStore;
//import org.checkerframework.checker.determinism.DeterminismValue;
//import org.checkerframework.checker.determinism.qual.*;
//import org.checkerframework.dataflow.cfg.CFGVisualizer;
//import org.checkerframework.framework.flow.CFAbstractAnalysis;
//import org.checkerframework.framework.flow.CFAbstractStore;
//
///**
// * Behaves like {@link InitializationStore}, but additionally tracks whether {@link PolyDet} is
// * known to be {@link OrderNonDet}.
// */
//public class DeterminismStore extends InitializationStore<DeterminismValue, DeterminismStore> {
//
//    protected boolean isPolyDetOrderNonDet;
//
//    public DeterminismStore(
//            CFAbstractAnalysis<DeterminismValue, DeterminismStore, ?> analysis,
//            boolean sequentialSemantics) {
//        super(analysis, sequentialSemantics);
//        isPolyDetOrderNonDet = false;
//    }
//
//    public DeterminismStore(DeterminismStore s) {
//        super(s);
//        isPolyDetOrderNonDet = s.isPolyDetOrderNonDet;
//    }
//
//    @Override
//    public DeterminismStore leastUpperBound(DeterminismStore other) {
//        DeterminismStore lub = super.leastUpperBound(other);
//        if (isPolyDetOrderNonDet == other.isPolyDetOrderNonDet) {
//            lub.isPolyDetOrderNonDet = isPolyDetOrderNonDet;
//        } else {
//            lub.isPolyDetOrderNonDet = false;
//        }
//        return lub;
//    }
//
//    @Override
//    protected boolean supersetOf(CFAbstractStore<DeterminismValue, DeterminismStore> o) {
//        DeterminismStore other = (DeterminismStore) o;
//        if (other.isPolyDetOrderNonDet != isPolyDetOrderNonDet) {
//            return false;
//        }
//        return super.supersetOf(other);
//    }
//
//    @Override
//    protected void internalVisualize(CFGVisualizer<DeterminismValue, DeterminismStore, ?> viz) {
//        super.internalVisualize(viz);
//        viz.visualizeStoreKeyVal("isPolyDetOrderNonDet", isPolyDetOrderNonDet);
//    }
//
//    public boolean isPolyDetOrderNonDet() {
//        return isPolyDetOrderNonDet;
//    }
//
//    public void setPolyDetOrderNonDet(boolean isPolyDetOrderNonDet) {
//        this.isPolyDetOrderNonDet = isPolyDetOrderNonDet;
//    }
//}
