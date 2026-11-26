package com.enterprise.ticket.response;

import java.time.Instant;
import lombok.Builder;

@Builder
public record TicketEscalationHistoryResponse(
    Long id,
    String ticketNumber,
    Long ruleId,
    String ruleName,
    Integer fromLevel,
    Integer toLevel,
    String reason,
    String escalatedByUsername,
    String previousTeamName,
    String newTeamName,
    String previousAgentUsername,
    String newAgentUsername,
    Instant createdAt) {}
