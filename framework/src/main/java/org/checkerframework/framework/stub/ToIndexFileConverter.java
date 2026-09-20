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
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
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
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
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
   * declarations are encountered.
   */
  private final @DotSeparatedIdentifiers String pkgName;

  /** Single-type imports that appear in the stub file, such as {@code import p.Foo;}. */
  private final List<String> singleTypeImports;

  /** Import-on-demand declarations that appear in the stub file, such as {@code import p.*;}. */
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
        Parameter param = params.get(i);
        AField field = method.parameters.getVivify(i);
        visitType(param.getType(), field.type);
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
    return super.visit(decl, field);
  }

  @Override
  public Void visit(EnumDeclaration decl, AElement elem) {
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
        Parameter param = params.get(i);
        AField field = method.parameters.getVivify(i);
        visitType(param.getType(), field.type);
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

  /** Copies information from an AST type node to an {@link ATypeElement}. */
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
   * Computes a type's "binary name".
   *
   * @param type the type
   * @return the type's binary name
   */
  private String getJVML(Type type) {
    return type.accept(
        new GenericVisitorAdapter<String, Void>() {
          @Override
          public String visit(ClassOrInterfaceType type, Void v) {
            // Use the name together with its scope, so that a qualified name such as
            // `java.util.List` or `Map.Entry` is not truncated to its last identifier.
            @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameWithScope
            @FullyQualifiedName String typeName = type.getNameWithScope();
            // A type parameter, and a type that the stub file declares, shadow an import and a
            // type on the classpath, so look for one before consulting imports and the classpath.
            Node declaration = stubDeclaration(type, typeName, true);
            if (declaration instanceof TypeParameter typeParam) {
              // The JVML descriptor uses the erasure, which is the first bound, or Object if
              // the type parameter has no bound.
              NodeList<ClassOrInterfaceType> bounds = typeParam.getTypeBound();
              return bounds.isEmpty() ? "Ljava/lang/Object;" : bounds.get(0).accept(this, null);
            }
            String name;
            if (declaration != null) {
              name = qualifiedStubBinaryName((TypeDeclaration<?>) declaration);
            } else {
              name = resolve(typeName, type.findCompilationUnit().orElse(null));
              if (name == null) {
                // The type might be a member type that an enclosing class inherits from a class
                // on the classpath.
                name = inheritedFromClasspath(type, typeName);
              }
              if (name == null) {
                name = unresolvedBinaryName(typeName);
              }
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
            return type.getSuperType().get().accept(this, null);
          }
        },
        null);
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
   * If the stub file declares the type whose fully qualified name is {@code fqName}, returns that
   * type's binary name; otherwise returns null.
   *
   * @param cu the stub file's compilation unit
   * @param fqName a fully qualified type name, in which a {@code .} separates a nested class from
   *     its enclosing class
   * @return the binary name of {@code fqName}, or null if the stub file does not declare it
   */
  private @Nullable String declaredInStubFile(CompilationUnit cu, String fqName) {
    String relativeName;
    if (pkgName == null) {
      relativeName = fqName;
    } else if (fqName.startsWith(pkgName + ".")) {
      relativeName = fqName.substring(pkgName.length() + 1);
    } else {
      // The name is in some other package, which the stub file does not declare.
      return null;
    }
    // Because `relativeName` is qualified by the package name, it names a top-level type of the
    // stub file or a member of one.
    Node declaration = stubDeclaration(cu, relativeName, true);
    return (declaration instanceof TypeDeclaration<?>)
        ? qualifiedStubBinaryName((TypeDeclaration<?>) declaration)
        : null;
  }

  /**
   * Returns the declaration, in the stub file, that {@code typeName} names at {@code node}: either
   * a {@link TypeParameter} or a {@link TypeDeclaration}. Returns null if the stub file declares no
   * such type.
   *
   * @param node the node in the stub file's AST at which {@code typeName} appears
   * @param typeName a type name, in which a {@code .} separates a nested class from its enclosing
   *     class
   * @param inherited if true, a class's member types include the ones that it inherits
   * @return the declaration of {@code typeName}, or null
   */
  private static @Nullable Node stubDeclaration(Node node, String typeName, boolean inherited) {
    String[] identifiers = typeName.split("\\.", -1);
    // Search each enclosing declaration, innermost first, and finally the stub file's top-level
    // classes.  That is the order in which Java resolves a type name.  Within one declaration, a
    // type parameter and a member type cannot have the same name.
    for (Node n = node; n != null; n = n.getParentNode().orElse(null)) {
      if (identifiers.length == 1 && n instanceof NodeWithTypeParameters) {
        for (TypeParameter typeParam : ((NodeWithTypeParameters<?>) n).getTypeParameters()) {
          if (typeParam.getNameAsString().equals(identifiers[0])) {
            return typeParam;
          }
        }
      }
      if (n instanceof TypeDeclaration<?>) {
        TypeDeclaration<?> declaration =
            memberTypeDeclaration((TypeDeclaration<?>) n, identifiers[0], inherited, visitedSet());
        if (declaration != null) {
          TypeDeclaration<?> result = nestedTypeDeclaration(declaration, identifiers, inherited);
          if (result != null) {
            return result;
          }
        }
      } else if (n instanceof CompilationUnit) {
        for (TypeDeclaration<?> topLevel : ((CompilationUnit) n).getTypes()) {
          if (topLevel.getNameAsString().equals(identifiers[0])) {
            return nestedTypeDeclaration(topLevel, identifiers, inherited);
          }
        }
        return null;
      }
    }
    return null;
  }

  /**
   * Resolves {@code identifiers}, other than its first element which {@code declaration} names,
   * against the member types of {@code declaration}.
   *
   * @param declaration the declaration that {@code identifiers[0]} names
   * @param identifiers a type name that has been split at its {@code .} separators
   * @param inherited if true, a class's member types include the ones that it inherits
   * @return the declaration that {@code identifiers} names, or null if there is none
   */
  private static @Nullable TypeDeclaration<?> nestedTypeDeclaration(
      TypeDeclaration<?> declaration, String[] identifiers, boolean inherited) {
    TypeDeclaration<?> result = declaration;
    for (int i = 1; i < identifiers.length; i++) {
      result = memberTypeDeclaration(result, identifiers[i], inherited, visitedSet());
      if (result == null) {
        return null;
      }
    }
    return result;
  }

  /**
   * Returns the member type named {@code identifier} that {@code declaration} declares or, if
   * {@code inherited} is true, inherits from a supertype that the stub file also declares.
   *
   * @param declaration a type declaration in a stub file
   * @param identifier the simple name of a member type
   * @param inherited if true, search the supertypes that the stub file declares
   * @param visited the type declarations whose members have already been searched; this method adds
   *     {@code declaration} to it
   * @return the declaration of {@code identifier}, or null if there is none
   */
  private static @Nullable TypeDeclaration<?> memberTypeDeclaration(
      TypeDeclaration<?> declaration,
      String identifier,
      boolean inherited,
      Set<TypeDeclaration<?>> visited) {
    if (!visited.add(declaration)) {
      // The stub file declares a cyclic inheritance hierarchy, which is not legal Java.
      return null;
    }
    for (BodyDeclaration<?> member : declaration.getMembers()) {
      if (member instanceof TypeDeclaration<?>
          && ((TypeDeclaration<?>) member).getNameAsString().equals(identifier)) {
        return (TypeDeclaration<?>) member;
      }
    }
    if (!inherited) {
      return null;
    }
    for (ClassOrInterfaceType supertype : supertypes(declaration)) {
      // Resolving the supertype's name does not consider inherited member types, which guarantees
      // that this method terminates.
      Node supertypeDeclaration = stubDeclaration(declaration, supertype.getNameWithScope(), false);
      if (!(supertypeDeclaration instanceof TypeDeclaration<?>)) {
        // The supertype is not declared in the stub file, so its members are unknown here.
        // `inheritedFromClasspath` searches such a supertype on the classpath.
        continue;
      }
      TypeDeclaration<?> result =
          memberTypeDeclaration(
              (TypeDeclaration<?>) supertypeDeclaration, identifier, true, visited);
      // A private member type is not inherited.
      if (result != null && !result.isPrivate()) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the direct supertypes that {@code declaration}'s {@code extends} and {@code implements}
   * clauses name.
   *
   * @param declaration a type declaration in a stub file
   * @return the direct supertypes of {@code declaration}
   */
  private static List<ClassOrInterfaceType> supertypes(TypeDeclaration<?> declaration) {
    List<ClassOrInterfaceType> result = new ArrayList<>(2);
    if (declaration instanceof NodeWithExtends<?>) {
      result.addAll(((NodeWithExtends<?>) declaration).getExtendedTypes());
    }
    if (declaration instanceof NodeWithImplements<?>) {
      result.addAll(((NodeWithImplements<?>) declaration).getImplementedTypes());
    }
    return result;
  }

  /**
   * Returns a new, empty set that compares type declarations by identity.
   *
   * @return a new, empty set of type declarations
   */
  private static Set<TypeDeclaration<?>> visitedSet() {
    // A set that uses equals() would conflate two structurally identical declarations, because
    // JavaParser's Node.equals() compares the structure of two ASTs.
    return Collections.newSetFromMap(new IdentityHashMap<TypeDeclaration<?>, Boolean>());
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
   * Finds the binary name of the class with the given name.
   *
   * @param className possibly unqualified name of class, in which a {@code .} (not a {@code $})
   *     separates a nested class from its enclosing class
   * @param cu the stub file's compilation unit, or null not to consider the types that the stub
   *     file declares
   * @return binary name of class that {@code className} identifies in the current context, or null
   *     if resolution fails
   */
  private @Nullable @BinaryName String resolve(
      @FullyQualifiedName String className, @Nullable CompilationUnit cu) {

    // The order of the lookups below is the order in which Java resolves a type name: a
    // single-type import shadows a type in the current package, which shadows a type that an
    // import-on-demand declaration makes available.

    for (String declName : singleTypeImports) {
      String qualifiedName = mergeImport(declName, className);
      if (qualifiedName != null) {
        String binaryName = classBinaryName(qualifiedName, cu);
        if (binaryName != null) {
          return binaryName;
        }
      }
    }

    if (pkgName != null) {
      String binaryName = classBinaryName(pkgName + "." + className, cu);
      if (binaryName != null) {
        return binaryName;
      }
    }

    for (String declName : onDemandImports) {
      String qualifiedName = mergeImport(declName, className);
      if (qualifiedName != null) {
        String binaryName = classBinaryName(qualifiedName, cu);
        if (binaryName != null) {
          return binaryName;
        }
      }
    }

    {
      // Every Java program implicitly does "import java.lang.*",
      // so see whether this class is in that package.
      String binaryName = loadClassBinaryName("java.lang." + className);
      if (binaryName != null) {
        return binaryName;
      }
    }

    return classBinaryName(className, cu);
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
   * If a class that encloses {@code node} inherits a member type named {@code typeName} from a
   * class on the classpath, returns that member type's binary name; otherwise returns null.
   *
   * @param node the node in the stub file's AST at which {@code typeName} appears
   * @param typeName a type name, in which a {@code .} separates a nested class from its enclosing
   *     class
   * @return the binary name of the inherited member type, or null
   */
  private @Nullable String inheritedFromClasspath(Node node, String typeName) {
    String[] identifiers = typeName.split("\\.", -1);
    CompilationUnit cu = node.findCompilationUnit().orElse(null);
    for (Node n = node; n != null; n = n.getParentNode().orElse(null)) {
      if (!(n instanceof TypeDeclaration<?>)) {
        continue;
      }
      for (ClassOrInterfaceType supertype : supertypes((TypeDeclaration<?>) n)) {
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getNameWithScope
        @FullyQualifiedName String supertypeName = supertype.getNameWithScope();
        String supertypeBinaryName = resolve(supertypeName, cu);
        if (supertypeBinaryName == null) {
          continue;
        }
        Class<?> clazz = loadClass(supertypeBinaryName);
        for (String identifier : identifiers) {
          clazz = memberClass(clazz, identifier, new HashSet<>());
          if (clazz == null) {
            break;
          }
        }
        if (clazz != null) {
          return clazz.getName();
        }
      }
    }
    return null;
  }

  /**
   * Returns the member type named {@code identifier} that {@code clazz} declares or inherits, or
   * null if there is none. A private member type is not inherited, so this method ignores one.
   *
   * @param clazz a class on the classpath, or null
   * @param identifier the simple name of a member type
   * @param visited the classes that have already been searched; this method adds {@code clazz} to
   *     it
   * @return the member type named {@code identifier}, or null if there is none
   */
  private static @Nullable Class<?> memberClass(
      @Nullable Class<?> clazz, String identifier, Set<Class<?>> visited) {
    if (clazz == null || !visited.add(clazz)) {
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
      if (member.getSimpleName().equals(identifier) && !Modifier.isPrivate(member.getModifiers())) {
        return member;
      }
    }
    Class<?> result = memberClass(clazz.getSuperclass(), identifier, visited);
    if (result != null) {
      return result;
    }
    for (Class<?> superinterface : clazz.getInterfaces()) {
      result = memberClass(superinterface, identifier, visited);
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
   * with no package name is assumed to refer to the stub file's own package.
   *
   * @param typeName a type name that resolution failed on
   * @return the binary name that {@code typeName} most likely refers to
   */
  private String unresolvedBinaryName(String typeName) {
    String[] identifiers = typeName.split("\\.", -1);
    // The index of the outermost class name; every identifier before it is a package name.
    int outermostClass = 0;
    while (outermostClass < identifiers.length - 1
        && !startsWithUppercase(identifiers[outermostClass])) {
      outermostClass++;
    }
    StringBuilder result = new StringBuilder();
    if (outermostClass == 0 && pkgName != null) {
      // The name has no package name, so it refers to the stub file's own package.
      result.append(pkgName).append('.');
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
