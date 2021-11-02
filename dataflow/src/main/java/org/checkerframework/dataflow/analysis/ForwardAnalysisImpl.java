package org.checkerframework.dataflow.analysis;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGLambda;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.RegularBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;
import org.plumelib.util.CollectionsPlume;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.type.TypeMirror;

/**
 * An implementation of a forward analysis to solve a org.checkerframework.dataflow problem given a
 * control flow graph and a forward transfer function.
 *
 * @param <V> the abstract value type to be tracked by the analysis
 * @param <S> the store type used in the analysis
 * @param <T> the transfer function type that is used to approximate runtime behavior
 */
public class ForwardAnalysisImpl<
                V extends AbstractValue<V>,
                S extends Store<S>,
                T extends ForwardTransferFunction<V, S>>
        extends AbstractAnalysis<V, S, T> implements ForwardAnalysis<V, S, T> {

    /**
     * Number of times each block has been analyzed since the last time widening was applied. Null
     * if maxCountBeforeWidening is -1, which implies widening isn't used for this analysis.
     */
    protected final @Nullable IdentityHashMap<Block, Integer> blockCount;

    /**
     * Number of times a block can be analyzed before widening. -1 implies that widening shouldn't
     * be used.
     */
    protected final int maxCountBeforeWidening;

    /** Then stores before every basic block (assumed to be 'no information' if not present). */
    protected final IdentityHashMap<Block, S> thenStores;

    /** Else stores before every basic block (assumed to be 'no information' if not present). */
    protected final IdentityHashMap<Block, S> elseStores;

    /** The stores after every return statement. */
    protected final IdentityHashMap<ReturnNode, TransferResult<V, S>> storesAtReturnStatements;

    // `@code`, not `@link`, because dataflow module doesn't depend on framework module.
    /**
     * Construct an object that can perform a org.checkerframework.dataflow forward analysis over a
     * control flow graph. When using this constructor, the transfer function is set later by the
     * subclass, e.g., {@code org.checkerframework.framework.flow.CFAbstractAnalysis}.
     *
     * @param maxCountBeforeWidening number of times a block can be analyzed before widening
     */
    public ForwardAnalysisImpl(int maxCountBeforeWidening) {
        super(Direction.FORWARD);
        this.maxCountBeforeWidening = maxCountBeforeWidening;
        this.blockCount = maxCountBeforeWidening == -1 ? null : new IdentityHashMap<>();
        this.thenStores = new IdentityHashMap<>();
        this.elseStores = new IdentityHashMap<>();
        this.storesAtReturnStatements = new IdentityHashMap<>();
    }

    /**
     * Construct an object that can perform a org.checkerframework.dataflow forward analysis over a
     * control flow graph given a transfer function.
     *
     * @param transfer the transfer function
     */
    public ForwardAnalysisImpl(@Nullable T transfer) {
        this(-1);
        this.transferFunction = transfer;
    }

    @Override
    public void performAnalysis(ControlFlowGraph cfg) {
        if (isRunning) {
            throw new BugInCF(
                    "ForwardAnalysisImpl::performAnalysis() shouldn't be called when the analysis"
                            + " is running.");
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
                    // Apply transfer function to contents
                    TransferInput<V, S> inputBefore = getInputBefore(rb);
                    assert inputBefore != null : "@AssumeAssertion(nullness): invariant";
                    currentInput = inputBefore.copy();
                    Node lastNode = null;
                    boolean addToWorklistAgain = false;
                    for (Node n : rb.getNodes()) {
                        assert currentInput != null : "@AssumeAssertion(nullness): invariant";
                        TransferResult<V, S> transferResult = callTransferFunction(n, currentInput);
                        addToWorklistAgain |= updateNodeValues(n, transferResult);
                        currentInput = new TransferInput<>(n, this, transferResult);
                        lastNode = n;
                    }
                    assert currentInput != null : "@AssumeAssertion(nullness): invariant";
                    // Loop will run at least once, making transferResult non-null
                    // Propagate store to successors
                    Block succ = rb.getSuccessor();
                    assert succ != null
                            : "@AssumeAssertion(nullness): regular basic block without"
                                    + " non-exceptional successor unexpected";
                    propagateStoresTo(
                            succ, lastNode, currentInput, rb.getFlowRule(), addToWorklistAgain);
                    break;
                }
            case EXCEPTION_BLOCK:
                {
                    ExceptionBlock eb = (ExceptionBlock) b;
                    // Apply transfer function to content
                    TransferInput<V, S> inputBefore = getInputBefore(eb);
                    assert inputBefore != null : "@AssumeAssertion(nullness): invariant";
                    currentInput = inputBefore.copy();
                    Node node = eb.getNode();
                    TransferResult<V, S> transferResult = callTransferFunction(node, currentInput);
                    boolean addToWorklistAgain = updateNodeValues(node, transferResult);
                    // Propagate store to successor
                    Block succ = eb.getSuccessor();
                    if (succ != null) {
                        currentInput = new TransferInput<>(node, this, transferResult);
                        propagateStoresTo(
                                succ, node, currentInput, eb.getFlowRule(), addToWorklistAgain);
                    }
                    // Propagate store to exceptional successors
                    for (Map.Entry<TypeMirror, Set<Block>> e :
                            eb.getExceptionalSuccessors().entrySet()) {
                        TypeMirror cause = e.getKey();
                        if (isIgnoredExceptionType(cause)) {
                            continue;
                        }
                        S exceptionalStore = transferResult.getExceptionalStore(cause);
                        if (exceptionalStore != null) {
                            for (Block exceptionSucc : e.getValue()) {
                                addStoreBefore(
                                        exceptionSucc,
                                        node,
                                        exceptionalStore,
                                        Store.Kind.BOTH,
                                        addToWorklistAgain);
                            }
                        } else {
                            for (Block exceptionSucc : e.getValue()) {
                                addStoreBefore(
                                        exceptionSucc,
                                        node,
                                        inputBefore.copy().getRegularStore(),
                                        Store.Kind.BOTH,
                                        addToWorklistAgain);
                            }
                        }
                    }
                    break;
                }
            case CONDITIONAL_BLOCK:
                {
                    ConditionalBlock cb = (ConditionalBlock) b;
                    // Get store before
                    TransferInput<V, S> inputBefore = getInputBefore(cb);
                    assert inputBefore != null : "@AssumeAssertion(nullness): invariant";
                    TransferInput<V, S> input = inputBefore.copy();
                    // Propagate store to successor
                    Block thenSucc = cb.getThenSuccessor();
                    Block elseSucc = cb.getElseSuccessor();
                    propagateStoresTo(thenSucc, null, input, cb.getThenFlowRule(), false);
                    propagateStoresTo(elseSucc, null, input, cb.getElseFlowRule(), false);
                    break;
                }
            case SPECIAL_BLOCK:
                {
                    // Special basic blocks are empty and cannot throw exceptions,
                    // thus there is no need to perform any analysis.
                    SpecialBlock sb = (SpecialBlock) b;
                    Block succ = sb.getSuccessor();
                    if (succ != null) {
                        TransferInput<V, S> input = getInputBefore(b);
                        assert input != null : "@AssumeAssertion(nullness): invariant";
                        propagateStoresTo(succ, null, input, sb.getFlowRule(), false);
                    }
                    break;
                }
            default:
                throw new BugInCF("Unexpected block type: " + b.getType());
        }
    }

    @Override
    public @Nullable TransferInput<V, S> getInput(Block b) {
        return getInputBefore(b);
    }

    @Override
    @SuppressWarnings("nullness:contracts.precondition.override.invalid") // implementation field
    @RequiresNonNull("cfg")
    public List<Pair<ReturnNode, @Nullable TransferResult<V, S>>> getReturnStatementStores() {
        return CollectionsPlume
                .<ReturnNode, Pair<ReturnNode, @Nullable TransferResult<V, S>>>mapList(
                        returnNode -> Pair.of(returnNode, storesAtReturnStatements.get(returnNode)),
                        cfg.getReturnNodes());
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

        // Prepare cache
        IdentityHashMap<Node, TransferResult<V, S>> cache;
        if (analysisCaches != null) {
            cache =
                    analysisCaches.computeIfAbsent(
                            blockTransferInput, __ -> new IdentityHashMap<>());
        } else {
            cache = null;
        }

        if (isRunning) {
            assert currentInput != null : "@AssumeAssertion(nullness): invariant";
            return currentInput.getRegularStore();
        }
        setNodeValues(nodeValues);
        isRunning = true;
        try {
            switch (block.getType()) {
                case REGULAR_BLOCK:
                    {
                        RegularBlock rb = (RegularBlock) block;
                        // Apply transfer function to contents until we found the node we are
                        // looking for.
                        TransferInput<V, S> store = blockTransferInput;
                        TransferResult<V, S> transferResult;
                        for (Node n : rb.getNodes()) {
                            setCurrentNode(n);
                            if (n == node && preOrPost == Analysis.BeforeOrAfter.BEFORE) {
                                return store.getRegularStore();
                            }
                            if (cache != null && cache.containsKey(n)) {
                                transferResult = cache.get(n);
                            } else {
                                // Copy the store to avoid changing other blocks' transfer inputs in
                                // {@link #inputs}
                                transferResult = callTransferFunction(n, store.copy());
                                if (cache != null) {
                                    cache.put(n, transferResult);
                                }
                            }
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
                        // Apply the transfer function to content
                        if (eb.getNode() != node) {
                            throw new BugInCF(
                                    "Node should be equal to eb.getNode(). But get: node: "
                                            + node
                                            + "\teb.getNode(): "
                                            + eb.getNode());
                        }
                        if (preOrPost == Analysis.BeforeOrAfter.BEFORE) {
                            return blockTransferInput.getRegularStore();
                        }
                        setCurrentNode(node);
                        // Copy the store to avoid changing other blocks' transfer inputs in {@link
                        // #inputs}
                        TransferResult<V, S> transferResult;
                        if (cache != null && cache.containsKey(node)) {
                            transferResult = cache.get(node);
                        } else {
                            // Copy the store to avoid changing other blocks' transfer inputs in
                            // {@link #inputs}
                            transferResult = callTransferFunction(node, blockTransferInput.copy());
                            if (cache != null) {
                                cache.put(node, transferResult);
                            }
                        }
                        return transferResult.getRegularStore();
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

    @Override
    protected void initFields(ControlFlowGraph cfg) {
        thenStores.clear();
        elseStores.clear();
        if (blockCount != null) {
            blockCount.clear();
        }
        storesAtReturnStatements.clear();
        super.initFields(cfg);
    }

    @Override
    @RequiresNonNull("cfg")
    protected void initInitialInputs() {
        worklist.process(cfg);
        Block entry = cfg.getEntryBlock();
        worklist.add(entry);
        UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
        List<LocalVariableNode> parameters = getParameters(underlyingAST);
        assert transferFunction != null : "@AssumeAssertion(nullness): invariant";
        S initialStore = transferFunction.initialStore(underlyingAST, parameters);
        thenStores.put(entry, initialStore);
        elseStores.put(entry, initialStore);
        inputs.put(entry, new TransferInput<>(null, this, initialStore));
    }

    /**
     * Returns the formal parameters for a method.
     *
     * @param underlyingAST the AST for the method
     * @return the formal parameters for the method
     */
    @SideEffectFree
    private List<LocalVariableNode> getParameters(UnderlyingAST underlyingAST) {
        switch (underlyingAST.getKind()) {
            case METHOD:
                MethodTree tree = ((CFGMethod) underlyingAST).getMethod();
                // TODO: document that LocalVariableNode has no block that it belongs to
                return CollectionsPlume.mapList(LocalVariableNode::new, tree.getParameters());
            case LAMBDA:
                LambdaExpressionTree lambda = ((CFGLambda) underlyingAST).getLambdaTree();
                // TODO: document that LocalVariableNode has no block that it belongs to
                return CollectionsPlume.mapList(LocalVariableNode::new, lambda.getParameters());
            default:
                return Collections.emptyList();
        }
    }

    @Override
    protected TransferResult<V, S> callTransferFunction(Node node, TransferInput<V, S> input) {
        TransferResult<V, S> transferResult = super.callTransferFunction(node, input);

        if (node instanceof ReturnNode) {
            // Save a copy of the store to later check if some property holds at a given return
            // statement
            storesAtReturnStatements.put((ReturnNode) node, transferResult);
        }
        return transferResult;
    }

    @Override
    protected void propagateStoresTo(
            Block succ,
            @Nullable Node node,
            TransferInput<V, S> currentInput,
            Store.FlowRule flowRule,
            boolean addToWorklistAgain) {
        switch (flowRule) {
            case EACH_TO_EACH:
                if (currentInput.containsTwoStores()) {
                    addStoreBefore(
                            succ,
                            node,
                            currentInput.getThenStore(),
                            Store.Kind.THEN,
                            addToWorklistAgain);
                    addStoreBefore(
                            succ,
                            node,
                            currentInput.getElseStore(),
                            Store.Kind.ELSE,
                            addToWorklistAgain);
                } else {
                    addStoreBefore(
                            succ,
                            node,
                            currentInput.getRegularStore(),
                            Store.Kind.BOTH,
                            addToWorklistAgain);
                }
                break;
            case THEN_TO_BOTH:
                addStoreBefore(
                        succ,
                        node,
                        currentInput.getThenStore(),
                        Store.Kind.BOTH,
                        addToWorklistAgain);
                break;
            case ELSE_TO_BOTH:
                addStoreBefore(
                        succ,
                        node,
                        currentInput.getElseStore(),
                        Store.Kind.BOTH,
                        addToWorklistAgain);
                break;
            case THEN_TO_THEN:
                addStoreBefore(
                        succ,
                        node,
                        currentInput.getThenStore(),
                        Store.Kind.THEN,
                        addToWorklistAgain);
                break;
            case ELSE_TO_ELSE:
                addStoreBefore(
                        succ,
                        node,
                        currentInput.getElseStore(),
                        Store.Kind.ELSE,
                        addToWorklistAgain);
                break;
        }
    }

    /**
     * Add a store before the basic block {@code b} by merging with the existing stores for that
     * location.
     *
     * @param b a basic block
     * @param node the node of the basic block {@code b}
     * @param s the store being added
     * @param kind the kind of store {@code s}
     * @param addBlockToWorklist whether the basic block {@code b} should be added back to {@code
     *     Worklist}
     */
    protected void addStoreBefore(
            Block b, @Nullable Node node, S s, Store.Kind kind, boolean addBlockToWorklist) {
        S thenStore = getStoreBefore(b, Store.Kind.THEN);
        S elseStore = getStoreBefore(b, Store.Kind.ELSE);
        boolean shouldWiden = false;
        if (blockCount != null) {
            Integer count = blockCount.getOrDefault(b, 0);
            shouldWiden = count >= maxCountBeforeWidening;
            if (shouldWiden) {
                blockCount.put(b, 0);
            } else {
                blockCount.put(b, count + 1);
            }
        }
        switch (kind) {
            case THEN:
                {
                    // Update the then store
                    S newThenStore = mergeStores(s, thenStore, shouldWiden);
                    if (!newThenStore.equals(thenStore)) {
                        thenStores.put(b, newThenStore);
                        if (elseStore != null) {
                            inputs.put(b, new TransferInput<>(node, this, newThenStore, elseStore));
                            addBlockToWorklist = true;
                        }
                    }
                    break;
                }
            case ELSE:
                {
                    // Update the else store
                    S newElseStore = mergeStores(s, elseStore, shouldWiden);
                    if (!newElseStore.equals(elseStore)) {
                        elseStores.put(b, newElseStore);
                        if (thenStore != null) {
                            inputs.put(b, new TransferInput<>(node, this, thenStore, newElseStore));
                            addBlockToWorklist = true;
                        }
                    }
                    break;
                }
            case BOTH:
                @SuppressWarnings("interning:not.interned")
                boolean sameStore = (thenStore == elseStore);
                if (sameStore) {
                    // Currently there is only one regular store
                    S newStore = mergeStores(s, thenStore, shouldWiden);
                    if (!newStore.equals(thenStore)) {
                        thenStores.put(b, newStore);
                        elseStores.put(b, newStore);
                        inputs.put(b, new TransferInput<>(node, this, newStore));
                        addBlockToWorklist = true;
                    }
                } else {
                    boolean storeChanged = false;
                    S newThenStore = mergeStores(s, thenStore, shouldWiden);
                    if (!newThenStore.equals(thenStore)) {
                        thenStores.put(b, newThenStore);
                        storeChanged = true;
                    }
                    S newElseStore = mergeStores(s, elseStore, shouldWiden);
                    if (!newElseStore.equals(elseStore)) {
                        elseStores.put(b, newElseStore);
                        storeChanged = true;
                    }
                    if (storeChanged) {
                        inputs.put(b, new TransferInput<>(node, this, newThenStore, newElseStore));
                        addBlockToWorklist = true;
                    }
                }
        }
        if (addBlockToWorklist) {
            addToWorklist(b);
        }
    }

    /**
     * Merge two stores, possibly widening the result.
     *
     * @param newStore the new Store
     * @param previousStore the previous Store
     * @param shouldWiden should widen or not
     * @return the merged Store
     */
    private S mergeStores(S newStore, @Nullable S previousStore, boolean shouldWiden) {
        if (previousStore == null) {
            return newStore;
        } else if (shouldWiden) {
            return newStore.widenedUpperBound(previousStore);
        } else {
            return newStore.leastUpperBound(previousStore);
        }
    }

    /**
     * Return the store corresponding to the location right before the basic block {@code b}.
     *
     * @param b a block
     * @param kind the kind of store which will be returned
     * @return the store corresponding to the location right before the basic block {@code b}
     */
    protected @Nullable S getStoreBefore(Block b, Store.Kind kind) {
        switch (kind) {
            case THEN:
                return readFromStore(thenStores, b);
            case ELSE:
                return readFromStore(elseStores, b);
            default:
                throw new BugInCF("Unexpected Store.Kind: " + kind);
        }
    }

    /**
     * Returns the transfer input corresponding to the location right before the basic block {@code
     * b}.
     *
     * @param b a block
     * @return the transfer input corresponding to the location right before the basic block {@code
     *     b}
     */
    protected @Nullable TransferInput<V, S> getInputBefore(Block b) {
        return inputs.get(b);
    }
}
