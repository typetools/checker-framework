package dataflow.analysis;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import javacutils.Pair;

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
import dataflow.cfg.node.LocalVariableNode;
import dataflow.cfg.node.Node;
import dataflow.cfg.node.ReturnNode;

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
     * The stores before every basic blocks (assumed to be 'no information' if
     * not present).
     */
    protected IdentityHashMap<Block, TransferInput<A, S>> stores;

    /**
     * The stores after every return statement.
     */
    protected IdentityHashMap<ReturnNode, TransferResult<A, S>> storesAtReturnStatements;

    /** The worklist used for the fix-point iteration. */
    protected Worklist worklist;

    /** Abstract values of nodes. */
    protected IdentityHashMap<Node, A> nodeValues;

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
     * The current store when the analysis is running.
     */
    protected TransferInput<A, S> currentStore;

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
                TransferInput<A, S> storeBefore = getStoreBefore(rb);
                currentStore = storeBefore.copy();
                TransferResult<A, S> transferResult = null;
                for (Node n : rb.getContents()) {
                    transferResult = callTransferFunction(n, currentStore);
                    A val = transferResult.getResultValue();
                    if (val != null) {
                        nodeValues.put(n, val);
                    }
                    currentStore = new TransferInput<>(n, this, transferResult);
                }
                // loop will run at least one, making transferResult non-null

                // propagate store to successors
                Block succ = rb.getSuccessor();
                assert succ != null : "regular basic block without non-exceptional successor unexpected";
                addStoreBefore(succ, currentStore);
                break;
            }

            case EXCEPTION_BLOCK: {
                ExceptionBlock eb = (ExceptionBlock) b;

                // apply transfer function to content
                TransferInput<A, S> storeBefore = getStoreBefore(eb);
                currentStore = storeBefore.copy();
                Node node = eb.getNode();
                TransferResult<A, S> transferResult = callTransferFunction(
                        node, currentStore);
                A val = transferResult.getResultValue();
                if (val != null) {
                    nodeValues.put(node, val);
                }

                // propagate store to successor
                Block succ = eb.getSuccessor();
                if (succ != null) {
                    currentStore = new TransferInput<>(node, this, transferResult);
                    addStoreBefore(succ, currentStore);
                }

                // propagate store to exceptional successors
                for (Entry<TypeMirror, Block> e : eb.getExceptionalSuccessors()
                        .entrySet()) {
                    Block exceptionSucc = e.getValue();
                    TypeMirror cause = e.getKey();
                    S exceptionalStore = transferResult
                            .getExceptionalStore(cause);
                    if (exceptionalStore != null) {
                        addStoreBefore(exceptionSucc, new TransferInput<>(node,
                                this, exceptionalStore));
                    } else {
                        addStoreBefore(exceptionSucc, storeBefore.copy());
                    }
                }
                break;
            }

            case CONDITIONAL_BLOCK: {
                ConditionalBlock cb = (ConditionalBlock) b;

                // get store before
                TransferInput<A, S> storeBefore = getStoreBefore(cb);
                TransferInput<A, S> store = storeBefore.copy();

                // propagate store to successor
                Block thenSucc = cb.getThenSuccessor();
                Block elseSucc = cb.getElseSuccessor();
                addStoreBefore(thenSucc,
                        new TransferInput<>(null, this, store.getThenStore()));
                addStoreBefore(elseSucc,
                        new TransferInput<>(null, this, store.getElseStore()));
                break;
            }

            case SPECIAL_BLOCK: {
                // special basic blocks are empty and cannot throw exceptions,
                // thus there is no need to perform any analysis.
                SpecialBlock sb = (SpecialBlock) b;
                Block succ = sb.getSuccessor();
                if (succ != null) {
                    addStoreBefore(succ, getStoreBefore(b));
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
        return transferResult;
    }

    /** Initialize the analysis with a new control flow graph. */
    protected void init(ControlFlowGraph cfg) {
        this.cfg = cfg;
        stores = new IdentityHashMap<>();
        storesAtReturnStatements = new IdentityHashMap<>();
        worklist = new Worklist();
        nodeValues = new IdentityHashMap<>();
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
        stores.put(cfg.getEntryBlock(), new TransferInput<>(null, this,
                transferFunction.initialStore(underlyingAST, parameters)));
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
     * existing store for that location.
     */
    protected void addStoreBefore(Block b, TransferInput<A, S> s) {
        TransferInput<A, S> storeBefore = getStoreBefore(b);
        TransferInput<A, S> newStoreBefore;
        if (storeBefore == null) {
            newStoreBefore = s;
        } else {
            newStoreBefore = storeBefore.leastUpperBound(s);
        }
        stores.put(b, newStoreBefore);
        if (storeBefore == null || !storeBefore.equals(newStoreBefore)) {
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
                result.addAll(((ExceptionBlock) block)
                        .getExceptionalSuccessors().values());
            }
            return result;
        }
    }

    /**
     * @return The store corresponding to the location right before the basic
     *         block <code>b</code>.
     */
    protected/* @Nullable */TransferInput<A, S> getStoreBefore(Block b) {
        return readFromStore(stores, b);
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
     * Read the {@link Store} for a particular basic block (or {@code null} if
     * none exists yet).
     */
    public/* @Nullable */TransferInput<A, S> getStore(Block b) {
        return readFromStore(stores, b);
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
        return new AnalysisResult<>(nodeValues, stores, treeLookup);
    }

    /**
     * @return The regular exit store, or {@code null}, if there is no such
     *         store (because the method cannot exit through the regular exit
     *         block).
     */
    public/* @Nullable */S getRegularExitStore() {
        SpecialBlock regularExitBlock = cfg.getRegularExitBlock();
        if (stores.containsKey(regularExitBlock)) {
            S regularExitStore = stores.get(regularExitBlock).getRegularStore();
            return regularExitStore;
        } else {
            return null;
        }
    }

    public S getExceptionalExitStore() {
        S exceptionalExitStore = stores.get(cfg.getExceptionalExitBlock())
                .getRegularStore();
        return exceptionalExitStore;
    }
}
