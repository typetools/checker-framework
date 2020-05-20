package org.checkerframework.framework.type;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;

/**
 * Computes the hashcode of an AnnotatedTypeMirror using the underlying type and primary annotations
 * of the type and its component type.
 *
 * <p>This class should be synchronized with EqualityAtmComparer.
 *
 * @see org.checkerframework.framework.type.EqualityAtmComparer for more details.
 *     <p>This is used by AnnotatedTypeMirror.hashcode.
 */
public class HashcodeAtmVisitor extends SimpleAnnotatedTypeScanner<@Nullable Integer, Void> {

    /** Used to combine the hashcodes of component types or a type and its component types. */
    @Override
    protected @Nullable Integer reduce(@Nullable Integer hashcode1, @Nullable Integer hashcode2) {
        if (hashcode1 == null) {
            return hashcode2;
        }

        if (hashcode2 == null) {
            return hashcode1;
        }

        return hashcode1 + hashcode2;
    }

    /**
     * Generates hashcode for type using the underlying type and the primary annotation. This method
     * does not descend into component types (this occurs in the scan method)
     *
     * @param type the type
     */
    @Override
    protected @Nullable Integer defaultAction(AnnotatedTypeMirror type, Void v) {
        // To differentiate between partially initialized type's (which may have null components)
        // and fully initialized types, null values are allowed
        if (type == null) {
            return null;
        }

        return type.getAnnotations().toString().hashCode() * 17
                + type.getUnderlyingType().toString().hashCode() * 13;
    }
}
