package com.tradesentry.agent.graph;

import com.tradesentry.agent.nodes.InvestigationNodes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assembles the investigation {@link StateGraph} from the individual nodes.
 *
 * <p>The graph is mostly linear (enrich → retrieveCases → assess → decide) but {@code assess} has
 * a conditional edge that can loop back through {@code investigateDeeper}, capped by
 * {@link #MAX_INVESTIGATION_DEPTH}, before finally routing to {@code decide}.
 */
@Configuration
public class InvestigationGraphFactory {

    public static final int MAX_INVESTIGATION_DEPTH = 2;

    @Bean
    public StateGraph<InvestigationState> investigationGraph(InvestigationNodes nodes) {
        return new StateGraph<InvestigationState>()
                .addNode("enrich", nodes.enrich())
                .addNode("retrieveCases", nodes.retrieveCases())
                .addNode("assess", nodes.assess())
                .addNode("investigateDeeper", nodes.investigateDeeper())
                .addNode("decide", nodes.decide())
                .setEntryPoint("enrich")
                .addEdge("enrich", "retrieveCases")
                .addEdge("retrieveCases", "assess")
                .addConditionalEdge("assess", state ->
                        state.isBorderline() && state.investigationDepth() < MAX_INVESTIGATION_DEPTH
                                ? "investigateDeeper" : "decide")
                .addEdge("investigateDeeper", "assess")     // the cycle
                .addEdge("decide", StateGraph.END);
    }
}
