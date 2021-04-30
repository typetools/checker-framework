package org.checkerframework.framework.stub;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Position;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.WildcardType;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.framework.ajava.DefaultJointVisitor;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.FromStubFile;
import org.checkerframework.framework.stub.AnnotationFileUtil.AnnotationFileType;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.JavaParserUtil;
import org.checkerframework.framework.util.element.ElementAnnotationUtil.ErrorTypeKindException;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

// From an implementation perspective, this class represents a single annotation file (stub file or
// ajava file), notably its annotated types and its declaration annotations.
// From a client perspective, it has static methods as described below in the Javadoc.
/**
 * This class has three static methods. Each method parses an annotation file and adds annotations
 * to the {@link AnnotationFileAnnotations} passed as an argument.
 *
 * <p>The first main entry point is {@link AnnotationFileParser#parseStubFile(String, InputStream,
 * AnnotatedTypeFactory, ProcessingEnvironment, AnnotationFileAnnotations)}, which side-effects its
 * last argument. It operates in two steps. First, it calls the Annotation File Parser to parse an
 * annotation file. Then, it walks the AST to create/collect types and declaration annotations.
 *
 * <p>The second main entry point is {@link #parseAjavaFile(String, InputStream,
 * CompilationUnitTree, AnnotatedTypeFactory, ProcessingEnvironment, AnnotationFileAnnotations)}.
 * This behaves the same as {@link AnnotationFileParser#parseStubFile(String, InputStream,
 * AnnotatedTypeFactory, ProcessingEnvironment, AnnotationFileAnnotations)}, but takes an ajava file
 * instead.
 *
 * <p>The other entry point is {@link #parseJdkFileAsStub}.
 */
public class AnnotationFileParser {

  /** The type of file being parsed: stub file or ajava file. */
  private final AnnotationFileType fileType;

  /**
   * If parsing an ajava file, represents the javac tree for the compilation root of the file being
   * parsed.
   */
  private CompilationUnitTree root;

  /**
   * Whether to print warnings about types/members that were not found. The warning states that a
   * class/field in the file is not found on the user's real classpath. Since the file may contain
   * packages that are not on the classpath, this can be OK, so default to false.
   */
  private final boolean warnIfNotFound;

  /**
   * Whether to ignore missing classes even when warnIfNotFound is set to true. This allows the
   * files to contain classes not in the classpath (even if another class in the classpath has the
   * same package), but still warn if members of the class (methods, fields) are missing. This
   * option does nothing unless warnIfNotFound is also set.
   */
  private final boolean warnIfNotFoundIgnoresClasses;

  /** Whether to print warnings about stub files that overwrite annotations from bytecode. */
  private final boolean warnIfStubOverwritesBytecode;

  /**
   * Whether to print warnings about stub files that are redundant with annotations from bytecode.
   */
  private final boolean warnIfStubRedundantWithBytecode;

  /** The diagnostic kind for stub file warnings: NOTE or WARNING. */
  private final Diagnostic.Kind stubWarnDiagnosticKind;

  /** Whether to print verbose debugging messages. */
  private final boolean debugAnnotationFileParser;

  /** The name of the file being processed; used only for diagnostic messages. */
  private final String filename;

  /**
   * The AST of the parsed file that this class is processing. May be null if there was a problem
   * parsing the file. (TODO: Should the Checker Framework just halt in that case?)
   */
  // Not final in order to accommodate a default value.
  private StubUnit stubUnit;

  private final ProcessingEnvironment processingEnv;
  private final AnnotatedTypeFactory atypeFactory;
  private final Elements elements;

  /**
   * The set of annotations found in the file. Keys are both fully-qualified and simple names. There
   * are two entries for each annotation: the annotation's simple name and its fully-qualified name.
   *
   * <p>The map is populated from import statements and also by {@link #getAnnotation(
   * AnnotationExpr, Map)} for annotations that are used fully-qualified.
   *
   * @see #getAllAnnotations
   */
  private Map<String, TypeElement> allAnnotations;

  /**
   * A list of the fully-qualified names of enum constants and static fields with constant values
   * that have been imported.
   */
  private final List<@FullyQualifiedName String> importedConstants = new ArrayList<>();

  /** A map of imported fully-qualified type names to type elements. */
  private final Map<String, TypeElement> importedTypes = new HashMap<>();

  /** The annotation {@code @FromStubFile}. */
  private final AnnotationMirror fromStubFileAnno;

  /**
   * List of AnnotatedTypeMirrors for class or method type parameters that are in scope of the
   * elements currently parsed.
   */
  private final List<AnnotatedTypeVariable> typeParameters = new ArrayList<>();

  /**
   * The annotations on the declared package of the complation unit being processed. Contains null
   * if not processing a compilation unit or if the file has no declared package.
   */
  @Nullable List<AnnotationExpr> packageAnnos;

  // The following variables are stored in the AnnotationFileParser because otherwise they would
  // need to be passed through everywhere, which would be verbose.

  /**
   * The name of the type that is currently being parsed. After processing a package declaration but
   * before processing a type declaration, the type part of this may be null.
   *
   * <p>It is used both for resolving symbols and for error messages.
   */
  private FqName typeBeingParsed;

  /**
   * Contains the annotations of the file currently being processed, or null if not currently
   * processing a file. The {@code process*} methods side-effect this data structure.
   */
  @Nullable AnnotationFileAnnotations annotationFileAnnos;

  /** The line separator. */
  private static final String LINE_SEPARATOR = System.lineSeparator().intern();

  /**
   * Whether or not the file is a stub file that's part of the JDK. Non-JDK stub files override JDK
   * stub files. (Ordinarily, if two stubs are provided, they are merged.) If this is true, then
   * {@link #isBuiltinStub} is also true.
   */
  private final boolean isJdkAsStub;

  /**
   * True if the stub file is built into the checker, false if it was provided on the command line.
   *
   * <p>For a built-in stub file,
   *
   * <ul>
   *   <li>private declarations are ignored,
   *   <li>some warning messages are not issued, and
   * </ul>
   */
  private final boolean isBuiltinStub;

  /** Whether or not the {@code -AmergeStubsWithSource} command-line argument was passed. */
  private final boolean mergeStubsWithSource;

  /**
   * The result of calling AnnotationFileParser.parse: the annotated types and declaration
   * annotations from the file.
   */
  public static class AnnotationFileAnnotations {

    /**
     * Map from element to its type as declared in the annotation file.
     *
     * <p>This is a fine-grained mapping that contains all sorts of elements; contrast with {@link
     * #fakeOverrides}.
     */
    public final Map<Element, AnnotatedTypeMirror> atypes = new HashMap<>();

    /**
     * Map from a name (actually declaration element string) to the set of declaration annotations
     * on it, as written in the annotation file.
     *
     * <p>Map keys cannot be Element, because a different Element appears in the annotation files
     * than in the real files. So, map keys are the verbose element name, as returned by
     * ElementUtils.getQualifiedName.
     */
    public final Map<String, Set<AnnotationMirror>> declAnnos = new HashMap<>(1);

    /**
     * Map from a method element to all the fake overrides of it. Given a key {@code ee}, the fake
     * overrides are always in subtypes of {@code ee.getEnclosingElement()}, which is the same as
     * {@code ee.getReceiverType()}.
     */
    public final Map<ExecutableElement, List<Pair<TypeMirror, AnnotatedTypeMirror>>> fakeOverrides =
        new HashMap<>(1);
  }

  /**
   * Create a new AnnotationFileParser object, which will parse and extract annotations from the
   * given file.
   *
   * @param filename name of annotation file, used only for diagnostic messages
   * @param atypeFactory AnnotatedTypeFactory to use
   * @param processingEnv ProcessingEnvironment to use
   * @param isJdkAsStub whether or not this is a stub file that's part of the JDK
   * @param isBuiltinStub true if the stub file is built into the checker, false if the stub file
   *     was provided on the command line
   * @param fileType the type of file being parsed (stub file or ajava file)
   */
  private AnnotationFileParser(
      String filename,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv,
      boolean isJdkAsStub,
      boolean isBuiltinStub,
      AnnotationFileType fileType) {
    this.filename = filename;
    this.atypeFactory = atypeFactory;
    this.processingEnv = processingEnv;
    this.elements = processingEnv.getElementUtils();
    this.fileType = fileType;
    this.root = null;

    // TODO: This should use SourceChecker.getOptions() to allow
    // setting these flags per checker.
    Map<String, String> options = processingEnv.getOptions();
    this.warnIfNotFound = options.containsKey("stubWarnIfNotFound");
    this.warnIfNotFoundIgnoresClasses = options.containsKey("stubWarnIfNotFoundIgnoresClasses");
    this.warnIfStubOverwritesBytecode = options.containsKey("stubWarnIfOverwritesBytecode");
    this.warnIfStubRedundantWithBytecode =
        options.containsKey("stubWarnIfRedundantWithBytecode")
            && atypeFactory.shouldWarnIfStubRedundantWithBytecode();
    this.stubWarnDiagnosticKind =
        options.containsKey("stubWarnNote") ? Diagnostic.Kind.NOTE : Diagnostic.Kind.WARNING;
    this.debugAnnotationFileParser = options.containsKey("stubDebug");

    this.fromStubFileAnno = AnnotationBuilder.fromClass(elements, FromStubFile.class);

    this.isJdkAsStub = isJdkAsStub;
    this.isBuiltinStub = isBuiltinStub;
    if (isJdkAsStub) {
      assert isBuiltinStub;
    }
    this.mergeStubsWithSource = atypeFactory.getChecker().hasOption("mergeStubsWithSource");
  }

  /**
   * Sets the root of the file currently being parsed to {@code root}.
   *
   * @param root compilation unit for the file being parsed
   */
  private void setRoot(CompilationUnitTree root) {
    this.root = root;
  }

  /**
   * All annotations defined in the package (but not those nested within classes in the package).
   * Keys are both fully-qualified and simple names.
   *
   * @param packageElement a package
   * @return a map from annotation name to TypeElement
   */
  public static Map<String, TypeElement> annosInPackage(PackageElement packageElement) {
    return createNameToAnnotationMap(ElementFilter.typesIn(packageElement.getEnclosedElements()));
  }

  /**
   * All annotations declared (directly) within a class. Keys are both fully-qualified and simple
   * names.
   *
   * @param typeElement a type
   * @return a map from annotation name to TypeElement
   */
  public static Map<String, TypeElement> annosInType(TypeElement typeElement) {
    return createNameToAnnotationMap(ElementFilter.typesIn(typeElement.getEnclosedElements()));
  }

  /**
   * All annotations declared within any of the given elements.
   *
   * @param typeElements the elements whose annotations to retrieve
   * @return a map from annotation names (both fully-qualified and simple names) to TypeElement
   */
  public static Map<String, TypeElement> createNameToAnnotationMap(List<TypeElement> typeElements) {
    Map<String, TypeElement> result = new HashMap<>();
    for (TypeElement typeElm : typeElements) {
      if (typeElm.getKind() == ElementKind.ANNOTATION_TYPE) {
        putIfAbsent(result, typeElm.getSimpleName().toString(), typeElm);
        putIfAbsent(result, typeElm.getQualifiedName().toString(), typeElm);
      }
    }
    return result;
  }

  /**
   * Get all members of a Type that are importable in an annotation file. Currently these are values
   * of enums, or compile time constants.
   *
   * @param typeElement the type whose members to return
   * @return a list of fully-qualified member names
   */
  private static List<@FullyQualifiedName String> getImportableMembers(TypeElement typeElement) {
    List<VariableElement> memberElements =
        ElementFilter.fieldsIn(typeElement.getEnclosedElements());
    List<@FullyQualifiedName String> result = new ArrayList<>();
    for (VariableElement varElement : memberElements) {
      if (varElement.getConstantValue() != null
          || varElement.getKind() == ElementKind.ENUM_CONSTANT) {
        @SuppressWarnings("signature") // string concatenation
        @FullyQualifiedName String fqName =
            typeElement.getQualifiedName().toString() + "." + varElement.getSimpleName().toString();
        result.add(fqName);
      }
    }
    return result;
  }

  /**
   * Returns all annotations imported by the annotation file, as a value for {@link
   * #allAnnotations}. Note that this also modifies {@link #importedConstants} and {@link
   * #importedTypes}.
   *
   * <p>This method misses annotations that are not imported. The {@link #getAnnotation} method
   * compensates for this deficiency by adding any fully-qualified annotation that it encounters.
   *
   * @return a map from names to TypeElement, for all annotations imported by the annotation file.
   *     Two entries for each annotation: one for the simple name and another for the
   *     fully-qualified name, with the same value.
   * @see #allAnnotations
   */
  private Map<String, TypeElement> getAllAnnotations() {
    Map<String, TypeElement> result = new HashMap<>();

    // TODO: The size can be greater than 1, but this ignores all but the first element.
    assert !stubUnit.getCompilationUnits().isEmpty();
    CompilationUnit cu = stubUnit.getCompilationUnits().get(0);

    if (cu.getImports() == null) {
      return result;
    }

    for (ImportDeclaration importDecl : cu.getImports()) {
      try {
        if (importDecl.isAsterisk()) {
          @SuppressWarnings("signature" // https://tinyurl.com/cfissue/3094:
          // com.github.javaparser.ast.expr.Name inherits toString,
          // so there can be no annotation for it
          )
          @DotSeparatedIdentifiers String imported = importDecl.getName().toString();
          if (importDecl.isStatic()) {
            // Wildcard import of members of a type (class or interface)
            TypeElement element = getTypeElement(imported, "Imported type not found", importDecl);
            if (element != null) {
              // Find nested annotations
              // Find compile time constant fields, or values of an enum
              putAllNew(result, annosInType(element));
              importedConstants.addAll(getImportableMembers(element));
              addEnclosingTypesToImportedTypes(element);
            }

          } else {
            // Wildcard import of members of a package
            PackageElement element = findPackage(imported, importDecl);
            if (element != null) {
              putAllNew(result, annosInPackage(element));
              addEnclosingTypesToImportedTypes(element);
            }
          }
        } else {
          // A single (non-wildcard) import.
          @SuppressWarnings("signature" // importDecl is non-wildcard, so its name is
          // @FullyQualifiedName
          )
          @FullyQualifiedName String imported = importDecl.getNameAsString();

          final TypeElement importType = elements.getTypeElement(imported);
          if (importType == null && !importDecl.isStatic()) {
            // Class or nested class (according to JSL), but we can't resolve

            stubWarnNotFound(importDecl, "Imported type not found: " + imported);
          } else if (importType == null) {
            // static import of field or method.

            Pair<@FullyQualifiedName String, String> typeParts =
                AnnotationFileUtil.partitionQualifiedName(imported);
            String type = typeParts.first;
            String fieldName = typeParts.second;
            TypeElement enclType =
                getTypeElement(
                    type,
                    String.format("Enclosing type of static field %s not found", fieldName),
                    importDecl);

            if (enclType != null) {
              // Don't use findFieldElement(enclType, fieldName), because we don't
              // want a warning, imported might be a method.
              for (VariableElement field : ElementUtils.getAllFieldsIn(enclType, elements)) {
                // field.getSimpleName() is a CharSequence, not a String
                if (fieldName.equals(field.getSimpleName().toString())) {
                  importedConstants.add(imported);
                }
              }
            }

          } else if (importType.getKind() == ElementKind.ANNOTATION_TYPE) {
            // Single annotation or nested annotation
            TypeElement annoElt = elements.getTypeElement(imported);
            if (annoElt != null) {
              putIfAbsent(result, annoElt.getSimpleName().toString(), annoElt);
              importedTypes.put(annoElt.getSimpleName().toString(), annoElt);
            } else {
              stubWarnNotFound(importDecl, "Could not load import: " + imported);
            }
          } else {
            // Class or nested class
            // TODO: Is this needed?
            importedConstants.add(imported);
            TypeElement element = getTypeElement(imported, "Imported type not found", importDecl);
            importedTypes.put(element.getSimpleName().toString(), element);
          }
        }
      } catch (AssertionError error) {
        stubWarnNotFound(importDecl, error.toString());
      }
    }
    return result;
  }

  // If a member is imported, then consider every containing class to also be imported.
  private void addEnclosingTypesToImportedTypes(Element element) {
    for (Element enclosedEle : element.getEnclosedElements()) {
      if (enclosedEle.getKind().isClass()) {
        importedTypes.put(enclosedEle.getSimpleName().toString(), (TypeElement) enclosedEle);
      }
    }
  }

  /**
   * The main entry point. Parse a stub file and side-effects the last argument.
   *
   * @param filename name of stub file, used only for diagnostic messages
   * @param inputStream of stub file to parse
   * @param atypeFactory AnnotatedTypeFactory to use
   * @param processingEnv ProcessingEnvironment to use
   * @param annotationFileAnnos annotations from the annotation file; side-effected by this method
   * @param isBuiltinStub true if this stub file is built in to the checker, false if the stub file
   *     was supplied by the user on the command line
   */
  public static void parseStubFile(
      String filename,
      InputStream inputStream,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv,
      AnnotationFileAnnotations annotationFileAnnos,
      boolean isBuiltinStub) {
    parseStubFile(
        filename,
        inputStream,
        atypeFactory,
        processingEnv,
        annotationFileAnnos,
        false,
        isBuiltinStub);
  }

  /**
   * The main entry point when parsing an ajava file. Parses an ajava file and side-effects the last
   * two arguments.
   *
   * @param filename name of ajava file, used only for diagnostic messages
   * @param inputStream of ajava file to parse
   * @param root javac tree for the file to be parsed
   * @param atypeFactory AnnotatedTypeFactory to use
   * @param processingEnv ProcessingEnvironment to use
   * @param ajavaAnnos annotations from the ajava file; side-effected by this method
   */
  public static void parseAjavaFile(
      String filename,
      InputStream inputStream,
      CompilationUnitTree root,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv,
      AnnotationFileAnnotations ajavaAnnos) {
    AnnotationFileParser afp =
        new AnnotationFileParser(
            filename, atypeFactory, processingEnv, false, false, AnnotationFileType.AJAVA);
    try {
      afp.parseStubUnit(inputStream);
      JavaParserUtil.concatenateAddedStringLiterals(afp.stubUnit);
      afp.setRoot(root);
      afp.process(ajavaAnnos);
    } catch (ParseProblemException e) {
      for (Problem p : e.getProblems()) {
        afp.warn(null, p.getVerboseMessage());
      }
    }
  }

  /**
   * Parse a stub file that is a part of the annotated JDK and side-effects the last two arguments.
   *
   * @param filename name of stub file, used only for diagnostic messages
   * @param inputStream of stub file to parse
   * @param atypeFactory AnnotatedTypeFactory to use
   * @param processingEnv ProcessingEnvironment to use
   * @param stubAnnos annotations from the stub file; side-effected by this method
   */
  public static void parseJdkFileAsStub(
      String filename,
      InputStream inputStream,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv,
      AnnotationFileAnnotations stubAnnos) {
    parseStubFile(filename, inputStream, atypeFactory, processingEnv, stubAnnos, true);
  }

  /**
   * Parse a stub file and adds annotations to {@code annotationFileAnnos}.
   *
   * @param filename name of stub file, used only for diagnostic messages
   * @param inputStream of stub file to parse
   * @param atypeFactory AnnotatedTypeFactory to use
   * @param processingEnv ProcessingEnvironment to use
   * @param annotationFileAnnos annotations from the annotation file; side-effected by this method
   * @param isJdkAsStub whether or not the stub file is a part of the annotated JDK
   * @param isBuiltinStub true if the stub file is built-in to the checker; false if the stub file
   *     was provided on the command line
   */
  private static void parseStubFile(
      String filename,
      InputStream inputStream,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv,
      AnnotationFileAnnotations annotationFileAnnos,
      boolean isJdkAsStub,
      boolean isBuiltinStub) {
    AnnotationFileParser afp =
        new AnnotationFileParser(
            filename,
            atypeFactory,
            processingEnv,
            isJdkAsStub,
            isBuiltinStub,
            AnnotationFileType.STUB);
    try {
      afp.parseStubUnit(inputStream);
      afp.process(annotationFileAnnos);
    } catch (ParseProblemException e) {
      for (Problem p : e.getProblems()) {
        afp.warn(null, p.getVerboseMessage());
      }
    }
  }

  /**
   * Delegate to the Stub Parser to parse the annotation file to an AST, and save it in {@link
   * #stubUnit}. Also modifies other fields of this.
   *
   * <p>Subsequently, all work uses the AST.
   *
   * @param inputStream the stream from which to read an annotation file
   */
  private void parseStubUnit(InputStream inputStream) {
    if (debugAnnotationFileParser) {
      stubDebug(String.format("parsing stub file %s", filename));
    }
    stubUnit = JavaParserUtil.parseStubUnit(inputStream);

    // getAllAnnotations() also modifies importedConstants and importedTypes. This should
    // be refactored to be nicer.
    allAnnotations = getAllAnnotations();
    if (allAnnotations.isEmpty()) {
      // This issues a warning if the stub file contains no import statements.  That is
      // incorrect if the stub file contains fully-qualified annotations.
      stubWarnNotFound(
          null,
          String.format(
              "No supported annotations found! Does stub file %s import them?", filename));
    }
    // Annotations in java.lang might be used without an import statement, so add them in case.
    allAnnotations.putAll(annosInPackage(findPackage("java.lang", null)));
  }

  /**
   * Process {@link #stubUnit}, which is the AST produced by {@link #parseStubUnit}. Processing
   * means copying annotations from Stub Parser data structures to {@code annotationFileAnnos}.
   *
   * @param annotationFileAnnos annotations from the file; side-effected by this method
   */
  private void process(AnnotationFileAnnotations annotationFileAnnos) {
    this.annotationFileAnnos = annotationFileAnnos;
    processStubUnit(this.stubUnit);
    this.annotationFileAnnos = null;
  }

  /**
   * Process the given StubUnit: copy its annotations to {@code #annotationFileAnnos}.
   *
   * @param su the StubUnit to process
   */
  private void processStubUnit(StubUnit su) {
    for (CompilationUnit cu : su.getCompilationUnits()) {
      processCompilationUnit(cu);
    }
  }

  /**
   * Process the given CompilationUnit: copy its annotations to {@code #annotationFileAnnos}.
   *
   * @param cu the CompilationUnit to process
   */
  private void processCompilationUnit(CompilationUnit cu) {

    if (!cu.getPackageDeclaration().isPresent()) {
      packageAnnos = null;
      typeBeingParsed = new FqName(null, null);
    } else {
      PackageDeclaration pDecl = cu.getPackageDeclaration().get();
      packageAnnos = pDecl.getAnnotations();
      processPackage(pDecl);
    }

    if (fileType == AnnotationFileType.STUB) {
      if (cu.getTypes() != null) {
        for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
          // Not processing an ajava file, so ignore the return value.
          processTypeDecl(typeDeclaration, null, null);
        }
      }
    } else {
      root.accept(new AjavaAnnotationCollectorVisitor(), cu);
    }

    packageAnnos = null;
  }

  /**
   * Process the given package declaration: copy its annotations to {@code #annotationFileAnnos}.
   *
   * @param packDecl the package declaration to process
   */
  private void processPackage(PackageDeclaration packDecl) {
    assert (packDecl != null);
    if (!isAnnotatedForThisChecker(packDecl.getAnnotations())) {
      return;
    }
    String packageName = packDecl.getNameAsString();
    typeBeingParsed = new FqName(packageName, null);
    Element elem = elements.getPackageElement(packageName);
    // If the element lookup fails, it's because we have an annotation for a
    // package that isn't on the classpath, which is fine.
    if (elem != null) {
      recordDeclAnnotation(elem, packDecl.getAnnotations(), packDecl);
    }
    // TODO: Handle atypes???
  }

  /**
   * Returns true if the given program construct need not be read: it is private and one of the
   * following is true:
   *
   * <ul>
   *   <li>It is in the annotated JDK. Private constructs can't be referenced outside of the JDK and
   *       might refer to types that are not accessible.
   *   <li>It is not an ajava file and {@code -AmergeStubsWithSource} was not supplied. As described
   *       at https://checkerframework.org/manual/#stub-multiple-specifications, source files take
   *       precedence over stub files unless {@code -AmergeStubsWithSource} is supplied. As
   *       described at https://checkerframework.org/manual/#ajava-using, source files do not take
   *       precedence over ajava files (when reading an ajava file, it is as if {@code
   *       -AmergeStubsWithSource} were supplied).
   * </ul>
   *
   * @param node a declaration
   * @return true if the given program construct is in the annotated JDK and is private
   */
  private boolean skipNode(NodeWithAccessModifiers<?> node) {
    // Must include everything with no access modifier, because stub files are allowed to omit the
    // access modifier.  Also, interface methods have no access modifier, but they are still public.
    // Must include protected JDK methods.  For example, Object.clone is protected, but it contains
    // annotations that apply to calls like `super.clone()` and `myArray.clone()`.
    return (isBuiltinStub || (fileType == AnnotationFileType.STUB && !mergeStubsWithSource))
        && node.getModifiers().contains(Modifier.privateModifier());
  }

  /**
   * Process a type declaration: copy its annotations to {@code #annotationFileAnnos}.
   *
   * <p>This method stores the declaration's type parameters in {@link #typeParameters}. When
   * processing an ajava file, where traversal is handled externaly by a {@link
   * org.checkerframework.framework.ajava.JointJavacJavaParserVisitor}, these type variables must be
   * removed after processing the type's members. Otherwise, this method removes them.
   *
   * @param typeDecl the type declaration to process
   * @param outertypeName the name of the containing class, when processing a nested class;
   *     otherwise null
   * @param classTree the tree corresponding to typeDecl if processing an ajava file, null otherwise
   * @return a list of types variables for {@code typeDecl}. Only non-null if processing an ajava
   *     file, in which case the contents should be removed from {@link #typeParameters} after
   *     processing the type declaration's members
   */
  private List<AnnotatedTypeVariable> processTypeDecl(
      TypeDeclaration<?> typeDecl, String outertypeName, @Nullable ClassTree classTree) {
    assert typeBeingParsed != null;
    if (skipNode(typeDecl)) {
      return null;
    }
    String innerName;
    @FullyQualifiedName String fqTypeName;
    TypeElement typeElt;
    if (classTree != null) {
      typeElt = TreeUtils.elementFromDeclaration(classTree);
      innerName = typeElt.getQualifiedName().toString();
      typeBeingParsed = new FqName(typeBeingParsed.packageName, innerName);
      fqTypeName = typeBeingParsed.toString();
    } else {
      String packagePrefix = outertypeName == null ? "" : outertypeName + ".";
      innerName = packagePrefix + typeDecl.getNameAsString();
      typeBeingParsed = new FqName(typeBeingParsed.packageName, innerName);
      fqTypeName = typeBeingParsed.toString();
      typeElt = elements.getTypeElement(fqTypeName);
    }

    if (!isAnnotatedForThisChecker(typeDecl.getAnnotations())) {
      return null;
    }
    if (typeElt == null) {
      if (debugAnnotationFileParser
          || (!warnIfNotFoundIgnoresClasses
              && !hasNoAnnotationFileParserWarning(typeDecl.getAnnotations())
              && !hasNoAnnotationFileParserWarning(packageAnnos))) {
        if (elements.getAllTypeElements(fqTypeName).isEmpty()) {
          stubWarnNotFound(typeDecl, "Type not found: " + fqTypeName);
        } else {
          stubWarnNotFound(
              typeDecl,
              "Type not found uniquely: "
                  + fqTypeName
                  + " : "
                  + elements.getAllTypeElements(fqTypeName));
        }
      }
      return null;
    }

    List<AnnotatedTypeVariable> typeDeclTypeParameters = null;
    if (typeElt.getKind() == ElementKind.ENUM) {
      if (!(typeDecl instanceof EnumDeclaration)) {
        warn(
            typeDecl,
            innerName
                + " is an enum, but stub file declared it as "
                + typeDecl.toString().split("\\R", 2)[0]
                + "...");
        return null;
      }
      typeDeclTypeParameters = processEnum((EnumDeclaration) typeDecl, typeElt);
      typeParameters.addAll(typeDeclTypeParameters);
    } else if (typeElt.getKind() == ElementKind.ANNOTATION_TYPE) {
      if (!(typeDecl instanceof AnnotationDeclaration)) {
        warn(
            typeDecl,
            innerName
                + " is an annotation, but stub file declared it as "
                + typeDecl.toString().split("\\R", 2)[0]
                + "...");
        return null;
      }
      stubWarnNotFound(typeDecl, "Skipping annotation type: " + fqTypeName);
    } else if (typeDecl instanceof ClassOrInterfaceDeclaration) {
      if (!(typeDecl instanceof ClassOrInterfaceDeclaration)) {
        warn(
            typeDecl,
            innerName
                + " is a class or interface, but stub file declared it as "
                + typeDecl.toString().split("\\R", 2)[0]
                + "...");
        return null;
      }
      typeDeclTypeParameters = processType((ClassOrInterfaceDeclaration) typeDecl, typeElt);
      typeParameters.addAll(typeDeclTypeParameters);
    } // else it's an EmptyTypeDeclaration.  TODO:  An EmptyTypeDeclaration can have
    // annotations, right?

    // If processing an ajava file, then traversal is handled by a visitor, rather than the rest
    // of this method.
    if (fileType == AnnotationFileType.AJAVA) {
      return typeDeclTypeParameters;
    }

    Pair<Map<Element, BodyDeclaration<?>>, Map<Element, List<BodyDeclaration<?>>>> members =
        getMembers(typeDecl, typeElt, typeDecl);
    for (Map.Entry<Element, BodyDeclaration<?>> entry : members.first.entrySet()) {
      final Element elt = entry.getKey();
      final BodyDeclaration<?> decl = entry.getValue();
      switch (elt.getKind()) {
        case FIELD:
          processField((FieldDeclaration) decl, (VariableElement) elt);
          break;
        case ENUM_CONSTANT:
          processEnumConstant((EnumConstantDeclaration) decl, (VariableElement) elt);
          break;
        case CONSTRUCTOR:
        case METHOD:
          processCallableDeclaration((CallableDeclaration<?>) decl, (ExecutableElement) elt);
          break;
        case CLASS:
        case INTERFACE:
          // Not processing an ajava file, so ignore the return value.
          processTypeDecl((ClassOrInterfaceDeclaration) decl, innerName, null);
          break;
        case ENUM:
          // Not processing an ajava file, so ignore the return value.
          processTypeDecl((EnumDeclaration) decl, innerName, null);
          break;
        default:
          /* do nothing */
          stubWarnNotFound(decl, "AnnotationFileParser ignoring: " + elt);
          break;
      }
    }
    for (Map.Entry<Element, List<BodyDeclaration<?>>> entry : members.second.entrySet()) {
      ExecutableElement fakeOverridden = (ExecutableElement) entry.getKey();
      List<BodyDeclaration<?>> fakeOverrideDecls = entry.getValue();
      for (BodyDeclaration<?> bodyDecl : fakeOverrideDecls) {
        processFakeOverride(fakeOverridden, (CallableDeclaration<?>) bodyDecl, typeElt);
      }
    }

    if (typeDeclTypeParameters != null) {
      typeParameters.removeAll(typeDeclTypeParameters);
    }

    return null;
  }

  /**
   * Returns true if the argument contains {@code @NoAnnotationFileParserWarning}.
   *
   * @param aexprs collection of annotation expressions
   * @return true if {@code aexprs} contains {@code @NoAnnotationFileParserWarning}
   */
  private boolean hasNoAnnotationFileParserWarning(Iterable<AnnotationExpr> aexprs) {
    if (aexprs == null) {
      return false;
    }
    for (AnnotationExpr anno : aexprs) {
      if (anno.getNameAsString().equals("NoAnnotationFileParserWarning")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Process the type's declaration: copy its annotations to {@code #annotationFileAnnos}. Does not
   * process any of its members. Returns the type's type parameter declarations.
   *
   * @param decl a type declaration
   * @param elt the type's element
   * @return the type's type parameter declarations
   */
  private List<AnnotatedTypeVariable> processType(
      ClassOrInterfaceDeclaration decl, TypeElement elt) {

    recordDeclAnnotation(elt, decl.getAnnotations(), decl);
    AnnotatedDeclaredType type = atypeFactory.fromElement(elt);
    annotate(type, decl.getAnnotations(), decl);

    final List<? extends AnnotatedTypeMirror> typeArguments = type.getTypeArguments();
    final List<TypeParameter> typeParameters = decl.getTypeParameters();

    // It can be the case that args=[] and params=null, so don't crash in that case.
    // if ((typeParameters == null) != (typeArguments == null)) {
    //     throw new Error(String.format("parseType (%s, %s): inconsistent nullness for args and
    // params%n  args = %s%n  params = %s%n", decl, elt, typeArguments, typeParameters));
    // }

    if (debugAnnotationFileParser) {
      int numParams = (typeParameters == null ? 0 : typeParameters.size());
      int numArgs = (typeArguments == null ? 0 : typeArguments.size());
      if (numParams != numArgs) {
        stubDebug(
            String.format(
                "parseType:  mismatched sizes for typeParameters=%s (size %d) and typeArguments=%s"
                    + " (size %d); decl=%s; elt=%s (%s); type=%s (%s); typeBeingParsed=%s",
                typeParameters,
                numParams,
                typeArguments,
                numArgs,
                decl.toString().replace(LINE_SEPARATOR, " "),
                elt.toString().replace(LINE_SEPARATOR, " "),
                elt.getClass(),
                type,
                type.getClass(),
                typeBeingParsed));
        stubDebug("Proceeding despite mismatched sizes");
      }
    }

    annotateTypeParameters(decl, elt, typeArguments, typeParameters);
    annotateSupertypes(decl, type);
    putMerge(annotationFileAnnos.atypes, elt, type);
    List<AnnotatedTypeVariable> typeVariables = new ArrayList<>(type.getTypeArguments().size());
    for (AnnotatedTypeMirror typeV : type.getTypeArguments()) {
      if (typeV.getKind() != TypeKind.TYPEVAR) {
        warn(
            decl,
            "expected an AnnotatedTypeVariable but found type kind "
                + typeV.getKind()
                + ": "
                + typeV);
      } else {
        typeVariables.add((AnnotatedTypeVariable) typeV);
      }
    }
    return typeVariables;
  }

  /**
   * Process an enum: copy its annotations to {@code #annotationFileAnnos}. Returns the enum's type
   * parameter declarations.
   *
   * @param decl enum declaration
   * @param elt element representing enum
   * @return the enum's type parameter declarations
   */
  private List<AnnotatedTypeVariable> processEnum(EnumDeclaration decl, TypeElement elt) {

    recordDeclAnnotation(elt, decl.getAnnotations(), decl);
    AnnotatedDeclaredType type = atypeFactory.fromElement(elt);
    annotate(type, decl.getAnnotations(), decl);

    putMerge(annotationFileAnnos.atypes, elt, type);
    List<AnnotatedTypeVariable> typeVariables = new ArrayList<>(type.getTypeArguments().size());
    for (AnnotatedTypeMirror typeV : type.getTypeArguments()) {
      if (typeV.getKind() != TypeKind.TYPEVAR) {
        warn(
            decl,
            "expected an AnnotatedTypeVariable but found type kind "
                + typeV.getKind()
                + ": "
                + typeV);
      } else {
        typeVariables.add((AnnotatedTypeVariable) typeV);
      }
    }
    return typeVariables;
  }

  private void annotateSupertypes(
      ClassOrInterfaceDeclaration typeDecl, AnnotatedDeclaredType type) {
    if (typeDecl.getExtendedTypes() != null) {
      for (ClassOrInterfaceType supertype : typeDecl.getExtendedTypes()) {
        AnnotatedDeclaredType annotatedSupertype =
            findAnnotatedType(supertype, type.directSupertypes(), typeDecl);
        if (annotatedSupertype == null) {
          warn(
              typeDecl,
              "stub file does not match bytecode: "
                  + "could not find superclass "
                  + supertype
                  + " from type "
                  + type);
        } else {
          annotate(annotatedSupertype, supertype, null, typeDecl);
        }
      }
    }
    if (typeDecl.getImplementedTypes() != null) {
      for (ClassOrInterfaceType supertype : typeDecl.getImplementedTypes()) {
        AnnotatedDeclaredType annotatedSupertype =
            findAnnotatedType(supertype, type.directSupertypes(), typeDecl);
        if (annotatedSupertype == null) {
          warn(
              typeDecl,
              "stub file does not match bytecode: "
                  + "could not find superinterface "
                  + supertype
                  + " from type "
                  + type);
        } else {
          annotate(annotatedSupertype, supertype, null, typeDecl);
        }
      }
    }
  }

  /**
   * Process a method or constructor declaration: copy its annotations to {@code
   * #annotationFileAnnos}.
   *
   * @param decl a method or constructor declaration, as read from an annotation file
   * @param elt the method or constructor's element
   * @return type variables for the method
   */
  private List<AnnotatedTypeVariable> processCallableDeclaration(
      CallableDeclaration<?> decl, ExecutableElement elt) {
    if (!isAnnotatedForThisChecker(decl.getAnnotations())) {
      return null;
    }
    // Declaration annotations
    recordDeclAnnotation(elt, decl.getAnnotations(), decl);
    if (decl.isMethodDeclaration()) {
      // AnnotationFileParser parses all annotations in type annotation position as type
      // annotations.
      recordDeclAnnotation(elt, ((MethodDeclaration) decl).getType().getAnnotations(), decl);
    }
    recordDeclAnnotationFromAnnotationFile(elt);

    AnnotatedExecutableType methodType = atypeFactory.fromElement(elt);
    AnnotatedExecutableType origMethodType =
        warnIfStubRedundantWithBytecode ? methodType.deepCopy() : null;

    // Type Parameters
    annotateTypeParameters(decl, elt, methodType.getTypeVariables(), decl.getTypeParameters());
    typeParameters.addAll(methodType.getTypeVariables());

    // Return type, from declaration annotations on the method or constructor
    if (decl.isMethodDeclaration()) {
      try {
        annotate(
            methodType.getReturnType(),
            ((MethodDeclaration) decl).getType(),
            decl.getAnnotations(),
            decl);
      } catch (ErrorTypeKindException e) {
        // Do nothing, per Issue #244.
      }
    } else {
      assert decl.isConstructorDeclaration();
      annotate(methodType.getReturnType(), decl.getAnnotations(), decl);
    }

    // Parameters
    processParameters(decl, elt, methodType);

    // Receiver
    if (decl.getReceiverParameter().isPresent()) {
      ReceiverParameter receiverParameter = decl.getReceiverParameter().get();
      if (methodType.getReceiverType() == null) {
        if (decl.isConstructorDeclaration()) {
          warn(
              receiverParameter,
              "parseParameter: constructor %s of a top-level class cannot have receiver"
                  + " annotations %s",
              methodType,
              decl.getReceiverParameter().get().getAnnotations());
        } else {
          warn(
              receiverParameter,
              "parseParameter: static method %s cannot have receiver annotations %s",
              methodType,
              decl.getReceiverParameter().get().getAnnotations());
        }
      } else {
        // Add declaration annotations.
        annotate(
            methodType.getReceiverType(),
            decl.getReceiverParameter().get().getAnnotations(),
            receiverParameter);
        // Add type annotations.
        annotate(
            methodType.getReceiverType(),
            decl.getReceiverParameter().get().getType(),
            decl.getReceiverParameter().get().getAnnotations(),
            receiverParameter);
      }
    }

    if (warnIfStubRedundantWithBytecode
        && methodType.toString().equals(origMethodType.toString())
        && !isBuiltinStub) {
      warn(
          decl,
          String.format(
              "redundant stub file specification for %s", ElementUtils.getQualifiedName(elt)));
    }

    // Store the type.
    putMerge(annotationFileAnnos.atypes, elt, methodType);
    if (fileType == AnnotationFileType.STUB) {
      typeParameters.removeAll(methodType.getTypeVariables());
    }

    return methodType.getTypeVariables();
  }

  /**
   * Process the parameters of a method or constructor declaration: copy their annotations to {@code
   * #annotationFileAnnos}.
   *
   * @param method a Method or Constructor declaration
   * @param elt ExecutableElement of {@code method}
   * @param methodType annotated type of {@code method}
   */
  private void processParameters(
      CallableDeclaration<?> method, ExecutableElement elt, AnnotatedExecutableType methodType) {
    List<Parameter> params = method.getParameters();
    List<? extends VariableElement> paramElts = elt.getParameters();
    List<? extends AnnotatedTypeMirror> paramTypes = methodType.getParameterTypes();

    for (int i = 0; i < methodType.getParameterTypes().size(); ++i) {
      VariableElement paramElt = paramElts.get(i);
      AnnotatedTypeMirror paramType = paramTypes.get(i);
      Parameter param = params.get(i);

      recordDeclAnnotation(paramElt, param.getAnnotations(), param);
      recordDeclAnnotation(paramElt, param.getType().getAnnotations(), param);

      if (param.isVarArgs()) {
        assert paramType.getKind() == TypeKind.ARRAY;
        // The "type" of param is actually the component type of the vararg.
        // For example, in "Object..." the type would be "Object".
        annotate(
            ((AnnotatedArrayType) paramType).getComponentType(),
            param.getType(),
            param.getAnnotations(),
            param);
        // The "VarArgsAnnotations" are those just before "...".
        annotate(paramType, param.getVarArgsAnnotations(), param);
      } else {
        annotate(paramType, param.getType(), param.getAnnotations(), param);
        putMerge(annotationFileAnnos.atypes, paramElt, paramType);
      }
    }
  }

  /**
   * Clear (remove) existing annotations on the type.
   *
   * <p>Stub files override annotations read from .class files. Using {@code replaceAnnotation}
   * usually achieves this; however, for annotations on type variables, it is sometimes necessary to
   * remove an existing annotation, leaving no annotation on the type variable. This method does so.
   *
   * @param atype the type to modify
   * @param typeDef the type from the annotation file, used only for diagnostic messages
   */
  @SuppressWarnings("unused") // for disabled warning message
  private void clearAnnotations(AnnotatedTypeMirror atype, Type typeDef) {
    // TODO: only produce output if the removed annotation isn't the top or default
    // annotation in the type hierarchy.  See https://tinyurl.com/cfissue/2759 .
    /*
    if (!atype.getAnnotations().isEmpty()) {
        stubWarnOverwritesBytecode(
                String.format(
                        "in file %s at line %s removed existing annotations on type: %s",
                        filename.substring(filename.lastIndexOf('/') + 1),
                        typeDef.getBegin().get().line,
                        atype.toString(true)));
    }
    */
    // Clear existing annotations, which only makes a difference for
    // type variables, but doesn't hurt in other cases.
    atype.clearAnnotations();
  }

  /**
   * Add the annotations from {@code type} to {@code atype}. Type annotations that parsed as
   * declaration annotations (i.e., type annotations in {@code declAnnos}) are applied to the
   * innermost component type.
   *
   * @param atype annotated type to which to add annotations
   * @param type parsed type
   * @param declAnnos annotations stored on the declaration of the variable with this type or null
   * @param astNode where to report errors
   */
  private void annotateAsArray(
      AnnotatedArrayType atype,
      ReferenceType type,
      @Nullable NodeList<AnnotationExpr> declAnnos,
      NodeWithRange<?> astNode) {
    annotateInnermostComponentType(atype, declAnnos, astNode);
    Type typeDef = type;
    AnnotatedTypeMirror currentAtype = atype;
    while (typeDef.isArrayType()) {
      if (currentAtype.getKind() != TypeKind.ARRAY) {
        warn(astNode, "Mismatched array lengths; atype: " + atype + "%n  type: " + type);
        return;
      }

      // handle generic type
      clearAnnotations(currentAtype, typeDef);

      List<AnnotationExpr> annotations = typeDef.getAnnotations();
      if (annotations != null) {
        annotate(currentAtype, annotations, astNode);
      }
      typeDef = ((com.github.javaparser.ast.type.ArrayType) typeDef).getComponentType();
      currentAtype = ((AnnotatedArrayType) currentAtype).getComponentType();
    }
    if (currentAtype.getKind() == TypeKind.ARRAY) {
      warn(astNode, "Mismatched array lengths; atype: " + atype + "%n  type: " + type);
    }
  }

  private ClassOrInterfaceType unwrapDeclaredType(Type type) {
    if (type instanceof ClassOrInterfaceType) {
      return (ClassOrInterfaceType) type;
    } else if (type instanceof ReferenceType && type.getArrayLevel() == 0) {
      return unwrapDeclaredType(type.getElementType());
    } else {
      return null;
    }
  }

  /**
   * Add to formal parameter {@code atype}:
   *
   * <ol>
   *   <li>the annotations from {@code typeDef}, and
   *   <li>any type annotations that parsed as declaration annotations (i.e., type annotations in
   *       {@code declAnnos}).
   * </ol>
   *
   * @param atype annotated type to which to add annotations
   * @param typeDef parsed type
   * @param declAnnos annotations stored on the declaration of the variable with this type, or null
   * @param astNode where to report errors
   */
  private void annotate(
      AnnotatedTypeMirror atype,
      Type typeDef,
      @Nullable NodeList<AnnotationExpr> declAnnos,
      NodeWithRange<?> astNode) {
    if (atype.getKind() == TypeKind.ARRAY) {
      if (typeDef instanceof ReferenceType) {
        annotateAsArray((AnnotatedArrayType) atype, (ReferenceType) typeDef, declAnnos, astNode);
      } else {
        warn(astNode, "expected ReferenceType but found: " + typeDef);
      }
      return;
    }

    clearAnnotations(atype, typeDef);

    // Primary annotations for the type of a variable declaration are not stored in typeDef, but
    // rather as declaration annotations (passed as declAnnos to this method).  But, if typeDef
    // is not the type of a variable, then the primary annotations are stored in typeDef.
    NodeList<AnnotationExpr> primaryAnnotations;
    if (typeDef.getAnnotations().isEmpty() && declAnnos != null) {
      primaryAnnotations = declAnnos;
    } else {
      primaryAnnotations = typeDef.getAnnotations();
    }
    if (atype.getKind() != TypeKind.WILDCARD) {
      // The primary annotation on a wildcard applies to the super or extends bound and
      // are added below.
      annotate(atype, primaryAnnotations, astNode);
    }

    switch (atype.getKind()) {
      case DECLARED:
        ClassOrInterfaceType declType = unwrapDeclaredType(typeDef);
        if (declType == null) {
          break;
        }
        AnnotatedDeclaredType adeclType = (AnnotatedDeclaredType) atype;
        // Process type arguments.
        if (declType.getTypeArguments().isPresent()
            && !declType.getTypeArguments().get().isEmpty()
            && !adeclType.getTypeArguments().isEmpty()) {
          if (declType.getTypeArguments().get().size() != adeclType.getTypeArguments().size()) {
            warn(
                astNode,
                String.format(
                    "Mismatch in type argument size between %s (%d) and %s (%d)",
                    declType,
                    declType.getTypeArguments().get().size(),
                    adeclType,
                    adeclType.getTypeArguments().size()));
            break;
          }
          for (int i = 0; i < declType.getTypeArguments().get().size(); ++i) {
            annotate(
                adeclType.getTypeArguments().get(i),
                declType.getTypeArguments().get().get(i),
                null,
                astNode);
          }
        }
        break;
      case WILDCARD:
        AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) atype;
        // Ensure that the file also has a wildcard type, report an error otherwise
        if (!typeDef.isWildcardType()) {
          // We throw an error here, as otherwise we are just getting a generic cast error
          // on the very next line.
          warn(
              astNode,
              "Wildcard type <"
                  + atype
                  + "> does not match type in stubs file"
                  + filename
                  + ": <"
                  + typeDef
                  + ">"
                  + " while parsing "
                  + typeBeingParsed);
          return;
        }
        WildcardType wildcardDef = (WildcardType) typeDef;
        if (wildcardDef.getExtendedType().isPresent()) {
          annotate(
              wildcardType.getExtendsBound(), wildcardDef.getExtendedType().get(), null, astNode);
          annotate(wildcardType.getSuperBound(), primaryAnnotations, astNode);
        } else if (wildcardDef.getSuperType().isPresent()) {
          annotate(wildcardType.getSuperBound(), wildcardDef.getSuperType().get(), null, astNode);
          annotate(wildcardType.getExtendsBound(), primaryAnnotations, astNode);
        } else {
          annotate(atype, primaryAnnotations, astNode);
        }
        break;
      case TYPEVAR:
        // Add annotations from the declaration of the TypeVariable
        AnnotatedTypeVariable typeVarUse = (AnnotatedTypeVariable) atype;
        Types typeUtils = processingEnv.getTypeUtils();
        for (AnnotatedTypeVariable typePar : typeParameters) {
          if (typeUtils.isSameType(typePar.getUnderlyingType(), atype.getUnderlyingType())) {
            atypeFactory.replaceAnnotations(typePar.getUpperBound(), typeVarUse.getUpperBound());
            atypeFactory.replaceAnnotations(typePar.getLowerBound(), typeVarUse.getLowerBound());
          }
        }
        break;
      default:
        // No additional annotations to add.
    }
  }

  /**
   * Process the field declaration in decl: copy its annotations to {@code #annotationFileAnnos}.
   *
   * @param decl the declaration in the annotation file
   * @param elt the element representing that same declaration
   */
  private void processField(FieldDeclaration decl, VariableElement elt) {
    if (skipNode(decl)) {
      // Don't process private fields of the JDK.  They can't be referenced outside of the JDK
      // and might refer to types that are not accessible.
      return;
    }
    recordDeclAnnotationFromAnnotationFile(elt);
    recordDeclAnnotation(elt, decl.getAnnotations(), decl);
    // AnnotationFileParser parses all annotations in type annotation position as type annotations
    recordDeclAnnotation(elt, decl.getElementType().getAnnotations(), decl);
    AnnotatedTypeMirror fieldType = atypeFactory.fromElement(elt);

    VariableDeclarator fieldVarDecl = null;
    String eltName = elt.getSimpleName().toString();
    for (VariableDeclarator var : decl.getVariables()) {
      if (var.getName().toString().equals(eltName)) {
        fieldVarDecl = var;
        break;
      }
    }
    assert fieldVarDecl != null;
    annotate(fieldType, fieldVarDecl.getType(), decl.getAnnotations(), fieldVarDecl);
    putMerge(annotationFileAnnos.atypes, elt, fieldType);
  }

  /**
   * Adds the annotations present on the declaration of an enum constant to the ATM of that
   * constant.
   *
   * @param decl the enum constant, in Javaparser AST form (the source of annotations)
   * @param elt the enum constant declaration, as an element (the destination for annotations)
   */
  private void processEnumConstant(EnumConstantDeclaration decl, VariableElement elt) {
    recordDeclAnnotationFromAnnotationFile(elt);
    recordDeclAnnotation(elt, decl.getAnnotations(), decl);
    AnnotatedTypeMirror enumConstType = atypeFactory.fromElement(elt);
    annotate(enumConstType, decl.getAnnotations(), decl);
    putMerge(annotationFileAnnos.atypes, elt, enumConstType);
  }

  /**
   * Returns the innermost component type of {@code type}.
   *
   * @param type array type
   * @return the innermost component type of {@code type}
   */
  private AnnotatedTypeMirror innermostComponentType(AnnotatedArrayType type) {
    AnnotatedTypeMirror componentType = type;
    while (componentType.getKind() == TypeKind.ARRAY) {
      componentType = ((AnnotatedArrayType) componentType).getComponentType();
    }
    return componentType;
  }

  /**
   * Adds {@code annotations} to the innermost component type of {@code type}.
   *
   * @param type array type
   * @param annotations annotations to add
   * @param astNode where to report errors
   */
  private void annotateInnermostComponentType(
      AnnotatedArrayType type, List<AnnotationExpr> annotations, NodeWithRange<?> astNode) {
    annotate(innermostComponentType(type), annotations, astNode);
  }

  /**
   * Annotate the type with the given type annotations, removing any existing annotations from the
   * same qualifier hierarchies.
   *
   * @param type the type to annotate
   * @param annotations the new annotations for the type
   * @param astNode where to report errors
   */
  private void annotate(
      AnnotatedTypeMirror type, List<AnnotationExpr> annotations, NodeWithRange<?> astNode) {
    if (annotations == null) {
      return;
    }
    for (AnnotationExpr annotation : annotations) {
      AnnotationMirror annoMirror = getAnnotation(annotation, allAnnotations);
      if (annoMirror != null) {
        type.replaceAnnotation(annoMirror);
      } else {
        // TODO: Maybe always warn here.  It's so easy to forget an import statement and
        // have an annotation silently ignored.
        stubWarnNotFound(astNode, "Unknown annotation " + annotation);
      }
    }
  }

  /**
   * Adds to {@code annotationFileAnnos} all the annotations in {@code annotations} that are
   * applicable to {@code elt}'s location. For example, if an annotation is a type annotation but
   * {@code elt} is a field declaration, the type annotation will be ignored.
   *
   * @param elt the element to be annotated
   * @param annotations set of annotations that may be applicable to elt
   * @param astNode where to report errors
   */
  private void recordDeclAnnotation(
      Element elt, List<AnnotationExpr> annotations, NodeWithRange<?> astNode) {
    if (annotations == null) {
      return;
    }
    Set<AnnotationMirror> annos = AnnotationUtils.createAnnotationSet();
    for (AnnotationExpr annotation : annotations) {
      AnnotationMirror annoMirror = getAnnotation(annotation, allAnnotations);
      if (annoMirror != null) {
        // The @Target annotation on `annotation`/`annoMirror`
        Target target = annoMirror.getAnnotationType().asElement().getAnnotation(Target.class);
        // Only add the declaration annotation if the annotation applies to the element.
        if (AnnotationUtils.getElementKindsForTarget(target).contains(elt.getKind())) {
          // `annoMirror` is applicable to `elt`
          annos.add(annoMirror);
        }
      } else {
        // TODO: Maybe always warn here.  It's so easy to forget an import statement and
        // have an annotation silently ignored.
        stubWarnNotFound(astNode, String.format("Unknown annotation %s", annotation));
      }
    }
    String eltName = ElementUtils.getQualifiedName(elt);
    putOrAddToMap(annotationFileAnnos.declAnnos, eltName, annos);
  }

  /**
   * Adds the declaration annotation {@code @FromStubFile} to {@link #annotationFileAnnos}, unless
   * we are parsing the JDK as a stub file.
   *
   * @param elt an element to be annotated as {@code @FromStubFile}
   */
  private void recordDeclAnnotationFromAnnotationFile(Element elt) {
    if (fileType == AnnotationFileType.AJAVA || isJdkAsStub) {
      return;
    }
    putOrAddToMap(
        annotationFileAnnos.declAnnos,
        ElementUtils.getQualifiedName(elt),
        Collections.singleton(fromStubFileAnno));
  }

  private void annotateTypeParameters(
      BodyDeclaration<?> decl, // for debugging
      Object elt, // for debugging; TypeElement or ExecutableElement
      List<? extends AnnotatedTypeMirror> typeArguments,
      List<TypeParameter> typeParameters) {
    if (typeParameters == null) {
      return;
    }

    if (typeParameters.size() != typeArguments.size()) {
      String msg =
          String.format(
              "annotateTypeParameters: mismatched sizes:  typeParameters (size %d)=%s; "
                  + " typeArguments (size %d)=%s;  decl=%s;  elt=%s (%s).",
              typeParameters.size(),
              typeParameters,
              typeArguments.size(),
              typeArguments,
              decl.toString().replace(LINE_SEPARATOR, " "),
              elt.toString().replace(LINE_SEPARATOR, " "),
              elt.getClass());
      if (!debugAnnotationFileParser) {
        msg = msg + "; for more details, run with -AstubDebug";
      }
      warn(decl, msg);
      return;
    }
    for (int i = 0; i < typeParameters.size(); ++i) {
      TypeParameter param = typeParameters.get(i);
      AnnotatedTypeVariable paramType = (AnnotatedTypeVariable) typeArguments.get(i);

      if (param.getTypeBound() == null || param.getTypeBound().isEmpty()) {
        // No bound so annotations are both lower and upper bounds
        annotate(paramType, param.getAnnotations(), param);
      } else if (param.getTypeBound() != null && !param.getTypeBound().isEmpty()) {
        annotate(paramType.getLowerBound(), param.getAnnotations(), param);
        annotate(paramType.getUpperBound(), param.getTypeBound().get(0), null, param);
        if (param.getTypeBound().size() > 1) {
          // TODO: add support for intersection types
          stubWarnNotFound(param, "Annotations on intersection types are not yet supported");
        }
      }
      putMerge(annotationFileAnnos.atypes, paramType.getUnderlyingType().asElement(), paramType);
    }
  }

  /**
   * Returns a pair of mappings. For each member declaration of the JavaParser type declaration
   * {@code typeDecl}:
   *
   * <ul>
   *   <li>If {@code typeElt} contains a member element for it, the first mapping maps the member
   *       element to it.
   *   <li>If it is a fake override, the second mapping maps each element it overrides to it.
   *   <li>Otherwise, does nothing.
   * </ul>
   *
   * This method does not read or write the field {@link #annotationFileAnnos}.
   *
   * @param typeDecl a JavaParser type declaration
   * @param typeElt the javac element for {@code typeDecl}
   * @return two mappings: from javac elements to their JavaParser declaration, and from javac
   *     elements to fake overrides of them
   * @param astNode where to report errors
   */
  private Pair<Map<Element, BodyDeclaration<?>>, Map<Element, List<BodyDeclaration<?>>>> getMembers(
      TypeDeclaration<?> typeDecl, TypeElement typeElt, NodeWithRange<?> astNode) {
    assert (typeElt.getSimpleName().contentEquals(typeDecl.getNameAsString())
            || typeDecl.getNameAsString().endsWith("$" + typeElt.getSimpleName()))
        : String.format("%s  %s", typeElt.getSimpleName(), typeDecl.getName());

    Map<Element, BodyDeclaration<?>> elementsToDecl = new LinkedHashMap<>();
    Map<Element, List<BodyDeclaration<?>>> fakeOverrideDecls = new LinkedHashMap<>();

    for (BodyDeclaration<?> member : typeDecl.getMembers()) {
      putNewElement(
          elementsToDecl, fakeOverrideDecls, typeElt, member, typeDecl.getNameAsString(), astNode);
    }
    // For an enum type declaration, also add the enum constants
    if (typeDecl instanceof EnumDeclaration) {
      EnumDeclaration enumDecl = (EnumDeclaration) typeDecl;
      // getEntries() gives the list of enum constant declarations
      for (BodyDeclaration<?> member : enumDecl.getEntries()) {
        putNewElement(
            elementsToDecl,
            fakeOverrideDecls,
            typeElt,
            member,
            typeDecl.getNameAsString(),
            astNode);
      }
    }

    return Pair.of(elementsToDecl, fakeOverrideDecls);
  }

  // Used only by getMembers().
  /**
   * If {@code typeElt} contains an element for {@code member}, adds to {@code elementsToDecl} a
   * mapping from member's element to member. Does nothing if a mapping already exists.
   *
   * <p>Otherwise (if there is no element for {@code member}), adds to {@code fakeOverrideDecls}
   * zero or more mappings. Each mapping is from an element that {@code member} would override to
   * {@code member}.
   *
   * <p>This method does not read or write field {@link annotationFileAnnos}.
   *
   * @param elementsToDecl the mapping that is side-effected by this method
   * @param fakeOverrideDecls fake overrides, also side-effected by this method
   * @param typeElt the class in which {@code member} is declared
   * @param member the stub file declaration of a method
   * @param typeDeclName used only for debugging
   * @param astNode where to report errors
   */
  private void putNewElement(
      Map<Element, BodyDeclaration<?>> elementsToDecl,
      Map<Element, List<BodyDeclaration<?>>> fakeOverrideDecls,
      TypeElement typeElt,
      BodyDeclaration<?> member,
      String typeDeclName,
      NodeWithRange<?> astNode) {
    if (member instanceof MethodDeclaration) {
      MethodDeclaration method = (MethodDeclaration) member;
      Element elt = findElement(typeElt, method, /*noWarn=*/ true);
      if (elt != null) {
        putIfAbsent(elementsToDecl, elt, method);
      } else {
        ExecutableElement overriddenMethod = fakeOverriddenMethod(typeElt, method);
        if (overriddenMethod == null) {
          // Didn't find the element and it isn't a fake override.  Issue a warning.
          findElement(typeElt, method, /*noWarn=*/ false);
        } else {
          List<BodyDeclaration<?>> l =
              fakeOverrideDecls.computeIfAbsent(overriddenMethod, __ -> new ArrayList<>());
          l.add(member);
        }
      }
    } else if (member instanceof ConstructorDeclaration) {
      Element elt = findElement(typeElt, (ConstructorDeclaration) member);
      if (elt != null) {
        putIfAbsent(elementsToDecl, elt, member);
      }
    } else if (member instanceof FieldDeclaration) {
      FieldDeclaration fieldDecl = (FieldDeclaration) member;
      for (VariableDeclarator var : fieldDecl.getVariables()) {
        Element varelt = findElement(typeElt, var);
        if (varelt != null) {
          putIfAbsent(elementsToDecl, varelt, fieldDecl);
        }
      }
    } else if (member instanceof EnumConstantDeclaration) {
      Element elt = findElement(typeElt, (EnumConstantDeclaration) member, astNode);
      if (elt != null) {
        putIfAbsent(elementsToDecl, elt, member);
      }
    } else if (member instanceof ClassOrInterfaceDeclaration) {
      Element elt = findElement(typeElt, (ClassOrInterfaceDeclaration) member);
      if (elt != null) {
        putIfAbsent(elementsToDecl, elt, member);
      }
    } else if (member instanceof EnumDeclaration) {
      Element elt = findElement(typeElt, (EnumDeclaration) member);
      if (elt != null) {
        putIfAbsent(elementsToDecl, elt, member);
      }
    } else {
      stubDebug(
          String.format("Ignoring element of type %s in %s", member.getClass(), typeDeclName));
    }
  }

  /**
   * Given a method declaration that does not correspond to an element, returns the method it
   * directly overrides or implements. As Java does, this prefers a method in a superclass to one in
   * an interface.
   *
   * <p>As with regular overrides, the parameter types must be exact matches; contravariance is not
   * permitted.
   *
   * @param typeElt the type in which the method appears
   * @param methodDecl the method declaration that does not correspond to an element
   * @return the methods that the given method declaration would override, or null if none
   */
  private @Nullable ExecutableElement fakeOverriddenMethod(
      TypeElement typeElt, MethodDeclaration methodDecl) {
    for (Element elt : typeElt.getEnclosedElements()) {
      if (elt.getKind() != ElementKind.METHOD) {
        continue;
      }
      ExecutableElement candidate = (ExecutableElement) elt;
      if (!candidate.getSimpleName().contentEquals(methodDecl.getName().getIdentifier())) {
        continue;
      }
      List<? extends VariableElement> candidateParams = candidate.getParameters();
      if (sameTypes(candidateParams, methodDecl.getParameters())) {
        return candidate;
      }
    }

    TypeElement superType = ElementUtils.getSuperClass(typeElt);
    if (superType != null) {
      ExecutableElement result = fakeOverriddenMethod(superType, methodDecl);
      if (result != null) {
        return result;
      }
    }

    for (TypeMirror interfaceTypeMirror : typeElt.getInterfaces()) {
      TypeElement interfaceElement = (TypeElement) ((DeclaredType) interfaceTypeMirror).asElement();
      ExecutableElement result = fakeOverriddenMethod(interfaceElement, methodDecl);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  /**
   * Returns true if the two signatures (represented as lists of formal parameters) are the same. No
   * contravariance is permitted.
   *
   * @param javacParams parameter list in javac form
   * @param javaParserParams parameter list in JavaParser form
   * @return true if the two signatures are the same
   */
  private boolean sameTypes(
      List<? extends VariableElement> javacParams, NodeList<Parameter> javaParserParams) {
    if (javacParams.size() != javaParserParams.size()) {
      return false;
    }
    for (int i = 0; i < javacParams.size(); i++) {
      TypeMirror javacType = javacParams.get(i).asType();
      Parameter javaParserParam = javaParserParams.get(i);
      Type javaParserType = javaParserParam.getType();
      if (javacType.getKind() == TypeKind.TYPEVAR) {
        // TODO: Hack, need to viewpoint-adapt.
        javacType = ((TypeVariable) javacType).getUpperBound();
      }
      if (!sameType(javacType, javaParserType)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the two types are the same.
   *
   * @param javacType type in javac form
   * @param javaParserType type in JavaParser form
   * @return true if the two types are the same
   */
  private boolean sameType(TypeMirror javacType, Type javaParserType) {

    switch (javacType.getKind()) {
      case BOOLEAN:
        return javaParserType.equals(PrimitiveType.booleanType());
      case BYTE:
        return javaParserType.equals(PrimitiveType.byteType());
      case CHAR:
        return javaParserType.equals(PrimitiveType.charType());
      case DOUBLE:
        return javaParserType.equals(PrimitiveType.doubleType());
      case FLOAT:
        return javaParserType.equals(PrimitiveType.floatType());
      case INT:
        return javaParserType.equals(PrimitiveType.intType());
      case LONG:
        return javaParserType.equals(PrimitiveType.longType());
      case SHORT:
        return javaParserType.equals(PrimitiveType.shortType());

      case DECLARED:
      case TYPEVAR:
        if (!(javaParserType instanceof ClassOrInterfaceType)) {
          return false;
        }
        com.sun.tools.javac.code.Type javacTypeInternal = (com.sun.tools.javac.code.Type) javacType;
        ClassOrInterfaceType javaParserClassType = (ClassOrInterfaceType) javaParserType;

        // Use asString() because toString() includes annotations.
        String javaParserString = javaParserClassType.asString();
        Element javacElement = javacTypeInternal.asElement();
        // Check both fully-qualified name and simple name.
        return javacElement.toString().equals(javaParserString)
            || javacElement.getSimpleName().contentEquals(javaParserString);

      case ARRAY:
        return javaParserType.isArrayType()
            && sameType(
                ((ArrayType) javacType).getComponentType(),
                javaParserType.asArrayType().getComponentType());

      default:
        throw new BugInCF("Unhandled type %s of kind %s", javacType, javacType.getKind());
    }
  }

  /**
   * Process a fake override: copy its annotations to the fake overrides part of {@code
   * #annotationFileAnnos}.
   *
   * @param element a real element
   * @param decl a fake override of the element
   * @param fakeLocation where the fake override was defined
   */
  private void processFakeOverride(
      ExecutableElement element, CallableDeclaration<?> decl, TypeElement fakeLocation) {
    // This is a fresh type, which this code may side-effect.
    AnnotatedExecutableType methodType = atypeFactory.getAnnotatedType(element);

    // Here is a hacky solution that does not use the visitor.  It just handles the return type.
    // TODO: Walk the type and the declaration, copying annotations from the declaration to the
    // element.  I think PR #3977 has a visitor that does that, which I should use after it is
    // merged.

    // The annotations on the method.  These include type annotations on the return type.
    NodeList<AnnotationExpr> annotations = decl.getAnnotations();
    annotate(methodType.getReturnType(), ((MethodDeclaration) decl).getType(), annotations, decl);

    List<Pair<TypeMirror, AnnotatedTypeMirror>> l =
        annotationFileAnnos.fakeOverrides.computeIfAbsent(element, __ -> new ArrayList<>());
    l.add(Pair.of(fakeLocation.asType(), methodType));
  }

  /**
   * Return the annotated type corresponding to {@code type}, or null if none exists. More
   * specifically, returns the element of {@code types} whose name matches {@code type}.
   *
   * @param type the type to search for
   * @param types the list of AnnotatedDeclaredTypes to search in
   * @param astNode where to report errors
   * @return the annotated type in {@code types} corresponding to {@code type}, or null if none
   *     exists
   */
  private @Nullable AnnotatedDeclaredType findAnnotatedType(
      ClassOrInterfaceType type, List<AnnotatedDeclaredType> types, NodeWithRange<?> astNode) {
    String typeString = type.getNameAsString();
    for (AnnotatedDeclaredType supertype : types) {
      if (supertype.getUnderlyingType().asElement().getSimpleName().contentEquals(typeString)) {
        return supertype;
      }
    }
    stubWarnNotFound(astNode, "Supertype " + typeString + " not found");
    if (debugAnnotationFileParser) {
      stubDebug("Supertypes that were searched:");
      for (AnnotatedDeclaredType supertype : types) {
        stubDebug(String.format("  %s", supertype));
      }
    }
    return null;
  }

  /**
   * Looks for the nested type element in the typeElt and returns it if the element has the same
   * name as provided class or interface declaration. In case nested element is not found it returns
   * null.
   *
   * @param typeElt an element where nested type element should be looked for
   * @param ciDecl class or interface declaration which name should be found among nested elements
   *     of the typeElt
   * @return nested in typeElt element with the name of the class or interface or null if nested
   *     element is not found
   */
  private @Nullable Element findElement(TypeElement typeElt, ClassOrInterfaceDeclaration ciDecl) {
    final String wantedClassOrInterfaceName = ciDecl.getNameAsString();
    for (TypeElement typeElement : ElementUtils.getAllTypeElementsIn(typeElt)) {
      if (wantedClassOrInterfaceName.equals(typeElement.getSimpleName().toString())) {
        return typeElement;
      }
    }

    stubWarnNotFound(
        ciDecl, "Class/interface " + wantedClassOrInterfaceName + " not found in type " + typeElt);
    if (debugAnnotationFileParser) {
      stubDebug(String.format("  Here are the type declarations of %s:", typeElt));
      for (TypeElement method : ElementFilter.typesIn(typeElt.getEnclosedElements())) {
        stubDebug(String.format("    %s", method));
      }
    }
    return null;
  }

  /**
   * Looks for the nested enum element in the typeElt and returns it if the element has the same
   * name as provided enum declaration. In case nested element is not found it returns null.
   *
   * @param typeElt an element where nested enum element should be looked for
   * @param enumDecl enum declaration which name should be found among nested elements of the
   *     typeElt
   * @return nested in typeElt enum element with the name of the provided enum or null if nested
   *     element is not found
   */
  private @Nullable Element findElement(TypeElement typeElt, EnumDeclaration enumDecl) {
    final String wantedEnumName = enumDecl.getNameAsString();
    for (TypeElement typeElement : ElementUtils.getAllTypeElementsIn(typeElt)) {
      if (wantedEnumName.equals(typeElement.getSimpleName().toString())) {
        return typeElement;
      }
    }

    stubWarnNotFound(enumDecl, "Enum " + wantedEnumName + " not found in type " + typeElt);
    if (debugAnnotationFileParser) {
      stubDebug(String.format("  Here are the type declarations of %s:", typeElt));
      for (TypeElement method : ElementFilter.typesIn(typeElt.getEnclosedElements())) {
        stubDebug(String.format("    %s", method));
      }
    }
    return null;
  }

  /**
   * Looks for an enum constant element in the typeElt and returns it if the element has the same
   * name as provided. In case enum constant element is not found it returns null.
   *
   * @param typeElt type element where enum constant element should be looked for
   * @param enumConstDecl the declaration of the enum constant
   * @param astNode where to report errors
   * @return enum constant element in typeElt with the provided name or null if enum constant
   *     element is not found
   */
  private @Nullable VariableElement findElement(
      TypeElement typeElt, EnumConstantDeclaration enumConstDecl, NodeWithRange<?> astNode) {
    final String enumConstName = enumConstDecl.getNameAsString();
    return findFieldElement(typeElt, enumConstName, astNode);
  }

  /**
   * Looks for a method element in {@code typeElt} that has the same name and formal parameter types
   * as {@code methodDecl}. Returns null, and possibly issues a warning, if no such method element
   * is found.
   *
   * @param typeElt type element where method element should be looked for
   * @param methodDecl method declaration with signature that should be found among methods in the
   *     typeElt
   * @param noWarn if true, don't issue a warning if the element is not found
   * @return method element in typeElt with the same signature as the provided method declaration or
   *     null if method element is not found
   */
  private @Nullable ExecutableElement findElement(
      TypeElement typeElt, MethodDeclaration methodDecl, boolean noWarn) {
    if (skipNode(methodDecl)) {
      return null;
    }
    final String wantedMethodName = methodDecl.getNameAsString();
    final int wantedMethodParams =
        (methodDecl.getParameters() == null) ? 0 : methodDecl.getParameters().size();
    final String wantedMethodString = AnnotationFileUtil.toString(methodDecl);
    for (ExecutableElement method : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
      if (wantedMethodParams == method.getParameters().size()
          && wantedMethodName.contentEquals(method.getSimpleName().toString())
          && ElementUtils.getSimpleSignature(method).equals(wantedMethodString)) {
        return method;
      }
    }
    if (!noWarn) {
      if (methodDecl.getAccessSpecifier() == AccessSpecifier.PACKAGE_PRIVATE) {
        // This might be a false positive warning.  The stub parser permits a stub file to
        // omit the access specifier, but package-private methods aren't in the TypeElement.
        stubWarnNotFound(
            methodDecl,
            "Package-private method "
                + wantedMethodString
                + " not found in type "
                + typeElt
                + System.lineSeparator()
                + "If the method is not package-private, add an access specifier in the stub file"
                + " and use pass -AstubDebug to receive a more useful error message.");
      } else {
        stubWarnNotFound(
            methodDecl, "Method " + wantedMethodString + " not found in type " + typeElt);
        if (debugAnnotationFileParser) {
          stubDebug(String.format("  Here are the methods of %s:", typeElt));
          for (ExecutableElement method : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
            stubDebug(String.format("    %s", method));
          }
        }
      }
    }
    return null;
  }

  /**
   * Looks for a constructor element in the typeElt and returns it if the element has the same
   * signature as provided constructor declaration. In case constructor element is not found it
   * returns null.
   *
   * @param typeElt type element where constructor element should be looked for
   * @param constructorDecl constructor declaration with signature that should be found among
   *     constructors in the typeElt
   * @return constructor element in typeElt with the same signature as the provided constructor
   *     declaration or null if constructor element is not found
   */
  private @Nullable ExecutableElement findElement(
      TypeElement typeElt, ConstructorDeclaration constructorDecl) {
    if (skipNode(constructorDecl)) {
      return null;
    }
    final int wantedMethodParams =
        (constructorDecl.getParameters() == null) ? 0 : constructorDecl.getParameters().size();
    final String wantedMethodString = AnnotationFileUtil.toString(constructorDecl);
    for (ExecutableElement method : ElementFilter.constructorsIn(typeElt.getEnclosedElements())) {
      if (wantedMethodParams == method.getParameters().size()
          && ElementUtils.getSimpleSignature(method).equals(wantedMethodString)) {
        return method;
      }
    }

    stubWarnNotFound(
        constructorDecl, "Constructor " + wantedMethodString + " not found in type " + typeElt);
    if (debugAnnotationFileParser) {
      for (ExecutableElement method : ElementFilter.constructorsIn(typeElt.getEnclosedElements())) {
        stubDebug(String.format("  %s", method));
      }
    }
    return null;
  }

  /**
   * Returns the element for the given variable.
   *
   * @param typeElt the type in which the variable is contained
   * @param variable the variable whose element to return
   * @return the element for the given variable
   */
  private VariableElement findElement(TypeElement typeElt, VariableDeclarator variable) {
    final String fieldName = variable.getNameAsString();
    return findFieldElement(typeElt, fieldName, variable);
  }

  /**
   * Looks for a field element in the typeElt and returns it if the element has the same name as
   * provided. In case field element is not found it returns null.
   *
   * @param typeElt type element where field element should be looked for
   * @param fieldName field name that should be found
   * @param astNode where to report errors
   * @return field element in typeElt with the provided name or null if field element is not found
   */
  private @Nullable VariableElement findFieldElement(
      TypeElement typeElt, String fieldName, NodeWithRange<?> astNode) {
    for (VariableElement field : ElementUtils.getAllFieldsIn(typeElt, elements)) {
      // field.getSimpleName() is a CharSequence, not a String
      if (fieldName.equals(field.getSimpleName().toString())) {
        return field;
      }
    }

    stubWarnNotFound(astNode, "Field " + fieldName + " not found in type " + typeElt);
    if (debugAnnotationFileParser) {
      for (VariableElement field : ElementFilter.fieldsIn(typeElt.getEnclosedElements())) {
        stubDebug(String.format("  %s", field));
      }
    }
    return null;
  }

  /**
   * Given a fully-qualified type name, return a TypeElement for it, or null if none exists. Also
   * cache in importedTypes.
   *
   * @param name a fully-qualified type name
   * @return a TypeElement for the name, or null
   */
  private @Nullable TypeElement getTypeElementOrNull(@FullyQualifiedName String name) {
    TypeElement typeElement = elements.getTypeElement(name);
    if (typeElement != null) {
      importedTypes.put(name, typeElement);
    }
    // for debugging: warn("getTypeElementOrNull(%s) => %s", name, typeElement);
    return typeElement;
  }

  /**
   * Get the type element for the given fully-qualified type name. If none is found, issue a warning
   * and return null.
   *
   * @param typeName a type name
   * @param msg a warning message to issue if the type element for {@code typeName} cannot be found
   * @param astNode where to report errors
   * @return the type element for the given fully-qualified type name, or null
   */
  private TypeElement getTypeElement(
      @FullyQualifiedName String typeName, String msg, NodeWithRange<?> astNode) {
    TypeElement classElement = elements.getTypeElement(typeName);
    if (classElement == null) {
      stubWarnNotFound(astNode, msg + ": " + typeName);
    }
    return classElement;
  }

  /**
   * Returns the element for the given package.
   *
   * @param packageName the package's name
   * @param astNode where to report errors
   * @return the element for the given package
   */
  private PackageElement findPackage(String packageName, NodeWithRange<?> astNode) {
    PackageElement packageElement = elements.getPackageElement(packageName);
    if (packageElement == null) {
      stubWarnNotFound(astNode, "Imported package not found: " + packageName);
    }
    return packageElement;
  }

  /**
   * Returns true if one of the annotations is {@link AnnotatedFor} and this checker is in its list
   * of checkers. If none of the annotations are {@code AnnotatedFor}, then also return true.
   *
   * @param annotations a list of JavaParser annotations
   * @return true if one of the annotations is {@link AnnotatedFor} and its list of checkers does
   *     not contain this checker
   */
  private boolean isAnnotatedForThisChecker(List<AnnotationExpr> annotations) {
    if (isJdkAsStub) {
      // The JDK stubs have purity annotations that should be read for all checkers.
      // TODO: Parse the JDK stubs, but only save the declaration annotations.
      return true;
    }
    for (AnnotationExpr ae : annotations) {
      if (ae.getNameAsString().equals("AnnotatedFor")
          || ae.getNameAsString().equals("org.checkerframework.framework.qual.AnnotatedFor")) {
        AnnotationMirror af = getAnnotation(ae, allAnnotations);
        if (atypeFactory.areSameByClass(af, AnnotatedFor.class)) {
          return atypeFactory.doesAnnotatedForApplyToThisChecker(af);
        }
      }
    }
    return true;
  }

  /**
   * Convert {@code annotation} into an AnnotationMirror. Returns null if the annotation isn't
   * supported by the checker or if some error occurred while converting it.
   *
   * @param annotation syntax tree for an annotation
   * @param allAnnotations map from simple name to annotation definition; side-effected by this
   *     method
   * @return the AnnotationMirror for the annotation, or null if it cannot be built
   */
  private @Nullable AnnotationMirror getAnnotation(
      AnnotationExpr annotation, Map<String, TypeElement> allAnnotations) {

    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
    @FullyQualifiedName String annoNameFq = annotation.getNameAsString();
    TypeElement annoTypeElt = allAnnotations.get(annoNameFq);
    if (annoTypeElt == null) {
      // If the annotation was not imported, then #getAllAnnotations did not add it to the
      // allAnnotations field. This code adds the annotation when it is encountered (i.e. here).
      // Note that this does not call AnnotationFileParser#getTypeElement to avoid a spurious
      // diagnostic if the annotation is actually unknown.
      annoTypeElt = elements.getTypeElement(annoNameFq);
      if (annoTypeElt == null) {
        // Not a supported annotation -> ignore
        return null;
      }
      putAllNew(allAnnotations, createNameToAnnotationMap(Collections.singletonList(annoTypeElt)));
    }
    @SuppressWarnings("signature") // not anonymous, so name is not empty
    @CanonicalName String annoName = annoTypeElt.getQualifiedName().toString();

    if (annotation instanceof MarkerAnnotationExpr) {
      return AnnotationBuilder.fromName(elements, annoName);
    } else if (annotation instanceof NormalAnnotationExpr) {
      NormalAnnotationExpr nrmanno = (NormalAnnotationExpr) annotation;
      AnnotationBuilder builder = new AnnotationBuilder(processingEnv, annoName);
      List<MemberValuePair> pairs = nrmanno.getPairs();
      if (pairs != null) {
        for (MemberValuePair mvp : pairs) {
          String member = mvp.getNameAsString();
          Expression exp = mvp.getValue();
          try {
            builderAddElement(builder, member, exp);
          } catch (AnnotationFileParserException e) {
            warn(
                exp,
                "For annotation %s, could not add  %s=%s  because %s",
                annotation,
                member,
                exp,
                e.getMessage());
            return null;
          }
        }
      }
      return builder.build();
    } else if (annotation instanceof SingleMemberAnnotationExpr) {
      SingleMemberAnnotationExpr sglanno = (SingleMemberAnnotationExpr) annotation;
      AnnotationBuilder builder = new AnnotationBuilder(processingEnv, annoName);
      Expression valExpr = sglanno.getMemberValue();
      try {
        builderAddElement(builder, "value", valExpr);
      } catch (AnnotationFileParserException e) {
        warn(
            valExpr,
            "For annotation %s, could not add  value=%s  because %s",
            annotation,
            valExpr,
            e.getMessage());
        return null;
      }
      return builder.build();
    } else {
      throw new BugInCF("AnnotationFileParser: unknown annotation type: " + annotation);
    }
  }

  /**
   * Returns the value of {@code expr}.
   *
   * @param name the name of an annotation element/argument, used for diagnostic messages
   * @param expr the expression to determine the value of
   * @param valueKind the type of the result
   * @return the value of {@code expr}
   * @throws AnnotationFileParserException if a problem occurred getting the value
   */
  private Object getValueOfExpressionInAnnotation(String name, Expression expr, TypeKind valueKind)
      throws AnnotationFileParserException {
    if (expr instanceof FieldAccessExpr || expr instanceof NameExpr) {
      VariableElement elem;
      if (expr instanceof NameExpr) {
        elem = findVariableElement((NameExpr) expr);
      } else {
        elem = findVariableElement((FieldAccessExpr) expr);
      }
      if (elem == null) {
        throw new AnnotationFileParserException(String.format("variable %s not found", expr));
      }
      Object value = elem.getConstantValue() != null ? elem.getConstantValue() : elem;
      if (value instanceof Number) {
        return convert((Number) value, valueKind);
      } else {
        return value;
      }
    } else if (expr instanceof StringLiteralExpr) {
      return ((StringLiteralExpr) expr).asString();
    } else if (expr instanceof BooleanLiteralExpr) {
      return ((BooleanLiteralExpr) expr).getValue();
    } else if (expr instanceof CharLiteralExpr) {
      return convert((int) ((CharLiteralExpr) expr).asChar(), valueKind);
    } else if (expr instanceof DoubleLiteralExpr) {
      // No conversion needed if the expression is a double, the annotation value must be a
      // double, too.
      return ((DoubleLiteralExpr) expr).asDouble();
    } else if (expr instanceof IntegerLiteralExpr) {
      return convert(((IntegerLiteralExpr) expr).asNumber(), valueKind);
    } else if (expr instanceof LongLiteralExpr) {
      return convert(((LongLiteralExpr) expr).asNumber(), valueKind);
    } else if (expr instanceof UnaryExpr) {
      switch (expr.toString()) {
          // Special-case the minimum values.  Separately parsing a "-" and a value
          // doesn't correctly handle the minimum values, because the absolute value of
          // the smallest member of an integral type is larger than the largest value.
        case "-9223372036854775808L":
        case "-9223372036854775808l":
          return convert(Long.MIN_VALUE, valueKind, false);
        case "-2147483648":
          return convert(Integer.MIN_VALUE, valueKind, false);
        default:
          if (((UnaryExpr) expr).getOperator() == UnaryExpr.Operator.MINUS) {
            Object value =
                getValueOfExpressionInAnnotation(
                    name, ((UnaryExpr) expr).getExpression(), valueKind);
            if (value instanceof Number) {
              return convert((Number) value, valueKind, true);
            }
          }
          throw new AnnotationFileParserException(
              "unexpected Unary annotation expression: " + expr);
      }
    } else if (expr instanceof ClassExpr) {
      ClassExpr classExpr = (ClassExpr) expr;
      @SuppressWarnings("signature") // Type.toString(): @FullyQualifiedName
      @FullyQualifiedName String className = classExpr.getType().toString();
      if (importedTypes.containsKey(className)) {
        return importedTypes.get(className).asType();
      }
      TypeElement typeElement = findTypeOfName(className);
      if (typeElement == null) {
        throw new AnnotationFileParserException("unknown class name " + className);
      }

      return typeElement.asType();
    } else if (expr instanceof NullLiteralExpr) {
      throw new AnnotationFileParserException("Illegal annotation value null, for " + name);
    } else {
      throw new AnnotationFileParserException("Unexpected annotation expression: " + expr);
    }
  }

  /**
   * Returns the TypeElement with the name {@code name}, if one exists. Otherwise, checks the class
   * and package of {@code typeBeingParsed} for a class named {@code name}.
   *
   * @param name classname (simple, or Outer.Inner, or fully-qualified)
   * @return the TypeElement for {@code name}, or null if not found
   */
  @SuppressWarnings("signature:argument") // string concatenation
  private @Nullable TypeElement findTypeOfName(@FullyQualifiedName String name) {
    String packageName = typeBeingParsed.packageName;
    String packagePrefix = (packageName == null) ? "" : packageName + ".";

    // warn("findTypeOfName(%s), typeBeingParsed %s %s", name, packageName, enclosingClass);

    // As soon as typeElement is set to a non-null value, it will be returned.
    TypeElement typeElement = getTypeElementOrNull(name);
    if (typeElement == null && packageName != null) {
      typeElement = getTypeElementOrNull(packagePrefix + name);
    }
    String enclosingClass = typeBeingParsed.className;
    while (typeElement == null && enclosingClass != null) {
      typeElement = getTypeElementOrNull(packagePrefix + enclosingClass + "." + name);
      int lastDot = enclosingClass.lastIndexOf('.');
      if (lastDot == -1) {
        break;
      } else {
        enclosingClass = enclosingClass.substring(0, lastDot);
      }
    }
    if (typeElement == null && !"java.lang".equals(packageName)) {
      typeElement = getTypeElementOrNull("java.lang." + name);
    }
    return typeElement;
  }

  /**
   * Converts {@code number} to {@code expectedKind}.
   *
   * <pre><code>
   * &nbsp; @interface Anno { long value(); }
   * &nbsp; @Anno(1)
   * </code></pre>
   *
   * To properly build @Anno, the IntegerLiteralExpr "1" must be converted from an int to a long.
   */
  private Object convert(Number number, TypeKind expectedKind) {
    return convert(number, expectedKind, false);
  }

  /**
   * Converts {@code number} to {@code expectedKind}. The value converted is multiplied by -1 if
   * {@code negate} is true
   *
   * @param number a Number value to be converted
   * @param expectedKind one of type {byte, short, int, long, char, float, double}
   * @param negate whether to negate the value of the Number Object while converting
   * @return the converted Object
   */
  private Object convert(Number number, TypeKind expectedKind, boolean negate) {
    byte scalefactor = (byte) (negate ? -1 : 1);
    switch (expectedKind) {
      case BYTE:
        return number.byteValue() * scalefactor;
      case SHORT:
        return number.shortValue() * scalefactor;
      case INT:
        return number.intValue() * scalefactor;
      case LONG:
        return number.longValue() * scalefactor;
      case CHAR:
        // It's not possible for `number` to be negative when `expectedkind` is a CHAR, and
        // casting a negative value to char is illegal.
        if (negate) {
          throw new BugInCF(
              "convert(%s, %s, %s): can't negate a char", number, expectedKind, negate);
        }
        return (char) number.intValue();
      case FLOAT:
        return number.floatValue() * scalefactor;
      case DOUBLE:
        return number.doubleValue() * scalefactor;
      default:
        throw new BugInCF("Unexpected expectedKind: " + expectedKind);
    }
  }

  /**
   * Adds an annotation element (argument) to {@code builder}. The element is a Java expression.
   *
   * @param builder the builder to side-effect
   * @param name the element name
   * @param expr the element value
   * @throws AnnotationFileParserException if the expression cannot be parsed and added to {@code
   *     builder}
   */
  private void builderAddElement(AnnotationBuilder builder, String name, Expression expr)
      throws AnnotationFileParserException {
    ExecutableElement var = builder.findElement(name);
    TypeMirror declaredType = var.getReturnType();
    TypeKind valueKind;
    if (declaredType.getKind() == TypeKind.ARRAY) {
      valueKind = ((ArrayType) declaredType).getComponentType().getKind();
    } else {
      valueKind = declaredType.getKind();
    }
    if (expr instanceof ArrayInitializerExpr) {
      if (declaredType.getKind() != TypeKind.ARRAY) {
        throw new AnnotationFileParserException(
            "unhandled annotation attribute type: " + expr + " and declaredType: " + declaredType);
      }

      List<Expression> arrayExpressions = ((ArrayInitializerExpr) expr).getValues();
      Object[] values = new Object[arrayExpressions.size()];

      for (int i = 0; i < arrayExpressions.size(); ++i) {
        Expression eltExpr = arrayExpressions.get(i);
        values[i] = getValueOfExpressionInAnnotation(name, eltExpr, valueKind);
      }
      builder.setValue(name, values);
    } else {
      Object value = getValueOfExpressionInAnnotation(name, expr, valueKind);
      if (declaredType.getKind() == TypeKind.ARRAY) {
        Object[] valueArray = {value};
        builder.setValue(name, valueArray);
      } else {
        builderSetValue(builder, name, value);
      }
    }
  }

  /**
   * Cast to non-array values so that correct the correct AnnotationBuilder#setValue method is
   * called. (Different types of values are handled differently.)
   *
   * @param builder the builder to side-effect
   * @param name the element name
   * @param value the element value
   */
  private void builderSetValue(AnnotationBuilder builder, String name, Object value) {
    if (value instanceof Boolean) {
      builder.setValue(name, (Boolean) value);
    } else if (value instanceof Character) {
      builder.setValue(name, (Character) value);
    } else if (value instanceof Class<?>) {
      builder.setValue(name, (Class<?>) value);
    } else if (value instanceof Double) {
      builder.setValue(name, (Double) value);
    } else if (value instanceof Enum<?>) {
      builder.setValue(name, (Enum<?>) value);
    } else if (value instanceof Float) {
      builder.setValue(name, (Float) value);
    } else if (value instanceof Integer) {
      builder.setValue(name, (Integer) value);
    } else if (value instanceof Long) {
      builder.setValue(name, (Long) value);
    } else if (value instanceof Short) {
      builder.setValue(name, (Short) value);
    } else if (value instanceof String) {
      builder.setValue(name, (String) value);
    } else if (value instanceof TypeMirror) {
      builder.setValue(name, (TypeMirror) value);
    } else if (value instanceof VariableElement) {
      builder.setValue(name, (VariableElement) value);
    } else {
      throw new BugInCF("Unexpected builder value: %s", value);
    }
  }

  /**
   * Mapping of a name access expression that has already been encountered to the resolved variable
   * element.
   */
  private final Map<NameExpr, VariableElement> findVariableElementNameCache = new HashMap<>();

  /**
   * Returns the element for the given variable.
   *
   * @param nexpr the variable name
   * @return the element for the given variable
   */
  private @Nullable VariableElement findVariableElement(NameExpr nexpr) {
    if (findVariableElementNameCache.containsKey(nexpr)) {
      return findVariableElementNameCache.get(nexpr);
    }

    VariableElement res = null;
    boolean importFound = false;
    for (String imp : importedConstants) {
      Pair<@FullyQualifiedName String, String> partitionedName =
          AnnotationFileUtil.partitionQualifiedName(imp);
      String typeName = partitionedName.first;
      String fieldName = partitionedName.second;
      if (fieldName.equals(nexpr.getNameAsString())) {
        TypeElement enclType =
            getTypeElement(
                typeName,
                String.format("Enclosing type of static import %s not found", fieldName),
                nexpr);

        if (enclType == null) {
          return null;
        } else {
          importFound = true;
          res = findFieldElement(enclType, fieldName, nexpr);
          break;
        }
      }
    }

    if (res == null) {
      if (importFound) {
        // TODO: Is this warning redundant?  Maybe imported but invalid types or fields will
        // have warnings from above.
        stubWarnNotFound(nexpr, nexpr.getName() + " was imported but not found");
      } else {
        stubWarnNotFound(nexpr, "Static field " + nexpr.getName() + " is not imported");
      }
    }

    findVariableElementNameCache.put(nexpr, res);
    return res;
  }

  /**
   * Mapping of a field access expression that has already been encountered to the resolved variable
   * element.
   */
  private final Map<FieldAccessExpr, VariableElement> findVariableElementFieldCache =
      new HashMap<>();

  /**
   * Returns the VariableElement for the given field access.
   *
   * @param faexpr a field access expression
   * @return the VariableElement for the given field access
   */
  @SuppressWarnings("signature:argument") // string manipulation
  private @Nullable VariableElement findVariableElement(FieldAccessExpr faexpr) {
    if (findVariableElementFieldCache.containsKey(faexpr)) {
      return findVariableElementFieldCache.get(faexpr);
    }
    TypeElement rcvElt = elements.getTypeElement(faexpr.getScope().toString());
    if (rcvElt == null) {
      // Search importedConstants for full annotation name.
      for (String imp : importedConstants) {
        // TODO: should this use AnnotationFileUtil.partitionQualifiedName?
        String[] importDelimited = imp.split("\\.");
        if (importDelimited[importDelimited.length - 1].equals(faexpr.getScope().toString())) {
          StringBuilder fullAnnotation = new StringBuilder();
          for (int i = 0; i < importDelimited.length - 1; i++) {
            fullAnnotation.append(importDelimited[i]);
            fullAnnotation.append('.');
          }
          fullAnnotation.append(faexpr.getScope().toString());
          rcvElt = elements.getTypeElement(fullAnnotation);
          break;
        }
      }

      if (rcvElt == null) {
        stubWarnNotFound(faexpr, "Type " + faexpr.getScope() + " not found");
        return null;
      }
    }

    VariableElement res = findFieldElement(rcvElt, faexpr.getNameAsString(), faexpr);
    findVariableElementFieldCache.put(faexpr, res);
    return res;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Map utilities
  ///

  /**
   * Just like Map.put, but does not override any existing value in the map.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param m a map
   * @param key a key
   * @param value the value to associate with the key, if the key isn't already in the map
   */
  public static <K, V> void putIfAbsent(Map<K, V> m, K key, V value) {
    if (key == null) {
      throw new BugInCF("AnnotationFileParser: key is null for value " + value);
    }
    if (!m.containsKey(key)) {
      m.put(key, value);
    }
  }

  /**
   * If the key is already in the map, then add the annos to the list. Otherwise put the key and the
   * annos in the map
   */
  private static void putOrAddToMap(
      Map<String, Set<AnnotationMirror>> map, String key, Set<AnnotationMirror> annos) {
    if (map.containsKey(key)) {
      map.get(key).addAll(annos);
    } else {
      map.put(key, new HashSet<>(annos));
    }
  }

  /**
   * Just like Map.put, but modifies an existing annotated type for the given key in {@code m}. If
   * {@code m} already has an annotated type for {@code key}, each annotation in {@code newType}
   * will replace annotations from the same hierarchy at the same location in the existing annotated
   * type. Annotations in other hierarchies will be preserved.
   *
   * @param m the map to put the new type into
   * @param key the key for the map
   * @param newType the new type for the key
   */
  private void putMerge(
      Map<Element, AnnotatedTypeMirror> m, Element key, AnnotatedTypeMirror newType) {
    if (key == null) {
      throw new BugInCF("AnnotationFileParser: key is null");
    }
    if (m.containsKey(key)) {
      AnnotatedTypeMirror existingType = m.get(key);
      // If the newType is from a JDK stub file, then keep the existing type.  This
      // way user-supplied stub files override JDK stub files.
      // This works because the JDK is always parsed last, on demand, after all other stub files.
      if (!isJdkAsStub) {
        atypeFactory.replaceAnnotations(newType, existingType);
      }
      m.put(key, existingType);
    } else {
      m.put(key, newType);
    }
  }

  /**
   * Just like Map.putAll, but modifies existing values using {@link #putIfAbsent(Map, Object,
   * Object)}.
   *
   * @param m the destination map
   * @param m2 the source map
   * @param <K> the key type for the maps
   * @param <V> the value type for the maps
   */
  public static <K, V> void putAllNew(Map<K, V> m, Map<K, V> m2) {
    for (Map.Entry<K, V> e2 : m2.entrySet()) {
      putIfAbsent(m, e2.getKey(), e2.getValue());
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Issue warnings
  ///

  /** The warnings that have been issued so far. */
  private static final Set<String> warnings = new HashSet<>();

  /**
   * Issues the given warning about missing elements, only if it has not been previously issued and
   * the -AstubWarnIfNotFound command-line argument was passed.
   *
   * @param astNode where to report errors
   * @param warning warning to print
   */
  private void stubWarnNotFound(NodeWithRange<?> astNode, String warning) {
    stubWarnNotFound(astNode, warning, warnIfNotFound);
  }

  /**
   * Issues the given warning about missing elements, only if it has not been previously issued and
   * the {@code warnIfNotFound} formal parameter is true.
   *
   * @param astNode where to report errors
   * @param warning warning to print
   * @param warnIfNotFound whether to print warnings about types/members that were not found
   */
  private void stubWarnNotFound(NodeWithRange<?> astNode, String warning, boolean warnIfNotFound) {
    if ((!isBuiltinStub || warnIfNotFound) || debugAnnotationFileParser) {
      warn(astNode, warning);
    }
  }

  /**
   * Issues the given warning about overwriting bytecode, only if it has not been previously issued
   * and the -AstubWarnIfOverwritesBytecode command-line argument was passed.
   *
   * @param astNode where to report errors
   * @param message the warning message to print
   */
  @SuppressWarnings("UnusedMethod") // not currently used
  private void stubWarnOverwritesBytecode(NodeWithRange<?> astNode, String message) {
    if (warnIfStubOverwritesBytecode || debugAnnotationFileParser) {
      warn(astNode, message);
    }
  }

  /**
   * Issues a warning, only if it has not been previously issued.
   *
   * @param astNode where to report errors
   * @param warning a format string
   * @param args the arguments for {@code warning}
   */
  @FormatMethod
  private void warn(@Nullable NodeWithRange<?> astNode, String warning, Object... args) {
    if (!isBuiltinStub) {
      warn(astNode, String.format(warning, args));
    }
  }

  /**
   * Issues a warning, only if it has not been previously issued.
   *
   * @param astNode where to report errors
   * @param warning a warning message
   */
  private void warn(@Nullable NodeWithRange<?> astNode, String warning) {
    if (!isJdkAsStub) {
      if (warnings.add(warning)) {
        processingEnv
            .getMessager()
            .printMessage(stubWarnDiagnosticKind, fileAndLine(astNode) + warning);
      }
    }
  }

  /**
   * If {@code warning} hasn't been printed yet, and {@code debugAnnotationFileParser} is true,
   * prints the given warning as a diagnostic message.
   *
   * @param warning warning to print
   */
  private void stubDebug(String warning) {
    if (debugAnnotationFileParser && warnings.add(warning)) {
      processingEnv
          .getMessager()
          .printMessage(javax.tools.Diagnostic.Kind.NOTE, "AnnotationFileParser: " + warning);
    }
  }

  /**
   * After obtaining the JavaParser AST for an ajava file and the javac tree for its corresponding
   * Java file, walks both in tandem. For each program construct with annotations, stores the
   * annotations from the ajava file in {@link #annotationFileAnnos} by calling the process method
   * corresponding to that construct, such as {@link #processCallableDeclaration} or {@link
   * #processField}.
   */
  private class AjavaAnnotationCollectorVisitor extends DefaultJointVisitor {
    @Override
    public Void visitClass(ClassTree javacTree, Node javaParserNode) {
      List<AnnotatedTypeVariable> typeDeclTypeParameters = null;
      if (javaParserNode instanceof TypeDeclaration<?>
          && !(javaParserNode instanceof AnnotationDeclaration)) {
        typeDeclTypeParameters =
            processTypeDecl((TypeDeclaration<?>) javaParserNode, null, javacTree);
      }

      super.visitClass(javacTree, javaParserNode);
      if (typeDeclTypeParameters != null) {
        typeParameters.removeAll(typeDeclTypeParameters);
      }

      return null;
    }

    @Override
    public Void visitVariable(VariableTree javacTree, Node javaParserNode) {
      if (TreeUtils.elementFromTree(javacTree) != null) {
        VariableElement elt = TreeUtils.elementFromDeclaration(javacTree);
        if (elt != null) {
          if (elt.getKind() == ElementKind.FIELD) {
            processField((FieldDeclaration) javaParserNode.getParentNode().get(), elt);
          }

          if (elt.getKind() == ElementKind.ENUM_CONSTANT) {
            processEnumConstant((EnumConstantDeclaration) javaParserNode, elt);
          }
        }
      }

      super.visitVariable(javacTree, javaParserNode);
      return null;
    }

    @Override
    public Void visitMethod(MethodTree javacTree, Node javaParserNode) {
      ExecutableElement elt = TreeUtils.elementFromDeclaration(javacTree);
      List<AnnotatedTypeVariable> variablesToClear = null;
      if (javaParserNode instanceof CallableDeclaration<?>) {
        variablesToClear = processCallableDeclaration((CallableDeclaration<?>) javaParserNode, elt);
      }

      super.visitMethod(javacTree, javaParserNode);
      if (variablesToClear != null) {
        typeParameters.removeAll(variablesToClear);
      }

      return null;
    }
  }

  /**
   * Return the prefix for a warning line: A file name, line number, and column number.
   *
   * @param astNode where to report errors
   * @return file name, line number, and column number
   */
  private String fileAndLine(NodeWithRange<?> astNode) {
    String filenamePrinted =
        (processingEnv.getOptions().containsKey("nomsgtext")
            ? new File(filename).getName()
            : filename);

    Optional<Position> begin = astNode == null ? Optional.empty() : astNode.getBegin();
    String lineAndColumn = (begin.isPresent() ? begin.get() + ":" : "");
    return filenamePrinted + ":" + lineAndColumn + " ";
  }

  /** An exception indicating a problem while parsing an annotation file. */
  public static class AnnotationFileParserException extends Exception {

    private static final long serialVersionUID = 20201222;

    /**
     * Create a new AnnotationFileParserException.
     *
     * @param message a description of the problem
     */
    AnnotationFileParserException(String message) {
      super(message);
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Parse state
  ///

  /** Represents a class: its package name and name (including outer class names if any). */
  private static class FqName {
    /** Name of the package being parsed, or null. */
    public @Nullable String packageName;

    /**
     * Name of the type being parsed. Includes outer class names if any. Null if the parser has
     * parsed a package declaration but has not yet gotten to a type declaration.
     */
    public @Nullable String className;

    /**
     * Create a new FqName, which represents a class.
     *
     * @param packageName name of the package, or null
     * @param className unqualified name of the type, including outer class names if any. May be
     *     null.
     */
    public FqName(String packageName, @Nullable String className) {
      this.packageName = packageName;
      this.className = className;
    }

    /** Fully-qualified name of the class. */
    @Override
    @SuppressWarnings("signature") // string concatenation
    public @FullyQualifiedName String toString() {
      if (packageName == null) {
        return className;
      } else {
        return packageName + "." + className;
      }
    }
  }
}
