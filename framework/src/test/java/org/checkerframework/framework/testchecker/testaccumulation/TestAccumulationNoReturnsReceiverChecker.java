package org.checkerframework.framework.testchecker.testaccumulation;

import java.util.EnumSet;
import org.checkerframework.common.accumulation.AccumulationChecker;

/**
 * A test accumulation checker that implements a basic version of called-methods accumulation,
 * without returns receiver support, to test the pluggable alias analysis functionality.
 */
public class TestAccumulationNoReturnsReceiverChecker extends AccumulationChecker {

    /**
     * Get the alias analyses that this checker should employ.
     *
     * @return the alias analyses
     */
    @Override
    protected EnumSet<AliasAnalysis> createAliasAnalyses() {
        return EnumSet.noneOf(AliasAnalysis.class);
    }
}
