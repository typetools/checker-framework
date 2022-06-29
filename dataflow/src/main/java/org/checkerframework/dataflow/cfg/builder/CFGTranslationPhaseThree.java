package org.checkerframework.dataflow.cfg.builder;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.Block.BlockType;
import org.checkerframework.dataflow.cfg.block.BlockImpl;
import org.checkerframework.dataflow.cfg.block.ConditionalBlockImpl;
import org.checkerframework.dataflow.cfg.block.ExceptionBlockImpl;
import org.checkerframework.dataflow.cfg.block.RegularBlockImpl;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlockImpl;
import org.checkerframework.javacutil.BugInCF;

/* --------------------------------------------------------- */
/* Phase Three */
/* --------------------------------------------------------- */

/**
 * Class that performs phase three of the translation process. In particular, the following
 * degenerate cases of basic blocks are removed:
 *
 * <ol>
 *   <li>Empty regular basic blocks: These blocks will be removed and their predecessors linked
 *       directly to the successor.
 *   <li>Conditional basic blocks that have the same basic block as the 'then' and 'else' successor:
 *       The conditional basic block will be removed in this case.
 *   <li>Two consecutive, non-empty, regular basic blocks where the second block has exactly one
 *       predecessor (namely the other of the two blocks): In this case, the two blocks are merged.
 *   <li>Some basic blocks might not be reachable from the entryBlock. These basic blocks are
 *       removed, and the list of predecessors (in the doubly-linked structure of basic blocks) are
 *       adapted correctly.
 * </ol>
 *
 * Eliminating the second type of degenerate cases might introduce cases of the third problem. These
 * are also removed.
 */
public class CFGTranslationPhaseThree {

  /** A simple wrapper object that holds a basic block and allows to set one of its successors. */
  protected interface PredecessorHolder {
    void setSuccessor(BlockImpl b);

    BlockImpl getBlock();
  }

  /**
   * Perform phase three on the control flow graph {@code cfg}.
   *
   * @param cfg the control flow graph. Ownership is transfered to this method and the caller is not
   *     allowed to read or modify {@code cfg} after the call to {@code process} any more.
   * @return the resulting control flow graph
   */
  @SuppressWarnings("nullness") // TODO: successors
  public static ControlFlowGraph process(ControlFlowGraph cfg) {
    Set<Block> worklist = cfg.getAllBlocks();
    Set<Block> dontVisit = new HashSet<>();

    // note: this method has to be careful when relinking basic blocks
    // to not forget to adjust the predecessors, too

    // fix predecessor lists by removing any unreachable predecessors
    for (Block c : worklist) {
      BlockImpl cur = (BlockImpl) c;
      for (Block pred : new HashSet<>(cur.getPredecessors())) {
        if (!worklist.contains(pred)) {
          cur.removePredecessor((BlockImpl) pred);
        }
      }
    }

    // remove empty blocks
    for (Block cur : worklist) {
      if (dontVisit.contains(cur)) {
        continue;
      }

      if (cur.getType() == BlockType.REGULAR_BLOCK) {
        RegularBlockImpl b = (RegularBlockImpl) cur;
        if (b.isEmpty()) {
          Set<RegularBlockImpl> emptyBlocks = new HashSet<>();
          Set<PredecessorHolder> predecessors = new LinkedHashSet<>();
          BlockImpl succ = computeNeighborhoodOfEmptyBlock(b, emptyBlocks, predecessors);
          for (RegularBlockImpl e : emptyBlocks) {
            succ.removePredecessor(e);
            dontVisit.add(e);
          }
          for (PredecessorHolder p : predecessors) {
            BlockImpl block = p.getBlock();
            dontVisit.add(block);
            succ.removePredecessor(block);
            p.setSuccessor(succ);
          }
        }
      }
    }

    // remove useless conditional blocks
    /* Issue 3267 revealed that this is a dangerous optimization:
       it merges a block that evaluates one condition onto an unrelated following block,
       which can also be a condition. The then/else stores from the first block are still
       set, leading to incorrect results for the then/else stores in the following block.
       The correct result would be to merge the then/else stores from the previous block.
       However, as this is late in the CFG construction, I didn't see how to add e.g. a
       dummy variable declaration node in a dummy regular block, which would cause a merge.
       So for now, let's not perform this optimization.
       It would be interesting to know how large the impact of this optimization is.

    worklist = cfg.getAllBlocks();
    for (Block c : worklist) {
        BlockImpl cur = (BlockImpl) c;

        if (cur.getType() == BlockType.CONDITIONAL_BLOCK) {
            ConditionalBlockImpl cb = (ConditionalBlockImpl) cur;
            assert cb.getPredecessors().size() == 1;
            if (cb.getThenSuccessor() == cb.getElseSuccessor()) {
                BlockImpl pred = cb.getPredecessors().iterator().next();
                PredecessorHolder predecessorHolder = getPredecessorHolder(pred, cb);
                BlockImpl succ = (BlockImpl) cb.getThenSuccessor();
                succ.removePredecessor(cb);
                predecessorHolder.setSuccessor(succ);
            }
        }
    }
    */

    // merge consecutive basic blocks if possible
    worklist = cfg.getAllBlocks();
    for (Block cur : worklist) {
      if (cur.getType() == BlockType.REGULAR_BLOCK) {
        RegularBlockImpl b = (RegularBlockImpl) cur;
        Block succ = b.getRegularSuccessor();
        if (succ.getType() == BlockType.REGULAR_BLOCK) {
          RegularBlockImpl rs = (RegularBlockImpl) succ;
          if (rs.getPredecessors().size() == 1) {
            b.setSuccessor(rs.getRegularSuccessor());
            b.addNodes(rs.getNodes());
            rs.getRegularSuccessor().removePredecessor(rs);
          }
        }
      }
    }
    return cfg;
  }

  /**
   * Compute the set of empty regular basic blocks {@code emptyBlocks}, starting at {@code start}
   * and going both forward and backwards. Furthermore, compute the predecessors of these empty
   * blocks ({@code predecessors} ), and their single successor (return value).
   *
   * @param start the starting point of the search (an empty, regular basic block)
   * @param emptyBlocks a set to be filled by this method with all empty basic blocks found
   *     (including {@code start}).
   * @param predecessors a set to be filled by this method with all predecessors
   * @return the single successor of the set of the empty basic blocks
   */
  @SuppressWarnings({
    "interning:not.interned", // AST node comparisons
    "nullness" // successors
  })
  protected static BlockImpl computeNeighborhoodOfEmptyBlock(
      RegularBlockImpl start,
      Set<RegularBlockImpl> emptyBlocks,
      Set<PredecessorHolder> predecessors) {

    // get empty neighborhood that come before 'start'
    computeNeighborhoodOfEmptyBlockBackwards(start, emptyBlocks, predecessors);

    // go forward
    BlockImpl succ = (BlockImpl) start.getSuccessor();
    while (succ.getType() == BlockType.REGULAR_BLOCK) {
      RegularBlockImpl cur = (RegularBlockImpl) succ;
      if (cur.isEmpty()) {
        computeNeighborhoodOfEmptyBlockBackwards(cur, emptyBlocks, predecessors);
        assert emptyBlocks.contains(cur) : "cur ought to be in emptyBlocks";
        succ = (BlockImpl) cur.getSuccessor();
        if (succ == cur) {
          // An infinite loop, making exit block unreachable
          break;
        }
      } else {
        break;
      }
    }
    return succ;
  }

  /**
   * Compute the set of empty regular basic blocks {@code emptyBlocks}, starting at {@code start}
   * and looking only backwards in the control flow graph. Furthermore, compute the predecessors of
   * these empty blocks ({@code predecessors}).
   *
   * @param start the starting point of the search (an empty, regular basic block)
   * @param emptyBlocks a set to be filled by this method with all empty basic blocks found
   *     (including {@code start}).
   * @param predecessors a set to be filled by this method with all predecessors
   */
  protected static void computeNeighborhoodOfEmptyBlockBackwards(
      RegularBlockImpl start,
      Set<RegularBlockImpl> emptyBlocks,
      Set<PredecessorHolder> predecessors) {

    RegularBlockImpl cur = start;
    emptyBlocks.add(cur);
    for (final Block p : cur.getPredecessors()) {
      BlockImpl pred = (BlockImpl) p;
      switch (pred.getType()) {
        case SPECIAL_BLOCK:
          // add pred correctly to predecessor list
          predecessors.add(getPredecessorHolder(pred, cur));
          break;
        case CONDITIONAL_BLOCK:
          // add pred correctly to predecessor list
          predecessors.add(getPredecessorHolder(pred, cur));
          break;
        case EXCEPTION_BLOCK:
          // add pred correctly to predecessor list
          predecessors.add(getPredecessorHolder(pred, cur));
          break;
        case REGULAR_BLOCK:
          RegularBlockImpl r = (RegularBlockImpl) pred;
          if (r.isEmpty()) {
            // recursively look backwards
            if (!emptyBlocks.contains(r)) {
              computeNeighborhoodOfEmptyBlockBackwards(r, emptyBlocks, predecessors);
            }
          } else {
            // add pred correctly to predecessor list
            predecessors.add(getPredecessorHolder(pred, cur));
          }
          break;
      }
    }
  }

  /**
   * Return a predecessor holder that can be used to set the successor of {@code pred} in the place
   * where previously the edge pointed to {@code cur}. Additionally, the predecessor holder also
   * takes care of unlinking (i.e., removing the {@code pred} from {@code cur's} predecessors).
   *
   * @param pred a block whose successor should be set
   * @param cur the previous successor of {@code pred}
   * @return a predecessor holder to set the successor of {@code pred}
   */
  @SuppressWarnings("interning:not.interned") // AST node comparisons
  protected static PredecessorHolder getPredecessorHolder(
      final BlockImpl pred, final BlockImpl cur) {
    switch (pred.getType()) {
      case SPECIAL_BLOCK:
        SingleSuccessorBlockImpl s = (SingleSuccessorBlockImpl) pred;
        return singleSuccessorHolder(s, cur);
      case CONDITIONAL_BLOCK:
        // add pred correctly to predecessor list
        final ConditionalBlockImpl c = (ConditionalBlockImpl) pred;
        if (c.getThenSuccessor() == cur) {
          return new PredecessorHolder() {
            @Override
            public void setSuccessor(BlockImpl b) {
              c.setThenSuccessor(b);
              cur.removePredecessor(pred);
            }

            @Override
            public BlockImpl getBlock() {
              return c;
            }
          };
        } else {
          assert c.getElseSuccessor() == cur;
          return new PredecessorHolder() {
            @Override
            public void setSuccessor(BlockImpl b) {
              c.setElseSuccessor(b);
              cur.removePredecessor(pred);
            }

            @Override
            public BlockImpl getBlock() {
              return c;
            }
          };
        }
      case EXCEPTION_BLOCK:
        // add pred correctly to predecessor list
        final ExceptionBlockImpl e = (ExceptionBlockImpl) pred;
        if (e.getSuccessor() == cur) {
          return singleSuccessorHolder(e, cur);
        } else {
          @SuppressWarnings("keyfor:assignment") // ignore keyfor type
          Set<Map.Entry<TypeMirror, Set<Block>>> entrySet = e.getExceptionalSuccessors().entrySet();
          for (final Map.Entry<TypeMirror, Set<Block>> entry : entrySet) {
            if (entry.getValue().contains(cur)) {
              return new PredecessorHolder() {
                @Override
                public void setSuccessor(BlockImpl b) {
                  e.addExceptionalSuccessor(b, entry.getKey());
                  cur.removePredecessor(pred);
                }

                @Override
                public BlockImpl getBlock() {
                  return e;
                }
              };
            }
          }
        }
        throw new BugInCF("Unreachable");
      case REGULAR_BLOCK:
        RegularBlockImpl r = (RegularBlockImpl) pred;
        return singleSuccessorHolder(r, cur);
      default:
        throw new BugInCF("Unexpected block type " + pred.getType());
    }
  }

  /**
   * Returns a {@link PredecessorHolder} that sets the successor of a single successor block {@code
   * s}.
   *
   * @return a {@link PredecessorHolder} that sets the successor of a single successor block {@code
   *     s}
   */
  protected static PredecessorHolder singleSuccessorHolder(
      final SingleSuccessorBlockImpl s, final BlockImpl old) {
    return new PredecessorHolder() {
      @Override
      public void setSuccessor(BlockImpl b) {
        s.setSuccessor(b);
        old.removePredecessor(s);
      }

      @Override
      public BlockImpl getBlock() {
        return s;
      }
    };
  }
}
