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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  // The possessive modifiers "*+" are for efficiency only.
  // private static Pattern packagePattern =
  //         Pattern.compile("\\bpackage *+((?:[^.]*+[.] *+)*+[^ ]*) *+;");
  /**
   * A pattern that matches an import statement. Its group 1 matches the imported name: a fully
   * qualified type name such as {@code java.util.Map.Entry} for a single-type import, or a name
   * followed by {@code .*} such as {@code java.util.*} for an import-on-demand. For a static
   * import, group 1 starts with {@code static}, so callers skip static imports before matching.
   */
  private static final Pattern importPattern =
      Pattern.compile("\\bimport *+((?:[^.]*+[.] *+)*+[^ ]*) *+;");

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
   * Package name that is active at the current point in the input file. Changes as package
   * declarations are encountered. Null if the input file has no package declaration.
   */
  private final @Nullable @DotSeparatedIdentifiers String pkgName;

  /** Single-type imports that appear in the stub file, such as {@code java.util.List}. */
  private final List<String> singleTypeImports;

  /** Imports-on-demand that appear in the stub file, such as {@code java.util.*}. */
  private final List<String> onDemandImports;

  /**
   * A scene that is read from the input JAIF file and that will be written to the output JAIF file.
   */
  private final AScene scene;

  /**
   * All the compilation units of the stub file, including the one that this converter converts. A
   * stub file has one compilation unit per {@code package} declaration, so other compilation units
   * can declare types in other packages. Empty if they are not known.
   */
  private List<CompilationUnit> compilationUnits = Collections.emptyList();

  /**
   * A cache for {@link #getTargets}. Maps the binary name of an annotation interface to the element
   * types in its {@code @Target} meta-annotation, or to null if they cannot be determined.
   */
  private final Map<String, @Nullable List<ElementType>> getTargetsCache = new HashMap<>();

  /**
   * A cache for {@link #resolve}. Maps a class name, as written in the stub file, to the binary
   * name that it identifies, or to null if resolution fails.
   */
  private final Map<String, @Nullable @BinaryName String> resolveCache = new HashMap<>();

  /**
   * A cache for {@link #memberTypeDeclaration}. Maps a the simple name of a simple name type
   * declaration of the stub file to its member types. It is an identity map because a JavaParser
   * node's {@code equals} and {@code hashCode} compare its whole subtree.
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
   * A cache for {@link #inheritedMemberType}. Maps a type declaration of the stub file to a map
   * from the simple name of a member type to the result of {@link #inheritedMemberType}.
   */
  private final IdentityHashMap<TypeDeclaration<?>, Map<String, @Nullable Object>>
      inheritedMemberTypesCache = new IdentityHashMap<>();

  /**
   * Creates a new ToIndexFileConverter.
   *
   * @param pkgDecl the AST node for the package declaration
   * @param importDecls the AST nodes for import declarations
   * @param scene scene for visitor methods to fill in
   */
  @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameAsString
  public ToIndexFileConverter(
      @Nullable PackageDeclaration pkgDecl, List<ImportDeclaration> importDecls, AScene scene) {
    this.scene = scene;
    pkgName = pkgDecl == null ? null : pkgDecl.getNameAsString();
    if (importDecls == null) {
      singleTypeImports = Collections.emptyList();
      onDemandImports = Collections.emptyList();
    } else {
      ArrayList<String> singles = new ArrayList<>(importDecls.size());
      ArrayList<String> onDemands = new ArrayList<>(importDecls.size());
      for (ImportDeclaration decl : importDecls) {
        if (!decl.isStatic()) {
          Matcher m = importPattern.matcher(decl.toString());
          if (m.find()) {
            String s = m.group(1);
            if (s != null) {
              (s.endsWith("*") ? onDemands : singles).add(s);
            }
          }
        }
      }
      singles.trimToSize();
      onDemands.trimToSize();
      singleTypeImports = Collections.unmodifiableList(singles);
      onDemandImports = Collections.unmodifiableList(onDemands);
    }
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
   */
  private static void extractScene(StubUnit iu, AScene scene) {
    for (CompilationUnit cu : iu.getCompilationUnits()) {
      NodeList<TypeDeclaration<?>> typeDecls = cu.getTypes();
      if (typeDecls != null && cu.getPackageDeclaration().isPresent()) {
        List<ImportDeclaration> impDecls = cu.getImports();
        PackageDeclaration pkgDecl = cu.getPackageDeclaration().get();
        ToIndexFileConverter converter = new ToIndexFileConverter(pkgDecl, impDecls, scene);
        converter.compilationUnits = iu.getCompilationUnits();
        String pkgName = converter.pkgName;
        for (TypeDeclaration<?> typeDecl : typeDecls) {
          String name = typeDecl.getNameAsString();
          if (pkgName != null) {
            name = pkgName + "." + name;
          }
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
              name = unresolvedBinaryName(typeName);
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
   *     #enclosingDeclaration} returns it, or null if it denotes no type in scope; not a type
   *     parameter
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
    if (declaration instanceof TypeDeclaration<?> typeDecl) {
      return memberBinaryName(typeDecl, identifiers, 1);
    } else if (declaration instanceof Class<?> clazz) {
      // A member type that an enclosing type of `node` inherits from a classpath supertype.
      return classMemberBinaryName(clazz, identifiers, 1, true);
    }
    String result = packageQualifiedStubBinaryName(node, name);
    return result != null ? result : resolve(name, node.findCompilationUnit().orElse(null));
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
    String binaryName = stubBinaryName(declaration);
    // `declaration` may be in another compilation unit, in another package, than this converter's.
    String pkg =
        declaration
            .findCompilationUnit()
            .flatMap(CompilationUnit::getPackageDeclaration)
            .map(PackageDeclaration::getNameAsString)
            .orElse(null);
    return pkg == null ? binaryName : pkg + "." + binaryName;
  }

  /**
   * If the stub file declares the type whose fully qualified name is {@code fqName}, in any of its
   * compilation units, returns that type's binary name; otherwise returns null.
   *
   * @param cu the stub file's compilation unit that is being converted
   * @param fqName a fully qualified type name, in which a {@code .} separates a nested class from
   *     its enclosing class
   * @return the binary name of {@code fqName}, or null if the stub file does not declare it
   */
  private @Nullable String declaredInStubFile(CompilationUnit cu, String fqName) {
    return declaredInStubFile(cu, fqName, Integer.MAX_VALUE);
  }

  /**
   * If the stub file declares the type whose fully qualified name is {@code fqName}, in any of its
   * compilation units whose package name is at most {@code maxPackageLength} characters long,
   * returns that type's binary name; otherwise returns null.
   *
   * @param cu the stub file's compilation unit that is being converted
   * @param fqName a fully qualified type name, in which a {@code .} separates a nested class from
   *     its enclosing class
   * @param maxPackageLength the maximum length of the package that declares the outermost type that
   *     {@code fqName} names. For example, for an import on demand {@code p.*}, it is the length of
   *     {@code p}, so that the imported name does not name a type in a subpackage of {@code p}.
   * @return the binary name of {@code fqName}, or null if the stub file does not declare it
   */
  private @Nullable String declaredInStubFile(
      CompilationUnit cu, String fqName, int maxPackageLength) {
    String result = declaredInCompilationUnit(cu, fqName, maxPackageLength);
    for (CompilationUnit other : compilationUnits) {
      if (result != null) {
        break;
      }
      // A type in the unnamed package cannot be referred to from another package.
      if (other != cu && other.getPackageDeclaration().isPresent()) {
        result = declaredInCompilationUnit(other, fqName, maxPackageLength);
      }
    }
    return result;
  }

  /**
   * If the stub file declares, in package {@code pkg}, the type that {@code relativeName} names,
   * returns that type's binary name; otherwise returns null. Unlike {@code declaredInStubFile(cu,
   * pkg + "." + relativeName)}, this does not consider subpackages of {@code pkg}: the first
   * identifier of {@code relativeName} names a top-level type of {@code pkg}.
   *
   * @param cu the stub file's compilation unit that is being converted
   * @param pkg a package name
   * @param relativeName a type name relative to {@code pkg}, in which a {@code .} separates a
   *     nested class from its enclosing class
   * @return the binary name of the type, or null if the stub file does not declare it in {@code
   *     pkg}
   */
  private @Nullable String declaredInPackage(CompilationUnit cu, String pkg, String relativeName) {
    List<CompilationUnit> units = new ArrayList<>(compilationUnits.size() + 1);
    units.add(cu);
    units.addAll(compilationUnits);
    for (CompilationUnit unit : units) {
      String unitPkg =
          unit.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse(null);
      if (pkg.equals(unitPkg)) {
        String result = topLevelRelativeBinaryName(unit, relativeName);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  /**
   * If {@code cu} declares the type whose fully qualified name is {@code fqName}, returns that
   * type's binary name; otherwise returns null.
   *
   * @param cu a compilation unit of the stub file
   * @param fqName a fully qualified type name, in which a {@code .} separates a nested class from
   *     its enclosing class
   * @param maxPackageLength the maximum length of the package name of {@code cu}; if it is longer,
   *     the result is null. See {@link #declaredInStubFile(CompilationUnit, String, int)}.
   * @return the binary name of {@code fqName}, or null if {@code cu} does not declare it
   */
  private @Nullable String declaredInCompilationUnit(
      CompilationUnit cu, String fqName, int maxPackageLength) {
    String pkg = cu.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse(null);
    String relativeName;
    if (pkg == null) {
      relativeName = fqName;
    } else if (pkg.length() <= maxPackageLength && fqName.startsWith(pkg + ".")) {
      relativeName = fqName.substring(pkg.length() + 1);
    } else {
      // The name is in some other package, which `cu` does not declare.
      return null;
    }
    // Because `fqName` is qualified by the package name, `relativeName` names a top-level type of
    // `cu` or a member of one.
    return topLevelRelativeBinaryName(cu, relativeName);
  }

  /**
   * If {@code relativeName} names a top-level type of {@code cu}, or a member type, declared or
   * inherited, of one, returns the binary name of that type; otherwise returns null.
   *
   * @param cu a compilation unit of the stub file
   * @param relativeName a type name relative to the package of {@code cu}, in which a {@code .}
   *     separates a nested class from its enclosing class
   * @return the binary name of the type that {@code relativeName} names, or null if there is none
   */
  private @Nullable String topLevelRelativeBinaryName(CompilationUnit cu, String relativeName) {
    String[] identifiers = relativeName.split("\\.", -1);
    Object first = firstDeclarationAt(cu, identifiers, true, true);
    return (first instanceof TypeDeclaration<?> typeDecl)
        ? memberBinaryName(typeDecl, identifiers, 1, false)
        : null;
  }

  /**
   * If {@code typeName} starts with the stub file's package name followed by {@code .}, and the
   * identifier that follows names a top-level type of the stub file, returns the binary name of the
   * type that {@code typeName} names. Otherwise returns null. As for an unqualified name, {@link
   * #memberBinaryName} resolves the identifiers after the top-level type, so {@code p.B.Missing}
   * and {@code B.Missing} have the same binary name.
   *
   * @param node a node in the stub file's AST
   * @param typeName a type name as written in Java source, not necessarily fully qualified, such as
   *     {@code Entry}, {@code Map.Entry}, or {@code java.util.Map.Entry}
   * @return the binary name of {@code typeName}, or null
   */
  private @Nullable String packageQualifiedStubBinaryName(Node node, String typeName) {
    if (pkgName == null || !typeName.startsWith(pkgName + ".")) {
      return null;
    }
    CompilationUnit cu = node.findCompilationUnit().orElse(null);
    if (cu == null) {
      return null;
    }
    String[] identifiers = typeName.substring(pkgName.length() + 1).split("\\.", -1);
    Object first = firstDeclarationAt(cu, identifiers, true, true);
    return (first instanceof TypeDeclaration<?> typeDecl)
        ? memberBinaryName(typeDecl, identifiers, 1)
        : null;
  }

  /**
   * Returns the type that the first element of {@code identifiers} names in the innermost scope,
   * enclosing {@code node}, that contains it. The scopes searched are those of the declarations
   * that enclose {@code node} and of its compilation unit's top-level types. A class's scope
   * contains the member types that it inherits from its supertypes. Once the first identifier names
   * a type, the caller resolves the remaining identifiers within that type, without searching outer
   * scopes.
   *
   * @param node a node in the stub file's AST
   * @param identifiers a type name that has been split at its {@code .} separators
   * @return the type that the first element of {@code identifiers} names, as {@link
   *     #firstDeclarationAt} returns it, or null
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
   *     or {@link TypeDeclaration} of the stub file, a {@link Class} on the classpath that {@code
   *     n} inherits as a member type, or null
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
      // A type's members include the member types that it inherits (JLS 8.5), which shadow
      // types in outer scopes and imports (JLS 6.4.1).
      TypeDeclaration<?> member = memberTypeDeclaration(typeDecl, identifiers[0]);
      return member != null ? member : inheritedMemberType(typeDecl, identifiers[0]);
    } else if (n instanceof CompilationUnit compilationUnit) {
      for (TypeDeclaration<?> topLevel : compilationUnit.getTypes()) {
        if (topLevel.getNameAsString().equals(identifiers[0])) {
          return topLevel;
        }
      }
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
              Map<String, TypeDeclaration<?>> memberTypes = new HashMap<>();
              for (BodyDeclaration<?> member : d.getMembers()) {
                if (member instanceof TypeDeclaration<?> memberType) {
                  memberTypes.putIfAbsent(memberType.getNameAsString(), memberType);
                }
              }
              return memberTypes;
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
   * @param cu the stub file's compilation unit, or null not to consider the types that the stub
   *     file declares
   * @return binary name of class that {@code className} identifies in the current context, or null
   *     if resolution fails
   */
  private @Nullable @BinaryName String resolve(
      @FullyQualifiedName String className, @Nullable CompilationUnit cu) {
    // The cache holds results for this converter's compilation unit, not for a null one.
    if (cu == null) {
      return resolveUncached(className, null);
    }
    if (resolveCache.containsKey(className)) {
      return resolveCache.get(className);
    }
    String result = resolveUncached(className, cu);
    resolveCache.put(className, result);
    return result;
  }

  /**
   * Returns the binary name of the class with the given name, without consulting the cache.
   *
   * @param className a type name as written in Java source, not necessarily fully qualified, such
   *     as {@code Entry}, {@code Map.Entry}, or {@code java.util.Map.Entry}
   * @param cu the stub file's compilation unit, or null not to consider the types that the stub
   *     file declares
   * @return binary name of class that {@code className} identifies in the current context, or null
   *     if resolution fails
   */
  private @Nullable @BinaryName String resolveUncached(
      @FullyQualifiedName String className, @Nullable CompilationUnit cu) {
    // The order of the lookups below is the order in which Java resolves a type name: a
    // single-type import shadows a type in the current package, which shadows a type that an
    // import-on-demand declaration makes available.

    for (String declName : singleTypeImports) {
      String qualifiedName = mergeImport(declName, className);
      if (qualifiedName != null) {
        // The import determines the type that `className` refers to, even if that type cannot be
        // loaded, so do not look further.  If it cannot be loaded, `unresolvedBinaryName` uses
        // the import.
        return classBinaryName(qualifiedName, cu);
      }
    }
    if (pkgName != null) {
      // The first identifier of `className` names a type in the current package, not a
      // subpackage, so every later identifier names a member type.  Therefore, search only the
      // stub file's declarations in the current package itself.
      @SuppressWarnings("signature") // the stub file's declarations determine the binary name
      @BinaryName String declared = (cu == null) ? null : declaredInPackage(cu, pkgName, className);
      if (declared != null) {
        return declared;
      }
      @SuppressWarnings("signature") // a type in a package, followed by its member types
      @BinaryName String binaryName = pkgName + "." + className.replace('.', '$');
      if (loadClass(binaryName) != null) {
        return binaryName;
      }
    }

    for (String declName : onDemandImports) {
      if (declName.endsWith(".*")) {
        String binaryName =
            onDemandImportBinaryName(declName.substring(0, declName.length() - 2), className, cu);
        if (binaryName != null) {
          return binaryName;
        }
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
   * <p>An import on demand makes available the member types of a package or type, not its
   * subpackages, so the first identifier of {@code className} names a type. For a type that the
   * stub file declares, the package that declares it is therefore no longer than {@code prefix}.
   * The type that {@code className} names may be a member type that the type {@code prefix}
   * inherits, perhaps from a supertype in another package.
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
    String qualifiedName = prefix + "." + className;
    if (cu != null) {
      @SuppressWarnings("signature") // the stub file's declarations determine the binary name
      @BinaryName String declared = declaredInStubFile(cu, qualifiedName, prefix.length());
      if (declared != null) {
        return declared;
      }
    }
    String binaryName = loadClassBinaryName(qualifiedName);
    // No `.` follows the part of `binaryName` that comes from the import.
    return (binaryName != null && binaryName.lastIndexOf('.') <= prefix.length())
        ? binaryName
        : null;
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
   * remaining identifier names a member type that the stub file declares, or that a type inherits
   * from a supertype. If an identifier names no member type of the type that the preceding
   * identifiers name, the result is that type's binary name followed by the identifier and the ones
   * after it, each preceded by {@code $}. For example, if the stub file declares a class {@code B}
   * in package {@code p}, and {@code B} has no member type {@code Missing}, then the result for
   * {@code B.Missing.X} is {@code p.B$Missing$X}.
   *
   * @param declaration a type declaration in the stub file
   * @param identifiers a type name that has been split at its {@code .} separators
   * @param start the index of the first element of {@code identifiers} that is a member of {@code
   *     declaration}
   * @return the binary name of the type that {@code identifiers} names
   */
  private String memberBinaryName(TypeDeclaration<?> declaration, String[] identifiers, int start) {
    String result = memberBinaryName(declaration, identifiers, start, true);
    assert result != null : "@AssumeAssertion(nullness): fallback is true";
    return result;
  }

  /**
   * Returns the binary name of the type that {@code identifiers} names, where {@code declaration}
   * is the type that the elements of {@code identifiers} before index {@code start} name. Each
   * remaining identifier names a member type that the stub file declares, or that a type inherits
   * from a supertype.
   *
   * @param declaration a type declaration in the stub file
   * @param identifiers a type name that has been split at its {@code .} separators
   * @param start the index of the first element of {@code identifiers} that is a member of {@code
   *     declaration}
   * @param fallback what to do if a member type cannot be found: if true, form the result from the
   *     binary name of the innermost type found; if false, return null
   * @return the binary name of the type that {@code identifiers} names, or null if {@code fallback}
   *     is false and a member type cannot be found
   */
  private @Nullable String memberBinaryName(
      TypeDeclaration<?> declaration, String[] identifiers, int start, boolean fallback) {
    if (start == identifiers.length) {
      return qualifiedStubBinaryName(declaration);
    }
    TypeDeclaration<?> member = memberTypeDeclaration(declaration, identifiers[start]);
    if (member != null) {
      return memberBinaryName(member, identifiers, start + 1, fallback);
    }
    Object inherited = inheritedMemberType(declaration, identifiers[start]);
    if (inherited instanceof TypeDeclaration<?> inheritedDecl) {
      return memberBinaryName(inheritedDecl, identifiers, start + 1, fallback);
    } else if (inherited instanceof Class<?> inheritedClass) {
      return classMemberBinaryName(inheritedClass, identifiers, start + 1, fallback);
    }
    if (!fallback) {
      return null;
    }
    return qualifiedStubBinaryName(declaration)
        + "$"
        + String.join("$", Arrays.copyOfRange(identifiers, start, identifiers.length));
  }

  /**
   * Returns the binary name of the type that {@code identifiers} names, where {@code clazz} is the
   * type that the elements of {@code identifiers} before index {@code start} name. Each remaining
   * identifier names a member type, declared or inherited, of a class on the classpath.
   *
   * @param clazz a class on the classpath
   * @param identifiers a type name that has been split at its {@code .} separators
   * @param start the index of the first element of {@code identifiers} that is a member of {@code
   *     clazz}
   * @param fallback what to do if a member type cannot be found: if true, form the result from the
   *     binary name of the innermost type found; if false, return null
   * @return the binary name of the type that {@code identifiers} names, or null if {@code fallback}
   *     is false and a member type cannot be found
   */
  private static @Nullable String classMemberBinaryName(
      Class<?> clazz, String[] identifiers, int start, boolean fallback) {
    for (int i = start; i < identifiers.length; i++) {
      Class<?> member = classMemberType(clazz, identifiers[i]);
      if (member == null) {
        if (!fallback) {
          return null;
        }
        return clazz.getName()
            + "$"
            + String.join("$", Arrays.copyOfRange(identifiers, i, identifiers.length));
      }
      clazz = member;
    }
    return clazz.getName();
  }

  /**
   * A value for the package of an inheritance path, in {@link #classMemberType}, whose classes are
   * not all in one package. It is not the name of any package.
   */
  private static final String MIXED_PACKAGES = "<mixed packages>";

  /**
   * Returns the non-private member type named {@code identifier} that a class on the classpath
   * declares or inherits.
   *
   * @param clazz a class on the classpath
   * @param identifier the simple name of a member type
   * @return the member type named {@code identifier}, or null if there is none or it cannot be
   *     determined
   */
  private static @Nullable Class<?> classMemberType(Class<?> clazz, String identifier) {
    return classMemberType(clazz, identifier, null);
  }

  /**
   * Returns the member type named {@code identifier} that a class on the classpath declares or
   * inherits, and that is inherited by the subclasses of {@code clazz} along an inheritance path.
   *
   * <p>A class inherits a member type of a supertype only if the member type is not private and is
   * accessible in the class (JLS 8.5). Therefore, a package-private member type is inherited along
   * an inheritance path only if every class on the path is in the member type's package.
   *
   * @param clazz a class on the classpath
   * @param identifier the simple name of a member type
   * @param subclassPackage the package of every subclass along the inheritance path that inherits
   *     the member types of {@code clazz}, {@link #MIXED_PACKAGES} if they are not all in one
   *     package, or null if {@code clazz} is the type whose member is sought; the unnamed package
   *     is {@code ""}
   * @return the member type named {@code identifier}, or null if there is none or it cannot be
   *     determined
   */
  private static @Nullable Class<?> classMemberType(
      Class<?> clazz, String identifier, @Nullable String subclassPackage) {
    List<Class<?>> worklist = new ArrayList<>();
    // For each element of `worklist`, the package of every class on the inheritance path from
    // `clazz` to it, excluding the element itself, as for `subclassPackage`.
    List<@Nullable String> pathPackages = new ArrayList<>();
    Set<Class<?>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    worklist.add(clazz);
    pathPackages.add(subclassPackage);
    // A class is reached only along the chain of superclasses, which is unique, so the first path
    // to a class is the only one.  An interface may be reached along several paths, but its member
    // types are public, so the path does not matter.
    for (int i = 0; i < worklist.size(); i++) {
      Class<?> c = worklist.get(i);
      String pathPackage = pathPackages.get(i);
      if (!seen.add(c)) {
        continue;
      }
      try {
        String cPackage = c.getPackageName();
        boolean packagePrivateInherited = pathPackage == null || pathPackage.equals(cPackage);
        for (Class<?> member : c.getDeclaredClasses()) {
          // A private member type is not inherited.  A private member type that `clazz` itself
          // declares is skipped too, but the caller's fallback yields the same binary name.
          if (member.getSimpleName().equals(identifier)
              && isInheritedMemberType(c, member, packagePrivateInherited)) {
            return member;
          }
        }
        String nextPathPackage = packagePrivateInherited ? cPackage : MIXED_PACKAGES;
        Class<?> superclass = c.getSuperclass();
        if (superclass != null) {
          worklist.add(superclass);
          pathPackages.add(nextPathPackage);
        }
        for (Class<?> iface : c.getInterfaces()) {
          worklist.add(iface);
          pathPackages.add(nextPathPackage);
        }
      } catch (NoClassDefFoundError | SecurityException e) {
        // A class that `c` depends on is not on the classpath; skip `c`.
      }
    }
    return null;
  }

  /**
   * Returns true if a member type of a class on the classpath is inherited along an inheritance
   * path.
   *
   * @param c a class on the classpath
   * @param member a member type that {@code c} declares
   * @param packagePrivateInherited whether a package-private member type of {@code c} is inherited
   *     along the inheritance path
   * @return true if {@code member} is inherited along the inheritance path
   */
  private static boolean isInheritedMemberType(
      Class<?> c, Class<?> member, boolean packagePrivateInherited) {
    if (c.isInterface()) {
      // A member type of an interface is implicitly public.
      return true;
    }
    int modifiers = member.getModifiers();
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      return false;
    }
    return packagePrivateInherited
        || java.lang.reflect.Modifier.isPublic(modifiers)
        || java.lang.reflect.Modifier.isProtected(modifiers);
  }

  /**
   * Returns the non-private member type named {@code identifier} that a type declaration of the
   * stub file inherits from its supertypes.
   *
   * @param declaration a type declaration in the stub file
   * @param identifier the simple name of a member type
   * @return the inherited member type: a {@link TypeDeclaration} if the stub file declares it, a
   *     {@link Class} if it is on the classpath, or null if there is none or it cannot be
   *     determined
   */
  private @Nullable Object inheritedMemberType(TypeDeclaration<?> declaration, String identifier) {
    // Only a result computed outside any other call is cached:  a nested call's result can be
    // incomplete, if cyclic inheritance cut its search short.
    boolean outermost = inheritanceVisiting.isEmpty();
    Map<String, @Nullable Object> cache =
        inheritedMemberTypesCache.computeIfAbsent(declaration, d -> new HashMap<>());
    if (outermost && cache.containsKey(identifier)) {
      return cache.get(identifier);
    }
    Object result = inheritedMemberTypeUncached(declaration, identifier);
    if (outermost) {
      cache.put(identifier, result);
    }
    return result;
  }

  /**
   * Returns the non-private member type named {@code identifier} that a type declaration of the
   * stub file inherits from its supertypes, without consulting the cache.
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
      return null;
    }
    try {
      for (ClassOrInterfaceType supertype : supertypes(declaration)) {
        Object superDecl = resolveSupertype(supertype);
        if (superDecl instanceof TypeDeclaration<?> superTypeDecl) {
          TypeDeclaration<?> member = memberTypeDeclaration(superTypeDecl, identifier);
          if (member != null && !member.isPrivate()) {
            return member;
          }
          Object inherited = inheritedMemberType(superTypeDecl, identifier);
          if (inherited != null) {
            return inherited;
          }
        } else if (superDecl instanceof Class<?> superClass) {
          Class<?> member = classMemberType(superClass, identifier, stubPackage(declaration));
          if (member != null) {
            return member;
          }
        }
      }
      if (declaration instanceof EnumDeclaration) {
        // The implicit superclass of an enum declares the member type `Enum.EnumDesc`.
        return classMemberType(Enum.class, identifier, stubPackage(declaration));
      }
      return null;
    } finally {
      inheritanceVisiting.remove(declaration);
    }
  }

  /**
   * Returns the package of a type declaration of the stub file, in the form that {@link
   * Class#getPackageName} returns.
   *
   * @param declaration a type declaration in the stub file
   * @return the package of {@code declaration}, or {@code ""} for the unnamed package
   */
  private static String stubPackage(TypeDeclaration<?> declaration) {
    // `declaration` may be in another compilation unit, in another package, than this converter's.
    return declaration
        .findCompilationUnit()
        .flatMap(CompilationUnit::getPackageDeclaration)
        .map(PackageDeclaration::getNameAsString)
        .orElse("");
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
   * <p>The declaration may be in any compilation unit of the stub file whose package is the package
   * of {@code binaryName}. A type in the unnamed package is sought only in {@code node}'s
   * compilation unit, because a type in the unnamed package cannot be referred to from another
   * package.
   *
   * @param node a node in the stub file's AST
   * @param binaryName a binary name
   * @return the type declaration of the stub file whose binary name is {@code binaryName}, or null
   *     if there is none
   */
  private @Nullable TypeDeclaration<?> stubDeclarationOfBinaryName(Node node, String binaryName) {
    int lastDot = binaryName.lastIndexOf('.');
    String pkg = lastDot < 0 ? null : binaryName.substring(0, lastDot);
    String[] identifiers = binaryName.substring(lastDot + 1).split("\\$", -1);
    CompilationUnit nodeCu = node.findCompilationUnit().orElse(null);
    List<CompilationUnit> units = new ArrayList<>(compilationUnits.size() + 1);
    if (nodeCu != null) {
      units.add(nodeCu);
    }
    if (pkg != null) {
      units.addAll(compilationUnits);
    }
    for (CompilationUnit unit : units) {
      String unitPkg =
          unit.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse(null);
      if (!Objects.equals(pkg, unitPkg)) {
        continue;
      }
      TypeDeclaration<?> result = null;
      for (TypeDeclaration<?> topLevel : unit.getTypes()) {
        if (topLevel.getNameAsString().equals(identifiers[0])) {
          result = topLevel;
          break;
        }
      }
      for (int i = 1; i < identifiers.length && result != null; i++) {
        result = memberTypeDeclaration(result, identifiers[i]);
      }
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the binary name that a type name most likely refers to, for a type name that cannot be
   * resolved. Because such a name does not indicate which of its dot-separated components are
   * package names and which are class names, this method uses the Java convention that a package
   * name starts with a lowercase letter and a class name starts with an uppercase letter. A name
   * whose first identifier a single-type import names is qualified by that import. Any other name
   * with no package name is assumed to refer to the stub file's own package.
   *
   * @param typeName a type name that resolution failed on
   * @return the binary name that {@code typeName} most likely refers to
   */
  private String unresolvedBinaryName(@FullyQualifiedName String typeName) {
    for (String declName : singleTypeImports) {
      String qualifiedName = mergeImport(declName, typeName);
      // An unchanged name is an import of a type in the unnamed package, which gives no package.
      if (qualifiedName != null && !qualifiedName.equals(typeName)) {
        // The import is fully qualified, so do not qualify the result by the stub file's package.
        return splitBinaryName(qualifiedName, null);
      }
    }
    return splitBinaryName(typeName, pkgName);
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
   * Combines an import with a partially qualified name, yielding a fully qualified name.
   *
   * @param importName package name or (for an inner class) the outer class name
   * @param className the class name
   * @return fully qualified class name if resolution succeeds, null otherwise
   */
  @SuppressWarnings("signature") // string manipulation of signature strings
  private static @Nullable @FullyQualifiedName String mergeImport(
      String importName, @FullyQualifiedName String className) {
    if (importName.isEmpty() || importName.equals(className)) {
      return className;
    }
    String[] importSplit = importName.split("\\.");
    String importEnd = importSplit[importSplit.length - 1];
    if ("*".equals(importEnd)) {
      return importName.substring(0, importName.length() - 1) + className;
    } else if (className.equals(importEnd) || className.startsWith(importEnd + ".")) {
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
