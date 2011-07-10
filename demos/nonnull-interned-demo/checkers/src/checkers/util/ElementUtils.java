package checkers.util;

import checkers.quals.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * A Utility class for analyzing {@code Element}s
 * 
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class ElementUtils {

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
            @Nullable Element encl = result.getEnclosingElement();
            assert encl != null; /*nninvariant*/
            result = encl;
        }
        return (TypeElement) result;
    }

    /**
     * Checks if the element is a static element, whether it is a static field, static method, or static class
     * 
     * @param element
     * @return true if element is static
     */
    public static boolean isStatic(Element element) {
        for (Modifier modifier : element.getModifiers()) {
            if (modifier == Modifier.STATIC)
                return true;
        }
        return false;
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
    public @Nullable Name getQualifiedClassName(Element element) {
        if (element.getKind() == ElementKind.PACKAGE) {
            PackageElement elem = (PackageElement) element;
            return elem.getQualifiedName();
        } else {
            TypeElement elem = enclosingClass(element);
            if (elem == null)
                return null;
            else
                return elem.getQualifiedName();
        }
    }

}
