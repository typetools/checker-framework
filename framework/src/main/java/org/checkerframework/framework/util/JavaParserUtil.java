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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
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
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
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
   * type that is not on the classpath. To learn whether the name names a type variable, and which
   * one, call {@link #resolveTypeVariableName(Elements, ClassOrInterfaceType, Map)}.
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
    return resolveTypeName(elements, type, new HashMap<>(4));
  }

  /**
   * Returns the element for the given JavaParser type, whose name is resolved in the scope of the
   * type declarations and the compilation unit that contain it. Returns null if the name cannot be
   * resolved: it names a type variable, a local class, a member of a local or anonymous class, or a
   * type that is not on the classpath. To learn whether the name names a type variable, and which
   * one, call {@link #resolveTypeVariableName(Elements, ClassOrInterfaceType, Map)}.
   *
   * <p>Resolving one name looks up many candidate names, most of which name no type, so a client
   * that resolves many names should pass the same cache to each call. A cache should not be reused
   * across annotation processing rounds or across {@code Elements} instances: a name that names no
   * type in one round might name a generated type in a later round.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it
   * @return the element for {@code type}, or null if it cannot be determined
   */
  public static @Nullable TypeElement resolveTypeName(
      Elements elements, ClassOrInterfaceType type, Map<String, @Nullable TypeElement> cache) {
    return resolveName(elements, type, cache).typeElement();
  }

  /**
   * Returns the declaration of the type variable that the given JavaParser type names, or null if
   * the name does not name a type variable. The name is resolved in the scope of the type
   * declarations and the compilation unit that contain it, so the result is null if a type
   * declaration shadows a type variable of the same name.
   *
   * <p>A client that resolves many names should call {@link #resolveTypeVariableName(Elements,
   * ClassOrInterfaceType, Map)}, which memoizes the name lookups.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @return the declaration of the type variable that {@code type} names, or null if {@code type}
   *     does not name a type variable
   */
  public static @Nullable TypeParameter resolveTypeVariableName(
      Elements elements, ClassOrInterfaceType type) {
    return resolveTypeVariableName(elements, type, new HashMap<>(4));
  }

  /**
   * Returns the declaration of the type variable that the given JavaParser type names, or null if
   * the name does not name a type variable. The name is resolved in the scope of the type
   * declarations and the compilation unit that contain it, so the result is null if a type
   * declaration shadows a type variable of the same name.
   *
   * <p>If the name has more than one component, as in {@code T.Inner}, then its first component is
   * the one that might name a type variable.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it
   * @return the declaration of the type variable that {@code type} names, or null if {@code type}
   *     does not name a type variable
   */
  public static @Nullable TypeParameter resolveTypeVariableName(
      Elements elements, ClassOrInterfaceType type, Map<String, @Nullable TypeElement> cache) {
    return resolveName(elements, type, cache).typeParameter();
  }

  /**
   * What a type name refers to: a type, a type variable, or neither. At most one component is
   * non-null.
   *
   * @param typeElement the type that the name refers to, or null if it refers to no type
   * @param typeParameter the declaration of the type variable that the name refers to, or null if
   *     it refers to no type variable
   */
  private record ResolvedName(
      @Nullable TypeElement typeElement, @Nullable TypeParameter typeParameter) {

    /** A name that refers to neither a type nor a type variable. */
    private static final ResolvedName NONE = new ResolvedName(null, null);

    /**
     * Returns a ResolvedName for the given type, or {@link #NONE} if it is null.
     *
     * @param typeElement a type, or null
     * @return a ResolvedName for {@code typeElement}
     */
    private static ResolvedName of(@Nullable TypeElement typeElement) {
      return typeElement == null ? NONE : new ResolvedName(typeElement, null);
    }

    /**
     * Returns a ResolvedName for the given type variable declaration.
     *
     * @param typeParameter the declaration of a type variable
     * @return a ResolvedName for {@code typeParameter}
     */
    private static ResolvedName of(TypeParameter typeParameter) {
      return new ResolvedName(null, typeParameter);
    }
  }

  /**
   * Returns what the given JavaParser type's name refers to: a type, a type variable, or neither.
   * The name is resolved in the scope of the type declarations and the compilation unit that
   * contain it.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it
   * @return what the name of {@code type} refers to
   */
  private static ResolvedName resolveName(
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
    // within a class body, where the class's member types are in scope, from a use elsewhere in
    // the class declaration -- in its annotations, its type parameter section, or its supertype
    // names -- where they are not.
    Node child = type;
    for (Node ancestor = type.getParentNode().orElse(null);
        ancestor != null;
        child = ancestor, ancestor = ancestor.getParentNode().orElse(null)) {
      if (declaresLocalType(ancestor, firstComponent)) {
        // `name` names a local class, or is nested within one.  A local class shadows any type of
        // the same name, including a type parameter, and `Elements` cannot look up a local class
        // by name.
        return ResolvedName.NONE;
      }

      // A member type that `ancestor` declares shadows a type parameter of `ancestor` that has the
      // same name, so the member types are searched first.  A member type that `ancestor` merely
      // inherits does not shadow a type parameter, so those are searched below, after the type
      // parameters.
      if (ancestor instanceof TypeDeclaration<?> enclosingType
          && declaresMemberType(enclosingType.getMembers(), firstComponent)
          && containsSame(scopeOfMemberTypes(enclosingType), child)) {
        String enclosingName = nameableFullyQualifiedName(enclosingType);
        if (enclosingName == null) {
          // A member type of an unnameable class has no name that `Elements` can look up.
          return ResolvedName.NONE;
        }
        // If `name` has a suffix, then the suffix names a type that is nested within the member
        // type.  If there is no such type, then `name` names nothing, because the member type
        // shadows every other type whose name starts with `firstComponent`.
        return ResolvedName.of(getTypeElement(elements, enclosingName + "." + name, cache));
      }

      if (ancestor instanceof NodeWithTypeParameters<?> genericDeclaration) {
        for (TypeParameter typeParameter : genericDeclaration.getTypeParameters()) {
          if (typeParameter.getNameAsString().equals(firstComponent)) {
            // `name` names a type parameter, or is nested within one.  A type parameter shadows
            // any type of the same name that is declared outside `ancestor`, and it has no
            // TypeElement.
            return ResolvedName.of(typeParameter);
          }
        }
      }

      if (ancestor instanceof EnumConstantDeclaration enumConstant
          && containsSame(enumConstant.getClassBody(), child)
          && declaresMemberType(enumConstant.getClassBody(), firstComponent)) {
        // The body of an enum constant declares an anonymous class, whose member types are in
        // scope only in that body and not in the constant's arguments.  A member type of an
        // anonymous class has no name that `Elements` can look up.  (There is no need to search
        // the anonymous class's supertype, which is the enum:  the enum declaration is an
        // ancestor, so a later iteration of this loop searches it.)
        return ResolvedName.NONE;
      }

      // The member types of a class that `Elements` cannot look up by name -- an anonymous class,
      // a local class, or a class that is nested within one -- shadow types that are declared
      // outside the class.  `unnameableSupertypes` is non-null if `type` appears within the body
      // of such a class, in which case it holds the class's supertypes, which this method searches
      // for a member type that `name` might refer to.
      List<ClassOrInterfaceType> unnameableSupertypes = null;
      if (ancestor instanceof ObjectCreationExpr creation) {
        List<? extends Node> body = creation.getAnonymousClassBody().orElse(null);
        // The anonymous class's member types are in scope only in its body, and not in the
        // creation expression's scope, type arguments, supertype name, or arguments.  Testing
        // `child` also prevents infinite recursion on the recursive call below.
        if (body != null && containsSame(body, child)) {
          if (declaresMemberType(body, firstComponent)) {
            // A member type of an anonymous class has no name that `Elements` can look up.
            return ResolvedName.NONE;
          }
          unnameableSupertypes = Collections.singletonList(creation.getType());
        }
      } else if (ancestor instanceof TypeDeclaration<?> enclosingType
          // The class's inherited member types are in scope only in its body, and not in its
          // annotations, its type parameter section, or its supertype names.  Testing `child`
          // also prevents infinite recursion on the recursive call below.  (The class's declared
          // member types are searched above, before its type parameters.)
          && containsSame(scopeOfMemberTypes(enclosingType), child)) {
        String enclosingName = nameableFullyQualifiedName(enclosingType);
        if (enclosingName != null) {
          // `enclosingType` does not declare the member type, or the search above would have
          // ended, but it might inherit it.
          TypeElement enclosingElement = getTypeElement(elements, enclosingName, cache);
          if (enclosingElement != null) {
            TypeElement result =
                resolveMemberType(elements, enclosingElement, firstComponent, suffix, cache);
            if (result != null) {
              return ResolvedName.of(result);
            }
          }
        } else {
          unnameableSupertypes = supertypes(enclosingType);
        }
      }
      if (unnameableSupertypes != null) {
        for (ClassOrInterfaceType supertype : unnameableSupertypes) {
          TypeElement supertypeElement = resolveTypeName(elements, supertype, cache);
          if (supertypeElement == null) {
            // The supertype could not be determined, so neither could the member types that the
            // unnameable class inherits and that might shadow `name`.
            return ResolvedName.NONE;
          }
          TypeElement result =
              resolveMemberType(elements, supertypeElement, firstComponent, suffix, cache);
          if (result != null) {
            return ResolvedName.of(result);
          }
        }
      }
    }

    CompilationUnit cu = type.findCompilationUnit().orElse(null);
    if (cu == null) {
      // The name might be fully-qualified.
      return ResolvedName.of(getTypeElement(elements, name, cache));
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
          return ResolvedName.of(result);
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
              return ResolvedName.of(result);
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
        return ResolvedName.of(result);
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
            return ResolvedName.of(result);
          }
        }
      }
    }

    // The name might be fully-qualified, or might be a top-level type in the unnamed package.
    // This lookup is last, because a type that is in scope shadows a type whose fully-qualified
    // name is `name`.
    return ResolvedName.of(getTypeElement(elements, name, cache));
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
   * interface, enum, or record whose name is {@code name}. Such a declaration shadows, throughout
   * the block that contains it, every type of the same name that is declared elsewhere.
   *
   * @param node a JavaParser node, such as a block
   * @param name a simple type name
   * @return true if {@code node} declares a local type named {@code name}
   */
  private static boolean declaresLocalType(Node node, String name) {
    for (Node child : node.getChildNodes()) {
      if (child instanceof Statement) {
        // A local type declaration is the only kind of statement whose child is a type
        // declaration.
        for (Node grandchild : child.getChildNodes()) {
          if (grandchild instanceof TypeDeclaration<?> localType
              && localType.getNameAsString().equals(name)) {
            return true;
          }
        }
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
   * Returns the supertypes of the given type declaration from which it can inherit a member type:
   * those that its {@code extends} clause and its {@code implements} clause name, plus the implicit
   * superclass {@code java.lang.Enum} of an enum, which declares the member type {@code
   * Enum.EnumDesc}. The result does not include the other implicit supertypes -- {@code
   * java.lang.Object}, {@code java.lang.Record}, and {@code java.lang.annotation.Annotation} --
   * none of which declares a member type.
   *
   * @param typeDecl a JavaParser type declaration
   * @return the supertypes of {@code typeDecl} that might declare a member type
   */
  private static List<ClassOrInterfaceType> supertypes(TypeDeclaration<?> typeDecl) {
    if (typeDecl instanceof ClassOrInterfaceDeclaration classDecl) {
      List<ClassOrInterfaceType> result = new ArrayList<>(classDecl.getExtendedTypes());
      result.addAll(classDecl.getImplementedTypes());
      return result;
    }
    if (typeDecl instanceof EnumDeclaration enumDecl) {
      // Copy the list rather than side-effecting the AST by adding to it.
      List<ClassOrInterfaceType> result = new ArrayList<>(enumDecl.getImplementedTypes());
      result.add(javaLangEnum());
      return result;
    }
    if (typeDecl instanceof RecordDeclaration recordDecl) {
      return recordDecl.getImplementedTypes();
    }
    // An annotation declaration's only supertype is `java.lang.annotation.Annotation`.
    return Collections.emptyList();
  }

  /**
   * Returns a new JavaParser type that names {@code java.lang.Enum}, the implicit superclass of
   * every enum. The result is not part of any AST, so {@link #resolveTypeName} resolves its name as
   * a fully-qualified name.
   *
   * @return a JavaParser type that names {@code java.lang.Enum}
   */
  private static ClassOrInterfaceType javaLangEnum() {
    return new ClassOrInterfaceType(
        new ClassOrInterfaceType(new ClassOrInterfaceType(null, "java"), "lang"), "Enum");
  }

  /**
   * Returns the children of the given type declaration in which its member types are in scope: its
   * body declarations, an enum's constants, and a record's components. The result does not include
   * a child in which they are not in scope: an annotation on the declaration, a type parameter, or
   * a supertype name.
   *
   * @param typeDecl a JavaParser type declaration
   * @return the children of {@code typeDecl} in which its member types are in scope
   */
  private static List<? extends Node> scopeOfMemberTypes(TypeDeclaration<?> typeDecl) {
    if (typeDecl instanceof EnumDeclaration enumDecl) {
      // A member type is in scope in a constant's arguments and in its class body.
      List<Node> result = new ArrayList<>(enumDecl.getMembers());
      result.addAll(enumDecl.getEntries());
      return result;
    }
    if (typeDecl instanceof RecordDeclaration recordDecl) {
      // A member type is in scope in the type of a record component.
      List<Node> result = new ArrayList<>(recordDecl.getMembers());
      result.addAll(recordDecl.getParameters());
      return result;
    }
    return typeDecl.getMembers();
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
   * A type whose member types {@link #resolveMemberType} searches, together with whether the type's
   * package-private member types are inherited by the type at which the search started.
   *
   * @param typeElement the type whose member types to search
   * @param packagePrivateIsInherited true if a package-private member type of {@code typeElement}
   *     is a member of the type at which the search started
   */
  private record SearchedType(TypeElement typeElement, boolean packagePrivateIsInherited) {}

  /**
   * Returns the element for the member type named {@code firstComponent + suffix} that {@code
   * typeElement} declares or inherits, or null if there is no such member type.
   *
   * <p>{@code typeElement} and its supertypes are searched in breadth-first order, so a member type
   * that is declared in a nearer supertype hides one that is declared in a farther supertype. A
   * declaration hides whatever its declaring type would otherwise inherit, even if the declaration
   * is not itself inherited: a private member type is inherited by no type, and a package-private
   * member type is inherited only within its own package.
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
    Deque<SearchedType> worklist = new ArrayDeque<>();
    // Every member type that `typeElement` declares is a member of `typeElement`, whatever its
    // access modifier is.
    worklist.add(new SearchedType(typeElement, true));
    while (!worklist.isEmpty()) {
      SearchedType current = worklist.remove();
      TypeElement currentElement = current.typeElement();
      // A type declares at most one member type with a given simple name.
      TypeElement declared = null;
      for (TypeElement member : ElementFilter.typesIn(currentElement.getEnclosedElements())) {
        if (member.getSimpleName().contentEquals(firstComponent)) {
          declared = member;
          break;
        }
      }
      if (declared != null) {
        if (isInherited(declared, current.packagePrivateIsInherited())) {
          if (suffix.isEmpty()) {
            return declared;
          }
          return getTypeElement(elements, declared.getQualifiedName() + suffix, cache);
        }
        // `declared` is not a member of the type at which the search started, and it hides every
        // member type of the same name that `currentElement` would otherwise inherit, so do not
        // search the supertypes of `currentElement`.
        continue;
      }
      for (TypeElement supertype :
          ElementUtils.getDirectSuperTypeElements(currentElement, elements)) {
        if (visited.add(supertype)) {
          // `currentElement` inherits a package-private member type of `supertype` only if the two
          // types are in the same package.
          worklist.add(
              new SearchedType(
                  supertype,
                  current.packagePrivateIsInherited()
                      && inSamePackage(elements, supertype, currentElement)));
        }
      }
    }
    return null;
  }

  /**
   * Returns true if {@code member} is a member of the type at which a search by {@link
   * #resolveMemberType} started -- that is, if every type between that type and the type that
   * declares {@code member} inherits it.
   *
   * @param member a member type of the type that is currently being searched
   * @param packagePrivateIsInherited true if a package-private member type of the type that is
   *     currently being searched is a member of the type at which the search started
   * @return true if {@code member} is a member of the type at which the search started
   */
  private static boolean isInherited(TypeElement member, boolean packagePrivateIsInherited) {
    Set<Modifier> modifiers = member.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE)) {
      // A private member type is not inherited.
      return false;
    }
    if (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED)) {
      return true;
    }
    return packagePrivateIsInherited;
  }

  /**
   * Returns true if the two types are declared in the same package.
   *
   * @param elements used for looking up the package that contains a type
   * @param type1 a type
   * @param type2 a type
   * @return true if {@code type1} and {@code type2} are declared in the same package
   */
  private static boolean inSamePackage(Elements elements, TypeElement type1, TypeElement type2) {
    return elements
        .getPackageOf(type1)
        .getQualifiedName()
        .contentEquals(elements.getPackageOf(type2).getQualifiedName());
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
