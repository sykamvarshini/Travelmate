package com.travelmate.dto;

import java.util.List;

public class ChatResponse {
    private String reply;
    private String modelUsed;
    private List<String> suggestions;
    private Boolean fallbackUsed;

    public ChatResponse() {}
    public ChatResponse(String reply, String modelUsed, List<String> suggestions, Boolean fallbackUsed) {
        this.reply = reply;
        this.modelUsed = modelUsed;
        this.suggestions = suggestions;
        this.fallbackUsed = fallbackUsed;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public Boolean getFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(Boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
}
