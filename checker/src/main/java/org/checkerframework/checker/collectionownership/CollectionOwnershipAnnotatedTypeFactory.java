package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.collectionownership.qual.NotOwningCollection;
import org.checkerframework.checker.collectionownership.qual.OwningCollection;
import org.checkerframework.checker.collectionownership.qual.OwningCollectionBottom;
import org.checkerframework.checker.collectionownership.qual.OwningCollectionWithoutObligation;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.checker.resourceleak.MustCallInference;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory.PotentiallyFulfillingLoop;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/** The annotated type factory for the Collection Ownership Checker. */
public class CollectionOwnershipAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

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

  /**
   * Enum for the types in the hierarchy. Combined with a few utility methods to get the right enum
   * value from various sources, this is a convenient interface to deal with annotations in this
   * hierarchy.
   */
  public enum CollectionOwnershipType {
    /** the @NotOwningCollection type */
    NotOwningCollection,
    /** the @OwningCollection type */
    OwningCollection,
    /** the @OwningCollectionWithoutObligation type */
    OwningCollectionWithoutObligation,
    /** the @OwningCollectionBottom type */
    OwningCollectionBottom,
    /** if no type in the hierarchy can be determined with certainty */
    None
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
   * Marks the specified loop as fulfilling a collection obligation
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
   * @param tree the tree that is potentially the condition for a fulfilling loop
   * @return the collection-obligation-fulfilling loop for which the given tree is the condition
   */
  public static PotentiallyFulfillingLoop getFulfillingLoopForCondition(Tree tree) {
    return conditionToFulfillingLoopMap.get(tree);
  }

  /**
   * Returns the collection-obligation-fulfilling loop for which the given block is the conditional
   * block for.
   *
   * @param block the block that is potentially the conditional block for a fulfilling loop
   * @return the collection-obligation-fulfilling loop for which the given block is the conditional
   *     block for
   */
  public static PotentiallyFulfillingLoop getFulfillingLoopForConditionalBlock(Block block) {
    return conditionalBlockToFulfillingLoopMap.get(block);
  }

  /**
   * Creates a CollectionOwnershipAnnotatedTypeFactory.
   *
   * @param checker the checker associated with this type factory
   */
  public CollectionOwnershipAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    NOTOWNINGCOLLECTION = AnnotationBuilder.fromClass(elements, NotOwningCollection.class);
    TOP = NOTOWNINGCOLLECTION;
    OWNINGCOLLECTION = AnnotationBuilder.fromClass(elements, OwningCollection.class);
    OWNINGCOLLECTIONWITHOUTOBLIGATION =
        AnnotationBuilder.fromClass(elements, OwningCollectionWithoutObligation.class);
    BOTTOM = AnnotationBuilder.fromClass(elements, OwningCollectionBottom.class);
    this.postInit();
  }

  @Override
  public void setRoot(@Nullable CompilationUnitTree newRoot) {
    super.setRoot(newRoot);
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            NotOwningCollection.class,
            OwningCollection.class,
            OwningCollectionWithoutObligation.class,
            OwningCollectionBottom.class));
  }

  /**
   * Fetches the store from the results of dataflow for {@code first}. If {@code afterFirstStore} is
   * true, then the store after {@code first} is returned; if {@code afterFirstStore} is false, the
   * store before {@code succ} is returned.
   *
   * @param afterFirstStore whether to use the store after the first block or the store before its
   *     successor, succ
   * @param first a block
   * @param succ first's successor
   * @return the appropriate CFStore, populated with MustCall annotations, from the results of
   *     running dataflow
   */
  public CFStore getStoreForBlock(boolean afterFirstStore, Block first, Block succ) {
    return afterFirstStore ? flowResult.getStoreAfter(first) : flowResult.getStoreBefore(succ);
  }

  @Override
  public void postAnalyze(ControlFlowGraph cfg) {
    ResourceLeakChecker rlc = ResourceLeakUtils.getResourceLeakChecker(this);
    RLCCalledMethodsAnnotatedTypeFactory cmAtf =
        (RLCCalledMethodsAnnotatedTypeFactory)
            ResourceLeakUtils.getRLCCalledMethodsChecker(this).getTypeFactory();
    rlc.setRoot(root);
    MustCallConsistencyAnalyzer mustCallConsistencyAnalyzer = new MustCallConsistencyAnalyzer(rlc);
    mustCallConsistencyAnalyzer.analyze(cfg);
    // Inferring owning annotations for @Owning fields/parameters, @EnsuresCalledMethods for
    // finalizer methods and @InheritableMustCall annotations for the class declarations.
    if (cmAtf.getWholeProgramInference() != null) {
      if (cfg.getUnderlyingAST().getKind() == UnderlyingAST.Kind.METHOD) {
        MustCallInference.runMustCallInference(cmAtf, cfg, mustCallConsistencyAnalyzer);
      }
    }

    super.postAnalyze(cfg);
  }

  /**
   * Returns whether the given type is a resource collection.
   *
   * <p>That is, whether the given type is:
   *
   * <ol>
   *   <li>An array type, whose component has non-empty MustCall type.
   *   <li>A type assignable from java.util.Collection, whose only type var has non-empty MustCall
   *       type.
   * </ol>
   *
   * @param t the AnnotatedTypeMirror
   * @return whether t is a resource collection
   */
  public boolean isResourceCollection(TypeMirror t) {
    List<String> list = getMustCallValuesOfResourceCollectionComponent(t);
    return list != null && list.size() > 0;
  }

  /**
   * If the given type is a collection, this method returns the MustCall values of its elements or
   * null if there are none or if the given type is not a collection.
   *
   * <p>That is:
   *
   * <ol>
   *   <li>if the given type is an array type, this method returns the MustCall values of its
   *       component type if there are any or else null.
   *   <li>if the given type is a Java.util.Collection implementation, this method returns the
   *       MustCall values of its type variable upper bound if there are any or else null.
   * </ol>
   *
   * @param t the AnnotatedTypeMirror
   * @return if the given type is a collection, returns the MustCall values of its elements or null
   *     if there are none or if the given type is not a collection.
   */
  public List<String> getMustCallValuesOfResourceCollectionComponent(TypeMirror t) {
    boolean isCollectionType = ResourceLeakUtils.isCollection(t);
    boolean isArrayType = t.getKind() == TypeKind.ARRAY;

    TypeMirror componentType = null;
    if (isArrayType) {
      componentType = ((ArrayType) t).getComponentType();
    } else if (isCollectionType) {
      List<? extends TypeMirror> typeArgs = ((DeclaredType) t).getTypeArguments();
      if (typeArgs.size() != 0) {
        componentType = typeArgs.get(0);
      }
    }

    MustCallAnnotatedTypeFactory mcAtf = ResourceLeakUtils.getMustCallAnnotatedTypeFactory(this);

    if (componentType != null) {
      List<String> list = ResourceLeakUtils.getMcValues(componentType, mcAtf);
      return list;
    } else {
      return null;
    }
  }

  /**
   * Utility method to get the {@code CollectionOwnershipType} that the given value extracted from a
   * store has.
   *
   * @param val the store value type
   * @return the {@code CollectionOwnershipType} that the given value extracted from a store has
   */
  public CollectionOwnershipType getCoType(CFValue val) {
    return val == null ? CollectionOwnershipType.None : getCoType(val.getAnnotations());
  }

  /**
   * Utility method to extract the {@code CollectionOwnershipType} from a collection of {@code
   * AnnotationMirror}s.
   *
   * @param annos the {@code AnnotationMirror} collection
   * @return the extracted {@code CollectionOwnershipType} from annos
   */
  public CollectionOwnershipType getCoType(Collection<AnnotationMirror> annos) {
    if (annos == null) {
      return CollectionOwnershipType.None;
    }
    for (AnnotationMirror anm : annos) {
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
    return CollectionOwnershipType.None;
  }

  @Override
  protected TypeAnnotator createTypeAnnotator() {
    return new ListTypeAnnotator(
        super.createTypeAnnotator(), new CollectionOwnershipTypeAnnotator(this));
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(), new CollectionOwnershipTreeAnnotator(this));
  }

  /**
   * The TypeAnnotator for the Collection Ownership type system.
   *
   * <p>This TypeAnnotator defaults resource collection return types to OwningCollection, and
   * resource collection parameters to NotOwningCollection.
   */
  private class CollectionOwnershipTypeAnnotator extends TypeAnnotator {

    /**
     * Constructor matching super.
     *
     * @param atypeFactory the type factory
     */
    public CollectionOwnershipTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitExecutable(AnnotatedTypeMirror.AnnotatedExecutableType t, Void p) {
      AnnotatedTypeMirror returnType = t.getReturnType();

      if (isResourceCollection(returnType.getUnderlyingType())) {
        AnnotationMirror manualAnno = returnType.getEffectiveAnnotationInHierarchy(TOP);
        if (manualAnno == null || AnnotationUtils.areSameByName(BOTTOM, manualAnno)) {
          returnType.replaceAnnotation(
              CollectionOwnershipAnnotatedTypeFactory.this.OWNINGCOLLECTION);
        }
      }

      for (AnnotatedTypeMirror paramType : t.getParameterTypes()) {
        if (isResourceCollection(paramType.getUnderlyingType())) {
          AnnotationMirror manualAnno = paramType.getEffectiveAnnotationInHierarchy(TOP);
          if (manualAnno == null || AnnotationUtils.areSameByName(BOTTOM, manualAnno)) {
            paramType.replaceAnnotation(
                CollectionOwnershipAnnotatedTypeFactory.this.NOTOWNINGCOLLECTION);
          }
        }
      }

      return super.visitExecutable(t, p);
    }
  }

  /*
   * Default resource collection fields to @OwningCollection and resource collection parameters to
   * @NotOwningCollection (inside the method).
   *
   * Resource collections are either java.util.Collection's or arrays, whose component has
   * non-empty @MustCall type, as defined by the predicate isResourceCollection(AnnotatedTypeMirror).
   */
  @Override
  public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
    super.addComputedTypeAnnotations(elt, type);

    if (elt instanceof VariableElement) {
      boolean isField = elt.getKind() == ElementKind.FIELD;
      boolean isParam = elt.getKind() == ElementKind.PARAMETER;
      boolean isResourceCollection = isResourceCollection(type.getUnderlyingType());

      if (isResourceCollection) {
        if (isField) {
          AnnotationMirror fieldAnno = type.getEffectiveAnnotationInHierarchy(TOP);
          if (fieldAnno == null || AnnotationUtils.areSameByName(BOTTOM, fieldAnno)) {
            type.replaceAnnotation(OWNINGCOLLECTION);
          }
        } else if (isParam) {
          AnnotationMirror paramAnno = type.getEffectiveAnnotationInHierarchy(TOP);
          if (paramAnno == null || AnnotationUtils.areSameByName(BOTTOM, paramAnno)) {
            type.replaceAnnotation(NOTOWNINGCOLLECTION);
          }
        }
      }
    }
  }

  /**
   * The TreeAnnotator for the Collection Ownership type system.
   *
   * <p>This tree annotator treats newly allocated resource arrays (arrays, whose component type has
   * non-empty MustCall value) as @OwningCollection.
   */
  private class CollectionOwnershipTreeAnnotator extends TreeAnnotator {

    /**
     * Create a CollectionOwnershipTreeAnnotator.
     *
     * @param collectionOwnershipAtf the type factory
     */
    public CollectionOwnershipTreeAnnotator(
        CollectionOwnershipAnnotatedTypeFactory collectionOwnershipAtf) {
      super(collectionOwnershipAtf);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
      if (isResourceCollection(type.getUnderlyingType())) {
        type.replaceAnnotation(OWNINGCOLLECTION);
      }
      return super.visitNewArray(tree, type);
    }
  }
}

  // @Override
  // protected QualifierPolymorphism createQualifierPolymorphism() {
  //   return new MustCallQualifierPolymorphism(processingEnv, this);
  // }
