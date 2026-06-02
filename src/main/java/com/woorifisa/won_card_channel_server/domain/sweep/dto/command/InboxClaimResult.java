package com.woorifisa.won_card_channel_server.domain.sweep.dto.command;

public record InboxClaimResult(
        Long inboxEventId,
        InboxClaimStatus status
) {
    public static InboxClaimResult claimed(Long inboxEventId) {
        return new InboxClaimResult(inboxEventId, InboxClaimStatus.CLAIMED);
    }

    public static InboxClaimResult alreadyProcessed(Long inboxEventId) {
        return new InboxClaimResult(inboxEventId, InboxClaimStatus.ALREADY_PROCESSED);
    }

    public static InboxClaimResult notClaimed(Long inboxEventId) {
        return new InboxClaimResult(inboxEventId, InboxClaimStatus.NOT_CLAIMED);
    }

    public boolean claimed() {
        return status == InboxClaimStatus.CLAIMED;
    }

    public boolean alreadyProcessed() {
        return status == InboxClaimStatus.ALREADY_PROCESSED;
    }

    public enum InboxClaimStatus {
        CLAIMED,
        ALREADY_PROCESSED,
        NOT_CLAIMED
    }
}
