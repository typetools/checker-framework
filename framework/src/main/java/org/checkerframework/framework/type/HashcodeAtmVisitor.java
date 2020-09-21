package org.checkerframework.framework.type;

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
public class HashcodeAtmVisitor extends SimpleAnnotatedTypeScanner<Integer, Void> {

    /** Creates a {@link HashcodeAtmVisitor}. */
    public HashcodeAtmVisitor() {
        super(Integer::sum, 0);
    }

    /**
     * Generates hashcode for type using the underlying type and the primary annotation. This method
     * does not descend into component types (this occurs in the scan method)
     *
     * @param type the type
     */
    @Override
    protected Integer defaultAction(AnnotatedTypeMirror type, Void v) {
        // To differentiate between partially initialized type's (which may have null components)
        // and fully initialized types, null values are allowed
        if (type == null) {
            return 0;
        }

        return type.getAnnotations().toString().hashCode() * 17
                + type.getUnderlyingType().toString().hashCode() * 13;
    }
}
