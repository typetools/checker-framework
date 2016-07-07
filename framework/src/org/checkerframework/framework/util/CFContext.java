package org.checkerframework.framework.util;

import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SourceVisitor;

/** A collection of references to various type checker components. */
public interface CFContext extends BaseContext {
    SourceChecker getChecker();

    SourceVisitor<?, ?> getVisitor();
}
