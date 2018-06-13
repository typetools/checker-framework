package org.checkerframework.checker.determinism;

import com.sun.source.tree.*;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.determinism.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.*;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.poly.QualifierPolymorphism;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.*;

public class DeterminismAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    public final AnnotationMirror POLYDET, POLYDET_USE;
    public final AnnotationMirror ORDERNONDET =
            AnnotationBuilder.fromClass(elements, OrderNonDet.class);
    public final AnnotationMirror NONDET = AnnotationBuilder.fromClass(elements, NonDet.class);
    public final AnnotationMirror DET = AnnotationBuilder.fromClass(elements, Det.class);

    public DeterminismAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, PolyDet.class);
        builder.setValue("value", "use");
        POLYDET_USE = builder.build();
        AnnotationBuilder builder2 = new AnnotationBuilder(processingEnv, PolyDet.class);
        builder2.setValue("value", "");
        POLYDET = builder2.build();
        postInit();
    }

    @Override
    public QualifierPolymorphism createQualifierPolymorphism() {
        return new DeterminismQualifierPolymorphism(processingEnv, this);
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new DeterminismTransfer((CFAnalysis) analysis);
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

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                super.createTypeAnnotator(),
                new DeterminismAnnotatedTypeFactory.DeterminismTypeAnnotator(this));
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
            if (node == null) return super.visitMethodInvocation(node, p);
            AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(node);
            if (receiver == null) {
                return super.visitMethodInvocation(node, p);
            }
            AnnotatedTypeMirror.AnnotatedExecutableType invokedMethod =
                    atypeFactory.methodFromUse(node).first;
            ExecutableElement invokedMethodElement = invokedMethod.getElement();

            //Static methods with no arguments: return type should be @Det, not @polyDet
            if (ElementUtils.isStatic(invokedMethodElement) && node.getArguments().size() == 0) {
                if (!p.hasExplicitAnnotation(NONDET)
                        && !p.hasExplicitAnnotation(DET)
                        && !p.hasExplicitAnnotation(ORDERNONDET)) {
                    p.replaceAnnotation(DET);
                }
            }
            if (TypesUtils.getTypeElement(receiver.getUnderlyingType()) == null) {
                return super.visitMethodInvocation(node, p);
            }
            return super.visitMethodInvocation(node, p);
        }

        //        @Override
        //        public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
        //            //Main method parameters must be annotated @Det.
        //            Void ret = super.visitMethod(node, p);
        //            if (node.getName().toString().equals("main")
        //                    && node.getReturnType().toString().equals("void")
        //                    && node.getParameters().size() == 1
        //                    && node.getParameters().get(0).getType().toString().equals("String[]")
        //                    && node.getModifiers().toString().contains("public static")) {
        //                ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);
        //                AnnotatedTypeMirror.AnnotatedArrayType annotatedType =
        //                        (AnnotatedTypeMirror.AnnotatedArrayType)
        //                                atypeFactory
        //                                        .getAnnotatedType(methodElement)
        //                                        .getParameterTypes()
        //                                        .get(0);
        //                annotatedType.replaceAnnotation(DET);
        //                System.out.println(annotatedType);
        //                System.out.println(
        //                        atypeFactory.getAnnotatedType(methodElement).getParameterTypes().get(0));
        //            }
        //            return ret;
        //        }
    }

    protected class DeterminismTypeAnnotator extends TypeAnnotator {
        public DeterminismTypeAnnotator(DeterminismAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitExecutable(
                final AnnotatedTypeMirror.AnnotatedExecutableType t, final Void p) {
            ExecutableElement elem = t.getElement();
            if (elem.getSimpleName().toString().equals("main")) {
                if (elem.getModifiers().contains(Modifier.PUBLIC)
                        && elem.getModifiers().contains(Modifier.STATIC)) {
                    if (t.getParameterTypes().size() == 1
                            && t.getParameterTypes().get(0).getKind() == TypeKind.ARRAY
                            && t.getReturnType().getKind() == TypeKind.VOID) {
                        AnnotatedTypeMirror paramType = t.getParameterTypes().get(0);
                        paramType.replaceAnnotation(DET);
                        System.out.println("here: " + paramType + " ==> " + t);
                    }
                }
            }
            return super.visitExecutable(t, p);
        }
    }

    public boolean isCollection(TypeMirror tm) {
        javax.lang.model.util.Types types = processingEnv.getTypeUtils();
        //List and subclasses
        TypeMirror ListTypeMirror =
                TypesUtils.typeFromClass(List.class, types, processingEnv.getElementUtils());
        TypeMirror ArrayListTypeMirror =
                TypesUtils.typeFromClass(ArrayList.class, types, processingEnv.getElementUtils());
        TypeMirror LinkedListTypeMirror =
                TypesUtils.typeFromClass(LinkedList.class, types, processingEnv.getElementUtils());
        TypeMirror AbstractListTypeMirror =
                TypesUtils.typeFromClass(
                        AbstractList.class, types, processingEnv.getElementUtils());
        TypeMirror AbstractSequentialListTypeMirror =
                TypesUtils.typeFromClass(
                        AbstractSequentialList.class, types, processingEnv.getElementUtils());
        TypeMirror ArraysTypeMirror =
                TypesUtils.typeFromClass(Arrays.class, types, processingEnv.getElementUtils());
        //Set and subclasses
        TypeMirror SetTypeMirror =
                TypesUtils.typeFromClass(Set.class, types, processingEnv.getElementUtils());
        TypeMirror AbstractSetTypeMirror =
                TypesUtils.typeFromClass(AbstractSet.class, types, processingEnv.getElementUtils());
        TypeMirror EnumSetTypeMirror =
                TypesUtils.typeFromClass(EnumSet.class, types, processingEnv.getElementUtils());
        TypeMirror HashSetTypeMirror =
                TypesUtils.typeFromClass(HashSet.class, types, processingEnv.getElementUtils());
        TypeMirror LinkedHashSetTypeMirror =
                TypesUtils.typeFromClass(
                        LinkedHashSet.class, types, processingEnv.getElementUtils());
        TypeMirror TreeSetTypeMirror =
                TypesUtils.typeFromClass(TreeSet.class, types, processingEnv.getElementUtils());
        TypeMirror SortedSetTypeMirror =
                TypesUtils.typeFromClass(SortedSet.class, types, processingEnv.getElementUtils());
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
