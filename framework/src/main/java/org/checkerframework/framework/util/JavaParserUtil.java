package org.checkerframework.framework.util;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;

/** Utility methods for working with JavaParser. Also see {@link StaticJavaParserUtil}. */
public final class JavaParserUtil {

  /** Do not instantiate. */
  private JavaParserUtil() {
    throw new Error("Do not instantiate.");
  }

  //
  // Resolving names
  //

  /**
   * Returns the element for the given JavaParser type, whose name is resolved in the scope of the
   * type declarations and the compilation unit that contain it. Returns null if the name cannot be
   * resolved, which happens for a type variable and for a type that is not on the classpath.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @return the element for {@code type}, or null if it cannot be determined
   */
  public static @Nullable TypeElement resolveTypeName(
      Elements elements, ClassOrInterfaceType type) {
    String name = type.getNameWithScope();

    // The name might already be fully-qualified.
    {
      TypeElement result = elements.getTypeElement(name);
      if (result != null) {
        return result;
      }
    }

    // A type that is lexically enclosed in a type declaration takes precedence over an import,
    // over a type in the same package, and over a type in `java.lang`.
    for (Node ancestor = type.getParentNode().orElse(null);
        ancestor != null;
        ancestor = ancestor.getParentNode().orElse(null)) {
      if (ancestor instanceof TypeDeclaration<?> enclosingType) {
        String enclosingName = enclosingType.getFullyQualifiedName().orElse(null);
        if (enclosingName != null) {
          TypeElement result = elements.getTypeElement(enclosingName + "." + name);
          if (result != null) {
            return result;
          }
        }
      }
    }

    CompilationUnit cu = type.findCompilationUnit().orElse(null);
    if (cu == null) {
      return null;
    }

    // `firstComponent` is what a single-type import must import; the rest of `name` names a
    // nested type, as in `Entry` and `Entry.Foo` for the import `java.util.Map.Entry`.
    int dotIndex = name.indexOf('.');
    String firstComponent;
    String suffix;
    if (dotIndex == -1) {
      firstComponent = name;
      suffix = "";
    } else {
      firstComponent = name.substring(0, dotIndex);
      suffix = name.substring(dotIndex);
    }

    // A single-type import takes precedence over an import on demand.
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (importDecl.isStatic() || importDecl.isAsterisk()) {
        continue;
      }
      String importedName = importDecl.getNameAsString();
      if (importedName.equals(firstComponent) || importedName.endsWith("." + firstComponent)) {
        TypeElement result = elements.getTypeElement(importedName + suffix);
        if (result != null) {
          return result;
        }
      }
    }

    // The type might be in the same package, in a package that is imported on demand, or in
    // `java.lang`.
    List<String> packageNames = new ArrayList<>();
    cu.getPackageDeclaration().ifPresent(pkg -> packageNames.add(pkg.getNameAsString()));
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (importDecl.isAsterisk() && !importDecl.isStatic()) {
        packageNames.add(importDecl.getNameAsString());
      }
    }
    packageNames.add("java.lang");
    for (String packageName : packageNames) {
      TypeElement result = elements.getTypeElement(packageName + "." + name);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  /**
   * Given the compilation unit node for a source file, returns the top level type definition with
   * the given name.
   *
   * @param root compilation unit to search
   * @param name name of a top level type declaration in {@code root}
   * @return a top level type declaration in {@code root} named {@code name}
   */
  public static TypeDeclaration<?> getTypeDeclarationByName(CompilationUnit root, String name) {
    Optional<ClassOrInterfaceDeclaration> classDecl = root.getClassByName(name);
    if (classDecl.isPresent()) {
      return classDecl.get();
    }

    Optional<ClassOrInterfaceDeclaration> interfaceDecl = root.getInterfaceByName(name);
    if (interfaceDecl.isPresent()) {
      return interfaceDecl.get();
    }

    Optional<EnumDeclaration> enumDecl = root.getEnumByName(name);
    if (enumDecl.isPresent()) {
      return enumDecl.get();
    }

    Optional<AnnotationDeclaration> annoDecl = root.getAnnotationDeclarationByName(name);
    if (annoDecl.isPresent()) {
      return annoDecl.get();
    }

    Optional<RecordDeclaration> recordDecl = root.getRecordByName(name);
    if (recordDecl.isPresent()) {
      return recordDecl.get();
    }

    Optional<CompilationUnit.Storage> storage = root.getStorage();
    if (storage.isPresent()) {
      throw new BugInCF("Type " + name + " not found in " + storage.get().getPath());
    } else {
      throw new BugInCF("Type " + name + " not found in " + root);
    }
  }

  /**
   * Returns the fully qualified name of a type appearing in a given compilation unit.
   *
   * @param type a type declaration
   * @param compilationUnit the compilation unit containing {@code type}
   * @return the fully qualified name of {@code type} if {@code compilationUnit} contains a package
   *     declaration, or just the name of {@code type} otherwise
   */
  public static String getFullyQualifiedName(
      TypeDeclaration<?> type, CompilationUnit compilationUnit) {
    if (compilationUnit.getPackageDeclaration().isPresent()) {
      return compilationUnit.getPackageDeclaration().get().getNameAsString()
          + "."
          + type.getNameAsString();
    } else {
      return type.getNameAsString();
    }
  }

  /**
   * Returns the {@code TypeKind} that corresponds to the given JavaParser primitive type.
   *
   * @param primitiveType a JavaParser primitive type
   * @return the {@code TypeKind} for {@code primitiveType}
   */
  public static TypeKind typeKindForPrimitive(PrimitiveType primitiveType) {
    return switch (primitiveType.getType()) {
      case BOOLEAN -> TypeKind.BOOLEAN;
      case BYTE -> TypeKind.BYTE;
      case CHAR -> TypeKind.CHAR;
      case DOUBLE -> TypeKind.DOUBLE;
      case FLOAT -> TypeKind.FLOAT;
      case INT -> TypeKind.INT;
      case LONG -> TypeKind.LONG;
      case SHORT -> TypeKind.SHORT;
    };
  }

  //
  // Perform side effects
  //

  /**
   * Side-effects {@code node} by removing all annotations from anywhere inside its subtree.
   *
   * @param node a JavaParser Node
   */
  public static void clearAnnotations(Node node) {
    node.accept(new ClearAnnotationsVisitor(), null);
  }

  /** A visitor that clears all annotations from a JavaParser AST. */
  private static final class ClearAnnotationsVisitor extends VoidVisitorWithDefaultAction {

    /** Creates a new ClearAnnotationsVisitor. */
    ClearAnnotationsVisitor() {}

    @Override
    public void defaultAction(Node node) {
      for (Node child : new ArrayList<>(node.getChildNodes())) {
        if (child instanceof AnnotationExpr) {
          node.remove(child);
        }
      }
    }

    @Override
    public void visit(ArrayInitializerExpr node, Void p) {
      // Do not remove annotations that are array elements.
    }
  }

  /**
   * Side-effects node by combining any added String literals in node's subtree into their
   * concatenation. For example, the expression {@code "a" + "b"} becomes {@code "ab"}. This occurs
   * even if, when reading from left to right, the two string literals are not added directly. For
   * example, the expression {@code 1 + "a" + "b"} parses as {@code (1 + "a") + "b"}}, but it is
   * transformed into {@code 1 + "ab"}.
   *
   * <p>This is the same transformation performed by javac automatically. Javac seems to ignore
   * string literals surrounded in parentheses, so this method does as well.
   *
   * @param node a JavaParser Node
   */
  public static void concatenateAddedStringLiterals(Node node) {
    node.accept(new StringLiteralConcatenateVisitor(), null);
  }

  /** Visitor that combines added String literals, see {@link #concatenateAddedStringLiterals}. */
  public static class StringLiteralConcatenateVisitor extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(BinaryExpr node, Void p) {
      super.visit(node, p);
      if (node.getOperator() == BinaryExpr.Operator.PLUS && node.getRight().isStringLiteralExpr()) {
        String right = node.getRight().asStringLiteralExpr().getValue();
        if (node.getLeft().isStringLiteralExpr()) {
          String left = node.getLeft().asStringLiteralExpr().getValue();
          node.replace(new StringLiteralExpr(left + right));
        } else if (node.getLeft().isBinaryExpr()) {
          BinaryExpr leftExpr = node.getLeft().asBinaryExpr();
          if (leftExpr.getOperator() == BinaryExpr.Operator.PLUS
              && leftExpr.getRight().isStringLiteralExpr()) {
            String left = leftExpr.getRight().asStringLiteralExpr().getValue();
            node.replace(
                new BinaryExpr(
                    leftExpr.getLeft(),
                    new StringLiteralExpr(left + right),
                    BinaryExpr.Operator.PLUS));
          }
        }
      }
    }
  }

  //
  // Deprecated
  //

  /**
   * Initialized by {@link #getCurrentSourceVersion(ProcessingEnvironment)}. Use that method to
   * access.
   */
  private static LanguageLevel currentSourceVersion = null;

  /**
   * Returns the {@link com.github.javaparser.ParserConfiguration.LanguageLevel} corresponding to
   * the current source version.
   *
   * @param env processing environment used to get source version
   * @return the current source version
   * @deprecated Does not seem to be used
   */
  @Deprecated // 2026-09-02
  public static ParserConfiguration.LanguageLevel getCurrentSourceVersion(
      ProcessingEnvironment env) {
    if (currentSourceVersion == null) {
      // Use String comparison so we can compile on older JDKs that
      // don't have all the latest SourceVersion constants.
      currentSourceVersion =
          switch (env.getSourceVersion().name()) {
            case "RELEASE_8" -> ParserConfiguration.LanguageLevel.JAVA_8;
            case "RELEASE_9" -> ParserConfiguration.LanguageLevel.JAVA_9;
            case "RELEASE_10" -> ParserConfiguration.LanguageLevel.JAVA_10;
            case "RELEASE_11" -> ParserConfiguration.LanguageLevel.JAVA_11;
            case "RELEASE_12" -> ParserConfiguration.LanguageLevel.JAVA_12;
            case "RELEASE_13" -> ParserConfiguration.LanguageLevel.JAVA_13;
            case "RELEASE_14" -> ParserConfiguration.LanguageLevel.JAVA_14;
            case "RELEASE_15" -> ParserConfiguration.LanguageLevel.JAVA_15;
            case "RELEASE_16" -> ParserConfiguration.LanguageLevel.JAVA_16;
            case "RELEASE_17" -> ParserConfiguration.LanguageLevel.JAVA_17;
            case "RELEASE_18" -> ParserConfiguration.LanguageLevel.JAVA_18;
            case "RELEASE_19" -> ParserConfiguration.LanguageLevel.JAVA_19;
            case "RELEASE_20" -> ParserConfiguration.LanguageLevel.JAVA_20;
            case "RELEASE_21" -> ParserConfiguration.LanguageLevel.JAVA_21;
            case "RELEASE_22" -> ParserConfiguration.LanguageLevel.JAVA_22;
            case "RELEASE_23" -> ParserConfiguration.LanguageLevel.JAVA_23;
            case "RELEASE_24" -> ParserConfiguration.LanguageLevel.JAVA_24;
            case "RELEASE_25" -> ParserConfiguration.LanguageLevel.JAVA_25;
            // Up-to-date as of 2026-03-26.  See
            // https://www.javadoc.io/doc/com.github.javaparser/javaparser-core/latest/com/github/javaparser/ParserConfiguration.LanguageLevel.html .
            default -> StaticJavaParserUtil.DEFAULT_LANGUAGE_LEVEL;
          };
    }
    return currentSourceVersion;
  }
}
