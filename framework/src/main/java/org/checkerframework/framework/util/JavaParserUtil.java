package org.checkerframework.framework.util;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
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

    CompilationUnit cu = type.findCompilationUnit().orElse(null);
    // The package that contains the use of `name`.  A use in the unnamed package, or in a node
    // that is not part of a compilation unit, has no package name.  Accessibility of a
    // package-private member type depends on this package.
    String usePackage =
        cu == null ? "" : cu.getPackageDeclaration().map(pkg -> pkg.getNameAsString()).orElse("");

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

      // A local type declaration is a block statement, so only a block or a switch entry can
      // directly contain one.
      if ((ancestor instanceof BlockStmt || ancestor instanceof SwitchEntry)
          && declaresLocalType(ancestor, firstComponent, child)) {
        // `name` names a local class, or is nested within one.  A local class shadows any type of
        // the same name, and `Elements` cannot look up a local class by name.
        return null;
      }

      if (ancestor instanceof EnumConstantDeclaration enumConstant
          && containsSame(enumConstant.getClassBody(), child)
          && declaresMemberType(enumConstant.getClassBody(), firstComponent)) {
        // The body of an enum constant declares an anonymous class, whose member types are in
        // scope only in that body and not in the constant's arguments.  A member type of an
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
        // The anonymous class's member types are in scope only in its body, and not in the
        // creation expression's scope, type arguments, supertype name, or arguments.  Testing
        // `child` also prevents infinite recursion on the recursive call below.
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
          // The class's member types, declared and inherited, are in scope only in its body, and
          // not in its annotations, its type parameter section, or its supertype names.  Testing
          // `child` also prevents infinite recursion on the recursive call below.
          && inScopeOfMemberTypes(child)) {
        String enclosingName = nameableFullyQualifiedName(enclosingType);
        if (enclosingName != null) {
          TypeElement result = getTypeElement(elements, enclosingName + "." + name, cache);
          if (result != null) {
            return result;
          }
          // The enclosing type might inherit the member type rather than declare it.
          TypeElement enclosingElement = getTypeElement(elements, enclosingName, cache);
          if (enclosingElement != null) {
            result =
                resolveMemberType(
                    elements, enclosingElement, firstComponent, suffix, usePackage, true, cache);
            if (result != null) {
              return result;
            }
          }
        } else {
          if (declaresMemberType(enclosingType.getMembers(), firstComponent)) {
            // A member type of an unnameable class has no name that `Elements` can look up.
            return null;
          }
          unnameableSupertypes = supertypes(enclosingType);
        }
      }
      if (unnameableSupertypes != null) {
        // Every direct supertype is searched, rather than returning the first member type that is
        // found, because a member type that is inherited from one supertype does not hide one that
        // is inherited from another.
        TypeElement inherited = null;
        for (ClassOrInterfaceType supertype : unnameableSupertypes) {
          TypeElement supertypeElement = resolveTypeName(elements, supertype, cache);
          if (supertypeElement == null) {
            // The supertype could not be determined, so neither could the member types that the
            // unnameable class inherits and that might shadow `name`.
            return null;
          }
          TypeElement fromSupertype =
              resolveMemberType(
                  elements, supertypeElement, firstComponent, suffix, usePackage, true, cache);
          if (fromSupertype != null) {
            if (inherited == null) {
              inherited = fromSupertype;
            } else if (!inherited.equals(fromSupertype)) {
              // The class inherits two different member types with the same simple name.  Which
              // one `name` refers to (if either is accessible) cannot be determined here.
              return null;
            }
          }
        }
        if (inherited != null) {
          return inherited;
        }
      }
    }

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
            result =
                resolveMemberType(
                    elements, containerElement, firstComponent, suffix, usePackage, false, cache);
            if (result != null) {
              return result;
            }
          }
        }
      }
    }

    // The type might be in the same package, in a package or type that is imported on demand, or
    // in `java.lang`.  A type in the same package shadows the others, so it is looked up first.  A
    // name in the unnamed package has no prefix.  An import on demand, whether static or not,
    // imports the member types that the named type inherits as well as those it declares.
    List<String> containerPrefixes = new ArrayList<>();
    containerPrefixes.add(usePackage.isEmpty() ? "" : usePackage + ".");
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (importDecl.isAsterisk()) {
        containerPrefixes.add(importDecl.getNameAsString() + ".");
      }
    }
    containerPrefixes.add("java.lang.");
    for (String containerPrefix : containerPrefixes) {
      // If the prefix names a type rather than a package, then the import on demand imports only
      // the member types that the type declares or inherits *and* that are accessible at the use
      // site, so the member type is looked up rather than merely its qualified name.
      TypeElement containerElement =
          containerPrefix.isEmpty()
              ? null
              : getTypeElement(
                  elements, containerPrefix.substring(0, containerPrefix.length() - 1), cache);
      if (containerElement != null) {
        TypeElement result =
            resolveMemberType(
                elements, containerElement, firstComponent, suffix, usePackage, false, cache);
        if (result != null) {
          return result;
        }
        // This type imports no accessible member type with this name, but another import on demand
        // might, so the search continues.
      } else {
        TypeElement result = getTypeElement(elements, containerPrefix + name, cache);
        if (result != null) {
          return result;
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
      // Copy the list rather than side-effecting the AST by adding to it.
      return new ArrayList<>(recordDecl.getImplementedTypes());
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
   * Returns true if the member types of a type declaration are in scope in the given child of it:
   * one of its body declarations, an enum's constant, or a record's component. They are not in
   * scope in its other children: an annotation on the declaration, a type parameter, a supertype
   * name, or a permitted subtype name.
   *
   * @param child a child of a JavaParser type declaration
   * @return true if the type declaration's member types are in scope in {@code child}
   */
  private static boolean inScopeOfMemberTypes(Node child) {
    // A member is a body declaration, and so is an enum constant (whose arguments and class body
    // are both in the scope of the member types).  A record's component is a `Parameter`; a type
    // declaration has no other child of that type.
    return child instanceof BodyDeclaration<?> || child instanceof Parameter;
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
   * <p>{@code typeElement} and its supertypes are searched in breadth-first order. If two
   * supertypes that are equally near declare different member types with this name, then neither
   * hides the other, the name is ambiguous, and this method returns null. Java considers the name
   * ambiguous even if the two supertypes are at different distances, because neither declaration
   * hides the other (see below); this method returns the nearer declaration, which affects no valid
   * program, because a use of an ambiguous name does not compile.
   *
   * <p>A member type declaration hides every declaration of the same name in a supertype of the
   * type that declares it, even if the declaration is not inherited or is not accessible. Such a
   * declaration therefore ends the search through the type that contains it: the supertypes of that
   * type are not searched, and if the declaration is not inherited or is not accessible, then that
   * type contributes no member type at all.
   *
   * <p>A private member type is not inherited. A package-private member type is inherited only by a
   * subclass in the package that declares it, and it is accessible only within that package; this
   * method therefore uses one only if every type from {@code typeElement} to the type that declares
   * it is in package {@code usePackage}. A protected member type is inherited even from a different
   * package, but outside the package that declares it, it is accessible only within the body of a
   * subclass of the type that declares it; this method therefore uses one that is declared in
   * another package only if {@code inSubclassBody} is true.
   *
   * @param elements used for looking up names
   * @param typeElement the type whose member types to search
   * @param firstComponent the simple name of a member type of {@code typeElement}
   * @param suffix the rest of the type name, which names a type nested within {@code
   *     firstComponent}; it is empty or starts with "."
   * @param usePackage the name of the package that contains the use of the type name, or "" for the
   *     unnamed package
   * @param inSubclassBody true if the use of the type name is within the body of {@code
   *     typeElement} or of a subclass of it, as it is for an ordinary lookup in the scope of a
   *     class, but not for the lookup that an import performs
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it
   * @return the element for the member type, or null if there is none or it cannot be determined
   */
  private static @Nullable TypeElement resolveMemberType(
      Elements elements,
      TypeElement typeElement,
      String firstComponent,
      String suffix,
      String usePackage,
      boolean inSubclassBody,
      Map<String, @Nullable TypeElement> cache) {
    Set<TypeElement> visited = new HashSet<>();
    visited.add(typeElement);
    // The types that are the same distance from `typeElement`:  first `typeElement` itself, then
    // its direct supertypes, and so forth.  Each one maps to true if it, and every type between it
    // and `typeElement`, is in package `usePackage` -- which is what makes a package-private member
    // type that it declares both inherited by `typeElement` and accessible at the use site.
    Map<TypeElement, Boolean> currentTypes = new LinkedHashMap<>();
    currentTypes.put(typeElement, isInPackage(elements, typeElement, usePackage));
    while (!currentTypes.isEmpty()) {
      // Every type at the current distance is searched, rather than returning the first member
      // type that is found, because a member type that is declared in one of them does not hide
      // one that is declared in another.
      TypeElement found = null;
      Map<TypeElement, Boolean> nextTypes = new LinkedHashMap<>();
      for (Map.Entry<TypeElement, Boolean> entry : currentTypes.entrySet()) {
        TypeElement current = entry.getKey();
        boolean samePackagePath = entry.getValue();
        TypeElement member = declaredMemberType(current, firstComponent);
        if (member != null) {
          // This declaration hides every declaration of the same name in a supertype of `current`,
          // so the supertypes of `current` are not searched.
          boolean protectedIsAccessible =
              inSubclassBody || isInPackage(elements, current, usePackage);
          if (isInheritedAndAccessible(member, samePackagePath, protectedIsAccessible)) {
            if (found == null) {
              found = member;
            } else if (!found.equals(member)) {
              // Two equally near supertypes declare different member types with this name, so the
              // name is ambiguous.
              return null;
            }
          }
          continue;
        }
        for (TypeElement supertype : ElementUtils.getDirectSuperTypeElements(current, elements)) {
          if (!visited.contains(supertype)) {
            // A supertype that more than one path reaches at this distance is searched once.  A
            // package-private member type that it declares is inherited if any of those paths
            // stays within package `usePackage`.
            nextTypes.merge(
                supertype,
                samePackagePath && isInPackage(elements, supertype, usePackage),
                (b1, b2) -> b1 || b2);
          }
        }
      }
      if (found != null) {
        if (suffix.isEmpty()) {
          return found;
        }
        return getTypeElement(elements, found.getQualifiedName() + suffix, cache);
      }
      visited.addAll(nextTypes.keySet());
      currentTypes = nextTypes;
    }
    return null;
  }

  /**
   * Returns the member type that {@code typeElement} declares with the given simple name, or null
   * if it declares none. A type declares at most one member type with a given simple name.
   *
   * @param typeElement a type
   * @param name a simple name
   * @return the member type that {@code typeElement} declares with the given simple name, or null
   *     if it declares none
   */
  private static @Nullable TypeElement declaredMemberType(TypeElement typeElement, String name) {
    for (TypeElement member : ElementFilter.typesIn(typeElement.getEnclosedElements())) {
      if (member.getSimpleName().contentEquals(name)) {
        return member;
      }
    }
    return null;
  }

  /**
   * Returns true if the given type is in the given package.
   *
   * @param elements used for looking up names
   * @param typeElement a type
   * @param packageName the name of a package, or "" for the unnamed package
   * @return true if {@code typeElement} is in the package named {@code packageName}
   */
  private static boolean isInPackage(
      Elements elements, TypeElement typeElement, String packageName) {
    return elements.getPackageOf(typeElement).getQualifiedName().contentEquals(packageName);
  }

  /**
   * Returns true if the given member type is inherited by the subtypes of the type that declares
   * it, and is accessible at the use site.
   *
   * @param member a member type
   * @param samePackagePath true if the type that declares {@code member}, and every type between it
   *     and the type whose member types are being searched, is in the package that contains the use
   *     of the type name
   * @param protectedIsAccessible true if a protected member of the type that declares {@code
   *     member} is accessible at the use site
   * @return true if {@code member} is inherited and is accessible at the use site
   */
  private static boolean isInheritedAndAccessible(
      TypeElement member, boolean samePackagePath, boolean protectedIsAccessible) {
    Set<Modifier> modifiers = member.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE)) {
      // A private member type is not inherited.
      return false;
    }
    if (modifiers.contains(Modifier.PUBLIC)) {
      return true;
    }
    if (modifiers.contains(Modifier.PROTECTED)) {
      // A protected member type is inherited even by a subclass in a different package, but
      // outside the package that declares it, it is accessible only within the body of such a
      // subclass.
      return protectedIsAccessible;
    }
    // A package-private member type is inherited only by a subclass in the package that declares
    // it, and it is accessible only within that package.
    return samePackagePath;
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
