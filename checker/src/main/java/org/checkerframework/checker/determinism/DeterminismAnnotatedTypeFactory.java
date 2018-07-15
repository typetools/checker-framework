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
    public final AnnotationMirror POLYDET, POLYDET_USE, POLYDET_UP, POLYDET_DOWN;
    public final AnnotationMirror ORDERNONDET =
            AnnotationBuilder.fromClass(elements, OrderNonDet.class);
    public final AnnotationMirror NONDET = AnnotationBuilder.fromClass(elements, NonDet.class);
    public final AnnotationMirror DET = AnnotationBuilder.fromClass(elements, Det.class);

    public DeterminismAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, PolyDet.class);
        builder.setValue("value", "use");
        POLYDET_USE = builder.build();
        AnnotationBuilder builder_up = new AnnotationBuilder(processingEnv, PolyDet.class);
        builder_up.setValue("value", "up");
        POLYDET_UP = builder_up.build();
        AnnotationBuilder builder_down = new AnnotationBuilder(processingEnv, PolyDet.class);
        builder_down.setValue("value", "down");
        POLYDET_DOWN = builder_down.build();
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
        public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
            for (VariableTree param : node.getParameters()) {
                if (param.getType().getKind() == Tree.Kind.ARRAY_TYPE) {
                    AnnotatedTypeMirror.AnnotatedArrayType paramAnno =
                            (AnnotatedTypeMirror.AnnotatedArrayType) getAnnotatedType(param);
                    if (paramAnno.getExplicitAnnotations().size() == 0) {
                        paramAnno.getComponentType().replaceAnnotation(POLYDET);
                    }
                }
            }
            return super.visitMethod(node, p);
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

            // Static methods with no arguments: return type should be @Det, not @polyDet
            if (ElementUtils.isStatic(invokedMethodElement) && node.getArguments().size() == 0) {
                if (p.getExplicitAnnotations().size() == 0) {
                    p.replaceAnnotation(DET);
                }
            }
            if (TypesUtils.getTypeElement(receiver.getUnderlyingType()) == null) {
                return super.visitMethodInvocation(node, p);
            }

            // If return type (non-array) resolves to @OrderNonDet, replace it with @NonDet
            if (p.getAnnotations().contains(ORDERNONDET)
                    && !(p.getUnderlyingType().getKind() == TypeKind.ARRAY)
                    && !(isCollection(TypesUtils.getTypeElement(p.getUnderlyingType()).asType()))
                    && !(isIterator(TypesUtils.getTypeElement(p.getUnderlyingType()).asType()))) {
                p.replaceAnnotation(NONDET);
            }

            // For Sets: "equals" on @OrderNonDet Sets without @OrderNonDet List type parameter
            // Return type is @Det.
            TypeElement receiverUnderlyingType =
                    TypesUtils.getTypeElement(receiver.getUnderlyingType());
            if (invokedMethodElement.getSimpleName().toString().equals("equals")
                    && isSet(receiverUnderlyingType.asType())
                    && AnnotationUtils.areSame(
                            receiver.getAnnotations().iterator().next(), ORDERNONDET)) {
                // Receiver does not have "@OrderNonDet List" type parameter
                if (!hasOrderNonDetListAsTypeParameter(receiver)) {
                    AnnotatedTypeMirror parameter =
                            atypeFactory.getAnnotatedType(node.getArguments().get(0));
                    if (isSet(TypesUtils.getTypeElement(parameter.getUnderlyingType()).asType())
                            && parameter.hasAnnotation(ORDERNONDET)) {
                        // Parameter - same type as receiver
                        // does not have "@OrderNonDet List" type parameter
                        if (types.isSameType(
                                        receiver.getUnderlyingType(), parameter.getUnderlyingType())
                                && !hasOrderNonDetListAsTypeParameter(parameter)) {
                            p.replaceAnnotation(DET);
                        }
                    }
                }
            }
            return super.visitMethodInvocation(node, p);
        }

        @Override
        public Void visitAssignment(AssignmentTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            System.out.println(
                    "assignment: "
                            + node
                            + " ; "
                            + atypeFactory.getAnnotatedType(node.getExpression()));
            return super.visitAssignment(node, annotatedTypeMirror);
        }

        @Override
        public Void visitMemberSelect(
                MemberSelectTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            // Length of a @NonDet array is @NonDet
            if (TreeUtils.isArrayLengthAccess(node)) {
                AnnotatedTypeMirror.AnnotatedArrayType arrType =
                        (AnnotatedTypeMirror.AnnotatedArrayType)
                                atypeFactory.getAnnotatedType(node.getExpression());
                if (AnnotationUtils.areSame(arrType.getAnnotations().iterator().next(), NONDET)) {
                    annotatedTypeMirror.replaceAnnotation(NONDET);
                }
            }
            return super.visitMemberSelect(node, annotatedTypeMirror);
        }
    }

    private boolean hasOrderNonDetListAsTypeParameter(AnnotatedTypeMirror atm) {
        AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
                (AnnotatedTypeMirror.AnnotatedDeclaredType) atm;
        Iterator<AnnotatedTypeMirror> it = declaredType.getTypeArguments().iterator();
        while (it.hasNext()) {
            AnnotatedTypeMirror argType = it.next();
            if (isList(argType.getUnderlyingType()) && argType.hasAnnotation(ORDERNONDET)) {
                return true;
            }
        }
        return false;
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

    public boolean isSet(TypeMirror tm) {
        javax.lang.model.util.Types types = processingEnv.getTypeUtils();
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

        if (types.isSubtype(tm, SetTypeMirror)
                || types.isSubtype(tm, HashSetTypeMirror)
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

    public boolean isList(TypeMirror tm) {
        // List and subclasses
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
        if (types.isSubtype(tm, ListTypeMirror)
                || types.isSubtype(tm, ArrayListTypeMirror)
                || types.isSubtype(tm, AbstractListTypeMirror)
                || types.isSubtype(tm, AbstractSequentialListTypeMirror)
                || types.isSubtype(tm, LinkedListTypeMirror)) {
            return true;
        }
        return false;
    }

    public boolean isArrays(TypeMirror tm) {
        TypeMirror ArraysTypeMirror =
                TypesUtils.typeFromClass(Arrays.class, types, processingEnv.getElementUtils());
        if (types.isSubtype(tm, ArraysTypeMirror)) {
            return true;
        }
        return false;
    }

    public boolean isCollections(TypeMirror tm) {
        TypeMirror CollectionsTypeMirror =
                TypesUtils.typeFromClass(Collections.class, types, processingEnv.getElementUtils());
        if (types.isSubtype(tm, CollectionsTypeMirror)) {
            return true;
        }
        return false;
    }

    public boolean isCollection(TypeMirror tm) {
        javax.lang.model.util.Types types = processingEnv.getTypeUtils();
        // Collection
        TypeMirror CollectionTypeMirror =
                TypesUtils.typeFromClass(Collection.class, types, processingEnv.getElementUtils());
        TypeMirror AbstractCollectionTypeMirror =
                TypesUtils.typeFromClass(
                        AbstractCollection.class, types, processingEnv.getElementUtils());
        // List and subclasses
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
        // Set and subclasses
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
                || types.isSubtype(tm, AbstractCollectionTypeMirror)
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
        javax.lang.model.util.Types types = processingEnv.getTypeUtils();
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
