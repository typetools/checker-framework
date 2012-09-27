package checkers.types;

import java.lang.annotation.Annotation;
import java.util.*;

import com.sun.source.tree.Tree;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.ImplicitFor;
import checkers.quals.TypeQualifiers;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.visitors.AnnotatedTypeScanner;
import checkers.util.AnnotationUtils;
import checkers.util.TypesUtils;

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
 *
 * This class takes care of two of the attributes of {@link ImplicitFor};
 * the others are handled in {@link TreeAnnotator}.
 *
 * @see TreeAnnotator
 */
public class TypeAnnotator extends AnnotatedTypeScanner<Void, ElementKind> {

    // TODO: like in TreeAnnotator, these should be maps to Set<AM>.
    private final Map<TypeKind, Set<AnnotationMirror>> typeKinds;
    private final Map<Class<? extends AnnotatedTypeMirror>, Set<AnnotationMirror>> typeClasses;
    private final Map<String, Set<AnnotationMirror>> typeNames;

    private final QualifierHierarchy qualHierarchy;
    // private final AnnotatedTypeFactory atypeFactory;

    /**
     * Creates a {@link TypeAnnotator} from the given checker, using that checker's
     * {@link TypeQualifiers} annotation to determine the annotations that are
     * in the type hierarchy.
     *
     * @param checker the type checker to which this annotator belongs
     */
    public TypeAnnotator(BaseTypeChecker checker, AnnotatedTypeFactory atypeFactory) {

        this.typeKinds = new EnumMap<TypeKind, Set<AnnotationMirror>>(TypeKind.class);
        this.typeClasses = new HashMap<Class<? extends AnnotatedTypeMirror>, Set<AnnotationMirror>>();
        this.typeNames = new IdentityHashMap<String, Set<AnnotationMirror>>();

        this.qualHierarchy = checker.getQualifierHierarchy();
        // this.atypeFactory = atypeFactory;

        // Get type qualifiers from the checker.
        Set<Class<? extends Annotation>> quals = checker.getSupportedTypeQualifiers();

        // For each qualifier, read the @ImplicitFor annotation and put its type
        // classes and kinds into maps.
        for (Class<? extends Annotation> qual : quals) {
            ImplicitFor implicit = qual.getAnnotation(ImplicitFor.class);
            if (implicit == null) continue;

            AnnotationMirror theQual = AnnotationUtils.fromClass(atypeFactory.elements, qual);
            for (TypeKind typeKind : implicit.types()) {
                addTypeKind(typeKind, theQual);
            }

            for (Class<? extends AnnotatedTypeMirror> typeClass : implicit.typeClasses()) {
                addTypeClass(typeClass, theQual);
            }

            for (Class<?> typeName : implicit.typeNames()) {
                addTypeName(typeName, theQual);
            }
        }
    }

    public void addTypeKind(TypeKind typeKind, AnnotationMirror theQual) {
        boolean res = AnnotationUtils.updateMappingToMutableSet(qualHierarchy, typeKinds, typeKind, theQual);
        if (!res) {
            SourceChecker.errorAbort("TypeAnnotator: invalid update of typeKinds " +
                    typeKinds + " at " + typeKind + " with " + theQual);
        }
    }

    public void addTypeClass(Class<? extends AnnotatedTypeMirror> typeClass, AnnotationMirror theQual) {
        boolean res = AnnotationUtils.updateMappingToMutableSet(qualHierarchy, typeClasses, typeClass, theQual);
        if (!res) {
            SourceChecker.errorAbort("TypeAnnotator: invalid update of typeClasses " +
                    typeClasses + " at " + typeClass + " with " + theQual);
        }
    }

    public void addTypeName(Class<?> typeName, AnnotationMirror theQual) {
        String typeNameString = typeName.getCanonicalName().intern();
        boolean res = AnnotationUtils.updateMappingToMutableSet(qualHierarchy, typeNames, typeNameString, theQual);
        if (!res) {
            SourceChecker.errorAbort("TypeAnnotator: invalid update of typeNames " +
                    typeNames + " at " + typeName + " with " + theQual);
        }
    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, ElementKind p) {

        if (type == null) // on bounds, etc.
            return super.scan(type, p);

        // If the type's fully-qualified name is in the appropriate map, annotate
        // the type. Do this before looking at kind or class, as this information
        // is more specific.

        String qname = null;
        if (type.getKind() == TypeKind.DECLARED) {
            qname = TypesUtils.getQualifiedName((DeclaredType)type.getUnderlyingType()).toString();
        } else if (type.getKind().isPrimitive()) {
            qname = type.getUnderlyingType().toString();
        }
        qname = (qname == null) ? null : qname.intern();
        if (qname != null && typeNames.containsKey(qname)) {
            Set<AnnotationMirror> fnd = typeNames.get(qname);
            for (AnnotationMirror f : fnd) {
                if (!type.isAnnotatedInHierarchy(f)) {
                    type.addAnnotation(f);
                }
            }
        }

        // If the type's kind or class is in the appropriate map, annotate the
        // type.

        if (typeKinds.containsKey(type.getKind())) {
            Set<AnnotationMirror> fnd = typeKinds.get(type.getKind()); 
            for (AnnotationMirror f : fnd) {
                if (!type.isAnnotatedInHierarchy(f)) {
                    type.addAnnotation(f);
                }
            }
        } else if (!typeClasses.isEmpty()) {
            Class<? extends AnnotatedTypeMirror> t = type.getClass();
            if (typeClasses.containsKey(t)) {
                Set<AnnotationMirror> fnd = typeClasses.get(t);
                for (AnnotationMirror f : fnd) {
                    if (!type.isAnnotatedInHierarchy(f)) {
                        type.addAnnotation(f);
                    }
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
