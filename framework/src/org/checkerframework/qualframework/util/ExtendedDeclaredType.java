package org.checkerframework.qualframework.util;

import java.util.List;
import javax.lang.model.element.Element;

/** {@link ExtendedTypeMirror} variant for {@link javax.lang.model.type.DeclaredType}. */
public interface ExtendedDeclaredType extends ExtendedReferenceType {
    /** Returns the element corresponding to this type. */
    Element asElement();
    /** Returns the type of the innermost enclosing instance, or a {@link
     * ExtendedNoType} of kind <code>NONE</code> if there is no enclosing
     * instance.  Only types corresponding to inner classes have an enclosing
     * instance. */
    ExtendedTypeMirror getEnclosingType();
    /**
     * Returns the actual type arguments of this type. For a type nested within
     * a parameterized type (such as
     * <code>Outer&lt;String&gt;.Inner&lt;Number&gt;</code>), only the type
     * arguments of the innermost type are included.
     */
    List<? extends ExtendedTypeMirror> getTypeArguments();
}

