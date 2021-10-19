package org.checkerframework.checker.resourceleak;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.Tree;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class contains Resource Leak Checker annotation inference algorithm. For now, it just
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

  /** The control flow graph. */
  private ControlFlowGraph cfg;

  /** The underlying AST for the method. */
  private UnderlyingAST.CFGMethod cfgMethod;

  /**
   * Creates a MustCallInferenceLogic. If the whole program inference is not null, the type
   * factory's postAnalyze method would instantiate a new MustCallInferenceLogic using this
   * constructor and then call {@link #inference()}.
   *
   * @param typeFactory the type factory
   * @param cfg the ControlFlowGraph
   */
  MustCallInferenceLogic(ResourceLeakAnnotatedTypeFactory typeFactory, ControlFlowGraph cfg) {
    this.typeFactory = typeFactory;
    this.cfg = cfg;
    this.cfgMethod = (UnderlyingAST.CFGMethod) cfg.getUnderlyingAST();
    OWNING = AnnotationBuilder.fromClass(this.typeFactory.getElementUtils(), Owning.class);
  }

  /**
   * It tracks called methods for fields with non-empty @MustCall obligation of type {@link
   * FieldWithCMVs} along all paths to the regular exit point in the method body. This is the main
   * function in the MustCallInferenceLogic.
   */
  void inference() {

    Set<BlockWithFieldsFacts> visited = new HashSet<>();
    Deque<BlockWithFieldsFacts> worklist = new ArrayDeque<>();
    BlockWithFieldsFacts entry =
        new BlockWithFieldsFacts(this.cfg.getEntryBlock(), new HashSet<>());
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

      propagateRegPaths(current, fieldsWithCMVs, visited, worklist);
    }
  }

  /**
   * If the receiver of {@code mNode} is a field of the containing class, updates {@code
   * fieldsWithCMVs} to include that field and the invoked method. Otherwise, does nothing.
   *
   * @param mNode the MethodInvocationNode
   * @param fieldsWithCMVs the set of FieldWithCMVs
   */
  private void updateFieldsWithCMVsForInvocation(
      MethodInvocationNode mNode, Set<FieldWithCMVs> fieldsWithCMVs) {
    Node receiver = mNode.getTarget().getReceiver();
    if (receiver.getTree() != null) {
      Element receiverEl = TreeUtils.elementFromTree(receiver.getTree());
      if (typeFactory.isElementPossibleOwningField(receiverEl)) {
        Element method = TreeUtils.elementFromTree(mNode.getTree());
        FieldWithCMVs fieldsWithCMV = getFieldsWithCMVs(receiverEl, fieldsWithCMVs);
        if (fieldsWithCMV != null) {
          Set<String> calledMethodsnew =
              FluentIterable.from(fieldsWithCMV.calledMethods)
                  .append(method.getSimpleName().toString())
                  .toSet();
          fieldsWithCMVs.remove(fieldsWithCMV);
          fieldsWithCMVs.add(new FieldWithCMVs(receiverEl, calledMethodsnew));
        } else {
          Set<String> calledMethods = new HashSet<>();
          calledMethods.add(method.getSimpleName().toString());
          fieldsWithCMVs.add(new FieldWithCMVs(receiverEl, calledMethods));
        }
      }
    }
  }

  /**
   * Gets the FieldsWithCMVs whose field is equal to the elt, if one exists in {@code
   * fieldsWithCMVs}, otherwise returns {@code null}.
   *
   * @param fieldsWithCMVs set of FieldWithCMVs
   * @param elt the field element
   * @return the FieldWithCMVs in {@code fieldsWithCMVs} whose field is equal to {@code elt}, or
   *     {@code null} if there is no such FieldWithCMVs
   */
  private @Nullable FieldWithCMVs getFieldsWithCMVs(
      Element elt, Set<FieldWithCMVs> fieldsWithCMVs) {
    for (FieldWithCMVs fieldsWithCMV : fieldsWithCMVs) {
      if (fieldsWithCMV.field.equals(elt)) {
        return fieldsWithCMV;
      }
    }
    return null;
  }

  /**
   * Propagates a set of FieldWithCMVs to successors. If the successor is a regular exit point, then
   * it searches for owning fields by looking at the called methods set stored in FieldWithCMVs and
   * the must call obligation of each field.
   *
   * @param curBlock the current block
   * @param fieldsWithCMVs the set of FieldWithCMVs for the current block
   * @param visited block-FieldWithCMVs pairs already on the worklist
   * @param worklist current worklist
   */
  private void propagateRegPaths(
      BlockWithFieldsFacts curBlock,
      Set<FieldWithCMVs> fieldsWithCMVs,
      Set<BlockWithFieldsFacts> visited,
      Deque<BlockWithFieldsFacts> worklist) {

    Set<Block> successors = curBlock.block.getSuccessors();

    for (Block b : successors) {
      if (b.getType() == Block.BlockType.SPECIAL_BLOCK) {

        if (((SpecialBlock) b).getSpecialType() == SpecialBlock.SpecialBlockType.EXCEPTIONAL_EXIT) {
          // Do nothing if the successor block is an exceptional exit point
          continue;
        }

        findOwningFields(fieldsWithCMVs, cfgMethod.getMethod());
      }

      BlockWithFieldsFacts successor = new BlockWithFieldsFacts(b, fieldsWithCMVs);
      if (visited.add(successor)) {
        worklist.add(successor);
      }
    }
  }

  /**
   * Checks all element in the fieldsWithCMVs and adds an {@code @Owning} annotation on a field if
   * the called methods set of that field in {@link FieldWithCMVs} includes all the methods in
   * the @MustCall type of the field. It also updates classToFieldToFinalizers with the new detected
   * finalizer.
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
          typeFactory.updateClassToFieldToFinalizers(
              cfgMethod.getClassTree(), fieldsWithCMV.field, TreeUtils.elementFromTree(methodTree));
        }
      }
    }
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
