package cfgconstruction;

import com.sun.source.tree.TryTree;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.MarkerNode;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

public class CFGConstruction {

  /** Do not instantiate. */
  private CFGConstruction() {
    throw new Error("Do not instantiate");
  }

  public static void main(String[] args) {

    String inputFile = "Test.java";
    String clazz = "Test";

    ControlFlowGraph cfg =
        CFGVisualizeLauncher.generateMethodCFG(
            inputFile, "manyNestedTryFinallyBlocks", clazz, /* analysis= */ null);
    cfg.checkInvariants();

    // The CFG builder translates the finally block several times, but the CFG lists each lambda
    // and class declaration in it once, so that each is analyzed once.
    cfg =
        CFGVisualizeLauncher.generateMethodCFG(
            inputFile, "lambdaAndClassInFinallyBlock", clazz, /* analysis= */ null);
    cfg.checkInvariants();
    if (cfg.getDeclaredLambdas().size() != 1) {
      throw new Error("Expected 1 declared lambda, found " + cfg.getDeclaredLambdas());
    }
    if (cfg.getDeclaredClasses().size() != 1) {
      throw new Error("Expected 1 declared class, found " + cfg.getDeclaredClasses());
    }

    // Before the budget for extra exceptional copies of finally blocks is used up, each exception
    // type gets its own copy, because each goes to a different catch block.
    cfg =
        CFGVisualizeLauncher.generateMethodCFG(
            inputFile, "finallyCopiesBeforeBudgetIsUsedUp", clazz, /* analysis= */ null);
    cfg.checkInvariants();
    List<ExceptionBlock> copyEnds = exceptionalCopyEnds(cfg);
    if (copyEnds.size() != 3) {
      throw new Error("Expected 3 exceptional copies, found " + copyEnds.size());
    }
    checkRethrowsToDistinctHandlers(copyEnds);

    // After the budget is used up, the exception types share one copy, which rethrows each of them
    // to its own catch block.
    cfg =
        CFGVisualizeLauncher.generateMethodCFG(
            inputFile, "finallyCopyAfterBudgetIsUsedUp", clazz, /* analysis= */ null);
    cfg.checkInvariants();
    copyEnds = exceptionalCopyEnds(cfg);
    if (copyEnds.size() != 1) {
      throw new Error("Expected 1 shared exceptional copy, found " + copyEnds.size());
    }
    checkRethrowsToDistinctHandlers(copyEnds);
  }

  /**
   * Returns the blocks that end the exceptional copies of the finally block that assigns {@code
   * finished}. Each such block rethrows the exceptions that its copy handles.
   *
   * @param cfg a control flow graph
   * @return the blocks whose node is the marker at the end of an exceptional copy of the finally
   *     block that assigns {@code finished}
   */
  private static List<ExceptionBlock> exceptionalCopyEnds(ControlFlowGraph cfg) {
    List<ExceptionBlock> result = new ArrayList<>();
    for (Block block : cfg.getAllBlocks()) {
      if (block instanceof ExceptionBlock exceptionBlock
          && exceptionBlock.getNode() instanceof MarkerNode marker
          && marker.getMessage().startsWith("end of finally block for ")
          && marker.getTree() instanceof TryTree tryTree
          && tryTree.getFinallyBlock() != null
          && tryTree.getFinallyBlock().toString().contains("finished = true")) {
        result.add(exceptionBlock);
      }
    }
    return result;
  }

  /**
   * Checks that the given blocks together rethrow exactly IOException, RuntimeException, and
   * Error, each to a different catch block.
   *
   * @param copyEnds the blocks that end the exceptional copies of a finally block
   */
  private static void checkRethrowsToDistinctHandlers(List<ExceptionBlock> copyEnds) {
    Set<String> typeNames = new HashSet<>();
    Set<Block> handlers = new HashSet<>();
    int numEdges = 0;
    for (ExceptionBlock copyEnd : copyEnds) {
      for (Map.Entry<TypeMirror, Set<Block>> entry :
          copyEnd.getExceptionalSuccessors().entrySet()) {
        typeNames.add(entry.getKey().toString());
        if (entry.getValue().size() != 1) {
          throw new Error(
              "Expected 1 successor for " + entry.getKey() + ", found " + entry.getValue());
        }
        handlers.addAll(entry.getValue());
        numEdges++;
      }
    }
    Set<String> expectedTypeNames =
        Set.of("java.io.IOException", "java.lang.RuntimeException", "java.lang.Error");
    if (!typeNames.equals(expectedTypeNames) || numEdges != 3) {
      throw new Error("Expected rethrows of " + expectedTypeNames + ", found " + typeNames);
    }
    if (handlers.size() != 3) {
      throw new Error("Expected rethrows to 3 different catch blocks, found " + handlers);
    }
    for (Block handler : handlers) {
      if (handler.getType() == Block.BlockType.SPECIAL_BLOCK) {
        throw new Error("Expected rethrows to catch blocks, found " + handler);
      }
    }
  }
}
