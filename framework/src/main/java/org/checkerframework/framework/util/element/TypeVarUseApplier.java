package org.checkerframework.framework.util.element;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.ElementAnnotationApplier;
import org.checkerframework.javacutil.BugInCF;

/** Apply annotations to the use of a type parameter declaration. */
public class TypeVarUseApplier {

    public static void apply(
            final AnnotatedTypeMirror type,
            final Element element,
            final AnnotatedTypeFactory typeFactory) {
        new TypeVarUseApplier(type, element, typeFactory).extractAndApply();
    }

    private static ElementKind[] acceptedKinds = {
        ElementKind.PARAMETER,
        ElementKind.FIELD,
        ElementKind.LOCAL_VARIABLE,
        ElementKind.RESOURCE_VARIABLE,
        ElementKind.METHOD
    };

    /**
     * @return true if type is an AnnotatedTypeVariable, or an AnnotatedArrayType with a type
     *     variable component, and the element is not a TYPE_PARAMETER
     */
    public static boolean accepts(AnnotatedTypeMirror type, Element element) {
        return (type instanceof AnnotatedTypeVariable || isGenericArrayType(type))
                && ElementAnnotationUtil.contains(element.getKind(), acceptedKinds);
    }

    private static boolean isGenericArrayType(AnnotatedTypeMirror type) {
        return type instanceof AnnotatedArrayType
                && getNestedComponentType(type) instanceof AnnotatedTypeVariable;
    }

    private static AnnotatedTypeMirror getNestedComponentType(AnnotatedTypeMirror type) {

        AnnotatedTypeMirror componentType = type;
        while (componentType instanceof AnnotatedArrayType) {
            componentType = ((AnnotatedArrayType) componentType).getComponentType();
        }

        return componentType;
    }

    // In order to avoid sprinkling code for type parameter uses all over the various locations
    // uses can show up we also handle generic array types.  T [] myTArr;
    private final AnnotatedArrayType arrayType;

    private final AnnotatedTypeVariable typeVariable;
    private final TypeParameterElement declarationElem;
    private final Element useElem;

    private AnnotatedTypeFactory typeFactory;

    TypeVarUseApplier(
            final AnnotatedTypeMirror type,
            final Element element,
            final AnnotatedTypeFactory typeFactory) {
        if (!accepts(type, element)) {
            throw new BugInCF(
                    "TypeParamUseApplier does not accept type/element combination ("
                            + " type ( "
                            + type
                            + " ) element ( "
                            + element
                            + " ) ");
        }

        if (isGenericArrayType(type)) {
            this.arrayType = (AnnotatedArrayType) type;
            this.typeVariable = (AnnotatedTypeVariable) getNestedComponentType(type);
            this.declarationElem =
                    (TypeParameterElement) typeVariable.getUnderlyingType().asElement();
            this.useElem = element;
            this.typeFactory = typeFactory;

        } else {
            this.arrayType = null;
            this.typeVariable = (AnnotatedTypeVariable) type;
            this.declarationElem =
                    (TypeParameterElement) typeVariable.getUnderlyingType().asElement();
            this.useElem = element;
            this.typeFactory = typeFactory;
        }
    }

    /**
     * Applies the bound annotations from the declaration of the type parameter and then applies the
     * explicit annotations written on the type variable.
     */
    public void extractAndApply() {
        ElementAnnotationUtil.addAnnotationsFromElement(
                typeVariable, useElem.getAnnotationMirrors());

        // apply declaration annotations
        ElementAnnotationApplier.apply(typeVariable, declarationElem, typeFactory);

        final List<Attribute.TypeCompound> annotations = getAnnotations(useElem, declarationElem);

        final List<Attribute.TypeCompound> typeVarAnnotations;
        if (arrayType != null) {
            // if the outer-most type is an array type then we want to ensure the outer annotations
            // are not applied as the type variables primary annotation
            typeVarAnnotations = removeComponentAnnotations(arrayType, annotations);
            ElementAnnotationUtil.annotateViaTypeAnnoPosition(arrayType, annotations);

        } else {
            typeVarAnnotations = annotations;
        }

        for (final Attribute.TypeCompound annotation : typeVarAnnotations) {
            typeVariable.removeAnnotationInHierarchy(annotation);
            typeVariable.addAnnotation(annotation);

            final List<? extends AnnotatedTypeMirror> upperBounds;
            if (typeVariable.getUpperBound() instanceof AnnotatedIntersectionType) {
                upperBounds = typeVariable.getUpperBound().directSuperTypes();
            } else {
                upperBounds = Arrays.asList(typeVariable.getUpperBound());
            }

            // TODO: Should we just make primary annotations on annotated intersection types apply
            // TODO: to all of them?  Que dealio?  What should we do?
            for (final AnnotatedTypeMirror bound : upperBounds) {
                bound.removeAnnotationInHierarchy(annotation);
                bound.addAnnotation(annotation);
            }
        }
    }

    private List<Attribute.TypeCompound> removeComponentAnnotations(
            final AnnotatedArrayType arrayType, final List<Attribute.TypeCompound> annotations) {

        final List<Attribute.TypeCompound> componentAnnotations = new ArrayList<>();

        if (arrayType != null) {
            for (int i = 0; i < annotations.size(); ) {
                final Attribute.TypeCompound anno = annotations.get(i);
                if (isBaseComponent(arrayType, anno)) {
                    componentAnnotations.add(anno);
                    annotations.remove(anno);
                } else {
                    i++;
                }
            }
        }

        return componentAnnotations;
    }

    private boolean isBaseComponent(
            final AnnotatedArrayType arrayType, final Attribute.TypeCompound anno) {
        return ElementAnnotationUtil.getTypeAtLocation(arrayType, anno.getPosition().location)
                .getClass()
                .equals(AnnotatedTypeVariable.class);
    }

    /**
     * Depending on what element type the annotations are stored on, the relevant annotations might
     * be stored with different annotation positions. getAnnotations finds the correct annotations
     * by annotation position and element kind and returns them
     */
    private static List<Attribute.TypeCompound> getAnnotations(
            final Element useElem, final Element declarationElem) {
        final List<Attribute.TypeCompound> annotations;
        switch (useElem.getKind()) {
            case METHOD:
                annotations = getReturnAnnos(useElem);
                break;

            case PARAMETER:
                annotations = getParameterAnnos(useElem);
                break;

            case FIELD:
            case LOCAL_VARIABLE:
            case RESOURCE_VARIABLE:
                annotations = getVariableAnnos(useElem);
                break;

            default:
                throw new BugInCF(
                        "TypeVarUseApplier::extractAndApply : "
                                + "Unhandled element kind "
                                + useElem.getKind()
                                + "useElem ( "
                                + useElem
                                + " ) "
                                + "declarationElem ( "
                                + declarationElem
                                + " ) ");
        }

        return annotations;
    }

    /** @return annotations on an element that apply to variable declarations */
    private static List<Attribute.TypeCompound> getVariableAnnos(final Element variableElem) {
        final VarSymbol varSymbol = (VarSymbol) variableElem;
        final List<Attribute.TypeCompound> annotations = new ArrayList<>();

        for (Attribute.TypeCompound anno : varSymbol.getRawTypeAttributes()) {

            TypeAnnotationPosition pos = anno.position;
            switch (pos.type) {
                case FIELD:
                case LOCAL_VARIABLE:
                case RESOURCE_VARIABLE:
                case EXCEPTION_PARAMETER:
                    annotations.add(anno);
                    break;

                default:
            }
        }

        return annotations;
    }

    /**
     * Currently, the metadata for storing annotations (i.e. the Attribute.TypeCompounds) is null
     * for binary-only parameters and type parameters. However, it is present on the method. So in
     * order to ensure that we correctly retrieve the annotations we need to index from the method
     * and retrieve the annotations from its metadata.
     *
     * @return a list of annotations that were found on METHOD_FORMAL_PARAMETERS that match the
     *     parameter index of the input element in the parent methods formal parameter list
     */
    private static List<Attribute.TypeCompound> getParameterAnnos(final Element paramElem) {
        final Element enclosingElement = paramElem.getEnclosingElement();
        if (!(enclosingElement instanceof ExecutableElement)) {
            throw new BugInCF(
                    "Bad element passed to TypeFromElement.getTypeParameterAnnotationAttributes: "
                            + "element: "
                            + paramElem
                            + " not found in enclosing executable: "
                            + enclosingElement);
        }

        final MethodSymbol enclosingMethod = (MethodSymbol) enclosingElement;

        final List<Attribute.TypeCompound> result = new ArrayList<>();
        if (enclosingMethod.getKind() != ElementKind.CONSTRUCTOR
                && enclosingMethod.getKind() != ElementKind.METHOD) {
            // Initializer blocks don't have parameters, so there is nothing to do.
            return result;
        }

        // TODO: for the parameter in a lambda expression, the enclosingMethod isn't
        // the lambda expression. Does this read the correct annotations?

        final int paramIndex = enclosingMethod.getParameters().indexOf(paramElem);
        final List<Attribute.TypeCompound> annotations = enclosingMethod.getRawTypeAttributes();

        for (final Attribute.TypeCompound typeAnno : annotations) {
            if (typeAnno.position.type == TargetType.METHOD_FORMAL_PARAMETER) {
                if (typeAnno.position.parameter_index == paramIndex) {
                    result.add(typeAnno);
                }
            }
        }

        return result;
    }

    /** @return the annotations on the return type of the input ExecutableElement */
    private static List<Attribute.TypeCompound> getReturnAnnos(final Element methodElem) {
        if (!(methodElem instanceof ExecutableElement)) {
            throw new BugInCF(
                    "Bad element passed to TypeVarUseApplier.getReturnAnnos:" + methodElem);
        }

        final MethodSymbol enclosingMethod = (MethodSymbol) methodElem;

        final List<Attribute.TypeCompound> annotations = enclosingMethod.getRawTypeAttributes();
        final List<Attribute.TypeCompound> result = new ArrayList<>();
        for (final Attribute.TypeCompound typeAnno : annotations) {
            if (typeAnno.position.type == TargetType.METHOD_RETURN) {
                result.add(typeAnno);
            }
        }

        return result;
    }
}
