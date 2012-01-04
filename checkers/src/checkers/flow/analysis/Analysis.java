package checkers.flow.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import checkers.flow.cfg.block.Block;
import checkers.flow.cfg.block.ConditionalBlock;
import checkers.flow.cfg.block.RegularBlock;
import checkers.flow.cfg.block.SpecialBlock;
import checkers.flow.cfg.node.Node;

public class Analysis<A extends AbstractValue, S extends Store<A>, L extends Lattice<A, S>> {

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
	protected Block cfg;

	/** The lattice of the abstract values. */
	protected L lattice;

	/**
	 * The stores before every basic blocks (assumed to be 'top' if not
	 * present).
	 */
	protected Map<Block, S> beforeStores;

	/**
	 * The stores after every basic blocks (assumed to be 'top' if not present).
	 */
	protected Map<Block, S> afterStores;

	/** The worklist used for the fixpoint iteration. */
	protected Stack<Block> worklist;

	/**
	 * Construct an object that can perform a dataflow analysis over the control
	 * flow graph <code>cfg</code>, given a set of transfer functions.
	 */
	public Analysis(TransferFunction<A, S> regularTransfer,
			TransferFunction<A, S> condTrueTransfer,
			TransferFunction<A, S> condFalseTransfer, Block cfg) {
		this.regularTransfer = regularTransfer;
		this.condTrueTransfer = condTrueTransfer;
		this.condFalseTransfer = condFalseTransfer;
		this.cfg = cfg;
		beforeStores = new HashMap<>();
		afterStores = new HashMap<>();
		worklist = new Stack<>();
	}

	/**
	 * Perform the actual analysis. Should only be called once after the object
	 * has been created.
	 */
	public void performAnalysis() {
		addToWorklist(cfg);

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
				@SuppressWarnings("unchecked")
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
				@SuppressWarnings("unchecked")
				S thenStore = (S) getStoreBefore(cb).copy();
				thenStore = cb.getCondition().accept(condTrueTransfer,
						thenStore);

				// apply transfer function to compute 'else' store
				@SuppressWarnings("unchecked")
				S elseStore = (S) getStoreBefore(cb).copy();
				elseStore = cb.getCondition().accept(condFalseTransfer,
						elseStore);

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
	 * Add a store after the basic block <code>b</code>.
	 * 
	 * @return true iff the store changed.
	 */
	protected void addStoreAfter(Block b, S s) {
		S storeAfter = getStoreAfter(b);
		S newStoreAfter = lattice.leastUpperBound(storeAfter, s);
		setStoreBefore(b, newStoreAfter);

		// add the basic block to the worklist if necessary
		if (!storeAfter.equals(newStoreAfter)) {
			addToWorklist(b);
		}
	}

	/**
	 * Add a store before the basic block <code>b</code>.
	 * 
	 * @return true iff the store changed.
	 */
	protected boolean addStoreBefore(Block b, S s) {
		S storeBefore = getStoreBefore(b);
		S newStoreBefore = lattice.leastUpperBound(storeBefore, s);
		setStoreBefore(b, newStoreBefore);
		return !storeBefore.equals(newStoreBefore);
	}

	/** Change the store before basic block <code>b</code> to <code>s</code>. */
	protected void setStoreBefore(Block b, S s) {
		beforeStores.put(b, s);
	}

	/**
	 * @return The store before the basic block <code>b</code>.
	 */
	protected S getStoreBefore(Block b) {
		if (beforeStores.containsKey(b)) {
			return beforeStores.get(b);
		}
		// return new S();
		return null; // TODO: how do we instantiate S?
	}

	/**
	 * @return The store after the basic block <code>b</code>.
	 */
	protected S getStoreAfter(Block b) {
		if (afterStores.containsKey(b)) {
			return afterStores.get(b);
		}
		// return new S();
		return null; // TODO: how do we instantiate S?
	}
}
