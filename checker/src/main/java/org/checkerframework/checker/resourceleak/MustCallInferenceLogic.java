package org.checkerframework.checker.resourceleak;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.Tree;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.BlockImpl;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class contains Resource Leak Checker annotation inference algorithm. For now it just
 * contains inference logic for @Owning annotations on final owning fields. It adds @Owning
 * annotation on a field if it finds a method that satisfies @MustCall obligation of the field along
 * some path to the regular exit point.
 */
public class MustCallInferenceLogic {

  /**
   * The type factory for the Resource Leak Checker, which is used to access the Must Call Checker.
   */
  private final ResourceLeakAnnotatedTypeFactory typeFactory;
  /** The @Owning annotation. */
  protected final AnnotationMirror OWNING;

  /**
   * Creates a MustCallInferenceLogic. If the Resource Leak Checker has infer option, the type
   * factory's postProcessClassTree method would instantiate a new MustCallInferenceLogic using this
   * constructor and then call {@link #inference(ControlFlowGraph, Tree)}.
   *
   * @param typeFactory the type factory
   */
  MustCallInferenceLogic(ResourceLeakAnnotatedTypeFactory typeFactory) {
    this.typeFactory = typeFactory;
    OWNING = AnnotationBuilder.fromClass(this.typeFactory.getElementUtils(), Owning.class);
  }

  /**
   * This is the main function in the MustCallInferenceLogic. It tracks called methods for fields
   * with non-empty @MustCall obligation of type {@link FieldWithCMVs} along all paths to regular
   * exit point in the method body.
   *
   * @param cfg the control flow graph of the method to check
   * @param methodTree the method to check
   */
  void inference(ControlFlowGraph cfg, Tree methodTree) {
    Set<BlockWithFieldsFacts> visited = new HashSet<>();
    Deque<BlockWithFieldsFacts> worklist = new ArrayDeque<>();

    Set<FieldWithCMVs> initFieldsWithCMVs = new HashSet<>();

    for (Element field : typeFactory.getFieldToFinalizers().keySet()) {
      initFieldsWithCMVs.add(new FieldWithCMVs(field, new HashSet<>()));
    }

    BlockWithFieldsFacts entry =
        new BlockWithFieldsFacts(cfg.getEntryBlock(), ImmutableSet.copyOf(initFieldsWithCMVs));
    worklist.add(entry);
    visited.add(entry);

    while (!worklist.isEmpty()) {
      BlockWithFieldsFacts current = worklist.remove();

      Set<FieldWithCMVs> fieldsWithCMVs = new LinkedHashSet<>(current.fieldsWithCMV);

      for (Node node : current.block.getNodes()) {
        if (node instanceof MethodInvocationNode) {
          updateFieldsWithCMVsForInvocation((MethodInvocationNode) node, fieldsWithCMVs);
        }
      }

      propagateRegPaths(current, fieldsWithCMVs, visited, worklist, methodTree);
    }
  }

  /**
   * Updates a set of called methods in FieldWithCMVs with a method invocation.
   *
   * @param mNode the MethodInvocationNode
   * @param fieldsWithCMVs the set of FieldWithCMVs
   */
  private void updateFieldsWithCMVsForInvocation(
      MethodInvocationNode mNode, Set<FieldWithCMVs> fieldsWithCMVs) {
    Node receiver = mNode.getTarget().getReceiver();
    if (receiver.getTree() != null) {
      Element receiverEl = TreeUtils.elementFromTree(receiver.getTree());
      if (typeFactory.getFieldToFinalizers().keySet().contains(receiverEl)) {
        Element method = TreeUtils.elementFromTree(mNode.getTree());
        FieldWithCMVs fieldsWithCMV = getFieldsWithCMVs(receiverEl, fieldsWithCMVs);
        if (fieldsWithCMV != null) {
          Set<String> calledMethodsnew =
              FluentIterable.from(fieldsWithCMV.calledMethods)
                  .append(method.getSimpleName().toString())
                  .toSet();
          fieldsWithCMVs.remove(fieldsWithCMV);
          fieldsWithCMVs.add(new FieldWithCMVs(receiverEl, calledMethodsnew));
        }
      }
    }
  }

  /**
   * Gets the FieldsWithCMVs whose field is equal to the elt, if one exists in {@code
   * fieldsWithCMVs}.
   *
   * @param fieldsWithCMVs set of FieldWithCMVs
   * @param elt the field element
   * @return the FieldWithCMVs in {@code fieldsWithCMVs} whose field is equal to {@code elt}, or
   *     {@code null} if there is no such FieldWithCMVs
   */
  private FieldWithCMVs getFieldsWithCMVs(Element elt, Set<FieldWithCMVs> fieldsWithCMVs) {
    for (FieldWithCMVs fieldsWithCMV : fieldsWithCMVs) {
      if (fieldsWithCMV.field.equals(elt)) {
        return fieldsWithCMV;
      }
    }
    return null;
  }

  /**
   * Propagates a set of FieldWithCMVs to successors, and searches for owning fields at the regular
   * exit point.
   *
   * @param curBlock the current block
   * @param fieldsWithCMVs the set of FieldWithCMVs for the current block
   * @param visited block-FieldWithCMVs pairs already on the worklist
   * @param worklist current worklist
   * @param methodTree the method tree
   */
  private void propagateRegPaths(
      BlockWithFieldsFacts curBlock,
      Set<FieldWithCMVs> fieldsWithCMVs,
      Set<BlockWithFieldsFacts> visited,
      Deque<BlockWithFieldsFacts> worklist,
      Tree methodTree) {
    List<BlockImpl> successors = getSuccessors((BlockImpl) curBlock.block);
    for (Block b : successors) {
      if (b.getType() == Block.BlockType.SPECIAL_BLOCK) {
        findOwningFields(fieldsWithCMVs, methodTree);
      }
      BlockWithFieldsFacts successor = new BlockWithFieldsFacts(b, fieldsWithCMVs);
      if (visited.add(successor)) {
        worklist.add(successor);
      }
    }
  }

  /**
   * Checks all element in the fieldsWithCMVs and adds @Owning annotation on a field if the called
   * methods set of that field in {@link FieldWithCMVs} includes all the methods in the @MustCall
   * type of the field. It also updates fieldToFinalizers with the new detected finalizer.
   *
   * @param fieldsWithCMVs the set of FieldWithCMVs
   * @param methodTree the method tree
   */
  private void findOwningFields(Set<FieldWithCMVs> fieldsWithCMVs, Tree methodTree) {
    for (FieldWithCMVs fieldsWithCMV : fieldsWithCMVs) {
      List<String> mcValues = typeFactory.getMustCallValue(fieldsWithCMV.field);
      if (fieldsWithCMV.calledMethods.containsAll(mcValues)) {
        WholeProgramInference wpi = typeFactory.getWholeProgramInference();
        if (wpi != null) {
          wpi.addFieldDeclarationAnnotation(fieldsWithCMV.field, OWNING);
          typeFactory.addFinalizerForField(
              fieldsWithCMV.field, TreeUtils.elementFromTree(methodTree));
        }
      }
    }
  }

  /**
   * Returns the non-exceptional successors of the current block.
   *
   * @param cur the current block
   * @return the successors of this current block
   */
  private List<BlockImpl> getSuccessors(BlockImpl cur) {
    List<BlockImpl> successorBlock = new ArrayList<>();

    if (cur.getType() == Block.BlockType.CONDITIONAL_BLOCK) {

      ConditionalBlock ccur = (ConditionalBlock) cur;

      successorBlock.add((BlockImpl) ccur.getThenSuccessor());
      successorBlock.add((BlockImpl) ccur.getElseSuccessor());

    } else {
      if (!(cur instanceof SingleSuccessorBlock)) {
        throw new BugInCF("BlockImpl is neither a conditional block nor a SingleSuccessorBlock");
      }

      Block b = ((SingleSuccessorBlock) cur).getSuccessor();
      if (b != null) {
        successorBlock.add((BlockImpl) b);
      }
    }
    return successorBlock;
  }

  /**
   * A pair of a {@link Block} and a set of FieldsWithCMV on entry to the block. Each FieldsWithCMV
   * represents a set of fields with a least upper bound of methods that were called somewhere in
   * the previous blocks.
   */
  private static class BlockWithFieldsFacts {

    /** The block. */
    public final Block block;

    /** The set of FieldsWithCMV. */
    public final ImmutableSet<FieldWithCMVs> fieldsWithCMV;

    /**
     * Create a new BlockWithFieldsFacts.
     *
     * @param b the block
     * @param fieldsWithCMVs the set of FieldWithCMVs
     */
    public BlockWithFieldsFacts(Block b, Set<FieldWithCMVs> fieldsWithCMVs) {
      this.block = b;
      this.fieldsWithCMV = ImmutableSet.copyOf(fieldsWithCMVs);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BlockWithFieldsFacts that = (BlockWithFieldsFacts) o;
      return block.equals(that.block) && fieldsWithCMV.equals(that.fieldsWithCMV);
    }

    @Override
    public int hashCode() {
      return Objects.hash(block, fieldsWithCMV);
    }
  }

  /**
   * A pair of a field {@link Element} and a set of method names that were called on the field
   * somewhere in a method body.
   */
  private static class FieldWithCMVs {

    /** The field. */
    public final Element field;

    /** The set of method names that were called on the field. */
    public ImmutableSet<String> calledMethods;

    /**
     * Creates a new FieldWithCMVs.
     *
     * @param fieldElt the field element
     * @param calledMethodsVals the field element
     */
    public FieldWithCMVs(Element fieldElt, Set<String> calledMethodsVals) {
      this.field = fieldElt;
      this.calledMethods = ImmutableSet.copyOf(calledMethodsVals);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      FieldWithCMVs that = (FieldWithCMVs) obj;
      return field.equals(that.field) && this.calledMethods.equals(that.calledMethods);
    }

    @Override
    public int hashCode() {
      return Objects.hash(calledMethods);
    }
  }
}
