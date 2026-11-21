package org.checkerframework.framework.stub;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.SimpleVoidVisitor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.javacutil.BugInCF;

/** Utility class for annotation files (stub files and ajava files). */
public class AnnotationFileUtil {

  /** Do not instantiate. */
  private AnnotationFileUtil() {
    throw new Error("Do not instantiate");
  }

  /**
   * The types of files that can contain annotations. Also indicates the file's source, such as from
   * the JDK, built in, or from the command line.
   *
   * <p>Stub files have extension ".astub". Ajava files have extension ".ajava".
   */
  public enum AnnotationFileType {
    /** Stub file in the annotated JDK. */
    JDK_STUB,
    /** Stub file built into a checker. */
    BUILTIN_STUB,
    /** Stub file provided on command line. */
    COMMAND_LINE_STUB,
    /** Ajava file being parsed as if it is a stub file. */
    AJAVA_AS_STUB,
    /** Ajava file provided on command line. */
    AJAVA,
    /** IntelliJ annotations. */
    INTELLIJ_ANNOTATIONS;

    /**
     * Returns true if this represents a stub file.
     *
     * @return true if this represents a stub file
     */
    public boolean isStub() {
      return switch (this) {
        case JDK_STUB, BUILTIN_STUB, COMMAND_LINE_STUB, AJAVA_AS_STUB -> true;
        case AJAVA, INTELLIJ_ANNOTATIONS -> false;
        default -> throw new BugInCF("unhandled case " + this);
      };
    }

    /**
     * Returns true if this annotation file is built-in (not provided on the command line).
     *
     * @return true if this annotation file is built-in (not provided on the command line)
     */
    public boolean isBuiltIn() {
      return switch (this) {
        case JDK_STUB, BUILTIN_STUB -> true;
        case COMMAND_LINE_STUB, AJAVA_AS_STUB, AJAVA, INTELLIJ_ANNOTATIONS -> false;
        default -> throw new BugInCF("unhandled case " + this);
      };
    }

    /**
     * Returns true if this annotation file was provided on the command line (not built-in).
     *
     * @return true if this annotation file was provided on the command line (not built-in)
     */
    public boolean isCommandLine() {
      return switch (this) {
        case JDK_STUB, BUILTIN_STUB -> false;
        case COMMAND_LINE_STUB, AJAVA_AS_STUB, AJAVA, INTELLIJ_ANNOTATIONS -> true;
        default -> throw new BugInCF("unhandled case " + this);
      };
    }
  }

  /**
   * Finds the type declaration with the given class name in a StubUnit.
   *
   * @param className fully qualified name of the type declaration to find
   * @param indexFile a StubUnit to search
   * @return the declaration in {@code indexFile} with {@code className} if it exists, null
   *     otherwise
   */
  /*package-private*/ static @Nullable TypeDeclaration<?> findDeclaration(
      String className, StubUnit indexFile) {
    int indexOfDot = className.lastIndexOf('.');

    if (indexOfDot == -1) {
      // classes not within a package needs to be the first in the index file
      CompilationUnit cu = indexFile.getCompilationUnits().get(0);
      assert !cu.getPackageDeclaration().isPresent();
      return findDeclaration(className, cu);
    }

    String packageName = className.substring(0, indexOfDot);
    String simpleName = className.substring(indexOfDot + 1);

    for (CompilationUnit cu : indexFile.getCompilationUnits()) {
      if (cu.getPackageDeclaration().isPresent()
          && cu.getPackageDeclaration().get().getNameAsString().equals(packageName)) {
        TypeDeclaration<?> type = findDeclaration(simpleName, cu);
        if (type != null) {
          return type;
        }
      }
    }

    // Couldn't find it
    return null;
  }

  /*package-private*/ static TypeDeclaration<?> findDeclaration(
      TypeElement type, StubUnit indexFile) {
    return findDeclaration(type.getQualifiedName().toString(), indexFile);
  }

  /*package-private*/ static @Nullable FieldDeclaration findDeclaration(
      VariableElement field, StubUnit indexFile) {
    TypeDeclaration<?> type = findDeclaration((TypeElement) field.getEnclosingElement(), indexFile);
    if (type == null) {
      return null;
    }

    for (BodyDeclaration<?> member : type.getMembers()) {
      if (!(member instanceof FieldDeclaration decl)) {
        continue;
      }
      for (VariableDeclarator var : decl.getVariables()) {
        if (toString(var).equals(field.getSimpleName().toString())) {
          return decl;
        }
      }
    }
    return null;
  }

  /*package-private*/ static @Nullable BodyDeclaration<?> findDeclaration(
      ExecutableElement method, StubUnit indexFile) {
    TypeDeclaration<?> type =
        findDeclaration((TypeElement) method.getEnclosingElement(), indexFile);
    if (type == null) {
      return null;
    }

    String methodRep = toString(method);

    for (BodyDeclaration<?> member : type.getMembers()) {
      if (member instanceof MethodDeclaration md) {
        if (toString(md).equals(methodRep)) {
          return member;
        }
      } else if (member instanceof ConstructorDeclaration cd) {
        if (toString(cd).equals(methodRep)) {
          return member;
        }
      }
    }
    return null;
  }

  /*package-private*/ static @Nullable TypeDeclaration<?> findDeclaration(
      String simpleName, CompilationUnit cu) {
    for (TypeDeclaration<?> type : cu.getTypes()) {
      if (simpleName.equals(type.getNameAsString())) {
        return type;
      }
    }
    // Couldn't find it
    return null;
  }

  /*package-private*/ static String toString(MethodDeclaration method) {
    return ElementPrinter.toString(method);
  }

  /*package-private*/ static String toString(ConstructorDeclaration constructor) {
    return ElementPrinter.toString(constructor);
  }

  /*package-private*/ static String toString(VariableDeclarator field) {
    return field.getNameAsString();
  }

  /*package-private*/ static String toString(FieldDeclaration field) {
    assert field.getVariables().size() == 1;
    return toString(field.getVariables().get(0));
  }

  /*package-private*/ static String toString(VariableElement element) {
    assert element.getKind().isField();
    return element.getSimpleName().toString();
  }

  /**
   * Returns a string representation of the given element, or null.
   *
   * @param element an Element
   * @return a string representation of the given element, or null
   */
  /*package-private*/ static @Nullable String toString(Element element) {
    if (element instanceof ExecutableElement ee) {
      return toString(ee);
    } else if (element instanceof VariableElement ve) {
      return toString(ve);
    } else {
      return null;
    }
  }

  /**
   * A qualified name, split into the part before the last period and the part after it.
   *
   * @param typeName the part before the last period
   * @param memberName the part after the last period; the name of a field or method of {@code
   *     typeName}
   */
  public record QualifiedName(@FullyQualifiedName String typeName, String memberName) {}

  /**
   * Splits a name (which comes from an import statement) into the part before the last period and
   * the part after the last period.
   *
   * @param imported the name to split
   * @return the type name and the member name
   */
  @SuppressWarnings("signature") // string parsing
  public static QualifiedName partitionQualifiedName(String imported) {
    int lastDot = imported.lastIndexOf('.');
    @FullyQualifiedName String typeName = imported.substring(0, lastDot);
    String memberName = imported.substring(lastDot + 1);
    return new QualifiedName(typeName, memberName);
  }

  private static final class ElementPrinter extends SimpleVoidVisitor<Void> {
    public static String toString(Node n) {
      ElementPrinter printer = new ElementPrinter();
      n.accept(printer, null);
      return printer.getOutput();
    }

    private final StringBuilder sb = new StringBuilder();

    public String getOutput() {
      return sb.toString();
    }

    @Override
    public void visit(ConstructorDeclaration n, Void arg) {
      sb.append("<init>");

      sb.append('(');
      if (n.getParameters() != null) {
        for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
          Parameter p = i.next();
          p.accept(this, arg);

          if (i.hasNext()) {
            sb.append(',');
          }
        }
      }
      sb.append(')');
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
      sb.append(n.getName());

      sb.append('(');
      if (n.getParameters() != null) {
        for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
          Parameter p = i.next();
          p.accept(this, arg);

          if (i.hasNext()) {
            sb.append(',');
          }
        }
      }
      sb.append(')');
    }

    @Override
    public void visit(Parameter n, Void arg) {
      n.getType().accept(this, arg);
      if (n.isVarArgs()) {
        sb.append("[]");
      }
    }

    // Types
    @Override
    public void visit(ClassOrInterfaceType n, Void arg) {
      sb.append(n.getName());
    }

    @Override
    public void visit(PrimitiveType n, Void arg) {
      switch (n.getType()) {
        case BOOLEAN -> sb.append("boolean");
        case BYTE -> sb.append("byte");
        case CHAR -> sb.append("char");
        case DOUBLE -> sb.append("double");
        case FLOAT -> sb.append("float");
        case INT -> sb.append("int");
        case LONG -> sb.append("long");
        case SHORT -> sb.append("short");
        default -> throw new BugInCF("AnnotationFileUtil: unknown type: " + n.getType());
      }
    }

    @Override
    public void visit(com.github.javaparser.ast.type.ArrayType n, Void arg) {
      n.getComponentType().accept(this, arg);
      sb.append("[]");
    }

    @Override
    public void visit(VoidType n, Void arg) {
      sb.append("void");
    }

    @Override
    public void visit(WildcardType n, Void arg) {
      // We don't write type arguments
      // TODO: Why?
      throw new BugInCF("AnnotationFileUtil: don't print type args");
    }
  }

  /**
   * Returns the file system location that a path names. Looks for it as an absolute path or
   * relative to the current directory, then relative to the {@code user.dir} system property, then
   * relative to the {@code test.src} system property (which jtreg sets to the directory that
   * contains the test).
   *
   * @param location a relative or absolute path
   * @return the file or directory that {@code location} names, or null if none exists
   */
  public static @Nullable File findFileSystemLocation(String location) {
    File file = new File(location);
    if (file.exists()) {
      return file;
    }
    if (file.isAbsolute()) {
      return null;
    }

    file = new File(System.getProperty("user.dir"), location);
    if (file.exists()) {
      return file;
    }

    String testSrc = System.getProperty("test.src");
    if (testSrc != null) {
      file = new File(testSrc, location);
      if (file.exists()) {
        return file;
      }
    }

    return null;
  }

  /**
   * Returns annotation files found at a given file system location (does not look on classpath).
   *
   * @param location an annotation file (a stub file, ajava file, or IntelliJ IDEA annotation file),
   *     a jar or zip archive, or a directory. Look for it as described in {@link
   *     #findFileSystemLocation}. The annotation files are those that {@link
   *     #forEachAnnotationFile} finds.
   * @param fileType file type of files to collect
   * @return annotation files with the given file type found in the file system (does not look on
   *     classpath). Returns null if the file system location does not exist; the caller might wish
   *     to issue a warning in that case.
   */
  public static @Nullable List<AnnotationFileResource> allAnnotationFiles(
      String location, AnnotationFileType fileType) {
    return allAnnotationFiles(location, fileType, archive -> {});
  }

  /**
   * Returns annotation files found at a given file system location (does not look on classpath).
   *
   * @param location an annotation file (a stub file, ajava file, or IntelliJ IDEA annotation file),
   *     a jar or zip archive, or a directory. Look for it as described in {@link
   *     #findFileSystemLocation}. The annotation files are those that {@link
   *     #forEachAnnotationFile} finds.
   * @param fileType file type of files to collect
   * @param ignoredArchiveAction the operation to perform on each archive within the directory
   *     {@code location}, which is not opened
   * @return annotation files with the given file type found in the file system (does not look on
   *     classpath). Returns null if the file system location does not exist; the caller might wish
   *     to issue a warning in that case.
   */
  public static @Nullable List<AnnotationFileResource> allAnnotationFiles(
      String location, AnnotationFileType fileType, Consumer<File> ignoredArchiveAction) {
    File file = findFileSystemLocation(location);
    if (file == null) {
      return null;
    }
    List<AnnotationFileResource> resources = new ArrayList<>();
    forEachAnnotationFile(file, fileType, resources::add, ignoredArchiveAction);
    return resources;
  }

  /**
   * Passes each annotation file of type {@code fileType} at the given file system location to
   * {@code action}, in order. Passes an archive entry while its archive is open, and reads the
   * entry's contents just before passing it, so an archive's entries need not all be in memory at
   * once.
   *
   * <p>If {@code location} is a file, then it is used if it is an archive (see {@link #isArchive})
   * or if {@link #isUsableNamedFile} returns true for it. If {@code location} is a directory or
   * archive, then a file or entry within it is used if its name ends with the extension of {@code
   * fileType} ({@code .astub} for a stub file or {@code .ajava} for an ajava file) and no component
   * of its path within the directory or archive is hidden (see {@link #isHidden}). An example of a
   * hidden file is the {@code ._Foo.astub} metadata file that macOS adds to archives and to non-Mac
   * file systems. An archive within a directory is not opened; it is passed to {@code
   * ignoredArchiveAction} instead. If an archive cannot be opened or a directory cannot be listed,
   * then {@code action} is passed a resource whose {@link AnnotationFileResource#getInputStream}
   * throws an exception, so the client reports the error rather than treating the archive or
   * directory as empty.
   *
   * <p>An ajava file in an archive is read only as a stub file: {@code
   * AnnotatedTypeFactory.setRoot} never selects it as the ajava file that corresponds to a source
   * file.
   *
   * @param location a file or directory that the user named
   * @param fileType type of annotation files to pass to {@code action}
   * @param action the operation to perform on each annotation file
   * @param ignoredArchiveAction the operation to perform on each archive within the directory
   *     {@code location}, which is not opened
   */
  public static void forEachAnnotationFile(
      File location,
      AnnotationFileType fileType,
      Consumer<AnnotationFileResource> action,
      Consumer<File> ignoredArchiveAction) {
    forEachAnnotationFile(location, fileType, true, action, ignoredArchiveAction);
  }

  /**
   * Returns true if a file or archive entry whose name is {@code name} has the file name extension
   * of the given type of annotation file: {@code .astub} for a stub file, or {@code .ajava} for an
   * ajava file. Within a directory or an archive, an IntelliJ IDEA annotation file is always named
   * {@code annotations.xml}, so for that type this method returns true if the last component of
   * {@code name} is {@code annotations.xml}.
   *
   * @param name a file name or path, or the name of an archive entry
   * @param fileType a type of annotation file
   * @return true if {@code name} ends with the file name extension of {@code fileType}
   */
  static boolean hasAnnotationFileExtension(String name, AnnotationFileType fileType) {
    if (fileType == AnnotationFileType.INTELLIJ_ANNOTATIONS) {
      // A file named on the command line may have any name; see isUsableNamedFile.
      return "annotations.xml".equals(new File(name).getName());
    }
    return name.endsWith(fileType.isStub() ? ".astub" : ".ajava");
  }

  /**
   * Returns true if {@code name} is the name of a hidden file or directory: one whose name starts
   * with ".", such as {@code .git} or {@code ._Foo.astub}. The special names {@code .} and {@code
   * ..}, which denote the current and parent directory, are not hidden.
   *
   * @param name the name of a file or directory, without any directory component
   * @return true if {@code name} is the name of a hidden file or directory
   */
  private static boolean isHidden(String name) {
    return name.startsWith(".") && !name.equals(".") && !name.equals("..");
  }

  /**
   * Returns true if the archive entry whose name is {@code entryName} is an annotation file of type
   * {@code fileType}: no component of its name is hidden, and it has the file name extension of
   * {@code fileType}.
   *
   * @param entryName the name of an archive entry, such as {@code java/lang/String.astub}
   * @param fileType a type of annotation file
   * @return true if the given archive entry is an annotation file of type {@code fileType}
   */
  private static boolean isArchiveEntryAnnotationFile(
      String entryName, AnnotationFileType fileType) {
    // Archive entry names use "/" as the separator on every platform.
    for (String component : entryName.split("/", -1)) {
      if (isHidden(component)) {
        return false;
      }
    }
    return hasAnnotationFileExtension(entryName, fileType);
  }

  /**
   * Returns true if {@code name} is the name of a JAR or ZIP archive file: one whose name ends with
   * {@code .jar} or {@code .zip}, ignoring case.
   *
   * @param name a file name
   * @return true if {@code name} is the name of a JAR or ZIP archive file
   */
  static boolean isArchive(String name) {
    String lowercaseName = name.toLowerCase(Locale.ROOT);
    return lowercaseName.endsWith(".jar") || lowercaseName.endsWith(".zip");
  }

  /**
   * Returns true if a file that the user named explicitly, and that is not an archive, should be
   * used as an annotation file of type {@code fileType}. Every ajava file, and no stub file, must
   * end with {@code .ajava}. An IntelliJ IDEA annotation file is used whatever its name. Unlike a
   * file found by searching, a named file is used even if it is hidden (its name starts with ".").
   *
   * @param path a file name or path
   * @param fileType the type of annotation file that the user supplied
   * @return true if the named file should be used
   */
  static boolean isUsableNamedFile(String path, AnnotationFileType fileType) {
    if (fileType == AnnotationFileType.INTELLIJ_ANNOTATIONS) {
      return true;
    }
    return path.endsWith(".ajava") != fileType.isStub();
  }

  /**
   * Returns the contents of an entry in an archive.
   *
   * @param jarFile an open JAR or ZIP archive
   * @param entry an entry in {@code jarFile}
   * @return the contents of {@code entry}
   * @throws IOException if the entry cannot be read
   */
  private static byte[] readEntry(JarFile jarFile, JarEntry entry) throws IOException {
    try (InputStream in = jarFile.getInputStream(entry)) {
      if (in == null) {
        throw new IOException("no entry " + entry.getName() + " in " + jarFile.getName());
      }
      return in.readAllBytes();
    }
  }

  /**
   * Returns an annotation file resource for the given entry in the given archive.
   *
   * @param archive a JAR or ZIP archive
   * @param jarFile {@code archive}, open
   * @param entry an entry in {@code archive}
   * @return an annotation file resource for {@code entry}
   */
  private static AnnotationFileResource archiveEntry(
      File archive, JarFile jarFile, JarEntry entry) {
    try {
      return new JarEntryAnnotationFileResource(
          archive, entry.getName(), readEntry(jarFile, entry));
    } catch (IOException e) {
      return unreadableResource(archive.getPath() + "!" + entry.getName(), e);
    }
  }

  /**
   * Returns an annotation file resource that could not be read. Its {@link
   * AnnotationFileResource#getInputStream} throws {@code e}, so the client reports the error when
   * it reads the resource.
   *
   * @param description a description of the resource
   * @param e the exception that occurred when reading the resource
   * @return an annotation file resource whose {@code getInputStream} method throws {@code e}
   */
  private static AnnotationFileResource unreadableResource(String description, IOException e) {
    return new AnnotationFileResource() {
      @Override
      public String getDescription() {
        return description;
      }

      @Override
      public InputStream getInputStream() throws IOException {
        throw e;
      }
    };
  }

  /**
   * Passes each annotation file of type {@code fileType} at the given file system location to
   * {@code action}, as described in {@link #forEachAnnotationFile(File, AnnotationFileType,
   * Consumer, Consumer)}.
   *
   * @param location a file or directory that the user named, or that was found by searching a
   *     directory
   * @param fileType type of annotation files to pass to {@code action}
   * @param isUserSupplied true if {@code location} was named by the user (say, on the command line)
   *     rather than being found by searching a directory. A file named by the user is used if
   *     {@link #isUsableNamedFile} returns true for it. A hidden file or directory found by
   *     searching is skipped, and an archive found by searching is passed to {@code
   *     ignoredArchiveAction} rather than being opened.
   * @param action the operation to perform on each annotation file
   * @param ignoredArchiveAction the operation to perform on each archive found by searching a
   *     directory
   */
  private static void forEachAnnotationFile(
      File location,
      AnnotationFileType fileType,
      boolean isUserSupplied,
      Consumer<AnnotationFileResource> action,
      Consumer<File> ignoredArchiveAction) {
    if (!isUserSupplied && isHidden(location.getName())) {
      return;
    }
    if (location.isDirectory()) {
      File[] directoryContents = location.listFiles();
      if (directoryContents == null) {
        action.accept(
            unreadableResource(location.getPath(), new IOException("could not list directory")));
        return;
      }
      Arrays.sort(directoryContents, Comparator.comparing(File::getName));
      for (File enclosed : directoryContents) {
        forEachAnnotationFile(enclosed, fileType, false, action, ignoredArchiveAction);
      }
    } else if (location.isFile()) {
      String name = location.getName();
      if (isArchive(name)) {
        if (isUserSupplied) {
          forEachArchiveEntry(location, fileType, action);
        } else {
          ignoredArchiveAction.accept(location);
        }
      } else if (isUserSupplied
          ? isUsableNamedFile(name, fileType)
          : hasAnnotationFileExtension(name, fileType)) {
        action.accept(new FileAnnotationFileResource(location));
      }
    }
  }

  /**
   * Passes each annotation file of type {@code fileType} in the given archive to {@code action},
   * while the archive is open.
   *
   * @param archive a JAR or ZIP archive
   * @param fileType type of annotation files to pass to {@code action}
   * @param action the operation to perform on each annotation file in {@code archive}
   */
  private static void forEachArchiveEntry(
      File archive, AnnotationFileType fileType, Consumer<AnnotationFileResource> action) {
    JarFile jarFile;
    try {
      jarFile = new JarFile(archive);
    } catch (IOException e) {
      action.accept(unreadableResource(archive.getPath(), e));
      return;
    }
    try (jarFile) {
      Iterator<JarEntry> entries = jarFile.entries().asIterator();
      while (entries.hasNext()) {
        JarEntry entry = entries.next();
        // A directory entry, such as "p/annotations.xml/", is not an annotation file.
        if (!entry.isDirectory() && isArchiveEntryAnnotationFile(entry.getName(), fileType)) {
          action.accept(archiveEntry(archive, jarFile, entry));
        }
      }
    } catch (IOException e) {
      // Closing an archive that was opened for reading does not lose any data.
    }
  }

  /**
   * Returns true if the given {@link ExecutableElement} is the canonical constructor of a record
   * (i.e., the parameter types of the constructor correspond to the parameter types of the record
   * components, ignoring annotations).
   *
   * @param elt the constructor/method to check
   * @param types the Types instance to use for comparing types
   * @return true if elt is the canonical constructor of the record containing it
   */
  public static boolean isCanonicalConstructor(ExecutableElement elt, Types types) {
    if (elt.getKind() != ElementKind.CONSTRUCTOR) {
      return false;
    }
    TypeElement enclosing = (TypeElement) elt.getEnclosingElement();
    if (enclosing.getKind() != ElementKind.RECORD) {
      return false;
    }
    List<? extends Element> recordComponents = enclosing.getRecordComponents();
    if (recordComponents.size() == elt.getParameters().size()) {
      for (int i = 0; i < recordComponents.size(); i++) {
        if (!types.isSameType(
            recordComponents.get(i).asType(), elt.getParameters().get(i).asType())) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
}
