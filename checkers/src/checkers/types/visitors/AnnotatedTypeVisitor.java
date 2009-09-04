package checkers.types.visitors;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.*;

/**
 * A Visitor class of annotated types, in the style of the visitor
 * design pattern.
 *
 * A visitor of types, in the style of the visitor design pattern.
 * Classes implementing this interface are used to operate on a type
 * when the kind of type is unknown at compile time. When a visitor is
 * passed to a type's accept method, the visitXYZ method most
 * applicable to that type is invoked.
 *
 * Classes implementing this interface may or may not throw a
 * NullPointerException if the additional parameter p is null; see
 * documentation of the implementing class for details.
 *
 * @param &lt;R&gt;
 *            the return type of this visitor's methods. Use Void for
 *            visitors that do not need to return results.
 *
 * @param &lt;P&gt;
 *            the type of the additional parameter to this visitor's
 *            methods. Use Void for visitors that do not need an
 *            additional parameter.
 *            <P>
 */
public interface AnnotatedTypeVisitor<R, P> {

    /**
     * A Convenience method equivalent to {@code v.visit(t, null)}.
     *
     * @param type  the type to visit
     * @return a visitor-specified result
     */
    public R visit(AnnotatedTypeMirror type);

    /**
     * Visits a type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    public R visit(AnnotatedTypeMirror type, P p);

    /**
     * Visits a declared type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
//    public R visitType(AnnotatedTypeMirror type, P p);

    /**
     * Visits a declared type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    public R visitDeclared(AnnotatedDeclaredType type, P p);

    /**
     * Visits an executable type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    public R visitExecutable(AnnotatedExecutableType type, P p);

    /**
     * Visits an array type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    public R visitArray(AnnotatedArrayType type, P p);

    /**
     * Visits a type variable.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    public R visitTypeVariable(AnnotatedTypeVariable type, P p);

    /**
     * Visits a primitive type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */

    public R visitPrimitive(AnnotatedPrimitiveType type, P p);

    /**
     * Visits NoType type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    public R visitNoType(AnnotatedNoType type, P p);

    /**
     * Visits a {@code null} type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    public R visitNull(AnnotatedNullType type, P p);

    /**
     * Visits a wildcard type.
     *
     * @param type  the type to visit
     * @param p a visitor-specified parameter
     * @return a visitor-specified result
     */
    public R visitWildcard(AnnotatedWildcardType type, P p);
}
