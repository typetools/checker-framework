package dataflow.cfg.block;

import dataflow.analysis.Store;
import dataflow.cfg.node.Node;

import javacutils.Pair;

/**
 * Represents a conditional basic block that contains exactly one boolean
 * {@link Node}.
 *
 * @author Stefan Heule
 *
 */
public interface ConditionalBlock extends Block {

    /**
     * @return The entry block of the then branch.
     */
    Block getThenSuccessor();

    /**
     * @return The entry block of the else branch.
     */
    Block getElseSuccessor();

    /**
     * @return The source and destination stores for information flowing from
     * this block to its then successor.
     */
    Pair<Store.Kind, Store.Kind> getThenStoreFlow();

    /**
     * @return The source and destination stores for information flowing from
     * this block to its else successor.
     */
    Pair<Store.Kind, Store.Kind> getElseStoreFlow();
}
