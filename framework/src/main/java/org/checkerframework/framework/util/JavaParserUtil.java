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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
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
   * type that is not on the classpath. To learn whether the name names a type variable, and which
   * one, call {@link #resolveTypeNameAsTypeParameter(Elements, ClassOrInterfaceType, Map)}.
   *
   * <p>A client that resolves many names should call {@link #resolveTypeNameAsTypeElement(Elements,
   * ClassOrInterfaceType, Map)}, which memoizes the name lookups.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @return the element for {@code type}, or null if it cannot be determined
   */
  public static @Nullable TypeElement resolveTypeNameAsTypeElement(
      Elements elements, ClassOrInterfaceType type) {
    return resolveTypeNameAsTypeElement(elements, type, new HashMap<>());
  }

  /**
   * Returns the element for the given JavaParser type, whose name is resolved in the scope of the
   * type declarations and the compilation unit that contain it. Returns null if the name cannot be
   * resolved: it names a type variable, a local class, a member of a local or anonymous class, or a
   * type that is not on the classpath. To learn whether the name names a type variable, and which
   * one, call {@link #resolveTypeNameAsTypeParameter(Elements, ClassOrInterfaceType, Map)}.
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
  public static @Nullable TypeElement resolveTypeNameAsTypeElement(
      Elements elements, ClassOrInterfaceType type, Map<String, @Nullable TypeElement> cache) {
    return resolveTypeName(elements, type, cache).typeElement();
  }

  /**
   * Returns the declaration of the type variable that the given JavaParser type names, or null if
   * the name does not name a type variable. The name is resolved in the scope of the type
   * declarations and the compilation unit that contain it, so the result is null if a type
   * declaration shadows a type variable of the same name.
   *
   * <p>A client that resolves many names should call {@link
   * #resolveTypeNameAsTypeParameter(Elements, ClassOrInterfaceType, Map)}, which memoizes the name
   * lookups.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @return the declaration of the type variable that {@code type} names, or null if {@code type}
   *     does not name a type variable
   */
  public static @Nullable TypeParameter resolveTypeNameAsTypeParameter(
      Elements elements, ClassOrInterfaceType type) {
    return resolveTypeNameAsTypeParameter(elements, type, new HashMap<>(4));
  }

  /**
   * Returns the declaration of the type variable that the given JavaParser type names, or null if
   * the name does not name a type variable. The name is resolved in the scope of the type
   * declarations and the compilation unit that contain it, so the result is null if a type
   * declaration shadows a type variable of the same name.
   *
   * <p>A name with more than one component, as in {@code T.Inner}, never names a type variable,
   * because a type variable has no member types.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it
   * @return the declaration of the type variable that {@code type} names, or null if {@code type}
   *     does not name a type variable
   */
  public static @Nullable TypeParameter resolveTypeNameAsTypeParameter(
      Elements elements, ClassOrInterfaceType type, Map<String, @Nullable TypeElement> cache) {
    return resolveTypeName(elements, type, cache).typeParameter();
  }

  /**
   * What a type name refers to: a type, a type variable, or neither. At most one component is
   * non-null.
   *
   * @param typeElement the type that the name refers to, or null if it refers to no type
   * @param typeParameter the declaration of the type variable that the name refers to, or null if
   *     it refers to no type variable
   */
  public record ResolvedTypeName(
      @Nullable TypeElement typeElement, @Nullable TypeParameter typeParameter) {

    /** A name that refers to neither a type nor a type variable. */
    private static final ResolvedTypeName NONE = new ResolvedTypeName(null, null);

    /**
     * Returns a ResolvedTypeName for the given type, or {@link #NONE} if it is null.
     *
     * @param typeElement a type, or null
     * @return a ResolvedTypeName for {@code typeElement}
     */
    private static ResolvedTypeName of(@Nullable TypeElement typeElement) {
      return typeElement == null ? NONE : new ResolvedTypeName(typeElement, null);
    }

    /**
     * Returns a ResolvedTypeName for the given type variable declaration.
     *
     * @param typeParameter the declaration of a type variable
     * @return a ResolvedTypeName for {@code typeParameter}
     */
    private static ResolvedTypeName of(TypeParameter typeParameter) {
      return new ResolvedTypeName(null, typeParameter);
    }
  }

  /**
   * Returns what the given JavaParser type's name refers to: a type, a type variable, or neither.
   * The name is resolved in the scope of the type declarations and the compilation unit that
   * contain it.
   *
   * <p>Resolving a name walks the enclosing scopes and searches supertypes, which is far more work
   * than the cache avoids. A client that needs to know both whether the name names a type and
   * whether it names a type variable should call this method once, rather than calling both {@link
   * #resolveTypeNameAsTypeElement(Elements, ClassOrInterfaceType, Map)} and {@link
   * #resolveTypeNameAsTypeParameter(Elements, ClassOrInterfaceType, Map)}, each of which repeats
   * the walk.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it. See {@link #resolveTypeNameAsTypeElement(Elements,
   *     ClassOrInterfaceType, Map)} for restrictions on it.
   * @return what the name of {@code type} refers to
   */
  public static ResolvedTypeName resolveTypeName(
      Elements elements, ClassOrInterfaceType type, Map<String, @Nullable TypeElement> cache) {
    return resolveTypeName(elements, type, cache, new IdentityHashMap<>(4));
  }

  /**
   * Returns what the given JavaParser type's name refers to, as {@link #resolveTypeName(Elements,
   * ClassOrInterfaceType, Map)} does. This method also memoizes its recursive calls.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it
   * @param nodeCache memoizes the recursive calls that this method makes; this method both reads
   *     and writes it
   * @return what the name of {@code type} refers to
   */
  private static ResolvedTypeName resolveTypeName(
      Elements elements,
      ClassOrInterfaceType type,
      Map<String, @Nullable TypeElement> cache,
      IdentityHashMap<ClassOrInterfaceType, @Nullable TypeElement> nodeCache) {
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
    // The package that contains `type`.  The empty string names the unnamed package, which is also
    // the fallback when `type` is not in a compilation unit.
    String packageName =
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
      // A local type declaration is a block statement, so only a block or a switch entry can
      // directly contain one.
      if ((ancestor instanceof BlockStmt || ancestor instanceof SwitchEntry)
          && declaresLocalType(ancestor, firstComponent, child)) {
        // `name` names a local class, or is nested within one.  A local class shadows any type of
        // the same name, including a type parameter, and `Elements` cannot look up a local class
        // by name.
        return ResolvedTypeName.NONE;
      }

      // A member type that `ancestor` declares shadows a type parameter of `ancestor` that has the
      // same name, so search the member types first.  A member type that `ancestor` merely
      // inherits does not shadow a type parameter, so those are searched below, after the type
      // parameters.
      if (ancestor instanceof TypeDeclaration<?> enclosingType
          && declaresMemberType(enclosingType.getMembers(), firstComponent)
          && inScopeOfMemberTypes(child)) {
        String enclosingName = nameableFullyQualifiedName(enclosingType);
        if (enclosingName == null) {
          // A member type of an unnameable class has no name that `Elements` can look up.
          return ResolvedTypeName.NONE;
        }
        TypeElement enclosingElement = getTypeElement(elements, enclosingName, cache);
        if (enclosingElement == null) {
          return ResolvedTypeName.NONE;
        }
        // If `name` has a suffix, then the suffix names a type that is nested within the member
        // type.  If there is no such type, then `name` names nothing, because the member type
        // shadows every other type whose name starts with `firstComponent`.
        return ResolvedTypeName.of(
            resolveMemberType(
                elements,
                // Every member type that `enclosingElement` declares is a member of it, whatever
                // its access modifier is.
                new SearchedType(enclosingElement, true, true, true),
                firstComponent,
                suffix,
                packageName,
                true,
                cache));
      }

      if (ancestor instanceof NodeWithTypeParameters<?> genericDeclaration) {
        for (TypeParameter typeParameter : genericDeclaration.getTypeParameters()) {
          if (typeParameter.getNameAsString().equals(firstComponent)) {
            // `name` names a type parameter, or is nested within one.  A type parameter shadows
            // any type of the same name that is declared outside `ancestor`, and it has no
            // TypeElement.  A type variable has no member types, so if `name` has a suffix, as
            // in `T.Inner`, then it names nothing at all.
            return suffix.isEmpty() ? ResolvedTypeName.of(typeParameter) : ResolvedTypeName.NONE;
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
        return ResolvedTypeName.NONE;
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
            return ResolvedTypeName.NONE;
          }
          if (creation.getScope().isPresent()) {
            // In `outer.new Inner() { ... }`, `Inner` is a member of the type of `outer` rather
            // than a name that is resolved in the scope of the expression, so this method cannot
            // determine the member types that the anonymous class inherits.
            return ResolvedTypeName.NONE;
          }
          unnameableSupertypes = Collections.singletonList(creation.getType());
        }
      } else if (ancestor instanceof TypeDeclaration<?> enclosingType
          // The class's inherited member types are in scope only in its body, and not in its
          // annotations, its type parameter section, or its supertype names.  Testing `child`
          // also prevents infinite recursion on the recursive call below.  (The class's declared
          // member types were searched above, before its type parameters.)
          && inScopeOfMemberTypes(child)) {
        String enclosingName = nameableFullyQualifiedName(enclosingType);
        if (enclosingName != null) {
          // `enclosingType` does not declare the member type, or the search above would have
          // ended, but it might inherit it.
          TypeElement enclosingElement = getTypeElement(elements, enclosingName, cache);
          if (enclosingElement != null) {
            TypeElement result =
                resolveMemberType(
                    elements,
                    // Every member type that `enclosingElement` declares is a member of it,
                    // whatever its access modifier is.
                    new SearchedType(enclosingElement, true, true, true),
                    firstComponent,
                    suffix,
                    packageName,
                    true,
                    cache);
            if (result != null) {
              return ResolvedTypeName.of(result);
            }
          }
        } else {
          unnameableSupertypes = supertypes(enclosingType);
        }
      }
      if (unnameableSupertypes != null) {
        // Every direct supertype is searched, rather than returning the first member type that is
        // found, because a member type that is inherited from one supertype does not hide one that
        // is inherited from another.
        TypeElement inherited = null;
        for (ClassOrInterfaceType supertype : unnameableSupertypes) {
          TypeElement supertypeElement =
              resolveTypeNameAsTypeElementMemoized(elements, supertype, cache, nodeCache);
          if (supertypeElement == null) {
            // The supertype could not be determined, so neither could the member types that the
            // unnameable class inherits and that might shadow `name`.
            return ResolvedTypeName.NONE;
          }
          TypeElement fromSupertype =
              resolveMemberType(
                  elements,
                  // The unnameable class inherits a package-private member type of
                  // `supertypeElement` only if the two are in the same package.  The unnameable
                  // class is declared in the compilation unit that contains `type`, so its package
                  // is `packageName`.
                  new SearchedType(
                      supertypeElement,
                      false,
                      inPackage(elements, supertypeElement, packageName),
                      true),
                  firstComponent,
                  suffix,
                  packageName,
                  true,
                  cache);
          if (fromSupertype != null) {
            if (inherited == null) {
              inherited = fromSupertype;
            } else if (!inherited.equals(fromSupertype)) {
              // The class inherits two different member types with the same simple name.  Which
              // one `name` refers to (if either is accessible) cannot be determined here.
              return ResolvedTypeName.NONE;
            }
          }
        }
        if (inherited != null) {
          return ResolvedTypeName.of(inherited);
        }
      }
    }

    if (cu == null) {
      // The name might be fully-qualified.
      return ResolvedTypeName.of(getTypeElement(elements, name, cache));
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
          return ResolvedTypeName.of(result);
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
            // An import imports only the member types that `containerElement` inherits; the ones
            // that it declares have canonical names, which the lookup above already tried.  A
            // package-private member type is a member of `containerElement` only if every type
            // from `containerElement` to the type that declares it is in `containerElement`'s
            // package; such a member type is accessible at this use, and therefore imported, only
            // if that package is also `packageName`.  A protected member type that
            // `containerElement` declares is accessible at this use only under the same condition,
            // because an import declaration is not within the body of a subclass.
            boolean containerInUsePackage = inPackage(elements, containerElement, packageName);
            result =
                resolveMemberType(
                    elements,
                    new SearchedType(
                        containerElement, false, containerInUsePackage, containerInUsePackage),
                    firstComponent,
                    suffix,
                    packageName,
                    false,
                    cache);
            if (result != null) {
              return ResolvedTypeName.of(result);
            }
          }
        }
      }
    }

    // The type might be in the same package.  A type in the same package shadows one that is
    // imported on demand, so it is looked up first.  A name in the unnamed package has no prefix.
    TypeElement samePackage =
        getTypeElement(elements, packageName.isEmpty() ? name : packageName + "." + name, cache);
    if (samePackage != null) {
      return ResolvedTypeName.of(samePackage);
    }

    // The type might be in a package or type that is imported on demand, or in `java.lang`, which
    // is imported on demand implicitly.  An import on demand imports only the types that are
    // accessible where it appears, so an inaccessible type does not resolve the name and does not
    // prevent a later import on demand from resolving it.
    List<String> containerPrefixes = new ArrayList<>();
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (importDecl.isAsterisk()) {
        containerPrefixes.add(importDecl.getNameAsString() + ".");
      }
    }
    containerPrefixes.add("java.lang.");
    for (String containerPrefix : containerPrefixes) {
      TypeElement result = getTypeElement(elements, containerPrefix + name, cache);
      if (result != null && isAccessible(elements, result, packageName)) {
        return ResolvedTypeName.of(result);
      }
    }

    // A static import on demand also imports the member types that the named type inherits.  A
    // type import on demand does not:  it imports only the member types that the named type
    // declares, which the lookup above already tried, because they have canonical names.
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (importDecl.isAsterisk() && importDecl.isStatic()) {
        TypeElement importedElement = getTypeElement(elements, importDecl.getNameAsString(), cache);
        if (importedElement != null) {
          // A package-private member type is a member of `importedElement` only if every type from
          // `importedElement` to the type that declares it is in `importedElement`'s package; such
          // a member type is accessible at this use, and therefore imported, only if that package
          // is also `packageName`.  A protected member type that `importedElement` declares is
          // accessible at this use only under the same condition, because an import declaration is
          // not within the body of a subclass.
          boolean importedInUsePackage = inPackage(elements, importedElement, packageName);
          TypeElement result =
              resolveMemberType(
                  elements,
                  new SearchedType(
                      importedElement, false, importedInUsePackage, importedInUsePackage),
                  firstComponent,
                  suffix,
                  packageName,
                  false,
                  cache);
          if (result != null) {
            return ResolvedTypeName.of(result);
          }
        }
      }
    }

    // The name might be fully-qualified, or might be a top-level type in the unnamed package.
    // This lookup is last, because a type that is in scope shadows a type whose fully-qualified
    // name is `name`.
    return ResolvedTypeName.of(getTypeElement(elements, name, cache));
  }

  /**
   * Returns the element for the given JavaParser type, memoizing the result under the identity of
   * the AST node.
   *
   * <p>Resolving a name that appears in the body of an anonymous or local class resolves that
   * class's supertype names, each of which may itself appear in the body of an anonymous or local
   * class. Without memoization, the same supertype name would be resolved once for every path
   * through the enclosing classes, which is exponential in the nesting depth.
   *
   * @param elements used for looking up names
   * @param type a JavaParser class or interface type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it
   * @param nodeCache maps an AST node to the type it names, or to null if that cannot be
   *     determined; this method both reads and writes it. Its keys are compared by reference
   *     equality, because JavaParser's {@code equals()} is structural but two occurrences of the
   *     same type name can name different types.
   * @return the element for {@code type}, or null if it cannot be determined
   */
  private static @Nullable TypeElement resolveTypeNameAsTypeElementMemoized(
      Elements elements,
      ClassOrInterfaceType type,
      Map<String, @Nullable TypeElement> cache,
      IdentityHashMap<ClassOrInterfaceType, @Nullable TypeElement> nodeCache) {
    if (nodeCache.containsKey(type)) {
      return nodeCache.get(type);
    }
    TypeElement result = resolveTypeName(elements, type, cache, nodeCache).typeElement();
    nodeCache.put(type, result);
    return result;
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
   * every enum. The result is not part of any AST, so {@link #resolveTypeNameAsTypeElement}
   * resolves its name as a fully-qualified name.
   *
   * @return a JavaParser type that names {@code java.lang.Enum}
   */
  @SuppressWarnings("nullness:argument") // JavaParser permits a null scope for an unqualified name
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
   * A type whose member types {@link #resolveMemberType} searches, together with which of its
   * member types are members at the place where the name is being resolved -- that is, of the type
   * in whose scope the name appears, or, for a name that an import declaration resolves, of the
   * imported type.
   *
   * <p>The search starts at that type itself only when the type can be looked up by name. The
   * search for a name that appears in an anonymous or local class starts at a supertype of that
   * class instead, and the search for a name that an import declaration resolves starts at the
   * imported type. In those cases a member type is a member at the place where the name is being
   * resolved only if it is inherited.
   *
   * <p>An import declaration imports only the member types that are accessible where it appears. A
   * package-private member type is accessible only in the package that declares it, and, outside
   * the package that declares it, a protected member type is accessible only within the body of a
   * subclass of the type that declares it -- which an import declaration is not within. For a name
   * that an import declaration resolves, {@code packagePrivateIsMember} and {@code
   * protectedIsMember} are therefore true only if {@code typeElement} is in the package that
   * contains the import declaration.
   *
   * @param typeElement the type whose member types to search
   * @param privateIsMember true if a private member type of {@code typeElement} is a member at the
   *     place where the name is being resolved; true only if the search starts at {@code
   *     typeElement} itself, because a private member type is inherited by no type
   * @param packagePrivateIsMember true if a package-private member type of {@code typeElement} is a
   *     member at the place where the name is being resolved
   * @param protectedIsMember true if a protected member type of {@code typeElement} is a member at
   *     the place where the name is being resolved
   */
  private record SearchedType(
      TypeElement typeElement,
      boolean privateIsMember,
      boolean packagePrivateIsMember,
      boolean protectedIsMember) {}

  /**
   * Returns the element for the member type named {@code firstComponent + suffix} that {@code
   * start}'s type declares or inherits, or null if there is no such member type.
   *
   * <p>{@code start}'s type and its supertypes are searched in breadth-first order. A declaration
   * hides whatever its declaring type would otherwise inherit, even if the declaration is not
   * itself inherited: a private member type is inherited by no type, and a package-private member
   * type is inherited only within its own package.
   *
   * <p>If two supertypes declare different member types with this name, then neither hides the
   * other and the name is ambiguous. If the two supertypes are equally near, this method returns
   * null. Java considers the name ambiguous even if the two supertypes are at different distances,
   * because distance creates no hiding relationship; this method returns the nearer declaration,
   * which affects no valid program, because a use of an ambiguous name does not compile.
   *
   * @param elements used for looking up names
   * @param start the type whose member types to search, together with which of its member types are
   *     members at the place where the name is being resolved
   * @param firstComponent the simple name of a member type of {@code start}'s type
   * @param suffix the rest of the type name, which names a type nested within {@code
   *     firstComponent}; it is empty or starts with "."
   * @param usePackage the name of the package that contains the use of the type name, or "" for the
   *     unnamed package
   * @param inSubclassBody true if the use of the type name is within the body of a subclass of the
   *     types that are searched, as it is for a name in the scope of a class but not for a name
   *     that an import declaration resolves; a protected member type that a type in another package
   *     declares is accessible only within such a body
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it
   * @return the element for the member type, or null if there is none or it cannot be determined
   */
  private static @Nullable TypeElement resolveMemberType(
      Elements elements,
      SearchedType start,
      String firstComponent,
      String suffix,
      String usePackage,
      boolean inSubclassBody,
      Map<String, @Nullable TypeElement> cache) {
    Set<TypeElement> visited = new HashSet<>();
    visited.add(start.typeElement());
    // The types that are the same distance from `start`'s type:  first that type itself, then its
    // direct supertypes, and so forth.
    Collection<SearchedType> currentTypes = Collections.singletonList(start);
    while (!currentTypes.isEmpty()) {
      // Every type at the current distance is searched, rather than returning the first member
      // type that is found, because a member type that is declared in one of them does not hide
      // one that is declared in another.
      TypeElement found = null;
      // The types at the next distance, keyed by type element so that a supertype that more than
      // one path reaches at that distance is searched just once, for the union of what those paths
      // make a member.
      Map<TypeElement, SearchedType> nextTypes = new LinkedHashMap<>();
      for (SearchedType current : currentTypes) {
        TypeElement currentElement = current.typeElement();
        TypeElement declared = declaredMemberType(currentElement, firstComponent);
        if (declared != null) {
          if (isMember(declared, current)) {
            if (found == null) {
              found = declared;
            } else if (!found.equals(declared)) {
              // Two equally near supertypes declare different member types with this name, so the
              // name is ambiguous.
              return null;
            }
          }
          // `declared` hides every member type of the same name that `currentElement` would
          // otherwise inherit -- even if `declared` is not a member of the type at which the
          // search started -- so do not search the supertypes of `currentElement`.
          continue;
        }
        for (TypeElement supertype :
            ElementUtils.getDirectSuperTypeElements(currentElement, elements)) {
          if (!visited.contains(supertype)) {
            SearchedType next =
                new SearchedType(
                    supertype,
                    // A private member type is inherited by no type.
                    false,
                    // `currentElement` inherits a package-private member type of `supertype` only
                    // if the two types are in the same package.
                    current.packagePrivateIsMember()
                        && inSamePackage(elements, supertype, currentElement),
                    // A protected member type of `supertype` is inherited even by a subclass in
                    // another package, but outside the package that declares it, it is accessible
                    // only within the body of such a subclass.
                    inSubclassBody || inPackage(elements, supertype, usePackage));
            // A package-private member type of `supertype` is a member at the place where the
            // name is being resolved if any of the paths that reach `supertype` inherits it.
            nextTypes.merge(
                supertype, next, (st1, st2) -> st1.packagePrivateIsMember() ? st1 : st2);
          }
        }
      }
      if (found != null) {
        if (suffix.isEmpty()) {
          return found;
        }
        TypeElement nested = getTypeElement(elements, found.getQualifiedName() + suffix, cache);
        if (nested != null) {
          return nested;
        }
        // `found.getQualifiedName() + suffix` is not a canonical name if any component of `suffix`
        // names an inherited member type, and `getTypeElement` finds a type only by its canonical
        // name.  Resolve the components of `suffix` one at a time instead, so that each one is
        // searched for in the supertypes of the type that contains it.
        int dot = suffix.indexOf('.', 1);
        // Every member type that `found` declares is a member of `found`, whatever its access
        // modifier is.
        SearchedType nestedStart = new SearchedType(found, true, true, true);
        if (dot == -1) {
          return resolveMemberType(
              elements, nestedStart, suffix.substring(1), "", usePackage, inSubclassBody, cache);
        }
        return resolveMemberType(
            elements,
            nestedStart,
            suffix.substring(1, dot),
            suffix.substring(dot),
            usePackage,
            inSubclassBody,
            cache);
      }
      visited.addAll(nextTypes.keySet());
      currentTypes = nextTypes.values();
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
   * Returns true if {@code member} is a member at the place where {@link #resolveMemberType} is
   * resolving a name -- that is, if every type between {@code searchedType}'s type and the type
   * that declares {@code member} inherits it, {@code searchedType}'s type either declares {@code
   * member} or inherits it, and {@code member} is accessible there.
   *
   * @param member a member type that {@code searchedType}'s type declares
   * @param searchedType the type that is currently being searched
   * @return true if {@code member} is a member at the place where the name is being resolved
   */
  private static boolean isMember(TypeElement member, SearchedType searchedType) {
    Set<Modifier> modifiers = member.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE)) {
      return searchedType.privateIsMember();
    }
    if (modifiers.contains(Modifier.PUBLIC)) {
      return true;
    }
    if (modifiers.contains(Modifier.PROTECTED)) {
      return searchedType.protectedIsMember();
    }
    return searchedType.packagePrivateIsMember();
  }

  /**
   * Returns true if a use in package {@code usePackage} can access {@code type}: no type from
   * {@code type} to the top-level type that contains it is private, and every one of them that is
   * package-private or protected is in package {@code usePackage}.
   *
   * <p>This method is for a type that an import on demand might import. An import declaration is
   * not within the body of a type, so a private member type is never accessible to one, and a
   * protected member type is accessible to one only within the package that declares it.
   *
   * @param elements used for looking up the package that contains a type
   * @param type a type
   * @param usePackage the fully-qualified name of the package that contains the use of the type's
   *     name; the empty string names the unnamed package
   * @return true if a use in package {@code usePackage} can access {@code type}
   */
  private static boolean isAccessible(Elements elements, TypeElement type, String usePackage) {
    for (Element element = type;
        element instanceof TypeElement;
        element = element.getEnclosingElement()) {
      Set<Modifier> modifiers = element.getModifiers();
      if (modifiers.contains(Modifier.PRIVATE)) {
        return false;
      }
      if (!modifiers.contains(Modifier.PUBLIC)
          && !inPackage(elements, (TypeElement) element, usePackage)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the given type is declared in the given package.
   *
   * @param elements used for looking up the package that contains a type
   * @param type a type
   * @param packageName the fully-qualified name of a package; the empty string names the unnamed
   *     package
   * @return true if {@code type} is declared in the package named {@code packageName}
   */
  private static boolean inPackage(Elements elements, TypeElement type, String packageName) {
    return elements.getPackageOf(type).getQualifiedName().contentEquals(packageName);
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

  /**
   * Returns the TypeMirror for the given JavaParser type, or null if it cannot be determined. It
   * cannot be determined for a use of a type variable, an intersection type, a union type, {@code
   * var}, a wildcard, a type parameter declaration, or a type that is not on the classpath.
   *
   * @param elements used for looking up names
   * @param types used for creating types
   * @param type a JavaParser type
   * @param cache maps a name to the type it names, or to null if it names no type; this method both
   *     reads and writes it. See {@link #resolveTypeNameAsTypeElement(Elements,
   *     ClassOrInterfaceType, Map)} for restrictions on it.
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
      TypeElement typeElt = resolveTypeNameAsTypeElement(elements, classType, cache);
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
