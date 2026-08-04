package com.templateai.sandbox.widget.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document("widgets")
@Getter
@Setter
@NoArgsConstructor
public class WidgetDocument {

    @Id
    private String id;

    private String name;

    private int quantity;

    public WidgetDocument(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }
}
