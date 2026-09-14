package org.checkerframework.common.wholeprograminference;

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.afu.scenelib.field.ArrayAFT;
import org.checkerframework.afu.scenelib.field.BasicAFT;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link SceneToStubWriter}. */
public class SceneToStubWriterTest {

  /** Creates type mirrors. */
  private static final Types types;

  /** Creates type elements. */
  private static final Elements elements;

  /** Reads the class files that {@link #elements} and {@link #types} resolve names in. */
  private static final StandardJavaFileManager fileManager;

  static {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new AssertionError("No Java compiler is available; run these tests on a JDK.");
    }
    fileManager = compiler.getStandardFileManager(null, null, null);
    JavacTask task =
        (JavacTask)
            compiler.getTask(
                null, fileManager, null, null, null, Collections.<JavaFileObject>emptyList());
    types = task.getTypes();
    elements = task.getElements();
  }

  /**
   * Closes the file manager. {@link #elements} and {@link #types} resolve names lazily, so the file
   * manager must stay open until every test in this class has run.
   *
   * @throws IOException if the file manager cannot be closed
   */
  @AfterClass
  public static void closeFileManager() throws IOException {
    fileManager.close();
  }

  /** The binary name of a declaration annotation that the tests place on formal parameters. */
  private static final String OWNING = "org.checkerframework.checker.mustcall.qual.Owning";

  /** The binary name of a type annotation that the tests place on types. */
  private static final String INTERNED = "org.checkerframework.checker.interning.qual.Interned";

  /** Creates a new SceneToStubWriterTest. */
  public SceneToStubWriterTest() {}

  /**
   * Returns an annotation with the given binary name and no elements (fields).
   *
   * @param binaryName the binary name of the annotation type
   * @return a scene-lib annotation with the given name
   */
  private static Annotation markerAnnotation(String binaryName) {
    return annotation(binaryName, Collections.emptyMap(), Collections.emptyMap());
  }

  /**
   * Returns an annotation with the given binary name, element types, and element values.
   *
   * @param binaryName the binary name of the annotation type
   * @param fieldTypes the types of the annotation's elements (fields)
   * @param fieldValues the values of the annotation's elements (fields)
   * @return a scene-lib annotation with the given name, element types, and element values
   */
  private static Annotation annotation(
      String binaryName,
      Map<String, AnnotationFieldType> fieldTypes,
      Map<String, Object> fieldValues) {
    AnnotationDef def = new AnnotationDef(binaryName, fieldTypes, "SceneToStubWriterTest");
    return new Annotation(def, fieldValues);
  }

  /**
   * Returns an AField named "x" with the given type and declaration annotations.
   *
   * @param type javac's representation of the type of the formal parameter
   * @param declAnnoNames the binary names of the declaration annotations on the formal parameter
   * @return the formal parameter
   */
  private static AField parameter(TypeMirror type, String... declAnnoNames) {
    AField param = new AField("x", type);
    for (String declAnnoName : declAnnoNames) {
      param.tlAnnotationsHere.add(markerAnnotation(declAnnoName));
    }
    return param;
  }

  /**
   * Returns an AField named "x" whose type is {@code int} and that has the given declaration
   * annotations.
   *
   * @param declAnnoNames the binary names of the declaration annotations on the formal parameter
   * @return the formal parameter
   */
  private static AField intParameter(String... declAnnoNames) {
    return parameter(types.getPrimitiveType(TypeKind.INT), declAnnoNames);
  }

  /**
   * Returns the scene-lib representation of the component type of the array type that {@code
   * arrayType} represents, creating it if it does not yet exist.
   *
   * @param arrayType the scene-lib representation of an array type
   * @return the scene-lib representation of the array's component type
   */
  private static ATypeElement componentTypeOf(ATypeElement arrayType) {
    // This is the same key that WholeProgramInferenceScenesStorage uses for a component type.
    List<TypePathEntry> location =
        TypePathEntry.getTypePathEntryListFromBinary(Collections.nCopies(2, 0));
    return arrayType.innerTypes.getVivify(location);
  }

  @Test
  public void formatParameterWithoutDeclarationAnnotation() {
    AField param = intParameter();
    Assert.assertEquals("int x", SceneToStubWriter.formatParameter(param, "x", "MyClass"));
  }

  /**
   * A declaration annotation on a formal parameter must be separated from the parameter's type by a
   * space; otherwise the stub file cannot be parsed.
   */
  @Test
  public void formatParameterWithDeclarationAnnotation() {
    AField param = intParameter(OWNING);
    Assert.assertEquals("@Owning int x", SceneToStubWriter.formatParameter(param, "x", "MyClass"));
  }

  @Test
  public void formatParameterWithTwoDeclarationAnnotations() {
    AField param = intParameter(OWNING, "java.lang.Deprecated");
    Assert.assertEquals(
        "@Owning @Deprecated int x", SceneToStubWriter.formatParameter(param, "x", "MyClass"));
  }

  /**
   * The type of a receiver parameter is the name of the enclosing class, which is passed as the
   * last argument to {@code formatParameter}; the receiver parameter's own type is not used.
   */
  @Test
  public void formatReceiverParameter() {
    AField receiver = new AField("this", types.getPrimitiveType(TypeKind.INT));
    Assert.assertEquals(
        "MyClass this", SceneToStubWriter.formatParameter(receiver, "this", "MyClass"));
    receiver.type.tlAnnotationsHere.add(markerAnnotation(INTERNED));
    Assert.assertEquals(
        "@Interned MyClass this", SceneToStubWriter.formatParameter(receiver, "this", "MyClass"));
  }

  /** An annotation on an array type is printed between the component type and the brackets. */
  @Test
  public void formatArrayParameter() {
    TypeMirror intArray = types.getArrayType(types.getPrimitiveType(TypeKind.INT));
    AField param = parameter(intArray);
    Assert.assertEquals("int [] x", SceneToStubWriter.formatParameter(param, "x", "MyClass"));
    param.type.tlAnnotationsHere.add(markerAnnotation(INTERNED));
    Assert.assertEquals(
        "int @Interned [] x", SceneToStubWriter.formatParameter(param, "x", "MyClass"));
  }

  /**
   * In a multidimensional array, each dimension is annotated separately, and the annotation on the
   * ultimate component type precedes the component type.
   */
  @Test
  public void formatMultidimensionalArrayParameter() {
    TypeMirror intArrayArray =
        types.getArrayType(types.getArrayType(types.getPrimitiveType(TypeKind.INT)));
    AField param = parameter(intArrayArray);
    Assert.assertEquals("int [] [] x", SceneToStubWriter.formatParameter(param, "x", "MyClass"));

    // The type is int[][]; annotate the int[][], the int[], and the int.
    ATypeElement innerDimension = componentTypeOf(param.type);
    ATypeElement componentType = componentTypeOf(innerDimension);
    param.type.tlAnnotationsHere.add(markerAnnotation("test.OuterAnno"));
    innerDimension.tlAnnotationsHere.add(markerAnnotation("test.InnerAnno"));
    componentType.tlAnnotationsHere.add(markerAnnotation("test.ComponentAnno"));
    Assert.assertEquals(
        "@ComponentAnno int @OuterAnno [] @InnerAnno [] x",
        SceneToStubWriter.formatParameter(param, "x", "MyClass"));
  }

  /**
   * Type arguments are not printed, because a stub file does not need them and the inferred ones
   * are sometimes wrong.
   */
  @Test
  public void formatParameterOfGenericType() {
    TypeMirror string = elements.getTypeElement("java.lang.String").asType();
    TypeElement listElement = elements.getTypeElement("java.util.List");
    TypeMirror listOfString = types.getDeclaredType(listElement, string);
    Assert.assertEquals(
        "java.util.List x",
        SceneToStubWriter.formatParameter(parameter(listOfString), "x", "MyClass"));

    TypeElement mapElement = elements.getTypeElement("java.util.Map");
    TypeMirror mapOfStringToListOfString = types.getDeclaredType(mapElement, string, listOfString);
    Assert.assertEquals(
        "java.util.Map x",
        SceneToStubWriter.formatParameter(parameter(mapOfStringToListOfString), "x", "MyClass"));
  }

  /** An array whose component type is generic is printed without the type arguments. */
  @Test
  public void formatParameterOfGenericArrayType() {
    TypeMirror string = elements.getTypeElement("java.lang.String").asType();
    TypeElement listElement = elements.getTypeElement("java.util.List");
    TypeMirror listOfString = types.getDeclaredType(listElement, string);
    Assert.assertEquals(
        "java.util.List [] x",
        SceneToStubWriter.formatParameter(
            parameter(types.getArrayType(listOfString)), "x", "MyClass"));
  }

  /** Tests formatting an annotation with no elements. */
  @Test
  public void formatAnnotationWithoutElements() {
    Assert.assertEquals("@Owning", SceneToStubWriter.formatAnnotation(markerAnnotation(OWNING)));
  }

  /** An annotation with a single element named "value" is printed without the element's name. */
  @Test
  public void formatAnnotationWithValueElement() {
    Annotation anno =
        annotation(
            "org.checkerframework.common.value.qual.MinLen",
            Collections.singletonMap("value", BasicAFT.forType(int.class)),
            Collections.singletonMap("value", 3));
    Assert.assertEquals("@MinLen(3)", SceneToStubWriter.formatAnnotation(anno));
  }

  /**
   * An annotation with a single element not named "value" is printed with the element's name. No
   * annotation in the Checker Framework has exactly one element that is not named "value", so this
   * test uses a hypothetical annotation.
   */
  @Test
  public void formatAnnotationWithNamedElement() {
    Annotation anno =
        annotation(
            "test.AnnoWithNamedElement",
            Collections.singletonMap("expression", new ArrayAFT(BasicAFT.forType(String.class))),
            Collections.singletonMap("expression", Collections.singletonList("this.f")));
    Assert.assertEquals(
        "@AnnoWithNamedElement(expression=\"this.f\")", SceneToStubWriter.formatAnnotation(anno));
  }

  /**
   * An annotation with more than one element is printed as a comma-separated list of {@code
   * name=value} pairs, in the order of the annotation's element values.
   */
  @Test
  public void formatAnnotationWithTwoElements() {
    Map<String, AnnotationFieldType> fieldTypes = new LinkedHashMap<>(2);
    fieldTypes.put("expression", new ArrayAFT(BasicAFT.forType(String.class)));
    fieldTypes.put("result", BasicAFT.forType(boolean.class));
    Map<String, Object> fieldValues = new LinkedHashMap<>(2);
    fieldValues.put("expression", Arrays.asList("this.f", "this.g"));
    fieldValues.put("result", true);
    Annotation anno =
        annotation(
            "org.checkerframework.checker.nullness.qual.EnsuresNonNullIf", fieldTypes, fieldValues);
    Assert.assertEquals(
        "@EnsuresNonNullIf(expression={\"this.f\", \"this.g\"}, result=true)",
        SceneToStubWriter.formatAnnotation(anno));
  }

  /** Tests extracting a package name from a binary name. */
  @Test
  public void packagePart() {
    Assert.assertEquals("java.util", SceneToStubWriter.packagePart("java.util.Map$Entry"));
    Assert.assertNull(SceneToStubWriter.packagePart("Outer$Inner"));
  }

  /** Tests extracting a basename from a binary name. */
  @Test
  public void basenamePart() {
    Assert.assertEquals("Map$Entry", SceneToStubWriter.basenamePart("java.util.Map$Entry"));
    Assert.assertEquals("Outer$Inner", SceneToStubWriter.basenamePart("Outer$Inner"));
  }
}
