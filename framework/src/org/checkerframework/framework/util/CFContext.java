package org.checkerframework.framework.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SourceVisitor;

/** A collection of references to various type checker components. */
public interface CFContext extends BaseContext {
    SourceChecker getChecker();
    SourceVisitor<?, ?> getVisitor();
}
