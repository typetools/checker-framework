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
import java.util.List;
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
import org.checkerframework.checker.signature.qual.Identifier;
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

  /** A scene read from the input JAIF file, and will be written to the output JAIF file. */
  private final AScene scene;

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
        for (TypeDeclaration<?> typeDecl : typeDecls) {
          ToIndexFileConverter converter = new ToIndexFileConverter(pkgDecl, impDecls, scene);
          String pkgName = converter.pkgName;
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
    // A nested class's members belong to the nested class, not to the class that encloses it.
    AClass clazz = classElement(decl);
    visitDecl(decl, clazz);
    return super.visit(decl, clazz);
  }

  @Override
  public Void visit(ConstructorDeclaration decl, AElement elem) {
    List<Parameter> params = decl.getParameters();
    List<AnnotationExpr> rcvrAnnos = decl.getAnnotations();
    BlockStmt body = decl.getBody();
    StringBuilder sb = new StringBuilder("<init>(");
    AClass clazz = (AClass) elem;
    AMethod method;

    // Some of the methods in the generated parser use null to represent an empty list.
    if (params != null) {
      for (Parameter param : params) {
        sb.append(getJVML(param));
      }
    }
    sb.append(")V");
    method = clazz.methods.getVivify(sb.toString());
    visitDecl(decl, method);
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        visitParameter(params.get(i), method.parameters.getVivify(i));
      }
    }
    if (rcvrAnnos != null) {
      for (AnnotationExpr expr : rcvrAnnos) {
        Annotation anno = extractAnnotation(expr);
        method.receiver.tlAnnotationsHere.add(anno);
      }
    }
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
    AClass clazz = classElement(decl);
    visitDecl(decl, clazz);
    return super.visit(decl, clazz);
  }

  @Override
  public Void visit(RecordDeclaration decl, AElement elem) {
    AClass clazz = classElement(decl);
    visitDecl(decl, clazz);
    return super.visit(decl, clazz);
  }

  @Override
  public Void visit(FieldDeclaration decl, AElement elem) {
    for (VariableDeclarator v : decl.getVariables()) {
      AClass clazz = (AClass) elem;
      AField field = clazz.fields.getVivify(v.getNameAsString());
      visitDecl(decl, field);
      visitType(decl.getCommonType(), field.type);
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
    Optional<ReceiverParameter> rcvrParam = decl.getReceiverParameter();
    BlockStmt body = decl.getBody().orElse(null);
    StringBuilder sb = new StringBuilder(decl.getNameAsString()).append('(');
    AClass clazz = (AClass) elem;
    AMethod method;
    if (params != null) {
      for (Parameter param : params) {
        sb.append(getJVML(param));
      }
    }
    sb.append(')').append(getJVML(type));
    method = clazz.methods.getVivify(sb.toString());
    visitDecl(decl, method);
    visitType(type, method.returnType);
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        visitParameter(params.get(i), method.parameters.getVivify(i));
      }
    }
    if (rcvrParam.isPresent()) {
      for (AnnotationExpr expr : rcvrParam.get().getAnnotations()) {
        Annotation anno = extractAnnotation(expr);
        method.receiver.type.tlAnnotationsHere.add(anno);
      }
    }
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
      visitType(expr.getCommonType(), field.type);
      if (annos != null) {
        for (AnnotationExpr annoExpr : annos) {
          Annotation anno = extractAnnotation(annoExpr);
          field.tlAnnotationsHere.add(anno);
        }
      }
    }
    return null;
  }

  /**
   * Returns the scene's element for the class that {@code decl} declares, creating the element if
   * the scene does not yet contain it.
   *
   * @param decl a type declaration in the stub file
   * @return the scene's element for {@code decl}
   */
  private AClass classElement(TypeDeclaration<?> decl) {
    return scene.classes.getVivify(qualifiedStubBinaryName(decl));
  }

  /**
   * Copies information from an AST declaration node to an {@link ADeclaration}. Called by visitors
   * for BodyDeclaration subclasses.
   */
  private Void visitDecl(BodyDeclaration<?> decl, ADeclaration elem) {
    NodeList<AnnotationExpr> annoExprs = decl.getAnnotations();
    if (annoExprs != null) {
      for (AnnotationExpr annoExpr : annoExprs) {
        Annotation anno = extractAnnotation(annoExpr);
        elem.tlAnnotationsHere.add(anno);
      }
    }
    return null;
  }

  /**
   * Copies information from a formal parameter to an {@link AField}.
   *
   * @param param a formal parameter
   * @param field the scene element for {@code param}
   */
  private void visitParameter(Parameter param, AField field) {
    // For a varargs parameter, `getType()` is the element type, and the annotations that precede
    // the `...` apply to the array type.  Wrap a copy of the element type, because making a node
    // the component of an array type would remove it from the parameter.
    Type type = param.isVarArgs() ? new ArrayType(param.getType().clone()) : param.getType();
    visitType(type, field.type);
    if (param.isVarArgs()) {
      for (AnnotationExpr expr : param.getVarArgsAnnotations()) {
        Annotation anno = extractAnnotation(expr);
        if (anno != null) {
          field.type.tlAnnotationsHere.add(anno);
        }
      }
    }

    // An annotation that precedes the parameter's type is a declaration annotation, a type
    // annotation, or both, according to its `@Target` (JLS 9.7.4).  As a type annotation, it
    // applies to the innermost component type of an array type:  in `@A String[]`, `@A` annotates
    // `String`.  An annotation whose `@Target` cannot be determined is recorded as a declaration
    // annotation, as for a field.
    List<TypePathEntry> elementLoc =
        new ArrayList<>(Collections.nCopies(type.getArrayLevel(), TypePathEntry.ARRAY_ELEMENT));
    for (AnnotationExpr expr : param.getAnnotations()) {
      Annotation anno = extractAnnotation(expr);
      if (anno == null) {
        continue;
      }
      List<ElementType> targets = getTargets(expr);
      if (targets == null || targets.contains(ElementType.PARAMETER)) {
        field.tlAnnotationsHere.add(anno);
      }
      if (targets != null && targets.contains(ElementType.TYPE_USE)) {
        ATypeElement elementType =
            elementLoc.isEmpty() ? field.type : field.type.innerTypes.getVivify(elementLoc);
        elementType.tlAnnotationsHere.add(anno);
      }
    }
  }

  /**
   * Returns the {@code @Target} meta-annotation of the annotation interface that an annotation
   * instantiates.
   *
   * @param expr an annotation
   * @return the element types in the {@code @Target} meta-annotation of {@code expr}'s annotation
   *     interface, or null if that interface cannot be loaded or has no {@code @Target}
   */
  private @Nullable List<ElementType> getTargets(AnnotationExpr expr) {
    @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameAsString
    @BinaryName String name = expr.getNameAsString();
    String qualifiedName = resolve(name);
    Class<?> annoClass = qualifiedName == null ? null : loadClass(qualifiedName);
    Target target = annoClass == null ? null : annoClass.getAnnotation(Target.class);
    return target == null ? null : Arrays.asList(target.value());
  }

  /**
   * Copies information from an AST type node to an {@link ATypeElement}.
   *
   * @param type the AST Type node to inspect
   * @param elem destination type element
   * @return null
   */
  private Void visitType(Type type, ATypeElement elem) {
    List<AnnotationExpr> exprs = type.getAnnotations();
    if (exprs != null) {
      for (AnnotationExpr expr : exprs) {
        Annotation anno = extractAnnotation(expr);
        if (anno != null) {
          elem.tlAnnotationsHere.add(anno);
        }
      }
    }
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
   * Computes a formal parameter's JVML descriptor.
   *
   * @param param a formal parameter
   * @return the JVML descriptor of {@code param}'s type
   */
  private String getJVML(Parameter param) {
    // For a varargs parameter, `getType()` returns the element type rather than the array type.
    return (param.isVarArgs() ? "[" : "") + getJVML(param.getType());
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
            @Identifier String typeName = type.getNameAsString();
            if (!type.getScope().isPresent()) {
              TypeParameter typeParam = typeParameterInScope(type, typeName);
              if (typeParam != null) {
                // The JVML descriptor uses the erasure, which is the first bound, or Object if
                // the type parameter has no bound.
                NodeList<ClassOrInterfaceType> bounds = typeParam.getTypeBound();
                return bounds.isEmpty() ? "Ljava/lang/Object;" : bounds.get(0).accept(this, null);
              }
              String stubName = stubDeclaredTypeInScope(type, typeName);
              if (stubName != null) {
                return "L" + stubName.replace('.', '/') + ";";
              }
            }
            @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameWithScope
            @BinaryName String qualifiedTypeName = type.getNameWithScope();
            String name = resolve(qualifiedTypeName);
            if (name == null) {
              return "L" + qualifiedTypeName.replace('.', '/') + ";";
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
   * Returns the type parameter named {@code name} that is in scope at {@code node}, or null if no
   * such type parameter is in scope.
   *
   * @param node a node in the stub file's AST
   * @param name a type name, without type arguments
   * @return the type parameter that {@code name} refers to at {@code node}, or null
   */
  private static @Nullable TypeParameter typeParameterInScope(Node node, String name) {
    for (Node n = node; n != null; n = n.getParentNode().orElse(null)) {
      if (n instanceof NodeWithTypeParameters) {
        for (TypeParameter typeParam : ((NodeWithTypeParameters<?>) n).getTypeParameters()) {
          if (typeParam.getNameAsString().equals(name)) {
            return typeParam;
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns the binary name of the type named {@code name} that the stub file declares and that is
   * in scope at {@code node}, or null if there is no such type. Such a type is a member of a type
   * that encloses {@code node}, or a top-level type in {@code node}'s compilation unit. A
   * single-type import may not import a type with the same simple name as a top-level type in the
   * same compilation unit (JLS 7.5.1), so such a type shadows every import.
   *
   * @param node a node in the stub file's AST
   * @param name a simple type name
   * @return the binary name of the stub-declared type that {@code name} refers to at {@code node},
   *     or null
   */
  private @Nullable String stubDeclaredTypeInScope(Node node, String name) {
    for (Node n = node; n != null; n = n.getParentNode().orElse(null)) {
      List<? extends Node> members;
      if (n instanceof TypeDeclaration<?>) {
        members = ((TypeDeclaration<?>) n).getMembers();
      } else if (n instanceof CompilationUnit) {
        members = ((CompilationUnit) n).getTypes();
      } else {
        continue;
      }
      for (Node member : members) {
        if (member instanceof TypeDeclaration<?>
            && ((TypeDeclaration<?>) member).getNameAsString().equals(name)) {
          return qualifiedStubBinaryName((TypeDeclaration<?>) member);
        }
      }
    }
    return null;
  }

  /**
   * Returns the binary name, qualified by the stub file's package, of a type that the stub file
   * declares. For example, if the stub file's package is {@code p} and the stub file declares a
   * top-level class {@code B} containing a nested class {@code C}, then this method maps the
   * declaration of {@code C} to {@code "p.B$C"}.
   *
   * @param declaration a type declaration in the stub file
   * @return the binary name of {@code declaration}
   */
  private String qualifiedStubBinaryName(TypeDeclaration<?> declaration) {
    String binaryName = stubBinaryName(declaration);
    return pkgName == null ? binaryName : pkgName + "." + binaryName;
  }

  /**
   * Returns the binary name, without its package, of a type that the stub file declares.
   *
   * @param declaration a type declaration in a stub file
   * @return the binary name of {@code declaration}, without its package
   */
  private static String stubBinaryName(TypeDeclaration<?> declaration) {
    StringBuilder binaryName = new StringBuilder(declaration.getNameAsString());
    for (Node n = declaration.getParentNode().orElse(null);
        n != null;
        n = n.getParentNode().orElse(null)) {
      if (n instanceof TypeDeclaration<?>) {
        binaryName.insert(0, ((TypeDeclaration<?>) n).getNameAsString() + "$");
      }
    }
    return binaryName.toString();
  }

  /**
   * Finds the fully qualified name of the class with the given name.
   *
   * @param className possibly unqualified name of class
   * @return fully qualified name of class that {@code className} identifies in the current context,
   *     or null if resolution fails
   */
  private @Nullable @BinaryName String resolve(@BinaryName String className) {
    // Follow the precedence of JLS 6.4.1: a single-type import shadows a class of the same name in
    // the current package, which in turn shadows a class imported on demand.  `java.lang` is
    // imported on demand implicitly.

    for (String declName : imports) {
      if (!declName.endsWith("*")) {
        String qualifiedName = mergeImport(declName, className);
        if (qualifiedName != null && loadClass(qualifiedName) != null) {
          return qualifiedName;
        }
      }
    }

    // The order of the lookups below is the order in which Java resolves a type name: a
    // single-type import shadows a type in the current package, which shadows a type that an
    // import-on-demand declaration makes available.

    for (String declName : singleTypeImports) {
      String qualifiedName = mergeImport(declName, className);
      String binaryName = qualifiedName == null ? null : loadableBinaryName(qualifiedName);
      if (binaryName != null) {
        return binaryName;
      }
    }

    if (pkgName != null) {
      String qualifiedName = Signatures.addPackage(pkgName, className);
      String binaryName = loadableBinaryName(qualifiedName);
      if (binaryName != null) {
        return binaryName;
      }
    }

    for (String declName : onDemandImports) {
      String qualifiedName = mergeImport(declName, className);
      String binaryName = qualifiedName == null ? null : loadableBinaryName(qualifiedName);
      if (binaryName != null) {
        return binaryName;
      }
    }

    {
      String qualifiedName = Signatures.addPackage("java.lang", className);
      String binaryName = loadableBinaryName(qualifiedName);
      if (binaryName != null) {
        return binaryName;
      }
    }

    return loadableBinaryName(className);
  }

  /**
   * Returns the binary name of the loadable class that {@code name} refers to, or null if there is
   * none. A nested class's name uses {@code .} where its binary name uses {@code $}, as in {@code
   * java.util.Map.Entry} and {@code java.util.Map$Entry}. Therefore, this method tries replacing
   * each {@code .} by {@code $}, starting from the end of {@code name}.
   *
   * @param name a fully qualified name or a binary name
   * @return the binary name of the class that {@code name} refers to, or null
   */
  @SuppressWarnings("signature") // string manipulation of signature strings
  private static @Nullable @BinaryName String loadableBinaryName(String name) {
    String candidate = name;
    while (true) {
      if (loadClass(candidate) != null) {
        return candidate;
      }
      int lastDot = candidate.lastIndexOf('.');
      if (lastDot == -1) {
        return null;
      }
      candidate = candidate.substring(0, lastDot) + "$" + candidate.substring(lastDot + 1);
    }
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
   *     or it cannot be loaded
   */
  private static @Nullable Class<?> loadClass(@ClassGetName String className) {
    assert className != null;
    try {
      return Class.forName(className, false, ToIndexFileConverter.class.getClassLoader());
    } catch (ClassNotFoundException | LinkageError e) {
      // A LinkageError, such as NoClassDefFoundError, means that the class exists but cannot be
      // used -- for example, one of its supertypes is not on the classpath.  Treat it the same
      // as a class that does not exist, rather than aborting the whole conversion.
      return null;
    }
  }
}
