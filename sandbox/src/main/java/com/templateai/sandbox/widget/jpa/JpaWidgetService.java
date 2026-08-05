package com.templateai.sandbox.widget.jpa;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.templateai.sandbox.common.exception.ResourceNotFoundException;
import com.templateai.sandbox.widget.WidgetDto;
import com.templateai.sandbox.widget.WidgetService;

@Service
@Profile({"h2", "postgres"})
public class JpaWidgetService implements WidgetService {

    private final WidgetJpaRepository repository;

    public JpaWidgetService(WidgetJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public WidgetDto create(WidgetDto dto) {
        WidgetEntity saved = repository.save(new WidgetEntity(dto.name(), dto.quantity()));
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
        WidgetEntity entity = findOrThrow(id);
        entity.setName(dto.name());
        entity.setQuantity(dto.quantity());
        return toDto(repository.save(entity));
    }

    @Override
    public void delete(String id) {
        repository.delete(findOrThrow(id));
    }

    private WidgetEntity findOrThrow(String id) {
        return repository.findById(parseId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Widget not found: " + id));
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid widget id: " + id);
        }
    }

    private WidgetDto toDto(WidgetEntity entity) {
        return new WidgetDto(String.valueOf(entity.getId()), entity.getName(), entity.getQuantity());
    }
}
