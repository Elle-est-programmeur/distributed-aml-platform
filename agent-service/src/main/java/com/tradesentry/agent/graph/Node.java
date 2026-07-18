package com.tradesentry.agent.graph;

/**
 * A single step in a {@link StateGraph}.
 *
 * <p>A node is a pure function from the current state to a <em>new</em> state. It must never
 * mutate the state it receives; instead it returns a fresh instance derived from the input.
 * Keeping nodes pure is what makes graph execution deterministic and safe to reason about.
 *
 * @param <S> the type of state threaded through the graph
 */
@FunctionalInterface
public interface Node<S> {

    /**
     * Computes the next state from the current one.
     *
     * @param state the current (immutable) state; must not be modified
     * @return a new state instance
     */
    S apply(S state);
}
