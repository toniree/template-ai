package com.templateai.sandbox;

import java.time.Clock;
import java.time.Instant;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.templateai.sandbox.card.Card;
import com.templateai.sandbox.card.CardRepository;
import com.templateai.sandbox.card.DisabledCard;
import com.templateai.sandbox.card.DisabledCardRepository;
import com.templateai.sandbox.card.Merchant;
import com.templateai.sandbox.card.MerchantBlock;
import com.templateai.sandbox.card.MerchantBlockRepository;
import com.templateai.sandbox.card.MerchantRepository;

/**
 * Seeds the database so the UI has something to show the moment it loads. Runs on the {@code
 * postgres} and {@code h2} profiles — never on {@code test}, whose tests assert on rows they
 * created themselves.
 *
 * <p>Goes through the repositories directly rather than a service — there's no admin CRUD for
 * cards/merchants in this problem, seeded fixtures stand in for it (see CLAUDE.md).
 */
@Component
@Profile({"postgres", "h2"})
public class DemoData implements CommandLineRunner {

    private final CardRepository cards;
    private final MerchantRepository merchants;
    private final DisabledCardRepository disabledCards;
    private final MerchantBlockRepository merchantBlocks;
    private final Clock clock;

    public DemoData(CardRepository cards, MerchantRepository merchants, DisabledCardRepository disabledCards,
            MerchantBlockRepository merchantBlocks, Clock clock) {
        this.cards = cards;
        this.merchants = merchants;
        this.disabledCards = disabledCards;
        this.merchantBlocks = merchantBlocks;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (!cards.findAll().isEmpty()) {
            return;
        }

        Instant now = Instant.now(clock);

        Card active = card("4111111111111111", "1111", "12/29", "123", Card.DEFAULT_BALANCE_MINOR, now);
        Card deactivated = card("4222222222222222", "2222", "11/28", "456", Card.DEFAULT_BALANCE_MINOR, now);
        Card lowBalance = card("4333333333333333", "3333", "10/27", "789", 5_00L, now);

        disabledCards.save(new DisabledCard(deactivated.getId()));

        Merchant acme = merchant("Acme Supplies", "San Francisco, CA");
        merchant("Office Depot", "New York, NY");
        Merchant blocker = merchant("Blocked Co", "Austin, TX");

        merchantBlocks.save(new MerchantBlock(blocker.getId(), active.getId()));
    }

    private Card card(String token, String last4, String expiryDate, String cvc, long balanceMinor, Instant now) {
        Card card = new Card();
        card.setCardToken(token);
        card.setLast4(last4);
        card.setExpiryDate(expiryDate);
        card.setCvc(cvc);
        card.setBalanceMinor(balanceMinor);
        card.setCreatedDate(now);
        return cards.save(card);
    }

    private Merchant merchant(String name, String location) {
        Merchant merchant = new Merchant();
        merchant.setName(name);
        merchant.setLocation(location);
        return merchants.save(merchant);
    }
}
