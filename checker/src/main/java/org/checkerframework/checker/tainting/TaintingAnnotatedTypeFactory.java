package org.checkerframework.checker.tainting;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Only overridden so that the supported qualifiers can be explicitly specified, because this
 * checker is used during the build process.
 */
public class TaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** Default constructor. */
    public TaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(Untainted.class, Tainted.class, PolyTainted.class));
    }
}
