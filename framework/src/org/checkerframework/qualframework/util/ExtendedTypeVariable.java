package org.checkerframework.qualframework.util;

import javax.lang.model.element.Element;

/** {@link ExtendedTypeMirror} variant for {@link javax.lang.model.type.TypeVariable}.
 *
 * Unlike {@link javax.lang.model.type.TypeVariable}, this interface does not
 * provide methods to obtain the upper and lower bounds of the type variable.
 * The bounds should be determined by inspecting the {@link Element} returned
 * by {@link asElement}. */
public interface ExtendedTypeVariable extends ExtendedReferenceType {
    /** Returns the element corresponding to this type variable. */
    Element asElement();

    /** Get the {@link ExtendedParameterDeclaration} for the declaration of
     * this type variable. */
    ExtendedParameterDeclaration getDeclaration();
}
