package org.checkerframework.qualframework.poly.qual;

public class DefaultValue {
    /**
     * The default "Target" in an annotation is the primary qualifier.
     * We can't use null in the annotation, so we use this special value.
     */
    public static final String PRIMARY_TARGET = "_primary";
}
