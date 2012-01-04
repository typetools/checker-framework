package checkers.flow.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import checkers.flow.cfg.ControlFlowGraph;
import checkers.flow.cfg.block.Block;
import checkers.flow.cfg.block.ConditionalBlock;
import checkers.flow.cfg.block.RegularBlock;
import checkers.flow.cfg.block.SpecialBlock;
import checkers.flow.cfg.node.Node;

public class Analysis<A extends AbstractValue, S extends Store<A>> {

	/** The transfer function for regular nodes. */
	protected TransferFunction<A, S> regularTransfer;

	/**
	 * The transfer function for conditional nodes to compute the result along
	 * the 'true' edge in the control flow graph.
	 */
	protected TransferFunction<A, S> condTrueTransfer;

	/**
	 * The transfer function for conditional nodes to compute the result along
	 * the 'false' edge in the control flow graph.
	 */
	protected TransferFunction<A, S> condFalseTransfer;

	/** The control flow graph to perform the analysis on. */
	protected ControlFlowGraph cfg;

	/**
	 * The stores before every basic blocks (assumed to be 'top' if not
	 * present).
	 */
	protected Map<Block, S> stores;

	/** The worklist used for the fixpoint iteration. */
	protected Stack<Block> worklist;

	/**
	 * Construct an object that can perform a dataflow analysis over a control
	 * flow graph, given a single transfer function (information along
	 * 'true'/'false' edges of conditionals is the same).
	 */
	public Analysis(TransferFunction<A, S> transfer) {
		this.regularTransfer = transfer;
		this.condTrueTransfer = transfer;
		this.condFalseTransfer = transfer;
	}

	/**
	 * Construct an object that can perform a dataflow analysis over a control
	 * flow graph, given a set of transfer functions.
	 */
	public Analysis(TransferFunction<A, S> regularTransfer,
			TransferFunction<A, S> condTrueTransfer,
			TransferFunction<A, S> condFalseTransfer) {
		this.regularTransfer = regularTransfer;
		this.condTrueTransfer = condTrueTransfer;
		this.condFalseTransfer = condFalseTransfer;
	}

	/**
	 * Perform the actual analysis. Should only be called once after the object
	 * has been created.
	 * 
	 * @param cfg
	 */
	@SuppressWarnings("unchecked")
	public void performAnalysis(ControlFlowGraph cfg) {
		init(cfg);

		while (!worklist.isEmpty()) {
			Block b = worklist.pop();

			// this basic block does not have any side-effects for the
			// exceptional successors
			for (Entry<Class<? extends Throwable>, Block> e : b
					.getExceptionalSuccessors().entrySet()) {
				Block succ = e.getValue();
				addStoreBefore(succ, getStoreBefore(b));
			}

			switch (b.getType()) {
			case REGULAR_BLOCK: {
				RegularBlock rb = (RegularBlock) b;

				// apply transfer function to contents
				S store = (S) getStoreBefore(rb).copy();
				for (Node n : rb.getContents()) {
					store = n.accept(regularTransfer, store);
				}

				// propagate store to successor
				Block succ = rb.getSuccessor();
				addStoreBefore(succ, store);
				break;
			}

			case CONDITIONAL_BLOCK: {
				ConditionalBlock cb = (ConditionalBlock) b;

				// apply transfer function to compute 'then' store
				S thenStore = (S) getStoreBefore(cb).copy();
				thenStore = cb.getCondition().accept(condTrueTransfer,
						thenStore);

				// apply transfer function to compute 'else' store
				S elseStore;
				if (condTrueTransfer != condFalseTransfer) {
					elseStore = (S) getStoreBefore(cb).copy();
					elseStore = cb.getCondition().accept(condFalseTransfer,
							elseStore);
				} else {
					// optimization: if the two transfer function are the same,
					// don't compute the resulting store twice.
					elseStore = (S) thenStore.copy();
				}

				// propagate store to successor
				Block thenSucc = cb.getThenSuccessor();
				Block elseSucc = cb.getElseSuccessor();
				addStoreBefore(thenSucc, thenStore);
				addStoreBefore(elseSucc, elseStore);
				break;
			}

			case SPECIAL_BLOCK: {
				// special basic blocks are empty, thus there is no need to
				// perform any analysis.
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
	}

	/** Initialize the analysis with a new control flow graph. */
	protected void init(ControlFlowGraph cfg) {
		this.cfg = cfg;
		stores = new HashMap<>();
		worklist = new Stack<>();
		addToWorklist(cfg.getEntryBlock());
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
	protected boolean addStoreBefore(Block b, S s) {
		S storeBefore = getStoreBefore(b);
		@SuppressWarnings("unchecked")
		S newStoreBefore = (S) storeBefore.leastUpperBound(s);
		setStoreBefore(b, newStoreBefore);
		return !storeBefore.equals(newStoreBefore);
	}

	/** Change the store before basic block <code>b</code> to <code>s</code>. */
	protected void setStoreBefore(Block b, S s) {
		stores.put(b, s);
	}

	/**
	 * @return The store corresponding to the location right before the basic
	 *         block <code>b</code>.
	 */
	protected S getStoreBefore(Block b) {
		return readFromStore(stores, b);
	}

	/**
	 * Read the {@link Store} for a particular basic block from a map of stores
	 * (handles default stores).
	 */
	public static <S> S readFromStore(Map<Block, S> stores, Block b) {
		if (stores.containsKey(b)) {
			return stores.get(b);
		}
		// return new S();
		return (S) new DefaultStore<>(); // TODO: how do we instantiate S?
	}

	public Map<Block, S> getStores() {
		return stores;
	}

}
