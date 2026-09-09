package org.checkerframework.common.wholeprograminference;

import com.sun.source.util.JavacTask;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationBuilder;
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
          "import java.lang.annotation.RetentionPolicy;",
          "@interface MyAnno {",
          "  boolean booleanElement();",
          "  byte byteElement();",
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
          // Both legal forms of an annotation-valued element.
          "  MyNested nestedElement();",
          "  MyNested[] nestedArrayElement();",
          // The following two elements exist only for
          // addFieldToAnnotationBuilderForEveryType, which needs elements whose type is an enum
          // (or an array thereof) that is loadable at run time, unlike MyEnum.  They have
          // defaults so that the annotation use below, and therefore the round-trip test, need
          // not mention them.
          "  RetentionPolicy policyElement() default RetentionPolicy.CLASS;",
          "  RetentionPolicy[] policyArrayElement() default {};",
          "}",
          "@interface MyNested {",
          "  int value();",
          "}",
          "enum MyEnum {",
          "  A, B;",
          "}",
          "@MyAnno(",
          "  booleanElement = true,",
          "  byteElement = 1,",
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
          "  enumArrayElement = {MyEnum.A, MyEnum.B},",
          "  nestedElement = @MyNested(12),",
          "  nestedArrayElement = {@MyNested(13), @MyNested(14)}",
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
          assertFieldType("enum java.lang.annotation.RetentionPolicy", elements, "policyElement");
          assertFieldType("String[]", elements, "stringArrayElement");
          assertFieldType("int[]", elements, "intArrayElement");
          assertFieldType("enum testpkg.MyEnum[]", elements, "enumArrayElement");
          assertFieldType(
              "enum java.lang.annotation.RetentionPolicy[]", elements, "policyArrayElement");
          assertFieldType("annotation-field testpkg.MyNested", elements, "nestedElement");
          assertFieldType("annotation-field testpkg.MyNested[]", elements, "nestedArrayElement");
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
   * annotation. {@link #addFieldToAnnotationBuilderForEveryType} tests the second half of that
   * conversion in more detail.
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
   * Tests one value for every branch of {@link AnnotationConverter#addFieldToAnnotationBuilder}.
   */
  @Test
  public void addFieldToAnnotationBuilderForEveryType() {
    withProcessingEnvironment(
        env -> {
          VariableElement enumA = enumConstant(env, "testpkg.MyEnum", "A");
          VariableElement enumB = enumConstant(env, "testpkg.MyEnum", "B");
          TypeMirror stringType = typeElement(env, "java.lang.String").asType();
          AnnotationMirror nested =
              new AnnotationBuilder(env, "testpkg.MyNested").setValue("value", 12).build();
          Annotation sceneNested = AnnotationConverter.annotationMirrorToAnnotation(nested);

          // One value per branch, in the order in which addFieldToAnnotationBuilder tests them.
          // List
          assertFieldValue(env, "{\"a\", \"b\"}", "stringArrayElement", Arrays.asList("a", "b"));
          // String
          assertFieldValue(env, "\"seven\"", "stringElement", "seven");
          // Integer
          assertFieldValue(env, "4", "intElement", 4);
          // Float
          assertFieldValue(env, "3.0", "floatElement", 3.0f);
          // Long
          assertFieldValue(env, "5", "longElement", 5L);
          // Boolean
          assertFieldValue(env, "true", "booleanElement", true);
          // Character
          assertFieldValue(env, "'c'", "charElement", 'c');
          // Class
          assertFieldValue(env, "java.lang.String.class", "classElement", String.class);
          // Double
          assertFieldValue(env, "2.0", "doubleElement", 2.0);
          // Enum
          assertFieldValue(
              env,
              "java.lang.annotation.RetentionPolicy.RUNTIME",
              "policyElement",
              RetentionPolicy.RUNTIME);
          // Enum[]
          assertFieldValue(
              env,
              "{java.lang.annotation.RetentionPolicy.RUNTIME,"
                  + " java.lang.annotation.RetentionPolicy.SOURCE}",
              "policyArrayElement",
              new RetentionPolicy[] {RetentionPolicy.RUNTIME, RetentionPolicy.SOURCE});
          // AnnotationMirror
          assertFieldValue(env, "@testpkg.MyNested(12)", "nestedElement", nested);
          // Object[]
          assertFieldValue(env, "{10, 11}", "intArrayElement", new Integer[] {10, 11});
          // TypeMirror
          assertFieldValue(env, "java.lang.String.class", "classElement", stringType);
          // Short
          assertFieldValue(env, "6", "shortElement", (short) 6);
          // Byte
          assertFieldValue(env, "1", "byteElement", (byte) 1);
          // Annotation
          assertFieldValue(env, "@testpkg.MyNested(12)", "nestedElement", sceneNested);
          // A List of Annotation, which the List branch handles.
          assertFieldValue(
              env,
              "{@testpkg.MyNested(12), @testpkg.MyNested(12)}",
              "nestedArrayElement",
              Arrays.asList(sceneNested, sceneNested));
          // VariableElement
          assertFieldValue(env, "testpkg.MyEnum.A", "enumElement", enumA);
          // The VariableElement[] branch is unreachable, because a VariableElement[] is an
          // Object[] and the Object[] branch precedes it.  This assertion shows that the Object[]
          // branch handles a VariableElement[] correctly anyway.
          assertFieldValue(
              env,
              "{testpkg.MyEnum.A, testpkg.MyEnum.B}",
              "enumArrayElement",
              new VariableElement[] {enumA, enumB});
        });
  }

  /**
   * Tests that {@link AnnotationConverter#addFieldToAnnotationBuilder} throws an exception, rather
   * than silently dropping the field, for a value whose type it does not handle. No value of a
   * legal annotation element type reaches that branch.
   */
  @Test
  public void addFieldToAnnotationBuilderForUnhandledType() {
    withProcessingEnvironment(
        env -> {
          AnnotationBuilder builder = new AnnotationBuilder(env, "testpkg.MyAnno");
          try {
            AnnotationConverter.addFieldToAnnotationBuilder(
                "byteElement", new StringBuilder("1"), builder, env);
            Assert.fail("No exception for a StringBuilder value");
          } catch (BugInCF e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Unrecognized type"));
          }
        });
  }

  /**
   * {@link AnnotationConverter#annotationMirrorToAnnotation} is called once per annotation per
   * storage write, so it must not construct the {@code AnnotationDef}'s source string, which is
   * used only for diagnostics. Furthermore, the {@code AnnotationDef} outlives the compilation of
   * the annotation, so it must not retain the {@code AnnotationMirror}; the source string is
   * therefore computed from strings rather than from the {@code AnnotationMirror} itself.
   */
  @Test
  public void sourceIsComputedOnlyOnDemand() {
    withProcessingEnvironment(
        env -> {
          CountingAnnotationMirror am = new CountingAnnotationMirror(theAnnotationMirror(env));
          Annotation converted = AnnotationConverter.annotationMirrorToAnnotation(am);
          AnnotationDef def = converted.def();
          Assert.assertNull(
              "annotationMirrorToAnnotation computed the source eagerly", computedSource(def));
          Assert.assertEquals(
              "annotationMirrorToAnnotation stringified its argument", 0, am.toStringCount);
          String source = def.getSource();
          String secondSource = def.getSource();
          Assert.assertTrue(source, source.startsWith("annotationMirrorToAnnotation "));
          Assert.assertTrue(source, source.contains("java.lang.Deprecated"));
          // Reference equality, because getSource() caches its result rather than recomputing it.
          Assert.assertSame(source, secondSource);
          Assert.assertSame("getSource() did not cache its result", source, computedSource(def));
          Assert.assertEquals("getSource() stringified the AnnotationMirror", 0, am.toStringCount);
        });
  }

  /**
   * Returns the source string that {@code def} has already computed, without computing it. Reads
   * the field directly, because {@link AnnotationDef#getSource()} computes the string if it has not
   * been computed yet.
   *
   * @param def an annotation definition
   * @return the source string that {@code def} has computed, or null if it has computed none
   */
  private static @Nullable String computedSource(AnnotationDef def) {
    try {
      Field sourceField = AnnotationDef.class.getDeclaredField("source");
      sourceField.setAccessible(true);
      return (String) sourceField.get(def);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new Error("Cannot read AnnotationDef.source", e);
    }
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
   * Asserts that passing {@code elementName} and {@code value} to {@link
   * AnnotationConverter#addFieldToAnnotationBuilder} sets exactly the element named {@code
   * elementName} of {@code testpkg.MyAnno}, to a value whose string representation is {@code
   * expected}.
   *
   * @param env the processing environment
   * @param expected the string representation of the expected value
   * @param elementName the name of an element of {@code testpkg.MyAnno}
   * @param value the value to set the element to
   */
  private static void assertFieldValue(
      ProcessingEnvironment env, String expected, String elementName, Object value) {
    AnnotationBuilder builder = new AnnotationBuilder(env, "testpkg.MyAnno");
    AnnotationConverter.addFieldToAnnotationBuilder(elementName, value, builder, env);
    Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
        builder.build().getElementValues();
    Assert.assertEquals("elements set while setting " + elementName, 1, elementValues.size());
    Assert.assertEquals(
        "value of " + elementName, expected, elementValues.values().iterator().next().toString());
  }

  /**
   * Returns the named constant of the named enum type.
   *
   * @param env the processing environment
   * @param enumName the fully-qualified name of an enum type
   * @param constantName the name of one of the constants of the enum type
   * @return the element for the constant
   */
  private static VariableElement enumConstant(
      ProcessingEnvironment env, String enumName, String constantName) {
    for (VariableElement constant :
        ElementFilter.fieldsIn(typeElement(env, enumName).getEnclosedElements())) {
      if (constant.getSimpleName().contentEquals(constantName)) {
        return constant;
      }
    }
    throw new AssertionError("no constant " + constantName + " in " + enumName);
  }

  /**
   * Returns the element for the named type declaration.
   *
   * @param env the processing environment
   * @param name the fully-qualified name of a type
   * @return the element for the type
   */
  private static TypeElement typeElement(ProcessingEnvironment env, String name) {
    TypeElement result = env.getElementUtils().getTypeElement(name);
    if (result == null) {
      throw new AssertionError("no element for " + name);
    }
    return result;
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
    // Records that body ran, so that a change in javac's behavior cannot turn every test in this
    // class into a silent no-op.
    AtomicBoolean ran = new AtomicBoolean(false);
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
                    ran.set(true);
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
    Assert.assertTrue("The processor never ran the test body", ran.get());
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
