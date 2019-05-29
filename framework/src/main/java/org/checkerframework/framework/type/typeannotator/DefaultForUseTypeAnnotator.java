package org.checkerframework.framework.type.typeannotator;

import java.lang.annotation.Annotation;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Adds annotations to a type based on the use of a type. This class applies annotations specified
 * by {@link DefaultFor}; it is designed to be used in a {@link
 * org.checkerframework.framework.type.treeannotator.ListTreeAnnotator} constructed in {@link
 * GenericAnnotatedTypeFactory#createTreeAnnotator()}
 *
 * <p>{@link DefaultForUseTypeAnnotator} traverses types deeply.
 *
 * <p>This class takes care of two of the attributes of {@link DefaultFor}; the others are handled
 * in {@link org.checkerframework.framework.util.defaults.QualifierDefaults}.
 *
 * @see org.checkerframework.framework.type.treeannotator.ListTreeAnnotator
 */
public class DefaultForUseTypeAnnotator extends TypeAnnotator {

    private final Map<TypeKind, Set<AnnotationMirror>> typeKinds;
    private final Map<Class<? extends AnnotatedTypeMirror>, Set<AnnotationMirror>> typeClasses;
    private final Map<String, Set<AnnotationMirror>> typeNames;

    private final QualifierHierarchy qualHierarchy;
    // private final AnnotatedTypeFactory atypeFactory;

    /**
     * Creates a {@link DefaultForUseTypeAnnotator} from the given checker, using that checker to
     * determine the annotations that are in the type hierarchy.
     */
    public DefaultForUseTypeAnnotator(AnnotatedTypeFactory typeFactory) {
        super(typeFactory);
        this.typeKinds = new EnumMap<>(TypeKind.class);
        this.typeClasses = new HashMap<>();
        this.typeNames = new HashMap<>();

        this.qualHierarchy = typeFactory.getQualifierHierarchy();

        // Get type qualifiers from the checker.
        Set<Class<? extends Annotation>> quals = typeFactory.getSupportedTypeQualifiers();

        // For each qualifier, read the @DefaultFor annotation and put its type classes and kinds
        // into maps.
        for (Class<? extends Annotation> qual : quals) {
            DefaultFor defaultFor = qual.getAnnotation(DefaultFor.class);
            if (defaultFor == null) {
                continue;
            }

            AnnotationMirror theQual =
                    AnnotationBuilder.fromClass(typeFactory.getElementUtils(), qual);
            for (org.checkerframework.framework.qual.TypeKind typeKind : defaultFor.types()) {
                TypeKind mappedTk = mapTypeKinds(typeKind);
                addTypeKind(mappedTk, theQual);
            }

            for (Class<?> typeName : defaultFor.typeNames()) {
                addTypeName(typeName, theQual);
            }
        }
    }

    /**
     * Map between {@link org.checkerframework.framework.qual.TypeKind} and {@link
     * javax.lang.model.type.TypeKind}.
     *
     * @param typeKind the Checker Framework TypeKind
     * @return the javax TypeKind
     */
    private TypeKind mapTypeKinds(org.checkerframework.framework.qual.TypeKind typeKind) {
        return TypeKind.valueOf(typeKind.name());
    }

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

    public void addTypeName(Class<?> typeName, AnnotationMirror theQual) {
        String typeNameString = typeName.getCanonicalName();
        boolean res = qualHierarchy.updateMappingToMutableSet(typeNames, typeNameString, theQual);
        if (!res) {
            throw new BugInCF(
                    "TypeAnnotator: invalid update of typeNames "
                            + typeNames
                            + " at "
                            + typeName
                            + " with "
                            + theQual);
        }
    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, Void p) {
        // If the type's fully-qualified name is in the appropriate map, annotate
        // the type. Do this before looking at kind or class, as this information
        // is more specific.

        String qname = null;
        if (type.getKind() == TypeKind.DECLARED) {
            qname = TypesUtils.getQualifiedName((DeclaredType) type.getUnderlyingType()).toString();
        } else if (type.getKind().isPrimitive()) {
            qname = type.getUnderlyingType().toString();
        }

        if (qname != null && typeNames.containsKey(qname)) {
            Set<AnnotationMirror> fnd = typeNames.get(qname);
            type.addMissingAnnotations(fnd);
        }

        // If the type's kind or class is in the appropriate map, annotate the
        // type.

        if (typeKinds.containsKey(type.getKind())) {
            Set<AnnotationMirror> fnd = typeKinds.get(type.getKind());
            type.addMissingAnnotations(fnd);
        } else if (!typeClasses.isEmpty()) {
            Class<? extends AnnotatedTypeMirror> t = type.getClass();
            if (typeClasses.containsKey(t)) {
                Set<AnnotationMirror> fnd = typeClasses.get(t);
                type.addMissingAnnotations(fnd);
            }
        }

        return super.scan(type, p);
    }

    /**
     * Adds standard rules. Currently, sets Void to bottom if no other implicit is set for Void.
     * Also, see {@link LiteralTreeAnnotator#addStandardLiteralQualifiers()}.
     *
     * @return this
     */
    public DefaultForUseTypeAnnotator addStandardDefaults() {
        if (!typeNames.containsKey(Void.class.getCanonicalName())) {
            for (AnnotationMirror bottom : qualHierarchy.getBottomAnnotations()) {
                addTypeName(Void.class, bottom);
            }
        } else {
            Set<AnnotationMirror> annos = typeNames.get(Void.class.getCanonicalName());
            for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
                if (qualHierarchy.findAnnotationInHierarchy(annos, top) == null) {
                    addTypeName(Void.class, qualHierarchy.getBottomAnnotation(top));
                }
            }
        }

        return this;
    }
}
