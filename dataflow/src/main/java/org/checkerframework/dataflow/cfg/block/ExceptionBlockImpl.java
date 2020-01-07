package org.checkerframework.dataflow.cfg.block;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;

/** Base class of the {@link Block} implementation hierarchy. */
public class ExceptionBlockImpl extends SingleSuccessorBlockImpl implements ExceptionBlock {

    /** The node of this block. */
    protected @Nullable Node node;

    /** Set of exceptional successors. */
    protected final Map<TypeMirror, Set<Block>> exceptionalSuccessors;

    /** Create an empty exceptional block. */
    public ExceptionBlockImpl() {
        super(BlockType.EXCEPTION_BLOCK);
        exceptionalSuccessors = new HashMap<>();
    }

    /** Set the node. */
    public void setNode(Node c) {
        node = c;
        c.setBlock(this);
    }

    @Override
    public Node getNode() {
        if (node == null) {
            throw new BugInCF("Requested node for exception block before initialization");
        }
        return node;
    }

    /** Add an exceptional successor. */
    public void addExceptionalSuccessor(BlockImpl b, TypeMirror cause) {
        Set<Block> blocks = exceptionalSuccessors.get(cause);
        if (blocks == null) {
            blocks = new HashSet<>();
            exceptionalSuccessors.put(cause, blocks);
        }
        blocks.add(b);
        b.addPredecessor(this);
    }

    @Override
    public Map<TypeMirror, Set<Block>> getExceptionalSuccessors() {
        if (exceptionalSuccessors == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(exceptionalSuccessors);
    }

    @Override
    public String toString() {
        return "ExceptionBlock(" + node + ")";
    }
}
