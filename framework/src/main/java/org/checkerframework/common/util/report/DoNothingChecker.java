package org.checkerframework.common.util.report;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * This is an annotation processor that does nothing.
 *
 * <p>Use it when you are required to provide an annotation processor.
 */
public class DoNothingChecker extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return false;
  }
}
