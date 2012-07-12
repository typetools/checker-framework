package checkers.util;

import static javax.lang.model.util.ElementFilter.methodsIn;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;

import checkers.quals.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

/**
 * A Utilities class that helps with {@link Element}s.
 * 
 */
// TODO: This class needs significant restructuring
@DefaultQualifier("checkers.nullness.quals.NonNull")
public final class TypesUtils {

    private ProcessingEnvironment env;

    /**
     * Constructor for TypeUtils.
     * 
     * @param environment
     *            the {@code ProcessingEnvironment} of the Annotation
     *            Type Processor
     */
    public TypesUtils(ProcessingEnvironment environment) {
        this.env = environment;
    }

    /**
     * A utility method that takes the element for a class/interface
     * type and returns a set of {@link TypeElement}s representing all
     * of the supertypes of that type.
     * 
     * @param subtype
     *            the element of the type for which all supertypes
     *            will be obtained
     * @return an unmodifiable set of supertypes for {@code subtype},
     */
    public Set<TypeMirror> superTypes(TypeMirror subtype) {
        Set<TypeMirror> supertypes = new HashSet<TypeMirror>();
        if (subtype == null) 
            return supertypes;

        // Set up a stack containing the type mirror of subtype, which
        // is our starting point.
        Deque<TypeMirror> stack = new ArrayDeque<TypeMirror>();
        stack.push(subtype);

        while (!stack.isEmpty()) {
            TypeMirror current = stack.pop();

            // For each direct supertype of the current type, if it
            // hasn't already been visited, push it onto the stack and
            // add it to our supertypes set.
            for (TypeMirror supertype : env.getTypeUtils()
                    .directSupertypes(current)) {
                if (!supertypes.contains(supertype)) {
                    stack.push(supertype);
                    supertypes.add(supertype);
                }
            }
        }

        return Collections.</*@NonNull*/ TypeMirror>unmodifiableSet(supertypes);
    }

    /**
     * A utility method that takes a Method element and returns a set
     * of all elements that this method overrides (as
     * {@link javax.lang.model.element.ExecutableElement}s)
     * 
     * @param method
     *            the overriding method
     * @return an unmodifiable set of {@link ExecutableElement}s
     *         representing the elements that method overrides
     */
    public Map<DeclaredType, ExecutableElement> overriddenMethods(
            ExecutableElement method) {
        Set<TypeMirror> supertypes =
            superTypes(method.getEnclosingElement().asType());
        return overriddenMethods(method, supertypes);
    }

    /**
     * A utility method that takes the element for a method and the
     * set of all supertypes of the method's containing class and
     * returns the set of all elements that method overrides (as
     * {@link javax.lang.model.element.ExecutableElement}s).
     * 
     * @param method
     *            the overriding method
     * @param supertypes
     *            the set of supertypes to check for methods that are
     *            overriden by {@code method}
     * @return an unmodified set of {@link ExecutableElement}s
     *         representing the elements that {@code method} overrides
     *         among {@code supertypes}
     */
    public Map<DeclaredType, ExecutableElement> overriddenMethods(
            ExecutableElement method, Set<TypeMirror> supertypes) {

        Map<DeclaredType, ExecutableElement> overrides =
            new HashMap<DeclaredType, ExecutableElement>();
        
        for (TypeMirror supertype : supertypes) {
            /*@Nullable*/ TypeElement superElement = 
                (TypeElement) env.getTypeUtils().asElement(supertype);
            assert superElement != null; /*nninvariant*/
            // For all method in the supertype, add it to the set if
            // it overrides the given method.
            for (ExecutableElement supermethod : methodsIn(superElement
                    .getEnclosedElements())) {
                if (env.getElementUtils().overrides(method, supermethod,
                        superElement)) {
                    overrides.put((DeclaredType) supertype, supermethod);
                    break;
                }
            }
        }

        return Collections.</*@NonNull*/ DeclaredType,
            /*@NonNull*/ ExecutableElement>unmodifiableMap(overrides);
    }

    /**
     * Gets the fully qualified name for a provided type.  It returns an empty
     * name if type is an anonymous type.
     * 
     * @param type the declared type
     * @return the name corresponding to that type
     */
    public static Name getQualifiedName(DeclaredType type) {
        TypeElement element = (TypeElement) type.asElement();
        return element.getQualifiedName();
    }

    /**
     * Checks if the type represents a java.lang.Object declared type
     * 
     * @param type  the type
     * @return true iff type represents java.lang.Object
     */
    public static boolean isObject(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED &&
            getQualifiedName((DeclaredType)type).contentEquals("java.lang.Object");
    }
    
    /**
     * Checks if the type represents an anonymous type, e.g. as a result of an 
     * intersection type
     * 
     * @param type  the declared type
     * @return true iff the type represents an anonymous type.
     */
    public static boolean isAnonymousType(TypeMirror type) {
        return ((type.getKind() == TypeKind.DECLARED) &&
                (getQualifiedName((DeclaredType)type).length() == 0));
    }
    
    public static boolean isBoxedPrimitive(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED)
            return false;
        
        Name qualifiedName = getQualifiedName((DeclaredType)type);

        return (qualifiedName.contentEquals("java.lang.String") ||
                qualifiedName.contentEquals("java.lang.Boolean") ||
                qualifiedName.contentEquals("java.lang.Byte") ||
                qualifiedName.contentEquals("java.lang.Character") ||
                qualifiedName.contentEquals("java.lang.Enum") ||
                qualifiedName.contentEquals("java.lang.Short") ||
                qualifiedName.contentEquals("java.lang.Integer") ||
                qualifiedName.contentEquals("java.lang.Number") ||
                qualifiedName.contentEquals("java.lang.Long") ||
                qualifiedName.contentEquals("java.lang.Double") ||
                qualifiedName.contentEquals("java.lang.Float") ||
                qualifiedName.contentEquals("java.lang.Class"));
    }
}
