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
    private final Map<Class<? extends AnnotatedTypeMirror>, Set<AnnotationMirror>> typeClasses;
    /** Map from full qualified class name strings to annotations. */
    private final Map<String, Set<AnnotationMirror>> classes;

    /** {@link QualifierHierarchy} */
    private final QualifierHierarchy qualHierarchy;
    // private final AnnotatedTypeFactory atypeFactory;

    /**
     * Creates a {@link DefaultForTypeAnnotator} from the given checker, using that checker to
     * determine the annotations that are in the type hierarchy.
     */
    public DefaultForTypeAnnotator(AnnotatedTypeFactory typeFactory) {
        super(typeFactory);
        this.typeKinds = new EnumMap<>(TypeKind.class);
        this.typeClasses = new HashMap<>();
        this.classes = new HashMap<>();

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

        if (qname != null && classes.containsKey(qname)) {
            Set<AnnotationMirror> fnd = classes.get(qname);
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
    public DefaultForTypeAnnotator addStandardDefaults() {
        if (!classes.containsKey(Void.class.getCanonicalName())) {
            for (AnnotationMirror bottom : qualHierarchy.getBottomAnnotations()) {
                addClasses(Void.class, bottom);
            }
        } else {
            Set<AnnotationMirror> annos = classes.get(Void.class.getCanonicalName());
            for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
                if (qualHierarchy.findAnnotationInHierarchy(annos, top) == null) {
                    addClasses(Void.class, qualHierarchy.getBottomAnnotation(top));
                }
            }
        }

        return this;
    }
}
