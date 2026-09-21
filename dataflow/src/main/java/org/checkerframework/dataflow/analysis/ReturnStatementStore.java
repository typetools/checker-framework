package org.checkerframework.dataflow.analysis;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.ReturnNode;

/**
 * A return statement and the transfer result at it.
 *
 * @param <V> the abstract value type tracked by the analysis
 * @param <S> the store type used in the analysis
 * @param returnNode a return statement
 * @param transferResult the transfer result at {@code returnNode}, or null if {@code returnNode} is
 *     unreachable
 */
public record ReturnStatementStore<V extends AbstractValue<V>, S extends Store<S>>(
    ReturnNode returnNode, @Nullable TransferResult<V, S> transferResult) {}
