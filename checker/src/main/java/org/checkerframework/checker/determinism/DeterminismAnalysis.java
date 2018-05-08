//package org.checkerframework.checker.determinism;
//
//import org.checkerframework.common.basetype.BaseTypeChecker;
//import org.checkerframework.framework.flow.CFAbstractAnalysis;
//import org.checkerframework.framework.flow.CFAbstractValue;
//import org.checkerframework.javacutil.Pair;
//
//import javax.lang.model.element.AnnotationMirror;
//import javax.lang.model.element.VariableElement;
//import javax.lang.model.type.TypeMirror;
//import java.util.List;
//import java.util.Set;
//
///**
// * The analysis class for the determinism type system (serves as factory for the transfer function,
// * stores and abstract values.
// */
//public class DeterminismAnalysis
//        extends CFAbstractAnalysis<DeterminismValue, DeterminismStore, DeterminismTransfer> {
//
//    public DeterminismAnalysis(
//            BaseTypeChecker checker,
//            DeterminismAnnotatedTypeFactory factory,
//            List<Pair<VariableElement, DeterminismValue>> fieldValues) {
//        super(checker, factory, fieldValues);
//    }
//
//    @Override
//    public DeterminismStore createEmptyStore(boolean sequentialSemantics) {
//        return new DeterminismStore(this, sequentialSemantics);
//    }
//
//    @Override
//    public DeterminismStore createCopiedStore(DeterminismStore s) {
//        return new DeterminismStore(s);
//    }
//
//    @Override
//    public DeterminismValue createAbstractValue(
//            Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
//        if (!CFAbstractValue.validateSet(annotations, underlyingType, qualifierHierarchy)) {
//            return null;
//        }
//        return new DeterminismValue(this, annotations, underlyingType);
//    }
//}
