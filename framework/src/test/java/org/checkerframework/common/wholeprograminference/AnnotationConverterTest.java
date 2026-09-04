package org.checkerframework.common.wholeprograminference;

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.afu.scenelib.Annotation;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link AnnotationConverter}. */
public class AnnotationConverterTest {

  /** The compilation unit whose annotation the tests use. */
  private static final String SOURCE =
      String.join(
          System.lineSeparator(),
          "package testpkg;",
          "public class Annotated {",
          "  @Deprecated void aMethod() {}",
          "}");

  /**
   * {@link AnnotationConverter#annotationMirrorToAnnotation} is called once per annotation per
   * storage write, so it must not construct the {@code AnnotationDef}'s source string, which is
   * used only for diagnostics.
   */
  @Test
  public void sourceIsComputedOnlyOnDemand() {
    CountingAnnotationMirror am = new CountingAnnotationMirror(theAnnotationMirror());
    Annotation converted = AnnotationConverter.annotationMirrorToAnnotation(am);
    Assert.assertEquals(
        "annotationMirrorToAnnotation stringified its argument", 0, am.toStringCount);
    String source = converted.def().getSource();
    Assert.assertTrue(source, source.startsWith("annotationMirrorToAnnotation "));
    Assert.assertTrue(source, source.contains("java.lang.Deprecated"));
    Assert.assertEquals("getSource() stringified its argument more than once", 1, am.toStringCount);
  }

  /**
   * Compiles {@link #SOURCE} and returns the annotation on its method.
   *
   * @return the annotation on the method declared in {@link #SOURCE}
   */
  private static AnnotationMirror theAnnotationMirror() {
    JavaFileObject fileObject =
        new SimpleJavaFileObject(
            URI.create("string:///testpkg/Annotated.java"), JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return SOURCE;
          }
        };
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    JavacTask task =
        (JavacTask)
            compiler.getTask(
                null,
                null,
                null,
                Collections.singletonList("-proc:none"),
                null,
                Collections.singletonList(fileObject));
    Iterable<? extends Element> topLevelElements;
    try {
      task.parse();
      topLevelElements = task.analyze();
    } catch (IOException e) {
      throw new Error("Cannot compile " + SOURCE, e);
    }
    TypeElement classElement = (TypeElement) topLevelElements.iterator().next();
    ExecutableElement method = ElementFilter.methodsIn(classElement.getEnclosedElements()).get(0);
    List<? extends AnnotationMirror> annotations = method.getAnnotationMirrors();
    Assert.assertEquals("annotations on " + method, 1, annotations.size());
    return annotations.get(0);
  }

  /** An {@code AnnotationMirror} that counts how often it is converted to a string. */
  private static class CountingAnnotationMirror implements AnnotationMirror {

    /** The annotation mirror that this delegates to. */
    private final AnnotationMirror delegate;

    /** The number of times that {@link #toString} has been called. */
    private int toStringCount = 0;

    /**
     * Creates a {@code CountingAnnotationMirror} that delegates to {@code delegate}.
     *
     * @param delegate the annotation mirror to delegate to
     */
    CountingAnnotationMirror(AnnotationMirror delegate) {
      this.delegate = delegate;
    }

    @Override
    public DeclaredType getAnnotationType() {
      return delegate.getAnnotationType();
    }

    @Override
    public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
      return delegate.getElementValues();
    }

    @Override
    public String toString() {
      toStringCount++;
      return delegate.toString();
    }
  }
}
