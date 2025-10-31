package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.collectionownership.qual.CollectionFieldDestructor;
import org.checkerframework.checker.collectionownership.qual.NotOwningCollection;
import org.checkerframework.checker.collectionownership.qual.OwningCollection;
import org.checkerframework.checker.collectionownership.qual.OwningCollectionBottom;
import org.checkerframework.checker.collectionownership.qual.OwningCollectionWithoutObligation;
import org.checkerframework.checker.collectionownership.qual.PolyOwningCollection;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.checker.resourceleak.MustCallInference;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory.PotentiallyFulfillingLoop;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionParseException;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/** The annotated type factory for the Collection Ownership Checker. */
public class CollectionOwnershipAnnotatedTypeFactory
    extends GenericAnnotatedTypeFactory<
        CFValue,
        CollectionOwnershipStore,
        CollectionOwnershipTransfer,
        CollectionOwnershipAnalysis> {

  /**
   * The {@code @} {@link MustCallAnnotatedTypeFactory} instance in the checker hierarchy. Used for
   * getting the {@code @MustCall} type of expressions.
   */
  private final MustCallAnnotatedTypeFactory mcAtf;

  /** The {@code @}{@link NotOwningCollection} annotation. */
  public final AnnotationMirror TOP;

  /** The {@code @}{@link NotOwningCollection} annotation. Equals TOP. */
  public final AnnotationMirror NOTOWNINGCOLLECTION;

  /** The {@code @}{@link OwningCollection} annotation. */
  public final AnnotationMirror OWNINGCOLLECTION;

  /** The {@code @}{@link OwningCollectionWithoutObligation} annotation. */
  public final AnnotationMirror OWNINGCOLLECTIONWITHOUTOBLIGATION;

  /**
   * The {@code @}{@link OwningCollectionBottom}{@code ()} annotation. It is the default in
   * unannotated code.
   */
  public final AnnotationMirror BOTTOM;

  /** The {@code @}{@link PolyOwningCollection}{@code ()} polymorphic annotation. */
  public final AnnotationMirror POLY;

  /** The value element of the {@code @}{@link CollectionFieldDestructor} annotation. */
  private final ExecutableElement collectionFieldDestructorValueElement =
      TreeUtils.getMethod(CollectionFieldDestructor.class, "value", 0, processingEnv);

  /**
   * Enum for the types in the hierarchy. Combined with a few utility methods to get the right enum
   * value from various sources, this is a convenient interface to deal with annotations in this
   * hierarchy.
   */
  public enum CollectionOwnershipType {
    /** The @NotOwningCollection type. */
    NotOwningCollection,
    /** The @OwningCollection type. */
    OwningCollection,
    /** The @OwningCollectionWithoutObligation type. */
    OwningCollectionWithoutObligation,
    /** The @OwningCollectionBottom type. */
    OwningCollectionBottom
  };

  /**
   * The method name used for CollectionObligations that represent an obligation of MustCallUnkown.
   * The digit in the first character ensures this cannot coincide with an actual method name.
   */
  public static final String UNKNOWN_METHOD_NAME = "1UNKNOWN";

  /**
   * Maps the AST-tree corresponding to the loop condition of a collection-obligation-fulfilling
   * loop to the loop wrapper.
   */
  private static Map<Tree, PotentiallyFulfillingLoop> conditionToFulfillingLoopMap =
      new HashMap<>();

  /**
   * Maps the cfg-block corresponding to the loop conditional block of a
   * collection-obligation-fulfilling loop to the loop wrapper.
   */
  private static Map<Block, PotentiallyFulfillingLoop> conditionalBlockToFulfillingLoopMap =
      new HashMap<>();

  /**
   * Marks the specified loop as fulfilling a collection obligation.
   *
   * @param loop the loop wrapper
   */
  public static void markFulfillingLoop(PotentiallyFulfillingLoop loop) {
    conditionToFulfillingLoopMap.put(loop.condition, loop);
    conditionalBlockToFulfillingLoopMap.put(loop.loopConditionalBlock, loop);
  }

  /**
   * Returns the collection-obligation-fulfilling loop for which the given tree is the condition.
   *
   * @param tree a tree that is potentially the condition for a fulfilling loop
   * @return the collection-obligation-fulfilling loop for which the given tree is the condition
   */
  public static PotentiallyFulfillingLoop getFulfillingLoopForCondition(Tree tree) {
    return conditionToFulfillingLoopMap.get(tree);
  }

  /**
   * Returns the collection-obligation-fulfilling loop for which the given block is the CFG
   * conditional block.
   *
   * @param block the block that is potentially the conditional block for a fulfilling loop
   * @return the collection-obligation-fulfilling loop for which the given block is the CFG
   *     conditional block
   */
  public static PotentiallyFulfillingLoop getFulfillingLoopForConditionalBlock(Block block) {
    return conditionalBlockToFulfillingLoopMap.get(block);
  }

  /**
   * Creates a CollectionOwnershipAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  @SuppressWarnings("this-escape")
  public CollectionOwnershipAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    NOTOWNINGCOLLECTION = AnnotationBuilder.fromClass(elements, NotOwningCollection.class);
    TOP = NOTOWNINGCOLLECTION;
    OWNINGCOLLECTION = AnnotationBuilder.fromClass(elements, OwningCollection.class);
    OWNINGCOLLECTIONWITHOUTOBLIGATION =
        AnnotationBuilder.fromClass(elements, OwningCollectionWithoutObligation.class);
    BOTTOM = AnnotationBuilder.fromClass(elements, OwningCollectionBottom.class);
    POLY = AnnotationBuilder.fromClass(elements, PolyOwningCollection.class);
    mcAtf = ResourceLeakUtils.getMustCallAnnotatedTypeFactory(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            PolyOwningCollection.class,
            NotOwningCollection.class,
            OwningCollection.class,
            OwningCollectionWithoutObligation.class,
            OwningCollectionBottom.class));
  }

  /**
   * Fetches the store from the results of dataflow for {@code firstBlock}. If {@code
   * afterFirstStore} is true, then the store after {@code firstBlock} is returned; if {@code
   * afterFirstStore} is false, the store before {@code succBlock} is returned.
   *
   * @param afterFirstStore if true, use the store after the first block or the store before its
   *     successor, succBlock
   * @param firstBlock a block
   * @param succBlock {@code firstBlock}'s successor
   * @return the appropriate CollectionOwnershipStore, populated with MustCall annotations, from the
   *     results of running dataflow
   */
  public CollectionOwnershipStore getStoreForBlock(
      boolean afterFirstStore, Block firstBlock, Block succBlock) {
    return afterFirstStore
        ? flowResult.getStoreAfter(firstBlock)
        : flowResult.getStoreBefore(succBlock);
  }

  // Note that this method is overridden here because the collections ownership
  // typechecker runs last in the RLC, not because this has anything to do with
  // collections. Whatever checker runs last in the RLC must do this. TODO: make this
  // run last in a more sensible way.
  @Override
  public void postAnalyze(ControlFlowGraph cfg) {
    ResourceLeakChecker rlc = ResourceLeakUtils.getResourceLeakChecker(this);
    rlc.setRoot(root);
    MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer =
        new MustCallConsistencyAnalyzer(rlc, false);
    mustCallConsistencyAnalyzer.analyze(cfg);
    RLCCalledMethodsAnnotatedTypeFactory cmAtf =
        (RLCCalledMethodsAnnotatedTypeFactory)
            ResourceLeakUtils.getRLCCalledMethodsChecker(this).getTypeFactory();
    // Inferring owning annotations for @Owning fields/parameters, @EnsuresCalledMethods for
    // finalizer methods, and @InheritableMustCall annotations for the class declarations.
    if (cmAtf.getWholeProgramInference() != null) {
      if (cfg.getUnderlyingAST().getKind() == UnderlyingAST.Kind.METHOD) {
        MustCallInference.runMustCallInference(cmAtf, cfg, mustCallConsistencyAnalyzer);
      }
    }

    super.postAnalyze(cfg);
  }

  /**
   * Returns true if the given type is a resource collection: a type assignable from {@code
   * Collection} whose single type var has non-empty MustCall type.
   *
   * <p>This overload should be used only before computation of AnnotatedTypeMirrors is completed,
   * in particular in addComputedTypeAnnotations(AnnotatedTypeMirror).
   *
   * @param t the AnnotatedTypeMirror
   * @return true if t is a resource collection
   */
  public boolean isResourceCollection(TypeMirror t) {
    List<String> mcValues = getMustCallValuesOfResourceCollectionComponent(t);
    return mcValues != null && mcValues.size() > 0;
  }

  /**
   * Returns true if the given element is a resource collection field that is declared as
   * {@code @OwningCollection}. Since that is the default, this method also returns true if the
   * field has no collection ownership annotation.
   *
   * @param elt a field that might be a resource collection
   * @return true if the element is a resource collection field that is {@code @OwningCollection} by
   *     declaration
   */
  public boolean isOwningCollectionField(Element elt) {
    if (elt == null) {
      return false;
    }
    if (elt.getKind().isField()) {
      if (isResourceCollection(elt.asType())) {
        AnnotatedTypeMirror atm = getAnnotatedType(elt);
        CollectionOwnershipType fieldType =
            getCoType(Collections.singletonList(atm.getPrimaryAnnotationInHierarchy(TOP)));
        if (fieldType == null) {
          return false;
        }
        switch (fieldType) {
          case OwningCollection:
          case OwningCollectionWithoutObligation:
            return true;
          default:
            return false;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if the given element is a resource collection field.
   *
   * @param elt an element
   * @return true if the element is a resource collection field
   */
  public boolean isResourceCollectionField(Element elt) {
    if (elt.getKind().isField()) {
      if (isResourceCollection(elt.asType())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the given element is a resource collection parameter that is declared as
   * {@code @OwningCollection}. Since that is the default, this method also returns true if the
   * parameter has no collection ownership annotation.
   *
   * @param elt an element
   * @return true if the element is a resource collection parameter that is
   *     {@code @OwningCollection} by declaration
   */
  public boolean isOwningCollectionParameter(Element elt) {
    if (isResourceCollection(elt.asType())) {
      if (elt.getKind() == ElementKind.PARAMETER) {
        AnnotatedTypeMirror atm = getAnnotatedType(elt);
        CollectionOwnershipType paramType =
            getCoType(Collections.singletonList(atm.getPrimaryAnnotationInHierarchy(TOP)));
        if (paramType == null) {
          return false;
        }
        switch (paramType) {
          case OwningCollection:
            return true;
          default:
            return false;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if the given AST tree is a resource collection.
   *
   * <p>That is, whether the given tree is of a type assignable from java.util.Collection, whose
   * only type var has non-empty MustCall type.
   *
   * @param tree the tree
   * @return true if the tree is a resource collection
   */
  public boolean isResourceCollection(Tree tree) {
    if (tree == null) {
      return false;
    }
    AnnotatedTypeMirror treeMcType;
    try {
      treeMcType = mcAtf.getAnnotatedType(tree);
    } catch (BugInCF e) {
      // this happens if the tree is not of a supported format, thrown by
      // AnnotatedTypeFactory#getAnnotatedType
      treeMcType = null;
    }
    List<String> mcValues = getMustCallValuesOfResourceCollectionComponent(treeMcType);
    return mcValues != null && mcValues.size() > 0;
  }

  /**
   * If the given type is a collection, this method returns the MustCall values of its elements or
   * null if there are none or if the given type is not a collection.
   *
   * <p>That is, if the given type is a Java.util.Collection implementation, this method returns the
   * MustCall values of its type variable upper bound if there are any or else null.
   *
   * @param atm the AnnotatedTypeMirror
   * @return if the given type is a collection, returns the MustCall values of its elements or null
   *     if there are none or if the given type is not a collection
   */
  public List<String> getMustCallValuesOfResourceCollectionComponent(AnnotatedTypeMirror atm) {
    if (atm == null) {
      return null;
    }
    boolean isCollectionType = ResourceLeakUtils.isCollection(atm.getUnderlyingType());

    AnnotatedTypeMirror componentType = null;
    if (isCollectionType) {
      List<? extends AnnotatedTypeMirror> typeArgs =
          ((AnnotatedDeclaredType) atm).getTypeArguments();
      if (typeArgs.size() == 1) {
        componentType = typeArgs.get(0);
      }
    }

    if (componentType != null) {
      List<String> list = ResourceLeakUtils.getMcValues(componentType, mcAtf);
      return list;
    } else {
      return null;
    }
  }

  /**
   * If the given tree represents a collection, this method returns the MustCall values of its
   * elements. It returns null if there are none or if the given type is not a collection.
   *
   * <p>That is, if the given tree is of a Java.util.Collection implementation, this method returns
   * the MustCall values of its type variable upper bound if there are any or else null.
   *
   * @param tree the AST tree
   * @return if the given tree represents a collection, returns the MustCall values of its elements
   *     or null if there are none or if the given type is not a collection
   */
  public List<String> getMustCallValuesOfResourceCollectionComponent(Tree tree) {
    return getMustCallValuesOfResourceCollectionComponent(mcAtf.getAnnotatedType(tree));
  }

  /**
   * If the given type is a collection, this method returns the MustCall values of its elements. It
   * returns null if there are none or if the given type is not a collection.
   *
   * <p>That is, if the given type is a Java.util.Collection implementation, this method returns the
   * MustCall values of its type variable upper bound if there are any or else null.
   *
   * @param t the TypeMirror
   * @return if the given type is a collection, returns the MustCall values of its elements or null
   *     if there are none or if the given type is not a collection
   */
  public List<String> getMustCallValuesOfResourceCollectionComponent(TypeMirror t) {
    boolean isCollectionType = ResourceLeakUtils.isCollection(t);

    TypeMirror componentType = null;
    if (isCollectionType) {
      List<? extends TypeMirror> typeArgs = ((DeclaredType) t).getTypeArguments();
      if (typeArgs.size() == 1) {
        componentType = typeArgs.get(0);
      }
    }

    if (componentType != null) {
      List<String> list = ResourceLeakUtils.getMcValues(componentType, mcAtf);
      return list;
    } else {
      return null;
    }
  }

  /**
   * Returns the flow-sensitive {@code CollectionOwnershipType} that the given node has in the given
   * store.
   *
   * @param node the node
   * @param coStore the store
   * @return the {@code CollectionOwnershipType} that the given node has in the given store
   */
  public CollectionOwnershipType getCoType(Node node, CollectionOwnershipStore coStore) {
    JavaExpression jx = JavaExpression.fromNode(node);
    CFValue storeVal;
    try {
      storeVal = coStore.getValue(jx);
    } catch (BugInCF e) {
      storeVal = null;
    }
    if (storeVal == null) {
      return null;
    }
    return getCoType(storeVal.getAnnotations());
  }

  /**
   * Returns the flow-sensitive {@code CollectionOwnershipType} of the given tree.
   *
   * @param tree the tree
   * @return the {@code CollectionOwnershipType} that the given tree has
   */
  public CollectionOwnershipType getCoType(Tree tree) {
    JavaExpression jx = null;
    if (tree instanceof ExpressionTree) {
      jx = JavaExpression.fromTree((ExpressionTree) tree);
    } else if (tree instanceof VariableTree) {
      jx = JavaExpression.fromVariableTree((VariableTree) tree);
    }
    try {
      CollectionOwnershipStore coStore = getStoreBefore(tree);
      CFValue storeVal = coStore.getValue(jx);
      return getCoType(storeVal.getAnnotations());
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Utility method to extract the {@code CollectionOwnershipType} from a collection of {@code
   * AnnotationMirror}s.
   *
   * @param annos the {@code AnnotationMirror} collection
   * @return the extracted {@code CollectionOwnershipType} from annos
   */
  public CollectionOwnershipType getCoType(Collection<? extends AnnotationMirror> annos) {
    for (AnnotationMirror anm : annos) {
      if (anm == null) {
        continue;
      }
      if (AnnotationUtils.areSame(anm, NOTOWNINGCOLLECTION)) {
        return CollectionOwnershipType.NotOwningCollection;
      } else if (AnnotationUtils.areSame(anm, OWNINGCOLLECTION)) {
        return CollectionOwnershipType.OwningCollection;
      } else if (AnnotationUtils.areSame(anm, OWNINGCOLLECTIONWITHOUTOBLIGATION)) {
        return CollectionOwnershipType.OwningCollectionWithoutObligation;
      } else if (AnnotationUtils.areSame(anm, BOTTOM)) {
        return CollectionOwnershipType.OwningCollectionBottom;
      }
    }
    return null;
  }

  /**
   * Returns the field names in the {@code @CollectionFieldDestructor} annotation that the given
   * method has or an empty list if there is no such annotation.
   *
   * @param method the method
   * @return the field names in the method's {@code @CollectionFieldDestructor} annotation, or an
   *     empty list if there is no such annotation
   */
  public List<String> getCollectionFieldDestructorAnnoFields(ExecutableElement method) {
    AnnotationMirror collectionFieldDestructorAnno =
        getDeclAnnotation(method, CollectionFieldDestructor.class);
    if (collectionFieldDestructorAnno == null) {
      return new ArrayList<String>();
    }
    return AnnotationUtils.getElementValueArray(
        collectionFieldDestructorAnno, collectionFieldDestructorValueElement, String.class);
  }

  /**
   * Returnst true if the given expression {@code e} refers to {@code this.field}.
   *
   * @param e the expression
   * @param field the field
   * @return true if {@code e} refers to {@code this.field}
   */
  public boolean expressionIsFieldAccess(String e, VariableElement field) {
    try {
      JavaExpression je = StringToJavaExpression.atFieldDecl(e, field, this.checker);
      return je instanceof FieldAccess && ((FieldAccess) je).getField().equals(field);
    } catch (JavaExpressionParseException ex) {
      // The parsing error will be reported elsewhere, assuming e was derived from an
      // annotation.
      return false;
    }
  }

  /**
   * Returns a JavaExpression for the given String. Returns null if string is not parsable as a Java
   * expression.
   *
   * @param s the string
   * @param method the method with the annotation
   * @return a JavaExpression for the given String, or null if the string is not parsable as a Java
   *     expression
   */
  public JavaExpression stringToJavaExpression(String s, ExecutableElement method) {
    Tree methodTree = declarationFromElement(method);
    if (methodTree instanceof MethodTree) {
      try {
        return StringToJavaExpression.atMethodBody(s, (MethodTree) methodTree, checker);
      } catch (JavaExpressionParseException ex) {
        return null;
      }
    }
    return null;
  }

  @Override
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(
        super.createTypeAnnotator(), new CollectionOwnershipTypeAnnotator(this));
  }

  /**
   * The TypeAnnotator for the Collection Ownership type system.
   *
   * <p>This TypeAnnotator defaults resource collection return types to OwningCollection, and
   * resource collection parameters to NotOwningCollection.
   */
  private class CollectionOwnershipTypeAnnotator extends TypeAnnotator {

    /**
     * Creates a CollectionOwnershipTypeAnnotator.
     *
     * @param atypeFactory the type factory
     */
    public CollectionOwnershipTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitExecutable(AnnotatedExecutableType t, Void p) {

      AnnotatedDeclaredType receiverType = t.getReceiverType();
      AnnotationMirror receiverAnno =
          receiverType == null ? null : receiverType.getEffectiveAnnotationInHierarchy(TOP);
      boolean receiverHasExplicitAnno =
          receiverAnno != null && !AnnotationUtils.areSameByName(BOTTOM, receiverAnno);

      AnnotatedTypeMirror returnType = t.getReturnType();
      AnnotationMirror returnAnno = returnType.getEffectiveAnnotationInHierarchy(TOP);
      boolean returnHasExplicitAnno =
          returnAnno != null && !AnnotationUtils.areSameByName(BOTTOM, returnAnno);

      // inherit supertype annotations

      Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
          AnnotatedTypes.overriddenMethods(
              elements, CollectionOwnershipAnnotatedTypeFactory.this, t.getElement());

      if (overriddenMethods != null) {
        for (ExecutableElement superElt : overriddenMethods.values()) {
          AnnotatedExecutableType annotatedSuperMethod =
              CollectionOwnershipAnnotatedTypeFactory.this.getAnnotatedType(superElt);

          if (!receiverHasExplicitAnno) {
            AnnotatedDeclaredType superReceiver = annotatedSuperMethod.getReceiverType();
            AnnotationMirror superReceiverAnno = superReceiver.getPrimaryAnnotationInHierarchy(TOP);
            boolean superReceiverHasExplicitAnno =
                superReceiverAnno != null
                    && !AnnotationUtils.areSameByName(BOTTOM, superReceiverAnno)
                    && !AnnotationUtils.areSameByName(POLY, superReceiverAnno);
            if (superReceiverHasExplicitAnno) {
              receiverType.replaceAnnotation(superReceiverAnno);
            }
          }

          if (!returnHasExplicitAnno) {
            AnnotatedTypeMirror superReturnType = annotatedSuperMethod.getReturnType();
            AnnotationMirror superReturnAnno = superReturnType.getPrimaryAnnotationInHierarchy(TOP);
            boolean superReturnHasExplicitAnno =
                superReturnAnno != null
                    && !AnnotationUtils.areSameByName(BOTTOM, superReturnAnno)
                    && !AnnotationUtils.areSameByName(POLY, superReturnAnno);
            if (superReturnHasExplicitAnno) {
              returnType.replaceAnnotation(superReturnAnno);
            }
          }

          List<? extends AnnotatedTypeMirror> paramTypes = t.getParameterTypes();
          List<? extends AnnotatedTypeMirror> superParamTypes =
              annotatedSuperMethod.getParameterTypes();
          for (int i = 0; i < superParamTypes.size(); i++) {
            AnnotationMirror paramAnno = paramTypes.get(i).getEffectiveAnnotationInHierarchy(TOP);
            boolean paramHasExplicitAnno =
                paramAnno != null && !AnnotationUtils.areSameByName(BOTTOM, paramAnno);
            if (!paramHasExplicitAnno) {
              AnnotationMirror superParamAnno =
                  superParamTypes.get(i).getPrimaryAnnotationInHierarchy(TOP);
              boolean superParamHasExplicitAnno =
                  superParamAnno != null
                      && !AnnotationUtils.areSameByName(BOTTOM, superParamAnno)
                      && !AnnotationUtils.areSameByName(POLY, superParamAnno);
              if (superParamHasExplicitAnno) {
                paramTypes.get(i).replaceAnnotation(superParamAnno);
              }
            }
          }
        }
      } // end "if (overriddenMethods != null)"

      if (isResourceCollection(returnType.getUnderlyingType())) {
        AnnotationMirror manualAnno = returnType.getEffectiveAnnotationInHierarchy(TOP);
        if (manualAnno == null || AnnotationUtils.areSameByName(BOTTOM, manualAnno)) {
          boolean isConstructor = t.getElement().getKind() == ElementKind.CONSTRUCTOR;
          if (isConstructor) {
            returnType.replaceAnnotation(OWNINGCOLLECTIONWITHOUTOBLIGATION);
          } else {
            returnType.replaceAnnotation(OWNINGCOLLECTION);
          }
        }
      }

      for (AnnotatedTypeMirror paramType : t.getParameterTypes()) {
        if (isResourceCollection(paramType.getUnderlyingType())) {
          AnnotationMirror manualAnno = paramType.getEffectiveAnnotationInHierarchy(TOP);
          if (manualAnno == null || AnnotationUtils.areSameByName(BOTTOM, manualAnno)) {
            paramType.replaceAnnotation(NOTOWNINGCOLLECTION);
          }
        }
      }

      return super.visitExecutable(t, p);
    }
  }

  /*
   * Defaults resource collection field uses within member methods to @OwningCollection.
   */
  @Override
  protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
    super.addComputedTypeAnnotations(tree, type, iUseFlow);

    if (type.getKind() == TypeKind.DECLARED) {
      Element elt = TreeUtils.elementFromTree(tree);
      if (elt != null) {
        boolean isField = elt.getKind() == ElementKind.FIELD;
        if (isField && isResourceCollection(type.getUnderlyingType())) {
          AnnotationMirror fieldAnno = type.getEffectiveAnnotationInHierarchy(TOP);
          if (fieldAnno == null || AnnotationUtils.areSameByName(BOTTOM, fieldAnno)) {
            TreePath currentPath = getPath(tree);
            MethodTree enclosingMethodTree = TreePathUtil.enclosingMethod(currentPath);
            if (enclosingMethodTree != null && TreeUtils.isConstructor(enclosingMethodTree)) {
              type.replaceAnnotation(OWNINGCOLLECTIONWITHOUTOBLIGATION);
            } else {
              type.replaceAnnotation(OWNINGCOLLECTION);
            }
          }
        }
      }
    }
  }

  // Default resource collection fields to @OwningCollection and resource collection parameters
  // to @NotOwningCollection (inside the method).
  //
  // A resource collection is a `Iterable` or `Iterator`, whose component has non-empty
  // @MustCall type, as defined by the predicate isResourceCollection(AnnotatedTypeMirror).
  @Override
  public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
    super.addComputedTypeAnnotations(elt, type);

    if (elt instanceof VariableElement) {
      if (isResourceCollection(type.getUnderlyingType())) {
        if (elt.getKind() == ElementKind.FIELD) {
          AnnotationMirror fieldAnno = type.getEffectiveAnnotationInHierarchy(TOP);
          if (fieldAnno == null || AnnotationUtils.areSameByName(BOTTOM, fieldAnno)) {
            type.replaceAnnotation(OWNINGCOLLECTION);
          }
        } else if (elt.getKind() == ElementKind.PARAMETER) {
          // Propagate the parameter annotation to the use site.
          ExecutableElement method = (ExecutableElement) elt.getEnclosingElement();
          AnnotatedExecutableType annotatedMethod =
              CollectionOwnershipAnnotatedTypeFactory.this.getAnnotatedType(method);
          List<? extends VariableElement> params = method.getParameters();
          List<? extends AnnotatedTypeMirror> paramTypes = annotatedMethod.getParameterTypes();
          for (int i = 0; i < params.size(); i++) {
            if (params.get(i).getSimpleName() == elt.getSimpleName()) {
              type.replaceAnnotation(paramTypes.get(i).getEffectiveAnnotationInHierarchy(TOP));
              break;
            }
          }
        }
      }
    }
  }
}
