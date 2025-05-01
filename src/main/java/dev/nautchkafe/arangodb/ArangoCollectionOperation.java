package dev.nautchkafe.arangodb;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.model.DocumentCreateOptions;

import java.util.function.Consumer;

interface ArangoCollectionOperation<TYPE> {

    void save(final TYPE document,
              final Consumer<DocumentCreateEntity<TYPE>> onSuccess,
              final Consumer<Throwable> onFailure);

    void save(final TYPE document,
              final DocumentCreateOptions options,
              final Consumer<DocumentCreateEntity<TYPE>> onSuccess,
              final Consumer<Throwable> onFailure);

    ArangoDatabaseDocumentOperation<TYPE> document(final String key);
}

