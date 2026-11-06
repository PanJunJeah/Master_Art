package com.pan.master_art.agent.tool;

import com.pan.master_art.agent.orchestration.AgentTurnState;
import org.springframework.ai.chat.model.ToolContext;

/**
 * 工具层公共支撑
 */
final class AgentToolSupport {

    private AgentToolSupport() {
    }

    static AgentTurnState stateOf(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object state = toolContext.getContext().get(AgentTurnState.TOOL_CONTEXT_KEY);
        return state instanceof AgentTurnState ? (AgentTurnState) state : null;
    }

    static void trace(AgentTurnState state, String toolName, boolean success, String message) {
        if (state != null) {
            state.recordTrace(toolName, success, message);
        }
    }

    static String abbreviate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}
