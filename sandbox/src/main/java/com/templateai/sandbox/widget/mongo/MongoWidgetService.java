package com.templateai.sandbox.widget.mongo;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.templateai.sandbox.common.exception.ResourceNotFoundException;
import com.templateai.sandbox.widget.WidgetDto;
import com.templateai.sandbox.widget.WidgetService;

@Service
@Profile("mongo")
public class MongoWidgetService implements WidgetService {

    private final WidgetMongoRepository repository;

    public MongoWidgetService(WidgetMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public WidgetDto create(WidgetDto dto) {
        WidgetDocument saved = repository.save(new WidgetDocument(dto.name(), dto.quantity()));
        return toDto(saved);
    }

    @Override
    public WidgetDto get(String id) {
        return toDto(findOrThrow(id));
    }

    @Override
    public List<WidgetDto> list() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public WidgetDto update(String id, WidgetDto dto) {
        WidgetDocument document = findOrThrow(id);
        document.setName(dto.name());
        document.setQuantity(dto.quantity());
        return toDto(repository.save(document));
    }

    @Override
    public void delete(String id) {
        repository.delete(findOrThrow(id));
    }

    private WidgetDocument findOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Widget not found: " + id));
    }

    private WidgetDto toDto(WidgetDocument document) {
        return new WidgetDto(document.getId(), document.getName(), document.getQuantity());
    }
}
