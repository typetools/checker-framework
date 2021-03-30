package org.checkerframework.framework.ajava;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.utils.Pair;
import com.sun.source.util.JavacTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.framework.stub.AnnotationFileParser;

/** Inserts annotations from an ajava file into a Java file. */
public class InsertAjavaAnnotations {
  /** Utility class for working with Elements. */
  private Elements elements;

  /**
   * Gets an instance of {@code Elements} from the current Java compiler.
   *
   * @return an instance of {@code Elements}
   */
  private static Elements createElements() {
    JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      System.err.println("Could not get compiler instance");
      System.exit(1);
    }

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
    JavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
    if (fileManager == null) {
      System.err.println("Could not get file manager");
    }

    CompilationTask cTask =
        compiler.getTask(
            null, fileManager, diagnostics, Collections.emptyList(), null, Collections.emptyList());
    if (!(cTask instanceof JavacTask)) {
      System.err.println("Could not get a valid JavacTask: " + cTask.getClass());
    }

    return ((JavacTask) cTask).getElements();
  }

  /**
   * Constructs an {@code InsertAjavaAnnotations} using the given {@code Elements} instance.
   *
   * @param elements an instance of {@code Elements}
   */
  public InsertAjavaAnnotations(Elements elements) {
    this.elements = elements;
  }

  /** Represents some text to be inserted at a file and its location. */
  private static class Insertion {
    /** Offset of the insertion in characters. */
    public int position;
    /** The contents of the insertion. */
    public String contents;
    /** Whether the insertion represents an object on its own line. */
    public boolean ownLine;

    /**
     * Constructs an insertion with the given position and contents.
     *
     * @param position offset of the insertion in the file
     * @param contents contents of the insertion
     */
    public Insertion(int position, String contents) {
      this(position, contents, false);
    }

    /**
     * Constructs an insertion with the given position and contents.
     *
     * @param position offset of the insertion in the file
     * @param contents contents of the insertion
     * @param ownLine true if this insertion represents an object on its own line (doesn't affect
     *     the contents of the insertion)
     */
    public Insertion(int position, String contents, boolean ownLine) {
      this.position = position;
      this.contents = contents;
      this.ownLine = ownLine;
    }

    @Override
    public String toString() {
      return "Insertion [contents=" + contents + ", position=" + position + "]";
    }
  }

  /**
   * Given two JavaParser ASTs representing the same Java file but with differing annotations,
   * stores a list of {@link Insertion}s for all annotations in the first AST into the second AST.
   * To use this class, call {@link #visit(CompilationUnit, Node)} on a pair of ASTs and then use
   * the contents of {@link #insertions}.
   */
  private class BuildInsertionsVisitor extends DoubleJavaParserVisitor {
    /**
     * The set of annotations found in the file. Keys are both fully-qualified and simple names.
     * There are two entries for each annotation: the annotation's simple name and its
     * fully-qualified name.
     *
     * <p>The map is populated from import statements and also by when parsing a file that uses the
     * fully qualified name of an annotation it doesn't import.
     */
    private Map<String, TypeElement> allAnnotations;

    /** The annotation insertions seen so far. */
    public List<Insertion> insertions;
    /** A printer for annotations. */
    private PrettyPrinter printer;
    /** The lines of the String representation of the second AST. */
    private List<String> lines;
    /**
     * Stores the offsets of the lines in the string representation of the second AST. At index i,
     * stores the number of characters from the start of the file to the beginning of the ith line.
     */
    private List<Integer> cumulativeLineSizes;

    /**
     * Constructs a {@code BuildInsertionsVisitor} where {@code destFileContents} is the String
     * representation of the AST to insertion annotations to. When visiting a node pair, the second
     * node must always be from an AST generated from this String.
     *
     * @param destFileContents the String the second vistide AST was parsed from
     */
    public BuildInsertionsVisitor(String destFileContents) {
      allAnnotations = null;
      insertions = new ArrayList<>();
      printer = new PrettyPrinter();
      // TODO: Make this line separator agnostic, which would require keeping track of the position
      // of the start of each line. Existing methods that convert files or Strings to lines don't
      // keep track which line separator that was used at which line, such as Files.readAllLines,
      // Files.lines, or Scanner.nextLine, so this would likely require an ad hoc line splitting
      // method.
      String[] lines = destFileContents.split(System.lineSeparator());
      this.lines = Arrays.asList(lines);
      cumulativeLineSizes = new ArrayList<>();
      cumulativeLineSizes.add(0);
      for (int i = 1; i < lines.length; i++) {
        int lastSize = cumulativeLineSizes.get(i - 1);
        int lastLineLength = lines[i - 1].length() + System.lineSeparator().length();
        cumulativeLineSizes.add(lastSize + lastLineLength);
      }
    }

    @Override
    public void defaultAction(Node src, Node dest) {
      if (!(src instanceof NodeWithAnnotations<?>)) {
        return;
      }

      NodeWithAnnotations<?> srcWithAnnos = (NodeWithAnnotations<?>) src;
      if (src instanceof MethodDeclaration) {
        addAnnotationOnOwnLine(dest.getBegin().get(), srcWithAnnos.getAnnotations());
        return;
      } else if (src instanceof FieldDeclaration) {
        addAnnotationOnOwnLine(dest.getBegin().get(), srcWithAnnos.getAnnotations());
        return;
      }

      Position position;
      if (dest instanceof ClassOrInterfaceType) {
        // In a multi-part name like my.package.MyClass, annotations go directly in front of
        // MyClass instead of the full name.
        position = ((ClassOrInterfaceType) dest).getName().getBegin().get();
      } else {
        position = dest.getBegin().get();
      }

      addAnnotations(position, srcWithAnnos.getAnnotations(), 0, false);
    }

    @Override
    public void visit(ArrayType src, Node other) {
      ArrayType dest = (ArrayType) other;
      // The second component of this pair contains a list of ArrayBracketPairs from left to
      // right. For example, if src contains String[][], then the list will contain the
      // types String[] and String[][]. To insert array annotations in the correct location,
      // we insert them directly to the right of the end of the previous element.
      Pair<Type, List<ArrayType.ArrayBracketPair>> srcArrayTypes = ArrayType.unwrapArrayTypes(src);
      Pair<Type, List<ArrayType.ArrayBracketPair>> destArrayTypes =
          ArrayType.unwrapArrayTypes(dest);
      // The first annotations go directly after the element type.
      Position firstPosition = destArrayTypes.a.getEnd().get();
      addAnnotations(firstPosition, srcArrayTypes.b.get(0).getAnnotations(), 1, false);
      for (int i = 1; i < srcArrayTypes.b.size(); i++) {
        Position position = destArrayTypes.b.get(i - 1).getTokenRange().get().toRange().get().end;
        addAnnotations(position, srcArrayTypes.b.get(i).getAnnotations(), 1, true);
      }

      // Visit the component type.
      srcArrayTypes.a.accept(this, destArrayTypes.a);
    }

    @Override
    public void visit(CompilationUnit src, Node other) {
      CompilationUnit dest = (CompilationUnit) other;
      defaultAction(src, dest);

      // Gather annotations used in the ajava file.
      allAnnotations = getImportedAnnotations(src);

      // Move any annotations that appear in the declaration position but belong only in the
      // type position.
      src.accept(new TypeAnnotationMover(allAnnotations, elements), null);

      // Transfer import statements from the ajava file to the Java file.
      Set<String> existingImports = new HashSet<>();
      for (ImportDeclaration importDecl : dest.getImports()) {
        existingImports.add(printer.print(importDecl));
      }

      List<String> newImports = new ArrayList<>();
      for (ImportDeclaration importDecl : src.getImports()) {
        String importString = printer.print(importDecl);
        if (!existingImports.contains(importString)) {
          newImports.add(importString);
        }
      }

      if (!newImports.isEmpty()) {
        int position;
        int lineBreaks;
        if (!dest.getImports().isEmpty()) {
          Position lastImportPosition =
              dest.getImports().get(dest.getImports().size() - 1).getEnd().get();
          position = getAbsolutePosition(lastImportPosition) + 1;
          lineBreaks = 1;
        } else if (dest.getPackageDeclaration().isPresent()) {
          Position packagePosition = dest.getPackageDeclaration().get().getEnd().get();
          position = getAbsolutePosition(packagePosition) + 1;
          lineBreaks = 2;
        } else {
          position = 0;
          lineBreaks = 0;
        }

        String insertionContent = String.join("", newImports);
        for (int i = 0; i < lineBreaks; i++) {
          insertionContent = System.lineSeparator() + insertionContent;
        }

        insertions.add(new Insertion(position, insertionContent));
      }

      src.getModule().ifPresent(l -> l.accept(this, dest.getModule().get()));
      src.getPackageDeclaration()
          .ifPresent(l -> l.accept(this, dest.getPackageDeclaration().get()));
      for (int i = 0; i < src.getTypes().size(); i++) {
        src.getTypes().get(i).accept(this, dest.getTypes().get(i));
      }
    }

    /**
     * Creates an insertion for a collection of annotations. The annotations will appear on their
     * own line unless any non-whitespace characters precede the insertion position on its own line.
     *
     * @param position the position of the insertion
     * @param annotations List of annotations to insert
     */
    private void addAnnotationOnOwnLine(Position position, List<AnnotationExpr> annotations) {
      String line = lines.get(position.line - 1);
      int insertionColumn = position.column - 1;
      boolean ownLine = true;
      for (int i = 0; i < insertionColumn; i++) {
        if (line.charAt(i) != ' ' && line.charAt(i) != '\t') {
          ownLine = false;
          break;
        }
      }

      if (ownLine) {
        StringJoiner insertionContent = new StringJoiner(" ");
        for (AnnotationExpr annotation : annotations) {
          insertionContent.add(printer.print(annotation));
        }

        if (insertionContent.length() == 0) {
          return;
        }

        String whitespaceCopy = line.substring(0, position.column - 1);
        int absolutePosition = getAbsolutePosition(position);
        insertions.add(
            new Insertion(
                absolutePosition,
                insertionContent.toString() + System.lineSeparator() + whitespaceCopy,
                true));
      } else {
        addAnnotations(position, annotations, 0, false);
      }
    }

    /**
     * Creates an insertion for a collection of annotations at {@code position} + {@code offset}.
     *
     * @param position the position of the insertion
     * @param annotations List of annotations to insert
     * @param offset additional offset of the insertion after {@code position}
     * @param addSpaceBefore if true, the insertion content will start with a space
     */
    private void addAnnotations(
        Position position,
        Iterable<AnnotationExpr> annotations,
        int offset,
        boolean addSpaceBefore) {
      StringBuilder insertionContent = new StringBuilder();
      for (AnnotationExpr annotation : annotations) {
        insertionContent.append(printer.print(annotation));
        insertionContent.append(" ");
      }

      if (insertionContent.length() == 0) {
        return;
      }

      if (addSpaceBefore) {
        insertionContent.insert(0, " ");
      }

      int absolutePosition = getAbsolutePosition(position);
      absolutePosition += offset;
      insertions.add(new Insertion(absolutePosition, insertionContent.toString()));
    }

    /**
     * Converts a Position (which contains a line and column) to an absolute offset from the start
     * of the file
     *
     * @param position a Position
     * @return the total offset of the position from the start of the file
     */
    private int getAbsolutePosition(Position position) {
      return cumulativeLineSizes.get(position.line - 1) + (position.column - 1);
    }
  }

  /**
   * Returns all annotations imported by the annotation file as a mapping from simple and qualified
   * names to TypeElements.
   *
   * @param cu compilation unit to extract imports from
   * @return a map from names to TypeElement, for all annotations imported by the annotation file.
   *     Two entries for each annotation: one for the simple name and another for the
   *     fully-qualified name, with the same value.
   */
  private Map<String, TypeElement> getImportedAnnotations(CompilationUnit cu) {
    Map<String, TypeElement> result = new HashMap<>();
    if (cu.getImports() == null) {
      return result;
    }

    for (ImportDeclaration importDecl : cu.getImports()) {
      if (importDecl.isAsterisk()) {
        @SuppressWarnings("signature" // https://tinyurl.com/cfissue/3094:
        // com.github.javaparser.ast.expr.Name inherits toString,
        // so there can be no annotation for it
        )
        @DotSeparatedIdentifiers String imported = importDecl.getName().toString();
        if (importDecl.isStatic()) {
          // Wildcard import of members of a type (class or interface)
          TypeElement element = elements.getTypeElement(imported);
          if (element != null) {
            // Find nested annotations
            result.putAll(AnnotationFileParser.annosInType(element));
          }

        } else {
          // Wildcard import of members of a package
          PackageElement element = elements.getPackageElement(imported);
          if (element != null) {
            result.putAll(AnnotationFileParser.annosInPackage(element));
          }
        }
      } else {
        @SuppressWarnings("signature" // importDecl is non-wildcard, so its name is
        // @FullyQualifiedName
        )
        @FullyQualifiedName String imported = importDecl.getNameAsString();
        TypeElement importType = elements.getTypeElement(imported);
        if (importType != null && importType.getKind() == ElementKind.ANNOTATION_TYPE) {
          TypeElement annoElt = elements.getTypeElement(imported);
          if (annoElt != null) {
            result.put(annoElt.getSimpleName().toString(), annoElt);
          }
        }
      }
    }
    return result;
  }

  /**
   * Inserts all annotations from the ajava file read from the stream {@code annotationFile} into a
   * Java file with contents {@code fileContents} and returns the result.
   *
   * @param annotationFile input stream for an ajava file for {@code fileContents}
   * @param fileContents contents of a Java file to insert annotations into
   * @return a modified {@code fileContents} with annotations from {@code annotationFile} inserted
   */
  public String insertAnnotations(InputStream annotationFile, String fileContents) {
    CompilationUnit annotationCu = StaticJavaParser.parse(annotationFile);
    CompilationUnit fileCu = StaticJavaParser.parse(fileContents);
    BuildInsertionsVisitor insertionVisitor = new BuildInsertionsVisitor(fileContents);
    annotationCu.accept(insertionVisitor, fileCu);
    List<Insertion> insertions = insertionVisitor.insertions;
    insertions.sort(InsertAjavaAnnotations::compareInsertions);
    StringBuilder result = new StringBuilder(fileContents);
    for (Insertion insertion : insertions) {
      result.insert(insertion.position, insertion.contents);
    }

    return result.toString();
  }

  /**
   * Compares two insertions in the reverse order of where their content should appear in the file.
   * Making an insertion changes the offset values of all content after the insertion, so making the
   * insertions in reverse order of appearance removes the need to recalculate the positions of
   * other insertions.
   *
   * <p>The order in which insertions should appear is determined first by their absolute position
   * in the file, and second by whether they have their own line. In a method like,
   * {@code @Pure @Tainting String myMethod()} both annotations should be inserted at the same
   * location (right before "String"), but {@code @Pure} should always come first because it belongs
   * on its own line.
   *
   * @param insertion1 the first insertion
   * @param insertion2 the second insertion
   * @return a negative integer, zero, or a positive integer if {@code insertion1} belongs before,
   *     at the same position, or after {@code insertion2} respectively in the above ordering
   */
  private static int compareInsertions(Insertion insertion1, Insertion insertion2) {
    int cmp = Integer.compare(insertion1.position, insertion2.position);
    if (cmp == 0 && (insertion1.ownLine != insertion2.ownLine)) {
      if (insertion1.ownLine) {
        cmp = -1;
      } else {
        cmp = 1;
      }
    }

    return -cmp;
  }

  /**
   * Inserts all annotations from the ajava file at {@code annotationFilePath} to {@code
   * javaFilePath}.
   *
   * @param annotationFilePath path to an ajava file for {@code javaFilePath}
   * @param javaFilePath path to a Java file to insert annotation into
   */
  public void insertAnnotations(String annotationFilePath, String javaFilePath) {
    try {
      Path path = Paths.get(javaFilePath);
      String fileContents = new String(Files.readAllBytes(path));
      FileInputStream annotationInputStream = new FileInputStream(annotationFilePath);
      String result = insertAnnotations(annotationInputStream, fileContents);
      annotationInputStream.close();
      Files.write(path, result.getBytes());
    } catch (IOException e) {
      System.err.println(
          "Failed to insertion annotations from file "
              + annotationFilePath
              + " to file "
              + javaFilePath);
      System.exit(1);
    }
  }

  /**
   * Inserts annotations from ajava files into Java files in place.
   *
   * <p>The first argument is a file or directory containing ajava files. It may be a single ajava
   * file or a directory containing ajava files.
   *
   * <p>The second argument is a Java file or a directory containing Java files to insert
   * annotations into. The files must use the same line separator as the host system.
   *
   * <p>For each file in the second argument, checks if an ajava file from the first argument
   * matches it. For each such file, inserts all its annotations into the Java file.
   *
   * @param args command line arguments, the first element should be a path to ajava files and the
   *     second should be the directory containing Java files to insert into
   */
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println(
          "Usage: java InsertAjavaAnnotations <ajava-directory> <java-file-directory");
      System.exit(1);
    }

    String ajavaDir = args[0];
    String javaSourceDir = args[1];
    AnnotationFileStore annotationFiles = new AnnotationFileStore();
    annotationFiles.addFileOrDirectory(new File(ajavaDir));
    InsertAjavaAnnotations inserter = new InsertAjavaAnnotations(createElements());
    FileVisitor<Path> visitor =
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            if (!path.getFileName().toString().endsWith(".java")) {
              return FileVisitResult.CONTINUE;
            }

            CompilationUnit root = null;
            try {
              root = StaticJavaParser.parse(path);
            } catch (IOException e) {
              System.err.println("Failed to read file: " + path);
              System.exit(1);
            }

            Set<String> annotationFilesForRoot = new LinkedHashSet<>();
            for (TypeDeclaration<?> type : root.getTypes()) {
              String name = JavaParserUtils.getFullyQualifiedName(type, root);
              annotationFilesForRoot.addAll(annotationFiles.getAnnotationFileForType(name));
            }

            for (String annotationFile : annotationFilesForRoot) {
              inserter.insertAnnotations(annotationFile, path.toString());
            }

            return FileVisitResult.CONTINUE;
          }
        };

    try {
      Files.walkFileTree(Paths.get(javaSourceDir), visitor);
    } catch (IOException e) {
      System.out.println("Error while adding annotations to: " + javaSourceDir);
      e.printStackTrace();
    }
  }
}
