package org.checkerframework.common.basetype;

import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.CFContext;

/** An extension of {@link CFContext} that includes {@link BaseTypeChecker}-specific components. */
public interface BaseTypeContext extends CFContext {
    @Override
    BaseTypeChecker getChecker();

    @Override
    BaseTypeVisitor<?> getVisitor();

    GenericAnnotatedTypeFactory<?, ?, ?, ?> getTypeFactory();
}
