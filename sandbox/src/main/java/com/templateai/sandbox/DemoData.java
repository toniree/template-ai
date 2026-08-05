package com.templateai.sandbox;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.templateai.sandbox.card.CardDtos.CardResponse;
import com.templateai.sandbox.card.CardDtos.CreateCardRequest;
import com.templateai.sandbox.card.CardService;
import com.templateai.sandbox.transaction.TransactionDtos.AuthorizeRequest;
import com.templateai.sandbox.transaction.TransactionService;

/**
 * Seeds the in-memory database so the UI has something to show the moment it loads. Runs on the
 * {@code h2} profile only — never against a real datastore, never in tests.
 *
 * <p>Goes through the services, not the repositories, so the seed exercises the same rules as a
 * real request (and one seeded charge deliberately trips the limit to show a DECLINED row).
 */
@Component
@Profile("h2")
public class DemoData implements CommandLineRunner {

    private final CardService cards;
    private final TransactionService transactions;

    public DemoData(CardService cards, TransactionService transactions) {
        this.cards = cards;
        this.transactions = transactions;
    }

    @Override
    public void run(String... args) {
        if (!cards.list().isEmpty()) {
            return;
        }

        CardResponse engineering = cards.create(new CreateCardRequest("Ada Lovelace", "4242", 250_000L, "USD"));
        CardResponse marketing = cards.create(new CreateCardRequest("Grace Hopper", "1881", 100_000L, "USD"));
        CardResponse contractor = cards.create(new CreateCardRequest("Alan Turing", "7702", 50_000L, "USD"));

        List.of(
                new AuthorizeRequest(engineering.id(), 12_99L, "USD", "AWS"),
                new AuthorizeRequest(engineering.id(), 89_00L, "USD", "GitHub"),
                new AuthorizeRequest(engineering.id(), 2_500_00L, "USD", "Dell"),   // over limit -> DECLINED
                new AuthorizeRequest(marketing.id(), 250_00L, "USD", "Figma"),
                new AuthorizeRequest(marketing.id(), 42_50L, "USD", "Canva"),
                new AuthorizeRequest(contractor.id(), 199_00L, "USD", "Adobe"))
                .forEach(request -> transactions.authorize(request, null));
    }
}
