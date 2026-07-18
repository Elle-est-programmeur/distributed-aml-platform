package com.tradesentry.agent.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A minimal, framework-free port of LangGraph's core execution model to typed Java.
 *
 * <p>A graph is a set of named {@link Node}s connected by edges. A single immutable state value
 * of type {@code S} is threaded through the nodes: each node receives the current state and
 * returns a new one. Execution starts at the entry point and follows edges until it reaches
 * {@link #END}.
 *
 * <p>Two kinds of edges exist:
 * <ul>
 *   <li><b>Fixed edges</b> ({@link #addEdge}) always route from one node to a fixed successor.</li>
 *   <li><b>Conditional edges</b> ({@link #addConditionalEdge}) compute the next node from the
 *       current state at runtime. These are what enable both <em>branching</em> (pick one of
 *       several successors) and <em>cycles</em> (route back to an earlier node).</li>
 * </ul>
 *
 * <p>Because conditional edges allow cycles, a buggy router could loop forever. The
 * {@code maxSteps} cap is the safety valve: it guarantees that even a cyclic graph always
 * terminates, aborting with an exception rather than spinning indefinitely.
 *
 * @param <S> the type of state threaded through the graph
 */
public class StateGraph<S> {

    public static final String END = "__end__";

    private final Map<String, Node<S>> nodes = new HashMap<>();
    private final Map<String, String> edges = new HashMap<>();
    private final Map<String, Function<S, String>> conditionalEdges = new HashMap<>();
    private String entryPoint;
    private final int maxSteps;

    public StateGraph() {
        this(50);
    }

    public StateGraph(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public StateGraph<S> addNode(String name, Node<S> node) {
        if (nodes.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate node: " + name);
        }
        nodes.put(name, node);
        return this;
    }

    public StateGraph<S> addEdge(String from, String to) {
        edges.put(from, to);
        return this;
    }

    public StateGraph<S> addConditionalEdge(String from, Function<S, String> router) {
        conditionalEdges.put(from, router);
        return this;
    }

    public StateGraph<S> setEntryPoint(String name) {
        this.entryPoint = name;
        return this;
    }

    public S invoke(S initialState) {
        if (entryPoint == null) {
            throw new IllegalStateException("No entry point set");
        }

        S state = initialState;
        String current = entryPoint;
        int steps = 0;

        while (!current.equals(END)) {
            if (steps++ >= maxSteps) {
                throw new IllegalStateException(
                        "Graph exceeded " + maxSteps + " steps — probable non-terminating cycle");
            }

            Node<S> node = nodes.get(current);
            if (node == null) {
                throw new IllegalStateException("No node registered for '" + current + "'");
            }

            state = node.apply(state);
            current = nextNode(current, state);
        }

        return state;
    }

    private String nextNode(String current, S state) {
        Function<S, String> router = conditionalEdges.get(current);
        if (router != null) {
            return router.apply(state);
        }
        return edges.getOrDefault(current, END);
    }
}
