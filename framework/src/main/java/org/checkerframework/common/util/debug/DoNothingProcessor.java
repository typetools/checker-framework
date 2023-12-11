package org.checkerframework.common.util.debug;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * This is an annotation processor that does nothing.
 *
 * <p>Use it when you are required to provide an annotation processor, or when you want to debug
 * compiler behavior with an annotation processor present.
 */
@SupportedAnnotationTypes("*")
public class DoNothingProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return false;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }
}
