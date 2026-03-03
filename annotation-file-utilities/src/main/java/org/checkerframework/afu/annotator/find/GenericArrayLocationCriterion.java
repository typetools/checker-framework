package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeKind;
import org.checkerframework.afu.annotator.Main;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.TypePath;

/**
 * GenericArrayLocationCriterion represents the criterion specifying the location of an element in
 * the generic/array hierarchy as specified by the JSR 308 proposal.
 */
public class GenericArrayLocationCriterion implements Criterion {

  /** If true, output debug information. */
  private static final boolean debug = false;

  /** The location as a list of TypePathEntrys. */
  private final List<TypePathEntry> location;

  /** The location as a full TypePath. */
  private final TypePath typePath;

  // represents all but the last element of the location list
  // TODO: this field is initialized, but never read!
  // I removed it, see the version control history.
  // private Criterion parentCriterion;

  /**
   * Creates a new GenericArrayLocationCriterion specifying that the element is an outer type, such
   * as: {@code @A List<Integer>} or {@code Integer @A []}
   */
  public GenericArrayLocationCriterion() {
    this(null, null);
  }

  /**
   * Creates a new GenericArrayLocationCriterion representing the end of the given path.
   *
   * @param typePath the path to the location of the element being represented
   */
  public GenericArrayLocationCriterion(TypePath typePath) {
    this(typePath, TypePathEntry.typePathToList(typePath));
  }

  /**
   * Creates a new GenericArrayLocationCriterion representing the end of the given path.
   *
   * @param location a list of TypePathEntrys to the location of the element being represented
   */
  public GenericArrayLocationCriterion(List<TypePathEntry> location) {
    this.location = location;
    this.typePath = TypePathEntry.listToTypePath(location);
  }

  /**
   * Creates a new GenericArrayLocationCriterion representing the end of the given path.
   *
   * @param typePath the path to the location of the element being represented
   * @param location a list of TypePathEntrys to the location of the element being represented
   */
  private GenericArrayLocationCriterion(TypePath typePath, List<TypePathEntry> location) {
    this.typePath = typePath;
    this.location = location;
  }

  @Override
  public boolean isSatisfiedBy(@Nullable TreePath path, @FindDistinct Tree leaf) {
    if (path == null) {
      return false;
    }
    assert path.getLeaf() == leaf;
    return isSatisfiedBy(path);
  }

  /**
   * Determines if the given list holds only {@link TypePathEntry}s with the tag {@link
   * TypePath#ARRAY_ELEMENT}.
   *
   * @param location the list to check
   * @return {@code true} if the list only contains {@link TypePath#ARRAY_ELEMENT}, {@code false}
   *     otherwise
   */
  private boolean containsOnlyArray(List<TypePathEntry> location) {
    for (TypePathEntry tpe : location) {
      if (tpe.step != TypePath.ARRAY_ELEMENT) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isSatisfiedBy(@Nullable TreePath path) {
    if (path == null || path.getParentPath() == null) {
      if (debug) {
        System.out.println(
            "GenericArrayLocationCriterion.isSatisfiedBy() with null path gives false.");
      }
      return false;
    }

    if (debug) {
      System.out.printf(
          "GenericArrayLocationCriterion.isSatisfiedBy():%n"
              + "  leaf of path: %s%n"
              + "  searched location: %s%n",
          path.getLeaf(), typePath);
    }

    TreePath pathRemaining = path;
    Tree leaf = path.getLeaf();

    // Don't allow annotations directly on these tree kinds if the child type is
    // a MEMBER_SELECT. This way we'll continue to traverse deeper in the tree
    // to find the correct MEMBER_SELECT.
    Tree child = null;
    if (leaf instanceof ParameterizedTypeTree) {
      child = ((ParameterizedTypeTree) leaf).getType();
    } else if (leaf instanceof VariableTree) {
      child = ((VariableTree) leaf).getType();
    } else if (leaf instanceof NewClassTree) {
      child = ((NewClassTree) leaf).getIdentifier();
    } else if (leaf instanceof NewArrayTree && typePath != null) {
      child = ((NewArrayTree) leaf).getType();
    }
    if (child != null && child instanceof MemberSelectTree) {
      JCExpression exp = ((JCFieldAccess) child).getExpression();
      if ((exp.type != null && exp.type.getKind() == TypeKind.PACKAGE)
          || typePath == null
          || typePath.getStep(typePath.getLength() - 1) != TypePath.INNER_TYPE) {
        return false;
      }
    }

    if (leaf instanceof MemberSelectTree) {
      JCFieldAccess fieldAccess = (JCFieldAccess) leaf;
      if (isStatic(fieldAccess)) {
        // If this MEMBER_SELECT is for a static class...
        if (typePath == null) {
          // ...and it does not go on a compound type, this is the right place.
          return true;
        } else if (isGenericOrArray(path.getParentPath().getLeaf())
            && isGenericOrArray(path.getParentPath().getParentPath().getLeaf())) {
          // If the two parents above this are compound types, then skip
          // the compound type directly above. For example, to get to Outer.Inner
          // of Outer.Inner<K, V> we had to get through the PARAMETERIZED_TYPE
          // node. But we didn't go deeper in the compound type in the way the
          // type path defines, we just went deeper to get to the outer type. So
          // skip this node later when checking to make sure that we're in the
          // correct part of the compound type.
          pathRemaining = path.getParentPath();
        }
      } else {
        JCExpression exp = fieldAccess.getExpression();
        if (exp instanceof MemberSelectTree
            && exp.type != null
            && exp.type.getKind() == TypeKind.PACKAGE) {
          if (typePath == null) {
            return true;
          } // else, keep going to make sure we're in the right part of the
          // compound type
        } else {
          if (typePath != null
              && typePath.getStep(typePath.getLength() - 1) != TypePath.INNER_TYPE) {
            return false;
          }
        }
      }
    }

    if (typePath == null) {
      // no inner type location, want to annotate outermost type
      // e.g.,  @Nullable List list;
      //        @Nullable List<String> list;
      //        String @Nullable [] array;
      leaf = path.getLeaf();
      Tree parent = path.getParentPath().getLeaf();

      boolean result =
          ((leaf instanceof NewArrayTree)
              || (leaf instanceof NewClassTree)
              || (leaf instanceof AnnotatedTypeTree
                  && isSatisfiedBy(
                      TreePath.getPath(path, ((AnnotatedTypeTree) leaf).getUnderlyingType())))
              || ((isGenericOrArray(leaf)
                      // or, it might be a raw type
                      || (leaf instanceof IdentifierTree)
                      || (leaf instanceof MethodTree)
                      || (leaf instanceof TypeParameterTree)
                      // I don't know why a GenericArrayLocationCriterion
                      // is being created in this case, but it is.
                      || (leaf instanceof PrimitiveTypeTree)
                  // TODO: do we need wildcards here?
                  // || leaf instanceof WildcardTree
                  )
                  && !isGenericOrArray(parent)));
      if (debug) {
        System.out.printf(
            "GenericArrayLocationCriterion.isSatisfiedBy: locationInParent==null%n"
                + "  leaf=%s (%s)%n"
                + "  parent=%s (%s)%n"
                + "  => %s (%s %s)%n",
            leaf,
            leaf.getClass(),
            parent,
            parent.getClass(),
            result,
            isGenericOrArray(leaf),
            !isGenericOrArray(parent));
      }

      return result;
    }

    // If we've made it this far then we've determined that *if* this is the right
    // place to insert the annotation this is the MEMBER_SELECT it should be
    // inserted on. So remove the rest of the MEMBER_SELECTs to get down to the
    // compound type and make sure the compound type location matches.
    while (pathRemaining.getParentPath().getLeaf() instanceof MemberSelectTree) {
      pathRemaining = pathRemaining.getParentPath();
    }

    List<TypePathEntry> locationRemaining = new ArrayList<>(location);

    while (!locationRemaining.isEmpty()) {
      // annotating an inner type
      leaf = pathRemaining.getLeaf();
      if ((leaf instanceof NewArrayTree) && containsOnlyArray(locationRemaining)) {
        if (debug) {
          System.out.println("Found a matching NEW_ARRAY");
        }
        return true;
      }
      TreePath parentPath = pathRemaining.getParentPath();
      if (parentPath == null) {
        if (debug) {
          System.out.println("Parent path is null and therefore false.");
        }
        return false;
      }
      Tree parent = parentPath.getLeaf();

      if (parent instanceof AnnotatedTypeTree) {
        // If the parent is an annotated type, we did not really go up a level.
        // Therefore, skip up one more level.
        parentPath = parentPath.getParentPath();
        parent = parentPath.getLeaf();
      }

      if (debug) {
        System.out.printf(
            "locationRemaining: %s, leaf: %s parent: %s %s%n",
            locationRemaining,
            Main.treeToString(leaf),
            Main.treeToString(parent),
            parent.getClass());
      }

      TypePathEntry loc = locationRemaining.get(locationRemaining.size() - 1);
      if (loc.step == TypePath.INNER_TYPE) {
        if (leaf instanceof ParameterizedTypeTree) {
          leaf = parent;
          parentPath = parentPath.getParentPath();
          parent = parentPath.getLeaf();
        }
        if (!(leaf instanceof MemberSelectTree)) {
          return false;
        }

        JCFieldAccess fieldAccess = (JCFieldAccess) leaf;
        if (isStatic(fieldAccess)) {
          return false;
        }
        locationRemaining.remove(locationRemaining.size() - 1);
        leaf = fieldAccess.selected;
        pathRemaining = parentPath;
        // TreePath.getPath(pathRemaining.getCompilationUnit(), leaf);
      } else if (loc.step == TypePath.WILDCARD_BOUND
          && leaf.getKind() == Tree.Kind.UNBOUNDED_WILDCARD) {
        // Check if the leaf is an unbounded wildcard instead of the parent, since unbounded
        // wildcard has no members so it can't be the parent of anything.
        if (locationRemaining.isEmpty()) {
          return false;
        }

        // The following check is necessary because Oracle has decided that
        //   x instanceof Class<? extends Object>
        // will remain illegal even though it means the same thing as
        //   x instanceof Class<?>.
        TreePath gpath = parentPath.getParentPath();
        if (gpath != null) { // TODO: skip over existing annotations?
          Tree gparent = gpath.getLeaf();
          if (gparent instanceof InstanceOfTree) {
            TreeFinder.warn.debug(
                "WARNING: wildcard bounds not allowed "
                    + "in 'instanceof' expression; skipping insertion%n");
            return false;
          } else if (gparent instanceof ParameterizedTypeTree) {
            gpath = gpath.getParentPath();
            if (gpath != null && gpath.getLeaf() instanceof ArrayTypeTree) {
              TreeFinder.warn.debug(
                  "WARNING: wildcard bounds not allowed "
                      + "in generic array type; skipping insertion%n");
              return false;
            }
          }
        }
        locationRemaining.remove(locationRemaining.size() - 1);
      } else if (parent instanceof ParameterizedTypeTree) {
        if (loc.step != TypePath.TYPE_ARGUMENT) {
          return false;
        }

        // Find the outermost type in the AST; if it has parameters,
        // pop the stack once for each inner type on the end of the type
        // path and check the parameter.
        Tree inner = ((ParameterizedTypeTree) parent).getType();
        int i = locationRemaining.size() - 1; // last valid type path index
        locationRemaining.remove(i--);
        while (inner instanceof MemberSelectTree && !isStatic((JCFieldAccess) inner)) {
          // fieldAccess.type != null && fieldAccess.type.getKind() == TypeKind.DECLARED
          // && fieldAccess.type.tsym.isStatic()
          // TODO: check whether MEMBER_SELECT indicates inner or qualifier?
          if (i < 0) {
            break;
          }
          if (locationRemaining.get(i).step != TypePath.INNER_TYPE) {
            return false;
          }
          locationRemaining.remove(i--);
          inner = ((MemberSelectTree) inner).getExpression();
          if (inner instanceof AnnotatedTypeTree) {
            inner = ((AnnotatedTypeTree) inner).getUnderlyingType();
          }
          if (inner instanceof ParameterizedTypeTree) {
            inner = ((ParameterizedTypeTree) inner).getType();
          }
        }
        if (i >= 0 && locationRemaining.get(i).step == TypePath.INNER_TYPE) {
          return false;
        }

        // annotating List<@A Integer>
        // System.out.printf("parent instanceof ParameterizedTypeTree: %s loc=%d%n",
        //                   Main.treeToString(parent), loc);
        List<? extends Tree> childTrees = ((ParameterizedTypeTree) parent).getTypeArguments();
        boolean found = false;
        if (childTrees.size() > loc.argument) {
          Tree childi = childTrees.get(loc.argument);
          if (childi instanceof AnnotatedTypeTree) {
            childi = ((AnnotatedTypeTree) childi).getUnderlyingType();
          }
          @SuppressWarnings("interning:not.interned") // reference equality check
          boolean foundLeaf = childi == leaf;
          if (foundLeaf) {
            for (TreePath outerPath = parentPath.getParentPath();
                outerPath.getLeaf() instanceof MemberSelectTree
                    && !isStatic((JCFieldAccess) outerPath.getLeaf());
                outerPath = outerPath.getParentPath()) {
              outerPath = outerPath.getParentPath();
              if (outerPath.getLeaf() instanceof AnnotatedTypeTree) {
                outerPath = outerPath.getParentPath();
              }
              if (!(outerPath.getLeaf() instanceof ParameterizedTypeTree)) {
                break;
              }
              parentPath = outerPath;
            }
            pathRemaining = parentPath;
            found = true;
          }
        }
        if (!found) {
          if (debug) {
            System.out.printf(
                "Generic failed for leaf: %s: nr children: %d loc: %s child: %s%n",
                leaf,
                childTrees.size(),
                loc,
                ((childTrees.size() > loc.argument) ? childTrees.get(loc.argument) : null));
          }
          return false;
        }
      } else if (parent.getKind() == Tree.Kind.EXTENDS_WILDCARD
          || parent.getKind() == Tree.Kind.SUPER_WILDCARD) {
        if (loc.step != TypePath.WILDCARD_BOUND || locationRemaining.size() == 1) {
          // If there's only one location left, this can't be a match since a wildcard
          // needs to be in another kind of compound type.
          return false;
        }
        locationRemaining.remove(locationRemaining.size() - 1);
        // annotating List<? extends @A Integer>
        // System.out.printf("parent instanceof extends WildcardTree: %s loc=%d%n",
        //                   Main.treeToString(parent), loc);
        WildcardTree wct = (WildcardTree) parent;
        Tree boundTree = wct.getBound();

        if (debug) {
          String wildcardType;
          if (parent.getKind() == Tree.Kind.EXTENDS_WILDCARD) {
            wildcardType = "ExtendsWildcard";
          } else {
            wildcardType = "SuperWildcard";
          }
          System.out.printf(
              "%s with bound %s gives %s%n", wildcardType, boundTree, boundTree.equals(leaf));
        }

        if (boundTree.equals(leaf)) {
          if (locationRemaining.isEmpty()) {
            return true;
          } else {
            pathRemaining = parentPath;
          }
        } else {
          return false;
        }
      } else if (parent instanceof ArrayTypeTree) {
        if (loc.step != TypePath.ARRAY_ELEMENT) {
          return false;
        }
        locationRemaining.remove(locationRemaining.size() - 1);
        // annotating Integer @A []
        parentPath = TreeFinder.largestContainingArray(parentPath);
        parent = parentPath.getLeaf();
        // System.out.printf("parent instanceof ArrayTypeTree: %s loc=%d%n",
        //                   parent, loc);
        Tree elt = ((ArrayTypeTree) parent).getType();
        while (!locationRemaining.isEmpty()
            && locationRemaining.get(locationRemaining.size() - 1).step == TypePath.ARRAY_ELEMENT) {
          if (!(elt instanceof ArrayTypeTree)) {
            if (debug) {
              System.out.printf("Element: %s is not an ArrayTypeTree and therefore false.%n", elt);
            }
            return false;
          }
          elt = ((ArrayTypeTree) elt).getType();
          locationRemaining.remove(locationRemaining.size() - 1);
        }

        boolean b = elt.equals(leaf);
        if (debug) {
          System.out.printf(
              "parent %s instanceof ArrayTypeTree: %b %s %s %s%n",
              parent, elt.equals(leaf), elt, leaf, loc);
          System.out.printf("b=%s elt=%s leaf=%s%n", b, elt, leaf);
        }

        // TODO:  The parent criterion should be exact, not just "in".
        // Otherwise the criterion [1]  matches  [5 4 3 2 1].
        // This is a disadvantage of working from the inside out instead of the outside in.
        if (b) {
          pathRemaining = parentPath;
        } else {
          return false;
        }
      } else if (parent instanceof NewArrayTree) {
        if (loc.step != TypePath.ARRAY_ELEMENT) {
          return false;
        }
        if (debug) {
          System.out.println("Parent is a NEW_ARRAY and always gives true.");
        }
        return true;
      } else {
        if (debug) {
          System.out.printf("unrecognized parent kind = %s%n", parent.getKind());
        }
        return false;
      }
    }

    // no (remaining) inner type location, want to annotate outermost type
    // e.g.,  @Nullable List list;
    //        @Nullable List<String> list;
    TreePath parentPath = pathRemaining.getParentPath();
    if (parentPath == null) {
      if (debug) {
        System.out.println("Parent path is null and therefore false.");
      }
      return false;
    }
    Tree parent = pathRemaining.getParentPath().getLeaf();
    if (debug) {
      leaf = pathRemaining.getLeaf();
      System.out.printf(
          "No (remaining) inner type location:%n  leaf: %s %b%n  parent: %s %b%n  result: %s%n",
          Main.treeToString(leaf),
          isGenericOrArray(leaf),
          Main.treeToString(parent),
          isGenericOrArray(parent),
          !isGenericOrArray(parent));
    }

    return !isGenericOrArray(parent);
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return true;
  }

  /**
   * Returns true if the field is static.
   *
   * @param fieldAccess a field access expression
   * @return true if the field is static
   */
  private boolean isStatic(JCFieldAccess fieldAccess) {
    return fieldAccess.type != null
        && fieldAccess.type.getKind() == TypeKind.DECLARED
        && fieldAccess.type.tsym.isStatic();
  }

  /**
   * Returns true if the given tree is generic or an array.
   *
   * @param t a tree
   * @return true if the given tree is generic or an array
   */
  private boolean isGenericOrArray(Tree t) {
    return ((t instanceof ParameterizedTypeTree)
        || (t instanceof ArrayTypeTree)
        || (t.getKind() == Tree.Kind.EXTENDS_WILDCARD)
        || (t.getKind() == Tree.Kind.SUPER_WILDCARD)
        || (t instanceof AnnotatedTypeTree
            && isGenericOrArray(((AnnotatedTypeTree) t).getUnderlyingType()))
    // Monolithic:  one node for entire "new".  So, handle specially.
    // || (t.getKind() == Tree.Kind.NEW_ARRAY)
    );
  }

  @Override
  public Kind getKind() {
    return Criterion.Kind.GENERIC_ARRAY_LOCATION;
  }

  @Override
  public String toString() {
    return "GenericArrayLocationCriterion at "
        + (typePath == null ? "outermost type" : ("( " + typePath.toString() + " )"));
  }

  /**
   * Gets the type path location of this criterion.
   *
   * @return an unmodifiable list of {@link TypePathEntry}s
   */
  public List<TypePathEntry> getLocation() {
    return location == null ? Collections.emptyList() : Collections.unmodifiableList(location);
  }
}
