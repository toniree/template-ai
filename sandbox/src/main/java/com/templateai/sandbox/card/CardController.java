package com.templateai.sandbox.card;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.templateai.sandbox.card.CardDtos.CardResponse;
import com.templateai.sandbox.card.CardDtos.CreateCardRequest;
import com.templateai.sandbox.card.CardDtos.UpdateCardRequest;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<CardResponse> create(@Valid @RequestBody CreateCardRequest request) {
        CardResponse created = cardService.create(request);
        return ResponseEntity.created(URI.create("/api/cards/" + created.id())).body(created);
    }

    @GetMapping
    public List<CardResponse> list() {
        return cardService.list();
    }

    @GetMapping("/{id}")
    public CardResponse get(@PathVariable Long id) {
        return cardService.get(id);
    }

    /** PATCH, not PUT: this changes card state, it does not replace the card. */
    @PatchMapping("/{id}")
    public CardResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCardRequest request) {
        return cardService.update(id, request);
    }
}
