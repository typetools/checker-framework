package org.checkerframework.checker.nullness;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import org.checkerframework.checker.nullness.PathShapeClassifier.PathShape;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PathShapeBottom;
import org.checkerframework.checker.nullness.qual.PolyPathShape;
import org.checkerframework.checker.nullness.qual.UnknownPathShape;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** The type factory for the Path Shape Checker. */
public class PathShapeAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The @{@link UnknownPathShape} annotation. */
  /*package-private*/ final AnnotationMirror UNKNOWN_PATH_SHAPE =
      AnnotationBuilder.fromClass(elements, UnknownPathShape.class);

  /** The @{@link HasName} annotation. */
  /*package-private*/ final AnnotationMirror HAS_NAME =
      AnnotationBuilder.fromClass(elements, HasName.class);

  /** The @{@link HasParent} annotation. */
  /*package-private*/ final AnnotationMirror HAS_PARENT =
      AnnotationBuilder.fromClass(elements, HasParent.class);

  /** The {@code java.nio.file.Path} interface. */
  /*package-private*/ final TypeElement pathElement = getTypeElement(Path.class);

  /** The {@code java.nio.file.Paths} class. */
  /*package-private*/ final TypeElement pathsElement = getTypeElement(Paths.class);

  /** The {@code java.nio.file.Files} class. */
  /*package-private*/ final TypeElement filesElement = getTypeElement(Files.class);

  /** The {@code java.io.File} class. */
  /*package-private*/ final TypeElement fileElement = getTypeElement(File.class);

  /**
   * Creates a new PathShapeAnnotatedTypeFactory.
   *
   * @param checker the associated checker
   */
  @SuppressWarnings("this-escape")
  public PathShapeAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  /**
   * Returns the type element for the given class.
   *
   * @param clazz a class
   * @return the type element for {@code clazz}
   */
  private TypeElement getTypeElement(Class<?> clazz) {
    return ElementUtils.getTypeElement(processingEnv, clazz);
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            UnknownPathShape.class,
            HasName.class,
            HasParent.class,
            PathShapeBottom.class,
            PolyPathShape.class));
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Skipping classes that do not use paths
  //

  /** The top-level class that was most recently passed to {@link #shouldCheck}. */
  private @Nullable ClassTree currentTopLevelClass = null;

  /** True if {@link #currentTopLevelClass} uses paths, as determined by {@link #usesPaths}. */
  private boolean currentTopLevelClassUsesPaths = false;

  /** A cache for {@link #isPathOrFile}. */
  private final Map<TypeElement, Boolean> isPathOrFileCache = new HashMap<>();

  /**
   * Returns true if the Path Shape Checker should check the given class. It checks a class only if
   * the class, or the top-level class that contains it, uses paths. In a class that does not use
   * paths, the Path Shape Checker never issues a warning and never refines a type, so checking the
   * class would only waste time.
   *
   * @param tree a class
   * @return true if the Path Shape Checker should check {@code tree}
   */
  /*package-private*/ boolean shouldCheck(ClassTree tree) {
    if (tree != currentTopLevelClass && root != null && root.getTypeDecls().contains(tree)) {
      currentTopLevelClass = tree;
      currentTopLevelClassUsesPaths = usesPaths(tree);
    }
    return currentTopLevelClassUsesPaths;
  }

  @Override
  public void preProcessClassTree(ClassTree classTree) {
    // Skip dataflow analysis, which is the most expensive part of checking a class.
    if (shouldCheck(classTree)) {
      super.preProcessClassTree(classTree);
    }
  }

  /**
   * Returns true if the given tree uses paths: some tree within it has a type that mentions a
   * {@code Path} or a {@code File}, or is a path shape qualifier.
   *
   * @param tree a tree
   * @return true if {@code tree} uses paths
   */
  private boolean usesPaths(Tree tree) {
    // Types that have been examined, to avoid infinite recursion through type variable bounds such
    // as `T extends Comparable<T>`.
    Set<TypeMirror> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    class UsesPathsScanner extends TreeScanner<Void, Void> {
      // True if a tree whose type mentions a path has been found.
      boolean found = false;

      @Override
      public Void scan(Tree t, Void p) {
        // Once a use of a path has been found, return immediately without descending.
        if (found || t == null) {
          return null;
        }
        TypeMirror type = TreeUtils.typeOf(t);
        if (type != null && mentionsPathOrFile(type, visited)) {
          found = true;
          return null;
        }
        return super.scan(t, p);
      }
    }
    UsesPathsScanner scanner = new UsesPathsScanner();
    scanner.scan(tree, null);
    return scanner.found;
  }

  /**
   * Returns true if the given type mentions a {@code Path} or a {@code File}: it is a subtype of
   * one of them, or a part of it (such as a type argument, an array component type, a bound, or a
   * parameter or return type) mentions one of them. Also returns true for a path shape qualifier,
   * so that a class that writes one is checked.
   *
   * @param type a type
   * @param visited types that have already been examined; is side-effected by this method
   * @return true if {@code type} mentions a {@code Path} or a {@code File}
   */
  private boolean mentionsPathOrFile(TypeMirror type, Set<TypeMirror> visited) {
    if (!visited.add(type)) {
      return false;
    }
    switch (type.getKind()) {
      case DECLARED -> {
        DeclaredType declaredType = (DeclaredType) type;
        return isPathOrFile((TypeElement) declaredType.asElement())
            || mentionsAny(declaredType.getTypeArguments(), visited);
      }
      case ARRAY -> {
        return mentionsPathOrFile(((ArrayType) type).getComponentType(), visited);
      }
      case WILDCARD -> {
        WildcardType wildcard = (WildcardType) type;
        return (wildcard.getExtendsBound() != null
                && mentionsPathOrFile(wildcard.getExtendsBound(), visited))
            || (wildcard.getSuperBound() != null
                && mentionsPathOrFile(wildcard.getSuperBound(), visited));
      }
      case TYPEVAR -> {
        TypeVariable typeVar = (TypeVariable) type;
        return mentionsPathOrFile(typeVar.getUpperBound(), visited)
            || mentionsPathOrFile(typeVar.getLowerBound(), visited);
      }
      case INTERSECTION -> {
        return mentionsAny(((IntersectionType) type).getBounds(), visited);
      }
      case UNION -> {
        return mentionsAny(((UnionType) type).getAlternatives(), visited);
      }
      case EXECUTABLE -> {
        ExecutableType methodType = (ExecutableType) type;
        return mentionsPathOrFile(methodType.getReturnType(), visited)
            || mentionsAny(methodType.getParameterTypes(), visited);
      }
      default -> {
        return false;
      }
    }
  }

  /**
   * Returns true if any of the given types mentions a {@code Path} or a {@code File}.
   *
   * @param types types
   * @param visited types that have already been examined; is side-effected by this method
   * @return true if any of {@code types} mentions a {@code Path} or a {@code File}
   */
  private boolean mentionsAny(List<? extends TypeMirror> types, Set<TypeMirror> visited) {
    for (TypeMirror type : types) {
      if (mentionsPathOrFile(type, visited)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the given class is a subtype of {@code Path} or {@code File}, or is a path
   * shape qualifier.
   *
   * @param clazz a class
   * @return true if {@code clazz} is a subtype of {@code Path} or {@code File}, or is a path shape
   *     qualifier
   */
  private boolean isPathOrFile(TypeElement clazz) {
    Boolean result = isPathOrFileCache.get(clazz);
    if (result == null) {
      TypeMirror erased = types.erasure(clazz.asType());
      result =
          types.isSubtype(erased, types.erasure(pathElement.asType()))
              || types.isSubtype(erased, fileElement.asType())
              || isSupportedQualifier(clazz.getQualifiedName().toString());
      isPathOrFileCache.put(clazz, result);
    }
    return result;
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(super.createTreeAnnotator(), new PathShapeTreeAnnotator(this));
  }

  @Override
  public CFTransfer createFlowTransferFunction(
      CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    return new PathShapeTransfer(analysis);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Conversions between qualifiers and path shapes
  //

  /**
   * Returns the qualifier that corresponds to the given path shape.
   *
   * @param shape a path shape
   * @return the qualifier that corresponds to {@code shape}
   */
  /*package-private*/ AnnotationMirror toQualifier(PathShape shape) {
    return switch (shape) {
      case UNKNOWN -> UNKNOWN_PATH_SHAPE;
      case HAS_NAME -> HAS_NAME;
      case HAS_PARENT -> HAS_PARENT;
    };
  }

  /**
   * Returns the path shape that the given qualifier guarantees.
   *
   * @param qualifier a qualifier in the Path Shape hierarchy, or null
   * @return the path shape that {@code qualifier} guarantees
   */
  private PathShape toPathShape(@Nullable AnnotationMirror qualifier) {
    if (qualifier == null) {
      return PathShape.UNKNOWN;
    } else if (qualHierarchy.isSubtypeQualifiersOnly(qualifier, HAS_PARENT)) {
      return PathShape.HAS_PARENT;
    } else if (qualHierarchy.isSubtypeQualifiersOnly(qualifier, HAS_NAME)) {
      return PathShape.HAS_NAME;
    } else {
      return PathShape.UNKNOWN;
    }
  }

  /**
   * Returns the path shape of the given type.
   *
   * @param type the type of a {@code Path} or {@code File} expression
   * @return the path shape of {@code type}
   */
  /*package-private*/ PathShape getPathShape(AnnotatedTypeMirror type) {
    return toPathShape(type.getAnnotationInHierarchy(UNKNOWN_PATH_SHAPE));
  }

  /**
   * Returns the path shape of the given expression.
   *
   * @param tree a {@code Path} or {@code File} expression
   * @return the path shape of {@code tree}
   */
  /*package-private*/ PathShape getPathShape(ExpressionTree tree) {
    return getPathShape(getAnnotatedType(tree));
  }

  /**
   * Returns the path shape of the given expression, which is a {@code Path} or a {@code String}
   * that is converted to a {@code Path}. A {@code String} has a known path shape only if it is a
   * compile-time constant.
   *
   * @param tree a {@code Path} or {@code String} expression
   * @return the path shape of {@code tree}, or of the {@code Path} that is made from it
   */
  /*package-private*/ PathShape getPathShapeOfPathOrString(ExpressionTree tree) {
    if (TypesUtils.isString(TreeUtils.typeOf(tree))) {
      String constant = TreeUtils.stringConstantValue(tree);
      return constant == null ? PathShape.UNKNOWN : PathShapeClassifier.classifyPath(constant);
    }
    return getPathShape(tree);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Refinement
  //

  /**
   * Refines the given type to the given path shape, if that is more specific than its current
   * qualifier.
   *
   * @param type a type; is side-effected by this method
   * @param shape a path shape
   */
  /*package-private*/ void refine(AnnotatedTypeMirror type, PathShape shape) {
    if (shape == PathShape.UNKNOWN) {
      return;
    }
    AnnotationMirror qualifier = toQualifier(shape);
    @Nullable AnnotationMirror existing = type.getPrimaryAnnotationInHierarchy(qualifier);
    if (existing == null || qualHierarchy.isSubtypeQualifiersOnly(qualifier, existing)) {
      type.replaceAnnotation(qualifier);
    }
  }

  /**
   * If {@code variable} is an effectively final local variable that has an initializer, refines the
   * type arguments or array component type of {@code type} from the type of the initializer. This
   * has the same effect as declaring the variable with {@code var}, so it is sound. It refines only
   * the type arguments and array component type that are {@code @UnknownPathShape}, so it does not
   * override a more specific qualifier that the programmer wrote.
   *
   * @param variable a variable
   * @param type the type of a declaration or use of {@code variable}; is side-effected by this
   *     method
   */
  /*package-private*/ void refineFromInitializer(Element variable, AnnotatedTypeMirror type) {
    TypeKind typeKind = type.getKind();
    List<AnnotatedTypeMirror> parts;
    if (typeKind == TypeKind.ARRAY) {
      parts = Collections.singletonList(((AnnotatedArrayType) type).getComponentType());
    } else if (typeKind == TypeKind.DECLARED) {
      parts = ((AnnotatedDeclaredType) type).getTypeArguments();
    } else {
      return;
    }
    if (!hasRelevantDeclaredType(parts)) {
      return;
    }
    ElementKind kind = variable.getKind();
    if (!((kind == ElementKind.LOCAL_VARIABLE || kind == ElementKind.RESOURCE_VARIABLE)
        && ElementUtils.isEffectivelyFinal(variable))) {
      return;
    }
    Tree decl = declarationFromElement(variable);
    if (!(decl instanceof VariableTree varTree) || varTree.getInitializer() == null) {
      return;
    }
    AnnotatedTypeMirror initializerType = getAnnotatedType(varTree.getInitializer());
    if (initializerType.getKind() != typeKind) {
      return;
    }
    if (typeKind == TypeKind.ARRAY) {
      refineFrom(
          ((AnnotatedArrayType) type).getComponentType(),
          ((AnnotatedArrayType) initializerType).getComponentType());
      return;
    }
    AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
    AnnotatedDeclaredType initializerAsSuper =
        AnnotatedTypes.castedAsSuper(this, (AnnotatedDeclaredType) initializerType, declaredType);
    List<AnnotatedTypeMirror> typeArgs = declaredType.getTypeArguments();
    List<AnnotatedTypeMirror> initializerTypeArgs = initializerAsSuper.getTypeArguments();
    if (typeArgs.size() != initializerTypeArgs.size()) {
      // One of the types is raw.
      return;
    }
    for (int i = 0; i < typeArgs.size(); i++) {
      refineFrom(typeArgs.get(i), initializerTypeArgs.get(i));
    }
  }

  /**
   * Returns true if one of the given types is a declared type to which a path shape qualifier
   * applies.
   *
   * @param types type arguments or an array component type
   * @return true if one of {@code types} is a declared type to which a path shape qualifier applies
   */
  private boolean hasRelevantDeclaredType(List<AnnotatedTypeMirror> types) {
    for (AnnotatedTypeMirror type : types) {
      if (type.getKind() == TypeKind.DECLARED && isRelevant(type)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Refines {@code type} to the path shape of {@code from}, if both are declared types and {@code
   * type} is {@code @UnknownPathShape}.
   *
   * @param type a type argument or array component type; is side-effected by this method
   * @param from the corresponding part of the initializer's type
   */
  private void refineFrom(AnnotatedTypeMirror type, AnnotatedTypeMirror from) {
    if (type.getKind() == TypeKind.DECLARED
        && from.getKind() == TypeKind.DECLARED
        && type.hasPrimaryAnnotation(UNKNOWN_PATH_SHAPE)) {
      refine(type, getPathShape(from));
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Method identification
  //

  /**
   * Returns true if the given method is declared in the given class and has the given name.
   *
   * @param method a method
   * @param clazz a class
   * @param name a method name
   * @return true if {@code method} is {@code clazz.name}
   */
  /*package-private*/ static boolean isMethod(
      ExecutableElement method, TypeElement clazz, String name) {
    return method.getEnclosingElement().equals(clazz) && method.getSimpleName().contentEquals(name);
  }

  /**
   * Returns true if the given method invocation returns a non-null value because of the path shape
   * of its receiver. That is, it is an invocation of {@code Path.getParent()}, {@code
   * File.getParent()}, or {@code File.getParentFile()} on a {@code @HasParent} receiver, or of
   * {@code Path.getFileName()} on a {@code @HasName} receiver.
   *
   * @param tree a method invocation
   * @return true if {@code tree} returns a non-null value because of the path shape of its receiver
   */
  public boolean returnsNonNullPathComponent(MethodInvocationTree tree) {
    ExecutableElement method = TreeUtils.elementFromUse(tree);
    PathShape required;
    if (isMethod(method, pathElement, "getParent")
        || isMethod(method, fileElement, "getParent")
        || isMethod(method, fileElement, "getParentFile")) {
      required = PathShape.HAS_PARENT;
    } else if (isMethod(method, pathElement, "getFileName")) {
      required = PathShape.HAS_NAME;
    } else {
      return false;
    }
    AnnotatedTypeMirror receiverType = getReceiverType(tree);
    return receiverType != null && getPathShape(receiverType).compareTo(required) >= 0;
  }
}
