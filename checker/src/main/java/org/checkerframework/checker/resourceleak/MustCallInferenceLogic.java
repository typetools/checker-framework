package org.checkerframework.checker.resourceleak;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.common.wholeprograminference.WholeProgramInference;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class contains Resource Leak Checker annotation inference algorithm. For now, it just
 * contains inference logic for owning annotations on final owning fields. It adds an @Owning
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
    OWNING = AnnotationBuilder.fromClass(this.typeFactory.getElementUtils(), Owning.class);
  }

  /**
   * It checks method invocations for fields with non-empty @MustCall obligation along all paths to
   * the regular exit point in the method body.
   */
  void runInference() {
    Set<Block> visited = new HashSet<>();
    Deque<Block> worklist = new ArrayDeque<>();
    Block entry = this.cfg.getEntryBlock();

    worklist.add(entry);
    visited.add(entry);

    while (!worklist.isEmpty()) {
      Block current = worklist.remove();

      for (Node node : current.getNodes()) {
        if (node instanceof MethodInvocationNode) {
          checksOwningFieldForInvocation((MethodInvocationNode) node);
        }
      }

      propagateRegPaths(current, visited, worklist);
    }
  }

  /**
   * If the receiver of {@code mNode} is a possible owning field and the method invocation satisfies
   * the field's must call obligation, then adds owning annotation for that field.
   *
   * @param mNode the MethodInvocationNode
   */
  private void checkForMustCallInvocationOnField(MethodInvocationNode mNode) {
    Node receiver = mNode.getTarget().getReceiver();
    if (receiver.getTree() == null) {
      return;
    }

    Element receiverEl = TreeUtils.elementFromTree(receiver.getTree());

    if (typeFactory.isElementPossibleOwningField(receiverEl)) {
      Element method = TreeUtils.elementFromTree(mNode.getTree());
      List<String> mustCallValues = typeFactory.getMustCallValue(receiverEl);

      // Because we assumed that any must call annotation has at most one method, the following
      // check is enough to decide whether the receiver is an owning field
      if (mustCallValues.contains(method.getSimpleName().toString())) {
        WholeProgramInference wpi = typeFactory.getWholeProgramInference();
        // wpi can't be null in this class
        wpi.addFieldDeclarationAnnotation(receiverEl, OWNING);
      }
    }
  }

  /**
   * updates worklist with the next block along all paths to the regular exit point.
   *
   * @param curBlock the current block
   * @param visited set of blocks already on the worklist
   * @param worklist current worklist
   */
  private void propagateRegPaths(Block curBlock, Set<Block> visited, Deque<Block> worklist) {

    Set<Block> successors = curBlock.getSuccessors();

    for (Block b : successors) {
      if (b.getType() == Block.BlockType.SPECIAL_BLOCK) {

        if (((SpecialBlock) b).getSpecialType() == SpecialBlock.SpecialBlockType.EXCEPTIONAL_EXIT) {
          // Do nothing if the successor block is an exceptional exit point
          continue;
        }
      }

      if (visited.add(b)) {
        worklist.add(b);
      }
    }
  }
}
