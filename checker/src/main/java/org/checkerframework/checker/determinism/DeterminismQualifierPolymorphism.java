package org.checkerframework.checker.determinism;

import java.util.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.poly.AbstractQualifierPolymorphism;
import org.checkerframework.framework.type.poly.DefaultQualifierPolymorphism;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.TypesUtils;

public class DeterminismQualifierPolymorphism
        extends DefaultQualifierPolymorphism { // AbstractQualifierPolymorphism {

    Elements elements;
    ProcessingEnvironment env;
    DeterminismAnnotatedTypeFactory factory;
    AnnotationMirror POLYDET_USE;
    AnnotationMirror POLYDET;
    AnnotationMirror POLYDET_UP;
    AnnotationMirror POLYDET_DOWN;
    /**
     * Creates an {@link AbstractQualifierPolymorphism} instance that uses the given checker for
     * querying type qualifiers and the given factory for getting annotated types. Subclass need to
     * add polymorphic qualifiers to {@code polyQual}.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    //    public DeterminismQualifierPolymorphism(
    //            ProcessingEnvironment env, DeterminismAnnotatedTypeFactory factory) {
    //        super(env, factory);
    //        this.env = env;
    //        this.factory = factory;
    //        elements = env.getElementUtils();
    //        POLYDET = factory.POLYDET;
    //        POLYDET_USE = factory.POLYDET_USE;
    //        polyQuals.put(POLYDET, factory.NONDET);
    //        polyQuals.put(POLYDET_USE, factory.NONDET);
    //        polyQuals.put(POLYDET_UP, factory.POLYDET_UP);
    //        polyQuals.put(POLYDET_DOWN, factory.POLYDET_DOWN);
    //    }

    public DeterminismQualifierPolymorphism(
            ProcessingEnvironment env, DeterminismAnnotatedTypeFactory factory) {
        super(env, factory);
        this.env = env;
        this.factory = factory;
        POLYDET = factory.POLYDET;
        POLYDET_USE = factory.POLYDET_USE;
        POLYDET_UP = factory.POLYDET_UP;
        POLYDET_DOWN = factory.POLYDET_DOWN;
        //        this.polyQuals.put(factory.POLYDET_USE, factory.NONDET);
    }

    @Override
    protected void replace(
            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> matches) {
        boolean polyUp = false;
        boolean polyDown = false;
        if (type.hasAnnotation(POLYDET_UP)) {
            polyUp = true;
            type.replaceAnnotation(POLYDET);
        } else if (type.hasAnnotation(POLYDET_DOWN)) {
            polyDown = true;
            type.replaceAnnotation(POLYDET);
        }
        for (Map.Entry<AnnotationMirror, AnnotationMirrorSet> pqentry : matches.entrySet()) {
            AnnotationMirror poly = pqentry.getKey();
            if (poly != null && (type.hasAnnotation(poly) || type.hasAnnotation(POLYDET_USE))) {
                type.removeAnnotation(poly);
                AnnotationMirrorSet quals = pqentry.getValue();
                type.replaceAnnotations(quals);

                if (type.hasAnnotation(factory.ORDERNONDET)) {
                    if (polyUp) {
                        replaceOrderNonDet(type, factory.NONDET);
                    }
                    if (polyDown) {
                        replaceOrderNonDet(type, factory.DET);
                    }
                }

                //                if (polyUp && type.hasAnnotation(factory.ORDERNONDET)) {
                //                    TypeMirror underlyingType =
                // TypesUtils.getTypeElement(type.getUnderlyingType()).asType();
                //                    type.replaceAnnotation(factory.NONDET);
                //                    AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
                //                            (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
                //                    boolean isCollIter = false;
                //                    if (factory.isCollection(underlyingType) ||
                // factory.isIterator(underlyingType)) {
                //                        isCollIter = true;
                //                    }
                //                    while(isCollIter){
                //                        //Replace all @OrderNonDet type parameters with @Det
                //                        Iterator<AnnotatedTypeMirror> it =
                // declaredType.getTypeArguments().iterator();
                //                        while (it.hasNext()) {
                //                            AnnotatedTypeMirror argType = it.next();
                //                            if (argType.hasAnnotation(factory.ORDERNONDET)) {
                //                                argType.replaceAnnotation(factory.NONDET);
                //                            }
                //                        }
                //                        TypeMirror declType =
                // TypesUtils.getTypeElement(declaredType.getTypeArguments().get(0).getUnderlyingType()).asType();
                //                        if(factory.isCollection(declType)
                //                                || factory.isIterator(declType)){
                //                            declaredType =
                // (AnnotatedTypeMirror.AnnotatedDeclaredType)
                // declaredType.getTypeArguments().get(0);
                //                        }
                //                        else{
                //                            isCollIter = false;
                //                        }
                //                    }
                //                } else if (polyDown && type.hasAnnotation(factory.ORDERNONDET)) {
                //                    TypeMirror underlyingType =
                // TypesUtils.getTypeElement(type.getUnderlyingType()).asType();
                //                    type.replaceAnnotation(factory.DET);
                //                    AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
                //                            (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
                //                    boolean isCollIter = false;
                //                    if (factory.isCollection(underlyingType) ||
                // factory.isIterator(underlyingType)) {
                //                        isCollIter = true;
                //                    }
                //                    while(isCollIter){
                //                        //Replace all @OrderNonDet type parameters with @Det
                //                        Iterator<AnnotatedTypeMirror> it =
                // declaredType.getTypeArguments().iterator();
                //                        while (it.hasNext()) {
                //                            AnnotatedTypeMirror argType = it.next();
                //                            if (argType.hasAnnotation(factory.ORDERNONDET)) {
                //                                argType.replaceAnnotation(factory.DET);
                //                            }
                //                        }
                //                        TypeMirror declType =
                // TypesUtils.getTypeElement(declaredType.getTypeArguments().get(0).getUnderlyingType()).asType();
                //                        if(factory.isCollection(declType)
                //                                || factory.isIterator(declType)){
                //                            declaredType =
                // (AnnotatedTypeMirror.AnnotatedDeclaredType)
                // declaredType.getTypeArguments().get(0);
                //                        }
                //                        else{
                //                            isCollIter = false;
                //                        }
                //                    }
                //                }
            }
        }
    }

    private void replaceOrderNonDet(AnnotatedTypeMirror type, AnnotationMirror replaceType) {
        TypeMirror underlyingType = TypesUtils.getTypeElement(type.getUnderlyingType()).asType();
        type.replaceAnnotation(replaceType);
        AnnotatedTypeMirror.AnnotatedDeclaredType declaredType = null;
        boolean isCollIter = false;
        if (factory.isCollection(underlyingType) || factory.isIterator(underlyingType)) {
            declaredType = (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
            isCollIter = true;
        }
        while (isCollIter) {
            // Replace all @OrderNonDet type parameters with @Det or @NonDet
            Iterator<AnnotatedTypeMirror> it = declaredType.getTypeArguments().iterator();
            // Iterate over all the type parameters of this collection
            while (it.hasNext()) {
                AnnotatedTypeMirror argType = it.next();
                if (argType.hasAnnotation(factory.ORDERNONDET)) {
                    argType.replaceAnnotation(replaceType);
                }
            }

            // Assuming a single type parameter (will not work for HashMaps)
            // TODO: Handle all type parameters
            TypeMirror declType =
                    TypesUtils.getTypeElement(
                                    declaredType.getTypeArguments().get(0).getUnderlyingType())
                            .asType();
            if (factory.isCollection(declType) || factory.isIterator(declType)) {
                declaredType =
                        (AnnotatedTypeMirror.AnnotatedDeclaredType)
                                declaredType.getTypeArguments().get(0);
            } else {
                isCollIter = false;
            }
        }
    }

    @Override
    protected AnnotationMirrorSet combine(
            AnnotationMirror polyQual, AnnotationMirrorSet a1Annos, AnnotationMirrorSet a2Annos) {
        if (a1Annos == null) {
            if (a2Annos == null) {
                return new AnnotationMirrorSet();
            }
            return a2Annos;
        } else if (a2Annos == null) {
            return a1Annos;
        }

        AnnotationMirrorSet lubSet = new AnnotationMirrorSet();
        for (AnnotationMirror top : topQuals) {
            AnnotationMirror a1 = qualhierarchy.findAnnotationInHierarchy(a1Annos, top);
            AnnotationMirror a2 = qualhierarchy.findAnnotationInHierarchy(a2Annos, top);
            AnnotationMirror lub = qualhierarchy.leastUpperBoundTypeVariable(a1, a2);
            if (lub != null) {
                lubSet.add(lub);
            }
        }
        return lubSet;
    }
}
