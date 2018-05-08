package org.checkerframework.checker.determinism;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.determinism.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.poly.QualifierPolymorphism;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

public class DeterminismAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    public final AnnotationMirror POLYDET, POLYDET_USE;
    public final AnnotationMirror ORDERNONDET =
            AnnotationBuilder.fromClass(elements, OrderNonDet.class);
    public final AnnotationMirror NONDET = AnnotationBuilder.fromClass(elements, NonDet.class);
    public final AnnotationMirror DET = AnnotationBuilder.fromClass(elements, Det.class);

    //    public final ExecutableElement polyValueElement;

    public DeterminismAnnotatedTypeFactory(BaseTypeChecker checker) {
                super(checker);

        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, PolyDet.class);
        builder.setValue("value", "use");
        POLYDET_USE = builder.build();
        AnnotationBuilder builder2 = new AnnotationBuilder(processingEnv, PolyDet.class);
        builder2.setValue("value", "");
        POLYDET = builder2.build();
        postInit();
        //        polyValueElement =
        //                TreeUtils.getMethod(
        //                        org.checkerframework.checker.determinism.qual.PolyDet.class.getName(),
        //                        "value",
        //                        0,
        //                        processingEnv);
    }

    @Override
    public QualifierPolymorphism createQualifierPolymorphism() {
        return new DeterminismQualifierPolymorphism(processingEnv, this);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(Det.class, OrderNonDet.class, NonDet.class, PolyDet.class));
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new DeterminismTreeAnnotator(this), super.createTreeAnnotator());
    }

    private class DeterminismTreeAnnotator extends TreeAnnotator {

        public DeterminismTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitReturn(ReturnTree node, AnnotatedTypeMirror p) {
            return super.visitReturn(node, p);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, AnnotatedTypeMirror p) {
            if(node == null)
                return super.visitMethodInvocation(node, p);
            AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(node);
            if(receiver == null)
                return super.visitMethodInvocation(node, p);
//            boolean isCollection = isCollection(receiver.getUnderlyingType());
//            if(isCollection){
//                System.out.println("Method name: " + TreeUtils.getMethodName(node));
//            }
            return super.visitMethodInvocation(node, p);
        }
    }

    public boolean isCollection(TypeMirror tm){
        javax.lang.model.util.Types types = processingEnv.getTypeUtils();
        //List and subclasses
        TypeMirror ListTypeMirror =
                TypesUtils.typeFromClass(
                        List.class, types, processingEnv.getElementUtils());
        TypeMirror ArrayListTypeMirror =
                TypesUtils.typeFromClass(
                        ArrayList.class, types, processingEnv.getElementUtils());
        TypeMirror LinkedListTypeMirror =
                TypesUtils.typeFromClass(
                        LinkedList.class, types, processingEnv.getElementUtils());
        TypeMirror AbstractListTypeMirror =
                TypesUtils.typeFromClass(
                        AbstractList.class, types, processingEnv.getElementUtils());
        TypeMirror AbstractSequentialListTypeMirror =
                TypesUtils.typeFromClass(
                        AbstractSequentialList.class,
                        types,
                        processingEnv.getElementUtils());
        TypeMirror ArraysTypeMirror =
                TypesUtils.typeFromClass(
                        Arrays.class, types, processingEnv.getElementUtils());
        //Set and subclasses
        TypeMirror SetTypeMirror =
                TypesUtils.typeFromClass(Set.class, types, processingEnv.getElementUtils());
        TypeMirror AbstractSetTypeMirror =
                TypesUtils.typeFromClass(
                        AbstractSet.class, types, processingEnv.getElementUtils());
        TypeMirror EnumSetTypeMirror =
                TypesUtils.typeFromClass(
                        EnumSet.class, types, processingEnv.getElementUtils());
        TypeMirror HashSetTypeMirror =
                TypesUtils.typeFromClass(
                        HashSet.class, types, processingEnv.getElementUtils());
        TypeMirror LinkedHashSetTypeMirror =
                TypesUtils.typeFromClass(
                        LinkedHashSet.class, types, processingEnv.getElementUtils());
        TypeMirror TreeSetTypeMirror =
                TypesUtils.typeFromClass(
                        TreeSet.class, types, processingEnv.getElementUtils());
        TypeMirror SortedSetTypeMirror =
                TypesUtils.typeFromClass(
                        SortedSet.class, types, processingEnv.getElementUtils());
        TypeMirror NavigableSetTypeMirror =
                TypesUtils.typeFromClass(
                        NavigableSet.class, types, processingEnv.getElementUtils());

        if (types.isSubtype(tm, ListTypeMirror)
                || types.isSubtype(tm, SetTypeMirror)
                || types.isSubtype(tm, ArrayListTypeMirror)
                || types.isSubtype(tm, HashSetTypeMirror)
                || types.isSubtype(tm, AbstractListTypeMirror)
                || types.isSubtype(tm, AbstractSequentialListTypeMirror)
                || types.isSubtype(tm, LinkedListTypeMirror)
                || types.isSubtype(tm, ArraysTypeMirror)
                || types.isSubtype(tm, AbstractSetTypeMirror)
                || types.isSubtype(tm, EnumSetTypeMirror)
                || types.isSubtype(tm, LinkedHashSetTypeMirror)
                || types.isSubtype(tm, TreeSetTypeMirror)
                || types.isSubtype(tm, SortedSetTypeMirror)
                || types.isSubtype(tm, NavigableSetTypeMirror)) {
            return true;
        }
        return false;
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(
            MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
        return new DeterminismQualifierHierarchy(factory, DET);
    }

    class DeterminismQualifierHierarchy extends GraphQualifierHierarchy {

        public DeterminismQualifierHierarchy(MultiGraphFactory f, AnnotationMirror bottom) {
            super(f, bottom);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameIgnoringValues(subAnno, POLYDET)) {
                subAnno = POLYDET;
            }
            if (AnnotationUtils.areSameIgnoringValues(superAnno, POLYDET)) {
                superAnno = POLYDET;
            }
            return super.isSubtype(subAnno, superAnno);
        }
    }
}
