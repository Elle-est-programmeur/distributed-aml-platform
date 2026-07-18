package com.tradesentry.agent.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StateGraphTest {

    record TestState(int counter, String trail) {
        TestState visit(String node) {
            return new TestState(counter, trail.isEmpty() ? node : trail + ">" + node);
        }

        TestState inc() {
            return new TestState(counter + 1, trail);
        }
    }

    @Test
    void linearPathRunsNodesInOrder() {
        StateGraph<TestState> graph = new StateGraph<TestState>()
                .addNode("A", s -> s.visit("A"))
                .addNode("B", s -> s.visit("B"))
                .setEntryPoint("A")
                .addEdge("A", "B")
                .addEdge("B", StateGraph.END);

        TestState result = graph.invoke(new TestState(0, ""));

        assertEquals("A>B", result.trail());
    }

    @Test
    void conditionalEdgeRoutesOnState() {
        StateGraph<TestState> graph = new StateGraph<TestState>()
                .addNode("start", s -> s.visit("start"))
                .addNode("even", s -> s.visit("even"))
                .addNode("odd", s -> s.visit("odd"))
                .setEntryPoint("start")
                .addConditionalEdge("start", s -> s.counter() % 2 == 0 ? "even" : "odd")
                .addEdge("even", StateGraph.END)
                .addEdge("odd", StateGraph.END);

        assertEquals("start>even", graph.invoke(new TestState(2, "")).trail());
        assertEquals("start>odd", graph.invoke(new TestState(3, "")).trail());
    }

    @Test
    void boundedCycleLoopsThenExits() {
        StateGraph<TestState> graph = new StateGraph<TestState>()
                .addNode("work", TestState::inc)
                .addNode("done", s -> s.visit("done"))
                .setEntryPoint("work")
                .addConditionalEdge("work", s -> s.counter() < 3 ? "work" : "done")
                .addEdge("done", StateGraph.END);

        TestState result = graph.invoke(new TestState(0, ""));

        assertEquals(3, result.counter());
        assertEquals("done", result.trail());
    }

    @Test
    void runawayCycleIsAbortedByStepCap() {
        StateGraph<TestState> graph = new StateGraph<TestState>(10)
                .addNode("spin", s -> s)
                .setEntryPoint("spin")
                .addEdge("spin", "spin");

        assertThrows(IllegalStateException.class, () -> graph.invoke(new TestState(0, "")));
    }
}
