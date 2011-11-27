package checkers.types;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;

import checkers.source.SourceChecker.CheckerError;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.util.TypesUtils;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/**
 * A utility class used to extract the annotations from an element and inserts
 * them into the elements type.
 */
public class TypeFromElement {
    /**
     * Whether to throw a CheckerError if an error in the elements was found.
     */
    private static final boolean strict = false;

    /**
     * Extracts type annotations from the element and inserts them into the
     * type of the element.
     *
     * The element could be one of:
     * 1. {@link TypeElement} of a class or an interface
     * 2. {@link ExecutableElement} of a method or a constructor
     * 3. {@link VariableElement} of a field or a method parameter
     * 4. {@link TypeParameterElement} of a method or a class type parameter
     *
     */
    public static void annotate(AnnotatedTypeMirror type, Element element) {
        if (element == null) {
            throw new CheckerError("TypeFromElement.annotate: element cannot be null");
        } else if (element.getKind().isField()) {
            annotate(type, (VariableElement) element);
        } else if (element instanceof TypeElement) {
            annotate((AnnotatedDeclaredType) type, (TypeElement) element);
        } else if (element instanceof ExecutableElement) {
            annotate((AnnotatedExecutableType) type, (ExecutableElement) element);
        } else if (element.getKind() == ElementKind.PARAMETER) {
            Element enclosing = element.getEnclosingElement();
            if (enclosing instanceof ExecutableElement) {
                ExecutableElement execElt = (ExecutableElement) enclosing;
                if (execElt.getParameters().contains(element)) {
                    int param_index = execElt.getParameters().indexOf(element);
                    for (Attribute.TypeCompound typeAnno : ((MethodSymbol) execElt).typeAnnotations)
                        if ((typeAnno.position.type == TargetType.METHOD_PARAMETER_GENERIC_OR_ARRAY)
                            && typeAnno.position.parameter_index == param_index) {
                            annotate(type, typeAnno);
                        } else if (strict) {
                            throw new CheckerError("TypeFromElement.annotate parameter: " +
                                    "invalid position " + typeAnno.position + " for annotation: " + typeAnno);
                        }
                } else if (strict) {
                    throw new CheckerError("TypeFromElement.annotate: parameter not found in enclosing executable");
                }
            } else if (strict) {
                throw new CheckerError("TypeFromElement.annotate: enclosing element not an executable");
            }
        } else if (element.getKind() == ElementKind.TYPE_PARAMETER) {
            Element enclosing = element.getEnclosingElement();
            if (enclosing instanceof TypeElement) {
                TypeElement clsElt = (TypeElement)enclosing;
                if (clsElt.getTypeParameters().contains(element)) {
                    int param_index = clsElt.getTypeParameters().indexOf(element);
                    for (Attribute.TypeCompound typeAnno : ((ClassSymbol) clsElt).typeAnnotations) {
                        switch (typeAnno.position.type) {
                        case CLASS_TYPE_PARAMETER_BOUND:
                        case CLASS_TYPE_PARAMETER_BOUND_GENERIC_OR_ARRAY:
                            if (typeAnno.position.parameter_index == param_index) {
                                annotatePossibleBound(type, typeAnno);
                            } else if (strict) {
                                throw new CheckerError("TypeFromElement.annotate: " +
                                        "invalid type paramter index " + typeAnno.position.parameter_index + " for annotation: " + typeAnno);
                            }
                            break;
                        default: if (strict) {
                            throw new CheckerError("TypeFromElement.annotate class type parameter: " +
                                    "invalid position " + typeAnno.position + " for annotation: " + typeAnno);
                        }
                        }
                    }
                } else if (strict) {
                    throw new CheckerError("TypeFromElement.annotate: class type parameter not found in enclosing element");
                }
            } else if (enclosing instanceof ExecutableElement) {
                ExecutableElement execElt = (ExecutableElement) enclosing;
                if (execElt.getTypeParameters().contains(element)) {
                    int param_index = execElt.getTypeParameters().indexOf(element);
                    for (Attribute.TypeCompound typeAnno : ((MethodSymbol) execElt).typeAnnotations) {
                        switch (typeAnno.position.type) {
                        case METHOD_TYPE_PARAMETER:
                        //case METHOD_TYPE_PARAMETER_GENERIC_OR_ARRAY:
                        case METHOD_TYPE_PARAMETER_BOUND:
                        case METHOD_TYPE_PARAMETER_BOUND_GENERIC_OR_ARRAY:
                            if (typeAnno.position.parameter_index == param_index) {
                                annotatePossibleBound(type, typeAnno);
                            }/* TODO: is this a problem or expected?
                              else if (strict) {
                                throw new CheckerError("TypeFromElement.annotate: " +
                                        "invalid method type parameter index " + typeAnno.position.parameter_index + " for annotation: " + typeAnno);
                            }*/
                            break;
                        case METHOD_RETURN:
                        case METHOD_RETURN_GENERIC_OR_ARRAY:
                        case METHOD_PARAMETER:
                        case METHOD_PARAMETER_GENERIC_OR_ARRAY:
                        case METHOD_RECEIVER:
                        // TODO? case METHOD_RECEIVER_GENERIC_OR_ARRAY:
                        case LOCAL_VARIABLE:
                        case LOCAL_VARIABLE_GENERIC_OR_ARRAY:
                        case NEW:
                        case NEW_GENERIC_OR_ARRAY:
                        case TYPECAST:
                        case TYPECAST_GENERIC_OR_ARRAY:
                            // Valid in this location, but handled elsewhere.
                            break;
                        default: if (strict) {
                            throw new CheckerError("TypeFromElement.annotate method type parameter: " +
                                    "invalid position " + typeAnno.position + " for annotation: " + typeAnno);
                        }
                        }
                    }
                } else if (strict) {
                    throw new CheckerError("TypeFromElement.annotate: method type parameter not found in enclosing element");
                }
            } else if (strict) {
                throw new CheckerError("TypeFromElement.annotate: enclosing element not a type or executable");
            }
        } else {
            throw new CheckerError("TypeFromElement.annotate: illegal argument: " + element.getKind());
        }
    }

    /**
     * Extracts type annotations from the element and inserts them into the
     * type of the element.
     *
     * the element needs to be that of a field
     * @param type  the type of the field
     * @param element the element of a field
     */
    public static void annotate(AnnotatedTypeMirror type, VariableElement element) {
        if (!element.getKind().isField()) {
            throw new CheckerError("TypeFromElement.annotate(VariableElement): " +
                    "invalid non-field element " + element + " [" + element.getKind() + "]");
        }

        VarSymbol symbol = (VarSymbol) element;

        addAnnotationsToElt(type, symbol.getAnnotationMirrors());

        for (Attribute.TypeCompound anno : symbol.typeAnnotations) {
            TypeAnnotationPosition pos = anno.position;
            switch (pos.type) {
            case FIELD_GENERIC_OR_ARRAY:
                annotate(type, anno);
                break;
            case FIELD:
            case NEW:
            case NEW_GENERIC_OR_ARRAY:
            case TYPECAST:
            case TYPECAST_GENERIC_OR_ARRAY:
                // Valid in this location, but handled elsewhere.
                break;
            default: if (strict) {
                throw new CheckerError("TypeFromElement.annotate(VariableElement): " +
                        "invalid position " + pos.type + " for annotation: " + anno);
            }
            }
        }
    }

    /**
     * Extracts type annotations from the element and inserts them into the
     * type of the element.
     *
     * the element needs to be that of a class or an interface
     *
     * @param type  the type of the class/interface
     * @param element the element of a class/interface
     */
    public static void annotate(AnnotatedDeclaredType type, TypeElement element) {
        ClassSymbol symbol = (ClassSymbol) element;

        // Annotate raw types
        type.addAnnotations(symbol.getAnnotationMirrors());

        List<AnnotatedTypeMirror> typeParameters = type.getTypeArguments();

        for (Attribute.TypeCompound anno : symbol.typeAnnotations) {
            TypeAnnotationPosition pos = anno.position;
            switch(pos.type) {
            case CLASS_TYPE_PARAMETER:
                if (pos.parameter_index >= 0 && pos.parameter_index < typeParameters.size()) {
                    AnnotatedTypeMirror typeParam = typeParameters.get(pos.parameter_index);
                    typeParam.addAnnotation(anno);
                    if (typeParam.getKind() == TypeKind.TYPEVAR) {
                        // Add an annotation on the type parameter also to the upper bound
                        ((AnnotatedTypeVariable) typeParam).getUpperBound().addAnnotation(anno);
                    } else {
                        throw new CheckerError("TypeFromElement.annotate(TypeElement): " +
                                "type parameter: " + typeParam + " is not a type variable");
                    }
                } else if (strict) {
                    throw new CheckerError("TypeFromElement.annotate(TypeElement): " +
                            "invalid parameter index " + pos.parameter_index + " for annotation: " + anno);
                }
                break;
            case CLASS_TYPE_PARAMETER_BOUND:
            case CLASS_TYPE_PARAMETER_BOUND_GENERIC_OR_ARRAY:
                if (pos.parameter_index >= 0 && pos.parameter_index < typeParameters.size()) {
                    List<AnnotatedTypeMirror> bounds = getBounds(typeParameters.get(pos.parameter_index));
                    if (pos.bound_index >= 0 && pos.bound_index < bounds.size()) {
                        annotate(bounds.get(pos.bound_index), anno);
                    } else if (strict) {
                        throw new CheckerError("TypeFromElement.annotate(TypeElement): " +
                                "invalid bound index " + pos.bound_index + " for annotation: " + anno);
                    }
                } else if (strict) {
                    throw new CheckerError("TypeFromElement.annotate(TypeElement): " +
                            "invalid parameter index " + pos.parameter_index + " for annotation: " + anno);
                }
                break;
            case CLASS_EXTENDS:
            case CLASS_EXTENDS_GENERIC_OR_ARRAY:
            case LOCAL_VARIABLE: // ? TODO: check why those appear on a type element
            case LOCAL_VARIABLE_GENERIC_OR_ARRAY: // ?
            case NEW: // ?
            case NEW_GENERIC_OR_ARRAY: // ?
            case TYPECAST: // ?
            case TYPECAST_GENERIC_OR_ARRAY: // ?
                // Valid in this location, but handled elsewhere.
                break;
            default: if (strict) {
                throw new CheckerError("TypeFromElement.annotate(TypeElement): " +
                        "invalid position " + pos.type + " for annotation: " + anno);
            }
            }
        }
    }

    public static void annotateSupers(List<AnnotatedDeclaredType> supertypes, TypeElement element) {
        final ClassSymbol symbol = (ClassSymbol)element;
        // Add the ones in the extends clauses
        final boolean hasSuperClass = element.getSuperclass().getKind() != TypeKind.NONE;

        AnnotatedDeclaredType superClassType = hasSuperClass ? supertypes.get(0) : null;
        List<AnnotatedDeclaredType> superInterfaces = hasSuperClass ? tail(supertypes) : supertypes;
        for (Attribute.TypeCompound anno : symbol.typeAnnotations) {
            TypeAnnotationPosition pos = anno.position;
            switch(pos.type) {
            case CLASS_EXTENDS:
            case CLASS_EXTENDS_GENERIC_OR_ARRAY:
                if (pos.type_index == -1 && superClassType != null) {
                    annotate(superClassType, anno);
                } else if (pos.type_index >= 0 && pos.type_index < superInterfaces.size()) {
                    annotate(superInterfaces.get(pos.type_index), anno);
                } else if (strict) {
                    throw new CheckerError("TypeFromElement.annotateSupers: " +
                            "invalid type index " + pos.type_index + " for annotation: " + anno);
                }
                break;
            case CLASS_TYPE_PARAMETER:
            case CLASS_TYPE_PARAMETER_BOUND:
            case CLASS_TYPE_PARAMETER_BOUND_GENERIC_OR_ARRAY:
                // Valid in this location, but handled elsewhere.
                break;
            default: if (strict) {
                throw new CheckerError("TypeFromElement.annotateSupers: " +
                        "invalid position " + pos.type + " for annotation: " + anno);
            }
            }
        }
    }

    /**
     * Extracts type annotations from the element and inserts them into the
     * type of the element.
     *
     * the element needs to be that of a method or a constructor.
     *
     * @param type  the type of the method
     * @param element the element of a method
     */
    public static void annotate(AnnotatedExecutableType type, ExecutableElement element) {
        MethodSymbol symbol = (MethodSymbol) element;

        // Add annotations on the return type
        addAnnotationsToElt(type.getReturnType(), symbol.getAnnotationMirrors());

        // Add annotations on the param raws
        final List<AnnotatedTypeMirror> thrown = type.getThrownTypes();
        final List<AnnotatedTypeMirror> params = type.getParameterTypes();
        final List<AnnotatedTypeVariable> typeParams = type.getTypeVariables();

        for (int i = 0; i < params.size(); ++i) {
            addAnnotationsToElt(params.get(i), element.getParameters().get(i).getAnnotationMirrors());
        }

        for (Attribute.TypeCompound typeAnno : symbol.typeAnnotations) {
            final TypeAnnotationPosition pos = typeAnno.position;

            switch (typeAnno.position.type) {
            case METHOD_RECEIVER:
            //TODO: case METHOD_RECEIVER_GENERIC_OR_ARRAY:
                annotate(type.getReceiverType(), typeAnno);
                break;

            case METHOD_RETURN:
            case METHOD_RETURN_GENERIC_OR_ARRAY:
                annotate(type.getReturnType(), typeAnno);
                break;

            case METHOD_PARAMETER:
            case METHOD_PARAMETER_GENERIC_OR_ARRAY:
                if (pos.parameter_index >= 0 && pos.parameter_index < params.size()) {
                    annotate(params.get(pos.parameter_index), typeAnno);
                } else if (strict) {
                    // TODO: 
                    throw new CheckerError("TypeFromElement.annotate(ExecutableElement): " +
                            "invalid parameter index " + pos.parameter_index + " for annotation: " + typeAnno);
                }
                break;

            case THROWS:
            //case THROWS_GENERIC_OR_ARRAY:
                if (pos.type_index >= 0 && pos.type_index < thrown.size()) {
                    annotate(thrown.get(pos.type_index), typeAnno);
                } else if (strict) {
                    throw new CheckerError("TypeFromElement.annotate(ExecutableElement): " +
                            "invalid throws index " + pos.type_index + " for annotation: " + typeAnno);
                }
                break;

            case METHOD_TYPE_PARAMETER:
            //case METHOD_TYPE_PARAMETER_GENERIC_OR_ARRAY:
                if (pos.parameter_index >= 0 && pos.parameter_index < typeParams.size()) {
                    annotate(typeParams.get(pos.parameter_index), typeAnno);
                } else if (strict) {
                    throw new CheckerError("TypeFromElement.annotate(ExecutableElement): " +
                            "invalid method type parameter index " + pos.parameter_index + " for annotation: " + typeAnno);
                }
                break;

            case METHOD_TYPE_PARAMETER_BOUND:
            case METHOD_TYPE_PARAMETER_BOUND_GENERIC_OR_ARRAY:
                if (pos.parameter_index >= 0 && pos.parameter_index < typeParams.size()) {
                    List<AnnotatedTypeMirror> bounds = getBounds(typeParams.get(pos.parameter_index));
                    if (pos.bound_index >= 0 && pos.bound_index < bounds.size()) {
                        annotate(bounds.get(pos.bound_index), typeAnno);
                    } else if (strict) {
                        throw new CheckerError("TypeFromElement.annotate(ExecutableElement): " +
                                "invalid method type parameter bound index " + pos.bound_index + " for annotation: " + typeAnno);
                    }
                } else if (strict) {
                    // TODO: parameter_index is -1 a few times in Daikon. What does that mean?
                    throw new CheckerError("TypeFromElement.annotate(ExecutableElement): " +
                            "invalid method type parameter index " + pos.parameter_index + " for annotation: " + typeAnno);
                }
                break;
            case WILDCARD_BOUND:
            case WILDCARD_BOUND_GENERIC_OR_ARRAY:
                // TODO: Handle these cases
                // System.out.println("TypeFromElement for element: " + element + " anno: " + typeAnno + "pos: " + pos);
                break;
            case LOCAL_VARIABLE:
            case LOCAL_VARIABLE_GENERIC_OR_ARRAY:
            case NEW:
            case NEW_GENERIC_OR_ARRAY:
            case TYPECAST:
            case TYPECAST_GENERIC_OR_ARRAY:
                // Valid in this location, but handled elsewhere.
                break;
            default: if (strict) {
                throw new CheckerError("TypeFromElement.annotate(ExecutableElement): " +
                        "invalid position " + pos.type + " for annotation: " + typeAnno);
            }
            }
        }
    }

    static void addAnnotationsToElt(AnnotatedTypeMirror type,
            List<? extends AnnotationMirror> annotations) {
        AnnotatedTypes.innerMostType(type).addAnnotations(annotations);
    }

    static void clearAnnotationsFromElt(AnnotatedTypeMirror type) {
        AnnotatedTypes.innerMostType(type).clearAnnotations();
    }

    /**
     * Extracts type annotations from the element and inserts them into the
     * type of the element.
     *
     * the element needs to be that of a field
     * @param type  the type of the field
     * @param element the element of a field
     */
    private static void annotate(AnnotatedTypeMirror type, Attribute.TypeCompound anno) {
        TypeAnnotationPosition pos = anno.position;
        if (!pos.type.hasLocation()) {
            type.addAnnotation(anno);
        } else {
            annotate(type, pos.location, Collections.singletonList(anno));
        }
    }

    private static void annotatePossibleBound(AnnotatedTypeMirror type, Attribute.TypeCompound anno) {
        if (!anno.position.type.hasBound()) {
            annotate(type, anno);
        } else {
            if (type.getKind() != TypeKind.TYPEVAR
                    && type.getKind() != TypeKind.WILDCARD) {
                if (strict) {
                    throw new CheckerError("TypeFromElement.annotatePossibleBound: " +
                            "trying to add a bound annotation: " + anno +
                            "to something that is not a type variable or wildcard: " + type);
                }
                return;
            }
            List<AnnotatedTypeMirror> bounds = getBounds(type);
            int boundIndex = anno.position.bound_index;
            if (boundIndex >= 0 && boundIndex < bounds.size()) {
                annotate(bounds.get(boundIndex), anno);
            } else if (strict) {
                throw new CheckerError("TypeFromElement.annotatePossibleBound: " +
                        "invalid boundIndex " + boundIndex + " for annotation: " + anno);
            }
        }
    }

    private static void annotate(AnnotatedTypeMirror type, List<Integer> location, List<? extends AnnotationMirror> annotations) {
        if (location.isEmpty()) {
            type.addAnnotations(annotations);
        } else if (type.getKind() == TypeKind.DECLARED) {
            annotate((AnnotatedDeclaredType)type, location, annotations);
        } else if (type.getKind() == TypeKind.ARRAY) {
            annotate((AnnotatedArrayType)type, location, annotations);
        } else {
            throw new CheckerError("TypeFromElement.annotate(ATM): only declared types and arrays can have annotations");
        }
    }

    private static void annotate(AnnotatedDeclaredType type,  List<Integer> location, List<? extends AnnotationMirror> annotations) {
        if (location.isEmpty()) {
            type.addAnnotations(annotations);
        } else if (location.get(0) < type.getTypeArguments().size()) {
            annotate(type.getTypeArguments().get(location.get(0)), tail(location), annotations);
        } else if (strict) {
            throw new CheckerError("TypeFromElement.annotate(ADT): " +
                    "invalid locations " + location + " for annotations: " + annotations);
        }
    }

    // Dealing with arrays requires much testing
    private static void annotate(AnnotatedArrayType type, List<Integer> location, List<? extends AnnotationMirror> annotations) {
        if (location.isEmpty()) {
            type.addAnnotations(annotations);
        } else if (location.size() == 1) {
            int arrayIndex = location.get(0);
            List<AnnotatedTypeMirror> arrays = createArraysList(type);
            if (arrayIndex < arrays.size())
                arrays.get(arrayIndex).addAnnotations(annotations);
        } else if (strict) {
            throw new CheckerError("TypeFromElement.annotate(AAT): " +
                    "invalid location " + location + " for annotations: " + annotations);
        }
    }

    private static List<AnnotatedTypeMirror> createArraysList(AnnotatedArrayType array) {
        LinkedList<AnnotatedTypeMirror> arrays = new LinkedList<AnnotatedTypeMirror>();

        // I think that type can never be null
        AnnotatedTypeMirror type = array;
        while (type != null && type.getKind() == TypeKind.ARRAY) {
            arrays.addFirst(type);
            type = ((AnnotatedArrayType)type).getComponentType();
        }

        // adding the component type
        if (type != null) {
            arrays.addFirst(type);
        } else if (strict) {
            throw new CheckerError("TypeFromElement.createArraysList: " +
                    "null component type for array: " + array);
        }

        return arrays;
    }

    private static <T> List<T> tail(List<T> list) {
        return list.subList(1, list.size());
    }

    /**
     * Return the bounds for the type and needs to be either
     * {@link AnnotatedTypeVariable} or
     * {@link AnnotatedWildcard}.  If the type has no bounds, it returns an
     * empty list.
     *
     * @param type a wildcard or type variable
     * @return bounds of a type variable
     */
    private static List<AnnotatedTypeMirror> getBounds(AnnotatedTypeMirror type) {
        AnnotatedTypeMirror bound = null;
        if (type.getKind() == TypeKind.TYPEVAR) {
            bound = ((AnnotatedTypeVariable)type).getUpperBound();
        } else if (type.getKind() == TypeKind.WILDCARD) {
            AnnotatedWildcardType wt = (AnnotatedWildcardType)type;
            if (wt.getExtendsBound() != null) {
                bound = wt.getExtendsBound();
            } else {
                bound = wt.getSuperBound();
            }
        } else {
            throw new CheckerError("TypeFromElement.getBounds: " +
                    "type has no bounds: " + type + " [" + type.getKind() + "]");
        }

        if (bound == null) {
            return Collections.emptyList();
        } else if (!TypesUtils.isAnonymousType(bound.getUnderlyingType())) {
            return Collections.singletonList(bound);
        } else {
            return Collections.unmodifiableList(bound.directSuperTypes());
        }
    }
}
