package org.checkerframework.common.wholeprograminference;

import com.sun.source.util.JavacTask;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link AnnotationConverter}. */
public class AnnotationConverterTest {

  /**
   * The compilation unit that the tests analyze. {@code MyAnno} declares an element of every type
   * that an annotation element may have, and {@code Annotated} is annotated with every element
   * whose value {@link AnnotationConverter} can convert in both directions.
   */
  private static final String SOURCE =
      String.join(
          System.lineSeparator(),
          "package testpkg;",
          "@interface MyAnno {",
          "  boolean booleanElement();",
          // AnnotationConverter.addFieldToAnnotationBuilder cannot convert a byte value back into
          // an AnnotationMirror, so byteElement has a default and the annotation use below does
          // not write it; that keeps it out of the round-trip test.
          "  byte byteElement() default 1;",
          "  char charElement();",
          "  double doubleElement();",
          "  float floatElement();",
          "  int intElement();",
          "  long longElement();",
          "  short shortElement();",
          "  String stringElement();",
          "  Class<?> classElement();",
          "  MyEnum enumElement();",
          "  String[] stringArrayElement();",
          "  int[] intArrayElement();",
          "  MyEnum[] enumArrayElement();",
          "}",
          "enum MyEnum {",
          "  A, B;",
          "}",
          "@MyAnno(",
          "  booleanElement = true,",
          "  charElement = 'c',",
          "  doubleElement = 2.0,",
          "  floatElement = 3.0f,",
          "  intElement = 4,",
          "  longElement = 5L,",
          "  shortElement = 6,",
          "  stringElement = \"seven\",",
          "  classElement = String.class,",
          "  enumElement = MyEnum.A,",
          "  stringArrayElement = {\"eight\", \"nine\"},",
          "  intArrayElement = {10, 11},",
          "  enumArrayElement = {MyEnum.A, MyEnum.B}",
          ")",
          "public class Annotated {",
          // The annotation on this method is the one that sourceIsComputedOnlyOnDemand uses.
          "  @Deprecated void aMethod() {}",
          "}");

  /** Tests {@link AnnotationConverter#getAnnotationFieldType} for every kind of element. */
  @Test
  public void getAnnotationFieldType() {
    withProcessingEnvironment(
        env -> {
          Map<String, ExecutableElement> elements = annotationElements(env, "testpkg.MyAnno");
          assertFieldType("boolean", elements, "booleanElement");
          assertFieldType("byte", elements, "byteElement");
          assertFieldType("char", elements, "charElement");
          assertFieldType("double", elements, "doubleElement");
          assertFieldType("float", elements, "floatElement");
          assertFieldType("int", elements, "intElement");
          assertFieldType("long", elements, "longElement");
          assertFieldType("short", elements, "shortElement");
          assertFieldType("String", elements, "stringElement");
          assertFieldType("Class", elements, "classElement");
          assertFieldType("enum testpkg.MyEnum", elements, "enumElement");
          assertFieldType("String[]", elements, "stringArrayElement");
          assertFieldType("int[]", elements, "intArrayElement");
          assertFieldType("enum testpkg.MyEnum[]", elements, "enumArrayElement");
        });
  }

  /**
   * Tests that {@link AnnotationConverter#typeMirrorToAnnotationFieldType} throws an exception,
   * rather than silently returning a wrong result, for a type that no annotation element can have.
   */
  @Test
  public void typeMirrorToAnnotationFieldTypeOfUnexpectedType() {
    withProcessingEnvironment(
        env -> {
          TypeMirror voidType = env.getTypeUtils().getNoType(TypeKind.VOID);
          try {
            AnnotationConverter.typeMirrorToAnnotationFieldType(voidType);
            Assert.fail("No exception for " + voidType);
          } catch (BugInCF e) {
            Assert.assertTrue(
                e.getMessage(), e.getMessage().contains("typeMirrorToAnnotationFieldType"));
          }
        });
  }

  /**
   * Tests that converting an annotation to an {@link Annotation} and back yields the original
   * annotation. This exercises every branch of {@link
   * AnnotationConverter#addFieldToAnnotationBuilder} that a value of a legal element type can
   * reach.
   */
  @Test
  public void annotationRoundTrip() {
    withProcessingEnvironment(
        env -> {
          TypeElement annotated = env.getElementUtils().getTypeElement("testpkg.Annotated");
          Assert.assertNotNull("no element for testpkg.Annotated", annotated);
          Assert.assertEquals(1, annotated.getAnnotationMirrors().size());
          AnnotationMirror am = annotated.getAnnotationMirrors().get(0);
          Annotation anno = AnnotationConverter.annotationMirrorToAnnotation(am);
          AnnotationMirror roundTripped =
              AnnotationConverter.annotationToAnnotationMirror(anno, env);
          Assert.assertTrue(
              "expected " + am + " but got " + roundTripped,
              AnnotationUtils.areSame(am, roundTripped));
        });
  }

  /**
   * {@link AnnotationConverter#annotationMirrorToAnnotation} is called once per annotation per
   * storage write, so it must not construct the {@code AnnotationDef}'s source string, which is
   * used only for diagnostics.
   */
  @Test
  public void sourceIsComputedOnlyOnDemand() {
    withProcessingEnvironment(
        env -> {
          CountingAnnotationMirror am = new CountingAnnotationMirror(theAnnotationMirror(env));
          Annotation converted = AnnotationConverter.annotationMirrorToAnnotation(am);
          Assert.assertEquals(
              "annotationMirrorToAnnotation stringified its argument", 0, am.toStringCount);
          String source = converted.def().getSource();
          String secondSource = converted.def().getSource();
          Assert.assertTrue(source, source.startsWith("annotationMirrorToAnnotation "));
          Assert.assertTrue(source, source.contains("java.lang.Deprecated"));
          Assert.assertEquals(source, secondSource);
          Assert.assertEquals(
              "getSource() stringified its argument more than once", 1, am.toStringCount);
        });
  }

  /**
   * Asserts that the type of the given element of an annotation is {@code expected}.
   *
   * @param expected the string representation of the expected annotation field type
   * @param elements the elements of an annotation, indexed by name
   * @param elementName the name of one of the elements
   */
  private static void assertFieldType(
      String expected, Map<String, ExecutableElement> elements, String elementName) {
    ExecutableElement element = elements.get(elementName);
    Assert.assertNotNull("no element named " + elementName, element);
    Assert.assertEquals(
        "type of " + elementName,
        expected,
        AnnotationConverter.getAnnotationFieldType(element).toString());
  }

  /**
   * Returns the elements of the given annotation type, indexed by name.
   *
   * @param env the processing environment
   * @param annotationName the fully-qualified name of an annotation type declared in {@link
   *     #SOURCE}
   * @return the elements of the annotation type, indexed by name
   */
  private static Map<String, ExecutableElement> annotationElements(
      ProcessingEnvironment env, String annotationName) {
    TypeElement annotationType = env.getElementUtils().getTypeElement(annotationName);
    Assert.assertNotNull("no element for " + annotationName, annotationType);
    Map<String, ExecutableElement> result = new LinkedHashMap<>();
    for (ExecutableElement element :
        ElementFilter.methodsIn(annotationType.getEnclosedElements())) {
      result.put(element.getSimpleName().toString(), element);
    }
    return result;
  }

  /**
   * Returns the annotation on the method declared in {@link #SOURCE}.
   *
   * @param env the processing environment
   * @return the annotation on the method declared in {@link #SOURCE}
   */
  private static AnnotationMirror theAnnotationMirror(ProcessingEnvironment env) {
    TypeElement annotated = env.getElementUtils().getTypeElement("testpkg.Annotated");
    Assert.assertNotNull("no element for testpkg.Annotated", annotated);
    List<ExecutableElement> methods = ElementFilter.methodsIn(annotated.getEnclosedElements());
    Assert.assertEquals("methods of " + annotated, 1, methods.size());
    ExecutableElement method = methods.get(0);
    List<? extends AnnotationMirror> annotations = method.getAnnotationMirrors();
    Assert.assertEquals("annotations on " + method, 1, annotations.size());
    return annotations.get(0);
  }

  /**
   * Runs {@code body} while javac is processing {@link #SOURCE}, so that {@code body} has access to
   * a processing environment, and rethrows anything that {@code body} throws.
   *
   * @param body the code to run
   */
  private static void withProcessingEnvironment(Consumer<ProcessingEnvironment> body) {
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
                // "-proc:only" runs the processor below without generating class files.
                Collections.singletonList("-proc:only"),
                null,
                Collections.singletonList(fileObject));
    AtomicReference<Throwable> thrown = new AtomicReference<>();
    task.setProcessors(
        Collections.singletonList(
            new AbstractProcessor() {
              @Override
              public Set<String> getSupportedAnnotationTypes() {
                return Collections.singleton("*");
              }

              @Override
              public SourceVersion getSupportedSourceVersion() {
                return SourceVersion.latest();
              }

              @Override
              public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment re) {
                // Throwing from a processor would make javac report an error that hides the
                // reason for the failure, so record what was thrown and rethrow it later.
                if (!re.processingOver() && thrown.get() == null) {
                  try {
                    body.accept(processingEnv);
                  } catch (Throwable t) {
                    thrown.set(t);
                  }
                }
                return false;
              }
            }));
    boolean success = task.call();
    Throwable t = thrown.get();
    if (t instanceof Error) {
      throw (Error) t;
    } else if (t instanceof RuntimeException) {
      throw (RuntimeException) t;
    } else if (t != null) {
      throw new Error(t);
    }
    Assert.assertTrue("Cannot compile " + SOURCE, success);
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
