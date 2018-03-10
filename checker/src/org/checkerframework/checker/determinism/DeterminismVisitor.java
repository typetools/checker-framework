package org.checkerframework.checker.determinism;

import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.determinism.qual.Det;
import org.checkerframework.checker.determinism.qual.NonDet;
import org.checkerframework.checker.determinism.qual.OrderNonDet;
import org.checkerframework.common.basetype.*;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TypesUtils;

public class DeterminismVisitor extends BaseTypeVisitor<DeterminismAnnotatedTypeFactory> {
    public DeterminismVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        // TODO Auto-generated method stub
        DeclaredType javaType = useType.getUnderlyingType();

        if (useType.hasAnnotation(AnnotationBuilder.fromClass(elements, OrderNonDet.class))) {
            if (!(isCollection(javaType.asElement().asType()))) {
                checker.report(
                        Result.failure("OrderNonDet - can only be used with collections"), tree);
                return false;
            }
        }

        //Sets and lists
        if (isCollection(javaType.asElement().asType())
                && javaType.getTypeArguments().size() == 1) {
            AnnotationMirror baseAnnotation = useType.getAnnotations().iterator().next();
            AnnotatedTypeMirror paramType = useType.getTypeArguments().iterator().next();
            Iterator<AnnotationMirror> paramAnnotationIt = paramType.getAnnotations().iterator();
            if (paramAnnotationIt.hasNext()) {
                AnnotationMirror paramAnnotation = paramAnnotationIt.next();
                if (isAnnoSubType(baseAnnotation, paramAnnotation))
                    checker.report(
                            Result.failure(
                                    " Cannot have a "
                                            + baseAnnotation
                                            + " collection of "
                                            + paramAnnotation),
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

    private boolean isCollection(TypeMirror tm) {
        ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();
        javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();

        TypeMirror ListTypeMirror =
                TypesUtils.typeFromClass(
                        List.class, types, processingEnvironment.getElementUtils());
        TypeMirror ArrayListTypeMirror =
                TypesUtils.typeFromClass(
                        ArrayList.class, types, processingEnvironment.getElementUtils());
        TypeMirror SetTypeMirror =
                TypesUtils.typeFromClass(Set.class, types, processingEnvironment.getElementUtils());
        TypeMirror HashSetTypeMirror =
                TypesUtils.typeFromClass(
                        HashSet.class, types, processingEnvironment.getElementUtils());

        if (types.isSubtype(tm, ListTypeMirror)
                || types.isSubtype(tm, SetTypeMirror)
                || types.isSubtype(tm, ArrayListTypeMirror)
                || types.isSubtype(tm, HashSetTypeMirror)) return true;
        return false;
    }

    private boolean isList(TypeMirror tm) {
        ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();
        javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();

        TypeMirror ListTypeMirror =
                TypesUtils.typeFromClass(
                        List.class, types, processingEnvironment.getElementUtils());
        TypeMirror ArrayListTypeMirror =
                TypesUtils.typeFromClass(
                        ArrayList.class, types, processingEnvironment.getElementUtils());

        if (types.isSubtype(tm, ListTypeMirror) || types.isSubtype(tm, ArrayListTypeMirror))
            return true;
        return false;
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type, Tree tree) {
        // TODO Auto-generated method stub
        Set<AnnotationMirror> annos = type.getAnnotations();
        if (annos.contains(AnnotationBuilder.fromClass(elements, OrderNonDet.class)))
            checker.report(Result.failure("OrderNonDet - can only be used with collections"), tree);
        return super.isValidUse(type, tree);
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            String errorKey) {
        //System.out.println("Assignment: " + varType + " ==> " + valueType + " === " + valueTree);
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
    }

    @Override
    protected TypeValidator createTypeValidator() {
        return new BaseTypeValidator(checker, this, atypeFactory) {
            @Override
            protected void reportInvalidAnnotationsOnUse(AnnotatedTypeMirror type, Tree p) {}
        };
    }
}
