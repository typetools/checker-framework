package checkers.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import checkers.nullness.quals.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * A Utility class for analyzing {@code Element}s
 *
 */
public class ElementUtils {

    // Cannot be instantiated
    private ElementUtils() { throw new AssertionError("un-initializable class"); }

    /**
     * Returns the innermost type element enclosing the given element
     *
     * @param elem the enclosed element of a class
     * @return  the innermost type element
     */
    public static TypeElement enclosingClass(final Element elem) {
        Element result = elem;
        while (result != null && !result.getKind().isClass()
                && !result.getKind().isInterface()) {
            /*@Nullable*/ Element encl = result.getEnclosingElement();
            result = encl;
        }
        return (TypeElement) result;
    }

    /**
     * Returns the innermost package element enclosing the given element.
     * The same effect as {@link javax.lang.model.util.Elements#getPackageOf(Element)}.
     *
     * @param elem the enclosed element of a package
     * @return the innermost package element
     *
     */
    public static PackageElement enclosingPackage(final Element elem) {
        Element result = elem;
        while (result != null && result.getKind()!=ElementKind.PACKAGE) {
            /*@Nullable*/ Element encl = result.getEnclosingElement();
            result = encl;
        }
        return (PackageElement) result;
    }

    /**
     * Returns true if the element is a static element: whether it is a static
     * field, static method, or static class
     *
     * @param element
     * @return true if element is static
     */
    public static boolean isStatic(Element element) {
        return element.getModifiers().contains(Modifier.STATIC);
    }

    /**
     * Returns true if the element is a final element: a final field, final
     * method, or final class
     *
     * @param element
     * @return true if the element is final
     */
    public static boolean isFinal(Element element) {
        return element.getModifiers().contains(Modifier.FINAL);
    }

    /**
     * Returns the {@code TypeMirror} for usage of Element as a value. It
     * returns the return type of a method element, the class type of a
     * constructor, or simply the type mirror of the element itself.
     *
     * @param element
     * @return  the type for the element used as a value
     */
    public static TypeMirror getType(Element element) {
        if (element.getKind() == ElementKind.METHOD)
            return ((ExecutableElement)element).getReturnType();
        else if (element.getKind() == ElementKind.CONSTRUCTOR)
            return enclosingClass(element).asType();
        else
            return element.asType();
    }

    /**
     * Returns the qualified name of the inner most class enclosing
     * the provided {@code Element}
     *
     * @param element
     *            an element enclosed by a class, or a
     *            {@code TypeElement}
     * @return The qualified {@code Name} of the innermost class
     *         enclosing the element
     */
    public static /*@Nullable*/ Name getQualifiedClassName(Element element) {
        if (element.getKind() == ElementKind.PACKAGE) {
            PackageElement elem = (PackageElement) element;
            return elem.getQualifiedName();
        }

        TypeElement elem = enclosingClass(element);
        if (elem == null)
            return null;

        return elem.getQualifiedName();
    }

    /**
     * Returns a verbose name that identifies the element.
     */
    public static String getVerboseName(Element elt) {
        return getQualifiedClassName(elt) + " " + elt.toString();
    }

    /**
     * Check if the element is an element for 'java.lang.Object'
     *
     * @param element   the type element
     * @return true iff the element is java.lang.Object element
     */
    public static boolean isObject(TypeElement element) {
        return element.getQualifiedName().contentEquals("java.lang.Object");
    }

    /**
     * Returns true if the element is a constant time reference
     */
    public static boolean isCompileTimeConstant(Element elt) {
        return elt != null
            && elt.getKind() == ElementKind.FIELD
            && ((VariableElement)elt).getConstantValue() != null;
    }

    /**
     * Returns the field of the class
     */
    public static VariableElement findFieldInType(TypeElement type, String name) {
        for (VariableElement field: ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (field.getSimpleName().toString().equals(name)) {
                return field;
            }
        }
        return null;
    }

    public static Set<VariableElement> findFieldsInType(TypeElement type, Collection<String> names) {
        Set<VariableElement> results = new HashSet<VariableElement>();
        for (VariableElement field: ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (names.contains(field.getSimpleName().toString())) {
                results.add(field);
            }
        }
        return results;
    }

    public static boolean isError(Element element) {
        return element.getClass().getName().equals("com.sun.tools.javac.comp.Resolve$SymbolNotFoundError");
    }
}
