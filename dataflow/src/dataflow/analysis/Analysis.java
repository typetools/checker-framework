package dataflow.analysis;

import dataflow.cfg.ControlFlowGraph;
import dataflow.cfg.UnderlyingAST;
import dataflow.cfg.UnderlyingAST.CFGMethod;
import dataflow.cfg.UnderlyingAST.Kind;
import dataflow.cfg.block.Block;
import dataflow.cfg.block.ConditionalBlock;
import dataflow.cfg.block.ExceptionBlock;
import dataflow.cfg.block.RegularBlock;
import dataflow.cfg.block.SingleSuccessorBlock;
import dataflow.cfg.block.SpecialBlock;
import dataflow.cfg.node.AssignmentNode;
import dataflow.cfg.node.LocalVariableNode;
import dataflow.cfg.node.Node;
import dataflow.cfg.node.ReturnNode;

import javacutils.ElementUtils;
import javacutils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * An implementation of an iterative algorithm to solve a dataflow problem,
 * given a control flow graph and a transfer function.
 *
 * @author Stefan Heule
 *
 * @param <A>
 *            The abstract value type to be tracked by the analysis.
 * @param <S>
 *            The store type used in the analsysis.
 * @param <T>
 *            The transfer function type that is used to approximated runtime
 *            behavior.
 */
public class Analysis<A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>> {

    /** Is the analysis currently running? */
    protected boolean isRunning = false;

    /** The transfer function for regular nodes. */
    protected T transferFunction;

    /** The control flow graph to perform the analysis on. */
    protected ControlFlowGraph cfg;

    /** The associated processing environment */
    protected final ProcessingEnvironment env;

    /** Instance of the types utility. */
    protected final Types types;

    /**
     * Then stores before every basic block (assumed to be 'no information' if
     * not present).
     */
    protected IdentityHashMap<Block, S> thenStores;

    /**
     * Else stores before every basic block (assumed to be 'no information' if
     * not present).
     */
    protected IdentityHashMap<Block, S> elseStores;

    /**
     * The transfer inputs before every basic block (assumed to be 'no information' if
     * not present).
     */
    protected IdentityHashMap<Block, TransferInput<A, S>> inputs;

    /**
     * The stores after every return statement.
     */
    protected IdentityHashMap<ReturnNode, TransferResult<A, S>> storesAtReturnStatements;

    /** The worklist used for the fix-point iteration. */
    protected Worklist worklist;

    /** Abstract values of nodes. */
    protected IdentityHashMap<Node, A> nodeValues;

    /** Map from (effectively final) local variable elements to their abstract value. */
    public HashMap<Element, A> finalLocalValues;

    /**
     * The node that is currently handled in the analysis (if it is running).
     * The following invariant holds:
     *
     * <pre>
     *   !isRunning ==> (currentNode == null)
     * </pre>
     */
    protected Node currentNode;

    /**
     * The tree that is currently being looked at. The transfer function can set
     * this tree to make sure that calls to {@code getValue} will not return
     * information for this given tree.
     */
    protected Tree currentTree;

    /**
     * The current transfer input when the analysis is running.
     */
    protected TransferInput<A, S> currentInput;

    public Tree getCurrentTree() {
        return currentTree;
    }

    public void setCurrentTree(Tree currentTree) {
        this.currentTree = currentTree;
    }

    /**
     * Construct an object that can perform a dataflow analysis over a control
     * flow graph. The transfer function is set later using
     * {@code setTransferFunction}.
     */
    public Analysis(ProcessingEnvironment env) {
        this.env = env;
        types = env.getTypeUtils();
    }

    /**
     * Construct an object that can perform a dataflow analysis over a control
     * flow graph, given a transfer function.
     */
    public Analysis(ProcessingEnvironment env, T transfer) {
        this(env);
        this.transferFunction = transfer;
    }

    public void setTransferFunction(T transfer) {
        this.transferFunction = transfer;
    }

    public Types getTypes() {
        return types;
    }

    public ProcessingEnvironment getEnv() {
        return env;
    }

    /**
     * Perform the actual analysis. Should only be called once after the object
     * has been created.
     *
     * @param cfg
     */
    public void performAnalysis(ControlFlowGraph cfg) {
        assert isRunning == false;
        isRunning = true;

        init(cfg);

        while (!worklist.isEmpty()) {
            Block b = worklist.poll();

            switch (b.getType()) {
            case REGULAR_BLOCK: {
                RegularBlock rb = (RegularBlock) b;

                // apply transfer function to contents
                TransferInput<A, S> inputBefore = getInputBefore(rb);
                currentInput = inputBefore.copy();
                TransferResult<A, S> transferResult = null;
                Node lastNode = null;
                boolean addToWorklistAgain = false;
                for (Node n : rb.getContents()) {
                    transferResult = callTransferFunction(n, currentInput);
                    A val = transferResult.getResultValue();
                    if (val != null) {
                        final boolean didNodeValuesChange = updateNodeValues(n, val);
                        addToWorklistAgain = didNodeValuesChange || addToWorklistAgain;
                    }
                    currentInput = new TransferInput<>(n, this, transferResult);
                    lastNode = n;
                }
                // loop will run at least one, making transferResult non-null

                // propagate store to successors
                Block succ = rb.getSuccessor();
                assert succ != null : "regular basic block without non-exceptional successor unexpected";
                Pair<Store.Kind, Store.Kind> storeFlow = rb.getStoreFlow();
                switch (storeFlow.first) {
                case THEN:
                    if (currentInput.containsTwoStores()) {
                        System.out.println("RB: Add THEN store: " + currentInput.getThenStore());
                        addStoreBefore(succ, lastNode, currentInput.getThenStore(), storeFlow.second,
                                       addToWorklistAgain);
                    } else {
                        System.out.println("RB: Add regular store #1: " + currentInput.getRegularStore());
                        addStoreBefore(succ, lastNode, currentInput.getRegularStore(), storeFlow.second,
                                       addToWorklistAgain);
                    }
                    break;
                case ELSE:
                    if (currentInput.containsTwoStores()) {
                        System.out.println("RB: Add ELSE store: " + currentInput.getElseStore());
                        addStoreBefore(succ, lastNode, currentInput.getElseStore(), storeFlow.second,
                                       addToWorklistAgain);
                    } else {
                        System.out.println("RB: Add regular store #2: " + currentInput.getRegularStore());
                        addStoreBefore(succ, lastNode, currentInput.getRegularStore(), storeFlow.second,
                                       addToWorklistAgain);
                    }
                    break;
                case BOTH:
                    System.out.println("RB: Add regular store #3: " + currentInput.getRegularStore());
                    addStoreBefore(succ, lastNode, currentInput.getRegularStore(), storeFlow.second,
                                   addToWorklistAgain);
                    break;
                }
                break;
            }

            case EXCEPTION_BLOCK: {
                ExceptionBlock eb = (ExceptionBlock) b;

                // apply transfer function to content
                TransferInput<A, S> inputBefore = getInputBefore(eb);
                currentInput = inputBefore.copy();
                Node node = eb.getNode();
                TransferResult<A, S> transferResult = callTransferFunction(
                        node, currentInput);
                A val = transferResult.getResultValue();
                boolean addToWorklistAgain = false;
                if (val != null) {
                    addToWorklistAgain = updateNodeValues(node, val);
                }

                // propagate store to successor
                Block succ = eb.getSuccessor();
                if (succ != null) {
                    currentInput = new TransferInput<>(node, this, transferResult);
                    Pair<Store.Kind, Store.Kind> storeFlow = eb.getStoreFlow();
                    assert storeFlow.first == Store.Kind.BOTH;
                    addStoreBefore(succ, node, currentInput.getRegularStore(), storeFlow.second,
                                   addToWorklistAgain);
                }

                // propagate store to exceptional successors
                for (Entry<TypeMirror, Set<Block>> e : eb.getExceptionalSuccessors()
                        .entrySet()) {
                    TypeMirror cause = e.getKey();
                    S exceptionalStore = transferResult
                            .getExceptionalStore(cause);
                    if (exceptionalStore != null) {
                        for (Block exceptionSucc : e.getValue()) {
                            addStoreBefore(exceptionSucc, node, exceptionalStore, Store.Kind.BOTH,
                                           addToWorklistAgain);
                        }
                    } else {
                        for (Block exceptionSucc : e.getValue()) {
                            addStoreBefore(exceptionSucc, node, inputBefore.copy().getRegularStore(),
                                           Store.Kind.BOTH, addToWorklistAgain);
                        }
                    }
                }
                break;
            }

            case CONDITIONAL_BLOCK: {
                ConditionalBlock cb = (ConditionalBlock) b;

                // get store before
                TransferInput<A, S> inputBefore = getInputBefore(cb);
                TransferInput<A, S> input = inputBefore.copy();

                // propagate store to successor
                Block thenSucc = cb.getThenSuccessor();
                Block elseSucc = cb.getElseSuccessor();

                Pair<Store.Kind, Store.Kind> thenStoreFlow = cb.getThenStoreFlow();
                assert thenStoreFlow.first == Store.Kind.THEN;
                System.out.println("CB: Add THEN store: " + input.getThenStore());
                addStoreBefore(thenSucc, null, input.getThenStore(), thenStoreFlow.second, false);

                Pair<Store.Kind, Store.Kind> elseStoreFlow = cb.getElseStoreFlow();
                assert elseStoreFlow.first == Store.Kind.ELSE;
                System.out.println("CB: Add ELSE store: " + input.getElseStore());
                addStoreBefore(elseSucc, null, input.getElseStore(), elseStoreFlow.second, false);
                break;
            }

            case SPECIAL_BLOCK: {
                // special basic blocks are empty and cannot throw exceptions,
                // thus there is no need to perform any analysis.
                SpecialBlock sb = (SpecialBlock) b;
                Block succ = sb.getSuccessor();
                if (succ != null) {
                    Pair<Store.Kind, Store.Kind> storeFlow = sb.getStoreFlow();
                    assert storeFlow.first == Store.Kind.BOTH;
                    addStoreBefore(succ, null, getInputBefore(b).getRegularStore(),
                                   storeFlow.second, false);
                }
                break;
            }

            default:
                assert false;
                break;
            }
        }

        assert isRunning == true;
        isRunning = false;
    }

    /**
     * Updates the value of node {@code n} to {@code val}, and returns true if
     * anything in {@code nodeValues} changed (i.e., if we need to iterate
     * further).
     */
    protected boolean updateNodeValues(Node n, A val) {
        A oldVal = nodeValues.get(n);
        nodeValues.put(n, val);
        boolean result = ((oldVal == null || val == null) && val != oldVal)
                || (!oldVal.equals(val));
        return result;
    }

    /**
     * Call the transfer function for node {@code node}, and set that node as
     * current node first.
     */
    protected TransferResult<A, S> callTransferFunction(Node node,
            TransferInput<A, S> store) {

        if (node.isLValue()) {
            // TODO: should the default behavior be to return either a regular
            // transfer result or a conditional transfer result (depending on
            // store.hasTwoStores()), or is the following correct?
            return new RegularTransferResult<A, S>(null,
                    store.getRegularStore());
        }
        store.node = node;
        currentNode = node;
        TransferResult<A, S> transferResult = node.accept(transferFunction,
                store);
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
                    finalLocalValues.put(elem, transferResult.getResultValue());
                }
            }
        }
        return transferResult;
    }

    /** Initialize the analysis with a new control flow graph. */
    protected void init(ControlFlowGraph cfg) {
        this.cfg = cfg;
        thenStores = new IdentityHashMap<>();
        elseStores = new IdentityHashMap<>();
        inputs = new IdentityHashMap<>();
        storesAtReturnStatements = new IdentityHashMap<>();
        worklist = new Worklist();
        nodeValues = new IdentityHashMap<>();
        finalLocalValues = new HashMap<>();
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
        } else {
            // nothing to do
        }
        S initialStore = transferFunction.initialStore(underlyingAST, parameters);
        Block entry = cfg.getEntryBlock();
        thenStores.put(entry, initialStore);
        elseStores.put(entry, initialStore);
        inputs.put(entry, new TransferInput<>(null, this, initialStore, initialStore));
    }

    /**
     * Add a basic block to the worklist. If <code>b</code> is already present,
     * the method does nothing.
     */
    protected void addToWorklist(Block b) {
        // TODO: use a more efficient way to check if b is already present
        if (!worklist.contains(b)) {
            worklist.add(b);
        }
    }

    /**
     * Add a store before the basic block <code>b</code> by merging with the
     * existing stores for that location.
     */
    protected void addStoreBefore(Block b, Node node, S s, Store.Kind kind,
            boolean addBlockToWorklist) {
        S thenStoreBefore = getStoreBefore(b, Store.Kind.THEN);
        S elseStoreBefore = getStoreBefore(b, Store.Kind.ELSE);

        switch (kind) {
        case THEN:
            // Update the then store
            if (thenStoreBefore == null) {
                System.out.println("addStoreBefore #1");
                thenStores.put(b, s);
                if (elseStoreBefore != null) {
                    System.out.println("addStoreBefore #2");
                    inputs.put(b, new TransferInput<>(node, this, s, elseStoreBefore));
                    addBlockToWorklist = true;
                }
            } else {
                System.out.println("addStoreBefore #3");
                S newThenStoreBefore = thenStoreBefore.leastUpperBound(s);
                if (!thenStoreBefore.equals(newThenStoreBefore)) {
                    System.out.println("addStoreBefore #4");
                    thenStores.put(b, s);
                    if (elseStoreBefore != null) {
                        System.out.println("addStoreBefore #5");
                        inputs.put(b, new TransferInput<>(node, this, newThenStoreBefore, elseStoreBefore));
                        addBlockToWorklist = true;
                    }
                }
            }
            break;
        case ELSE:
            // Update the else store
            if (elseStoreBefore == null) {
                System.out.println("addStoreBefore #6");
                elseStores.put(b, s);
                if (thenStoreBefore != null) {
                    System.out.println("addStoreBefore #7");
                    inputs.put(b, new TransferInput<>(node, this, thenStoreBefore, s));
                    addBlockToWorklist = true;
                }
            } else {
                System.out.println("addStoreBefore #8");
                S newElseStoreBefore = elseStoreBefore.leastUpperBound(s);
                if (!elseStoreBefore.equals(newElseStoreBefore)) {
                    System.out.println("addStoreBefore #9");
                    elseStores.put(b, s);
                    if (thenStoreBefore != null) {
                        System.out.println("addStoreBefore #10");
                        inputs.put(b, new TransferInput<>(node, this, thenStoreBefore, newElseStoreBefore));
                        addBlockToWorklist = true;
                    }
                }
            }
            break;
        case BOTH:
            if (thenStoreBefore == elseStoreBefore) {
                // Currently there is only one regular store
                if (thenStoreBefore == null) {
                    System.out.println("addStoreBefore #11");
                    thenStores.put(b, s);
                    elseStores.put(b, s);
                    inputs.put(b, new TransferInput<>(node, this, s));
                    addBlockToWorklist = true;
                } else {
                    System.out.println("addStoreBefore #12");
                    S newStoreBefore = thenStoreBefore.leastUpperBound(s);
                    if (!thenStoreBefore.equals(newStoreBefore)) {
                        System.out.println("addStoreBefore #13");
                        thenStores.put(b, newStoreBefore);
                        elseStores.put(b, newStoreBefore);
                        inputs.put(b, new TransferInput<>(node, this, newStoreBefore));
                        addBlockToWorklist = true;
                    }
                }
            } else {
                System.out.println("addStoreBefore #14");
                S newThenStoreBefore = null;
                S newElseStoreBefore = null;
                boolean storeChanged = false;
                if (thenStoreBefore == null) {
                    System.out.println("addStoreBefore #15");
                    newThenStoreBefore = s;
                    thenStores.put(b, newThenStoreBefore);
                    storeChanged = true;
                } else {
                    System.out.println("addStoreBefore #16");
                    newThenStoreBefore = thenStoreBefore.leastUpperBound(s);
                    if (!thenStoreBefore.equals(newThenStoreBefore)) {
                        System.out.println("addStoreBefore #17");
                        thenStores.put(b, newThenStoreBefore);
                        storeChanged = true;
                    }
                }

                if (elseStoreBefore == null) {
                    System.out.println("addStoreBefore #18");
                    newElseStoreBefore = s;
                    elseStores.put(b, newElseStoreBefore);
                    storeChanged = true;
                } else {
                    System.out.println("addStoreBefore #19");
                    newElseStoreBefore = elseStoreBefore.leastUpperBound(s);
                    if (!elseStoreBefore.equals(newElseStoreBefore)) {
                        System.out.println("addStoreBefore #20");
                        elseStores.put(b, newElseStoreBefore);
                        storeChanged = true;
                    }
                }

                if (storeChanged) {
                    System.out.println("addStoreBefore #21");
                    inputs.put(b, new TransferInput<>(node, this, newThenStoreBefore, newElseStoreBefore));
                    addBlockToWorklist = true;
                }
            }
        }
        if (addBlockToWorklist) {
            addToWorklist(b);
        }
    }

    /**
     * A worklist that keeps track of blocks that still needs to be processed.
     * The object implements a priority queue where blocks with the smallest
     * number of incoming edges (from blocks that are also in the queue) are
     * processed first. If the number of incoming edges is the same, then the
     * block added to the worklist first is processed first.
     */
    protected static class Worklist {

        /**
         * A wrapper class for a block that tracks the in-edge count as well as
         * an index (i.e., the creation time) for sorting in the priority queue.
         */
        protected static class Item implements Comparable<Item> {

            public final Block block;
            public int inEdgeCount = 0;
            public final int index;
            /** Static variable holding the index of the next item. */
            protected static int currentIndex = 0;

            public Item(Block block) {
                this.block = block;
                index = (currentIndex++);
            }

            @Override
            public int compareTo(Item o) {
                if (inEdgeCount == o.inEdgeCount) {
                    return index - o.index;
                }
                return inEdgeCount - o.inEdgeCount;
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof Item)) {
                    return false;
                }
                return block.equals(((Item) obj).block);
            }

            @Override
            public int hashCode() {
                return block.hashCode();
            }

            @Override
            public String toString() {
                return "" + index;
            }
        }

        /** The backing priority queue. */
        protected PriorityQueue<Item> queue = new PriorityQueue<Item>();

        /** Map for all blocks in the worklist to their item. */
        protected Map<Block, Item> lookupMap = new IdentityHashMap<>();

        public boolean isEmpty() {
            return queue.isEmpty();
        }

        public boolean contains(Block o) {
            return lookupMap.containsKey(o);
        }

        public void add(Block block) {
            Item item = new Item(block);
            lookupMap.put(block, item);
            // Update inEdgeCounts.
            for (Block succ : successors(block)) {
                if (lookupMap.containsKey(succ)) {
                    Item i = lookupMap.get(succ);
                    // Remove and re-add the object to let the priority queue
                    // know about the inEdgeCount update.
                    queue.remove(i);
                    i.inEdgeCount++;
                    queue.add(i);
                }
            }
            queue.add(item);
        }

        public Block poll() {
            Item head = queue.poll();
            if (head == null) {
                return null;
            }
            Block block = head.block;
            lookupMap.remove(block);
            // Update inEdgeCounts.
            for (Block succ : successors(block)) {
                if (lookupMap.containsKey(succ)) {
                    Item item = lookupMap.get(succ);
                    // Remove and re-add the object to let the priority queue
                    // know about the inEdgeCount update.
                    queue.remove(item);
                    item.inEdgeCount--;
                    queue.add(item);
                }
            }
            return block;
        }

        /** Returns the list of all successors of a given {@link Block}. */
        public static List<Block> successors(Block block) {
            List<Block> result = new ArrayList<>();
            if (block instanceof ConditionalBlock) {
                result.add(((ConditionalBlock) block).getThenSuccessor());
                result.add(((ConditionalBlock) block).getElseSuccessor());
            } else if (block instanceof SingleSuccessorBlock) {
                result.add(((SingleSuccessorBlock) block).getSuccessor());
            } else if (block instanceof ExceptionBlock) {
                result.add(((ExceptionBlock) block).getSuccessor());
                // Exceptional successors may contain duplicates
                for (Set<Block> exceptionSuccSet : ((ExceptionBlock) block)
                         .getExceptionalSuccessors().values()) {
                    for (Block exceptionSucc : exceptionSuccSet) {
                        if (!result.contains(exceptionSucc)) {
                            result.add(exceptionSucc);
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public String toString() {
            List<Item> items = new ArrayList<>();
            for (Item e : queue) {
                items.add(e);
            }
            return "Worklist(" + items + ")";
        }
    }

    /**
     * Read the {@link TransferInput} for a particular basic block (or {@code null} if
     * none exists yet).
     */
    public/* @Nullable */TransferInput<A, S> getInput(Block b) {
        return getInputBefore(b);
    }

    /**
     * @return The transfer input corresponding to the location right before the basic
     *         block <code>b</code>.
     */
    protected/* @Nullable */TransferInput<A, S> getInputBefore(Block b) {
        return inputs.get(b);
    }

    /**
     * @return The store corresponding to the location right before the basic
     *         block <code>b</code>.
     */
    protected/* @Nullable */S getStoreBefore(Block b, Store.Kind kind) {
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
     * Read the {@link Store} for a particular basic block from a map of stores
     * (or {@code null} if none exists yet).
     */
    protected static <S> /* @Nullable */S readFromStore(Map<Block, S> stores,
            Block b) {
        return stores.get(b);
    }

    /** Is the analysis currently running? */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * @return The abstract value for {@link Node} {@code n}, or {@code null} if
     *         no information is available. Note that if the analysis has not
     *         finished yet, this value might not represent the final value for
     *         this node.
     */
    public/* @Nullable */A getValue(Node n) {
        if (isRunning) {
            // we do not yet have a dataflow fact about the current node
            if (currentNode == n
                    || (currentTree != null && currentTree == n.getTree())) {
                return null;
            }
            // check that 'n' is a subnode of 'node'. Check immediate operands
            // first for efficiency.
            assert currentNode != null;
            assert !n.isLValue() : "Did not expect an lvalue, but got " + n;
            if (!(currentNode != n && (currentNode.getOperands().contains(n) || currentNode
                    .getTransitiveOperands().contains(n)))) {
                return null;
            }
            return nodeValues.get(n);
        }
        return nodeValues.get(n);
    }

    /**
     * @return The abstract value for {@link Tree} {@code t}, or {@code null} if
     *         no information is available. Note that if the analysis has not
     *         finished yet, this value might not represent the final value for
     *         this node.
     */
    public/* @Nullable */A getValue(Tree t) {
        // we do not yet have a dataflow fact about the current node
        if (t == currentTree) {
            return null;
        }
        Node nodeCorrespondingToTree = getNodeForTree(t);
        if (nodeCorrespondingToTree == null || nodeCorrespondingToTree.isLValue()) {
            return null;
        }
        return getValue(nodeCorrespondingToTree);
    }

    /**
     * Get the {@link Node} for a given {@link Tree}.
     */
    public Node getNodeForTree(Tree t) {
        return cfg.getNodeCorrespondingToTree(t);
    }

    /**
     * Get the {@link MethodTree} of the current CFG if the argument {@link Tree} maps
     * to a {@link Node} in the CFG or null otherwise.
     */
    public/* @Nullable */MethodTree getContainingMethod(Tree t) {
        return cfg.getContainingMethod(t);
    }

    /**
     * Get the {@link ClassTree} of the current CFG if the argument {@link Tree} maps
     * to a {@link Node} in the CFG or null otherwise.
     */
    public/* @Nullable */ClassTree getContainingClass(Tree t) {
        return cfg.getContainingClass(t);
    }

    public List<Pair<ReturnNode, TransferResult<A, S>>> getReturnStatementStores() {
        List<Pair<ReturnNode, TransferResult<A, S>>> result = new ArrayList<>();
        for (ReturnNode returnNode : cfg.getReturnNodes()) {
            TransferResult<A, S> store = storesAtReturnStatements
                    .get(returnNode);
            result.add(Pair.of(returnNode, store));
        }
        return result;
    }

    public AnalysisResult<A, S> getResult() {
        assert !isRunning;
        IdentityHashMap<Tree, Node> treeLookup = cfg.getTreeLookup();
        return new AnalysisResult<>(nodeValues, inputs, treeLookup, finalLocalValues);
    }

    /**
     * @return The regular exit store, or {@code null}, if there is no such
     *         store (because the method cannot exit through the regular exit
     *         block).
     */
    public/* @Nullable */S getRegularExitStore() {
        SpecialBlock regularExitBlock = cfg.getRegularExitBlock();
        if (inputs.containsKey(regularExitBlock)) {
            S regularExitStore = inputs.get(regularExitBlock).getRegularStore();
            return regularExitStore;
        } else {
            return null;
        }
    }

    public S getExceptionalExitStore() {
        S exceptionalExitStore = inputs.get(cfg.getExceptionalExitBlock())
                .getRegularStore();
        return exceptionalExitStore;
    }
}
