package com.templateai.sandbox.transaction;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.templateai.sandbox.card.Card;

/**
 * An authorization attempt against a card. Declines are stored, not thrown away — the decline
 * history is the product (disputes, fraud review, "why was I declined?") and the audit trail.
 */
@Entity
@Table(
        name = "transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_tx_idempotency_key", columnNames = "idempotency_key"),
        indexes = @Index(name = "ix_tx_card_created", columnList = "card_id, created_at"))
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    public enum Status { APPROVED, DECLINED }

    /** Machine-readable codes: clients branch on these, humans read the message. */
    public enum DeclineReason { CARD_FROZEN, CARD_CANCELLED, LIMIT_EXCEEDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** LAZY on purpose: nothing loads a Card just to serialize a transaction row. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    /** Minor units (cents), always positive here. A refund would be a separate credit record. */
    @Column(nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private DeclineReason declineReason;

    /**
     * Caller-supplied Idempotency-Key. Unique index makes the database, not application logic,
     * the thing that guarantees a retried charge cannot post twice.
     */
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
