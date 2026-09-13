package org.checkerframework.framework.test.junit;

import com.github.javaparser.ParseProblemException;
import com.sun.source.util.JavacTask;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.SceneToStubWriter;
import org.checkerframework.common.wholeprograminference.scenelib.ASceneWrapper;
import org.checkerframework.framework.util.JavaParserUtil;
import org.checkerframework.javacutil.BugInCF;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/** Tests for {@link SceneToStubWriter}. */
public class SceneToStubWriterTest {

  /** A concrete checker; {@link SceneToStubWriter} uses only its name. */
  private static class TestChecker extends BaseTypeChecker {
    /** Creates a TestChecker. */
    TestChecker() {}
  }

  /**
   * Returns a scene containing a single printable class named "Foo", whose body is empty.
   *
   * @return a scene containing a single printable class
   */
  private ASceneWrapper sceneWithOnePrintableClass() {
    AScene scene = new AScene();
    AClass aClass = scene.classes.getVivify("Foo");
    aClass.setTypeElement(typeElementFor("Foo", "class Foo {}"));
    return new ASceneWrapper(scene);
  }

  /**
   * Compiles the given source code and returns the TypeElement for one of the classes that it
   * declares.
   *
   * @param canonicalName the canonical name of a class declared in {@code source}
   * @param source the contents of a compilation unit
   * @return the TypeElement for {@code canonicalName}
   */
  private static TypeElement typeElementFor(String canonicalName, String source) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    JavaFileObject fileObject =
        new SimpleJavaFileObject(
            URI.create("string:///SceneToStubWriterTestInput.java"), JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
          }
        };
    JavacTask task =
        (JavacTask)
            compiler.getTask(null, null, null, null, null, Collections.singletonList(fileObject));
    try {
      task.analyze();
    } catch (IOException e) {
      throw new AssertionError("Could not compile:\n" + source, e);
    }
    TypeElement result = task.getElements().getTypeElement(canonicalName);
    if (result == null) {
      throw new AssertionError("Did not find " + canonicalName + " in:\n" + source);
    }
    return result;
  }

  /**
   * Writes the given scene to a temporary stub file and returns the file's contents.
   *
   * @param scene the scene to write
   * @return the contents of the stub file
   * @throws IOException if the temporary file cannot be created or read
   */
  private static String writeToString(AScene scene) throws IOException {
    File astub = File.createTempFile("SceneToStubWriterTest", ".astub");
    astub.deleteOnExit();
    SceneToStubWriter.write(new ASceneWrapper(scene), astub.getPath(), new TestChecker());
    return new String(Files.readAllBytes(astub.toPath()), StandardCharsets.UTF_8);
  }

  /**
   * Asserts that the given stub file contents can be parsed, as {@code AnnotationFileParser} would
   * parse them.
   *
   * @param contents the contents of a stub file
   */
  private static void assertParseable(String contents) {
    try (InputStream inputStream =
        new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))) {
      JavaParserUtil.parseStubUnit(inputStream);
    } catch (IOException e) {
      throw new AssertionError(e);
    } catch (ParseProblemException e) {
      Assert.fail("Could not parse the stub file:\n" + contents + "\n" + e.getMessage());
    }
  }

  /** When the output file cannot be opened, the {@code IOException} is the cause of the error. */
  @Test
  public void unopenableFile() {
    String filename =
        new File(new File("no-such-directory-created-by-SceneToStubWriterTest"), "Foo.astub")
            .getPath();
    try {
      SceneToStubWriter.write(sceneWithOnePrintableClass(), filename, new TestChecker());
      Assert.fail("SceneToStubWriter.write should have thrown BugInCF");
    } catch (BugInCF e) {
      Assert.assertTrue(
          "The IOException should be the cause, but the cause is " + e.getCause(),
          e.getCause() instanceof IOException);
    }
  }

  /**
   * When writing to the output file fails, the error is detected. This test writes to /dev/full,
   * which discards all writes and reports that the device is full; the test is skipped if /dev/full
   * is not available.
   */
  @Test
  public void unwritableFile() {
    File devFull = new File("/dev/full");
    Assume.assumeTrue(devFull.exists() && devFull.canWrite());
    try {
      SceneToStubWriter.write(sceneWithOnePrintableClass(), devFull.getPath(), new TestChecker());
      Assert.fail("SceneToStubWriter.write should have thrown BugInCF");
    } catch (BugInCF e) {
      Assert.assertTrue(
          "Unexpected message: " + e.getMessage(),
          e.getMessage().contains("error writing file during WPI"));
    }
  }

  /**
   * An enum with neither enum constants nor body declarations needs no enum constant declaration,
   * so the stub file should contain neither the {@code // enum constants:} header nor the semicolon
   * that terminates the list of enum constants.
   */
  @Test
  public void emptyEnumHasNoEnumConstantDeclaration() throws IOException {
    AScene scene = new AScene();
    AClass aClass = scene.classes.getVivify("Foo");
    aClass.setTypeElement(typeElementFor("Foo", "enum Foo {}"));
    aClass.markAsEnum("Foo");
    aClass.setEnumConstants(Collections.emptyList());

    String contents = writeToString(scene);

    Assert.assertFalse(
        "The stub file should not mention enum constants, but is:\n" + contents,
        contents.contains("enum constants"));
    Assert.assertFalse(
        "The stub file should not contain an empty enum constant declaration, but is:\n" + contents,
        Pattern.compile("^\\s*;\\s*$", Pattern.MULTILINE).matcher(contents).find());
    assertParseable(contents);
  }

  /**
   * An enum with no enum constants but with body declarations still needs the semicolon that
   * separates the (empty) list of enum constants from the body declarations; without it, the stub
   * file does not parse.
   */
  @Test
  public void enumWithNoConstantsButWithAFieldHasSemicolon() throws IOException {
    TypeElement typeElt = typeElementFor("Foo", "enum Foo { ; int myfield; }");
    List<VariableElement> fields = ElementFilter.fieldsIn(typeElt.getEnclosedElements());
    Assert.assertEquals(1, fields.size());

    AScene scene = new AScene();
    AClass aClass = scene.classes.getVivify("Foo");
    aClass.setTypeElement(typeElt);
    aClass.markAsEnum("Foo");
    aClass.setEnumConstants(Collections.emptyList());
    AField aField = aClass.fields.getVivify("myfield");
    aField.setTypeMirror(fields.get(0).asType());

    String contents = writeToString(scene);

    Assert.assertFalse(
        "The stub file should not mention enum constants, but is:\n" + contents,
        contents.contains("enum constants"));
    Assert.assertTrue(
        "The stub file should declare the field, but is:\n" + contents,
        contents.contains("myfield;"));
    assertParseable(contents);
  }

  /**
   * A dollar sign is legal in a Java identifier, so a top-level class whose simple name contains a
   * dollar sign is not a nested class, even though its binary name looks like one.
   */
  @Test
  public void classWithDollarSignInItsSimpleName() throws IOException {
    AScene scene = new AScene();
    AClass aClass = scene.classes.getVivify("Foo$Bar");
    aClass.setTypeElement(typeElementFor("Foo$Bar", "class Foo$Bar {}"));

    String contents = writeToString(scene);

    Assert.assertTrue(
        "The stub file should declare one class named Foo$Bar, but is:\n" + contents,
        contents.contains("class Foo$Bar {"));
    assertParseable(contents);
  }
}
