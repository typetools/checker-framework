package org.checkerframework.checker.determinism;

import com.sun.source.tree.*;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
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

            //For Collections: equals on different types (Ex: List and Set) is always false
            //Return type is @Det.
            if (invokedMethodElement.getSimpleName().toString().equals("equals")) {
                TypeMirror receiverType =
                        TypesUtils.getTypeElement(receiver.getUnderlyingType()).asType();
                if (isCollection(receiverType)) {
                    System.out.println(invokedMethodElement.getParameters().get(0).asType());
                }
            }

            return super.visitMethodInvocation(node, p);
        }
    }

    protected class DeterminismTypeAnnotator extends TypeAnnotator {
        public DeterminismTypeAnnotator(DeterminismAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitExecutable(
                final AnnotatedTypeMirror.AnnotatedExecutableType t, final Void p) {
            if (isMainMethod(t.getElement())) {
                AnnotatedTypeMirror paramType = t.getParameterTypes().get(0);
                paramType.replaceAnnotation(DET);
                System.out.println("here: " + paramType + " ==> " + t);
            }
            return super.visitExecutable(t, p);
        }
    }

    /** @return true if {@code method} is a main method */
    private static boolean isMainMethod(ExecutableElement method) {
        if (method.getReturnType().getKind() == TypeKind.VOID
                && method.getSimpleName().contentEquals("main")
                && method.getParameters().size() == 1
                && method.getParameters().get(0).asType().getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) method.getParameters().get(0).asType();
            if (TypesUtils.isString(arrayType.getComponentType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        if (elt.getKind() == ElementKind.PARAMETER) {
            if (elt.getEnclosingElement().getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) elt.getEnclosingElement();
                if (isMainMethod(method)) {
                    type.addMissingAnnotations(Collections.singleton(DET));
                }
            }
        }
        super.addComputedTypeAnnotations(elt, type);
    }

    public boolean isCollection(TypeMirror tm) {
        javax.lang.model.util.Types types = processingEnv.getTypeUtils();
        //Collection
        TypeMirror CollectionTypeMirror =
                TypesUtils.typeFromClass(Collection.class, types, processingEnv.getElementUtils());
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

        if (types.isSubtype(tm, CollectionTypeMirror)
                || types.isSubtype(tm, ListTypeMirror)
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

    public boolean isIterator(TypeMirror tm) {
        TypeMirror IteratorTypeMirror =
                TypesUtils.typeFromClass(Iterator.class, types, processingEnv.getElementUtils());
        if (types.isSubtype(tm, IteratorTypeMirror)) {
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
