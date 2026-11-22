package org.checkerframework.framework.stub;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests the diagnostics that {@link AnnotationFileElementTypes} issues about the annotation files
 * named by the {@code -Astubs} command-line option.
 */
public class AnnotationFileElementTypesTest {

  /** A temporary directory, deleted after each test. */
  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  /**
   * Creates a file with the given contents.
   *
   * @param file the file to create
   * @param contents the contents of the file
   * @return {@code file}
   * @throws IOException if the file cannot be written
   */
  private static File write(File file, String contents) throws IOException {
    File parent = file.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    return file;
  }

  /**
   * Compiles a trivial class with the Value Checker and the given {@code -Astubs} command-line
   * option, and returns the compiler's diagnostics.
   *
   * @param stubs the argument to the {@code -Astubs} command-line option
   * @return the diagnostics, each formatted as its kind, a colon, and its message, such as
   *     "WARNING: Ignoring Foo.stub: ..."
   * @throws IOException if the source file cannot be written or the compiler cannot be run
   */
  private List<String> diagnostics(String stubs) throws IOException {
    return diagnostics(stubs, null);
  }

  /**
   * Compiles a trivial class with the Value Checker and the given {@code -Astubs} command-line
   * option, and returns the compiler's diagnostics.
   *
   * @param stubs the argument to the {@code -Astubs} command-line option
   * @param extraClasspath a directory to prepend to the classpath, or null
   * @return the diagnostics, each formatted as its kind, a colon, and its message, such as
   *     "WARNING: Ignoring Foo.stub: ..."
   * @throws IOException if the source file cannot be written or the compiler cannot be run
   */
  private List<String> diagnostics(String stubs, @Nullable File extraClasspath)
      throws IOException {
    String classpath = System.getProperty("java.class.path");
    if (extraClasspath != null) {
      classpath = extraClasspath.getPath() + File.pathSeparator + classpath;
    }
    File sourceFile = write(new File(tmp.getRoot(), "src/Test.java"), "class Test {}");
    File classes = tmp.newFolder();
    List<String> options =
        Arrays.asList(
            "-processor",
            "org.checkerframework.common.value.ValueChecker",
            "-Astubs=" + stubs,
            "-ApermitMissingJdk",
            "-d",
            classes.getPath(),
            "-classpath",
            classpath);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      Iterable<? extends JavaFileObject> javaFiles = fileManager.getJavaFileObjects(sourceFile);
      compiler.getTask(null, fileManager, collector, options, null, javaFiles).call();
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
    List<String> result = new ArrayList<>();
    for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
      result.add(diagnostic.getKind() + ": " + diagnostic.getMessage(Locale.ROOT));
    }
    return result;
  }

  /**
   * Asserts that one of the given diagnostics starts with the given text.
   *
   * @param prefix the expected start of a diagnostic, such as "WARNING: Ignoring"
   * @param diagnostics the diagnostics that the compiler issued
   */
  private static void assertHasDiagnostic(String prefix, List<String> diagnostics) {
    for (String diagnostic : diagnostics) {
      if (diagnostic.startsWith(prefix)) {
        return;
      }
    }
    Assert.fail("No diagnostic starts with \"" + prefix + "\"; diagnostics: " + diagnostics);
  }

  /** Tests that a named file whose name does not suit its file type yields a warning. */
  @Test
  public void unusableNamedFile() throws IOException {
    File stub = write(new File(tmp.getRoot(), "Foo.stub"), "package p;");
    assertHasDiagnostic(
        "WARNING: Ignoring " + stub.getPath() + ": the name of a stub file must end with .astub",
        diagnostics(stub.getPath()));

    File ajava = write(new File(tmp.getRoot(), "Foo.ajava"), "package p;");
    assertHasDiagnostic(
        "WARNING: Ignoring " + ajava.getPath() + ", which is an ajava file; supply it via -Aajava",
        diagnostics(ajava.getPath()));
  }

  /** Tests that an archive within a named directory yields a note. */
  @Test
  public void archiveInDirectory() throws IOException {
    File dir = tmp.newFolder("stubdir");
    File jar = new File(dir, "lib.jar");
    try (OutputStream os = Files.newOutputStream(jar.toPath());
        ZipOutputStream zos = new ZipOutputStream(os)) {
      zos.putNextEntry(new ZipEntry("A.astub"));
      zos.closeEntry();
    }
    assertHasDiagnostic("NOTE: Not reading archive " + jar, diagnostics(dir.getPath()));
  }

  /** Tests that a named archive that cannot be opened yields a warning. */
  @Test
  public void corruptArchive() throws IOException {
    File jar = write(new File(tmp.getRoot(), "stubs.jar"), "not a jar file");
    assertHasDiagnostic(
        "WARNING: Could not read annotation resource: " + jar.getPath(),
        diagnostics(jar.getPath()));
  }

  /**
   * Tests that a classpath annotation file that starts with much whitespace is parsed, not skipped
   * as if it were empty.
   */
  @Test
  public void leadingWhitespaceOnClasspath() throws IOException {
    File classpathDir = tmp.newFolder();
    write(new File(classpathDir, "Padded.astub"), " ".repeat(150) + "this is not a stub file");
    assertHasDiagnostic(
        "WARNING: /Padded.astub:", diagnostics("checker.jar/Padded.astub", classpathDir));
  }
}
