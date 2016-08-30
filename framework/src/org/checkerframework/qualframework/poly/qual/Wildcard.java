package org.checkerframework.qualframework.poly.qual;

/**
 * When using {@link org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter}, this enum
 * allows specifying super and extends bounds, e.g {@code {@literal @}Tainted(wildcard=Extends) MyClass} is equivalent
 * to MyClass《? extends {@literal @}Tainted》.
 */
public enum Wildcard {
    NONE,
    EXTENDS,
    SUPER
}
