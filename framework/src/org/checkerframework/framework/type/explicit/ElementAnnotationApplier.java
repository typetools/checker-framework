package org.checkerframework.framework.type.explicit;

/**
 *  A common interface for classes used to extract annotations from a Javac Element
 *  and apply them to a type.
 */
interface ElementAnnotationApplier {
    public void extractAndApply();
}
