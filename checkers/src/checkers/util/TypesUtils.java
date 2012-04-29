package checkers.util;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

/**
 * A utility class that helps with {@link TypeMirror}s.
 *
 */
// TODO: This class needs significant restructuring
public final class TypesUtils {

    // Cannot be instantiated
    private TypesUtils() { throw new AssertionError("un-initializable class"); }

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
     * Checks if the type represents a java.lang.Object declared type.
     *
     * @param type  the type
     * @return true iff type represents java.lang.Object
     */
    public static boolean isObject(TypeMirror type) {
        return isDeclaredOfName(type, "java.lang.Object");
    }

    /**
     * Checks if the type represents a java.lang.Class declared type.
     *
     * @param type  the type
     * @return true iff type represents java.lang.Class
     */
    public static boolean isClass(TypeMirror type) {
        return isDeclaredOfName(type, "java.lang.Class");
    }

    /**
     * Checks if the type represents a java.lang.String declared type.
     * TODO: it would be cleaner to use String.class.getCanonicalName(), but
     *   the two existing methods above don't do that, I guess for performance reasons.
     *
     * @param type  the type
     * @return true iff type represents java.lang.String
     */
    public static boolean isString(TypeMirror type) {
        return isDeclaredOfName(type, "java.lang.String");
    }

    /**
     * Check if the type represent a declared type of the given qualified name
     *
     * @param type the type
     * @return type iff type represents a declared type of the qualified name
     */
    public static boolean isDeclaredOfName(TypeMirror type, CharSequence qualifiedName) {
        return type.getKind() == TypeKind.DECLARED
            && getQualifiedName((DeclaredType)type).contentEquals(qualifiedName);

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

        String qualifiedName = getQualifiedName((DeclaredType)type).toString();

        return (qualifiedName.equals("java.lang.Boolean")
                || qualifiedName.equals("java.lang.Byte")
                || qualifiedName.equals("java.lang.Character")
                || qualifiedName.equals("java.lang.Short")
                || qualifiedName.equals("java.lang.Integer")
                || qualifiedName.equals("java.lang.Long")
                || qualifiedName.equals("java.lang.Double")
                || qualifiedName.equals("java.lang.Float"));
    }

    /** @return type represents a Throwable type (e.g. Exception, Error) **/
    public static boolean isThrowable(TypeMirror type) {
        while (type != null && type.getKind() == TypeKind.DECLARED) {
            DeclaredType dt = (DeclaredType) type;
            TypeElement elem = (TypeElement) dt.asElement();
            Name name = elem.getQualifiedName();
            if ("java.lang.Throwable".contentEquals(name))
                return true;
            type = elem.getSuperclass();
        }
        return false;
    }
}
