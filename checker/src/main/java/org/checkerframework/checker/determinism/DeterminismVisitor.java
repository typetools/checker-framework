package org.checkerframework.checker.determinism;

import com.sun.source.tree.Tree;
import java.util.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.determinism.qual.*;
import org.checkerframework.common.basetype.*;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

public class DeterminismVisitor extends BaseTypeVisitor<DeterminismAnnotatedTypeFactory> {
    public DeterminismVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    private static final @CompilerMessageKey String INVALID_ANNOTATION = "invalid.annotation";
    private static final @CompilerMessageKey String INVALID_ANNOTATION_SUBTYPE =
            "invalid.parameter.type";

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<AnnotationMirror> exceptionParam = AnnotationUtils.createAnnotationSet();
        exceptionParam.add(atypeFactory.DET);
        return exceptionParam;
    }

    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        DeclaredType javaType = useType.getUnderlyingType();

        ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();
        if (useType.hasAnnotation(AnnotationBuilder.fromClass(elements, OrderNonDet.class))) {
            if (!(atypeFactory.isCollection(javaType.asElement().asType()))) {
                checker.report(Result.failure(INVALID_ANNOTATION), tree);
                return false;
            }
        }

        //Sets and lists
        if (atypeFactory.isCollection(javaType.asElement().asType())
                && javaType.getTypeArguments().size() == 1) {
            AnnotationMirror baseAnnotation = useType.getAnnotations().iterator().next();
            AnnotatedTypeMirror paramType = useType.getTypeArguments().iterator().next();
            Iterator<AnnotationMirror> paramAnnotationIt = paramType.getAnnotations().iterator();
            if (paramAnnotationIt.hasNext()) {
                AnnotationMirror paramAnnotation = paramAnnotationIt.next();
                if (isAnnoSubType(baseAnnotation, paramAnnotation))
                    checker.report(
                            Result.failure(
                                    INVALID_ANNOTATION_SUBTYPE, paramAnnotation, baseAnnotation),
                            tree);
                return false;
            }
        }

        return true;
    }

    private boolean isAnnoSubType(AnnotationMirror baseAnno, AnnotationMirror paramAnno) {
        AnnotationMirror DetTypeMirror = AnnotationBuilder.fromClass(elements, Det.class);
        AnnotationMirror OrderNonDetTypeMirror =
                AnnotationBuilder.fromClass(elements, OrderNonDet.class);
        AnnotationMirror NonDetTypeMirror = AnnotationBuilder.fromClass(elements, NonDet.class);

        if (types.isSameType(
                        baseAnno.getAnnotationType(), OrderNonDetTypeMirror.getAnnotationType())
                && types.isSameType(
                        paramAnno.getAnnotationType(), NonDetTypeMirror.getAnnotationType())) {
            return true;
        }

        if (types.isSameType(baseAnno.getAnnotationType(), DetTypeMirror.getAnnotationType())) {
            if (types.isSameType(
                            paramAnno.getAnnotationType(),
                            OrderNonDetTypeMirror.getAnnotationType())
                    || types.isSameType(
                            paramAnno.getAnnotationType(), NonDetTypeMirror.getAnnotationType()))
                return true;
        }
        return false;
    }

    //    public static boolean isCollection(AnnotatedTypeMirror atm, ProcessingEnvironment processingEnvironment) {
    //        javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();
    //        //List and subclasses
    //        TypeMirror ListTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        List.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror ArrayListTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        ArrayList.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror LinkedListTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        LinkedList.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror AbstractListTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        AbstractList.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror AbstractSequentialListTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        AbstractSequentialList.class,
    //                        types,
    //                        processingEnvironment.getElementUtils());
    //        TypeMirror ArraysTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        Arrays.class, types, processingEnvironment.getElementUtils());
    //        //Set and subclasses
    //        TypeMirror SetTypeMirror =
    //                TypesUtils.typeFromClass(Set.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror AbstractSetTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        AbstractSet.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror EnumSetTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        EnumSet.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror HashSetTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        HashSet.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror LinkedHashSetTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        LinkedHashSet.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror TreeSetTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        TreeSet.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror SortedSetTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        SortedSet.class, types, processingEnvironment.getElementUtils());
    //        TypeMirror NavigableSetTypeMirror =
    //                TypesUtils.typeFromClass(
    //                        NavigableSet.class, types, processingEnvironment.getElementUtils());
    //
    //        if (types.isSubtype(tm, ListTypeMirror)
    //                || types.isSubtype(tm, SetTypeMirror)
    //                || types.isSubtype(tm, ArrayListTypeMirror)
    //                || types.isSubtype(tm, HashSetTypeMirror)
    //                || types.isSubtype(tm, AbstractListTypeMirror)
    //                || types.isSubtype(tm, AbstractSequentialListTypeMirror)
    //                || types.isSubtype(tm, LinkedListTypeMirror)
    //                || types.isSubtype(tm, ArraysTypeMirror)
    //                || types.isSubtype(tm, AbstractSetTypeMirror)
    //                || types.isSubtype(tm, EnumSetTypeMirror)
    //                || types.isSubtype(tm, LinkedHashSetTypeMirror)
    //                || types.isSubtype(tm, TreeSetTypeMirror)
    //                || types.isSubtype(tm, SortedSetTypeMirror)
    //                || types.isSubtype(tm, NavigableSetTypeMirror)) {
    //            System.out.println("is it really a collection??? Or is it not??? " + tm + " => true");
    //            return true;
    //// }
    //        System.out.println("is it really a collection??? Or is it not??? " + tm + " => false");
    //        return false;
    //    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type, Tree tree) {
        Set<AnnotationMirror> annos = type.getAnnotations();
        if (annos.contains(AnnotationBuilder.fromClass(elements, OrderNonDet.class)))
            checker.report(Result.failure(INVALID_ANNOTATION), tree);
        return super.isValidUse(type, tree);
    }

    @Override
    public boolean isValidUse(AnnotatedTypeMirror.AnnotatedArrayType type, Tree tree) {
        Set<AnnotationMirror> annos = type.getAnnotations();
        //TODO: Allow OrderNonDet array?
        if (annos.contains(AnnotationBuilder.fromClass(elements, OrderNonDet.class)))
            checker.report(Result.failure(INVALID_ANNOTATION), tree);
        //Do not allow arrays of type @NonDet Object @Det []
        AnnotationMirror arrayType;
        AnnotationMirror elementType;
        if(type.getAnnotations().size() > 0 && type.getComponentType().getAnnotations().size() > 0){
            arrayType = type.getAnnotations().iterator().next();
            elementType = type.getComponentType().getAnnotations().iterator().next();
            if(isAnnoSubType(arrayType, elementType)){
                checker.report(
                        Result.failure(
                                INVALID_ANNOTATION_SUBTYPE, elementType, arrayType),
                        tree);
            }
        }
        return super.isValidUse(type, tree);
    }

    @Override
    protected TypeValidator createTypeValidator() {
        return new BaseTypeValidator(checker, this, atypeFactory) {
            @Override
            protected void reportInvalidAnnotationsOnUse(AnnotatedTypeMirror type, Tree p) {}
        };
    }
}
