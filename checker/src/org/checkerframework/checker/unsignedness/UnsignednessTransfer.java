package org.checkerframework.checker.unsignedness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.flow.CFStore;

public class UnsignednessTransfer extends
        CFAbstractTransfer<CFValue, CFStore, UnsignednessTransfer> {

    public UnsignednessTransfer( UnsignednessAnalysis analysis ) {
        super( analysis );
    }
}