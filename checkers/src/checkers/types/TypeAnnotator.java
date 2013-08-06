package checkers.types;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.ImplicitFor;
import checkers.quals.TypeQualifiers;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.visitors.AnnotatedTypeScanner;

import javacutils.AnnotationUtils;
import javacutils.ErrorReporter;
import javacutils.TypesUtils;

import java.lang.annotation.Annotation;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.Tree;

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
 * The Element parameter is the use-site of the type.
 * TODO: the parameter is null if no Element is available, e.g. for a cast.
 *
 * @see TreeAnnotator
 */
public class TypeAnnotator extends AnnotatedTypeScanner<Void, Element> {

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
     * @param checker the type-checker to which this annotator belongs
     */
    public TypeAnnotator(BaseTypeChecker<?> checker, AnnotatedTypeFactory atypeFactory) {

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
        boolean res = qualHierarchy.updateMappingToMutableSet(typeKinds, typeKind, theQual);
        if (!res) {
            ErrorReporter.errorAbort("TypeAnnotator: invalid update of typeKinds " +
                    typeKinds + " at " + typeKind + " with " + theQual);
        }
    }

    public void addTypeClass(Class<? extends AnnotatedTypeMirror> typeClass, AnnotationMirror theQual) {
        boolean res = qualHierarchy.updateMappingToMutableSet(typeClasses, typeClass, theQual);
        if (!res) {
            ErrorReporter.errorAbort("TypeAnnotator: invalid update of typeClasses " +
                    typeClasses + " at " + typeClass + " with " + theQual);
        }
    }

    public void addTypeName(Class<?> typeName, AnnotationMirror theQual) {
        String typeNameString = typeName.getCanonicalName().intern();
        boolean res = qualHierarchy.updateMappingToMutableSet(typeNames, typeNameString, theQual);
        if (!res) {
            ErrorReporter.errorAbort("TypeAnnotator: invalid update of typeNames " +
                    typeNames + " at " + typeName + " with " + theQual);
        }
    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, Element elem) {

        if (type == null) // on bounds, etc.
            return super.scan(type, elem);

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

        return super.scan(type, elem);
    }

    @Override
    public Void visitExecutable(AnnotatedExecutableType t, Element elem) {
        // skip the receiver
        scan(t.getReturnType(), elem);
        scanAndReduce(t.getParameterTypes(), elem, null);
        scanAndReduce(t.getThrownTypes(), elem, null);
        scanAndReduce(t.getTypeVariables(), elem, null);
        return null;
    }
}
