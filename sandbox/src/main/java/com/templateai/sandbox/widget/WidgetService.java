package com.templateai.sandbox.widget;

import java.util.List;

/**
 * Backend-agnostic contract, implemented by {@code JpaWidgetService} and
 * activated for both the "h2" and "postgres" Spring profiles.
 */
public interface WidgetService {

    WidgetDto create(WidgetDto dto);

    WidgetDto get(String id);

    List<WidgetDto> list();

    WidgetDto update(String id, WidgetDto dto);

    void delete(String id);
}
