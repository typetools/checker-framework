package org.checkerframework.framework.type;

import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;

public class HashcodeAtmVisitor extends AnnotatedTypeScanner<Integer, Void> {

    @Override
    protected Integer scan(AnnotatedTypeMirror type, Void v) {
        return reduce(super.scan(type, null), generateHashcode(type));
    }

    @Override
    protected Integer reduce(Integer r1, Integer r2) {
        if (r1 == null) {
            return r2;
        }

        if (r2 == null) {
            return r1;
        }

        return r1 + r2;
    }

    private Integer generateHashcode(AnnotatedTypeMirror type) {
        if (type == null) {
            return null;
        }

        return type.getAnnotations().toString().hashCode() * 17
             + type.getUnderlyingType().toString().hashCode() * 13;
    }

}
