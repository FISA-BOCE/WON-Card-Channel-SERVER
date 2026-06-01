package com.woorifisa.won_card_channel_server.domain.sweep.dto.command;

public record InboxClaimResult(
        Long inboxEventId,
        boolean claimed,
        boolean alreadyProcessed
) {
    public static InboxClaimResult claimed(Long inboxEventId) {
        return new InboxClaimResult(inboxEventId, true, false);
    }

    public static InboxClaimResult alreadyProcessed(Long inboxEventId) {
        return new InboxClaimResult(inboxEventId, false, true);
    }

    public static InboxClaimResult notClaimed(Long inboxEventId) {
        return new InboxClaimResult(inboxEventId, false, false);
    }
}
