package org.checkerframework.common.basetype;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;

/**
 * A visitor to validate the types in a tree.
 *
 * <p>Note: A TypeValidator (this class and its subclasses) cannot tell whether an annotation was
 * written by a programmer or defaulted/inferred/computed by the Checker Framework, because the
 * AnnotatedTypeMirror does not make distinctions about which annotations in an AnnotatedTypeMirror
 * were explicitly written and which were added by a checker. To issue a warning/error only when a
 * programmer writes an annotation, override {@link BaseTypeVisitor#visitAnnotatedType} and {@link
 * BaseTypeVisitor#visitVariable}.
 */
public class BaseTypeValidator extends AnnotatedTypeScanner<Void, Tree> implements TypeValidator {
  /** Is the type valid? This is side-effected by the visitor, and read at the end of visiting. */
  protected boolean isValid = true;

  /** Should the primary annotation on the top level type be checked? */
  protected boolean checkTopLevelDeclaredOrPrimitiveType = true;

  /** BaseTypeChecker. */
  protected final BaseTypeChecker checker;
  /** BaseTypeVisitor. */
  protected final BaseTypeVisitor<?> visitor;
  /** AnnotatedTypeFactory. */
  protected final AnnotatedTypeFactory atypeFactory;

  // TODO: clean up coupling between components
  public BaseTypeValidator(
      BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
    this.checker = checker;
    this.visitor = visitor;
    this.atypeFactory = atypeFactory;
  }

  /**
   * Validate the type against the given tree. This method both issues error messages and also
   * returns a boolean value.
   *
   * <p>This is the entry point to the type validator. Neither this method nor visit should be
   * called directly by a visitor, only use {@link BaseTypeVisitor#validateTypeOf(Tree)}.
   *
   * <p>This method is only called on top-level types, but it validates the entire type including
   * components of a compound type. Subclasses should override this only if there is special-case
   * behavior that should be performed only on top-level types.
   *
   * @param type the type to validate
   * @param tree the tree from which the type originated. If the tree is a method tree, {@code type}
   *     is its return type. If the tree is a variable tree, {@code type} is the variable's type.
   * @return true if the type is valid
   */
  @Override
  public boolean isValid(AnnotatedTypeMirror type, Tree tree) {
    List<DiagMessage> diagMessages =
        isValidStructurally(atypeFactory.getQualifierHierarchy(), type);
    if (!diagMessages.isEmpty()) {
      for (DiagMessage d : diagMessages) {
        checker.report(tree, d);
      }
      return false;
    }
    this.isValid = true;
    this.checkTopLevelDeclaredOrPrimitiveType =
        shouldCheckTopLevelDeclaredOrPrimitiveType(type, tree);
    visit(type, tree);
    return this.isValid;
  }

  /**
   * Should the top-level declared or primitive type be checked?
   *
   * <p>If {@code type} is not a declared or primitive type, then this method returns true.
   *
   * <p>Top-level type is not checked if tree is a local variable or an expression tree.
   *
   * @param type AnnotatedTypeMirror being validated
   * @param tree a Tree whose type is {@code type}
   * @return whether or not the top-level type should be checked, if {@code type} is a declared or
   *     primitive type.
   */
  protected boolean shouldCheckTopLevelDeclaredOrPrimitiveType(
      AnnotatedTypeMirror type, Tree tree) {
    if (type.getKind() != TypeKind.DECLARED && !type.getKind().isPrimitive()) {
      return true;
    }
    return !TreeUtils.isLocalVariable(tree)
        && (!TreeUtils.isExpressionTree(tree) || TreeUtils.isTypeTree(tree));
  }

  /**
   * Performs some well-formedness checks on the given {@link AnnotatedTypeMirror}. Returns a list
   * of failures. If successful, returns an empty list. The method will never return failures for a
   * valid type, but might not catch all invalid types.
   *
   * <p>This method ensures that the type is structurally or lexically well-formed, but it does not
   * check whether the annotations are semantically sensible. Subclasses should generally override
   * visit methods such as {@link #visitDeclared} rather than this method.
   *
   * <p>Currently, this implementation checks the following (subclasses can extend this behavior):
   *
   * <ol>
   *   <li>There should not be multiple annotations from the same qualifier hierarchy.
   *   <li>There should not be more annotations than the width of the QualifierHierarchy.
   *   <li>If the type is not a type variable, then the number of annotations should be the same as
   *       the width of the QualifierHierarchy.
   *   <li>These properties should also hold recursively for component types of arrays and for
   *       bounds of type variables and wildcards.
   * </ol>
   *
   * @param qualifierHierarchy the QualifierHierarchy
   * @param type the type to test
   * @return list of reasons the type is invalid, or empty list if the type is valid
   */
  protected List<DiagMessage> isValidStructurally(
      QualifierHierarchy qualifierHierarchy, AnnotatedTypeMirror type) {
    SimpleAnnotatedTypeScanner<List<DiagMessage>, QualifierHierarchy> scanner =
        new SimpleAnnotatedTypeScanner<>(
            (atm, q) -> isTopLevelValidType(q, atm),
            DiagMessage::mergeLists,
            Collections.emptyList());
    return scanner.visit(type, qualifierHierarchy);
  }

  /**
   * Checks every property listed in {@link #isValidStructurally}, but only for the top level type.
   * If successful, returns an empty list. If not successful, returns diagnostics.
   *
   * @param qualifierHierarchy the QualifierHierarchy
   * @param type the type to be checked
   * @return the diagnostics indicating failure, or an empty list if successful
   */
  // This method returns a singleton or empyty list.  Its return type is List rather than
  // DiagMessage (with null indicting success) because its caller, isValidStructurally(), expects
  // a list.
  protected List<DiagMessage> isTopLevelValidType(
      QualifierHierarchy qualifierHierarchy, AnnotatedTypeMirror type) {
    // multiple annotations from the same hierarchy
    Set<AnnotationMirror> annotations = type.getAnnotations();
    Set<AnnotationMirror> seenTops = AnnotationUtils.createAnnotationSet();
    for (AnnotationMirror anno : annotations) {
      AnnotationMirror top = qualifierHierarchy.getTopAnnotation(anno);
      if (AnnotationUtils.containsSame(seenTops, top)) {
        return Collections.singletonList(
            new DiagMessage(Kind.ERROR, "conflicting.annos", annotations, type));
      }
      seenTops.add(top);
    }

    boolean canHaveEmptyAnnotationSet = QualifierHierarchy.canHaveEmptyAnnotationSet(type);

    // wrong number of annotations
    if (!canHaveEmptyAnnotationSet && seenTops.size() < qualifierHierarchy.getWidth()) {
      return Collections.singletonList(
          new DiagMessage(Kind.ERROR, "too.few.annotations", annotations, type));
    }

    // success
    return Collections.emptyList();
  }

  protected void reportValidityResult(
      final @CompilerMessageKey String errorType, final AnnotatedTypeMirror type, final Tree p) {
    checker.reportError(p, errorType, type.getAnnotations(), type.toString());
    isValid = false;
  }

  /**
   * Like {@link #reportValidityResult}, but the type is printed in the error message without
   * annotations. This method would print "annotation @NonNull is not permitted on type int",
   * whereas {@link #reportValidityResult} would print "annotation @NonNull is not permitted on
   * type @NonNull int". In addition, when the underlying type is a compound type such as
   * {@code @Bad List<String>}, the erased type will be used, i.e., "{@code List}" will print
   * instead of "{@code @Bad List<String>}".
   */
  protected void reportValidityResultOnUnannotatedType(
      final @CompilerMessageKey String errorType, final AnnotatedTypeMirror type, final Tree p) {
    TypeMirror underlying =
        TypeAnnotationUtils.unannotatedType(type.getErased().getUnderlyingType());
    checker.reportError(p, errorType, type.getAnnotations(), underlying.toString());
    isValid = false;
  }

  /**
   * Most errors reported by this class are of the form type.invalid. This method reports when the
   * bounds of a wildcard or type variable don't make sense. Bounds make sense when the effective
   * annotations on the upper bound are supertypes of those on the lower bounds for all hierarchies.
   * To ensure that this subtlety is not lost on users, we report "bound" and print the bounds along
   * with the invalid type rather than a "type.invalid".
   *
   * @param type the type with invalid bounds
   * @param tree where to report the error
   */
  protected void reportInvalidBounds(final AnnotatedTypeMirror type, final Tree tree) {
    final String label;
    final AnnotatedTypeMirror upperBound;
    final AnnotatedTypeMirror lowerBound;

    switch (type.getKind()) {
      case TYPEVAR:
        label = "type parameter";
        upperBound = ((AnnotatedTypeVariable) type).getUpperBound();
        lowerBound = ((AnnotatedTypeVariable) type).getLowerBound();
        break;

      case WILDCARD:
        label = "wildcard";
        upperBound = ((AnnotatedWildcardType) type).getExtendsBound();
        lowerBound = ((AnnotatedWildcardType) type).getSuperBound();
        break;

      default:
        throw new BugInCF("Type is not bounded.%ntype=%s%ntree=%s", type, tree);
    }

    checker.reportError(
        tree,
        "bound",
        label,
        type.toString(),
        upperBound.toString(true),
        lowerBound.toString(true));
    isValid = false;
  }

  protected void reportInvalidType(final AnnotatedTypeMirror type, final Tree p) {
    reportValidityResult("type.invalid", type, p);
  }

  /**
   * Report an "annotations.on.use" error for the given type and tree.
   *
   * @param type the type with invalid annotations
   * @param p the tree where to report the error
   */
  protected void reportInvalidAnnotationsOnUse(final AnnotatedTypeMirror type, final Tree p) {
    reportValidityResultOnUnannotatedType("annotations.on.use", type, p);
  }

  @Override
  public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
    if (visitedNodes.containsKey(type)) {
      return visitedNodes.get(type);
    }

    final boolean skipChecks = checker.shouldSkipUses(type.getUnderlyingType().asElement());

    if (checkTopLevelDeclaredOrPrimitiveType && !skipChecks) {
      // Ensure that type use is a subtype of the element type
      // isValidUse determines the erasure of the types.

      Set<AnnotationMirror> bounds =
          atypeFactory.getTypeDeclarationBounds(type.getUnderlyingType());

      AnnotatedDeclaredType elemType = type.deepCopy();
      elemType.clearAnnotations();
      elemType.addAnnotations(bounds);

      if (!visitor.isValidUse(elemType, type, tree)) {
        reportInvalidAnnotationsOnUse(type, tree);
      }
    }
    // Set checkTopLevelDeclaredType to true, because the next time visitDeclared is called,
    // the type isn't the top level, so always do the check.
    checkTopLevelDeclaredOrPrimitiveType = true;

    /*
     * Try to reconstruct the ParameterizedTypeTree from the given tree.
     * TODO: there has to be a nicer way to do this...
     */
    Pair<ParameterizedTypeTree, AnnotatedDeclaredType> p = extractParameterizedTypeTree(tree, type);
    ParameterizedTypeTree typeArgTree = p.first;
    type = p.second;

    if (typeArgTree == null) {
      return super.visitDeclared(type, tree);
    } // else

    // We put this here because we don't want to put it in visitedNodes before calling
    // super (in the else branch) because that would cause the super implementation
    // to detect that we've already visited type and to immediately return.
    visitedNodes.put(type, null);

    // We have a ParameterizedTypeTree -> visit it.

    visitParameterizedType(type, typeArgTree);

    /*
     * Instead of calling super with the unchanged "tree", adapt the
     * second argument to be the corresponding type argument tree. This
     * ensures that the first and second parameter to this method always
     * correspond. visitDeclared is the only method that had this
     * problem.
     */
    List<? extends AnnotatedTypeMirror> tatypes = type.getTypeArguments();

    if (tatypes == null) {
      return null;
    }

    // May be zero for a "diamond" (inferred type args in constructor invocation).
    int numTypeArgs = typeArgTree.getTypeArguments().size();
    if (numTypeArgs != 0) {
      // TODO: this should be an equality, but in
      // http://buffalo.cs.washington.edu:8080/job/jdk6-daikon-typecheck/2061/console
      // it failed with:
      // daikon/Debug.java; message: size mismatch for type arguments:
      // @NonNull Object and Class<?>
      // but I didn't manage to reduce it to a test case.
      assert tatypes.size() <= numTypeArgs || skipChecks
          : "size mismatch for type arguments: " + type + " and " + typeArgTree;

      for (int i = 0; i < tatypes.size(); ++i) {
        scan(tatypes.get(i), typeArgTree.getTypeArguments().get(i));
      }
    }

    // Don't call the super version, because it creates a mismatch
    // between the first and second parameters.
    // return super.visitDeclared(type, tree);

    return null;
  }

  private Pair<ParameterizedTypeTree, AnnotatedDeclaredType> extractParameterizedTypeTree(
      Tree tree, AnnotatedDeclaredType type) {
    ParameterizedTypeTree typeargtree = null;

    switch (tree.getKind()) {
      case VARIABLE:
        Tree lt = ((VariableTree) tree).getType();
        if (lt instanceof ParameterizedTypeTree) {
          typeargtree = (ParameterizedTypeTree) lt;
        } else {
          // System.out.println("Found a: " + lt);
        }
        break;
      case PARAMETERIZED_TYPE:
        typeargtree = (ParameterizedTypeTree) tree;
        break;
      case NEW_CLASS:
        NewClassTree nct = (NewClassTree) tree;
        ExpressionTree nctid = nct.getIdentifier();
        if (nctid.getKind() == Tree.Kind.PARAMETERIZED_TYPE) {
          typeargtree = (ParameterizedTypeTree) nctid;
          /*
           * This is quite tricky... for anonymous class instantiations,
           * the type at this point has no type arguments. By doing the
           * following, we get the type arguments again.
           */
          type = (AnnotatedDeclaredType) atypeFactory.getAnnotatedType(typeargtree);
        }
        break;
      case ANNOTATED_TYPE:
        AnnotatedTypeTree tr = (AnnotatedTypeTree) tree;
        ExpressionTree undtr = tr.getUnderlyingType();
        if (undtr instanceof ParameterizedTypeTree) {
          typeargtree = (ParameterizedTypeTree) undtr;
        } else if (undtr instanceof IdentifierTree) {
          // @Something D -> Nothing to do
        } else {
          // TODO: add more test cases to ensure that nested types are
          // handled correctly,
          // e.g. @Nullable() List<@Nullable Object>[][]
          Pair<ParameterizedTypeTree, AnnotatedDeclaredType> p =
              extractParameterizedTypeTree(undtr, type);
          typeargtree = p.first;
          type = p.second;
        }
        break;
      case IDENTIFIER:
      case ARRAY_TYPE:
      case NEW_ARRAY:
      case MEMBER_SELECT:
      case UNBOUNDED_WILDCARD:
      case EXTENDS_WILDCARD:
      case SUPER_WILDCARD:
      case TYPE_PARAMETER:
        // Nothing to do.
        break;
      default:
        // The parameterized type is the result of some expression tree.
        // No need to do anything further.
        break;
    }

    return Pair.of(typeargtree, type);
  }

  @Override
  @SuppressWarnings("signature:argument") // PrimitiveType.toString(): @PrimitiveType
  public Void visitPrimitive(AnnotatedPrimitiveType type, Tree tree) {
    if (!checkTopLevelDeclaredOrPrimitiveType
        || checker.shouldSkipUses(type.getUnderlyingType().toString())) {
      return super.visitPrimitive(type, tree);
    }

    if (!visitor.isValidUse(type, tree)) {
      reportInvalidAnnotationsOnUse(type, tree);
    }

    return super.visitPrimitive(type, tree);
  }

  @Override
  public Void visitArray(AnnotatedArrayType type, Tree tree) {
    // TODO: is there already or add a helper method
    // to determine the non-array component type
    AnnotatedTypeMirror comp = type;
    do {
      comp = ((AnnotatedArrayType) comp).getComponentType();
    } while (comp.getKind() == TypeKind.ARRAY);

    if (comp.getKind() == TypeKind.DECLARED
        && checker.shouldSkipUses(((AnnotatedDeclaredType) comp).getUnderlyingType().asElement())) {
      return super.visitArray(type, tree);
    }

    if (!visitor.isValidUse(type, tree)) {
      reportInvalidAnnotationsOnUse(type, tree);
    }

    return super.visitArray(type, tree);
  }

  /**
   * Checks that the annotations on the type arguments supplied to a type or a method invocation are
   * within the bounds of the type variables as declared, and issues the "type.argument" error if
   * they are not.
   *
   * @param type the type to check
   * @param tree the type's tree
   */
  protected Void visitParameterizedType(AnnotatedDeclaredType type, ParameterizedTypeTree tree) {
    // System.out.printf("TypeValidator.visitParameterizedType: type: %s, tree: %s%n", type, tree);

    if (TreeUtils.isDiamondTree(tree)) {
      return null;
    }

    final TypeElement element = (TypeElement) type.getUnderlyingType().asElement();
    if (checker.shouldSkipUses(element)) {
      return null;
    }

    List<AnnotatedTypeParameterBounds> bounds = atypeFactory.typeVariablesFromUse(type, element);

    visitor.checkTypeArguments(
        tree,
        bounds,
        type.getTypeArguments(),
        tree.getTypeArguments(),
        element.getSimpleName(),
        element.getTypeParameters());

    return null;
  }

  @Override
  public Void visitTypeVariable(AnnotatedTypeVariable type, Tree tree) {
    if (visitedNodes.containsKey(type)) {
      return visitedNodes.get(type);
    }

    if (type.isDeclaration() && !areBoundsValid(type.getUpperBound(), type.getLowerBound())) {
      reportInvalidBounds(type, tree);
    }

    return super.visitTypeVariable(type, tree);
  }

  @Override
  public Void visitWildcard(AnnotatedWildcardType type, Tree tree) {
    if (visitedNodes.containsKey(type)) {
      return visitedNodes.get(type);
    }

    if (!areBoundsValid(type.getExtendsBound(), type.getSuperBound())) {
      reportInvalidBounds(type, tree);
    }

    return super.visitWildcard(type, tree);
  }

  /**
   * Returns true if the effective annotations on the upperBound are above those on the lowerBound.
   *
   * @return true if the effective annotations on the upperBound are above those on the lowerBound
   */
  public boolean areBoundsValid(
      final AnnotatedTypeMirror upperBound, final AnnotatedTypeMirror lowerBound) {
    final QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();
    final Set<AnnotationMirror> upperBoundAnnos =
        AnnotatedTypes.findEffectiveAnnotations(qualifierHierarchy, upperBound);
    final Set<AnnotationMirror> lowerBoundAnnos =
        AnnotatedTypes.findEffectiveAnnotations(qualifierHierarchy, lowerBound);

    if (upperBoundAnnos.size() == lowerBoundAnnos.size()) {
      return qualifierHierarchy.isSubtype(lowerBoundAnnos, upperBoundAnnos);
    } // else
    //  When upperBoundAnnos.size() != lowerBoundAnnos.size() one of the two bound types will
    //  be reported as invalid.  Therefore, we do not do any other comparisons nor do we report
    //  a bound

    return true;
  }
}
