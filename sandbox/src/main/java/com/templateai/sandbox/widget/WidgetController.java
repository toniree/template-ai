package com.templateai.sandbox.widget;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/widgets")
public class WidgetController {

    private final WidgetService widgetService;

    public WidgetController(WidgetService widgetService) {
        this.widgetService = widgetService;
    }

    @PostMapping
    public ResponseEntity<WidgetDto> create(@Valid @RequestBody WidgetDto dto) {
        WidgetDto created = widgetService.create(dto);
        return ResponseEntity.created(URI.create("/api/widgets/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public WidgetDto get(@PathVariable String id) {
        return widgetService.get(id);
    }

    @GetMapping
    public List<WidgetDto> list() {
        return widgetService.list();
    }

    @PutMapping("/{id}")
    public WidgetDto update(@PathVariable String id, @Valid @RequestBody WidgetDto dto) {
        return widgetService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        widgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
