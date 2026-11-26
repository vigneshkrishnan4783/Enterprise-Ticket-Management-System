package com.enterprise.ticket.response;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.model.EscalationTriggerType;
import java.time.Instant;
import lombok.Builder;

@Builder
public record EscalationRuleResponse(
    Long id,
    String name,
    EscalationTriggerType triggerType,
    TicketPriority priority,
    Integer targetLevel,
    Long targetTeamId,
    String targetTeamName,
    Long targetAgentId,
    String targetAgentUsername,
    Boolean isActive,
    Instant createdAt) {}
