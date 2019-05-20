package org.checkerframework.framework.type.typeannotator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.CollectionUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Adds annotations to types that are not relevant specified by the {@link RelevantJavaTypes} on a
 * checker.
 */
public class IrrelevantTypeAnnotator extends TypeAnnotator {
    /**
     * List of relevantTypes translated from classes in {@link RelevantJavaTypes} to {@link
     * TypeMirror}s.
     */
    private List<TypeMirror> relevantTypes;

    /**
     * Cache of types found that are relevantTypes or subclass of supported types. Used so that
     * isSubtype doesn't need to be called repeatedly on the same types.
     */
    private Set<TypeMirror> allFoundRelevantTypes;

    private boolean arraysAreRelevant;
    private Set<? extends AnnotationMirror> annotations;

    /**
     * Annotate every type with the annotationMirror except for those whose underlying Java type is
     * one of (or a subtype of) a class in relevantClasses. (Only adds annotationMirror if no
     * annotation in the hierarchy are already on the type.) If relevantClasses includes
     * Object[].class, then all arrays are considered relevant.
     *
     * @param typeFactory AnnotatedTypeFactory
     * @param annotations annotations to add
     * @param relevantClasses types that should not be annotated with annotationMirror
     */
    public IrrelevantTypeAnnotator(
            AnnotatedTypeFactory typeFactory,
            Set<? extends AnnotationMirror> annotations,
            Class<?>[] relevantClasses) {
        super(typeFactory);
        this.annotations = annotations;
        this.arraysAreRelevant = false;
        this.relevantTypes = new ArrayList<>(relevantClasses.length);
        for (Class<?> clazz : relevantClasses) {
            if (clazz.equals(Object[].class)) {
                arraysAreRelevant = true;
            } else {
                relevantTypes.add(
                        TypesUtils.typeFromClass(
                                clazz,
                                typeFactory.getContext().getTypeUtils(),
                                typeFactory.getElementUtils()));
            }
        }
        this.allFoundRelevantTypes = Collections.newSetFromMap(CollectionUtils.createLRUCache(300));
    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
        switch (type.getKind()) {
            case TYPEVAR:
            case WILDCARD:
            case EXECUTABLE:
            case INTERSECTION:
            case UNION:
            case NULL:
            case NONE:
            case PACKAGE:
            case VOID:
                return super.scan(type, aVoid);
            default:
                // go on
        }

        Types types = typeFactory.getContext().getTypeUtils();
        TypeMirror typeMirror = type.getUnderlyingType();

        if (TypesUtils.isPrimitive(typeMirror)) {
            typeMirror = types.boxedClass((PrimitiveType) typeMirror).asType();
        }

        boolean shouldAnnotate = true;
        if (allFoundRelevantTypes.contains(typeMirror)) {
            shouldAnnotate = false;
        } else if (typeMirror.getKind() == TypeKind.DECLARED) {
            for (TypeMirror supportedType : relevantTypes) {
                if (types.isSubtype(typeMirror, supportedType)) {
                    shouldAnnotate = false;
                    allFoundRelevantTypes.add(typeMirror);
                    break;
                }
            }
        } else if (typeMirror.getKind() == TypeKind.ARRAY) {
            shouldAnnotate = arraysAreRelevant;
            if (arraysAreRelevant) {
                allFoundRelevantTypes.add(typeMirror);
            }
        }

        if (shouldAnnotate) {
            type.addMissingAnnotations(annotations);
        }
        return super.scan(type, aVoid);
    }
}
