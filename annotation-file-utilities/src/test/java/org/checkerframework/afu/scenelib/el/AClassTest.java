package org.checkerframework.afu.scenelib.el;

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link AClass}. */
public class AClassTest {

  /** Creates type elements. */
  private static final Elements elements;

  /** Reads the class files that {@link #elements} resolves names in. */
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
    elements = task.getElements();
  }

  /**
   * Closes the file manager. {@link #elements} resolves names lazily, so the file manager must stay
   * open until every test in this class has run.
   *
   * @throws IOException if the file manager cannot be closed
   */
  @AfterClass
  public static void closeFileManager() throws IOException {
    fileManager.close();
  }

  /** Creates a new AClassTest. */
  public AClassTest() {}

  /**
   * A copy of an AClass has the same information about the class declaration as the original: not
   * only annotations, but also which classes are enums, interfaces, annotations, or records; the
   * enum constants; and the type element. A client that writes out a copy of a scene (as the
   * Checker Framework's whole-program inference does) needs all of that information.
   */
  @Test
  public void cloneRetainsDeclarationInformation() {
    TypeElement dayOfWeek = elements.getTypeElement("java.time.DayOfWeek");
    List<VariableElement> enumConstants = enumConstants(dayOfWeek);

    AClass aClass = new AClass("java.time.DayOfWeek");
    aClass.setTypeElement(dayOfWeek);
    aClass.markAsEnum("DayOfWeek");
    aClass.setEnumConstants(enumConstants);
    aClass.markAsAnnotation("MyAnnotation");
    aClass.markAsInterface("MyInterface");
    aClass.markAsRecord("MyRecord");

    AClass copy = aClass.clone();

    Assert.assertEquals(dayOfWeek, copy.getTypeElement());
    Assert.assertTrue(copy.isEnum("DayOfWeek"));
    Assert.assertEquals(enumConstants, copy.getEnumConstants());
    Assert.assertTrue(copy.isAnnotation("MyAnnotation"));
    Assert.assertTrue(copy.isInterface("MyInterface"));
    Assert.assertTrue(copy.isRecord("MyRecord"));
  }

  /** A copy of an AClass about which nothing is known has no declaration information either. */
  @Test
  public void cloneOfUnknownClassRetainsNoDeclarationInformation() {
    AClass copy = new AClass("MyClass").clone();

    Assert.assertNull(copy.getTypeElement());
    Assert.assertFalse(copy.isEnum("MyClass"));
    Assert.assertNull(copy.getEnumConstants());
    Assert.assertFalse(copy.isAnnotation("MyClass"));
    Assert.assertFalse(copy.isInterface("MyClass"));
    Assert.assertFalse(copy.isRecord("MyClass"));
  }

  /**
   * Copying an AClass whose enum constants are unset leaves them unset in the copy, so that the
   * copy's enum constants can still be set. ({@link AClass#setEnumConstants} may be called at most
   * once.)
   */
  @Test
  public void cloneOfNonEnumCanBeMadeAnEnum() {
    TypeElement dayOfWeek = elements.getTypeElement("java.time.DayOfWeek");
    AClass copy = new AClass("java.time.DayOfWeek").clone();
    copy.setEnumConstants(enumConstants(dayOfWeek));
    Assert.assertEquals(enumConstants(dayOfWeek), copy.getEnumConstants());
  }

  /**
   * Returns the enum constants of the given enum class.
   *
   * @param enumClass an enum class
   * @return the enum constants of {@code enumClass}
   */
  private static List<VariableElement> enumConstants(TypeElement enumClass) {
    List<VariableElement> result = new ArrayList<>();
    for (Element member : enumClass.getEnclosedElements()) {
      if (member.getKind() == ElementKind.ENUM_CONSTANT) {
        result.add((VariableElement) member);
      }
    }
    return result;
  }
}
