package org.checkerframework.framework.type;

import java.lang.annotation.Annotation;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.qual.UpperBoundFor;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

public class QualifierUpperBounds {

    /** Map from {@link TypeKind} to annotations. */
    private final Map<TypeKind, Set<AnnotationMirror>> typeKinds;
    /** Map from {@link AnnotatedTypeMirror} classes to annotations. */
    private final Map<Class<? extends AnnotatedTypeMirror>, Set<AnnotationMirror>> typeClasses;
    /** Map from full qualified class name strings to annotations. */
    private final Map<String, Set<AnnotationMirror>> classes;

    /** {@link QualifierHierarchy} */
    private final QualifierHierarchy qualHierarchy;

    private final AnnotatedTypeFactory atypeFactory;

    /**
     * Creates a {@link QualifierUpperBounds} from the given checker, using that checker to
     * determine the annotations that are in the type hierarchy.
     */
    public QualifierUpperBounds(AnnotatedTypeFactory typeFactory) {
        this.atypeFactory = typeFactory;
        this.typeKinds = new EnumMap<>(TypeKind.class);
        this.typeClasses = new HashMap<>();
        this.classes = new HashMap<>();

        this.qualHierarchy = typeFactory.getQualifierHierarchy();

        // Get type qualifiers from the checker.
        Set<Class<? extends Annotation>> quals = typeFactory.getSupportedTypeQualifiers();

        // For each qualifier, read the @DefaultFor annotation and put its type classes and kinds
        // into maps.
        for (Class<? extends Annotation> qual : quals) {
            UpperBoundFor defaultFor = qual.getAnnotation(UpperBoundFor.class);
            if (defaultFor == null) {
                continue;
            }

            AnnotationMirror theQual =
                    AnnotationBuilder.fromClass(typeFactory.getElementUtils(), qual);
            for (org.checkerframework.framework.qual.TypeKind typeKind : defaultFor.typeKinds()) {
                TypeKind mappedTk = mapTypeKinds(typeKind);
                addTypeKind(mappedTk, theQual);
            }

            for (Class<?> typeName : defaultFor.types()) {
                addClasses(typeName, theQual);
            }
        }
    }

    /**
     * Map between {@link org.checkerframework.framework.qual.TypeKind} and {@link TypeKind}.
     *
     * @param typeKind the Checker Framework TypeKind
     * @return the javax TypeKind
     */
    private TypeKind mapTypeKinds(org.checkerframework.framework.qual.TypeKind typeKind) {
        return TypeKind.valueOf(typeKind.name());
    }

    /** Add default qualifier, {@code theQual}, for the given TypeKind. */
    public void addTypeKind(TypeKind typeKind, AnnotationMirror theQual) {
        boolean res = qualHierarchy.updateMappingToMutableSet(typeKinds, typeKind, theQual);
        if (!res) {
            throw new BugInCF(
                    "TypeAnnotator: invalid update of typeKinds "
                            + typeKinds
                            + " at "
                            + typeKind
                            + " with "
                            + theQual);
        }
    }

    /** Add default qualifier, {@code theQual}, for the given {@link AnnotatedTypeMirror} class. */
    public void addTypeClass(
            Class<? extends AnnotatedTypeMirror> typeClass, AnnotationMirror theQual) {
        boolean res = qualHierarchy.updateMappingToMutableSet(typeClasses, typeClass, theQual);
        if (!res) {
            throw new BugInCF(
                    "TypeAnnotator: invalid update of typeClasses "
                            + typeClasses
                            + " at "
                            + typeClass
                            + " with "
                            + theQual);
        }
    }

    /** Add default qualifier, {@code theQual}, for the given class. */
    public void addClasses(Class<?> clazz, AnnotationMirror theQual) {
        String typeNameString = clazz.getCanonicalName();
        boolean res = qualHierarchy.updateMappingToMutableSet(classes, typeNameString, theQual);
        if (!res) {
            throw new BugInCF(
                    "TypeAnnotator: invalid update of types "
                            + classes
                            + " at "
                            + clazz
                            + " with "
                            + theQual);
        }
    }

    protected AnnotationMirrorSet getBoundAnnotations(AnnotatedTypeMirror annotatedTypeMirror) {
        TypeMirror type = annotatedTypeMirror.getUnderlyingType();
        AnnotationMirrorSet bounds = new AnnotationMirrorSet();
        String qname;
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            bounds.addAll(
                    atypeFactory
                            .fromElement((TypeElement) declaredType.asElement())
                            .getAnnotations());
            qname = TypesUtils.getQualifiedName(declaredType).toString();
        } else if (type.getKind().isPrimitive()) {
            qname = type.toString();
        } else {
            qname = null;
        }

        if (qname != null && classes.containsKey(qname)) {
            Set<AnnotationMirror> fnd = classes.get(qname);
            addMissingAnnotations(bounds, fnd);
        }

        // If the type's kind or class is in the appropriate map, annotate the
        // type.

        if (typeKinds.containsKey(type.getKind())) {
            Set<AnnotationMirror> fnd = typeKinds.get(type.getKind());
            addMissingAnnotations(bounds, fnd);
        } else if (!typeClasses.isEmpty()) {
            Class<? extends AnnotatedTypeMirror> t = annotatedTypeMirror.getClass();
            if (typeClasses.containsKey(t)) {
                Set<AnnotationMirror> fnd = typeClasses.get(t);
                addMissingAnnotations(bounds, fnd);
            }
        }
        addMissingAnnotations(bounds, atypeFactory.getDefaultTypeDeclarationBound());
        return bounds;
    }

    private void addMissingAnnotations(
            AnnotationMirrorSet annos, Set<? extends AnnotationMirror> missing) {
        for (AnnotationMirror miss : missing) {
            if (atypeFactory.getQualifierHierarchy().findAnnotationInSameHierarchy(annos, miss)
                    == null) {
                annos.add(miss);
            }
        }
    }
}
