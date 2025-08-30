package org.checkerframework.afu.annotator.find;

import com.google.errorprone.annotations.InlineMe;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.afu.annotator.Main;
import org.checkerframework.afu.scenelib.el.BoundLocation;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.afu.scenelib.el.TypeIndexLocation;
import org.checkerframework.afu.scenelib.io.ASTPath;
import org.checkerframework.afu.scenelib.io.DebugWriter;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.objectweb.asm.TypePath;

/**
 * Represents a set of Criterion objects for locating a program element in a source tree.
 *
 * <p>This class also contains static factory methods for creating a {@code Criterion}.
 */
public final class Criteria {
  /** Debugging logger. */
  public static DebugWriter dbug = new DebugWriter(false);

  // NOTE: This does not permit multiple criteria of a given kind.
  /** The set of criterion objects, indexed by kind. */
  private final Map<Criterion.Kind, Criterion> criteria;

  /** Creates a new {@code Criteria} without any {@code Criterion}. */
  public Criteria() {
    this.criteria = new LinkedHashMap<>();
  }

  /**
   * Add a {@code Criterion} to this {@code Criteria}.
   *
   * @param c the criterion to add
   */
  public void add(Criterion c) {
    Criterion old = criteria.put(c.getKind(), c);
    if (old != null && !c.toString().equals(old.toString())) {
      throw new Error("Overwrote " + c.getKind() + " => " + old + "; new = " + c);
    }
  }

  /**
   * Add a {@code Criterion} to this {@code Criteria}, permitting replacement.
   *
   * @param c the criterion to add
   */
  /*package-protected*/ void addPermitReplacement(Criterion c) {
    criteria.put(c.getKind(), c);
  }

  /**
   * Returns true if the program element at the leaf of the specified path is satisfied by these
   * criteria.
   *
   * @param path the tree path to check against
   * @param leaf the tree at the leaf of the path; only relevant when the path is null, in which
   *     case the leaf is a CompilationUnitTree
   * @return true if all of these criteria are satisfied by the given path, false otherwise
   */
  // @FindDistinct is for the benefit of an assertion
  public boolean isSatisfiedBy(@Nullable TreePath path, @FindDistinct Tree leaf) {
    if (path == null) {
      return false;
    }
    assert path.getLeaf() == leaf;
    for (Criterion c : criteria.values()) {
      if (!c.isSatisfiedBy(path, leaf)) {
        dbug.debug(
            "UNsatisfied criterion of type %s [%s]:%n    leaf=%s%n",
            c, c.getClass(), Main.leafString(path));
        return false;
      } else {
        dbug.debug(
            "satisfied criterion of type %s [%s]:%n    leaf=%s%n",
            c, c.getClass(), Main.leafString(path));
      }
    }

    if (isSatisfiedByShouldReturnFalse(leaf)) {
      return false;
    }

    return true;
  }

  /**
   * Returns true if the program element at the leaf of the specified path is satisfied by these
   * criteria.
   *
   * @param path the tree path to check against
   * @return true if all of these criteria are satisfied by the given path, false otherwise
   */
  public boolean isSatisfiedBy(@Nullable TreePath path) {
    for (Criterion c : criteria.values()) {
      if (!c.isSatisfiedBy(path)) {
        dbug.debug("UNsatisfied criterion: %s%n", c);
        return false;
      } else {
        dbug.debug("satisfied criterion: %s%n", c);
      }
    }

    if (isSatisfiedByShouldReturnFalse(path.getLeaf())) {
      return false;
    }

    return true;
  }

  /**
   * Returns true if isSatisfiedBy should return false.
   *
   * @param leaf the tree at the leaf of the path
   * @return true if isSatisfiedBy should return false
   */
  private boolean isSatisfiedByShouldReturnFalse(Tree leaf) {
    // A criterion for the constructor method matches a field declaration that has an
    // initializer, since the initialization conceptually occurs in the constructor.

    // Return true if this is a Criteria for a constructor (no deeper)
    // and leaf is not the constructor.
    return (criteria.size() == 2
        && isOnMethod("<init>()V")
        && criteria.containsKey(Criterion.Kind.IN_CLASS)
        && !(leaf instanceof MethodTree));
  }

  /**
   * Returns true if this Criteria only permits type annotations, not declaration annotations.
   *
   * @return true if this Criteria only permits type annotations, not declaration annotations
   */
  public boolean isOnlyTypeAnnotationCriterion() {
    for (Criterion c : criteria.values()) {
      if (c.isOnlyTypeAnnotationCriterion()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if this is the criteria on a receiver.
   *
   * @return true iff this is the criteria on a receiver
   */
  public boolean isOnReceiver() {
    return criteria.containsKey(Criterion.Kind.RECEIVER);
  }

  /**
   * Returns true if this is the criteria on a package.
   *
   * @return true iff this is the criteria on a package
   */
  public boolean isOnPackage() {
    return criteria.containsKey(Criterion.Kind.PACKAGE);
  }

  /**
   * Returns true if this is the criteria on a return type.
   *
   * @return true iff this is the criteria on a return type
   */
  public boolean isOnReturnType() {
    return criteria.containsKey(Criterion.Kind.RETURN_TYPE);
  }

  /**
   * Returns true if this is the criteria on a local variable.
   *
   * @return true iff this is the criteria on a local variable
   */
  public boolean isOnLocalVariable() {
    return criteria.containsKey(Criterion.Kind.LOCAL_VARIABLE);
  }

  /**
   * Returns true if this is the criteria on the RHS of an occurrence of 'instanceof'.
   *
   * @return true if this is the criteria on the RHS of an occurrence of 'instanceof'
   */
  public boolean isOnInstanceof() {
    return criteria.containsKey(Criterion.Kind.INSTANCE_OF);
  }

  /**
   * Returns true if this is the criteria on an object initializer.
   *
   * @return true if this is the criteria on an object initializer
   */
  public boolean isOnNew() {
    return criteria.containsKey(Criterion.Kind.NEW);
  }

  /** Returns true if this is the criteria on a class {@code extends} bound. */
  public boolean isOnTypeDeclarationExtendsClause() {
    for (Criterion c : criteria.values()) {
      if (c.getKind() == Criterion.Kind.EXTIMPLS_LOCATION) {
        return ((ExtImplsLocationCriterion) c).getIndex() == -1;
      }
    }
    return false;
  }

  /**
   * Returns true if this Criteria is on the given method.
   *
   * @param methodname the name of a method
   * @return true if this Criteria is on the given method
   */
  public boolean isOnMethod(String methodname) {
    Criterion c = criteria.get(Criterion.Kind.IN_METHOD);
    return c != null && ((InMethodCriterion) c).name.equals(methodname);
  }

  /** Returns true if this Criteria is on a field declaration. */
  public boolean isOnFieldDeclaration() {
    for (Criterion c : criteria.values()) {
      if (c.getKind() == Criterion.Kind.FIELD && ((FieldCriterion) c).isDeclaration) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if this is the criteria on a variable declaration: a local variable or a field
   * declaration, but not a formal parameter declaration.
   *
   * @return true iff this is the criteria on a local variable
   */
  public boolean isOnVariableDeclaration() {
    // Could fuse the loops for efficiency, but is it important to do so?
    return isOnLocalVariable() || isOnFieldDeclaration();
  }

  /**
   * Gives the AST path specified in the criteria, if any.
   *
   * @return AST path from {@link ASTPathCriterion}, or null if none present
   */
  public ASTPath getASTPath() {
    for (Criterion c : criteria.values()) {
      if (c.getKind() == Criterion.Kind.AST_PATH) {
        return ((ASTPathCriterion) c).astPath;
      }
    }

    return null;
  }

  /**
   * Returns the name of the class specified in the Criteria, if any.
   *
   * @return class name from {@link InClassCriterion}, or null if none present
   */
  public @Nullable @ClassGetName String getClassName() {
    String result = null;
    for (Criterion c : criteria.values()) {
      if (c.getKind() == Criterion.Kind.IN_CLASS) {
        if (result == null) {
          result = ((InClassCriterion) c).className;
        } else {
          throw new Error(
              String.format("In two classes: %s %s", result, ((InClassCriterion) c).className));
        }
      }
    }

    return result;
  }

  /**
   * Returns the name of the method specified in the Criteria, if any.
   *
   * @return method name from {@link InMethodCriterion}, or null if none present
   */
  public String getMethodName() {
    for (Criterion c : criteria.values()) {
      if (c.getKind() == Criterion.Kind.IN_METHOD) {
        return ((InMethodCriterion) c).name;
      }
    }

    return null;
  }

  /**
   * Returns the name of the member field specified in the Criteria, if any.
   *
   * @return field name from {@link FieldCriterion}, or null if none present
   */
  public String getFieldName() {
    for (Criterion c : criteria.values()) {
      if (c.getKind() == Criterion.Kind.FIELD) {
        return ((FieldCriterion) c).varName;
      }
    }

    return null;
  }

  /**
   * Returns a GenericArrayLocationCriterion if this has one, else null.
   *
   * @return a GenericArrayLocationCriterion if this has one, else null
   */
  public GenericArrayLocationCriterion getGenericArrayLocation() {
    for (Criterion c : criteria.values()) {
      if (c.getKind() == Criterion.Kind.GENERIC_ARRAY_LOCATION) {
        return (GenericArrayLocationCriterion) c;
      }
    }
    return null;
  }

  /**
   * Returns a RelativeCriterion if this has one, else null.
   *
   * @return a RelativeCriterion if this has one, else null
   */
  public RelativeLocation getCastRelativeLocation() {
    RelativeLocation result = null;
    for (Criterion c : criteria.values()) {
      if (c.getKind() == Criterion.Kind.CAST) {
        result = ((CastCriterion) c).getLocation();
      }
    }
    return result;
  }

  // Returns the last one. Should really return the outermost one.
  // However, there should not be more than one unless all are equivalent.
  /**
   * Returns an InClassCriterion if this has one, else null.
   *
   * @return an InClassCriterion if this has one, else null
   */
  public InClassCriterion getInClass() {
    InClassCriterion result = null;
    for (Criterion c : criteria.values()) {
      if (c.getKind() == Criterion.Kind.IN_CLASS) {
        result = (InClassCriterion) c;
      }
    }
    return result;
  }

  /**
   * Returns true if this is on the zeroth bound of a type.
   *
   * @return true if this is on the zeroth bound of a type
   */
  // Used when determining whether an annotation is on an implicit upper
  // bound (the "extends Object" that is customarily omitted).
  public boolean onBoundZero() {
    for (Criterion c : criteria.values()) {
      switch (c.getKind()) {
        case CLASS_BOUND:
          if (((ClassBoundCriterion) c).boundLoc.boundIndex != 0) {
            break;
          }
          return true;
        case METHOD_BOUND:
          if (((MethodBoundCriterion) c).boundLoc.boundIndex != 0) {
            break;
          }
          return true;
        case AST_PATH:
          ASTPath astPath = ((ASTPathCriterion) c).astPath;
          if (!astPath.isEmpty()) {
            ASTPath.ASTEntry entry = astPath.getLast();
            if (entry.childSelectorIs(ASTPath.BOUND) && entry.getArgument() == 0) {
              return true;
            }
          }
          break;
        default:
          break;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return criteria.toString();
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Factory methods
  //

  /**
   * Creates an "is" criterion: that a program element has the specified kind and name.
   *
   * @param kind the program element's kind
   * @param name the program element's name
   * @return an "is" criterion
   */
  public static final Criterion is(Tree.Kind kind, String name) {
    return new IsCriterion(kind, name);
  }

  /**
   * Creates an "enclosed by" criterion: that a program element is enclosed by the specified kind of
   * program element.
   *
   * @param kind the kind of enclosing program element
   * @return an "enclosed by" criterion
   */
  public static final Criterion enclosedBy(Tree.Kind kind) {
    return new EnclosedByCriterion(kind);
  }

  /**
   * Creates an "in package" criterion: that a program element is enclosed by the specified package.
   *
   * @param name the name of the enclosing package
   * @return an "in package" criterion
   */
  public static final Criterion inPackage(String name) {
    return new InPackageCriterion(name);
  }

  /**
   * Creates an "in class" criterion: that a program element is enclosed by the specified class.
   *
   * @param name the name of the enclosing class
   * @param exactMatch if true, match only in the class itself, not in its inner classes
   * @return an "in class" criterion
   */
  // TODO: Should `name` be `@BinaryName`??
  public static final Criterion inClass(@ClassGetName String name, boolean exactMatch) {
    return new InClassCriterion(name, /* exactMatch= */ true);
  }

  /**
   * Creates an "in method" criterion: that a program element is enclosed by the specified method.
   *
   * @param name the name of the enclosing method
   * @return an "in method" criterion
   */
  public static final Criterion inMethod(String name) {
    return new InMethodCriterion(name);
  }

  /**
   * Creates a "not in method" criterion: that a program element is not enclosed by any method.
   *
   * @return a "not in method" criterion
   */
  public static final Criterion notInMethod() {
    return new NotInMethodCriterion();
  }

  public static final Criterion packageDecl(String packageName) {
    return new PackageCriterion(packageName);
  }

  public static final Criterion atLocation() {
    return new GenericArrayLocationCriterion();
  }

  /**
   * Creates a GenericArrayLocationCriterion for a location.
   *
   * @param loc location of the generic array
   * @return a GenericArrayLocationCriterion for the given location
   */
  public static final Criterion atLocation(TypePath loc) {
    return new GenericArrayLocationCriterion(loc);
  }

  /**
   * Creates a GenericArrayLocationCriterion for a field.
   *
   * @param varName location of the field
   * @return a GenericArrayLocationCriterion for the given field
   */
  @Deprecated
  @InlineMe(
      replacement = "new FieldCriterion(varName)",
      imports = "org.checkerframework.afu.annotator.find.FieldCriterion")
  public static final Criterion field(String varName) {
    return new FieldCriterion(varName);
  }

  public static final Criterion field(String varName, boolean isOnDeclaration) {
    return new FieldCriterion(varName, isOnDeclaration);
  }

  public static final Criterion inStaticInit(int blockID) {
    return new InInitBlockCriterion(blockID, true);
  }

  public static final Criterion inInstanceInit(int blockID) {
    return new InInitBlockCriterion(blockID, false);
  }

  public static final Criterion inFieldInit(String varName) {
    return new InFieldInitCriterion(varName);
  }

  public static final Criterion receiver(String methodName) {
    return new ReceiverCriterion(methodName);
  }

  /**
   * Returns a ReturnTypeCriterion.
   *
   * @param className the class name
   * @param methodName the method name
   * @return a new ReturnTypeCriterion
   */
  public static final Criterion returnType(@ClassGetName String className, String methodName) {
    return new ReturnTypeCriterion(className, methodName);
  }

  /**
   * Creates an IsSigMethodCriterion.
   *
   * @param methodName the method name
   * @return a new IsSigMethodCriterion
   */
  @SuppressWarnings("signature:argument") // likely bug; value used as both a method & a signature
  public static final Criterion isSigMethod(String methodName) {
    return new IsSigMethodCriterion(methodName);
  }

  public static final Criterion param(String methodName, Integer pos) {
    return new ParamCriterion(methodName, pos);
  }

  //  public final static Criterion param(String methodName, Integer pos, InnerTypeLocation loc) {
  //    return new ParamCriterion(methodName, pos, loc);
  //  }

  public static final Criterion local(String methodName, LocalLocation loc) {
    return new LocalVariableCriterion(methodName, loc);
  }

  public static final Criterion cast(String methodName, RelativeLocation loc) {
    return new CastCriterion(methodName, loc);
  }

  public static final Criterion newObject(String methodName, RelativeLocation loc) {
    return new NewCriterion(methodName, loc);
  }

  public static final Criterion instanceOf(String methodName, RelativeLocation loc) {
    return new InstanceOfCriterion(methodName, loc);
  }

  public static Criterion memberReference(String methodName, RelativeLocation loc) {
    return new MemberReferenceCriterion(methodName, loc);
  }

  public static Criterion methodCall(String methodName, RelativeLocation loc) {
    return new CallCriterion(methodName, loc);
  }

  public static final Criterion typeArgument(String methodName, RelativeLocation loc) {
    return new TypeArgumentCriterion(methodName, loc);
  }

  public static final Criterion lambda(String methodName, RelativeLocation loc) {
    return new LambdaCriterion(methodName, loc);
  }

  public static final Criterion atBoundLocation(BoundLocation loc) {
    return new BoundLocationCriterion(loc);
  }

  public static final Criterion atExtImplsLocation(String className, TypeIndexLocation loc) {
    return new ExtImplsLocationCriterion(className, loc);
  }

  public static final Criterion methodBound(String methodName, BoundLocation boundLoc) {
    return new MethodBoundCriterion(methodName, boundLoc);
  }

  public static final Criterion classBound(String className, BoundLocation boundLoc) {
    return new ClassBoundCriterion(className, boundLoc);
  }

  public static final Criterion astPath(ASTPath astPath) {
    return new ASTPathCriterion(astPath);
  }
}
