package org.checkerframework.framework.stub;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.checkerframework.stubparser.JavaParser;
import org.checkerframework.stubparser.ParseException;
import org.checkerframework.stubparser.ast.CompilationUnit;
import org.checkerframework.stubparser.ast.ImportDeclaration;
import org.checkerframework.stubparser.ast.IndexUnit;
import org.checkerframework.stubparser.ast.PackageDeclaration;
import org.checkerframework.stubparser.ast.TypeParameter;
import org.checkerframework.stubparser.ast.body.AnnotationDeclaration;
import org.checkerframework.stubparser.ast.body.BodyDeclaration;
import org.checkerframework.stubparser.ast.body.ClassOrInterfaceDeclaration;
import org.checkerframework.stubparser.ast.body.ConstructorDeclaration;
import org.checkerframework.stubparser.ast.body.EnumConstantDeclaration;
import org.checkerframework.stubparser.ast.body.EnumDeclaration;
import org.checkerframework.stubparser.ast.body.FieldDeclaration;
import org.checkerframework.stubparser.ast.body.InitializerDeclaration;
import org.checkerframework.stubparser.ast.body.MethodDeclaration;
import org.checkerframework.stubparser.ast.body.Parameter;
import org.checkerframework.stubparser.ast.body.TypeDeclaration;
import org.checkerframework.stubparser.ast.body.VariableDeclarator;
import org.checkerframework.stubparser.ast.expr.AnnotationExpr;
import org.checkerframework.stubparser.ast.expr.Expression;
import org.checkerframework.stubparser.ast.expr.ObjectCreationExpr;
import org.checkerframework.stubparser.ast.expr.VariableDeclarationExpr;
import org.checkerframework.stubparser.ast.stmt.BlockStmt;
import org.checkerframework.stubparser.ast.type.ClassOrInterfaceType;
import org.checkerframework.stubparser.ast.type.PrimitiveType;
import org.checkerframework.stubparser.ast.type.ReferenceType;
import org.checkerframework.stubparser.ast.type.Type;
import org.checkerframework.stubparser.ast.type.VoidType;
import org.checkerframework.stubparser.ast.type.WildcardType;
import org.checkerframework.stubparser.ast.visitor.GenericVisitorAdapter;
import org.checkerframework.framework.util.PluginUtil;

import annotations.Annotation;
import annotations.el.AClass;
import annotations.el.ADeclaration;
import annotations.el.AElement;
import annotations.el.AField;
import annotations.el.AMethod;
import annotations.el.AScene;
import annotations.el.ATypeElement;
import annotations.el.AnnotationDef;
import annotations.el.BoundLocation;
import annotations.el.DefException;
import annotations.el.InnerTypeLocation;
import annotations.el.LocalLocation;
import annotations.field.AnnotationFieldType;
import annotations.io.IndexFileParser;
import annotations.io.IndexFileWriter;

import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;

/**
 * Program to convert a stub file into index files (JAIFs).
 * Note that the resulting index files will not include annotation
 * definitions, for which stubfiles do not generally provide complete
 * information.
 *
 * @author dbro
 */
public class ToIndexFileConverter extends GenericVisitorAdapter<Void, AElement> {
  private static Pattern packagePattern = Pattern.compile(
      "\\bpackage *+((?:[^.]*+[.] *+)*+[^ ]*) *+;",
      Pattern.DOTALL);
  private static Pattern importPattern = Pattern.compile(
      "\\bimport *+((?:[^.]*+[.] *+)*+[^ ]*) *+;",
      Pattern.DOTALL);

  private final String pkgName;
  private final List<String> imports;
  private final AScene scene;

  /**
   * @param pkgDecl AST node for package declaration
   * @param importDecls AST node for import declarations
   * @param scene scene for visitor methods to fill in
   */
  public ToIndexFileConverter(PackageDeclaration pkgDecl,
      List<ImportDeclaration> importDecls, AScene scene) {
    this.scene = scene;
    if (pkgDecl == null) {
      pkgName = "";
    } else {
      Matcher m = packagePattern.matcher(pkgDecl.toString());
      String s = m.find() ? m.group(1) : null;
      pkgName = s == null ? "" : s;
    }
    if (importDecls == null) {
      imports = Collections.emptyList();
    } else {
      ArrayList<String> imps =
          new ArrayList<String>(importDecls.size());
      for (ImportDeclaration decl : importDecls) {
        if (!decl.isStatic()) {
          Matcher m = importPattern.matcher(decl.toString());
          if (m.find()) {
            String s = m.group(1);
            if (s != null) { imps.add(s); }
          }
        }
      }
      imps.trimToSize();
      imports = Collections.unmodifiableList(imps);
    }
  }

  /**
   * Parse stub files and write out equivalent JAIFs.
   * Note that the results do not include annotation definitions, for
   * which stubfiles do not generally provide complete information.
   *
   * @param args name of JAIF with annotation definition, followed by
   * names of stub files to be converted (if none given, program reads
   * from standard input)
   * @throws IOException
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("usage: java ToIndexFileConverter jaif [stubfile...]");
      System.err.println("(JAIF contains needed annotation definitions)");
      System.exit(1);
    }

    AScene scene = new AScene();
    try {
      IndexFileParser.parseFile(args[0], scene);

      if (args.length == 1) {
        convert(scene, System.in, System.out);
        return;
      }

      for (int i = 1; i < args.length; i++) {
        String f0 = args[i];
        String f1 = (f0.endsWith(".astub")
            ? f0.substring(0, f0.length()-6)
            : f0) + ".jaif";
        try (
            InputStream in = new FileInputStream(f0);
            OutputStream out = new FileOutputStream(f1);
            ) {
          convert(new AScene(scene), in, out);
        }
      }
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Augment given scene with information from stubfile, reading stubs
   * from input stream and writing JAIF to output stream.
   *
   * @param scene the initial scene
   * @param in stubfile contents
   * @param out JAIF representing augmented scene
   * @throws ParseException
   * @throws DefException
   */
  private static void convert(AScene scene, InputStream in, OutputStream out)
      throws IOException, DefException, ParseException {
    IndexUnit iu = JavaParser.parse(in);
    extractScene(iu, scene);
    try (Writer w = new BufferedWriter(new OutputStreamWriter(out))) {
      IndexFileWriter.write(scene, w);
    }
  }

  /**
   * Entry point of recursive-descent IndexUnit to AScene transformer.
   *
   * @param iu {@link IndexUnit} representing stubfile
   * @return {@link AScene} containing annotations from stubfile
   */
  private static void extractScene(IndexUnit iu, AScene scene) {
    for (CompilationUnit cu : iu.getCompilationUnits()) {
      List<TypeDeclaration> typeDecls = cu.getTypes();
      if (typeDecls != null) {
        List<ImportDeclaration> impDecls = cu.getImports();
        PackageDeclaration pkgDecl = cu.getPackage();
        for (TypeDeclaration typeDecl : typeDecls) {
          ToIndexFileConverter converter =
              new ToIndexFileConverter(pkgDecl, impDecls, scene);
          String pkgName = converter.pkgName;
          String name = typeDecl.getName();
          if (!pkgName.isEmpty()) { name = pkgName + "." + name; }
          typeDecl.accept(converter, scene.classes.vivify(name));
        }
      }
    }
  }

  /**
   * Builds simplified annotation from its declaration.
   * Only the name is included, because stubfiles do not generally have
   * access to the full definitions of annotations.
   *
   * @param expr
   * @return
   */
  private static Annotation extractAnnotation(AnnotationExpr expr) {
    //String exprName = expr.getName().getName();
    String exprName = expr.toString().substring(1);  // 1 for '@'
    //String exprName = resolve(expr.getName().getName());
    AnnotationDef def = new AnnotationDef(exprName);
    def.setFieldTypes(Collections.<String, AnnotationFieldType>emptyMap());
    return new Annotation(def, Collections.<String, Object>emptyMap());
  }

  @Override
  public Void visit(AnnotationDeclaration decl, AElement elem) {
    return null;
  }

  @Override
  public Void visit(BlockStmt stmt, AElement elem) {
    return null;
    //super.visit(stmt, elem);
  }

  @Override
  public Void visit(ClassOrInterfaceDeclaration decl, AElement elem) {
    visitDecl(decl, (ADeclaration) elem);
    return super.visit(decl, elem);
  }

  @Override
  public Void visit(ConstructorDeclaration decl, AElement elem) {
    int i = 0;
    List<Parameter> params = decl.getParameters();
    List<AnnotationExpr> rcvrAnnos = decl.getReceiverAnnotations();
    BlockStmt body = decl.getBlock();
    StringBuilder sb = new StringBuilder("<init>(");
    AClass clazz = (AClass) elem;
    AMethod method;
    if (params != null) {
      for (Parameter param : params) {
        Type ptype = param.getType();
        sb.append(getJVML(ptype));
      }
    }
    sb.append(")V");
    method = clazz.methods.vivify(sb.toString());
    visitDecl(decl, method);
    if (params != null) {
      for (Parameter param : params) {
        AField field = method.parameters.vivify(i++);
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
    //return super.visit(decl, elem);
  }

  @Override
  public Void visit(EnumConstantDeclaration decl, AElement elem) {
    AField field = ((AClass) elem).fields.vivify(decl.getName());
    visitDecl(decl, field);
    return super.visit(decl, field);
  }

  @Override
  public Void visit(EnumDeclaration decl, AElement elem) {
    visitDecl(decl, (ADeclaration) elem);
    return super.visit(decl, elem);
  }

  @Override
  public Void visit(FieldDeclaration decl, AElement elem) {
    for (VariableDeclarator v : decl.getVariables()) {
      AClass clazz = (AClass) elem;
      AField field = clazz.fields.vivify(v.getId().getName());
      visitDecl(decl, field);
      visitType(decl.getType(), field.type);
    }
    return null;
  }

  @Override
  public Void visit(InitializerDeclaration decl, AElement elem) {
    BlockStmt block = decl.getBlock();
    AClass clazz = (AClass) elem;
    block.accept(this,
        clazz.methods.vivify(decl.isStatic() ? "<clinit>" : "<init>"));
    return null;
  }

  @Override
  public Void visit(MethodDeclaration decl, AElement elem) {
    Type type = decl.getType();
    List<Parameter> params = decl.getParameters();
    List<TypeParameter> typeParams = decl.getTypeParameters();
    List<AnnotationExpr> rcvrAnnos = decl.getReceiverAnnotations();
    BlockStmt body = decl.getBody();
    StringBuilder sb = new StringBuilder(decl.getName()).append('(');
    AClass clazz = (AClass) elem;
    AMethod method;
    if (params != null) {
      for (Parameter param : params) {
        Type ptype = param.getType();
        sb.append(getJVML(ptype));
      }
    }
    sb.append(')').append(getJVML(type));
    method = clazz.methods.vivify(sb.toString());
    visitDecl(decl, method);
    visitType(type, method.returnType);
    if (params != null) {
      int i = 0;
      for (Parameter param : params) {
        AField field = method.parameters.vivify(i++);
        visitType(param.getType(), field.type);
      }
    }
    if (rcvrAnnos != null) {
      for (AnnotationExpr expr : rcvrAnnos) {
        Annotation anno = extractAnnotation(expr);
        method.receiver.type.tlAnnotationsHere.add(anno);
      }
    }
    if (typeParams != null) {
      int i = 0;
      for (TypeParameter typeParam : typeParams) {
        List<ClassOrInterfaceType> bounds = typeParam.getTypeBound();
        if (bounds != null) {
          int j = 0;
          for (ClassOrInterfaceType bound : bounds) {
            BoundLocation loc = new BoundLocation(i, j);
            bound.accept(this, method.bounds.vivify(loc));
            ++j;
          }
        }
        ++i;
      }
    }
    return body == null ? null : body.accept(this, method);
  }

  @Override
  public Void visit(ObjectCreationExpr expr, AElement elem) {
    ClassOrInterfaceType type = expr.getType();
    AClass clazz = scene.classes.vivify(type.getName());
    Expression scope = expr.getScope();
    List<Type> typeArgs = expr.getTypeArgs();
    List<Expression> args = expr.getArgs();
    List<BodyDeclaration> decls = expr.getAnonymousClassBody();
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
    if (decls != null) {
      for (BodyDeclaration decl : decls) {
        decl.accept(this, clazz);
      }
    }
    return null;
  }

  @Override
  public Void visit(VariableDeclarationExpr expr, AElement elem) {
    List<AnnotationExpr> annos = expr.getAnnotations();
    AMethod method = (AMethod) elem;
    int i = 0;
    for (VariableDeclarator decl : expr.getVars()) {
      LocalLocation loc = new LocalLocation(decl.getId().getName(), i);
      AField field = method.body.locals.vivify(loc);
      visitType(expr.getType(), field.type);
      if (annos != null) {
        for (AnnotationExpr annoExpr : annos) {
          Annotation anno = extractAnnotation(annoExpr);
          field.tlAnnotationsHere.add(anno);
        }
      }
      ++i;
    }
    return null;
  }

  /**
   * Copies information from an AST declaration node to an {@link ADeclaration}.
   * Called by visitors for BodyDeclaration subclasses.
   */
  private Void visitDecl(BodyDeclaration decl, ADeclaration elem) {
    List<AnnotationExpr> annoExprs = decl.getAnnotations();
    if (annoExprs != null) {
      for (AnnotationExpr annoExpr : annoExprs) {
        Annotation anno = extractAnnotation(annoExpr);
        elem.tlAnnotationsHere.add(anno);
      }
    }
    return null;
  }

  /**
   * Copies information from an AST type node to an {@link ATypeElement}.
   */
  private Void visitType(Type type, final ATypeElement elem) {
    List<AnnotationExpr> exprs = type.getAnnotations();
    if (exprs != null) {
      for (AnnotationExpr expr : exprs) {
        Annotation anno = extractAnnotation(expr);
        elem.tlAnnotationsHere.add(anno);
      }
    }
    visitInnerTypes(type, elem);
    return null;
  }

  /**
   * Copies information from an AST type node's inner type nodes to an
   * {@link ATypeElement}.
   */
  private static Void visitInnerTypes(Type type, final ATypeElement elem) {
    return type.accept(new GenericVisitorAdapter<Void, InnerTypeLocation>() {
      @Override
      public Void visit(ClassOrInterfaceType type, InnerTypeLocation loc) {
        int i = 0;
        for (Type inner : type.getTypeArgs()) {
          InnerTypeLocation ext = extendedTypePath(loc, 3, i++);
          visitInnerType(inner, ext);
        }
        return null;
      }

      @Override
      public Void visit(ReferenceType type, InnerTypeLocation loc) {
        InnerTypeLocation ext = loc;
        int n = type.getArrayCount();
        for (int i = 0; i < n; i++) {
          ext = extendedTypePath(ext, 1, 0);
          for (AnnotationExpr expr : type.getAnnotationsAtLevel(i)) {
            ATypeElement typeElem = elem.innerTypes.vivify(ext);
            Annotation anno = extractAnnotation(expr);
            typeElem.tlAnnotationsHere.add(anno);
          }
        }
        return null;
      }

      @Override
      public Void visit(WildcardType type, InnerTypeLocation loc) {
        ReferenceType lower = type.getExtends();
        ReferenceType upper = type.getSuper();
        if (lower != null) {
          InnerTypeLocation ext = extendedTypePath(loc, 2, 0);
          visitInnerType(lower, ext);
        }
        if (upper != null) {
          InnerTypeLocation ext = extendedTypePath(loc, 2, 0);
          visitInnerType(upper, ext);
        }
        return null;
      }

      /**
       * Copies information from an AST inner type node to an
       * {@link ATypeElement}.
       */
      private void visitInnerType(Type type, InnerTypeLocation loc) {
        ATypeElement typeElem = elem.innerTypes.vivify(loc);
        for (AnnotationExpr expr : type.getAnnotations()) {
          Annotation anno = extractAnnotation(expr);
          typeElem.tlAnnotationsHere.add(anno);
          type.accept(this, loc);
        }
      }

      /**
       * Extends type path by one element.
       * @see TypePathEntry.fromBinary
       */
      private InnerTypeLocation extendedTypePath(InnerTypeLocation loc,
          int tag, int arg) {
        List<TypePathEntry> path =
            new ArrayList<TypePathEntry>(loc.location.size()+1);
        path.addAll(loc.location);
        path.add(TypePathEntry.fromBinary(tag, arg));
        return new InnerTypeLocation(path);
      }
    }, InnerTypeLocation.EMPTY_INNER_TYPE_LOCATION);
  }

  /**
   * Computes a type's "binary name".
   *
   * @param type the type
   * @return the type's binary name
   */
  private String getJVML(Type type) {
    return type.accept(new GenericVisitorAdapter<String, Void>() {
      @Override
      public String visit(ClassOrInterfaceType type, Void v) {
        String typeName = type.getName();
        String name = resolve(typeName);
        if (name != null) {
          String[] parts = name.split("\\.");
          StringBuilder sb = new StringBuilder("L").append(parts[0]);
          for (int i = 1; i < parts.length; i++) {
            sb.append('/').append(parts[i]);
          }
          sb.append(";");
          return sb.toString();
        }
        return "L" + typeName + ";";
      }

      @Override
      public String visit(PrimitiveType type, Void v) {
        switch (type.getType()) {
        case Boolean:
          return "Z";
        case Byte:
          return "B";
        case Char:
          return "C";
        case Double:
          return "D";
        case Float:
          return "F";
        case Int:
          return "I";
        case Long:
          return "J";
        case Short:
          return "S";
        default:
          throw new IllegalArgumentException(
              "baseTypeName(): unknown primitive type " + type);
        }
      }

      @Override
      public String visit(ReferenceType type, Void v) {
        String typeName = type.getType().accept(this, null);
        StringBuilder sb = new StringBuilder();
        int n = type.getArrayCount();
        while (--n >= 0) { sb.append("["); }
        sb.append(typeName);
        return sb.toString();
      }

      @Override
      public String visit(VoidType type, Void v) {
        return "V";
      }

      @Override
      public String visit(WildcardType type, Void v) {
        return "Ljava/lang/Object;";
      }
    }, null);
  }

  /**
   * Finds the fully qualified name of the class with the given name.
   *
   * @param className possibly unqualified name of class
   * @return fully qualified name of class that {@code className}
   * identifies in the current context
   */
  private String resolve(String className) {
    String qualifiedName;
    Class<?> resolved = null;

    if (pkgName.isEmpty()) {
      qualifiedName = className;
      resolved = loadClass(qualifiedName);
      if (resolved == null) {
        qualifiedName = "java.lang." + className;
        resolved = loadClass(qualifiedName);
      }
    } else {
      qualifiedName = pkgName + "." + className;
      resolved = loadClass(qualifiedName);
      if (resolved == null) {
        qualifiedName = "java.lang." + className;
        resolved = loadClass(qualifiedName);
        if (resolved == null) {
          qualifiedName = className;
          resolved = loadClass(qualifiedName);
        }
      }
    }

    if (resolved == null) {
      for (String declName : imports) {
        qualifiedName = mergePrefix(declName, className);
        if (qualifiedName != null) { return qualifiedName; }
      }
      return className;
    }
    return qualifiedName;
  }

  /**
   * Attempts to resolve class name with respect to import.
   *
   * @param prefix name of imported package
   * @param base name of class, possibly qualified
   * @return fully qualified class name if resolution succeeds, null otherwise
   */
  private static String mergePrefix(String prefix, String base) {
    if (prefix.isEmpty() || prefix.equals(base)) {
      return base;
    }
    String[] a0 = prefix.split("\\.");
    String[] a1 = base.split("\\.");
    String prefixEnd = a0[a0.length-1];
    if ("*".equals(prefixEnd)) {
      int n = a0.length-1;
      String[] a = Arrays.copyOf(a0, n + a1.length);
      for (int i = 0; i < a1.length; i++) { a[n+i] = a1[i]; }
      return PluginUtil.join(".", a);
    } else {
      int i = a0.length;
      int n = i - a1.length;
      while (--i >= n) {
        if (!a1[i-n].equals(a0[i])) { return null; }
      }
      return prefix;
    }
  }

  /**
   * Finds {@link Class} corresponding to a name.
   *
   * @param className
   * @return {@link Class} object corresponding to className, or null if
   * none found
   */
  private static Class<?> loadClass(String className) {
    if (className != null) {
      try {
        return Class.forName(className, false, null);
      } catch (ClassNotFoundException e) {}
    }
    return null;
  }
}
