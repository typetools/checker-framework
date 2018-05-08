//package org.checkerframework.checker.determinism;
//
//import org.checkerframework.checker.determinism.qual.OrderNonDet;
//import org.checkerframework.checker.determinism.qual.PolyDet;
//import org.checkerframework.framework.flow.CFAbstractAnalysis;
//import org.checkerframework.framework.flow.CFAbstractValue;
//import org.checkerframework.framework.flow.CFValue;
//
//import javax.lang.model.element.AnnotationMirror;
//import javax.lang.model.type.TypeMirror;
//import java.util.Set;
//
///**
// * Behaves just like {@link CFValue}, but additionally tracks whether at this point {@link PolyDet}
// * is known to be {@link OrderNonDet}.
// */
//public class DeterminismValue extends CFAbstractValue<DeterminismValue> {
//
//    protected boolean isPolyDetOrderNonDet;
//
//    public DeterminismValue(
//            CFAbstractAnalysis<DeterminismValue, ?, ?> analysis,
//            Set<AnnotationMirror> annotations,
//            TypeMirror underlyingType) {
//        super(analysis, annotations, underlyingType);
//    }
//}