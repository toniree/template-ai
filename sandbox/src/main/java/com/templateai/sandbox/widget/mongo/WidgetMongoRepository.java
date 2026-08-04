package com.templateai.sandbox.widget.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface WidgetMongoRepository extends MongoRepository<WidgetDocument, String> {
}
