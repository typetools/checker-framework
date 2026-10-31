package org.checkerframework.checker.nullness;

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
import java.util.ArrayList;
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
import javax.lang.model.element.VariableElement;
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
  /*package-private*/ final TypeElement pathElement =
      ElementUtils.getTypeElement(processingEnv, Path.class);

  /** The {@code java.nio.file.Paths} class. */
  /*package-private*/ final TypeElement pathsElement =
      ElementUtils.getTypeElement(processingEnv, Paths.class);

  /** The {@code java.nio.file.Files} class. */
  /*package-private*/ final TypeElement filesElement =
      ElementUtils.getTypeElement(processingEnv, Files.class);

  /** The {@code java.io.File} class. */
  /*package-private*/ final TypeElement fileElement =
      ElementUtils.getTypeElement(processingEnv, File.class);

  /** The {@code Path.getParent()} method. */
  private final ExecutableElement pathGetParent =
      TreeUtils.getMethod(Path.class, "getParent", 0, processingEnv);

  /** The {@code Path.getFileName()} method. */
  private final ExecutableElement pathGetFileName =
      TreeUtils.getMethod(Path.class, "getFileName", 0, processingEnv);

  /** The {@code File.getParent()} method. */
  private final ExecutableElement fileGetParent =
      TreeUtils.getMethod(File.class, "getParent", 0, processingEnv);

  /** The {@code File.getParentFile()} method. */
  private final ExecutableElement fileGetParentFile =
      TreeUtils.getMethod(File.class, "getParentFile", 0, processingEnv);

  /** The {@code Files.isRegularFile(Path, LinkOption...)} method. */
  /*package-private*/ final ExecutableElement filesIsRegularFile =
      TreeUtils.getMethod(Files.class, "isRegularFile", 2, processingEnv);

  /** The {@code File.isFile()} method. */
  /*package-private*/ final ExecutableElement fileIsFile =
      TreeUtils.getMethod(File.class, "isFile", 0, processingEnv);

  /** The {@code Stream.toList()} method, or null if it does not exist (before Java 16). */
  private final @Nullable ExecutableElement streamToList =
      TreeUtils.getMethodOrNull("java.util.stream.Stream", "toList", 0, processingEnv);

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

  /** Returns false. Redundancy in the path shape hierarchy is not worth warning about. */
  @Override
  public boolean shouldWarnIfStubRedundantWithBytecode() {
    return false;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Determining whether a class uses paths
  //

  /** A cache for {@link #isPathOrFile}. */
  private final Map<TypeElement, Boolean> isPathOrFileCache = new HashMap<>();

  /**
   * Returns true if the given tree uses paths: some tree within it has a type that mentions a
   * {@code Path} or a {@code File}, or is a path shape qualifier.
   *
   * @param tree a tree
   * @return true if {@code tree} uses paths
   */
  /*package-private*/ boolean usesPaths(Tree tree) {
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
    AnnotationMirror existing = type.getPrimaryAnnotationInHierarchy(qualifier);
    if (existing == null || qualHierarchy.isSubtypeQualifiersOnly(qualifier, existing)) {
      type.replaceAnnotation(qualifier);
    }
  }

  /**
   * For each effectively final local variable whose initializer has been examined by {@link
   * #refineFromInitializer}, the path shapes of the type arguments of the initializer's type,
   * viewed as the variable's declared type. A path shape is {@code UNKNOWN} if the type argument is
   * not a declared type. The list is empty if the variable has no initializer, or if the
   * initializer's type is not a declared type or is raw.
   *
   * <p>{@link PathShapeTransfer} removes a variable's entry whenever dataflow analysis processes
   * the variable's declaration, because the type of the initializer can change as the analysis
   * proceeds.
   */
  private final Map<VariableElement, List<PathShape>> initializerShapes = new HashMap<>();

  /**
   * If {@code variable} is an effectively final local variable that has an initializer, refines the
   * covariant type arguments (see {@link org.checkerframework.framework.qual.Covariant}) of {@code
   * type} from the type of the initializer. This is sound, because it has the same effect on those
   * type arguments as declaring the variable with {@code var}. It refines only the type arguments
   * that are {@code @UnknownPathShape}, so it does not override a more specific qualifier that the
   * programmer wrote.
   *
   * <p>Because the refined type arguments are covariant, a use of the variable remains acceptable
   * wherever the declared type is. This method does not refine invariant type arguments, such as
   * that of {@code List}, nor array component types, because doing so would reject code such as
   * passing the variable to a method whose formal parameter has the declared type, or storing an
   * element whose path shape is unknown.
   *
   * @param variable a variable
   * @param type the type of a use of {@code variable}; is side-effected by this method
   */
  /*package-private*/ void refineFromInitializer(Element variable, AnnotatedTypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return;
    }
    AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
    List<AnnotatedTypeMirror> typeArgs = declaredType.getTypeArguments();
    List<Integer> refinable = new ArrayList<>();
    for (int i : getTypeHierarchy().getCovariantArgIndexes(declaredType)) {
      if (i < typeArgs.size() && isRefinable(typeArgs.get(i))) {
        refinable.add(i);
      }
    }
    if (refinable.isEmpty()) {
      return;
    }
    ElementKind kind = variable.getKind();
    if (!((kind == ElementKind.LOCAL_VARIABLE || kind == ElementKind.RESOURCE_VARIABLE)
        && ElementUtils.isEffectivelyFinal(variable))) {
      return;
    }
    VariableElement localVariable = (VariableElement) variable;
    List<PathShape> shapes = initializerShapes.get(localVariable);
    if (shapes == null) {
      shapes = computeInitializerShapes(localVariable, declaredType);
      if (shouldCache) {
        initializerShapes.put(localVariable, shapes);
      }
    }
    if (shapes.size() != typeArgs.size()) {
      return;
    }
    for (int i : refinable) {
      refine(typeArgs.get(i), shapes.get(i));
    }
  }

  /**
   * Returns true if the given type argument may be refined by {@link #refineFromInitializer}: it is
   * a declared type to which a path shape qualifier applies, and it is {@code @UnknownPathShape}.
   *
   * @param typeArg a type argument
   * @return true if {@code typeArg} may be refined
   */
  private boolean isRefinable(AnnotatedTypeMirror typeArg) {
    return typeArg.getKind() == TypeKind.DECLARED
        && isRelevant(typeArg)
        && typeArg.hasPrimaryAnnotation(UNKNOWN_PATH_SHAPE);
  }

  /**
   * Returns the path shapes of the type arguments of the type of the initializer of the given
   * variable, viewed as {@code declaredType}. See {@link #initializerShapes}.
   *
   * @param variable a local variable
   * @param declaredType the type of a use of {@code variable}
   * @return the path shapes of the type arguments of the initializer's type
   */
  private List<PathShape> computeInitializerShapes(
      VariableElement variable, AnnotatedDeclaredType declaredType) {
    Tree decl = declarationFromElement(variable);
    if (!(decl instanceof VariableTree varTree) || varTree.getInitializer() == null) {
      return Collections.emptyList();
    }
    AnnotatedTypeMirror initializerType = getAnnotatedType(varTree.getInitializer());
    if (initializerType.getKind() != TypeKind.DECLARED) {
      return Collections.emptyList();
    }
    AnnotatedDeclaredType initializerAsSuper =
        AnnotatedTypes.castedAsSuper(this, (AnnotatedDeclaredType) initializerType, declaredType);
    List<PathShape> result = new ArrayList<>();
    for (AnnotatedTypeMirror typeArg : initializerAsSuper.getTypeArguments()) {
      result.add(
          typeArg.getKind() == TypeKind.DECLARED ? getPathShape(typeArg) : PathShape.UNKNOWN);
    }
    return result;
  }

  /**
   * Discards the result of {@link #refineFromInitializer} for the given variable, because the type
   * of its initializer might have changed.
   *
   * @param variable a variable
   */
  /*package-private*/ void forgetInitializerShapes(VariableElement variable) {
    initializerShapes.remove(variable);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Method identification
  //

  /**
   * Returns true if the given method invocation is an invocation of {@code Stream.toList()}.
   *
   * @param tree a method invocation
   * @return true if {@code tree} is an invocation of {@code Stream.toList()}
   */
  /*package-private*/ boolean isStreamToList(MethodInvocationTree tree) {
    return TreeUtils.elementFromUse(tree).equals(streamToList);
  }

  /**
   * Returns true if the given method invocation returns a non-null value because of the path shape
   * of its receiver. That is, it is an invocation of {@code Path.getParent()}, {@code
   * File.getParent()}, or {@code File.getParentFile()} on a {@code @HasParent} receiver, or of
   * {@code Path.getFileName()} on a {@code @HasName} receiver.
   *
   * @param tree a method invocation
   * @param method the method that {@code tree} invokes
   * @return true if {@code tree} returns a non-null value because of the path shape of its receiver
   */
  public boolean returnsNonNullPathComponent(MethodInvocationTree tree, ExecutableElement method) {
    PathShape required;
    if (method.equals(pathGetParent)
        || method.equals(fileGetParent)
        || method.equals(fileGetParentFile)) {
      required = PathShape.HAS_PARENT;
    } else if (method.equals(pathGetFileName)) {
      required = PathShape.HAS_NAME;
    } else {
      return false;
    }
    AnnotatedTypeMirror receiverType = getReceiverType(tree);
    return receiverType != null && getPathShape(receiverType).compareTo(required) >= 0;
  }
}
