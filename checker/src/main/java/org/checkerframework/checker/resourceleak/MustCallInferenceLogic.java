package org.checkerframework.checker.resourceleak;

import static org.checkerframework.dataflow.cfg.block.Block.BlockType.SPECIAL_BLOCK;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.*;
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
import org.checkerframework.javacutil.*;

public class MustCallInferenceLogic {

  private final ResourceLeakAnnotatedTypeFactory typeFactory;

  protected final AnnotationMirror OWNING;

  MustCallInferenceLogic(ResourceLeakAnnotatedTypeFactory typeFactory) {
    this.typeFactory = typeFactory;
    OWNING = AnnotationBuilder.fromClass(this.typeFactory.getElementUtils(), Owning.class);
  }

  void inference(ControlFlowGraph cfg) {
    Set<BlockWithFields> visited = new HashSet<>();
    Deque<BlockWithFields> worklist = new ArrayDeque<>();

    Set<FieldsWithCMV> fieldsWithCMVSTemp = new HashSet<>();

    for (ResourceLeakAnnotatedTypeFactory.FieldToFinalizers fieldToFinalizers :
        typeFactory.getFieldToFinalizers()) {
      fieldsWithCMVSTemp.add(new FieldsWithCMV(fieldToFinalizers.field, new HashSet<>()));
    }

    BlockWithFields entry =
        new BlockWithFields(cfg.getEntryBlock(), ImmutableSet.copyOf(fieldsWithCMVSTemp));
    worklist.add(entry);
    visited.add(entry);

    while (!worklist.isEmpty()) {
      BlockWithFields current = worklist.remove();

      Set<FieldsWithCMV> fieldsWithCMVs = new LinkedHashSet<>(current.fieldsWithCMV);

      for (Node node : current.block.getNodes()) {
        if (node instanceof MethodInvocationNode) {
          MethodInvocationNode mNode = (MethodInvocationNode) node;
          Node receiver = mNode.getTarget().getReceiver();
          if (receiver.getTree() != null) {
            Element receiverEl = TreeUtils.elementFromTree(receiver.getTree());
            if (receiverEl.getKind().isField()
                && ElementUtils.isFinal(receiverEl)
                && !typeFactory.getMustCallValue(receiverEl).isEmpty()) {
              Element method = TreeUtils.elementFromTree(mNode.getTree());
              FieldsWithCMV fieldsWithCMV = getFieldsWithCMVs(receiverEl, fieldsWithCMVs);
              if (fieldsWithCMV != null) {
                Set<String> calledMethodsnew =
                    FluentIterable.from(fieldsWithCMV.calledMethods)
                        .append(method.getSimpleName().toString())
                        .toSet();
                fieldsWithCMVs.remove(fieldsWithCMV);
                fieldsWithCMVs.add(new FieldsWithCMV(receiverEl, calledMethodsnew));
              }
            }
          }
        }
      }

      propagateRegPaths(current, fieldsWithCMVs, visited, worklist);
    }
  }

  public FieldsWithCMV getFieldsWithCMVs(Element elt, Set<FieldsWithCMV> fieldsWithCMVs) {
    for (FieldsWithCMV fieldsWithCMV : fieldsWithCMVs) {
      if (fieldsWithCMV.field.equals(elt)) {
        return fieldsWithCMV;
      }
    }
    return null;
  }

  private void propagateRegPaths(
      BlockWithFields curBlock,
      Set<FieldsWithCMV> fieldsWithCMVs,
      Set<BlockWithFields> visited,
      Deque<BlockWithFields> worklist) {
    List<BlockImpl> successors = getSuccessors((BlockImpl) curBlock.block);
    for (Block b : successors) {
      if (b.getType() == SPECIAL_BLOCK) {
        for (FieldsWithCMV fieldsWithCMV : fieldsWithCMVs) {
          List<String> mcValues = typeFactory.getMustCallValue(fieldsWithCMV.field);
          if (fieldsWithCMV.calledMethods.containsAll(mcValues)) {
            WholeProgramInference wpi = typeFactory.getWholeProgramInference();
            if (wpi != null) {
              wpi.addFieldDeclarationAnnotation(fieldsWithCMV.field, OWNING);
            }
          }
        }
      }
      BlockWithFields successor = new BlockWithFields(b, fieldsWithCMVs);
      if (visited.add(successor)) {
        worklist.add(successor);
      }
    }
  }

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

  private static class BlockWithFields {

    /** The block. */
    public final Block block;

    /** The dataflow facts. */
    public final ImmutableSet<FieldsWithCMV> fieldsWithCMV;

    public BlockWithFields(Block b, Set<FieldsWithCMV> fieldsWithCMVs) {
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
      BlockWithFields that = (BlockWithFields) o;
      return block.equals(that.block) && fieldsWithCMV.equals(that.fieldsWithCMV);
    }

    @Override
    public int hashCode() {
      return Objects.hash(block, fieldsWithCMV);
    }
  }

  private static class FieldsWithCMV {
    public final Element field;

    public ImmutableSet<String> calledMethods;

    public FieldsWithCMV(Element f, Set<String> set) {
      this.field = f;
      this.calledMethods = ImmutableSet.copyOf(set);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      FieldsWithCMV that = (FieldsWithCMV) obj;
      return field.equals(that.field) && this.calledMethods.equals(that.calledMethods);
    }

    @Override
    public int hashCode() {
      return Objects.hash(calledMethods);
    }
  }
}
