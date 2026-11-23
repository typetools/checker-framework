package org.checkerframework.framework.stub;

import com.github.javaparser.ParseException;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
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
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.ADeclaration;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.el.BoundLocation;
import org.checkerframework.afu.scenelib.el.DefException;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.io.IndexFileParser;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.framework.util.StaticJavaParserUtil;
import org.checkerframework.javacutil.BugInCF;

/**
 * Converts a JAIF file plus a stub file into index files (JAIFs). Note that the resulting index
 * files will not include annotation definitions, for which stubfiles do not generally provide
 * complete information.
 *
 * <p>An instance of the class represents conversion of 1 stub file, but the static {@link
 * #main(String[])} method converts multiple stub files, instantiating the class multiple times.
 */
public class ToIndexFileConverter extends GenericVisitorAdapter<Void, AElement> {

  // The asm library is not on this class's compile classpath, so the following three constants
  // are written literally rather than as references to fields of org.objectweb.asm.TypePath.

  /**
   * The {@code step} value of a {@link TypePathEntry} that steps from an array type to its
   * component type; that is, {@code org.objectweb.asm.TypePath.ARRAY_ELEMENT}.
   */
  private static final int ARRAY_ELEMENT = 0;

  /**
   * The {@code step} value of a {@link TypePathEntry} that steps from a wildcard to its bound; that
   * is, {@code org.objectweb.asm.TypePath.WILDCARD_BOUND}.
   */
  private static final int WILDCARD_BOUND = 2;

  /**
   * The {@code step} value of a {@link TypePathEntry} that steps from a parameterized type to one
   * of its type arguments; that is, {@code org.objectweb.asm.TypePath.TYPE_ARGUMENT}.
   */
  private static final int TYPE_ARGUMENT = 3;

  /**
   * The package and imports that determine the meaning of a type name written in a node that is not
   * part of any compilation unit, such as a node that a test creates.
   */
  private final ImportContext importContext;

  /**
   * The package and the imports of a compilation unit, which determine the meaning of a simple or
   * partially qualified type name written in it. In Java, imports apply only within the compilation
   * unit that contains them (JLS 7.5). In a stub file, however, the stub parser gives every
   * compilation unit all of the stub file's imports, so an import applies throughout the stub file.
   *
   * @param pkgName the package name, or null for the unnamed package
   * @param singleTypeImports the types that single-type imports import, such as {@code
   *     java.util.List} for {@code import java.util.List;}
   * @param onDemandImports the packages and types that imports-on-demand import from, such as
   *     {@code java.util} for {@code import java.util.*;}
   */
  private record ImportContext(
      @Nullable @DotSeparatedIdentifiers String pkgName,
      List<String> singleTypeImports,
      List<String> onDemandImports) {

    /**
     * Creates the import context of a compilation unit.
     *
     * @param pkgDecl the package declaration, or null if there is none
     * @param importDecls the import declarations, or null if there are none
     * @return the import context
     */
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameAsString
    static ImportContext of(
        @Nullable PackageDeclaration pkgDecl, @Nullable List<ImportDeclaration> importDecls) {
      String pkgName = pkgDecl == null ? null : pkgDecl.getNameAsString();
      if (importDecls == null) {
        return new ImportContext(pkgName, Collections.emptyList(), Collections.emptyList());
      }
      ArrayList<String> singles = new ArrayList<>(importDecls.size());
      ArrayList<String> onDemands = new ArrayList<>(importDecls.size());
      for (ImportDeclaration decl : importDecls) {
        // A static import imports members, not types, and a module import is not used in a stub
        // file.
        if (!decl.isStatic() && !decl.isModule()) {
          (decl.isAsterisk() ? onDemands : singles).add(decl.getNameAsString());
        }
      }
      singles.trimToSize();
      onDemands.trimToSize();
      return new ImportContext(
          pkgName, Collections.unmodifiableList(singles), Collections.unmodifiableList(onDemands));
    }

    /**
     * Creates the import context of a compilation unit.
     *
     * @param cu a compilation unit
     * @return the import context of {@code cu}
     */
    static ImportContext of(CompilationUnit cu) {
      return of(cu.getPackageDeclaration().orElse(null), cu.getImports());
    }
  }

  /**
   * A cache of the import contexts of the stub file's compilation units. It is an identity map
   * because a JavaParser node's {@code equals} and {@code hashCode} compare its whole subtree.
   */
  private final IdentityHashMap<CompilationUnit, ImportContext> importContexts =
      new IdentityHashMap<>();

  /**
   * A scene that is read from the input JAIF file and that will be written to the output JAIF file.
   */
  private final AScene scene;

  /**
   * Maps a package name, such as {@code java.util}, to the compilation units of the stub file that
   * declare that package. A stub file has one compilation unit per {@code package} declaration, so
   * different compilation units can declare types in different packages. It omits the compilation
   * units in the unnamed package, because a type in the unnamed package cannot be referred to from
   * another package. It is empty for a converter that the public constructor creates, which knows
   * only the compilation unit in which each name is written.
   */
  private final Map<String, List<CompilationUnit>> compilationUnitsByPackage = new HashMap<>();

  /**
   * A cache for {@link #getTargets}. Maps the binary name of an annotation interface to the element
   * types in its {@code @Target} meta-annotation, or to null if they cannot be determined.
   */
  private final Map<String, @Nullable List<ElementType>> getTargetsCache = new HashMap<>();

  /**
   * A cache for {@link #resolve}. Maps a compilation unit of the stub file, and a class name as
   * written in it, to the binary name that the class name identifies, or to null if resolution
   * fails. The compilation unit is part of the key because its package and imports affect the
   * result. It is an identity map because a JavaParser node's {@code equals} and {@code hashCode}
   * compare its whole subtree.
   */
  private final IdentityHashMap<CompilationUnit, Map<String, @Nullable @BinaryName String>>
      resolveCache = new IdentityHashMap<>();

  /**
   * A cache for {@link #memberTypeDeclaration}. Maps a type declaration of the stub file to a map
   * from the simple name of each of its member types to that member type's declaration. It is an
   * identity map because a JavaParser node's {@code equals} and {@code hashCode} compare its whole
   * subtree.
   */
  private final IdentityHashMap<TypeDeclaration<?>, Map<String, TypeDeclaration<?>>>
      memberTypeDeclarationCache = new IdentityHashMap<>();

  /**
   * The type declarations of the stub file whose inherited member types are being sought by {@link
   * #inheritedMemberType}. It prevents infinite recursion on a malformed stub file whose
   * inheritance is cyclic.
   */
  private final Set<TypeDeclaration<?>> inheritanceVisiting =
      Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * The type declarations of the stub file whose classpath counterparts' member types are being
   * sought by {@link #classpathMemberClass}. It prevents infinite recursion on a malformed stub
   * file whose inheritance is cyclic.
   */
  private final Set<TypeDeclaration<?>> classpathInheritanceVisiting =
      Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * True if, during the current call to {@link #inheritedMemberType} or {@link
   * #classpathMemberClass}, cyclic inheritance cut a search short because {@link
   * #inheritanceVisiting} or {@link #classpathInheritanceVisiting} already contained a type. The
   * result of such a call is not cached, unless the call is the outermost one.
   */
  private boolean inheritanceCycleCut = false;

  /**
   * A cache for {@link #inheritedMemberType}. Maps a type declaration of the stub file to a map
   * from the simple name of a member type to the result of {@link #inheritedMemberType}.
   */
  private final IdentityHashMap<TypeDeclaration<?>, Map<String, @Nullable Object>>
      inheritedMemberTypesCache = new IdentityHashMap<>();

  /**
   * The arguments of a call to {@link #classpathMemberClass}, other than the type declaration.
   *
   * @param identifier the simple name of a member type
   * @param inherited true if the type declaration is a supertype of the class whose members are
   *     sought
   * @param packageName the package of the stub declaration whose members are sought, or the empty
   *     string for the unnamed package
   */
  private record ClasspathMemberQuery(String identifier, boolean inherited, String packageName) {}

  /**
   * A cache for {@link #classpathMemberClass}. Maps a type declaration of the stub file, and the
   * other arguments of a call to {@link #classpathMemberClass}, to the result of the call. It is an
   * identity map because a JavaParser node's {@code equals} and {@code hashCode} compare its whole
   * subtree.
   */
  private final IdentityHashMap<TypeDeclaration<?>, Map<ClasspathMemberQuery, @Nullable Class<?>>>
      classpathMemberClassCache = new IdentityHashMap<>();

  /**
   * Creates a new ToIndexFileConverter for one compilation unit of a stub file. The converter does
   * not consider the types that other compilation units of the stub file declare.
   *
   * @param pkgDecl the AST node for the package declaration
   * @param importDecls the AST nodes for import declarations
   * @param scene scene for visitor methods to fill in
   */
  public ToIndexFileConverter(
      @Nullable PackageDeclaration pkgDecl, List<ImportDeclaration> importDecls, AScene scene) {
    this(ImportContext.of(pkgDecl, importDecls), Collections.emptyList(), scene);
  }

  /**
   * Creates a new ToIndexFileConverter.
   *
   * @param importContext the package and imports for a name that is not in any compilation unit
   * @param compilationUnits the compilation units of a stub file, or an empty list if the converter
   *     does not consider the types that other compilation units declare
   * @param scene scene for visitor methods to fill in
   */
  private ToIndexFileConverter(
      ImportContext importContext, List<CompilationUnit> compilationUnits, AScene scene) {
    this.scene = scene;
    this.importContext = importContext;
    for (CompilationUnit cu : compilationUnits) {
      ImportContext cuContext = ImportContext.of(cu);
      importContexts.put(cu, cuContext);
      String pkg = cuContext.pkgName();
      if (pkg != null) {
        compilationUnitsByPackage.computeIfAbsent(pkg, k -> new ArrayList<>()).add(cu);
      }
    }
  }

  /**
   * Returns the import context of a compilation unit of the stub file.
   *
   * @param cu a compilation unit of the stub file, or null for a name that is not in any
   *     compilation unit
   * @return the package and imports of {@code cu}
   */
  private ImportContext importContext(@Nullable CompilationUnit cu) {
    return cu == null ? importContext : importContexts.computeIfAbsent(cu, ImportContext::of);
  }

  /**
   * Parses stub files and writes out equivalent JAIFs. Note that the results do not include
   * annotation definitions, for which stubfiles do not generally provide complete information.
   *
   * @param args name of JAIF with annotation definition, followed by names of stub files to be
   *     converted (if none given, program reads from standard input)
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("usage: java ToIndexFileConverter myfile.jaif [stubfile...]");
      System.err.println("(myfile.jaif contains needed annotation definitions)");
      System.exit(1);
    }

    AScene scene = new AScene();
    try {
      // args[0] is a jaif file with needed annotation definitions
      IndexFileParser.parseFile(args[0], scene);

      if (args.length == 1) {
        convert(scene, System.in, System.out);
        return;
      }

      for (int i = 1; i < args.length; i++) {
        String f0 = args[i];
        String f1 = (f0.endsWith(".astub") ? f0.substring(0, f0.length() - 6) : f0) + ".jaif";
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(f0)));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(f1)); ) {
          convert(new AScene(scene), in, out);
        }
      }
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Augments the given scene with information from a stub file, reading stubs from an input stream
   * and writing a JAIF to an output stream.
   *
   * @param scene the initial scene
   * @param in stubfile contents
   * @param out the output stream for the JAIF file that holds the augmented scene
   * @throws ParseException if the stub file cannot be parsed
   * @throws DefException if two different definitions of the same annotation cannot be unified
   * @throws IOException if there is trouble with file reading or writing
   */
  // Not private, so that tests can call it.
  static void convert(AScene scene, InputStream in, OutputStream out)
      throws IOException, DefException, ParseException {
    StubUnit iu;
    try {
      iu = StaticJavaParserUtil.parseStubUnit(in);
    } catch (ParseProblemException e) {
      iu = null;
      throw new BugInCF(
          "ToIndexFileConverter: exception from JavaParser.parseStubUnit for InputStream."
              + System.lineSeparator()
              + "Problem message with problems encountered: "
              + e.getMessage());
    }
    extractScene(iu, scene);
    try (Writer w = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      IndexFileWriter.write(scene, w);
    }
  }

  /**
   * Entry point of recursive-descent IndexUnit to AScene transformer. It operates by visiting the
   * stub and scene in parallel, descending into them in the same way. It augments the existing
   * scene (it does not create a new scene).
   *
   * @param iu {@link StubUnit} representing stubfile
   * @param scene the scene to augment
   */
  private static void extractScene(StubUnit iu, AScene scene) {
    // Every name is written in a compilation unit, whose package and imports apply to it.
    ToIndexFileConverter converter =
        new ToIndexFileConverter(ImportContext.of(null, null), iu.getCompilationUnits(), scene);
    for (CompilationUnit cu : iu.getCompilationUnits()) {
      NodeList<TypeDeclaration<?>> typeDecls = cu.getTypes();
      String pkgName = converter.importContext(cu).pkgName();
      if (typeDecls != null && pkgName != null) {
        for (TypeDeclaration<?> typeDecl : typeDecls) {
          String name = pkgName + "." + typeDecl.getNameAsString();
          typeDecl.accept(converter, scene.classes.getVivify(name));
        }
      }
    }
  }

  /**
   * Builds a simplified annotation from its declaration. Only the name is included, because
   * stubfiles do not generally have access to the full definitions of annotations.
   *
   * @param expr an annotation expression, such as {@code @Nullable}
   * @return a simplified annotation with only the name, or null if the annotation is synthetic
   */
  private static @Nullable Annotation extractAnnotation(AnnotationExpr expr) {
    String exprName = expr.toString().substring(1); // leave off leading '@'

    // Eliminate jdk.Profile+Annotation, a synthetic annotation that
    // the JDK adds, apparently for profiling.
    if (exprName.contains("+")) {
      return null;
    }
    @SuppressWarnings("signature") // special case for annotations containing "+"
    AnnotationDef def =
        new AnnotationDef(
            exprName,
            Collections.emptyMap(),
            "ToIndexFileConverter.extractAnnotation(" + expr + ")");
    return new Annotation(def, Collections.emptyMap());
  }

  /**
   * Adds annotations to a scene element, omitting any that {@link #extractAnnotation} discards.
   *
   * @param exprs annotations
   * @param elem the scene element to which to add the annotations
   */
  private static void addAnnotations(List<AnnotationExpr> exprs, AElement elem) {
    for (AnnotationExpr expr : exprs) {
      Annotation anno = extractAnnotation(expr);
      if (anno != null) {
        elem.tlAnnotationsHere.add(anno);
      }
    }
  }

  @Override
  public Void visit(AnnotationDeclaration decl, AElement elem) {
    return null;
  }

  @Override
  public Void visit(BlockStmt stmt, AElement elem) {
    return null;
    // super.visit(stmt, elem);
  }

  @Override
  public Void visit(ClassOrInterfaceDeclaration decl, AElement elem) {
    AClass clazz = classFor(decl, elem);
    visitDecl(decl, clazz);
    return super.visit(decl, clazz);
  }

  @Override
  public Void visit(ConstructorDeclaration decl, AElement elem) {
    List<Parameter> params = decl.getParameters();
    BlockStmt body = decl.getBody();
    StringBuilder sb = new StringBuilder("<init>(");
    AClass clazz = (AClass) elem;
    AMethod method;

    // Some of the methods in the generated parser use null to represent an empty list.
    List<Type> paramTypes = new ArrayList<>();
    if (params != null) {
      for (Parameter param : params) {
        paramTypes.add(parameterType(param));
        sb.append(parameterJVML(param));
      }
    }
    sb.append(")V");
    method = clazz.methods.getVivify(sb.toString());
    // A type annotation on a constructor declaration applies to the type of the constructed object.
    visitDeclAndTypeAnnotations(
        decl.getAnnotations(),
        ElementType.CONSTRUCTOR,
        method,
        new ClassOrInterfaceType().setName(decl.getNameAsString()),
        method.returnType);
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        visitParameter(params.get(i), paramTypes.get(i), method.parameters.getVivify(i));
      }
    }
    visitReceiverParameter(decl, method);
    return body == null ? null : body.accept(this, method);
    // return super.visit(decl, elem);
  }

  @Override
  public Void visit(EnumConstantDeclaration decl, AElement elem) {
    AField field = ((AClass) elem).fields.getVivify(decl.getNameAsString());
    visitDecl(decl, field);
    // Do not visit the arguments or the class body.  A class body declares an anonymous class,
    // whose members do not belong to the field.
    return null;
  }

  @Override
  public Void visit(EnumDeclaration decl, AElement elem) {
    AClass clazz = classFor(decl, elem);
    visitDecl(decl, clazz);
    return super.visit(decl, clazz);
  }

  @Override
  public Void visit(RecordDeclaration decl, AElement elem) {
    AClass clazz = classFor(decl, elem);
    visitDecl(decl, clazz);
    return super.visit(decl, clazz);
  }

  @Override
  public Void visit(FieldDeclaration decl, AElement elem) {
    for (VariableDeclarator v : decl.getVariables()) {
      AClass clazz = (AClass) elem;
      AField field = clazz.fields.getVivify(v.getNameAsString());
      visitType(v.getType(), field.type);
      visitDeclAndTypeAnnotations(
          decl.getAnnotations(), ElementType.FIELD, field, v.getType(), field.type);
    }
    return null;
  }

  @Override
  public Void visit(InitializerDeclaration decl, AElement elem) {
    BlockStmt block = decl.getBody();
    AClass clazz = (AClass) elem;
    block.accept(this, clazz.methods.getVivify(decl.isStatic() ? "<clinit>" : "<init>"));
    return null;
  }

  @Override
  public Void visit(MethodDeclaration decl, AElement elem) {
    Type type = decl.getType();
    List<Parameter> params = decl.getParameters();
    List<TypeParameter> typeParams = decl.getTypeParameters();
    BlockStmt body = decl.getBody().orElse(null);
    StringBuilder sb = new StringBuilder(decl.getNameAsString()).append('(');
    AClass clazz = (AClass) elem;
    AMethod method;
    List<Type> paramTypes = new ArrayList<>();
    if (params != null) {
      for (Parameter param : params) {
        paramTypes.add(parameterType(param));
        sb.append(parameterJVML(param));
      }
    }
    sb.append(')').append(getJVML(type));
    method = clazz.methods.getVivify(sb.toString());
    visitType(type, method.returnType);
    visitDeclAndTypeAnnotations(
        decl.getAnnotations(), ElementType.METHOD, method, type, method.returnType);
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        visitParameter(params.get(i), paramTypes.get(i), method.parameters.getVivify(i));
      }
    }
    visitReceiverParameter(decl, method);
    if (typeParams != null) {
      for (int i = 0; i < typeParams.size(); i++) {
        TypeParameter typeParam = typeParams.get(i);
        List<ClassOrInterfaceType> bounds = typeParam.getTypeBound();
        if (bounds != null) {
          for (int j = 0; j < bounds.size(); j++) {
            ClassOrInterfaceType bound = bounds.get(j);
            BoundLocation loc = new BoundLocation(i, j);
            bound.accept(this, method.bounds.getVivify(loc));
          }
        }
      }
    }
    return body == null ? null : body.accept(this, method);
  }

  @Override
  public Void visit(ObjectCreationExpr expr, AElement elem) {
    ClassOrInterfaceType type = expr.getType();
    AClass clazz = scene.classes.getVivify(type.getNameAsString());
    Expression scope = expr.getScope().orElse(null);
    List<Type> typeArgs = expr.getTypeArguments().orElse(null);
    List<Expression> args = expr.getArguments();
    NodeList<BodyDeclaration<?>> bodyDecls = expr.getAnonymousClassBody().orElse(null);
    if (scope != null) {
      scope.accept(this, elem);
    }
    if (args != null) {
      for (Expression arg : args) {
        arg.accept(this, elem);
      }
    }
    if (typeArgs != null) {
      for (Type typeArg : typeArgs) {
        typeArg.accept(this, elem);
      }
    }
    type.accept(this, clazz);
    if (bodyDecls != null) {
      for (BodyDeclaration<?> decl : bodyDecls) {
        decl.accept(this, clazz);
      }
    }
    return null;
  }

  @Override
  public Void visit(VariableDeclarationExpr expr, AElement elem) {
    List<AnnotationExpr> annos = expr.getAnnotations();
    AMethod method = (AMethod) elem;
    List<VariableDeclarator> varDecls = expr.getVariables();
    for (int i = 0; i < varDecls.size(); i++) {
      VariableDeclarator decl = varDecls.get(i);
      LocalLocation loc = new LocalLocation(i, decl.getNameAsString());
      AField field = method.body.locals.getVivify(loc);
      visitType(decl.getType(), field.type);
      visitDeclAndTypeAnnotations(
          annos, ElementType.LOCAL_VARIABLE, field, decl.getType(), field.type);
    }
    return null;
  }

  /**
   * Returns the scene element for a type declaration.
   *
   * @param decl a type declaration
   * @param elem the scene element for {@code decl} if it is a top-level type, or for its enclosing
   *     type if it is a nested type
   * @return the scene element for {@code decl}
   */
  private AClass classFor(TypeDeclaration<?> decl, AElement elem) {
    AClass clazz = (AClass) elem;
    if (!decl.isNestedType()) {
      return clazz;
    }
    return scene.classes.getVivify(clazz.className + "$" + decl.getNameAsString());
  }

  /**
   * Copies information from an AST declaration node to an {@link ADeclaration}. Called by visitors
   * for BodyDeclaration subclasses.
   *
   * @param decl the AST declaration node to inspect
   * @param elem destination declaration element
   * @return null
   */
  private Void visitDecl(BodyDeclaration<?> decl, ADeclaration elem) {
    addAnnotations(decl.getAnnotations(), elem);
    return null;
  }

  /**
   * Copies information from a method's or constructor's receiver parameter, if it has one, to the
   * receiver's scene element.
   *
   * @param decl a method or constructor declaration
   * @param method the scene element for {@code decl}
   */
  private void visitReceiverParameter(CallableDeclaration<?> decl, AMethod method) {
    Optional<ReceiverParameter> rcvrParam = decl.getReceiverParameter();
    if (rcvrParam.isPresent()) {
      addAnnotations(rcvrParam.get().getAnnotations(), method.receiver.type);
      visitType(rcvrParam.get().getType(), method.receiver.type);
    }
  }

  /**
   * Copies information from a formal parameter to an {@link AField}.
   *
   * @param param a formal parameter
   * @param type the type of {@code param}, as returned by {@link #parameterType}
   * @param field the scene element for {@code param}
   */
  private void visitParameter(Parameter param, Type type, AField field) {
    visitType(type, field.type);
    visitDeclAndTypeAnnotations(
        param.getAnnotations(), ElementType.PARAMETER, field, type, field.type);
  }

  /**
   * Returns a formal parameter's type. For a varargs parameter, that is an array type, which bears
   * the annotations that precede the {@code ...}.
   *
   * @param param a formal parameter
   * @return the type of {@code param}
   */
  private static Type parameterType(Parameter param) {
    if (!param.isVarArgs()) {
      return param.getType();
    }
    // For a varargs parameter, `getType()` is the element type.  Wrap copies of it and of the
    // annotations, because making a node part of the array type would remove it from the parameter.
    NodeList<AnnotationExpr> annos = new NodeList<>();
    for (AnnotationExpr anno : param.getVarArgsAnnotations()) {
      annos.add(anno.clone());
    }
    return new ArrayType(param.getType().clone(), ArrayType.Origin.TYPE, annos);
  }

  /**
   * Returns a formal parameter's JVML representation. Unlike {@code getJVML(parameterType(param))},
   * this resolves the type in the context of the parameter's declaration. For a varargs parameter,
   * {@link #parameterType} returns a copy of the element type that is not part of the AST, so
   * resolving that copy would not consider the declarations that enclose the parameter.
   *
   * @param param a formal parameter
   * @return the JVML representation of the type of {@code param}
   */
  private String parameterJVML(Parameter param) {
    String elementJVML = getJVML(param.getType());
    return param.isVarArgs() ? "[" + elementJVML : elementJVML;
  }

  /**
   * Copies the annotations that precede a declaration's type to the declaration, to the type, or to
   * both.
   *
   * <p>Such an annotation is a declaration annotation, a type annotation, or both, according to its
   * {@code @Target} (JLS 9.7.4). As a type annotation, it applies to the innermost component type
   * of an array type: in {@code @A String[]}, {@code @A} annotates {@code String}. It is not a type
   * annotation if {@code type} is {@code void}. An annotation that is not a type annotation is
   * recorded as a declaration annotation, even if its {@code @Target} cannot be determined or does
   * not permit the declaration.
   *
   * @param exprs the annotations that precede the declaration's type
   * @param declTarget the kind of declaration, such as {@link ElementType#FIELD}
   * @param decl the scene element for the declaration
   * @param type the declared type
   * @param typeElem the scene element for {@code type}
   */
  private void visitDeclAndTypeAnnotations(
      List<AnnotationExpr> exprs,
      ElementType declTarget,
      ADeclaration decl,
      Type type,
      ATypeElement typeElem) {
    List<TypePathEntry> elementLoc =
        Collections.nCopies(type.getArrayLevel(), TypePathEntry.ARRAY_ELEMENT);
    for (AnnotationExpr expr : exprs) {
      Annotation anno = extractAnnotation(expr);
      if (anno == null) {
        continue;
      }
      List<ElementType> targets = getTargets(expr);
      if (targets != null && targets.contains(ElementType.TYPE_USE) && !type.isVoidType()) {
        ATypeElement elementType =
            elementLoc.isEmpty() ? typeElem : typeElem.innerTypes.getVivify(elementLoc);
        elementType.tlAnnotationsHere.add(anno);
        if (targets.contains(declTarget)) {
          decl.tlAnnotationsHere.add(anno);
        }
      } else {
        decl.tlAnnotationsHere.add(anno);
      }
    }
  }

  /**
   * Returns the element types in the {@code @Target} meta-annotation of the annotation interface
   * that an annotation instantiates.
   *
   * @param expr an annotation
   * @return the element types in the {@code @Target} meta-annotation of {@code expr}'s annotation
   *     interface, or null if that interface cannot be loaded or has no {@code @Target}, or if
   *     {@code expr}'s name denotes a type parameter
   */
  private @Nullable List<ElementType> getTargets(AnnotationExpr expr) {
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameAsString
    @FullyQualifiedName String name = expr.getNameAsString();
    String[] identifiers = name.split("\\.", -1);
    Object declaration = enclosingDeclaration(expr, identifiers);
    if (declaration instanceof TypeParameter) {
      // A type parameter is not an annotation interface.
      return null;
    }
    String binaryName = classBinaryName(expr, declaration, name, identifiers);
    if (binaryName == null) {
      return null;
    }
    if (getTargetsCache.containsKey(binaryName)) {
      return getTargetsCache.get(binaryName);
    }
    @SuppressWarnings("signature") // a binary name of a class
    Class<?> annoClass = loadClass(binaryName);
    Target target = annoClass == null ? null : annoClass.getAnnotation(Target.class);
    List<ElementType> result = target == null ? null : Arrays.asList(target.value());
    getTargetsCache.put(binaryName, result);
    return result;
  }

  /**
   * Copies information from an AST type node to an {@link ATypeElement}.
   *
   * @param type the AST Type node to inspect
   * @param elem destination type element
   * @return null
   */
  private Void visitType(Type type, ATypeElement elem) {
    addAnnotations(type.getAnnotations(), elem);
    visitInnerTypes(type, elem);
    return null;
  }

  /**
   * Copies information from an AST type node's inner type nodes to an {@link ATypeElement}.
   *
   * @param type the AST Type node to inspect
   * @param elem destination type element
   */
  @SuppressWarnings("NotJavadoc") // Error Prone flags Javadoc comments on local class methods.
  private static Void visitInnerTypes(Type type, ATypeElement elem) {
    return type.accept(
        new GenericVisitorAdapter<Void, List<TypePathEntry>>() {
          @Override
          public Void visit(ClassOrInterfaceType type, List<TypePathEntry> loc) {
            if (type.getTypeArguments().isPresent()) {
              List<Type> typeArgs = type.getTypeArguments().get();
              for (int i = 0; i < typeArgs.size(); i++) {
                Type inner = typeArgs.get(i);
                List<TypePathEntry> ext = extendedTypePath(loc, TYPE_ARGUMENT, i);
                visitInnerType(inner, ext);
              }
            }
            return null;
          }

          @Override
          public Void visit(ArrayType type, List<TypePathEntry> loc) {
            // The annotations on `type` itself apply to the array type, whose type path is `loc`;
            // the caller has already recorded them.  This method handles the component type, which
            // is one ARRAY_ELEMENT step deeper.  A multi-dimensional array is a nest of ArrayType
            // nodes, so one step per call suffices.
            List<TypePathEntry> ext = extendedTypePath(loc, ARRAY_ELEMENT, 0);
            visitInnerType(type.getComponentType(), ext);
            return null;
          }

          @Override
          public Void visit(WildcardType type, List<TypePathEntry> loc) {
            ReferenceType lower = type.getExtendedType().orElse(null);
            ReferenceType upper = type.getSuperType().orElse(null);
            if (lower != null) {
              List<TypePathEntry> ext = extendedTypePath(loc, WILDCARD_BOUND, 0);
              visitInnerType(lower, ext);
            }
            if (upper != null) {
              List<TypePathEntry> ext = extendedTypePath(loc, WILDCARD_BOUND, 0);
              visitInnerType(upper, ext);
            }
            return null;
          }

          /**
           * Copies information from an AST inner type node to an {@link ATypeElement}, then
           * descends into the inner type's own inner types.
           *
           * @param type the AST node for the inner type
           * @param loc the type path of {@code type}
           */
          private void visitInnerType(Type type, List<TypePathEntry> loc) {
            for (AnnotationExpr expr : type.getAnnotations()) {
              Annotation anno = extractAnnotation(expr);
              if (anno != null) {
                // Vivify the entry for `loc` only when there is an annotation to put in it.  An
                // entry with no annotations would be written to the JAIF as a content-free
                // `inner-type` line.
                elem.innerTypes.getVivify(loc).tlAnnotationsHere.add(anno);
              }
            }
            // Descend into the type's own inner types, exactly once, whether or not the type
            // itself is annotated.
            type.accept(this, loc);
          }

          /**
           * Extends type path by one element.
           *
           * @see TypePathEntry(int, int)
           */
          private List<TypePathEntry> extendedTypePath(List<TypePathEntry> loc, int tag, int arg) {
            List<TypePathEntry> path = new ArrayList<>(loc.size() + 1);
            path.addAll(loc);
            path.add(TypePathEntry.create(tag, arg));
            return path;
          }
        },
        Collections.emptyList());
  }

  /**
   * Computes a type's JVML representation: its field descriptor, such as {@code I} for {@code int}
   * or {@code [[Ljava/lang/String;} for {@code String[][]}. For {@code void}, the result is {@code
   * V}.
   *
   * @param type the type
   * @return the type's JVML representation
   */
  // Not private, so that it can be tested.
  String getJVML(Type type) {
    return type.accept(
        new GenericVisitorAdapter<String, Void>() {
          // The type parameters whose erasure is being computed.  It prevents infinite recursion
          // on a malformed stub file whose bounds are cyclic, as in `<T extends U, U extends T>`.
          private final Set<TypeParameter> visiting =
              Collections.newSetFromMap(new IdentityHashMap<>());

          @Override
          public String visit(ClassOrInterfaceType type, Void v) {
            // Use the name together with its scope, so that a qualified name such as
            // `java.util.List` or `Map.Entry` is not truncated to its last identifier.
            @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameWithScope
            @FullyQualifiedName String typeName = type.getNameWithScope();
            String[] identifiers = typeName.split("\\.", -1);
            Object declaration = enclosingDeclaration(type, identifiers);
            if (declaration instanceof TypeParameter typeParam) {
              // The JVML descriptor uses the erasure, which is the first bound, or Object if
              // the type parameter has no bound.
              NodeList<ClassOrInterfaceType> bounds = typeParam.getTypeBound();
              if (bounds.isEmpty() || !visiting.add(typeParam)) {
                return "Ljava/lang/Object;";
              }
              String result = bounds.get(0).accept(this, null);
              visiting.remove(typeParam);
              return result;
            }
            String name = classBinaryName(type, declaration, typeName, identifiers);
            if (name == null) {
              name = unresolvedBinaryName(typeName, type.findCompilationUnit().orElse(null));
            }
            return "L" + name.replace('.', '/') + ";";
          }

          @Override
          public String visit(PrimitiveType type, Void v) {
            return switch (type.getType()) {
              case BOOLEAN -> "Z";
              case BYTE -> "B";
              case CHAR -> "C";
              case DOUBLE -> "D";
              case FLOAT -> "F";
              case INT -> "I";
              case LONG -> "J";
              case SHORT -> "S";
              default -> throw new BugInCF("unknown primitive type " + type);
            };
          }

          @Override
          public String visit(ArrayType type, Void v) {
            String typeName = type.getElementType().accept(this, null);
            StringBuilder sb = new StringBuilder();
            int n = type.getArrayLevel();
            sb.append("[".repeat(Math.max(0, n)));
            sb.append(typeName);
            return sb.toString();
          }

          @Override
          public String visit(VoidType type, Void v) {
            return "V";
          }

          @Override
          public String visit(WildcardType type, Void v) {
            // The erasure of a wildcard is the erasure of its upper bound.  The upper bound of an
            // unbounded wildcard, and of a "super" wildcard, is Object.
            ReferenceType extendedType = type.getExtendedType().orElse(null);
            if (extendedType == null) {
              return "Ljava/lang/Object;";
            }
            return extendedType.accept(this, null);
          }
        },
        null);
  }

  /**
   * Returns the binary name of the class that a type name denotes at a given node of the stub file.
   * A type that the stub file declares shadows an import and a type on the classpath.
   *
   * @param node a node in the stub file's AST, where {@code name} is written
   * @param declaration the type that the first element of {@code identifiers} denotes, as {@link
   *     #enclosingDeclaration} returns it, or null if it denotes no type in an enclosing scope; not
   *     a type parameter
   * @param name a type name as written in Java source, not necessarily fully qualified, such as
   *     {@code Entry}, {@code Map.Entry}, or {@code java.util.Map.Entry}
   * @param identifiers {@code name}, split at its {@code .} separators
   * @return the binary name of the class that {@code name} denotes, or null if it cannot be
   *     determined
   */
  private @Nullable String classBinaryName(
      Node node,
      @Nullable Object declaration,
      @FullyQualifiedName String name,
      String[] identifiers) {
    CompilationUnit cu = node.findCompilationUnit().orElse(null);
    String result = memberChainBinaryName(declaration, identifiers, packageName(cu));
    return result != null ? result : resolve(name, cu);
  }

  /**
   * Returns the binary name of the type that {@code identifiers} names, where {@code first} is the
   * type that the first element of {@code identifiers} names. Each remaining identifier names a
   * member type, as for {@link #memberBinaryName(TypeDeclaration, String[], int)} and {@link
   * #memberBinaryName(Class, String[], int, String)}.
   *
   * @param first the type that the first element of {@code identifiers} names: a {@link
   *     TypeDeclaration} of the stub file, a {@link Class} on the classpath, or null
   * @param identifiers a type name that has been split at its {@code .} separators
   * @param packageName the package of the compilation unit in which the name is written, or the
   *     empty string for the unnamed package
   * @return the binary name of the type that {@code identifiers} names, or null if {@code first} is
   *     null
   */
  private @Nullable String memberChainBinaryName(
      @Nullable Object first, String[] identifiers, String packageName) {
    if (first instanceof TypeDeclaration<?> typeDecl) {
      return memberBinaryName(typeDecl, identifiers, 1);
    } else if (first instanceof Class<?> clazz) {
      // A member type, on the classpath, of an enclosing type.
      return memberBinaryName(clazz, identifiers, 1, packageName);
    }
    return null;
  }

  /**
   * Returns the binary name, qualified by its package, of the given type declaration of the stub
   * file. For example, if the stub file declares, in package {@code p}, a top-level class {@code B}
   * containing a nested class {@code C}, then this method maps the declaration of {@code C} to
   * {@code "p.B$C"}.
   *
   * @param declaration a type declaration in the stub file
   * @return the binary name of {@code declaration}
   */
  private String qualifiedStubBinaryName(TypeDeclaration<?> declaration) {
    String pkg = stubPackage(declaration);
    String binaryName = stubBinaryName(declaration);
    return pkg.isEmpty() ? binaryName : pkg + "." + binaryName;
  }

  /**
   * If the stub file declares the type whose fully qualified name is {@code fqName}, in any of its
   * compilation units, returns that type's binary name; otherwise returns null.
   *
   * @param cu the stub file's compilation unit in which the name is written
   * @param fqName a fully qualified type name, in which a {@code .} separates a nested class from
   *     its enclosing class
   * @return the binary name of {@code fqName}, or null if the stub file does not declare it
   */
  private @Nullable String declaredInStubFile(CompilationUnit cu, String fqName) {
    // A fully qualified name does not indicate which of its identifiers name packages and which
    // name types.  As in Java (JLS 6.5.5.2), the shortest prefix of `fqName` that is a package and
    // that is followed by a top-level type of that package determines the type.
    if (importContext(cu).pkgName() == null) {
      String result = declaredInPackage(cu, null, fqName);
      if (result != null) {
        return result;
      }
    }
    for (int dot = fqName.indexOf('.'); dot != -1; dot = fqName.indexOf('.', dot + 1)) {
      String result = declaredInPackage(cu, fqName.substring(0, dot), fqName.substring(dot + 1));
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * If the stub file declares, in package {@code pkg}, the type that {@code relativeName} names,
   * returns that type's binary name; otherwise returns null. The first identifier of {@code
   * relativeName} names a top-level type of {@code pkg}, not a subpackage. As for an unqualified
   * name, {@link #memberBinaryName} resolves the identifiers after the top-level type, so {@code
   * p.B.Missing} and {@code B.Missing} have the same binary name.
   *
   * @param cu the stub file's compilation unit in which the name is written
   * @param pkg a package name, or null for the unnamed package
   * @param relativeName a type name relative to {@code pkg}, in which a {@code .} separates a
   *     nested class from its enclosing class
   * @return the binary name of the type, or null if the stub file does not declare it in {@code
   *     pkg}
   */
  private @Nullable String declaredInPackage(
      CompilationUnit cu, @Nullable String pkg, String relativeName) {
    String[] identifiers = relativeName.split("\\.", -1);
    TypeDeclaration<?> topLevel = stubTopLevelType(cu, pkg, identifiers[0]);
    return topLevel == null ? null : memberBinaryName(topLevel, identifiers, 1);
  }

  /**
   * Returns the top-level type that the stub file declares, in package {@code pkg}, with the given
   * simple name. The search considers {@code cu} first. A type in the unnamed package is sought
   * only in {@code cu}, because a type in the unnamed package cannot be referred to from another
   * compilation unit.
   *
   * @param cu the stub file's compilation unit in which the name is written, or null
   * @param pkg a package name, or null for the unnamed package
   * @param simpleName the simple name of a type
   * @return the top-level type of {@code pkg} named {@code simpleName}, or null if the stub file
   *     declares none
   */
  @SuppressWarnings("interning:not.interned") // reference equality of AST nodes
  private @Nullable TypeDeclaration<?> stubTopLevelType(
      @Nullable CompilationUnit cu, @Nullable String pkg, String simpleName) {
    if (cu != null && Objects.equals(pkg, importContext(cu).pkgName())) {
      TypeDeclaration<?> result = topLevelType(cu, simpleName);
      if (result != null) {
        return result;
      }
    }
    if (pkg != null) {
      for (CompilationUnit unit : compilationUnitsByPackage.getOrDefault(pkg, List.of())) {
        if (unit != cu) {
          TypeDeclaration<?> result = topLevelType(unit, simpleName);
          if (result != null) {
            return result;
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns the top-level type that the given compilation unit of the stub file declares with the
   * given simple name.
   *
   * @param cu a compilation unit of the stub file
   * @param simpleName the simple name of a type
   * @return the top-level type of {@code cu} named {@code simpleName}, or null if there is none
   */
  private static @Nullable TypeDeclaration<?> topLevelType(CompilationUnit cu, String simpleName) {
    for (TypeDeclaration<?> topLevel : cu.getTypes()) {
      if (topLevel.getNameAsString().equals(simpleName)) {
        return topLevel;
      }
    }
    return null;
  }

  /**
   * Returns the type that the first element of {@code identifiers} names in the innermost scope,
   * enclosing {@code node}, that contains it. The scopes searched are those of the declarations
   * that enclose {@code node} and of its compilation unit's top-level types. A class's scope
   * contains the member types that its counterpart on the classpath declares, and the member types
   * that it inherits from its supertypes. Within a class, a member type that the class declares, in
   * the stub file or in its counterpart, shadows an inherited one. Once the first identifier names
   * a type, the caller resolves the remaining identifiers within that type, without searching outer
   * scopes.
   *
   * @param node a node in the stub file's AST
   * @param identifiers a type name that has been split at its {@code .} separators
   * @return the type that the first element of {@code identifiers} names, as {@link
   *     #firstDeclarationAt} returns it: a {@link TypeParameter} or a {@link TypeDeclaration} of
   *     the stub file, or a {@link Class} on the classpath for a member type that the stub file
   *     does not declare; or null
   */
  private @Nullable Object enclosingDeclaration(Node node, String[] identifiers) {
    Node child = null;
    for (Node n = node; n != null; child = n, n = n.getParentNode().orElse(null)) {
      // The scope of a member type is the body of the type that declares it (JLS 6.3), so a
      // member type is not in scope in its enclosing type's header: its annotations, type
      // parameters, supertypes, and record components.  The members of a type's body, including
      // its enum constants, are its BodyDeclaration children.
      boolean inBody = child == null || child instanceof BodyDeclaration;
      // The scope of a type parameter of a class, method, or constructor excludes the
      // declaration's modifiers (JLS 6.3), which include its annotations.
      boolean inModifiers = isModifierAnnotation(child, n);
      // For a type declaration, firstDeclarationAt finds, via memberType, the member types that
      // the stub file declares, that its classpath counterpart declares, and that it inherits.
      Object declaration = firstDeclarationAt(n, identifiers, !inModifiers, inBody);
      if (declaration != null || n instanceof CompilationUnit) {
        return declaration;
      }
    }
    return null;
  }

  /**
   * Returns true if {@code child} is one of the annotations among the modifiers of {@code n}.
   *
   * @param child a node in the stub file's AST, or null
   * @param n a node in the stub file's AST
   * @return true if {@code child} is an annotation among the modifiers of {@code n}
   */
  @SuppressWarnings("interning:not.interned") // reference equality of AST nodes
  private static boolean isModifierAnnotation(@Nullable Node child, Node n) {
    if (!(child instanceof AnnotationExpr) || !(n instanceof NodeWithAnnotations<?> annotated)) {
      return false;
    }
    // Compare by identity, because a JavaParser node's `equals` compares its whole subtree.
    for (AnnotationExpr anno : annotated.getAnnotations()) {
      if (anno == child) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the type that the first element of {@code identifiers} names among the types that
   * {@code n} itself puts in scope: its type parameters, its member types (declared or inherited),
   * or (for a compilation unit) its top-level types. A type parameter is considered only if {@code
   * identifiers} has one element, because a type parameter has no member types.
   *
   * @param n a node in the stub file's AST
   * @param identifiers a type name that has been split at its {@code .} separators
   * @param typeParamsInScope whether the type parameters of {@code n} are in scope; false if the
   *     name is written in the modifiers of {@code n}
   * @param memberTypesInScope whether the member types of {@code n}, if it is a type declaration,
   *     are in scope, including the ones that it inherits from its supertypes; false if the name is
   *     written in the header of {@code n} rather than its body
   * @return the type that the first element of {@code identifiers} names: a {@link TypeParameter}
   *     or {@link TypeDeclaration} of the stub file, a {@link Class} on the classpath that is a
   *     member type of {@code n} (see {@link #memberType}), or null
   */
  private @Nullable Object firstDeclarationAt(
      Node n, String[] identifiers, boolean typeParamsInScope, boolean memberTypesInScope) {
    // Within one declaration, a type parameter and a member type cannot have the same name.  A
    // type parameter shadows a member type that the declaration inherits.
    if (typeParamsInScope
        && identifiers.length == 1
        && n instanceof NodeWithTypeParameters<?> nodeWithTypeParams) {
      for (TypeParameter typeParam : nodeWithTypeParams.getTypeParameters()) {
        if (typeParam.getNameAsString().equals(identifiers[0])) {
          return typeParam;
        }
      }
    }
    if (n instanceof TypeDeclaration<?> typeDecl) {
      if (!memberTypesInScope) {
        return null;
      }
      // A type's members include the member types that its classpath counterpart declares and
      // the ones that it inherits (JLS 8.5).  They shadow types in outer scopes and imports
      // (JLS 6.4.1).
      return memberType(typeDecl, identifiers[0]);
    } else if (n instanceof CompilationUnit compilationUnit) {
      return topLevelType(compilationUnit, identifiers[0]);
    }
    return null;
  }

  /**
   * Returns the member type named {@code identifier} that {@code declaration} declares.
   *
   * @param declaration a type declaration in a stub file
   * @param identifier the simple name of a member type
   * @return the declaration of {@code identifier}, or null if there is none
   */
  private @Nullable TypeDeclaration<?> memberTypeDeclaration(
      TypeDeclaration<?> declaration, String identifier) {
    return memberTypeDeclarationCache
        .computeIfAbsent(
            declaration,
            d -> {
              Map<String, TypeDeclaration<?>> nestedTypes = new HashMap<>();
              for (BodyDeclaration<?> member : d.getMembers()) {
                if (member instanceof TypeDeclaration<?> nestedType) {
                  nestedTypes.putIfAbsent(nestedType.getNameAsString(), nestedType);
                }
              }
              return nestedTypes;
            })
        .get(identifier);
  }

  /**
   * Returns the binary name, without its package, of the given type declaration of the stub file.
   * For example, if the stub file declares a top-level class {@code B} containing a nested class
   * {@code C}, then this method maps the declaration of {@code C} to {@code "B$C"}.
   *
   * @param declaration a type declaration in a stub file
   * @return the binary name of {@code declaration}, without its package
   */
  private static String stubBinaryName(TypeDeclaration<?> declaration) {
    StringBuilder binaryName = new StringBuilder(declaration.getNameAsString());
    for (Node n = declaration.getParentNode().orElse(null);
        n != null;
        n = n.getParentNode().orElse(null)) {
      if (n instanceof TypeDeclaration<?> typeDecl) {
        binaryName.insert(0, typeDecl.getNameAsString() + "$");
      }
    }
    return binaryName.toString();
  }

  /**
   * Returns the binary name of the class with the given name.
   *
   * @param className a type name as written in Java source, not necessarily fully qualified, such
   *     as {@code Entry}, {@code Map.Entry}, or {@code java.util.Map.Entry}
   * @param cu the stub file's compilation unit in which {@code className} is written, whose package
   *     and imports are used; or null for a name that is not in any compilation unit, to use the
   *     package and imports that the public constructor received and not to consider the types that
   *     the stub file declares
   * @return binary name of class that {@code className} identifies in the current context, or null
   *     if resolution fails
   */
  private @Nullable @BinaryName String resolve(
      @FullyQualifiedName String className, @Nullable CompilationUnit cu) {
    // The cache holds results for non-null compilation units only.
    if (cu == null) {
      return resolveUncached(className, null);
    }
    Map<String, @Nullable @BinaryName String> cache =
        resolveCache.computeIfAbsent(cu, c -> new HashMap<>());
    if (cache.containsKey(className)) {
      return cache.get(className);
    }
    String result = resolveUncached(className, cu);
    cache.put(className, result);
    return result;
  }

  /**
   * Returns the binary name of the class with the given name, without consulting the cache.
   *
   * @param className a type name as written in Java source, not necessarily fully qualified, such
   *     as {@code Entry}, {@code Map.Entry}, or {@code java.util.Map.Entry}
   * @param cu the stub file's compilation unit in which {@code className} is written, whose package
   *     and imports are used; or null for a name that is not in any compilation unit, to use the
   *     package and imports that the public constructor received and not to consider the types that
   *     the stub file declares
   * @return binary name of class that {@code className} identifies in the current context, or null
   *     if resolution fails
   */
  private @Nullable @BinaryName String resolveUncached(
      @FullyQualifiedName String className, @Nullable CompilationUnit cu) {
    // The package applies only within the compilation unit that declares it (JLS 7.5).  The stub
    // parser gives every compilation unit of a stub file all of the file's imports, so an import
    // applies throughout the stub file.
    ImportContext context = importContext(cu);
    String cuPkgName = context.pkgName();

    // The order of the lookups below is the order in which Java resolves a type name: a
    // single-type import shadows a type in the current package, which shadows a type that an
    // import-on-demand declaration makes available.

    for (String declName : context.singleTypeImports()) {
      String qualifiedName = mergeImport(declName, className);
      if (qualifiedName != null) {
        // The import determines the type that `className` refers to, even if that type cannot be
        // loaded, so do not look further.  If it cannot be loaded, `unresolvedBinaryName` uses
        // the import.
        return classBinaryName(qualifiedName, cu);
      }
    }
    if (cuPkgName != null) {
      // The first identifier of `className` names a type in the current package, not a
      // subpackage, so every later identifier names a member type.  Therefore, search only the
      // stub file's declarations in the current package itself.
      @SuppressWarnings("signature") // the stub file's declarations determine the binary name
      @BinaryName String declared = (cu == null) ? null : declaredInPackage(cu, cuPkgName, className);
      if (declared != null) {
        return declared;
      }
      @SuppressWarnings("signature") // a type in a package, followed by its member types
      @BinaryName String binaryName = cuPkgName + "." + className.replace('.', '$');
      if (loadClass(binaryName) != null) {
        return binaryName;
      }
    }

    for (String prefix : context.onDemandImports()) {
      String binaryName = onDemandImportBinaryName(prefix, className, cu);
      if (binaryName != null) {
        return binaryName;
      }
    }

    {
      // Every Java program implicitly does "import java.lang.*",
      // so see whether this class is in that package.
      String binaryName = onDemandImportBinaryName("java.lang", className, cu);
      if (binaryName != null) {
        return binaryName;
      }
    }

    return classBinaryName(className, cu);
  }

  /**
   * Returns the binary name of the class that {@code className} names via the import on demand
   * {@code prefix.*}.
   *
   * <p>An import on demand makes available the top-level types of a package, not its subpackages,
   * or the member types of a type. Therefore, the first identifier of {@code className} names a
   * top-level type of package {@code prefix} or a member type, declared or inherited, of type
   * {@code prefix}; the import does not make available any other name.
   *
   * @param prefix the name of the package or type that the import on demand imports from
   * @param className the class name, in which a {@code .} separates a nested class from its
   *     enclosing class
   * @param cu the stub file's compilation unit, or null not to consider the types that the stub
   *     file declares
   * @return the binary name of the class that {@code className} names via the import, or null
   */
  private @Nullable @BinaryName String onDemandImportBinaryName(
      String prefix, String className, @Nullable CompilationUnit cu) {
    if (cu != null) {
      @SuppressWarnings("signature") // the stub file's declarations determine the binary name
      @BinaryName String declared = onDemandImportStubBinaryName(prefix, className, cu);
      if (declared != null) {
        return declared;
      }
    }
    String qualifiedName = prefix + "." + className;
    String binaryName = loadClassBinaryName(qualifiedName);
    // No `.` follows the part of `binaryName` that comes from the import.
    if (binaryName == null || binaryName.lastIndexOf('.') > prefix.length()) {
      return null;
    }
    // An import on demand makes available only the accessible top-level or member types (JLS
    // 7.5.2).  An inaccessible one does not shadow a type that a later import on demand makes
    // available.  The first identifier of `className` names the type that the import makes
    // available; the binary name of that type is a prefix of `binaryName`.
    int firstLength = className.indexOf('.');
    if (firstLength < 0) {
      firstLength = className.length();
    }
    @SuppressWarnings("signature") // a prefix of a binary name, ending at the end of an identifier
    @ClassGetName String firstBinaryName = binaryName.substring(0, prefix.length() + 1 + firstLength);
    if (!isImportAccessible(loadClass(firstBinaryName), cu)) {
      return null;
    }
    return binaryName;
  }

  /**
   * If {@code className} names, via the import on demand {@code prefix.*}, a top-level type of a
   * package that the stub file declares, or a member type of a type that the stub file declares,
   * returns the binary name of the type that {@code className} names. Otherwise returns null.
   *
   * @param prefix the name of the package or type that the import on demand imports from
   * @param className the class name, in which a {@code .} separates a nested class from its
   *     enclosing class
   * @param cu the stub file's compilation unit in which the import is written
   * @return the binary name of the class that {@code className} names via the import, or null
   */
  private @Nullable String onDemandImportStubBinaryName(
      String prefix, String className, CompilationUnit cu) {
    String[] identifiers = className.split("\\.", -1);
    // An import on demand makes available only the accessible top-level types of a package (JLS
    // 7.5.2).  An inaccessible one does not shadow a type that a later import on demand makes
    // available.
    TypeDeclaration<?> topLevel = stubTopLevelType(cu, prefix, identifiers[0]);
    if (topLevel != null && isImportAccessible(topLevel, cu)) {
      return memberBinaryName(topLevel, identifiers, 1);
    }
    String prefixBinaryName = declaredInStubFile(cu, prefix);
    TypeDeclaration<?> prefixDecl =
        prefixBinaryName == null ? null : stubDeclarationOfBinaryName(cu, prefixBinaryName);
    if (prefixDecl == null) {
      return null;
    }
    Object first = firstDeclarationAt(prefixDecl, identifiers, false, true);
    // An import on demand makes available only the accessible member types (JLS 7.5.2).  An
    // inaccessible one does not shadow a type that a later import on demand makes available.
    if (!isImportAccessible(first, cu)) {
      return null;
    }
    return memberChainBinaryName(first, identifiers, packageName(cu));
  }

  /**
   * Returns true if a top-level or member type is accessible to an import on demand in the given
   * compilation unit, as javac determines it: a public type, or a member type of an interface or
   * annotation interface, is accessible; a private member type is not; a protected or
   * package-private type is accessible only in its own package.
   *
   * @param member a top-level or member type: a {@link TypeDeclaration} of the stub file, a {@link
   *     Class} on the classpath, or null
   * @param cu the stub file's compilation unit in which the import is written, or null for the
   *     converter's import context
   * @return true if {@code member} is non-null and accessible to an import in {@code cu}
   */
  private boolean isImportAccessible(@Nullable Object member, @Nullable CompilationUnit cu) {
    String cuPkgName = importContext(cu).pkgName();
    String cuPackage = cuPkgName == null ? "" : cuPkgName;
    if (member instanceof TypeDeclaration<?> memberDecl) {
      Node parent = memberDecl.getParentNode().orElse(null);
      if ((parent instanceof ClassOrInterfaceDeclaration classDecl && classDecl.isInterface())
          || parent instanceof AnnotationDeclaration
          || memberDecl.isPublic()) {
        return true;
      }
      if (memberDecl.isPrivate()) {
        return false;
      }
      return stubPackage(memberDecl).equals(cuPackage);
    } else if (member instanceof Class<?> memberClass) {
      Class<?> declaringClass = memberClass.getDeclaringClass();
      int modifiers = memberClass.getModifiers();
      if ((declaringClass != null && declaringClass.isInterface())
          || java.lang.reflect.Modifier.isPublic(modifiers)) {
        return true;
      }
      if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
        return false;
      }
      return memberClass.getPackageName().equals(cuPackage);
    }
    return false;
  }

  /**
   * Returns the binary name of the class that a fully qualified name refers to: the class that the
   * stub file declares, if the stub file declares it, and otherwise a class on the classpath.
   *
   * @param fqName a fully qualified class name
   * @param cu the stub file's compilation unit, or null not to consider the types that the stub
   *     file declares
   * @return the binary name of the class that {@code fqName} refers to, or null
   */
  private @Nullable @BinaryName String classBinaryName(
      String fqName, @Nullable CompilationUnit cu) {
    if (cu != null) {
      @SuppressWarnings("signature") // the stub file's declarations determine the binary name
      @BinaryName String declared = declaredInStubFile(cu, fqName);
      if (declared != null) {
        return declared;
      }
    }
    return loadClassBinaryName(fqName);
  }

  /**
   * Returns the binary name of the type that {@code identifiers} names, where {@code declaration}
   * is the type that the elements of {@code identifiers} before index {@code start} name. Each
   * remaining identifier names a member type that the stub file declares, or that the classpath
   * counterpart of a stub declaration declares, or that a type inherits from a supertype. If an
   * identifier names no member type of the type that the preceding identifiers name, the result is
   * that type's binary name followed by the identifier and the ones after it, each preceded by
   * {@code $}. For example, if the stub file declares a class {@code B} in package {@code p}, and
   * {@code B} has no member type {@code Missing}, then the result for {@code B.Missing.X} is {@code
   * p.B$Missing$X}.
   *
   * @param declaration a type declaration in the stub file
   * @param identifiers a type name that has been split at its {@code .} separators
   * @param start the index of the first element of {@code identifiers} that is a member of {@code
   *     declaration}
   * @return the binary name of the type that {@code identifiers} names
   */
  private String memberBinaryName(TypeDeclaration<?> declaration, String[] identifiers, int start) {
    if (start == identifiers.length) {
      return qualifiedStubBinaryName(declaration);
    }
    Object member = memberType(declaration, identifiers[start]);
    if (member instanceof TypeDeclaration<?> memberDecl) {
      return memberBinaryName(memberDecl, identifiers, start + 1);
    } else if (member instanceof Class<?> classpathMember) {
      return memberBinaryName(classpathMember, identifiers, start + 1, stubPackage(declaration));
    }
    return unresolvedMemberBinaryName(qualifiedStubBinaryName(declaration), identifiers, start);
  }

  /**
   * Returns the member type named {@code identifier} that {@code declaration} declares or inherits.
   * A member type that {@code declaration} declares, in the stub file or in its classpath
   * counterpart, shadows an inherited one.
   *
   * @param declaration a type declaration in the stub file
   * @param identifier the simple name of a member type
   * @return the member type: a {@link TypeDeclaration} if the stub file declares it, a {@link
   *     Class} if it is on the classpath, or null if there is none or it cannot be determined
   */
  private @Nullable Object memberType(TypeDeclaration<?> declaration, String identifier) {
    TypeDeclaration<?> nestedType = memberTypeDeclaration(declaration, identifier);
    if (nestedType != null) {
      return nestedType;
    }
    Class<?> clazz = counterpartMemberClass(declaration, identifier);
    if (clazz != null) {
      return clazz;
    }
    Object inherited = inheritedMemberType(declaration, identifier);
    if (inherited != null) {
      return inherited;
    }
    return classpathMemberClass(declaration, identifier, false, stubPackage(declaration));
  }

  /**
   * Returns the binary name of the type that {@code identifiers} names, where {@code clazz} is the
   * type that the elements of {@code identifiers} before index {@code start} name. Each remaining
   * identifier names a member type, declared or inherited, of a class on the classpath. If an
   * identifier names no member type of the class that the preceding identifiers name, the result is
   * that class's binary name followed by the identifier and the ones after it, each preceded by
   * {@code $}.
   *
   * @param clazz a class on the classpath
   * @param identifiers a type name that has been split at its {@code .} separators
   * @param start the index of the first element of {@code identifiers} that is a member of {@code
   *     clazz}
   * @param packageName the package of the code in which the name is written, or the empty string
   *     for the unnamed package
   * @return the binary name of the type that {@code identifiers} names
   */
  private static String memberBinaryName(
      Class<?> clazz, String[] identifiers, int start, String packageName) {
    for (int i = start; i < identifiers.length; i++) {
      Class<?> nestedType = memberClass(clazz, identifiers[i], packageName, false, new HashSet<>());
      if (nestedType == null) {
        return unresolvedMemberBinaryName(clazz.getName(), identifiers, i);
      }
      clazz = nestedType;
    }
    return clazz.getName();
  }

  /**
   * Returns the binary name of a member type that cannot be found, by appending the unresolved
   * identifiers to the binary name of the innermost type that was found.
   *
   * @param binaryName the binary name of the type that {@code identifiers[start - 1]} names
   * @param identifiers a type name that has been split at its {@code .} separators
   * @param start the index of the first element of {@code identifiers} that cannot be found
   * @return the binary name that {@code identifiers} most likely refers to
   */
  private static String unresolvedMemberBinaryName(
      String binaryName, String[] identifiers, int start) {
    StringBuilder result = new StringBuilder(binaryName);
    for (int i = start; i < identifiers.length; i++) {
      result.append('$').append(identifiers[i]);
    }
    return result.toString();
  }

  /**
   * Returns the member type named {@code identifier} that a type declaration of the stub file
   * inherits from its supertypes. The member types of a supertype that the stub file declares
   * include the ones that its classpath counterpart declares, even if the stub file omits them.
   *
   * @param declaration a type declaration in the stub file
   * @param identifier the simple name of a member type
   * @return the inherited member type: a {@link TypeDeclaration} if the stub file declares it, a
   *     {@link Class} if it is on the classpath, or null if there is none or it cannot be
   *     determined
   */
  private @Nullable Object inheritedMemberType(TypeDeclaration<?> declaration, String identifier) {
    Map<String, @Nullable Object> cache =
        inheritedMemberTypesCache.computeIfAbsent(declaration, d -> new HashMap<>());
    if (cache.containsKey(identifier)) {
      return cache.get(identifier);
    }
    // A call from within classpathMemberClass is not outermost:  classpathMemberClass's search,
    // which is in progress, can also be cut short.
    boolean outermost = inheritanceVisiting.isEmpty() && classpathInheritanceVisiting.isEmpty();
    boolean enclosingCycleCut = inheritanceCycleCut;
    inheritanceCycleCut = false;
    Object result = inheritedMemberTypeUncached(declaration, identifier);
    // A result is incomplete if cyclic inheritance cut its search short at a type whose search was
    // already in progress.  The outermost call's search includes those types, so its result is
    // complete.
    if (outermost || !inheritanceCycleCut) {
      cache.put(identifier, result);
    }
    inheritanceCycleCut |= enclosingCycleCut;
    return result;
  }

  /**
   * Returns the member type named {@code identifier} that a type declaration of the stub file
   * inherits from its supertypes, without consulting the cache.
   *
   * @param declaration a type declaration in the stub file
   * @param identifier the simple name of a member type
   * @return the inherited member type: a {@link TypeDeclaration} if the stub file declares it, a
   *     {@link Class} if it is on the classpath, or null if there is none or it cannot be
   *     determined
   */
  private @Nullable Object inheritedMemberTypeUncached(
      TypeDeclaration<?> declaration, String identifier) {
    if (!inheritanceVisiting.add(declaration)) {
      inheritanceCycleCut = true;
      return null;
    }
    try {
      for (ClassOrInterfaceType supertype : supertypes(declaration)) {
        Object superDecl = resolveSupertype(supertype);
        if (superDecl instanceof TypeDeclaration<?> superTypeDecl) {
          // A member type that `declaration` does not inherit, because it is inaccessible, does
          // not hide a member type of a later supertype.
          TypeDeclaration<?> nestedType = memberTypeDeclaration(superTypeDecl, identifier);
          if (nestedType != null) {
            if (isInheritedStubMemberType(superTypeDecl, nestedType, declaration)) {
              return nestedType;
            }
            // `nestedType` is not inherited, but it hides every member type of the same name in the
            // supertypes of `superTypeDecl` (JLS 8.5).
            continue;
          }
          // The stub file often omits a member type that the classpath counterpart of
          // `superTypeDecl` declares.
          Class<?> counterpartMember = counterpartMemberClass(superTypeDecl, identifier);
          if (counterpartMember != null) {
            if (isInheritedClasspathMemberType(counterpartMember, declaration)) {
              return counterpartMember;
            }
            continue;
          }
          // `superTypeDecl` inherits `inherited`; checking that `declaration` inherits it too, at
          // each step, checks every class on the inheritance path.
          Object inherited = inheritedMemberType(superTypeDecl, identifier);
          if (inherited instanceof TypeDeclaration<?> inheritedDecl) {
            TypeDeclaration<?> declaringType =
                inheritedDecl.getParentNode().orElse(null) instanceof TypeDeclaration<?> parent
                    ? parent
                    : null;
            if (declaringType != null
                && isInheritedStubMemberType(declaringType, inheritedDecl, declaration)) {
              return inheritedDecl;
            }
          } else if (inherited instanceof Class<?> inheritedClass) {
            if (isInheritedClasspathMemberType(inheritedClass, declaration)) {
              return inheritedClass;
            }
          } else if (inherited == null) {
            // The stub file often omits a supertype that the classpath counterpart of
            // `superTypeDecl` has.
            Class<?> classpathMember =
                classpathMemberClass(superTypeDecl, identifier, true, stubPackage(declaration));
            if (classpathMember != null) {
              return classpathMember;
            }
          }
        } else if (superDecl instanceof Class<?> superClass) {
          Class<?> nestedType =
              memberClass(superClass, identifier, stubPackage(declaration), true, new HashSet<>());
          if (nestedType != null) {
            return nestedType;
          }
        }
      }
      if (declaration instanceof EnumDeclaration) {
        // The implicit superclass of an enum declares the member type `Enum.EnumDesc`.
        return memberClass(Enum.class, identifier, stubPackage(declaration), true, new HashSet<>());
      }
      return null;
    } finally {
      inheritanceVisiting.remove(declaration);
    }
  }

  /**
   * Returns true if a type declaration of the stub file inherits a member type that a supertype
   * declares. As for {@link #memberClass}, a member type is inherited only if it is not private and
   * it is accessible in the subtype (JLS 8.5): it is public or protected, it is a member of an
   * interface or annotation interface (and so implicitly public), or it is in the subtype's
   * package.
   *
   * @param declaringType a type declaration of the stub file
   * @param member a member type that {@code declaringType} declares
   * @param subtype a type declaration of the stub file that is a subtype of {@code declaringType}
   * @return true if {@code subtype} inherits {@code member}
   */
  private boolean isInheritedStubMemberType(
      TypeDeclaration<?> declaringType, TypeDeclaration<?> member, TypeDeclaration<?> subtype) {
    if ((declaringType instanceof ClassOrInterfaceDeclaration classDecl && classDecl.isInterface())
        || declaringType instanceof AnnotationDeclaration) {
      // A member type of an interface is implicitly public.
      return true;
    }
    if (member.isPrivate()) {
      return false;
    }
    return member.isPublic()
        || member.isProtected()
        || stubPackage(member).equals(stubPackage(subtype));
  }

  /**
   * Returns true if the given type declaration of the stub file inherits the given member type,
   * which is on the classpath and which a supertype declares or inherits. As for {@link
   * #isInheritedStubMemberType}, a member type is inherited only if it is accessible in the subtype
   * (JLS 8.5).
   *
   * @param member a member type on the classpath
   * @param subtype the type declaration of the stub file whose direct supertype declares or
   *     inherits {@code member}
   * @return true if {@code subtype} inherits {@code member}
   */
  private boolean isInheritedClasspathMemberType(Class<?> member, TypeDeclaration<?> subtype) {
    return member.getDeclaringClass() != null && isAccessible(member, stubPackage(subtype));
  }

  /**
   * Returns the package of a type declaration of the stub file, in the form that {@link
   * Class#getPackageName} returns.
   *
   * @param declaration a type declaration in the stub file
   * @return the package of {@code declaration}, or {@code ""} for the unnamed package
   */
  private String stubPackage(TypeDeclaration<?> declaration) {
    CompilationUnit cu = declaration.findCompilationUnit().orElse(null);
    String result = cu == null ? null : importContext(cu).pkgName();
    return result == null ? "" : result;
  }

  /**
   * Returns the supertypes that a type declaration of the stub file names in its {@code extends}
   * and {@code implements} clauses. The result does not include implicit supertypes; {@link
   * #inheritedMemberType} handles the implicit superclass {@code java.lang.Enum} of an enum.
   *
   * @param declaration a type declaration in the stub file
   * @return the supertypes of {@code declaration}
   */
  private static List<ClassOrInterfaceType> supertypes(TypeDeclaration<?> declaration) {
    if (declaration instanceof ClassOrInterfaceDeclaration classDecl) {
      List<ClassOrInterfaceType> result = new ArrayList<>(classDecl.getExtendedTypes());
      result.addAll(classDecl.getImplementedTypes());
      return result;
    } else if (declaration instanceof EnumDeclaration enumDecl) {
      return enumDecl.getImplementedTypes();
    } else if (declaration instanceof RecordDeclaration recordDecl) {
      return recordDecl.getImplementedTypes();
    }
    return Collections.emptyList();
  }

  /**
   * Returns the type that a supertype in the header of a type declaration of the stub file names.
   *
   * @param supertype a supertype in an {@code extends} or {@code implements} clause
   * @return a {@link TypeDeclaration} if the stub file declares the supertype, a {@link Class} if
   *     it is on the classpath, or null if it cannot be determined
   */
  private @Nullable Object resolveSupertype(ClassOrInterfaceType supertype) {
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameWithScope
    @FullyQualifiedName String typeName = supertype.getNameWithScope();
    String[] identifiers = typeName.split("\\.", -1);
    Object declaration = enclosingDeclaration(supertype, identifiers);
    if (declaration instanceof TypeParameter) {
      return null;
    }
    String binaryName = classBinaryName(supertype, declaration, typeName, identifiers);
    if (binaryName == null) {
      return null;
    }
    TypeDeclaration<?> stubDecl = stubDeclarationOfBinaryName(supertype, binaryName);
    if (stubDecl != null) {
      return stubDecl;
    }
    @SuppressWarnings("signature") // a binary name of a class
    Class<?> result = loadClass(binaryName);
    return result;
  }

  /**
   * Returns the type declaration of the stub file that has the given binary name.
   *
   * <p>The declaration is in a compilation unit of the stub file whose package is the package of
   * {@code binaryName}. A type in the unnamed package is sought only in {@code node}'s compilation
   * unit, because a type in the unnamed package cannot be referred to from another package.
   *
   * <p>A {@code $} in a binary name may separate a member type from its enclosing type, or it may
   * be part of an identifier, as in a class named {@code Base$Generated}. This method matches the
   * names of actual declarations, so it handles both.
   *
   * @param node a node in the stub file's AST
   * @param binaryName a binary name
   * @return the type declaration of the stub file whose binary name is {@code binaryName}, or null
   *     if there is none
   */
  private @Nullable TypeDeclaration<?> stubDeclarationOfBinaryName(Node node, String binaryName) {
    int lastDot = binaryName.lastIndexOf('.');
    String pkg = lastDot < 0 ? null : binaryName.substring(0, lastDot);
    String nameInPackage = binaryName.substring(lastDot + 1);
    CompilationUnit cu = node.findCompilationUnit().orElse(null);
    int end = -1;
    do {
      end = nameInPackage.indexOf('$', end + 1);
      if (end < 0) {
        end = nameInPackage.length();
      }
      TypeDeclaration<?> topLevel = stubTopLevelType(cu, pkg, nameInPackage.substring(0, end));
      if (topLevel != null) {
        TypeDeclaration<?> result = memberDeclarationOfBinaryName(topLevel, nameInPackage, end);
        if (result != null) {
          return result;
        }
      }
    } while (end < nameInPackage.length());
    return null;
  }

  /**
   * Returns the type declaration, at or within {@code declaration}, whose binary name without its
   * package is {@code nameInPackage}. Each {@code $} in {@code nameInPackage} at or after {@code
   * start} may separate a member type from its enclosing type or may be part of an identifier; this
   * method tries each possibility.
   *
   * @param declaration a type declaration in a stub file, whose binary name without its package is
   *     {@code nameInPackage.substring(0, start)}
   * @param nameInPackage a binary name without its package
   * @param start the index in {@code nameInPackage} just after the name of {@code declaration};
   *     either the length of {@code nameInPackage} or the index of a {@code $}
   * @return the type declaration whose binary name without its package is {@code nameInPackage}, or
   *     null if there is none
   */
  private @Nullable TypeDeclaration<?> memberDeclarationOfBinaryName(
      TypeDeclaration<?> declaration, String nameInPackage, int start) {
    if (start == nameInPackage.length()) {
      return declaration;
    }
    int end = start;
    do {
      end = nameInPackage.indexOf('$', end + 1);
      if (end < 0) {
        end = nameInPackage.length();
      }
      TypeDeclaration<?> member =
          memberTypeDeclaration(declaration, nameInPackage.substring(start + 1, end));
      if (member != null) {
        TypeDeclaration<?> result = memberDeclarationOfBinaryName(member, nameInPackage, end);
        if (result != null) {
          return result;
        }
      }
    } while (end < nameInPackage.length());
    return null;
  }

  /**
   * Returns the package of a compilation unit of the stub file.
   *
   * @param cu a compilation unit of the stub file, or null for a name that is not in any
   *     compilation unit
   * @return the package of {@code cu}, or the empty string for the unnamed package
   */
  private String packageName(@Nullable CompilationUnit cu) {
    String result = importContext(cu).pkgName();
    return result == null ? "" : result;
  }

  /**
   * Returns the member type named {@code identifier} that the classpath counterpart of {@code
   * declaration} itself declares, or null if there is none. The result can be any member type that
   * the counterpart declares, including a private or otherwise inaccessible one; each caller checks
   * whether the result is accessible where the caller uses it.
   *
   * @param declaration a type declaration in a stub file
   * @param identifier the simple name of a member type
   * @return the member type named {@code identifier}, or null
   */
  private @Nullable Class<?> counterpartMemberClass(
      TypeDeclaration<?> declaration, String identifier) {
    @SuppressWarnings("signature") // a binary name is a valid argument to Class.forName
    @ClassGetName String counterpartName = qualifiedStubBinaryName(declaration);
    return declaredMemberClass(loadClass(counterpartName), identifier);
  }

  /**
   * Returns the member type named {@code identifier} that {@code clazz} itself declares, or null if
   * there is none.
   *
   * @param clazz a class on the classpath, or null
   * @param identifier the simple name of a member type
   * @return the member type named {@code identifier}, or null
   */
  private static @Nullable Class<?> declaredMemberClass(
      @Nullable Class<?> clazz, String identifier) {
    if (clazz == null) {
      return null;
    }
    Class<?>[] members;
    try {
      members = clazz.getDeclaredClasses();
    } catch (LinkageError | SecurityException e) {
      // The class exists but its members cannot be read; treat it as having none.
      return null;
    }
    for (Class<?> member : members) {
      if (member.getSimpleName().equals(identifier)) {
        return member;
      }
    }
    return null;
  }

  /**
   * Returns the member type named {@code identifier} that the classpath counterpart of {@code
   * declaration}, or of one of its supertypes, declares or inherits; or null if there is none. The
   * classpath counterpart of a stub declaration is the class on the classpath that has the same
   * binary name. A stub file often omits some of that class's members.
   *
   * <p>This method does not return a member type that the stub file declares, which {@link
   * #memberTypeDeclaration} finds, but such a member type hides the member types of the same name
   * in the supertypes of the type that declares it.
   *
   * @param declaration a type declaration in a stub file
   * @param identifier the simple name of a member type
   * @param inherited if true, {@code declaration} is a supertype of the class whose members are
   *     sought, so only a member type that is accessible from {@code packageName} is inherited
   * @param packageName the package of the stub declaration whose members are sought, or the empty
   *     string for the unnamed package
   * @return the member type named {@code identifier}, or null
   */
  private @Nullable Class<?> classpathMemberClass(
      TypeDeclaration<?> declaration, String identifier, boolean inherited, String packageName) {
    Map<ClasspathMemberQuery, @Nullable Class<?>> cache =
        classpathMemberClassCache.computeIfAbsent(declaration, d -> new HashMap<>());
    ClasspathMemberQuery query = new ClasspathMemberQuery(identifier, inherited, packageName);
    if (cache.containsKey(query)) {
      return cache.get(query);
    }
    // As in inheritedMemberType, a result is cached only if it is complete.  Without the cache, a
    // search through a hierarchy in which several paths lead to the same supertype, as in a
    // diamond, would search that supertype once per path.
    boolean outermost = inheritanceVisiting.isEmpty() && classpathInheritanceVisiting.isEmpty();
    boolean enclosingCycleCut = inheritanceCycleCut;
    inheritanceCycleCut = false;
    Class<?> result = classpathMemberClassUncached(declaration, identifier, inherited, packageName);
    if (outermost || !inheritanceCycleCut) {
      cache.put(query, result);
    }
    inheritanceCycleCut |= enclosingCycleCut;
    return result;
  }

  /**
   * Returns the member type named {@code identifier} that the classpath counterpart of {@code
   * declaration}, or of one of its supertypes, declares or inherits, without consulting the cache;
   * or null if there is none.
   *
   * @param declaration a type declaration in a stub file
   * @param identifier the simple name of a member type
   * @param inherited if true, {@code declaration} is a supertype of the class whose members are
   *     sought, so only a member type that is accessible from {@code packageName} is inherited
   * @param packageName the package of the stub declaration whose members are sought, or the empty
   *     string for the unnamed package
   * @return the member type named {@code identifier}, or null
   */
  private @Nullable Class<?> classpathMemberClassUncached(
      TypeDeclaration<?> declaration, String identifier, boolean inherited, String packageName) {
    if (!classpathInheritanceVisiting.add(declaration)) {
      // The stub file declares a cyclic inheritance hierarchy, which is not legal Java.
      inheritanceCycleCut = true;
      return null;
    }
    try {
      // A member type that `declaration` declares, in the stub file or in its classpath
      // counterpart, hides every member type of the same name in the supertypes of `declaration`,
      // even if it is not inherited.
      if (memberTypeDeclaration(declaration, identifier) != null) {
        return null;
      }
      @SuppressWarnings("signature") // a binary name is a valid argument to Class.forName
      @ClassGetName String counterpartName = qualifiedStubBinaryName(declaration);
      Class<?> counterpart = loadClass(counterpartName);
      // When its `inherited` argument is false, memberClass returns a member type that
      // `counterpart` declares even if it is inaccessible, because such a member type hides the
      // member types of the same name in the supertypes of `counterpart`.  Every member type that
      // memberClass finds in a supertype is accessible, so the check below can reject only a
      // member type that `counterpart` declares.
      Class<?> result = memberClass(counterpart, identifier, packageName, false, new HashSet<>());
      if (result != null) {
        return (!inherited || isAccessible(result, packageName)) ? result : null;
      }
      for (ClassOrInterfaceType supertype : supertypes(declaration)) {
        Object superDecl = resolveSupertype(supertype);
        if (superDecl instanceof TypeDeclaration<?> superTypeDecl) {
          result = classpathMemberClass(superTypeDecl, identifier, true, packageName);
        } else if (superDecl instanceof Class<?> superClass) {
          result = memberClass(superClass, identifier, packageName, true, new HashSet<>());
        }
        if (result != null) {
          return result;
        }
      }
      return null;
    } finally {
      classpathInheritanceVisiting.remove(declaration);
    }
  }

  /**
   * Returns the member type named {@code identifier} that {@code clazz} declares or inherits, or
   * null if there is none. A class inherits only the member types that are accessible from it, so
   * this method ignores an inherited private member type. It also ignores an inherited
   * package-private member type unless the member type and every class along the inheritance path
   * are in {@code packageName}. A member type that a class declares hides every member type of the
   * same name in the class's supertypes, even if the declared member type is not inherited because
   * it is private or inaccessible (JLS 8.5).
   *
   * <p>An <em>inheritance path</em> is a sequence of types, each a direct supertype of the one
   * before it, that starts at the type whose member type is sought and ends at a type that declares
   * the member type. For example, if {@code Sub extends Middle}, {@code Middle extends Base}, and
   * {@code Base} declares {@code Inner}, then {@code Sub}, {@code Middle}, {@code Base} is an
   * inheritance path for {@code Sub.Inner}.
   *
   * @param clazz a class on the classpath, or null
   * @param identifier the simple name of a member type
   * @param packageName the package of every class along the inheritance path from the class whose
   *     members are sought to {@code clazz}, excluding {@code clazz}; the empty string for the
   *     unnamed package; or null if those classes are not all in the same package
   * @param inherited if true, the member types that {@code clazz} declares are themselves inherited
   *     by the class whose members are sought, so they must be accessible from {@code packageName}
   * @param visited the classes that have already been searched; this method adds {@code clazz} to
   *     it
   * @return the member type named {@code identifier}, or null if there is none
   */
  private static @Nullable Class<?> memberClass(
      @Nullable Class<?> clazz,
      String identifier,
      @Nullable String packageName,
      boolean inherited,
      Set<Class<?>> visited) {
    if (clazz == null || !visited.add(clazz)) {
      return null;
    }
    Class<?> member = declaredMemberClass(clazz, identifier);
    if (member != null) {
      // A member type that `clazz` declares hides every member type of the same name in the
      // supertypes of `clazz`, even if it is not inherited.
      return (!inherited || isAccessible(member, packageName)) ? member : null;
    }
    // A supertype's member type is inherited by `clazz` only if it is accessible from `clazz`.
    String superPackageName = clazz.getPackageName().equals(packageName) ? packageName : null;
    Class<?> result =
        memberClass(clazz.getSuperclass(), identifier, superPackageName, true, visited);
    if (result != null) {
      return result;
    }
    for (Class<?> superinterface : clazz.getInterfaces()) {
      result = memberClass(superinterface, identifier, superPackageName, true, visited);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns true if the member type {@code member} is accessible from code in {@code packageName}
   * that is in a subclass of the class that declares {@code member}.
   *
   * @param member a member type
   * @param packageName a package name, the empty string for the unnamed package, or null for no
   *     package
   * @return true if {@code member} is accessible from {@code packageName}
   */
  private static boolean isAccessible(Class<?> member, @Nullable String packageName) {
    Class<?> declaringClass = member.getDeclaringClass();
    if (declaringClass != null && declaringClass.isInterface()) {
      // A member type of an interface is implicitly public.
      return true;
    }
    int modifiers = member.getModifiers();
    if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
      return true;
    }
    return !Modifier.isPrivate(modifiers) && member.getPackageName().equals(packageName);
  }

  /**
   * Returns the binary name that a type name most likely refers to, for a type name that cannot be
   * resolved. Because such a name does not indicate which of its dot-separated components are
   * package names and which are class names, this method uses the Java convention that a package
   * name starts with a lowercase letter and a class name starts with an uppercase letter. A name
   * whose first identifier a single-type import names is qualified by that import. Any other name
   * with no package name is assumed to refer to the package of the compilation unit in which it is
   * written.
   *
   * @param typeName a type name that resolution failed on
   * @param cu the compilation unit in which {@code typeName} is written, or null if it is not in
   *     any compilation unit
   * @return the binary name that {@code typeName} most likely refers to
   */
  private String unresolvedBinaryName(
      @FullyQualifiedName String typeName, @Nullable CompilationUnit cu) {
    ImportContext context = importContext(cu);
    for (String declName : context.singleTypeImports()) {
      String qualifiedName = mergeImport(declName, typeName);
      // An unchanged name is an import of a type in the unnamed package, which gives no package.
      if (qualifiedName != null && !qualifiedName.equals(typeName)) {
        // The import is fully qualified, so do not qualify the result by the stub file's package.
        return splitBinaryName(qualifiedName, null);
      }
    }
    return splitBinaryName(typeName, context.pkgName());
  }

  /**
   * Returns the binary name that a type name most likely refers to, using the Java convention that
   * a package name starts with a lowercase letter and a class name starts with an uppercase letter.
   *
   * @param typeName a type name
   * @param defaultPackage the package of {@code typeName} if it has no package name, or null
   * @return the binary name that {@code typeName} most likely refers to
   */
  private static String splitBinaryName(String typeName, @Nullable String defaultPackage) {
    String[] identifiers = typeName.split("\\.", -1);
    // The index of the outermost class name; every identifier before it is a package name.
    int outermostClass = 0;
    while (outermostClass < identifiers.length - 1
        && !startsWithUppercase(identifiers[outermostClass])) {
      outermostClass++;
    }
    StringBuilder result = new StringBuilder();
    if (outermostClass == 0 && defaultPackage != null) {
      // The name has no package name, so it refers to the default package.
      result.append(defaultPackage).append('.');
    }
    for (int i = 0; i < identifiers.length; i++) {
      if (i > 0) {
        result.append(i <= outermostClass ? '.' : '$');
      }
      result.append(identifiers[i]);
    }
    return result.toString();
  }

  /**
   * Returns true if {@code identifier} starts with an uppercase letter.
   *
   * @param identifier an identifier
   * @return true if {@code identifier} starts with an uppercase letter
   */
  private static boolean startsWithUppercase(String identifier) {
    return !identifier.isEmpty() && Character.isUpperCase(identifier.charAt(0));
  }

  /**
   * Returns the binary name of the class that a fully qualified name refers to, or null if no such
   * class can be loaded. A fully qualified name does not indicate which of its dot-separated
   * components are packages and which are enclosing classes, so this method tries each possibility,
   * starting with the one that has the fewest enclosing classes.
   *
   * @param fqName a fully qualified class name
   * @return the binary name of the class that {@code fqName} refers to, or null
   */
  @SuppressWarnings("signature") // string manipulation of signature strings
  private static @Nullable @BinaryName String loadClassBinaryName(String fqName) {
    StringBuilder candidate = new StringBuilder(fqName);
    int dot = candidate.length();
    while (true) {
      if (loadClass(candidate.toString()) != null) {
        return candidate.toString();
      }
      dot = candidate.lastIndexOf(".", dot - 1);
      if (dot < 0) {
        return null;
      }
      candidate.setCharAt(dot, '$');
    }
  }

  /**
   * Combines a single-type import with a partially qualified name, yielding a fully qualified name.
   *
   * @param importName the type that a single-type import imports, such as {@code java.util.Map}
   * @param className the class name
   * @return fully qualified class name if resolution succeeds, null otherwise
   */
  @SuppressWarnings("signature") // string manipulation of signature strings
  private static @Nullable @FullyQualifiedName String mergeImport(
      String importName, @FullyQualifiedName String className) {
    if (importName.equals(className)) {
      return className;
    }
    String importEnd = importName.substring(importName.lastIndexOf('.') + 1);
    if (className.equals(importEnd) || className.startsWith(importEnd + ".")) {
      // The import supplies the prefix, such as in
      //   import a.b.C;
      //   C.D myvar;
      return importName + className.substring(importEnd.length());
    } else {
      // A single-type import makes available only the simple name of the type that it imports, so
      // it cannot supply a prefix for any other name.
      return null;
    }
  }

  /**
   * Finds the {@link Class} corresponding to a name.
   *
   * @param className a class name
   * @return the {@link Class} object corresponding to {@code className}, or null if none is found
   *     or a class that it depends on is not found
   */
  private static @Nullable Class<?> loadClass(@ClassGetName String className) {
    assert className != null;
    try {
      return Class.forName(className, false, ToIndexFileConverter.class.getClassLoader());
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      // A NoClassDefFoundError means that the class exists but cannot be used, because a class
      // that it depends on, such as a supertype, is not on the classpath.  Treat it the same as a
      // class that does not exist, rather than aborting the whole conversion.  Other linkage
      // errors, such as UnsupportedClassVersionError, indicate a misconfiguration, so they
      // propagate.
      return null;
    }
  }
}
