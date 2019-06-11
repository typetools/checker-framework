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

/** Class that computes and stores the qualifier upper bounds for type uses. */
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

        // For each qualifier, read the @UpperBoundFor annotation and put its type classes and kinds
        // into maps.
        for (Class<? extends Annotation> qual : quals) {
            UpperBoundFor upperBoundFor = qual.getAnnotation(UpperBoundFor.class);
            if (upperBoundFor == null) {
                continue;
            }

            AnnotationMirror theQual =
                    AnnotationBuilder.fromClass(typeFactory.getElementUtils(), qual);
            for (org.checkerframework.framework.qual.TypeKind typeKind :
                    upperBoundFor.typeKinds()) {
                TypeKind mappedTk = mapTypeKinds(typeKind);
                addTypeKind(mappedTk, theQual);
            }

            for (Class<?> typeName : upperBoundFor.types()) {
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

    /** Returns the set of qualifiers that are the upper bounds for a use of the type. */
    protected Set<AnnotationMirror> getBoundQualifiers(AnnotatedTypeMirror type) {
        TypeMirror javaType = type.getUnderlyingType();
        AnnotationMirrorSet bounds = new AnnotationMirrorSet();
        String qname;
        if (javaType.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) javaType;
            bounds.addAll(
                    atypeFactory
                            .fromElement((TypeElement) declaredType.asElement())
                            .getAnnotations());
            qname = TypesUtils.getQualifiedName(declaredType).toString();
        } else if (javaType.getKind().isPrimitive()) {
            qname = javaType.toString();
        } else {
            qname = null;
        }

        if (qname != null && classes.containsKey(qname)) {
            Set<AnnotationMirror> fnd = classes.get(qname);
            addMissingAnnotations(bounds, fnd);
        }

        // If the javaType's kind or class is in the appropriate map, annotate the
        // javaType.

        if (typeKinds.containsKey(javaType.getKind())) {
            Set<AnnotationMirror> fnd = typeKinds.get(javaType.getKind());
            addMissingAnnotations(bounds, fnd);
        } else if (!typeClasses.isEmpty()) {
            Class<? extends AnnotatedTypeMirror> t = type.getClass();
            if (typeClasses.containsKey(t)) {
                Set<AnnotationMirror> fnd = typeClasses.get(t);
                addMissingAnnotations(bounds, fnd);
            }
        }
        addMissingAnnotations(bounds, atypeFactory.getDefaultTypeDeclarationBounds());
        return bounds;
    }

    /**
     * Adds {@code missing} to {@code annos}, for which no annotation from the same qualifier
     * hierarchy is present.
     */
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
