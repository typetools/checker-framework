package org.checkerframework.framework.util;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;

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
   * resolved: it names a type variable, a local class, a member of a local or anonymous class, or a
   * type that is not on the classpath.
   *
   * <p>A client that resolves many names should call {@link #resolveTypeName(Elements,
   * ClassOrInterfaceType, Map)}, which memoizes the name lookups.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @return the element for {@code type}, or null if it cannot be determined
   */
  public static @Nullable TypeElement resolveTypeName(
      Elements elements, ClassOrInterfaceType type) {
    return resolveTypeName(elements, type, new HashMap<>());
  }

  /**
   * Returns the element for the given JavaParser type, whose name is resolved in the scope of the
   * type declarations and the compilation unit that contain it. Returns null if the name cannot be
   * resolved: it names a type variable, a local class, a member of a local or anonymous class, or a
   * type that is not on the classpath.
   *
   * <p>Resolving one name looks up many candidate names, most of which name no type, so a client
   * that resolves many names should pass the same cache to each call. A cache should not be reused
   * across annotation processing rounds or across {@code Elements} instances: a name that names no
   * type in one round might name a generated type in a later round.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it. It must permit null values, so it cannot be a {@code
   *     ConcurrentHashMap}; this method is not thread-safe.
   * @return the element for {@code type}, or null if it cannot be determined
   */
  public static @Nullable TypeElement resolveTypeName(
      Elements elements, ClassOrInterfaceType type, Map<String, @Nullable TypeElement> cache) {
    String name = type.getNameWithScope();

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

    // A type parameter, a local class, or a type that is lexically enclosed in a type declaration,
    // takes precedence over an import, over a type in the same package, over a type in
    // `java.lang`, and over the interpretation of `name` as a fully-qualified name.
    //
    // `child` is the child of `ancestor` that contains `type`.  It distinguishes a use of `name`
    // within a class body, where the class's member types are in scope, from a use in the class's
    // header -- its annotations, its type parameter bounds, and its own supertype names -- where
    // they are not.
    Node child = type;
    for (Node ancestor = type.getParentNode().orElse(null);
        ancestor != null;
        child = ancestor, ancestor = ancestor.getParentNode().orElse(null)) {
      if (ancestor instanceof NodeWithTypeParameters<?> genericDeclaration) {
        for (TypeParameter typeParameter : genericDeclaration.getTypeParameters()) {
          if (typeParameter.getNameAsString().equals(firstComponent)) {
            // `name` names a type parameter, or is nested within one.  A type parameter shadows
            // any type of the same name, and it has no TypeElement.
            return null;
          }
        }
      }

      if (declaresLocalType(ancestor, firstComponent, child)) {
        // `name` names a local class, or is nested within one.  A local class shadows any type of
        // the same name, and `Elements` cannot look up a local class by name.
        return null;
      }

      if (ancestor instanceof EnumConstantDeclaration enumConstant
          && containsSame(enumConstant.getClassBody(), child)
          && declaresMemberType(enumConstant.getClassBody(), firstComponent)) {
        // The body of an enum constant declares an anonymous class.  A member type of an
        // anonymous class has no name that `Elements` can look up.  (There is no need to search
        // the anonymous class's supertype, which is the enum:  the enum declaration is an
        // ancestor, so a later iteration of this loop searches it.)
        return null;
      }

      // The member types of a class that `Elements` cannot look up by name -- an anonymous class,
      // a local class, or a class that is nested within one -- shadow types that are declared
      // outside the class.  `unnameableSupertypes` is non-null if `type` appears within the body
      // of such a class, in which case it holds the class's supertypes, which this method searches
      // for a member type that `name` might refer to.
      List<ClassOrInterfaceType> unnameableSupertypes = null;
      if (ancestor instanceof ObjectCreationExpr creation) {
        List<? extends Node> body = creation.getAnonymousClassBody().orElse(null);
        // The member types are in scope only in the anonymous class's body, not in the supertype
        // name or in the constructor arguments.  Testing `child` also prevents infinite recursion
        // on the recursive call below.
        if (body != null && containsSame(body, child)) {
          if (declaresMemberType(body, firstComponent)) {
            // A member type of an anonymous class has no name that `Elements` can look up.
            return null;
          }
          if (creation.getScope().isPresent()) {
            // In `outer.new Inner() { ... }`, `Inner` is a member of the type of `outer` rather
            // than a name that is resolved in the scope of the expression, so this method cannot
            // determine the member types that the anonymous class inherits.
            return null;
          }
          unnameableSupertypes = Collections.singletonList(creation.getType());
        }
      } else if (ancestor instanceof TypeDeclaration<?> enclosingType
          && isInBody(enclosingType, child)) {
        String enclosingName = nameableFullyQualifiedName(enclosingType);
        if (enclosingName != null) {
          TypeElement result = getTypeElement(elements, enclosingName + "." + name, cache);
          if (result != null) {
            return result;
          }
          // The enclosing type might inherit the member type rather than declare it.
          TypeElement enclosingElement = getTypeElement(elements, enclosingName, cache);
          if (enclosingElement != null) {
            result = resolveMemberType(elements, enclosingElement, firstComponent, suffix, cache);
            if (result != null) {
              return result;
            }
          }
        } else {
          if (declaresMemberType(enclosingType.getMembers(), firstComponent)) {
            // A member type of an unnameable class has no name that `Elements` can look up.
            return null;
          }
          if (enclosingType instanceof EnumDeclaration) {
            // An enum's implicit supertype `java.lang.Enum` declares the member type `EnumDesc`.
            TypeElement enumElement = getTypeElement(elements, "java.lang.Enum", cache);
            if (enumElement == null) {
              return null;
            }
            TypeElement result =
                resolveMemberType(elements, enumElement, firstComponent, suffix, cache);
            if (result != null) {
              return result;
            }
          }
          unnameableSupertypes = supertypes(enclosingType);
        }
      }
      if (unnameableSupertypes != null) {
        for (ClassOrInterfaceType supertype : unnameableSupertypes) {
          TypeElement supertypeElement = resolveTypeName(elements, supertype, cache);
          if (supertypeElement == null) {
            // The supertype could not be determined, so neither could the member types that the
            // unnameable class inherits and that might shadow `name`.
            return null;
          }
          TypeElement result =
              resolveMemberType(elements, supertypeElement, firstComponent, suffix, cache);
          if (result != null) {
            return result;
          }
        }
      }
    }

    CompilationUnit cu = type.findCompilationUnit().orElse(null);
    if (cu == null) {
      // The name might be fully-qualified.
      return getTypeElement(elements, name, cache);
    }

    // A single-type import or a single-static import of a member type takes precedence over an
    // import on demand.
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (importDecl.isAsterisk()) {
        continue;
      }
      String importedName = importDecl.getNameAsString();
      if (importedName.equals(firstComponent) || importedName.endsWith("." + firstComponent)) {
        TypeElement result = getTypeElement(elements, importedName + suffix, cache);
        if (result != null) {
          return result;
        }
        // If `importedName` equals `firstComponent`, the import has no qualifier, so it names no
        // container to search.  (JavaParser accepts such an import even though javac does not.)
        if (importDecl.isStatic() && !importedName.equals(firstComponent)) {
          // A static import can name a member type that the named type inherits.  (A static
          // import that names a field or a method resolves to no type element at all.)
          String containerName =
              importedName.substring(0, importedName.length() - firstComponent.length() - 1);
          TypeElement containerElement = getTypeElement(elements, containerName, cache);
          if (containerElement != null) {
            result = resolveMemberType(elements, containerElement, firstComponent, suffix, cache);
            if (result != null) {
              return result;
            }
          }
        }
      }
    }

    // The type might be in the same package, in a package or type that is imported on demand, or
    // in `java.lang`.  A type in the same package shadows the others, so it is looked up first.  A
    // name in the unnamed package has no prefix.
    List<String> containerPrefixes = new ArrayList<>();
    containerPrefixes.add(
        cu.getPackageDeclaration().map(pkg -> pkg.getNameAsString() + ".").orElse(""));
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (importDecl.isAsterisk()) {
        containerPrefixes.add(importDecl.getNameAsString() + ".");
      }
    }
    containerPrefixes.add("java.lang.");
    for (String containerPrefix : containerPrefixes) {
      TypeElement result = getTypeElement(elements, containerPrefix + name, cache);
      if (result != null) {
        return result;
      }
    }

    // An import on demand, whether static or not, also imports the member types that the named
    // type inherits.
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (importDecl.isAsterisk()) {
        TypeElement importedElement = getTypeElement(elements, importDecl.getNameAsString(), cache);
        if (importedElement != null) {
          TypeElement result =
              resolveMemberType(elements, importedElement, firstComponent, suffix, cache);
          if (result != null) {
            return result;
          }
        }
      }
    }

    // The name might be fully-qualified, or might be a top-level type in the unnamed package.
    // This lookup is last, because a type that is in scope shadows a type whose fully-qualified
    // name is `name`.
    return getTypeElement(elements, name, cache);
  }

  /**
   * Returns the element for the type that {@code name} names, or null if it names no type. The
   * result of every lookup, including a lookup that finds no type, is memoized in {@code cache}.
   *
   * @param elements used for looking up names
   * @param name a name that might name a type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it
   * @return the element for the type named {@code name}, or null if there is none
   */
  @SuppressWarnings("signature:argument") // a call to getTypeElement() whose result is checked
  private static @Nullable TypeElement getTypeElement(
      Elements elements, String name, Map<String, @Nullable TypeElement> cache) {
    if (cache.containsKey(name)) {
      return cache.get(name);
    }
    TypeElement result = elements.getTypeElement(name);
    cache.put(name, result);
    return result;
  }

  /**
   * Returns true if {@code node} directly contains a statement that declares a local class,
   * interface, enum, or record whose name is {@code name}, and that declaration is in scope at
   * {@code child}. The scope of a local type declaration is the rest of the block that contains it,
   * including the declaration itself; a use that appears earlier in the block refers to some other
   * type of the same name.
   *
   * @param node a JavaParser node, such as a block
   * @param name a simple type name
   * @param child the child of {@code node} that contains the use of {@code name}
   * @return true if {@code node} declares a local type named {@code name} that is in scope at
   *     {@code child}
   */
  private static boolean declaresLocalType(Node node, String name, Node child) {
    for (Node statement : node.getChildNodes()) {
      if (statement instanceof Statement) {
        // A local type declaration is the only kind of statement whose child is a type
        // declaration.
        for (Node grandchild : statement.getChildNodes()) {
          if (grandchild instanceof TypeDeclaration<?> localType
              && localType.getNameAsString().equals(name)) {
            return true;
          }
        }
      }
      @SuppressWarnings("interning:not.interned")
      boolean sameNode = statement == child;
      if (sameNode) {
        // A local type that is declared later in the block is not in scope at `child`.
        return false;
      }
    }
    return false;
  }

  /**
   * Returns true if one of the given body declarations is a type declaration whose name is {@code
   * name}.
   *
   * @param members the body declarations of a class or interface
   * @param name a simple type name
   * @return true if {@code members} contains a type declaration named {@code name}
   */
  private static boolean declaresMemberType(List<? extends Node> members, String name) {
    for (Node member : members) {
      if (member instanceof TypeDeclaration<?> memberType
          && memberType.getNameAsString().equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the fully-qualified name by which {@link Elements} can look up the given type
   * declaration, or null if there is none: {@code typeDecl} is a local class, or it is a member of
   * a local class or of an anonymous class (including the body of an enum constant). ({@link
   * TypeDeclaration#getFullyQualifiedName} does not make this distinction; it returns a name that
   * {@link Elements} might resolve to a different type declaration.)
   *
   * @param typeDecl a JavaParser type declaration
   * @return the fully-qualified name of {@code typeDecl}, or null if it has none
   */
  private static @Nullable String nameableFullyQualifiedName(TypeDeclaration<?> typeDecl) {
    for (Node node = typeDecl; node != null; node = node.getParentNode().orElse(null)) {
      if (node instanceof TypeDeclaration<?>) {
        Node parent = node.getParentNode().orElse(null);
        // The parent of a local type declaration is the statement that declares it.  The parent
        // of a member of an anonymous class is the object creation expression, or the enum
        // constant declaration, that declares the anonymous class.
        if (parent instanceof Statement
            || parent instanceof ObjectCreationExpr
            || parent instanceof EnumConstantDeclaration) {
          return null;
        }
      }
    }
    return typeDecl.getFullyQualifiedName().orElse(null);
  }

  /**
   * Returns the supertypes that the given type declaration names: its {@code extends} clause and
   * its {@code implements} clause. The result does not include an implicit supertype such as {@code
   * java.lang.Object} or {@code java.lang.Enum}. Of those, only {@code java.lang.Enum} declares a
   * member type, so a caller that searches the result for an inherited member type must search
   * {@code java.lang.Enum} itself when {@code typeDecl} is an enum.
   *
   * @param typeDecl a JavaParser type declaration
   * @return the supertypes that {@code typeDecl} names
   */
  private static List<ClassOrInterfaceType> supertypes(TypeDeclaration<?> typeDecl) {
    if (typeDecl instanceof ClassOrInterfaceDeclaration classDecl) {
      List<ClassOrInterfaceType> result = new ArrayList<>(classDecl.getExtendedTypes());
      result.addAll(classDecl.getImplementedTypes());
      return result;
    }
    if (typeDecl instanceof EnumDeclaration enumDecl) {
      return enumDecl.getImplementedTypes();
    }
    if (typeDecl instanceof RecordDeclaration recordDecl) {
      return recordDecl.getImplementedTypes();
    }
    // An annotation declaration's only supertype is `java.lang.annotation.Annotation`.
    return Collections.emptyList();
  }

  /**
   * Returns true if the given child of the given type declaration is part of its body, rather than
   * part of its header: its annotations, its modifiers, its name, its type parameters and their
   * bounds, or its supertype names. The member types of a type declaration are in scope in its
   * body, but not in its header.
   *
   * @param typeDecl a JavaParser type declaration
   * @param child a child node of {@code typeDecl}
   * @return true if {@code child} is part of the body of {@code typeDecl}
   */
  private static boolean isInBody(TypeDeclaration<?> typeDecl, Node child) {
    if (containsSame(typeDecl.getMembers(), child)) {
      return true;
    }
    if (typeDecl instanceof EnumDeclaration enumDecl) {
      // An enum's constants are part of its body, but are not among its members.
      return containsSame(enumDecl.getEntries(), child);
    }
    if (typeDecl instanceof RecordDeclaration recordDecl) {
      // A record's components are part of its header, but its member types are in scope in them.
      return containsSame(recordDecl.getParameters(), child);
    }
    return false;
  }

  /**
   * Returns true if one of the given nodes is the given node, compared by reference equality.
   * JavaParser's {@code equals()} is structural, so {@link List#contains} does not distinguish two
   * occurrences of the same type name.
   *
   * @param nodes some JavaParser nodes
   * @param node a JavaParser node
   * @return true if {@code nodes} contains {@code node} itself
   */
  @SuppressWarnings("interning:not.interned") // reference equality of AST nodes
  private static boolean containsSame(List<? extends Node> nodes, Node node) {
    for (Node n : nodes) {
      if (n == node) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the element for the member type named {@code firstComponent + suffix} that {@code
   * typeElement} declares or inherits, or null if there is no such member type.
   *
   * <p>{@code typeElement} and its supertypes are searched in breadth-first order, so a member type
   * that is declared in a nearer supertype hides one that is declared in a farther supertype.
   *
   * @param elements used for looking up names
   * @param typeElement the type whose member types to search
   * @param firstComponent the simple name of a member type of {@code typeElement}
   * @param suffix the rest of the type name, which names a type nested within {@code
   *     firstComponent}; it is empty or starts with "."
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it
   * @return the element for the member type, or null if it cannot be determined
   */
  private static @Nullable TypeElement resolveMemberType(
      Elements elements,
      TypeElement typeElement,
      String firstComponent,
      String suffix,
      Map<String, @Nullable TypeElement> cache) {
    Set<TypeElement> visited = new HashSet<>();
    visited.add(typeElement);
    Deque<TypeElement> worklist = new ArrayDeque<>();
    worklist.add(typeElement);
    while (!worklist.isEmpty()) {
      TypeElement current = worklist.remove();
      for (TypeElement member : ElementFilter.typesIn(current.getEnclosedElements())) {
        // A private member type is not inherited.
        if (member.getSimpleName().contentEquals(firstComponent)
            && !member.getModifiers().contains(Modifier.PRIVATE)) {
          if (suffix.isEmpty()) {
            return member;
          }
          return getTypeElement(elements, member.getQualifiedName() + suffix, cache);
        }
      }
      for (TypeElement supertype : ElementUtils.getDirectSuperTypeElements(current, elements)) {
        if (visited.add(supertype)) {
          worklist.add(supertype);
        }
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

  /**
   * Returns the TypeMirror for the given JavaParser type, or null if it cannot be determined. It
   * cannot be determined for an intersection type, a union type, {@code var}, a wildcard, a type
   * parameter declaration, or a type that is not on the classpath.
   *
   * <p>A client that converts many types should call {@link #typeToTypeMirror(Elements, Types,
   * Type, Map)}, which memoizes the name lookups.
   *
   * @param elements used for looking up names
   * @param types used for creating types
   * @param type a JavaParser type
   * @return the TypeMirror for {@code type}, or null if it cannot be determined
   */
  public static @Nullable TypeMirror typeToTypeMirror(Elements elements, Types types, Type type) {
    return typeToTypeMirror(elements, types, type, new HashMap<>());
  }

  /**
   * Returns the TypeMirror for the given JavaParser type, or null if it cannot be determined. It
   * cannot be determined for an intersection type, a union type, {@code var}, a wildcard, a type
   * parameter declaration, or a type that is not on the classpath.
   *
   * @param elements used for looking up names
   * @param types used for creating types
   * @param type a JavaParser type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it. See {@link #resolveTypeName(Elements, ClassOrInterfaceType, Map)} for
   *     restrictions on it.
   * @return the TypeMirror for {@code type}, or null if it cannot be determined
   */
  public static @Nullable TypeMirror typeToTypeMirror(
      Elements elements, Types types, Type type, Map<String, @Nullable TypeElement> cache) {
    if (type instanceof ArrayType arrayType) {
      TypeMirror componentType =
          typeToTypeMirror(elements, types, arrayType.getComponentType(), cache);
      return componentType == null ? null : types.getArrayType(componentType);
    }
    if (type instanceof PrimitiveType primitiveType) {
      return types.getPrimitiveType(typeKindForPrimitive(primitiveType));
    }
    if (type instanceof VoidType) {
      return types.getNoType(TypeKind.VOID);
    }
    if (type instanceof ClassOrInterfaceType classType) {
      TypeElement typeElt = resolveTypeName(elements, classType, cache);
      return typeElt == null ? null : typeElt.asType();
    }
    // An intersection type, a union type, `var`, a wildcard, a type parameter declaration, etc.
    return null;
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
