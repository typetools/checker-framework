package org.checkerframework.qualframework.util;

import javax.lang.model.element.Element;

/** {@link ExtendedTypeMirror} variant for {@link
 * javax.lang.model.type.TypeVariable}s representing declarations.
 */
public interface ExtendedParameterDeclaration extends ExtendedTypeMirror {
    /** Returns the element corresponding to this type variable. */
    Element asElement();
}
