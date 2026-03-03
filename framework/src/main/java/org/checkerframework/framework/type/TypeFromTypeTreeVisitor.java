package org.checkerframework.framework.type;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.WildcardTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;

/**
 * Converts type trees into AnnotatedTypeMirrors.
 *
 * @see org.checkerframework.framework.type.TypeFromTree
 */
class TypeFromTypeTreeVisitor extends TypeFromTreeVisitor {

  /** Creates a TypeFromTypeTreeVisitor. */
  public TypeFromTypeTreeVisitor() {}

  /**
   * A mapping from TypeParameterTree to its type. This is used to correctly initialize recursive
   * type variables.
   */
  private final Map<TypeParameterTree, AnnotatedTypeVariable> visitedTypeParameter =
      new HashMap<>();

  @Override
  public AnnotatedTypeMirror visitAnnotatedType(AnnotatedTypeTree tree, AnnotatedTypeFactory f) {
    AnnotatedTypeMirror type = visit(tree.getUnderlyingType(), f);
    if (type == null) { // e.g., for receiver type
      type = f.toAnnotatedType(f.types.getNoType(TypeKind.NONE), false);
    }
    assert AnnotatedTypeFactory.validAnnotatedType(type);
    List<? extends AnnotationMirror> annos = TreeUtils.annotationsFromTree(tree);

    if (type.getKind() == TypeKind.WILDCARD) {
      // Work-around for https://github.com/eisop/checker-framework/issues/17
      // For an annotated wildcard tree tree, the type attached to the
      // tree is a WildcardType with a correct bound (set to the type
      // variable which the wildcard instantiates). The underlying type is
      // also a WildcardType but with a bound of null. Here we update the
      // bound of the underlying WildcardType to be consistent.
      WildcardType wildcardAttachedToNode = (WildcardType) TreeUtils.typeOf(tree);
      WildcardType underlyingWildcard = (WildcardType) type.getUnderlyingType();
      underlyingWildcard.withTypeVar(wildcardAttachedToNode.bound);
      // End of work-around

      AnnotatedWildcardType wctype = ((AnnotatedWildcardType) type);
      ExpressionTree underlyingTree = tree.getUnderlyingType();

      if (underlyingTree.getKind() == Tree.Kind.UNBOUNDED_WILDCARD) {
        // primary annotations on unbounded wildcard types apply to both bounds
        wctype.getExtendsBound().addAnnotations(annos);
        wctype.getSuperBound().addAnnotations(annos);
      } else if (underlyingTree.getKind() == Tree.Kind.EXTENDS_WILDCARD) {
        wctype.getSuperBound().addAnnotations(annos);
      } else if (underlyingTree.getKind() == Tree.Kind.SUPER_WILDCARD) {
        wctype.getExtendsBound().addAnnotations(annos);
      } else {
        throw new BugInCF(
            "Unexpected kind for type.  tree="
                + tree
                + " type="
                + type
                + " kind="
                + underlyingTree.getKind());
      }
    } else {
      type.addAnnotations(annos);
    }

    return type;
  }

  @Override
  public AnnotatedTypeMirror visitArrayType(ArrayTypeTree tree, AnnotatedTypeFactory f) {
    AnnotatedTypeMirror component = visit(tree.getType(), f);

    AnnotatedTypeMirror result = f.type(tree);
    assert result instanceof AnnotatedArrayType;
    ((AnnotatedArrayType) result).setComponentType(component);
    return result;
  }

  @Override
  public AnnotatedTypeMirror visitParameterizedType(
      ParameterizedTypeTree tree, AnnotatedTypeFactory f) {

    ClassSymbol baseType = (ClassSymbol) TreeUtils.elementFromTree(tree.getType());
    updateWildcardBounds(tree.getTypeArguments(), baseType.getTypeParameters());

    List<AnnotatedTypeMirror> args =
        CollectionsPlume.mapList((Tree t) -> visit(t, f), tree.getTypeArguments());

    AnnotatedTypeMirror result = f.type(tree); // use creator?
    AnnotatedTypeMirror atype = visit(tree.getType(), f);
    result.addAnnotations(atype.getPrimaryAnnotations());
    // new ArrayList<>() type is AnnotatedExecutableType for some reason

    // Don't initialize the type arguments if they are empty. The type arguments might be a
    // diamond which should be inferred.
    if (result instanceof AnnotatedDeclaredType && !args.isEmpty()) {
      assert result instanceof AnnotatedDeclaredType : tree + " --> " + result;
      ((AnnotatedDeclaredType) result).setTypeArguments(args);
    }
    return result;
  }

  /**
   * Work around a bug in javac 9 where sometimes the bound field is set to the transitive
   * supertype's type parameter instead of the type parameter which the wildcard directly
   * instantiates. See https://github.com/eisop/checker-framework/issues/18
   *
   * <p>Sets each wildcard type argument's bound from typeArgs to the corresponding type parameter
   * from typeParams.
   *
   * <p>If typeArgs.isEmpty() the method does nothing and returns. Otherwise, typeArgs.size() has to
   * be equal to typeParams.size().
   *
   * <p>For each wildcard type argument and corresponding type parameter, sets the
   * WildcardType.bound field to the corresponding type parameter, if and only if the owners of the
   * existing bound and the type parameter are different.
   *
   * <p>In scenarios where the bound's owner is the same, we don't want to replace a
   * capture-converted bound in the wildcard type with a non-capture-converted bound given by the
   * type parameter declaration.
   *
   * @param typeArgs the type of the arguments at (e.g., at the call side)
   * @param typeParams the type of the formal parameters (e.g., at the method declaration)
   */
  @SuppressWarnings("interning:not.interned") // workaround for javac bug
  private void updateWildcardBounds(
      List<? extends Tree> typeArgs, List<TypeVariableSymbol> typeParams) {
    if (typeArgs.isEmpty()) {
      // Nothing to do for empty type arguments.
      return;
    }
    assert typeArgs.size() == typeParams.size();

    Iterator<? extends Tree> typeArgsItr = typeArgs.iterator();
    Iterator<TypeVariableSymbol> typeParamsItr = typeParams.iterator();
    while (typeArgsItr.hasNext()) {
      Tree typeArg = typeArgsItr.next();
      TypeVariableSymbol typeParam = typeParamsItr.next();
      if (typeArg instanceof WildcardTree) {
        TypeVar typeVar = (TypeVar) typeParam.asType();
        WildcardType wcType = (WildcardType) ((JCWildcard) typeArg).type;
        if (wcType.bound != null
            && wcType.bound.tsym != null
            && typeVar.tsym != null
            && wcType.bound.tsym.owner != typeVar.tsym.owner) {
          wcType.withTypeVar(typeVar);
        }
      }
    }
  }

  @Override
  public AnnotatedTypeMirror visitPrimitiveType(PrimitiveTypeTree tree, AnnotatedTypeFactory f) {
    return f.type(tree);
  }

  @Override
  public AnnotatedTypeVariable visitTypeParameter(
      TypeParameterTree tree, @FindDistinct AnnotatedTypeFactory f) {
    if (visitedTypeParameter.containsKey(tree)) {
      return visitedTypeParameter.get(tree);
    }

    AnnotatedTypeVariable result = (AnnotatedTypeVariable) f.type(tree);
    // If this type parameter is recursive and it is found again while visiting the bounds, then
    // use the same AnnotateTypeVariable object.
    visitedTypeParameter.put(tree, result);

    List<AnnotatedTypeMirror> bounds = new ArrayList<>(tree.getBounds().size());
    for (Tree t : tree.getBounds()) {
      bounds.add(visit(t, f));
    }
    visitedTypeParameter.remove(tree);

    List<? extends AnnotationMirror> annotations = TreeUtils.annotationsFromTree(tree);
    result.getLowerBound().addAnnotations(annotations);

    switch (bounds.size()) {
      case 0:
        break;
      case 1:
        result.setUpperBound(bounds.get(0));
        break;
      default:
        AnnotatedIntersectionType intersection = (AnnotatedIntersectionType) result.getUpperBound();
        intersection.setBounds(bounds);
        intersection.copyIntersectionBoundAnnotations();
    }

    return result;
  }

  @Override
  public AnnotatedTypeMirror visitWildcard(WildcardTree tree, AnnotatedTypeFactory f) {

    AnnotatedTypeMirror bound = visit(tree.getBound(), f);

    AnnotatedTypeMirror result = f.type(tree);
    assert result instanceof AnnotatedWildcardType;
    f.initializeAtm(result);

    // for wildcards unlike type variables there are bounds that differ in type from
    // result.  These occur for RAW types.  In this case, use the newly created bound
    // rather than merging into result
    if (tree.getKind() == Tree.Kind.SUPER_WILDCARD) {
      ((AnnotatedWildcardType) result).setSuperBound(bound);

    } else if (tree.getKind() == Tree.Kind.EXTENDS_WILDCARD) {
      ((AnnotatedWildcardType) result).setExtendsBound(bound);
    }
    return result;
  }

  /**
   * If a tree is can be found for the declaration of the type variable {@code type}, then a {@link
   * AnnotatedTypeVariable} is returned with explicit annotations from the type variables declared
   * bounds. If a tree cannot be found, then {@code type}, converted to a use, is returned.
   *
   * @param type type variable used to find declaration tree
   * @param f annotated type factory
   * @return the AnnotatedTypeVariable from the declaration of {@code type} or {@code type} if no
   *     tree is found
   */
  private AnnotatedTypeVariable getTypeVariableFromDeclaration(
      AnnotatedTypeVariable type, AnnotatedTypeFactory f) {
    TypeVariable typeVar = type.getUnderlyingType();
    TypeParameterElement tpe = (TypeParameterElement) typeVar.asElement();
    Element elt = tpe.getGenericElement();
    if (elt instanceof TypeElement) {
      TypeElement typeElt = (TypeElement) elt;
      int idx = typeElt.getTypeParameters().indexOf(tpe);
      if (idx == -1) {
        idx = findIndex(typeElt.getTypeParameters(), tpe);
      }
      ClassTree cls = (ClassTree) f.declarationFromElement(typeElt);
      if (cls == null || cls.getTypeParameters().isEmpty()) {
        // The type parameters in the source tree were already erased. The element already
        // contains all necessary information and we can return that.
        return type.asUse();
      }

      // `forTypeVariable` is called for Identifier, MemberSelect and UnionType trees,
      // none of which are declarations.  But `cls.getTypeParameters()` returns a list
      // of type parameter declarations (`TypeParameterTree`), so this call
      // will return a declaration ATV.  So change it to a use.
      return visitTypeParameter(cls.getTypeParameters().get(idx), f).asUse();
    } else if (elt instanceof ExecutableElement) {
      ExecutableElement exElt = (ExecutableElement) elt;
      int idx = exElt.getTypeParameters().indexOf(tpe);
      if (idx == -1) {
        idx = findIndex(exElt.getTypeParameters(), tpe);
      }
      MethodTree meth = (MethodTree) f.declarationFromElement(exElt);
      if (meth == null) {
        // meth can be null when no source code was found for it.
        return type.asUse();
      }
      // This works the same as the case above.  Even though `meth` itself is not a
      // type declaration tree, the elements of `meth.getTypeParameters()` still are.
      AnnotatedTypeVariable result =
          visitTypeParameter(meth.getTypeParameters().get(idx), f).shallowCopy();
      result.setDeclaration(false);
      return result;
    } else if (TypesUtils.isCapturedTypeVariable(typeVar)) {
      // Captured type variables can have a generic element (owner) that is
      // not an element at all, namely Symtab.noSymbol.
      return type.asUse();
    } else {
      throw new BugInCF("TypeFromTree.forTypeVariable: not a supported element: " + elt);
    }
  }

  /**
   * Finds the index of {@code type} in {@code typeParameters} using {@link
   * TypesUtils#areSame(TypeVariable, TypeVariable)} instead of {@link Object#equals(Object)}.
   *
   * @param typeParameters a list of type parameters
   * @param type a type parameter
   * @return the index of {@code type} in {@code typeParameters} using {@link
   *     TypesUtils#areSame(TypeVariable, TypeVariable)} or -1 if it does not exist
   */
  private int findIndex(
      List<? extends TypeParameterElement> typeParameters, TypeParameterElement type) {

    TypeVariable typeVariable = (TypeVariable) type.asType();

    for (int i = 0; i < typeParameters.size(); i++) {
      TypeVariable typeVariable1 = (TypeVariable) typeParameters.get(i).asType();
      if (TypesUtils.areSame(typeVariable1, typeVariable)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public AnnotatedTypeMirror visitIdentifier(IdentifierTree tree, AnnotatedTypeFactory f) {

    AnnotatedTypeMirror type = f.type(tree);

    if (type.getKind() == TypeKind.TYPEVAR) {
      return getTypeVariableFromDeclaration((AnnotatedTypeVariable) type, f);
    }

    return type;
  }

  @Override
  public AnnotatedTypeMirror visitMemberSelect(MemberSelectTree tree, AnnotatedTypeFactory f) {

    AnnotatedTypeMirror type = f.type(tree);

    if (type.getKind() == TypeKind.TYPEVAR) {
      return getTypeVariableFromDeclaration((AnnotatedTypeVariable) type, f);
    }

    return type;
  }

  @Override
  public AnnotatedTypeMirror visitUnionType(UnionTypeTree tree, AnnotatedTypeFactory f) {
    AnnotatedTypeMirror type = f.type(tree);

    if (type.getKind() == TypeKind.TYPEVAR) {
      return getTypeVariableFromDeclaration((AnnotatedTypeVariable) type, f);
    }

    return type;
  }

  @Override
  public AnnotatedTypeMirror visitIntersectionType(
      IntersectionTypeTree tree, AnnotatedTypeFactory f) {
    // This method is only called for IntersectionTypes in casts.  There is no
    // IntersectionTypeTree
    // for a type variable bound that is an intersection.  See #visitTypeParameter.
    AnnotatedIntersectionType type = (AnnotatedIntersectionType) f.type(tree);
    List<AnnotatedTypeMirror> bounds =
        CollectionsPlume.mapList((Tree boundTree) -> visit(boundTree, f), tree.getBounds());
    type.setBounds(bounds);
    type.copyIntersectionBoundAnnotations();
    return type;
  }
}
