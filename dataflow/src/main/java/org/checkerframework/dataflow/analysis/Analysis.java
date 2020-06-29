package org.checkerframework.dataflow.analysis;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGLambda;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.UnderlyingAST.Kind;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.RegularBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;

/**
 * An implementation of an iterative algorithm to solve a org.checkerframework.dataflow problem,
 * given a control flow graph and a transfer function.
 *
 * @param <V> the abstract value type to be tracked by the analysis
 * @param <S> the store type used in the analysis
 * @param <T> the transfer function type that is used to approximated runtime behavior
 */
public class Analysis<
        V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>> {

    /** Is the analysis currently running? */
    protected boolean isRunning = false;

    /** The transfer function for regular nodes. */
    // TODO: make final. Currently, the transferFunction has a reference to the analysis, so it
    // can't be created until the Analysis is initialized.
    protected @Nullable T transferFunction;

    /** The current control flow graph to perform the analysis on. */
    protected @MonotonicNonNull ControlFlowGraph cfg;

    /** Then stores before every basic block (assumed to be 'no information' if not present). */
    protected final IdentityHashMap<Block, S> thenStores;

    /** Else stores before every basic block (assumed to be 'no information' if not present). */
    protected final IdentityHashMap<Block, S> elseStores;

    /**
     * Number of times every block has been analyzed since the last time widening was applied. Null,
     * if maxCountBeforeWidening is -1 which implies widening isn't used for this analysis.
     */
    protected final @Nullable IdentityHashMap<Block, Integer> blockCount;

    /**
     * Number of times a block can be analyzed before widening. -1 implies that widening shouldn't
     * be used.
     */
    protected final int maxCountBeforeWidening;

    /**
     * The transfer inputs before every basic block (assumed to be 'no information' if not present).
     */
    protected final IdentityHashMap<Block, TransferInput<V, S>> inputs;

    /** The stores after every return statement. */
    protected final IdentityHashMap<ReturnNode, TransferResult<V, S>> storesAtReturnStatements;

    /** The worklist used for the fix-point iteration. */
    protected final Worklist worklist;

    /** Abstract values of nodes. */
    protected final IdentityHashMap<Node, V> nodeValues;

    /** Map from (effectively final) local variable elements to their abstract value. */
    public final HashMap<Element, V> finalLocalValues;

    /**
     * The node that is currently handled in the analysis (if it is running). The following
     * invariant holds:
     *
     * <pre>
     *   !isRunning &rArr; (currentNode == null)
     * </pre>
     */
    protected @Nullable Node currentNode;

    /**
     * The tree that is currently being looked at. The transfer function can set this tree to make
     * sure that calls to {@code getValue} will not return information for this given tree.
     */
    protected @Nullable Tree currentTree;

    /** The current transfer input when the analysis is running. */
    protected @Nullable TransferInput<V, S> currentInput;

    /** The tree that is currently being looked at. */
    public @Nullable Tree getCurrentTree() {
        return currentTree;
    }

    public void setCurrentTree(Tree currentTree) {
        this.currentTree = currentTree;
    }

    // `@code`, not `@link`, because dataflow module doesn't depend on framework module.
    /**
     * Construct an object that can perform a org.checkerframework.dataflow analysis over a control
     * flow graph. The transfer function is set by the subclass, e.g., {@code
     * org.checkerframework.framework.flow.CFAbstractAnalysis}, later.
     */
    public Analysis() {
        this(null, -1);
    }

    // `@code`, not `@link`, because dataflow module doesn't depend on framework moduel.
    /**
     * Construct an object that can perform a org.checkerframework.dataflow analysis over a control
     * flow graph. The transfer function is set by the subclass, e.g., {@code
     * org.checkerframework.framework.flow.CFAbstractAnalysis}, later.
     *
     * @param maxCountBeforeWidening number of times a block can be analyzed before widening
     */
    public Analysis(int maxCountBeforeWidening) {
        this(null, maxCountBeforeWidening);
    }

    /**
     * Construct an object that can perform a org.checkerframework.dataflow analysis over a control
     * flow graph, given a transfer function.
     *
     * @param transfer transfer function
     */
    public Analysis(T transfer) {
        this(transfer, -1);
    }

    /**
     * Construct an object that can perform a org.checkerframework.dataflow analysis over a control
     * flow graph, given a transfer function.
     *
     * @param transfer transfer function
     * @param maxCountBeforeWidening number of times a block can be analyzed before widening
     */
    public Analysis(@Nullable T transfer, int maxCountBeforeWidening) {
        this.transferFunction = transfer;
        this.maxCountBeforeWidening = maxCountBeforeWidening;
        this.thenStores = new IdentityHashMap<>();
        this.elseStores = new IdentityHashMap<>();
        this.blockCount = maxCountBeforeWidening == -1 ? null : new IdentityHashMap<>();
        this.inputs = new IdentityHashMap<>();
        this.storesAtReturnStatements = new IdentityHashMap<>();
        this.worklist = new Worklist();
        this.nodeValues = new IdentityHashMap<>();
        this.finalLocalValues = new HashMap<>();
    }

    /**
     * The current transfer function.
     *
     * @return {@link #transferFunction}
     */
    public @Nullable T getTransferFunction() {
        return transferFunction;
    }

    /**
     * Perform the actual analysis.
     *
     * @param cfg the control flow graph used to perform analysis
     */
    public void performAnalysis(ControlFlowGraph cfg) {
        assert !isRunning;
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

    /** Perform the actual analysis on one block. */
    protected void performAnalysisBlock(Block b) {
        switch (b.getType()) {
            case REGULAR_BLOCK:
                {
                    RegularBlock rb = (RegularBlock) b;

                    // apply transfer function to contents
                    TransferInput<V, S> inputBefore = getInputBefore(rb);
                    assert inputBefore != null : "@AssumeAssertion(nullness): invariant";
                    currentInput = inputBefore.copy();
                    TransferResult<V, S> transferResult = null;
                    Node lastNode = null;
                    boolean addToWorklistAgain = false;
                    for (Node n : rb.getContents()) {
                        assert currentInput != null : "@AssumeAssertion(nullness): invariant";
                        transferResult = callTransferFunction(n, currentInput);
                        addToWorklistAgain |= updateNodeValues(n, transferResult);
                        currentInput = new TransferInput<>(n, this, transferResult);
                        lastNode = n;
                    }
                    assert currentInput != null : "@AssumeAssertion(nullness): invariant";
                    // loop will run at least once, making transferResult non-null

                    // propagate store to successors
                    Block succ = rb.getSuccessor();
                    assert succ != null
                            : "@AssumeAssertion(nullness): regular basic block without non-exceptional successor unexpected";
                    propagateStoresTo(
                            succ, lastNode, currentInput, rb.getFlowRule(), addToWorklistAgain);
                    break;
                }

            case EXCEPTION_BLOCK:
                {
                    ExceptionBlock eb = (ExceptionBlock) b;

                    // apply transfer function to content
                    TransferInput<V, S> inputBefore = getInputBefore(eb);
                    assert inputBefore != null : "@AssumeAssertion(nullness): invariant";
                    currentInput = inputBefore.copy();
                    Node node = eb.getNode();
                    TransferResult<V, S> transferResult = callTransferFunction(node, currentInput);
                    boolean addToWorklistAgain = updateNodeValues(node, transferResult);

                    // propagate store to successor
                    Block succ = eb.getSuccessor();
                    if (succ != null) {
                        currentInput = new TransferInput<>(node, this, transferResult);
                        // TODO? Variable wasn't used.
                        // Store.FlowRule storeFlow = eb.getFlowRule();
                        propagateStoresTo(
                                succ, node, currentInput, eb.getFlowRule(), addToWorklistAgain);
                    }

                    // propagate store to exceptional successors
                    for (Map.Entry<TypeMirror, Set<Block>> e :
                            eb.getExceptionalSuccessors().entrySet()) {
                        TypeMirror cause = e.getKey();
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

                    // get store before
                    TransferInput<V, S> inputBefore = getInputBefore(cb);
                    assert inputBefore != null : "@AssumeAssertion(nullness): invariant";
                    TransferInput<V, S> input = inputBefore.copy();

                    // propagate store to successor
                    Block thenSucc = cb.getThenSuccessor();
                    Block elseSucc = cb.getElseSuccessor();

                    propagateStoresTo(thenSucc, null, input, cb.getThenFlowRule(), false);
                    propagateStoresTo(elseSucc, null, input, cb.getElseFlowRule(), false);
                    break;
                }

            case SPECIAL_BLOCK:
                {
                    // special basic blocks are empty and cannot throw exceptions,
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
                assert false;
                break;
        }
    }

    /**
     * Propagate the stores in currentInput to the successor block, succ, according to the flowRule.
     */
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
     * Updates the value of node {@code node} to the value of the {@code transferResult}. Returns
     * true if the node's value changed, or a store was updated.
     *
     * @param node a node
     * @param transferResult the new transfer result to use as {@code node}'s value
     * @return true if the node's value changed, or a store was updated
     */
    protected boolean updateNodeValues(Node node, TransferResult<V, S> transferResult) {
        V newVal = transferResult.getResultValue();
        boolean nodeValueChanged = false;

        if (newVal != null) {
            V oldVal = nodeValues.get(node);
            nodeValues.put(node, newVal);
            nodeValueChanged = !Objects.equals(oldVal, newVal);
        }

        return nodeValueChanged || transferResult.storeChanged();
    }

    /**
     * Call the transfer function for node {@code node}, and set that node as current node first.
     *
     * @param node a node
     * @param store the input of a transfer function
     * @return the transfer result for the node
     */
    protected TransferResult<V, S> callTransferFunction(Node node, TransferInput<V, S> store) {
        assert transferFunction != null : "@AssumeAssertion(nullness): invariant";
        if (node.isLValue()) {
            // TODO: should the default behavior be to return either a regular
            // transfer result or a conditional transfer result (depending on
            // store.hasTwoStores()), or is the following correct?
            return new RegularTransferResult<>(null, store.getRegularStore());
        }
        store.node = node;
        currentNode = node;
        TransferResult<V, S> transferResult = node.accept(transferFunction, store);
        currentNode = null;
        if (node instanceof ReturnNode) {
            // save a copy of the store to later check if some property held at
            // a given return statement
            storesAtReturnStatements.put((ReturnNode) node, transferResult);
        }
        if (node instanceof AssignmentNode) {
            // store the flow-refined value for effectively final local variables
            AssignmentNode assignment = (AssignmentNode) node;
            Node lhst = assignment.getTarget();
            if (lhst instanceof LocalVariableNode) {
                LocalVariableNode lhs = (LocalVariableNode) lhst;
                Element elem = lhs.getElement();
                if (ElementUtils.isEffectivelyFinal(elem)) {
                    V resval = transferResult.getResultValue();
                    if (resval != null) {
                        finalLocalValues.put(elem, resval);
                    }
                }
            }
        }
        return transferResult;
    }

    /**
     * Initialize the analysis with a new control flow graph.
     *
     * @param cfg the control flow graph to use
     */
    @EnsuresNonNull("this.cfg")
    protected void init(ControlFlowGraph cfg) {
        thenStores.clear();
        elseStores.clear();
        if (blockCount != null) {
            blockCount.clear();
        }
        inputs.clear();
        storesAtReturnStatements.clear();
        nodeValues.clear();
        finalLocalValues.clear();

        this.cfg = cfg;
        worklist.process(cfg);
        worklist.add(cfg.getEntryBlock());

        List<LocalVariableNode> parameters = null;
        UnderlyingAST underlyingAST = cfg.getUnderlyingAST();
        if (underlyingAST.getKind() == Kind.METHOD) {
            MethodTree tree = ((CFGMethod) underlyingAST).getMethod();
            parameters = new ArrayList<>();
            for (VariableTree p : tree.getParameters()) {
                LocalVariableNode var = new LocalVariableNode(p);
                parameters.add(var);
                // TODO: document that LocalVariableNode has no block that it
                // belongs to
            }
        } else if (underlyingAST.getKind() == Kind.LAMBDA) {
            LambdaExpressionTree lambda = ((CFGLambda) underlyingAST).getLambdaTree();
            parameters = new ArrayList<>();
            for (VariableTree p : lambda.getParameters()) {
                LocalVariableNode var = new LocalVariableNode(p);
                parameters.add(var);
                // TODO: document that LocalVariableNode has no block that it
                // belongs to
            }

        } else {
            // nothing to do
        }
        assert transferFunction != null : "@AssumeAssertion(nullness): invariant";
        S initialStore = transferFunction.initialStore(underlyingAST, parameters);
        Block entry = cfg.getEntryBlock();
        thenStores.put(entry, initialStore);
        elseStores.put(entry, initialStore);
        inputs.put(entry, new TransferInput<>(null, this, initialStore));
    }

    /**
     * Add a basic block to the worklist. If {@code b} is already present, the method does nothing.
     */
    protected void addToWorklist(Block b) {
        // TODO: use a more efficient way to check if b is already present
        if (!worklist.contains(b)) {
            worklist.add(b);
        }
    }

    /**
     * Add a store before the basic block {@code b} by merging with the existing stores for that
     * location.
     */
    protected void addStoreBefore(
            Block b, @Nullable Node node, S s, Store.Kind kind, boolean addBlockToWorklist) {
        S thenStore = getStoreBefore(b, Store.Kind.THEN);
        S elseStore = getStoreBefore(b, Store.Kind.ELSE);
        boolean shouldWiden = false;

        if (blockCount != null) {
            Integer count = blockCount.get(b);
            if (count == null) {
                count = 0;
            }
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
                if (thenStore == elseStore) {
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

    /** Merge two stores, possibly widening the result. */
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
     * A worklist is a priority queue of blocks in which the order is given by depth-first ordering
     * to place non-loop predecessors ahead of successors.
     */
    protected static class Worklist {

        /** Map all blocks in the CFG to their depth-first order. */
        protected final IdentityHashMap<Block, Integer> depthFirstOrder;

        /** Comparator to allow priority queue to order blocks by their depth-first order. */
        public class DFOComparator implements Comparator<Block> {
            @SuppressWarnings("unboxing.of.nullable")
            @Override
            public int compare(Block b1, Block b2) {
                return depthFirstOrder.get(b1) - depthFirstOrder.get(b2);
            }
        }

        /** The backing priority queue. */
        protected final PriorityQueue<Block> queue;

        public Worklist() {
            depthFirstOrder = new IdentityHashMap<>();
            queue = new PriorityQueue<>(11, new DFOComparator());
        }

        public void process(ControlFlowGraph cfg) {
            depthFirstOrder.clear();
            int count = 1;
            for (Block b : cfg.getDepthFirstOrderedBlocks()) {
                depthFirstOrder.put(b, count++);
            }

            queue.clear();
        }

        /**
         * See {@link PriorityQueue#isEmpty}.
         *
         * @see PriorityQueue#isEmpty
         */
        @EnsuresNonNullIf(result = false, expression = "poll()")
        @SuppressWarnings("nullness:contracts.conditional.postcondition.not.satisfied") // forwarded
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        public boolean contains(Block block) {
            return queue.contains(block);
        }

        public void add(Block block) {
            queue.add(block);
        }

        /**
         * See {@link PriorityQueue#poll}.
         *
         * @see PriorityQueue#poll
         */
        public @Nullable Block poll() {
            return queue.poll();
        }

        @Override
        public String toString() {
            return "Worklist(" + queue + ")";
        }
    }

    /**
     * Read the {@link TransferInput} for a particular basic block (or {@code null} if none exists
     * yet).
     *
     * @param b a basic block
     * @return the transfer input for the basic block
     */
    public @Nullable TransferInput<V, S> getInput(Block b) {
        return getInputBefore(b);
    }

    /**
     * Returns the transfer input corresponding to the location right before the basic block {@code
     * b}.
     *
     * @param b a basic block
     * @return the transfer input corresponding to the location right before the basic block {@code
     *     b}
     */
    protected @Nullable TransferInput<V, S> getInputBefore(Block b) {
        return inputs.get(b);
    }

    /**
     * Returns the store corresponding to the location right before the basic block {@code b}.
     *
     * @return the store corresponding to the location right before the basic block {@code b}
     */
    protected @Nullable S getStoreBefore(Block b, Store.Kind kind) {
        switch (kind) {
            case THEN:
                return readFromStore(thenStores, b);
            case ELSE:
                return readFromStore(elseStores, b);
            default:
                assert false;
                return null;
        }
    }

    /**
     * Read the {@link Store} for a particular basic block from a map of stores (or {@code null} if
     * none exists yet).
     */
    protected static <S> @Nullable S readFromStore(Map<Block, S> stores, Block b) {
        return stores.get(b);
    }

    /** Is the analysis currently running? */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Returns the abstract value for node {@code n}, or {@code null} if no information is
     * available. Note that if the analysis has not finished yet, this value might not represent the
     * final value for this node.
     *
     * @param n a node
     * @return the abstract value for node {@code n}, or {@code null} if no information is available
     */
    public @Nullable V getValue(Node n) {
        if (isRunning) {
            // we do not yet have a org.checkerframework.dataflow fact about the current node
            if (currentNode == null
                    || currentNode == n
                    || (currentTree != null && currentTree == n.getTree())) {
                return null;
            }
            // check that 'n' is a subnode of 'node'. Check immediate operands
            // first for efficiency.
            assert !n.isLValue() : "Did not expect an lvalue, but got " + n;
            if (currentNode == n
                    || (!currentNode.getOperands().contains(n)
                            && !currentNode.getTransitiveOperands().contains(n))) {
                return null;
            }
            return nodeValues.get(n);
        }
        return nodeValues.get(n);
    }

    /**
     * Return all current node values.
     *
     * @return all current node values
     */
    public IdentityHashMap<Node, V> getNodeValues() {
        return nodeValues;
    }

    /**
     * Set all current node values to the given map.
     *
     * @param in the current node values
     */
    /*package-private*/ void setNodeValues(IdentityHashMap<Node, V> in) {
        assert !isRunning;
        nodeValues.clear();
        nodeValues.putAll(in);
    }

    /**
     * Returns the abstract value for {@code t}, or {@code null} if no information is available.
     * Note that if the analysis has not finished yet, this value might not represent the final
     * value for this node.
     *
     * @param t a tree
     * @return the abstract value for {@code t}, or {@code null} if no information is available
     */
    public @Nullable V getValue(Tree t) {
        // we do not yet have a org.checkerframework.dataflow fact about the current node
        if (t == currentTree) {
            return null;
        }
        Set<Node> nodesCorrespondingToTree = getNodesForTree(t);
        if (nodesCorrespondingToTree == null) {
            return null;
        }
        V merged = null;
        for (Node aNode : nodesCorrespondingToTree) {
            if (aNode.isLValue()) {
                return null;
            }
            V a = getValue(aNode);
            if (merged == null) {
                merged = a;
            } else if (a != null) {
                merged = merged.leastUpperBound(a);
            }
        }
        return merged;
    }

    /**
     * Get the set of {@link Node}s for a given {@link Tree}. Returns null for trees that don't
     * produce a value.
     */
    public @Nullable Set<Node> getNodesForTree(Tree t) {
        if (cfg == null) {
            return null;
        }
        Set<Node> nodes = cfg.getNodesCorrespondingToTree(t);
        return nodes;
    }

    /**
     * Get the {@link MethodTree} of the current CFG if the argument {@link Tree} maps to a {@link
     * Node} in the CFG or null otherwise.
     */
    public @Nullable MethodTree getContainingMethod(Tree t) {
        if (cfg == null) {
            return null;
        }
        MethodTree mt = cfg.getContainingMethod(t);
        return mt;
    }

    /**
     * Get the {@link ClassTree} of the current CFG if the argument {@link Tree} maps to a {@link
     * Node} in the CFG or null otherwise.
     */
    public @Nullable ClassTree getContainingClass(Tree t) {
        if (cfg == null) {
            return null;
        }
        ClassTree ct = cfg.getContainingClass(t);
        return ct;
    }

    /**
     * The transfer results for each return node in the CFG.
     *
     * @return the transfer results for each return node in the CFG
     */
    @RequiresNonNull("cfg")
    public List<Pair<ReturnNode, @Nullable TransferResult<V, S>>> getReturnStatementStores() {
        assert cfg != null : "@AssumeAssertion(nullness): invariant";
        List<Pair<ReturnNode, @Nullable TransferResult<V, S>>> result = new ArrayList<>();
        for (ReturnNode returnNode : cfg.getReturnNodes()) {
            TransferResult<V, S> store = storesAtReturnStatements.get(returnNode);
            result.add(Pair.of(returnNode, store));
        }
        return result;
    }

    /**
     * The result of running the analysis. This is only available once the analysis finished
     * running.
     *
     * @return the result of running the analysis
     */
    @RequiresNonNull("cfg")
    public AnalysisResult<V, S> getResult() {
        assert !isRunning;
        assert cfg != null : "@AssumeAssertion(nullness): invariant";
        return new AnalysisResult<>(
                nodeValues,
                inputs,
                cfg.getTreeLookup(),
                cfg.getUnaryAssignNodeLookup(),
                finalLocalValues);
    }

    /**
     * Returns the regular exit store, or {@code null}, if there is no such store (because the
     * method cannot exit through the regular exit block).
     *
     * @return the regular exit store, or {@code null}, if there is no such store (because the
     *     method cannot exit through the regular exit block)
     */
    @RequiresNonNull("cfg")
    public @Nullable S getRegularExitStore() {
        assert cfg != null : "@AssumeAssertion(nullness): invariant";
        SpecialBlock regularExitBlock = cfg.getRegularExitBlock();
        if (inputs.containsKey(regularExitBlock)) {
            S regularExitStore = inputs.get(regularExitBlock).getRegularStore();
            return regularExitStore;
        } else {
            return null;
        }
    }

    /**
     * Returns the exceptional exit store.
     *
     * @return the exceptional exit store
     */
    @RequiresNonNull("cfg")
    public @Nullable S getExceptionalExitStore() {
        assert cfg != null : "@AssumeAssertion(nullness): invariant";
        SpecialBlock exceptionalExitBlock = cfg.getExceptionalExitBlock();
        if (inputs.containsKey(exceptionalExitBlock)) {
            S exceptionalExitStore = inputs.get(exceptionalExitBlock).getRegularStore();
            return exceptionalExitStore;
        } else {
            return null;
        }
    }
}
