package com.woorifisa.won_card_channel_server.domain.ai.graph.dto.response;

import java.util.List;

public record GraphRelationResponse(
        String queryType,
        List<GraphNode> nodes,
        List<GraphLink> links,
        String summary
) {
    public record GraphNode(String id, String label, String value) {}

    public record GraphLink(String source, String target, String relation) {}
}