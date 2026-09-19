package org.checkerframework.framework.stub;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.framework.util.StaticJavaParserUtil;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link ToIndexFileConverter}. */
public class ToIndexFileConverterTest {

  /**
   * The {@code step} value of a {@link TypePathEntry} that stands for a type argument; that is,
   * {@code org.objectweb.asm.TypePath.TYPE_ARGUMENT}. It is written literally because the asm
   * library is not on this test's classpath.
   */
  private static final int TYPE_ARGUMENT = 3;

  /** The type path entry for the first type argument of a type. */
  private static final TypePathEntry firstTypeArgument = TypePathEntry.create(TYPE_ARGUMENT, 0);

  /** The type path of the type argument of a type: {@code List<HERE>}. */
  private static final List<TypePathEntry> outerTypeArgument = Arrays.asList(firstTypeArgument);

  /** The type path of the type argument of a type argument: {@code List<List<HERE>>}. */
  private static final List<TypePathEntry> innerTypeArgument =
      Arrays.asList(firstTypeArgument, firstTypeArgument);

  /**
   * Tests that an annotation on a nested type argument is recorded even when no annotation appears
   * on the type argument that encloses it.
   */
  @Test
  public void testNestedTypeArgumentWithoutEnclosingAnnotation() {
    AField field =
        fieldOfStub(
            "package p;",
            "import java.util.List;",
            "class C {",
            "  List<List<@Nullable String>> f;",
            "}");
    ATypeElement inner = field.type.innerTypes.get(innerTypeArgument);
    Assert.assertNotNull("no entry for the nested type argument", inner);
    Assert.assertNotNull(
        "@Nullable was not recorded on the nested type argument", inner.lookup("Nullable"));
  }

  /** Tests that annotations at several levels of nesting are each recorded at their own level. */
  @Test
  public void testAnnotationsAtEveryLevel() {
    AField field =
        fieldOfStub(
            "package p;",
            "import java.util.List;",
            "class C {",
            "  List<@A List<@B String>> f;",
            "}");
    ATypeElement outer = field.type.innerTypes.get(outerTypeArgument);
    Assert.assertNotNull("no entry for the outer type argument", outer);
    Assert.assertNotNull("@A was not recorded on the outer type argument", outer.lookup("A"));
    Assert.assertNull("@B was recorded on the outer type argument", outer.lookup("B"));
    ATypeElement inner = field.type.innerTypes.get(innerTypeArgument);
    Assert.assertNotNull("no entry for the nested type argument", inner);
    Assert.assertNotNull("@B was not recorded on the nested type argument", inner.lookup("B"));
    Assert.assertNull("@A was recorded on the nested type argument", inner.lookup("A"));
  }

  /**
   * Converts a stub file that declares a single class {@code p.C} with a single field {@code f},
   * and returns the field.
   *
   * @param lines the lines of the stub file
   * @return the scene element for field {@code f} of class {@code p.C}
   */
  private static AField fieldOfStub(String... lines) {
    String stubContents = String.join(System.lineSeparator(), lines);
    StubUnit stubUnit =
        StaticJavaParserUtil.parseStubUnit(
            new ByteArrayInputStream(stubContents.getBytes(StandardCharsets.UTF_8)));
    CompilationUnit cu = stubUnit.getCompilationUnits().get(0);
    AScene scene = new AScene();
    ToIndexFileConverter converter =
        new ToIndexFileConverter(cu.getPackageDeclaration().get(), cu.getImports(), scene);
    TypeDeclaration<?> typeDecl = cu.getType(0);
    AClass clazz = scene.classes.getVivify("p." + typeDecl.getNameAsString());
    typeDecl.accept(converter, clazz);
    AField field = clazz.fields.get("f");
    Assert.assertNotNull("no scene element for field f", field);
    return field;
  }
}
