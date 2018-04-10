package org.checkerframework.framework.util;

import com.sun.source.util.Trees;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.javacutil.AnnotationProvider;

/** A collection of references to javac components. */
public interface BaseContext {
    ProcessingEnvironment getProcessingEnvironment();

    Elements getElementUtils();

    Types getTypeUtils();

    Trees getTreeUtils();

    AnnotationProvider getAnnotationProvider();

    OptionConfiguration getOptionConfiguration();
}
