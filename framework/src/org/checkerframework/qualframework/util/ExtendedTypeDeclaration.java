package org.checkerframework.qualframework.util;

import java.util.List;
import javax.lang.model.element.Element;

/** {@link ExtendedTypeMirror} variant for {@link
 * javax.lang.model.type.DeclaredType}s representing declarations. */
public interface ExtendedTypeDeclaration extends ExtendedTypeMirror {
    /** Returns the element corresponding to this type. */
    Element asElement();
    /** Returns the type of the innermost enclosing instance, or a {@link
     * ExtendedNoType} of kind <code>NONE</code> if there is no enclosing
     * instance.  Only types corresponding to inner classes have an enclosing
     * instance. */
    ExtendedTypeMirror getEnclosingType();
    /**
     * Returns the type parameters declared by this type. For a type nested
     * within a parameterized type (such as
     * <code>Outer&lt;String&gt;.Inner&lt;Number&gt;</code>), only the type
     * parameters of the innermost type are included.
     */
    List<? extends ExtendedParameterDeclaration> getTypeParameters();
}

