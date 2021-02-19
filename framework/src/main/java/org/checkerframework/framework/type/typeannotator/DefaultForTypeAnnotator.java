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
 * by {@link DefaultFor}; it is designed to be used in a {@link ListTypeAnnotator} constructed in
 * {@link GenericAnnotatedTypeFactory#createTypeAnnotator()} ()}
 *
 * <p>{@link DefaultForTypeAnnotator} traverses types deeply.
 *
 * <p>This class takes care of two of the attributes of {@link DefaultFor}; the others are handled
 * in {@link org.checkerframework.framework.util.defaults.QualifierDefaults}.
 *
 * @see ListTypeAnnotator
 */
public class DefaultForTypeAnnotator extends TypeAnnotator {

    /** Map from {@link TypeKind} to annotations. */
    private final Map<TypeKind, Set<AnnotationMirror>> typeKinds;
    /** Map from {@link AnnotatedTypeMirror} classes to annotations. */
    private final Map<Class<? extends AnnotatedTypeMirror>, Set<AnnotationMirror>> atmClasses;
    /** Map from fully qualified class name strings to annotations. */
    private final Map<String, Set<AnnotationMirror>> types;

    /** {@link QualifierHierarchy} */
    private final QualifierHierarchy qualHierarchy;

    /**
     * Creates a {@link DefaultForTypeAnnotator} from the given checker, using that checker to
     * determine the annotations that are in the type hierarchy.
     */
    public DefaultForTypeAnnotator(AnnotatedTypeFactory typeFactory) {
        super(typeFactory);
        this.typeKinds = new EnumMap<>(TypeKind.class);
        this.atmClasses = new HashMap<>();
        this.types = new HashMap<>();

        this.qualHierarchy = typeFactory.getQualifierHierarchy();

        // Get type qualifiers from the checker.
        Set<Class<? extends Annotation>> quals = typeFactory.getSupportedTypeQualifiers();

        // For each qualifier, read the @DefaultFor annotation and put its type types and kinds
        // into maps.
        for (Class<? extends Annotation> qual : quals) {
            DefaultFor defaultFor = qual.getAnnotation(DefaultFor.class);
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
                addTypes(typeName, theQual);
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
    public void addAtmClass(
            Class<? extends AnnotatedTypeMirror> typeClass, AnnotationMirror theQual) {
        boolean res = qualHierarchy.updateMappingToMutableSet(atmClasses, typeClass, theQual);
        if (!res) {
            throw new BugInCF(
                    "TypeAnnotator: invalid update of atmClasses "
                            + atmClasses
                            + " at "
                            + typeClass
                            + " with "
                            + theQual);
        }
    }

    /** Add default qualifier, {@code theQual}, for the given type. */
    public void addTypes(Class<?> clazz, AnnotationMirror theQual) {
        String typeNameString = clazz.getCanonicalName();
        boolean res = qualHierarchy.updateMappingToMutableSet(types, typeNameString, theQual);
        if (!res) {
            throw new BugInCF(
                    "TypeAnnotator: invalid update of types "
                            + types
                            + " at "
                            + clazz
                            + " with "
                            + theQual);
        }
    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, Void p) {
        // If the type's fully-qualified name is in the appropriate map, annotate
        // the type. Do this before looking at kind or class, as this information
        // is more specific.

        String qname;
        if (type.getKind() == TypeKind.DECLARED) {
            qname = TypesUtils.getQualifiedName((DeclaredType) type.getUnderlyingType()).toString();
        } else if (type.getKind().isPrimitive()) {
            qname = type.getUnderlyingType().toString();
        } else {
            qname = null;
        }

        if (qname != null) {
            Set<AnnotationMirror> fromQname = types.get(qname);
            if (fromQname != null) {
                type.addMissingAnnotations(fromQname);
            }
        }

        // If the type's kind or class is in the appropriate map, annotate the
        // type.
        Set<AnnotationMirror> fromKind = typeKinds.get(type.getKind());
        if (fromKind != null) {
            type.addMissingAnnotations(fromKind);
        } else if (!atmClasses.isEmpty()) {
            Class<? extends AnnotatedTypeMirror> t = type.getClass();
            Set<AnnotationMirror> fromClass = atmClasses.get(t);
            if (fromClass != null) {
                type.addMissingAnnotations(fromClass);
            }
        }

        return super.scan(type, p);
    }

    /**
     * Adds standard rules. Currently, sets Void to bottom if no other qualifier is set for Void.
     * Also, see {@link LiteralTreeAnnotator#addStandardLiteralQualifiers()}.
     *
     * @return this
     */
    public DefaultForTypeAnnotator addStandardDefaults() {
        if (!types.containsKey(Void.class.getCanonicalName())) {
            for (AnnotationMirror bottom : qualHierarchy.getBottomAnnotations()) {
                addTypes(Void.class, bottom);
            }
        } else {
            Set<AnnotationMirror> annos = types.get(Void.class.getCanonicalName());
            for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
                if (qualHierarchy.findAnnotationInHierarchy(annos, top) == null) {
                    addTypes(Void.class, qualHierarchy.getBottomAnnotation(top));
                }
            }
        }

        return this;
    }
}
