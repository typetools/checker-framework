package org.checkerframework.framework.type;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeVisitor;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Finds the direct supertypes of an input AnnotatedTypeMirror. See
 * https://docs.oracle.com/javase/specs/jls/se10/html/jls-4.html#jls-4.10.2
 *
 * @see Types#directSupertypes(TypeMirror)
 */
class SupertypeFinder {

    // Version of method below for declared types
    /** @see Types#directSupertypes(TypeMirror) */
    public static List<AnnotatedDeclaredType> directSuperTypes(AnnotatedDeclaredType type) {
        SupertypeFindingVisitor supertypeFindingVisitor =
                new SupertypeFindingVisitor(type.atypeFactory);
        List<AnnotatedDeclaredType> supertypes = supertypeFindingVisitor.visitDeclared(type, null);
        type.atypeFactory.postDirectSuperTypes(type, supertypes);
        return supertypes;
    }

    // Version of method above for all types
    /** @see Types#directSupertypes(TypeMirror) */
    public static final List<? extends AnnotatedTypeMirror> directSuperTypes(
            AnnotatedTypeMirror type) {
        SupertypeFindingVisitor supertypeFindingVisitor =
                new SupertypeFindingVisitor(type.atypeFactory);
        List<? extends AnnotatedTypeMirror> supertypes = supertypeFindingVisitor.visit(type, null);
        type.atypeFactory.postDirectSuperTypes(type, supertypes);
        return supertypes;
    }

    private static class SupertypeFindingVisitor
            extends SimpleAnnotatedTypeVisitor<List<? extends AnnotatedTypeMirror>, Void> {
        private final Types types;
        private final AnnotatedTypeFactory atypeFactory;
        private final TypeParamReplacer typeParamReplacer;

        SupertypeFindingVisitor(AnnotatedTypeFactory atypeFactory) {
            this.atypeFactory = atypeFactory;
            this.types = atypeFactory.types;
            this.typeParamReplacer = new TypeParamReplacer(types);
        }

        @Override
        public List<AnnotatedTypeMirror> defaultAction(AnnotatedTypeMirror t, Void p) {
            return new ArrayList<>();
        }

        /**
         * Primitive Rules:
         *
         * <pre>{@code
         * double >1 float
         * float >1 long
         * long >1 int
         * int >1 char
         * int >1 short
         * short >1 byte
         * }</pre>
         *
         * For easiness:
         *
         * <pre>{@code
         * boxed(primitiveType) >: primitiveType
         * }</pre>
         */
        @Override
        public List<AnnotatedTypeMirror> visitPrimitive(AnnotatedPrimitiveType type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<>();
            Set<AnnotationMirror> annotations = type.getAnnotations();

            // Find Boxed type
            TypeElement boxed = types.boxedClass(type.getUnderlyingType());
            AnnotatedDeclaredType boxedType = atypeFactory.getAnnotatedType(boxed);
            boxedType.replaceAnnotations(annotations);
            superTypes.add(boxedType);

            TypeKind superPrimitiveType = null;

            if (type.getKind() == TypeKind.BOOLEAN) {
                // Nothing
            } else if (type.getKind() == TypeKind.BYTE) {
                superPrimitiveType = TypeKind.SHORT;
            } else if (type.getKind() == TypeKind.CHAR) {
                superPrimitiveType = TypeKind.INT;
            } else if (type.getKind() == TypeKind.DOUBLE) {
                // Nothing
            } else if (type.getKind() == TypeKind.FLOAT) {
                superPrimitiveType = TypeKind.DOUBLE;
            } else if (type.getKind() == TypeKind.INT) {
                superPrimitiveType = TypeKind.LONG;
            } else if (type.getKind() == TypeKind.LONG) {
                superPrimitiveType = TypeKind.FLOAT;
            } else if (type.getKind() == TypeKind.SHORT) {
                superPrimitiveType = TypeKind.INT;
            } else {
                assert false : "Forgot the primitive " + type;
            }

            if (superPrimitiveType != null) {
                AnnotatedPrimitiveType superPrimitive =
                        (AnnotatedPrimitiveType)
                                atypeFactory.toAnnotatedType(
                                        types.getPrimitiveType(superPrimitiveType), false);
                superPrimitive.addAnnotations(annotations);
                superTypes.add(superPrimitive);
            }

            return superTypes;
        }

        @Override
        public List<AnnotatedDeclaredType> visitDeclared(AnnotatedDeclaredType type, Void p) {
            List<AnnotatedDeclaredType> supertypes = new ArrayList<>();
            // Set<AnnotationMirror> annotations = type.getAnnotations();

            TypeElement typeElement = (TypeElement) type.getUnderlyingType().asElement();

            // Mapping of type variable to actual types
            Map<TypeParameterElement, AnnotatedTypeMirror> mapping = new HashMap<>();

            if (type.getTypeArguments().size() != typeElement.getTypeParameters().size()) {
                if (!type.wasRaw()) {
                    throw new BugInCF(
                            "AnnotatedDeclaredType's element has a different number of type parameters than type.\n"
                                    + "type="
                                    + type
                                    + "\n"
                                    + "element="
                                    + typeElement);
                }
            }

            AnnotatedDeclaredType enclosing = type;
            while (enclosing != null) {
                TypeElement enclosingTypeElement =
                        (TypeElement) enclosing.getUnderlyingType().asElement();
                List<AnnotatedTypeMirror> typeArgs = enclosing.getTypeArguments();
                List<? extends TypeParameterElement> typeParams =
                        enclosingTypeElement.getTypeParameters();
                for (int i = 0; i < enclosing.getTypeArguments().size(); ++i) {
                    AnnotatedTypeMirror typArg = typeArgs.get(i);
                    TypeParameterElement ele = typeParams.get(i);
                    mapping.put(ele, typArg);
                }

                enclosing = enclosing.getEnclosingType();
            }

            ClassTree classTree = atypeFactory.trees.getTree(typeElement);
            // Testing against enum and annotation. Ideally we can simply use element!
            if (classTree != null) {
                supertypes.addAll(supertypesFromTree(type, classTree));
            } else {
                supertypes.addAll(supertypesFromElement(type, typeElement));
                // final Element elem = type.getElement() == null ? typeElement : type.getElement();
            }

            if (typeElement.getKind() == ElementKind.ANNOTATION_TYPE) {
                TypeElement jlaElement =
                        atypeFactory.elements.getTypeElement(Annotation.class.getCanonicalName());
                AnnotatedDeclaredType jlaAnnotation = atypeFactory.fromElement(jlaElement);
                jlaAnnotation.addAnnotations(type.getAnnotations());
                supertypes.add(jlaAnnotation);
            }

            for (AnnotatedDeclaredType dt : supertypes) {
                typeParamReplacer.visit(dt, mapping);
            }

            return supertypes;
        }

        private List<AnnotatedDeclaredType> supertypesFromElement(
                AnnotatedDeclaredType type, TypeElement typeElement) {
            List<AnnotatedDeclaredType> supertypes = new ArrayList<>();
            // Find the super types: Start with enums and superclass
            if (typeElement.getKind() == ElementKind.ENUM) {
                DeclaredType dt = (DeclaredType) typeElement.getSuperclass();
                AnnotatedDeclaredType adt =
                        (AnnotatedDeclaredType) atypeFactory.toAnnotatedType(dt, false);

                List<AnnotatedTypeMirror> tas = adt.getTypeArguments();
                List<AnnotatedTypeMirror> newtas = new ArrayList<>();
                for (AnnotatedTypeMirror t : tas) {
                    // If the type argument of super is the same as the input type
                    if (atypeFactory.types.isSameType(
                            t.getUnderlyingType(), type.getUnderlyingType())) {
                        t.addAnnotations(type.getAnnotations());
                        newtas.add(t);
                    }
                }
                adt.setTypeArguments(newtas);

                supertypes.add(adt);

            } else if (typeElement.getSuperclass().getKind() != TypeKind.NONE) {
                DeclaredType superClass = (DeclaredType) typeElement.getSuperclass();
                AnnotatedDeclaredType dt =
                        (AnnotatedDeclaredType) atypeFactory.toAnnotatedType(superClass, false);
                supertypes.add(dt);

            } else if (!ElementUtils.isObject(typeElement)) {
                supertypes.add(AnnotatedTypeMirror.createTypeOfObject(atypeFactory));
            }

            for (TypeMirror st : typeElement.getInterfaces()) {
                if (type.wasRaw()) {
                    st = types.erasure(st);
                }
                AnnotatedDeclaredType ast =
                        (AnnotatedDeclaredType) atypeFactory.toAnnotatedType(st, false);
                supertypes.add(ast);
                if (type.wasRaw()) {
                    if (st.getKind() == TypeKind.DECLARED) {
                        final List<? extends TypeMirror> typeArgs =
                                ((DeclaredType) st).getTypeArguments();
                        final List<AnnotatedTypeMirror> annotatedTypeArgs = ast.getTypeArguments();
                        for (int i = 0; i < typeArgs.size(); i++) {
                            atypeFactory.addComputedTypeAnnotations(
                                    types.asElement(typeArgs.get(i)), annotatedTypeArgs.get(i));
                        }
                    }
                }
            }
            ElementAnnotationApplier.annotateSupers(supertypes, typeElement);

            if (type.wasRaw()) {
                for (AnnotatedDeclaredType adt : supertypes) {
                    adt.setWasRaw();
                }
            }
            return supertypes;
        }

        private List<AnnotatedDeclaredType> supertypesFromTree(
                AnnotatedDeclaredType type, ClassTree classTree) {
            List<AnnotatedDeclaredType> supertypes = new ArrayList<>();
            if (classTree.getExtendsClause() != null) {
                AnnotatedDeclaredType adt =
                        (AnnotatedDeclaredType)
                                atypeFactory.getAnnotatedTypeFromTypeTree(
                                        classTree.getExtendsClause());
                supertypes.add(adt);
            } else if (!ElementUtils.isObject(TreeUtils.elementFromDeclaration(classTree))) {
                supertypes.add(AnnotatedTypeMirror.createTypeOfObject(atypeFactory));
            }

            for (Tree implemented : classTree.getImplementsClause()) {
                AnnotatedDeclaredType adt =
                        (AnnotatedDeclaredType)
                                atypeFactory.getAnnotatedTypeFromTypeTree(implemented);
                supertypes.add(adt);
            }

            TypeElement elem = TreeUtils.elementFromDeclaration(classTree);
            if (elem.getKind() == ElementKind.ENUM) {
                DeclaredType dt = (DeclaredType) elem.getSuperclass();
                AnnotatedDeclaredType adt =
                        (AnnotatedDeclaredType) atypeFactory.toAnnotatedType(dt, false);
                List<AnnotatedTypeMirror> tas = adt.getTypeArguments();
                List<AnnotatedTypeMirror> newtas = new ArrayList<>();
                for (AnnotatedTypeMirror t : tas) {
                    // If the type argument of super is the same as the input type
                    if (atypeFactory.types.isSameType(
                            t.getUnderlyingType(), type.getUnderlyingType())) {
                        t.addAnnotations(type.getAnnotations());
                        newtas.add(t);
                    }
                }
                adt.setTypeArguments(newtas);
                supertypes.add(adt);
            }
            if (type.wasRaw()) {
                for (AnnotatedDeclaredType adt : supertypes) {
                    adt.setWasRaw();
                }
            }
            return supertypes;
        }

        /**
         *
         *
         * <pre>{@code
         * For type = A[ ] ==>
         *  Object >: A[ ]
         *  Clonable >: A[ ]
         *  java.io.Serializable >: A[ ]
         *
         * if A is reference type, then also
         *  B[ ] >: A[ ] for any B[ ] >: A[ ]
         * }</pre>
         */
        @Override
        public List<AnnotatedTypeMirror> visitArray(AnnotatedArrayType type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<>();
            Set<AnnotationMirror> annotations = type.getAnnotations();
            Elements elements = atypeFactory.elements;
            final AnnotatedTypeMirror objectType =
                    atypeFactory.getAnnotatedType(elements.getTypeElement("java.lang.Object"));
            objectType.addAnnotations(annotations);
            superTypes.add(objectType);

            final AnnotatedTypeMirror cloneableType =
                    atypeFactory.getAnnotatedType(elements.getTypeElement("java.lang.Cloneable"));
            cloneableType.addAnnotations(annotations);
            superTypes.add(cloneableType);

            final AnnotatedTypeMirror serializableType =
                    atypeFactory.getAnnotatedType(elements.getTypeElement("java.io.Serializable"));
            serializableType.addAnnotations(annotations);
            superTypes.add(serializableType);

            for (AnnotatedTypeMirror sup : type.getComponentType().directSuperTypes()) {
                ArrayType arrType = atypeFactory.types.getArrayType(sup.getUnderlyingType());
                AnnotatedArrayType aarrType =
                        (AnnotatedArrayType) atypeFactory.toAnnotatedType(arrType, false);
                aarrType.setComponentType(sup);
                aarrType.addAnnotations(annotations);
                superTypes.add(aarrType);
            }

            return superTypes;
        }

        @Override
        public List<AnnotatedTypeMirror> visitTypeVariable(AnnotatedTypeVariable type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<>();
            superTypes.add(type.getUpperBound().deepCopy());
            return superTypes;
        }

        @Override
        public List<AnnotatedTypeMirror> visitWildcard(AnnotatedWildcardType type, Void p) {
            List<AnnotatedTypeMirror> superTypes = new ArrayList<>();
            superTypes.add(type.getExtendsBound().deepCopy());
            return superTypes;
        }

        /**
         * Note: The explanation below is my interpretation of why we have this code. I am not sure
         * if this was the author's original intent but I can see no other reasoning, exercise
         * caution:
         *
         * <p>Classes may have type parameters that are used in extends or implements clauses. E.g.
         * {@code class MyList<T> extends List<T>}
         *
         * <p>Direct supertypes will contain a type {@code List<T>} but the type T may become out of
         * sync with the annotations on type {@code MyList<T>}. To keep them in-sync, we substitute
         * out the copy of T with the same reference to T that is on {@code MyList<T>}
         */
        private static class TypeParamReplacer
                extends AnnotatedTypeScanner<Void, Map<TypeParameterElement, AnnotatedTypeMirror>> {
            private final Types types;

            public TypeParamReplacer(Types types) {
                this.types = types;
            }

            @Override
            public Void visitDeclared(
                    AnnotatedDeclaredType type,
                    Map<TypeParameterElement, AnnotatedTypeMirror> mapping) {
                if (visitedNodes.containsKey(type)) {
                    return visitedNodes.get(type);
                }
                visitedNodes.put(type, null);
                if (type.getEnclosingType() != null) {
                    scan(type.getEnclosingType(), mapping);
                }

                List<AnnotatedTypeMirror> args = new ArrayList<>();
                for (AnnotatedTypeMirror arg : type.getTypeArguments()) {
                    Element elem = types.asElement(arg.getUnderlyingType());
                    if ((elem != null)
                            && (elem.getKind() == ElementKind.TYPE_PARAMETER)
                            && mapping.containsKey(elem)) {
                        AnnotatedTypeMirror other = mapping.get(elem).deepCopy();
                        other.replaceAnnotations(arg.getAnnotationsField());
                        args.add(other);
                    } else {
                        args.add(arg);
                        scan(arg, mapping);
                    }
                }
                type.setTypeArguments(args);

                return null;
            }

            @Override
            public Void visitArray(
                    AnnotatedArrayType type,
                    Map<TypeParameterElement, AnnotatedTypeMirror> mapping) {
                AnnotatedTypeMirror comptype = type.getComponentType();
                Element elem = types.asElement(comptype.getUnderlyingType());
                AnnotatedTypeMirror other;
                if ((elem != null)
                        && (elem.getKind() == ElementKind.TYPE_PARAMETER)
                        && mapping.containsKey(elem)) {
                    other = mapping.get(elem);
                    other.replaceAnnotations(comptype.getAnnotationsField());
                    type.setComponentType(other);
                } else {
                    scan(type.getComponentType(), mapping);
                }

                return null;
            }
        }
    }
}
