package org.checkerframework.dataflow.analysis;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.dataflow.analysis.Store.FlowRule;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.RegularBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock.SpecialBlockType;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.javacutil.BugInCF;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * An implementation of a backward analysis to solve a org.checkerframework.dataflow problem given a
 * control flow graph and a backward transfer function.
 *
 * @param <V> the abstract value type to be tracked by the analysis
 * @param <S> the store type used in the analysis
 * @param <T> the transfer function type that is used to approximate runtime behavior
 */
public class BackwardAnalysisImpl<
                V extends AbstractValue<V>,
                S extends Store<S>,
                T extends BackwardTransferFunction<V, S>>
        extends AbstractAnalysis<V, S, T> implements BackwardAnalysis<V, S, T> {

    // TODO: Add widening support like what the forward analysis does.

    /** Out stores after every basic block (assumed to be 'no information' if not present). */
    protected final IdentityHashMap<Block, S> outStores;

    /**
     * Exception store of an exception block, propagated by exceptional successors of its exception
     * block, and merged with the normal {@link TransferResult}.
     */
    protected final IdentityHashMap<ExceptionBlock, S> exceptionStores;

    /** The store right before the entry block. */
    protected @Nullable S storeAtEntry;

    // `@code`, not `@link`, because dataflow module doesn't depend on framework module.
    /**
     * Construct an object that can perform a org.checkerframework.dataflow backward analysis over a
     * control flow graph. When using this constructor, the transfer function is set later by the
     * subclass, e.g., {@code org.checkerframework.framework.flow.CFAbstractAnalysis}.
     */
    public BackwardAnalysisImpl() {
        super(Direction.BACKWARD);
        this.outStores = new IdentityHashMap<>();
        this.exceptionStores = new IdentityHashMap<>();
        this.storeAtEntry = null;
    }

    /**
     * Construct an object that can perform a org.checkerframework.dataflow backward analysis over a
     * control flow graph given a transfer function.
     *
     * @param transfer the transfer function
     */
    public BackwardAnalysisImpl(@Nullable T transfer) {
        this();
        this.transferFunction = transfer;
    }

    @Override
    public void performAnalysis(ControlFlowGraph cfg) {
        if (isRunning) {
            throw new BugInCF(
                    "performAnalysis() shouldn't be called when the analysis is running.");
        }
        isRunning = true;
        try {
            init(cfg);
            while (!worklist.isEmpty()) {
                Block b = worklist.poll();
                performAnalysisBlock(b);
            }
        } finally {
            assert isRunning;
            // In case performAnalysisBlock crashed, reset isRunning to false.
            isRunning = false;
        }
    }

    @Override
    public void performAnalysisBlock(Block b) {
        switch (b.getType()) {
            case REGULAR_BLOCK:
                {
                    RegularBlock rb = (RegularBlock) b;
                    TransferInput<V, S> inputAfter = getInput(rb);
                    assert inputAfter != null : "@AssumeAssertion(nullness): invariant";
                    currentInput = inputAfter.copy();
                    Node firstNode = null;
                    boolean addToWorklistAgain = false;
                    List<Node> nodeList = rb.getNodes();
                    ListIterator<Node> reverseIter = nodeList.listIterator(nodeList.size());
                    while (reverseIter.hasPrevious()) {
                        Node node = reverseIter.previous();
                        assert currentInput != null : "@AssumeAssertion(nullness): invariant";
                        TransferResult<V, S> transferResult =
                                callTransferFunction(node, currentInput);
                        addToWorklistAgain |= updateNodeValues(node, transferResult);
                        currentInput = new TransferInput<>(node, this, transferResult);
                        firstNode = node;
                    }
                    // Propagate store to predecessors
                    for (Block pred : rb.getPredecessors()) {
                        assert currentInput != null : "@AssumeAssertion(nullness): invariant";
                        propagateStoresTo(
                                pred,
                                firstNode,
                                currentInput,
                                FlowRule.EACH_TO_EACH,
                                addToWorklistAgain);
                    }
                    break;
                }
            case EXCEPTION_BLOCK:
                {
                    ExceptionBlock eb = (ExceptionBlock) b;
                    TransferInput<V, S> inputAfter = getInput(eb);
                    assert inputAfter != null : "@AssumeAssertion(nullness): invariant";
                    currentInput = inputAfter.copy();
                    Node node = eb.getNode();
                    TransferResult<V, S> transferResult = callTransferFunction(node, currentInput);
                    boolean addToWorklistAgain = updateNodeValues(node, transferResult);
                    // Merge transferResult with exceptionStore if there exists one
                    S exceptionStore = exceptionStores.get(eb);
                    S mergedStore =
                            exceptionStore != null
                                    ? transferResult
                                            .getRegularStore()
                                            .leastUpperBound(exceptionStore)
                                    : transferResult.getRegularStore();
                    for (Block pred : eb.getPredecessors()) {
                        addStoreAfter(pred, node, mergedStore, addToWorklistAgain);
                    }
                    break;
                }
            case CONDITIONAL_BLOCK:
                {
                    ConditionalBlock cb = (ConditionalBlock) b;
                    TransferInput<V, S> inputAfter = getInput(cb);
                    assert inputAfter != null : "@AssumeAssertion(nullness): invariant";
                    TransferInput<V, S> input = inputAfter.copy();
                    for (Block pred : cb.getPredecessors()) {
                        propagateStoresTo(pred, null, input, FlowRule.EACH_TO_EACH, false);
                    }
                    break;
                }
            case SPECIAL_BLOCK:
                {
                    // Special basic blocks are empty and cannot throw exceptions,
                    // thus there is no need to perform any analysis.
                    SpecialBlock sb = (SpecialBlock) b;
                    final SpecialBlockType sType = sb.getSpecialType();
                    if (sType == SpecialBlockType.ENTRY) {
                        // storage the store at entry
                        storeAtEntry = outStores.get(sb);
                    } else {
                        assert sType == SpecialBlockType.EXIT
                                || sType == SpecialBlockType.EXCEPTIONAL_EXIT;
                        TransferInput<V, S> input = getInput(sb);
                        assert input != null : "@AssumeAssertion(nullness): invariant";
                        for (Block pred : sb.getPredecessors()) {
                            propagateStoresTo(pred, null, input, FlowRule.EACH_TO_EACH, false);
                        }
                    }
                    break;
                }
            default:
                throw new BugInCF("Unexpected block type: " + b.getType());
        }
    }

    @Override
    public @Nullable TransferInput<V, S> getInput(Block b) {
        return inputs.get(b);
    }

    @Override
    public @Nullable S getEntryStore() {
        return storeAtEntry;
    }

    @Override
    protected void initFields(ControlFlowGraph cfg) {
        super.initFields(cfg);
        outStores.clear();
        exceptionStores.clear();
        // storeAtEntry is null before analysis begin
        storeAtEntry = null;
    }

    @Override
    @RequiresNonNull("cfg")
    protected void initInitialInputs() {
        worklist.process(cfg);
        SpecialBlock regularExitBlock = cfg.getRegularExitBlock();
        SpecialBlock exceptionExitBlock = cfg.getExceptionalExitBlock();
        if (worklist.depthFirstOrder.get(regularExitBlock) == null
                && worklist.depthFirstOrder.get(exceptionExitBlock) == null) {
            throw new BugInCF(
                    "regularExitBlock and exceptionExitBlock should never both be null at the same"
                            + " time.");
        }
        UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
        List<ReturnNode> returnNodes = cfg.getReturnNodes();
        assert transferFunction != null : "@AssumeAssertion(nullness): invariant";
        S normalInitialStore = transferFunction.initialNormalExitStore(underlyingAST, returnNodes);
        S exceptionalInitialStore = transferFunction.initialExceptionalExitStore(underlyingAST);
        // If regularExitBlock or exceptionExitBlock is reachable in the control flow graph, then
        // initialize it as a start point of the analysis.
        if (worklist.depthFirstOrder.get(regularExitBlock) != null) {
            worklist.add(regularExitBlock);
            inputs.put(regularExitBlock, new TransferInput<>(null, this, normalInitialStore));
            outStores.put(regularExitBlock, normalInitialStore);
        }
        if (worklist.depthFirstOrder.get(exceptionExitBlock) != null) {
            worklist.add(exceptionExitBlock);
            inputs.put(
                    exceptionExitBlock, new TransferInput<>(null, this, exceptionalInitialStore));
            outStores.put(exceptionExitBlock, exceptionalInitialStore);
        }
        if (worklist.isEmpty()) {
            throw new BugInCF("The worklist needs at least one exit block as starting point.");
        }
        if (inputs.isEmpty() || outStores.isEmpty()) {
            throw new BugInCF("At least one input and one output store are required.");
        }
    }

    @Override
    protected void propagateStoresTo(
            Block pred,
            @Nullable Node node,
            TransferInput<V, S> currentInput,
            FlowRule flowRule,
            boolean addToWorklistAgain) {
        if (flowRule != FlowRule.EACH_TO_EACH) {
            throw new BugInCF(
                    "Backward analysis always propagates EACH to EACH, because there is no control"
                            + " flow.");
        }

        addStoreAfter(pred, node, currentInput.getRegularStore(), addToWorklistAgain);
    }

    /**
     * Add a store after the basic block {@code pred} by merging with the existing stores for that
     * location.
     *
     * @param pred the basic block
     * @param node the node of the basic block {@code b}
     * @param s the store being added
     * @param addBlockToWorklist whether the basic block {@code b} should be added back to {@code
     *     Worklist}
     */
    protected void addStoreAfter(Block pred, @Nullable Node node, S s, boolean addBlockToWorklist) {
        // If the block pred is an exception block, decide whether the block of passing node is an
        // exceptional successor of the block pred
        if (pred instanceof ExceptionBlock
                && ((ExceptionBlock) pred).getSuccessor() != null
                && node != null) {
            @Nullable Block succBlock = ((ExceptionBlock) pred).getSuccessor();
            @Nullable Block block = node.getBlock();
            if (succBlock != null && block != null && succBlock.getUid() == block.getUid()) {
                // If the block of passing node is an exceptional successor of Block pred, propagate
                // store to the exceptionStores. Currently it doesn't track the label of an
                // exceptional edge from exception block to its exceptional successors in backward
                // direction. Instead, all exception stores of exceptional successors of an
                // exception block will merge to one exception store at the exception block
                ExceptionBlock ebPred = (ExceptionBlock) pred;
                S exceptionStore = exceptionStores.get(ebPred);
                S newExceptionStore =
                        (exceptionStore != null) ? exceptionStore.leastUpperBound(s) : s;
                if (!newExceptionStore.equals(exceptionStore)) {
                    exceptionStores.put(ebPred, newExceptionStore);
                    inputs.put(ebPred, new TransferInput<V, S>(node, this, newExceptionStore));
                    addBlockToWorklist = true;
                }
            }
        } else {
            S predOutStore = getStoreAfter(pred);
            S newPredOutStore = (predOutStore != null) ? predOutStore.leastUpperBound(s) : s;
            if (!newPredOutStore.equals(predOutStore)) {
                outStores.put(pred, newPredOutStore);
                inputs.put(pred, new TransferInput<>(node, this, newPredOutStore));
                addBlockToWorklist = true;
            }
        }
        if (addBlockToWorklist) {
            addToWorklist(pred);
        }
    }

    /**
     * Returns the store corresponding to the location right after the basic block {@code b}.
     *
     * @param b the given block
     * @return the store right after the given block
     */
    protected @Nullable S getStoreAfter(Block b) {
        return readFromStore(outStores, b);
    }

    @Override
    public S runAnalysisFor(
            @FindDistinct Node node,
            Analysis.BeforeOrAfter preOrPost,
            TransferInput<V, S> blockTransferInput,
            IdentityHashMap<Node, V> nodeValues,
            Map<TransferInput<V, S>, IdentityHashMap<Node, TransferResult<V, S>>> analysisCaches) {
        Block block = node.getBlock();
        assert block != null : "@AssumeAssertion(nullness): invariant";
        Node oldCurrentNode = currentNode;
        if (isRunning) {
            assert currentInput != null : "@AssumeAssertion(nullness): invariant";
            return currentInput.getRegularStore();
        }
        isRunning = true;
        try {
            switch (block.getType()) {
                case REGULAR_BLOCK:
                    {
                        RegularBlock rBlock = (RegularBlock) block;
                        // Apply transfer function to contents until we found the node we are
                        // looking for.
                        TransferInput<V, S> store = blockTransferInput;
                        List<Node> nodeList = rBlock.getNodes();
                        ListIterator<Node> reverseIter = nodeList.listIterator(nodeList.size());
                        while (reverseIter.hasPrevious()) {
                            Node n = reverseIter.previous();
                            setCurrentNode(n);
                            if (n == node && preOrPost == Analysis.BeforeOrAfter.AFTER) {
                                return store.getRegularStore();
                            }
                            // Copy the store to avoid changing other blocks' transfer inputs in
                            // {@link #inputs}
                            TransferResult<V, S> transferResult =
                                    callTransferFunction(n, store.copy());
                            if (n == node) {
                                return transferResult.getRegularStore();
                            }
                            store = new TransferInput<>(n, this, transferResult);
                        }
                        throw new BugInCF("node %s is not in node.getBlock()=%s", node, block);
                    }
                case EXCEPTION_BLOCK:
                    {
                        ExceptionBlock eb = (ExceptionBlock) block;
                        if (eb.getNode() != node) {
                            throw new BugInCF(
                                    "Node should be equal to eb.getNode(). But get: node: "
                                            + node
                                            + "\teb.getNode(): "
                                            + eb.getNode());
                        }
                        if (preOrPost == Analysis.BeforeOrAfter.AFTER) {
                            return blockTransferInput.getRegularStore();
                        }
                        setCurrentNode(node);
                        // Copy the store to avoid changing other blocks' transfer inputs in {@link
                        // #inputs}
                        TransferResult<V, S> transferResult =
                                callTransferFunction(node, blockTransferInput.copy());
                        // Merge transfer result with the exception store of this exceptional block
                        S exceptionStore = exceptionStores.get(eb);
                        return exceptionStore == null
                                ? transferResult.getRegularStore()
                                : transferResult.getRegularStore().leastUpperBound(exceptionStore);
                    }
                default:
                    // Only regular blocks and exceptional blocks can hold nodes.
                    throw new BugInCF("Unexpected block type: " + block.getType());
            }

        } finally {
            setCurrentNode(oldCurrentNode);
            isRunning = false;
        }
    }
}
