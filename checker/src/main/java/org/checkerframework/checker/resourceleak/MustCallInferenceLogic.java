package org.checkerframework.checker.resourceleak;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class contains the Resource Leak Checker's annotation inference algorithm. For now, it just
 * contains inference logic for owning annotations on final owning fields. It adds an @Owning
 * annotation on a field if it finds a method that satisfies the @MustCall obligation of the field
 * along some path to the regular exit point.
 */
public class MustCallInferenceLogic {

  /** The set of owning fields. */
  private Set<Element> owningFields = new HashSet<>();

  /**
   * The type factory for the Resource Leak Checker, which is used to access the Must Call Checker.
   */
  private final ResourceLeakAnnotatedTypeFactory typeFactory;

  /** The {@link Owning} annotation. */
  protected final AnnotationMirror OWNING;

  /** The control flow graph. */
  private ControlFlowGraph cfg;

  /**
   * Creates a MustCallInferenceLogic. If the type factory has whole program inference enabled, its
   * postAnalyze method should instantiate a new MustCallInferenceLogic using this constructor and
   * then call {@link #runInference()}.
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
   * Runs the inference algorithm on the contents of the {@link #cfg} field.
   *
   * <p>Operationally, it checks method invocations for fields with non-empty @MustCall obligations
   * along all paths to the regular exit point in the method body of the method represented by
   * {@link #cfg}, and updates the {@link #owningFields} set if it discovers an owning field whose
   * must-call obligations were satisfied along one of the checked paths.
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
          checkForMustCallInvocationOnField((MethodInvocationNode) node);
        }
      }

      propagateRegPaths(current, visited, worklist);
    }
  }

  /**
   * If the receiver of {@code mNode} is a candidate owning field and the method invocation
   * satisfies the field's must-call obligation, then adds that field to the {@link #owningFields}
   * set.
   *
   * @param mNode the MethodInvocationNode
   */
  private void checkForMustCallInvocationOnField(MethodInvocationNode mNode) {
    Node receiver = mNode.getTarget().getReceiver();
    if (receiver.getTree() == null) {
      return;
    }

    Element receiverEl = TreeUtils.elementFromTree(receiver.getTree());

    if (receiverEl != null && typeFactory.isCandidateOwningField(receiverEl)) {
      Element method = TreeUtils.elementFromTree(mNode.getTree());
      List<String> mustCallValues = typeFactory.getMustCallValue(receiverEl);

      // This assumes that any MustCall annotation has at most one element.
      // TODO: generalize this to MustCall annotations with more than one element.
      if (mustCallValues.size() == 1
          && mustCallValues.contains(method.getSimpleName().toString())) {
        owningFields.add(receiverEl);
      }
    }
  }

  /**
   * Updates {@code worklist} with the next block along all paths to the regular exit point. If the
   * next block is a regular exit point, adds an {@literal @}Owning annotation for fields in {@link
   * #owningFields}.
   *
   * @param curBlock the current block
   * @param visited set of blocks already on the worklist
   * @param worklist current worklist
   */
  private void propagateRegPaths(Block curBlock, Set<Block> visited, Deque<Block> worklist) {

    List<Block> successors = getNormalSuccessors(curBlock);

    for (Block b : successors) {
      // If b is a special block, it must be the regular exit, since we do not propagate to
      // exceptional successors.
      if (b.getType() == Block.BlockType.SPECIAL_BLOCK) {
        WholeProgramInference wpi = typeFactory.getWholeProgramInference();
        assert wpi != null : "MustCallInference is running without WPI.";
        for (Element fieldElt : owningFields) {
          wpi.addFieldDeclarationAnnotation(fieldElt, OWNING);
        }
      }

      if (visited.add(b)) {
        worklist.add(b);
      }
    }
  }

  /**
   * Returns the non-exceptional successors of the current block.
   *
   * @param cur the current block
   * @return the successors of this current block
   */
  private List<Block> getNormalSuccessors(Block cur) {
    List<Block> successorBlock = new ArrayList<>();

    if (cur.getType() == Block.BlockType.CONDITIONAL_BLOCK) {

      ConditionalBlock ccur = (ConditionalBlock) cur;

      successorBlock.add(ccur.getThenSuccessor());
      successorBlock.add(ccur.getElseSuccessor());

    } else {
      if (!(cur instanceof SingleSuccessorBlock)) {
        throw new BugInCF("BlockImpl is neither a conditional block nor a SingleSuccessorBlock");
      }

      Block b = ((SingleSuccessorBlock) cur).getSuccessor();
      if (b != null) {
        successorBlock.add(b);
      }
    }
    return successorBlock;
  }
}
