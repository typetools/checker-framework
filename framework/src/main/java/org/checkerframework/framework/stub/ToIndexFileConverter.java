package org.checkerframework.framework.stub;

import com.github.javaparser.ParseException;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
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
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.plumelib.reflection.Signatures;

/**
 * Convert a JAIF file plus a stub file into index files (JAIFs). Note that the resulting index
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
  /** A pattern that matches an import statement. */
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

  /** Imports that appear in the stub file. */
  private final List<String> imports;

  /** A scene read from the input JAIF file, and will be written to the output JAIF file. */
  private final AScene scene;

  /**
   * A cache for {@link #getTargets}. Maps an annotation name, as written in the stub file, to the
   * element types in the {@code @Target} meta-annotation of the annotation interface that the name
   * denotes, or to null if they cannot be determined.
   */
  private final Map<String, @Nullable List<ElementType>> targetsCache = new HashMap<>();

  /**
   * A cache for {@link #resolve}. Maps a class name, as written in the stub file, to the fully
   * qualified name that it identifies, or to null if resolution fails.
   */
  private final Map<String, @Nullable @BinaryName String> resolveCache = new HashMap<>();

  /**
   * Creates a new ToIndexFileConverter.
   *
   * @param pkgDecl the AST node for package declaration
   * @param importDecls the AST nodes for import declarations
   * @param scene scene for visitor methods to fill in
   */
  @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameAsString
  public ToIndexFileConverter(
      @Nullable PackageDeclaration pkgDecl, List<ImportDeclaration> importDecls, AScene scene) {
    this.scene = scene;
    pkgName = pkgDecl == null ? null : pkgDecl.getNameAsString();
    if (importDecls == null) {
      imports = Collections.emptyList();
    } else {
      ArrayList<String> imps = new ArrayList<>(importDecls.size());
      for (ImportDeclaration decl : importDecls) {
        if (!decl.isStatic()) {
          Matcher m = importPattern.matcher(decl.toString());
          if (m.find()) {
            String s = m.group(1);
            if (s != null) {
              imps.add(s);
            }
          }
        }
      }
      imps.trimToSize();
      imports = Collections.unmodifiableList(imps);
    }
  }

  /**
   * Parse stub files and write out equivalent JAIFs. Note that the results do not include
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
   * Augment given scene with information from stubfile, reading stubs from input stream and writing
   * JAIF to output stream.
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
   * Builds simplified annotation from its declaration. Only the name is included, because stubfiles
   * do not generally have access to the full definitions of annotations.
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
        Type paramType = parameterType(param);
        paramTypes.add(paramType);
        sb.append(getJVML(paramType));
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
    return super.visit(decl, field);
  }

  @Override
  public Void visit(EnumDeclaration decl, AElement elem) {
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
        Type paramType = parameterType(param);
        paramTypes.add(paramType);
        sb.append(getJVML(paramType));
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
   *     interface, or null if that interface cannot be loaded or has no {@code @Target}
   */
  private @Nullable List<ElementType> getTargets(AnnotationExpr expr) {
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameAsString
    @BinaryName String name = expr.getNameAsString();
    if (targetsCache.containsKey(name)) {
      return targetsCache.get(name);
    }
    String qualifiedName = resolve(name);
    Class<?> annoClass = qualifiedName == null ? null : loadClass(qualifiedName);
    Target target = annoClass == null ? null : annoClass.getAnnotation(Target.class);
    List<ElementType> result = target == null ? null : Arrays.asList(target.value());
    targetsCache.put(name, result);
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
          @Override
          public String visit(ClassOrInterfaceType type, Void v) {
            @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameAsString
            @FullyQualifiedName String typeName = type.getNameAsString();
            @SuppressWarnings("signature" // TODO:  bug in ToIndexFileConverter:
            // resolve requires a @BinaryName, but this passes a @FullyQualifiedName.
            // They differ for inner classes.
            )
            String name = resolve(typeName);
            if (name == null) {
              // could be defined in the same stub file
              return "L" + typeName + ";";
            }
            return "L" + String.join("/", name.split("\\.")) + ";";
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
   * Finds the fully qualified name of the class with the given name.
   *
   * @param className possibly unqualified name of class
   * @return fully qualified name of class that {@code className} identifies in the current context,
   *     or null if resolution fails
   */
  private @Nullable @BinaryName String resolve(@BinaryName String className) {
    if (resolveCache.containsKey(className)) {
      return resolveCache.get(className);
    }
    String result = resolveUncached(className);
    resolveCache.put(className, result);
    return result;
  }

  /**
   * Finds the fully qualified name of the class with the given name, without consulting the cache.
   *
   * @param className possibly unqualified name of class
   * @return fully qualified name of class that {@code className} identifies in the current context,
   *     or null if resolution fails
   */
  private @Nullable @BinaryName String resolveUncached(@BinaryName String className) {
    if (pkgName != null) {
      String qualifiedName = Signatures.addPackage(pkgName, className);
      if (loadClass(qualifiedName) != null) {
        return qualifiedName;
      }
    }

    {
      // Every Java program implicitly does "import java.lang.*",
      // so see whether this class is in that package.
      String qualifiedName = Signatures.addPackage("java.lang", className);
      if (loadClass(qualifiedName) != null) {
        return qualifiedName;
      }
    }

    for (String declName : imports) {
      String qualifiedName = mergeImport(declName, className);
      if (qualifiedName != null && loadClass(qualifiedName) != null) {
        return qualifiedName;
      }
    }

    if (loadClass(className) != null) {
      return className;
    }

    return null;
  }

  /**
   * Combines an import with a partial binary name, yielding a binary name.
   *
   * @param importName package name or (for an inner class) the outer class name
   * @param className the class name
   * @return fully qualified class name if resolution succeeds, null otherwise
   */
  @SuppressWarnings("signature") // string manipulation of signature strings
  private static @Nullable @BinaryName String mergeImport(
      String importName, @BinaryName String className) {
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
