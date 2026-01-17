package com.agriprocurement.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record EventMetadata(
    @JsonProperty("correlationId") String correlationId,
    @JsonProperty("causationId") String causationId,
    @JsonProperty("userId") String userId
) {
    @JsonCreator
    public EventMetadata {
    }

    public static EventMetadata create(String correlationId, String userId) {
        return new EventMetadata(correlationId, null, userId);
    }

    public static EventMetadata createWithCausation(String correlationId, String causationId, String userId) {
        return new EventMetadata(correlationId, causationId, userId);
    }
}
