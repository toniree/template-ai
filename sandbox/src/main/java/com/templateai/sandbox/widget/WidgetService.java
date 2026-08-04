package com.templateai.sandbox.widget;

import java.util.List;

/**
 * Backend-agnostic contract. Exactly one implementation is active at a time,
 * selected by the "sql" or "mongo" Spring profile (see {@link WidgetService}
 * implementations in the jpa/ and mongo/ subpackages).
 */
public interface WidgetService {

    WidgetDto create(WidgetDto dto);

    WidgetDto get(String id);

    List<WidgetDto> list();

    WidgetDto update(String id, WidgetDto dto);

    void delete(String id);
}
