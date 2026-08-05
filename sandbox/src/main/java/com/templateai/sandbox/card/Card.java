package com.templateai.sandbox.card;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
public class Card {

    public enum Status { ACTIVE, FROZEN, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cardholderName;

    /** Last four digits only. A real system never stores or logs a full PAN — that is PCI scope. */
    @Column(nullable = false, length = 4)
    private String last4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    /** Spend limit in minor units (cents). Money is a long, never a double. */
    @Column(nullable = false)
    private long spendLimitMinor;

    /** ISO-4217 code. Stored next to every amount so a value is never ambiguous. */
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Instant createdAt;
}
