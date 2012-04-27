package checkers.types;

import java.lang.annotation.Annotation;
import java.util.*;

import com.sun.source.tree.Tree;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.ImplicitFor;
import checkers.quals.TypeQualifiers;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.util.AnnotationUtils;

/**
 * Adds annotations to a type based on the contents of a type. By default, this
 * class honors the {@link ImplicitFor} annotation and applies implicit
 * annotations specified by {@link ImplicitFor} for any type whose visitor is
 * not overridden or does not call {@code super}; it is designed to be invoked
 * from
 * {@link AnnotatedTypeFactory#annotateImplicit(Element, AnnotatedTypeMirror)}
 * and
 * {@link AnnotatedTypeFactory#annotateImplicit(Tree, AnnotatedTypeMirror)}.
 *
 * <p>
 *
 * {@link TypeAnnotator} traverses types deeply by default, except that it skips
 * the method receiver of executable types (for interoperability with
 * {@link AnnotatedTypeFactory#annotateInheritedFromClass(AnnotatedTypeMirror)}).
 */
public class TypeAnnotator extends AnnotatedTypeScanner<Void, ElementKind> {

    private final Map<TypeKind, AnnotationMirror> typeKinds;
    private final Map<Class<?>, AnnotationMirror> typeClasses;

    /**
     * Creates a {@link TypeAnnotator} from the given checker, using that checker's
     * {@link TypeQualifiers} annotation to determine the annotations that are
     * in the type hierarchy.
     *
     * @param checker the type checker to which this annotator belongs
     */
    public TypeAnnotator(BaseTypeChecker checker) {

        this.typeKinds = new EnumMap<TypeKind, AnnotationMirror>(TypeKind.class);
        this.typeClasses = new HashMap<Class<?>, AnnotationMirror>();

        AnnotationUtils annoFactory = AnnotationUtils.getInstance(checker.getProcessingEnvironment());

        // Get type qualifiers from the checker.
        Set<Class<? extends Annotation>> quals
            = checker.getSupportedTypeQualifiers();

        // For each qualifier, read the @ImplicitFor annotation and put its type
        // classes and kinds into maps.
        for (Class<? extends Annotation> qual : quals) {
            ImplicitFor implicit = qual.getAnnotation(ImplicitFor.class);
            if (implicit == null) continue;

            AnnotationMirror theQual = annoFactory.fromClass(qual);
            for (Class<? extends AnnotatedTypeMirror> typeClass : implicit.typeClasses())
                typeClasses.put(typeClass, theQual);

            for (TypeKind typeKind : implicit.types())
                typeKinds.put(typeKind, theQual);
        }

    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, ElementKind p) {

        if (type == null) // on bounds, etc.
            return super.scan(type, p);

        // If the type's kind or class is in the appropriate map, annotate the
        // type.

        if (typeKinds.containsKey(type.getKind())) {
            AnnotationMirror fnd = typeKinds.get(type.getKind());
            if (!type.isAnnotatedInHierarchy(fnd)) {
                type.addAnnotation(fnd);
            }
        } else if (!typeClasses.isEmpty()) {
            Class<? extends AnnotatedTypeMirror> t = type.getClass();
            if (typeClasses.containsKey(t)) {
                AnnotationMirror fnd = typeClasses.get(t);
                if (!type.isAnnotatedInHierarchy(fnd)) {
                    type.addAnnotation(fnd);
                }
            }
        }

        return super.scan(type, p);
    }

    @Override
    public Void visitExecutable(AnnotatedExecutableType t, ElementKind p) {
        // skip the receiver
        scan(t.getReturnType(), p);
        scanAndReduce(t.getParameterTypes(), p, null);
        scanAndReduce(t.getThrownTypes(), p, null);
        scanAndReduce(t.getTypeVariables(), p, null);
        return null;
    }
}
