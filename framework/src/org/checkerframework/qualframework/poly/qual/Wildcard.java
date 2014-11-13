package org.checkerframework.qualframework.poly.qual;

import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;

/**
 * When using {@link SimpleQualifierParameterAnnotationConverter}, this enum specifies
 * allows specifying super and extends bounds, e.g @Tainted(wildcard=Extends) MyClass is equivelant
 * to MyClass&lt;&lt;? extends @Tainted&gt;&gt;.
 */
public enum Wildcard {
    NONE, EXTENDS, SUPER
}
