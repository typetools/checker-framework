package org.checkerframework.dataflow.cfg.builder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.BlockImpl;
import org.checkerframework.dataflow.cfg.block.ConditionalBlockImpl;
import org.checkerframework.dataflow.cfg.block.ExceptionBlockImpl;
import org.checkerframework.dataflow.cfg.block.RegularBlockImpl;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlockImpl;
import org.checkerframework.dataflow.cfg.block.SpecialBlock.SpecialBlockType;
import org.checkerframework.dataflow.cfg.block.SpecialBlockImpl;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.util.MostlySingleton;

/** Class that performs phase two of the translation process. */
@SuppressWarnings("nullness") // TODO
public class CFGTranslationPhaseTwo {

  private CFGTranslationPhaseTwo() {}

  /**
   * Perform phase two of the translation.
   *
   * @param in the result of phase one
   * @return a control flow graph that might still contain degenerate basic block (such as empty
   *     regular basic blocks or conditional blocks with the same block as 'then' and 'else'
   *     successor)
   */
  @SuppressWarnings("interning:not.interned") // AST node comparisons
  public static ControlFlowGraph process(PhaseOneResult in) {

    Map<Label, Integer> bindings = in.bindings;
    ArrayList<ExtendedNode> nodeList = in.nodeList;
    // A leader is an extended node which will give rise to a basic block in phase two.
    Set<Integer> leaders = in.leaders;

    assert !in.nodeList.isEmpty();

    // exit blocks
    SpecialBlockImpl regularExitBlock = new SpecialBlockImpl(SpecialBlockType.EXIT);
    SpecialBlockImpl exceptionalExitBlock = new SpecialBlockImpl(SpecialBlockType.EXCEPTIONAL_EXIT);

    // record missing edges that will be added later
    Set<MissingEdge> missingEdges = new MostlySingleton<>();

    // missing exceptional edges
    Set<MissingEdge> missingExceptionalEdges = new LinkedHashSet<>();

    // create start block
    SpecialBlockImpl startBlock = new SpecialBlockImpl(SpecialBlockType.ENTRY);
    missingEdges.add(new MissingEdge(startBlock, 0));

    // Loop through all 'leaders' (while dynamically detecting the leaders).
    @NonNull RegularBlockImpl block = new RegularBlockImpl(); // block being processed/built
    int i = 0;
    for (ExtendedNode node : nodeList) {
      switch (node.getType()) {
        case NODE:
          if (leaders.contains(i)) {
            RegularBlockImpl b = new RegularBlockImpl();
            block.setSuccessor(b);
            block = b;
          }
          block.addNode(node.getNode());
          node.setBlock(block);

          // does this node end the execution (modeled as an edge to
          // the exceptional exit block)
          boolean terminatesExecution = node.getTerminatesExecution();
          if (terminatesExecution) {
            block.setSuccessor(exceptionalExitBlock);
            block = new RegularBlockImpl();
          }
          break;
        case CONDITIONAL_JUMP:
          {
            ConditionalJump cj = (ConditionalJump) node;
            // Exception nodes may fall through to conditional jumps, so we set the block which is
            // required for the insertion of missing edges.
            node.setBlock(block);
            assert block != null;
            final ConditionalBlockImpl cb = new ConditionalBlockImpl();
            if (cj.getTrueFlowRule() != null) {
              cb.setThenFlowRule(cj.getTrueFlowRule());
            }
            if (cj.getFalseFlowRule() != null) {
              cb.setElseFlowRule(cj.getFalseFlowRule());
            }
            block.setSuccessor(cb);
            block = new RegularBlockImpl();

            // use two anonymous SingleSuccessorBlockImpl that set the
            // 'then' and 'else' successor of the conditional block
            final Label thenLabel = cj.getThenLabel();
            final Label elseLabel = cj.getElseLabel();
            Integer target = bindings.get(thenLabel);
            assert target != null;
            missingEdges.add(
                new MissingEdge(
                    new RegularBlockImpl() {
                      @Override
                      public void setSuccessor(BlockImpl successor) {
                        cb.setThenSuccessor(successor);
                      }
                    },
                    target));
            target = bindings.get(elseLabel);
            assert target != null;
            missingEdges.add(
                new MissingEdge(
                    new RegularBlockImpl() {
                      @Override
                      public void setSuccessor(BlockImpl successor) {
                        cb.setElseSuccessor(successor);
                      }
                    },
                    target));
            break;
          }
        case UNCONDITIONAL_JUMP:
          UnconditionalJump uj = (UnconditionalJump) node;
          if (leaders.contains(i)) {
            RegularBlockImpl b = new RegularBlockImpl();
            block.setSuccessor(b);
            block = b;
          }
          node.setBlock(block);
          if (node.getLabel() == in.regularExitLabel) {
            block.setSuccessor(regularExitBlock);
            block.setFlowRule(uj.getFlowRule());
          } else if (node.getLabel() == in.exceptionalExitLabel) {
            block.setSuccessor(exceptionalExitBlock);
            block.setFlowRule(uj.getFlowRule());
          } else {
            int target = bindings.get(node.getLabel());
            missingEdges.add(new MissingEdge(block, target, uj.getFlowRule()));
          }
          block = new RegularBlockImpl();
          break;
        case EXCEPTION_NODE:
          NodeWithExceptionsHolder en = (NodeWithExceptionsHolder) node;
          // create new exception block and link with previous block
          ExceptionBlockImpl e = new ExceptionBlockImpl();
          Node nn = en.getNode();
          e.setNode(nn);
          node.setBlock(e);
          block.setSuccessor(e);
          block = new RegularBlockImpl();

          // Ensure linking between e and next block (normal edge).
          // Note: do not link to the next block for throw statements (these throw exceptions for
          // sure).
          if (!node.getTerminatesExecution()) {
            missingEdges.add(new MissingEdge(e, i + 1));
          }

          // exceptional edges
          for (Map.Entry<TypeMirror, Set<Label>> entry : en.getExceptions().entrySet()) {
            TypeMirror cause = entry.getKey();
            for (Label label : entry.getValue()) {
              Integer target = bindings.get(label);
              // TODO: This is sometimes null; is this a problem?
              // assert target != null;
              missingExceptionalEdges.add(new MissingEdge(e, target, cause));
            }
          }
          break;
      }
      i++;
    }

    // add missing edges
    for (MissingEdge p : missingEdges) {
      Integer index = p.index;
      assert index != null : "CFGBuilder: problem in CFG construction " + p.source;
      ExtendedNode extendedNode = nodeList.get(index);
      BlockImpl target = extendedNode.getBlock();
      SingleSuccessorBlockImpl source = p.source;
      source.setSuccessor(target);
      if (p.flowRule != null) {
        source.setFlowRule(p.flowRule);
      }
    }

    // add missing exceptional edges
    for (MissingEdge p : missingExceptionalEdges) {
      Integer index = p.index;
      TypeMirror cause = p.cause;
      ExceptionBlockImpl source = (ExceptionBlockImpl) p.source;
      if (index == null) {
        // edge to exceptional exit
        source.addExceptionalSuccessor(exceptionalExitBlock, cause);
      } else {
        // edge to specific target
        ExtendedNode extendedNode = nodeList.get(index);
        BlockImpl target = extendedNode.getBlock();
        source.addExceptionalSuccessor(target, cause);
      }
    }

    return new ControlFlowGraph(
        startBlock,
        regularExitBlock,
        exceptionalExitBlock,
        in.underlyingAST,
        in.treeLookupMap,
        in.convertedTreeLookupMap,
        in.unaryAssignNodeLookupMap,
        in.returnNodes,
        in.declaredClasses,
        in.declaredLambdas);
  }
}
