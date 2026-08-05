package com.templateai.sandbox.transaction;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.templateai.sandbox.common.PageResponse;
import com.templateai.sandbox.transaction.TransactionDtos.AuthorizeRequest;
import com.templateai.sandbox.transaction.TransactionDtos.AuthorizeResult;
import com.templateai.sandbox.transaction.TransactionDtos.TransactionResponse;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    /** Cap the page size so one caller cannot ask for the whole table. */
    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * 201 for a new authorization, 200 when an {@code Idempotency-Key} replays one we already
     * answered — so a client retrying after a timeout can tell the two apart.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> authorize(
            @Valid @RequestBody AuthorizeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        AuthorizeResult result = transactionService.authorize(request, idempotencyKey);
        if (result.replayed()) {
            return ResponseEntity.ok(result.transaction());
        }
        return ResponseEntity
                .created(URI.create("/api/transactions/" + result.transaction().id()))
                .body(result.transaction());
    }

    @GetMapping
    public PageResponse<TransactionResponse> list(
            @RequestParam(required = false) Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return transactionService.list(cardId, pageable);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id) {
        return transactionService.get(id);
    }
}
