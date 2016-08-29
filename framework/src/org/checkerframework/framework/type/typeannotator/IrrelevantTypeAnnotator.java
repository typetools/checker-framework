package org.checkerframework.framework.type.typeannotator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.TypesUtils;

public class IrrelevantTypeAnnotator extends TypeAnnotator {
    List<TypeMirror> supportedTypes;
    Set<TypeMirror> allFoundSupportedTypes;

    boolean excludeArrays = false;
    Set<? extends AnnotationMirror> annotations;

    /**
     * Annotate every type with the annotationMirror except for those whose underlying Java type is
     * one of (or a subtype of) a class in classes.  (Only adds annotationMirror if no annotation
     * in the hierarchy are already on the type.)
     * @param typeFactory AnnotatedTypeFactory
     * @param annotations
     * @param classes types that should not be annotated with annotationMirror
     */
    public IrrelevantTypeAnnotator(
            AnnotatedTypeFactory typeFactory,
            Set<? extends AnnotationMirror> annotations,
            Class<?>[] classes) {
        super(typeFactory);
        this.annotations = annotations;

        this.supportedTypes = new ArrayList<>(classes.length);
        for (Class<?> clazz : classes) {
            if (clazz.equals(Object[].class)) {
                excludeArrays = true;
            } else {
                supportedTypes.add(
                        TypesUtils.typeFromClass(
                                typeFactory.getContext().getTypeUtils(),
                                typeFactory.getElementUtils(),
                                clazz));
            }
        }
        this.allFoundSupportedTypes = new HashSet<>();
        allFoundSupportedTypes.addAll(supportedTypes);
    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
        if (type == null) {
            return aVoid;
        }
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
        }

        Types types = typeFactory.getContext().getTypeUtils();
        TypeMirror typeMirror = type.getUnderlyingType();

        if (TypesUtils.isPrimitive(typeMirror)) {
            typeMirror = types.boxedClass((PrimitiveType) typeMirror).asType();
        }

        boolean shouldAnnotate = true;
        if (allFoundSupportedTypes.contains(typeMirror)) {
            shouldAnnotate = false;
        } else if (typeMirror.getKind() == TypeKind.DECLARED) {
            for (TypeMirror supportedType : supportedTypes) {
                if (types.isSubtype(typeMirror, supportedType)) {
                    shouldAnnotate = false;
                    allFoundSupportedTypes.add(typeMirror);
                    break;
                }
            }
        } else if (typeMirror.getKind() == TypeKind.ARRAY) {
            allFoundSupportedTypes.add(typeMirror);
            shouldAnnotate = excludeArrays;
        }

        if (shouldAnnotate) {
            type.addMissingAnnotations(annotations);
        }
        return super.scan(type, aVoid);
    }

    @Override
    public Void visitExecutable(AnnotatedExecutableType t, Void p) {
        // super skips the receiver
        scan(t.getReturnType(), p);
        scanAndReduce(t.getReceiverType(), p, null);
        scanAndReduce(t.getParameterTypes(), p, null);
        scanAndReduce(t.getThrownTypes(), p, null);
        scanAndReduce(t.getTypeVariables(), p, null);
        return null;
    }
}
