package dev.nautchkafe.arangodb;

import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.DocumentUpdateOptions;

import java.util.function.Consumer;

interface ArangoDatabaseDocumentOperation<TYPE> {

    void fetch(final Consumer<TYPE> onSuccess,
               final Consumer<Throwable> onFailure);

    void update(final TYPE document,
                final Consumer<DocumentUpdateEntity<TYPE>> onSuccess,
                final Consumer<Throwable> onFailure) throws Exception;
    void update(final TYPE document,
                final DocumentUpdateOptions options,
                final Consumer<DocumentUpdateEntity<TYPE>> onSuccess,
                final Consumer<Throwable> onFailure) throws Exception;

    void replace(final TYPE document,
                 final Consumer<DocumentUpdateEntity<TYPE>> onSuccess,
                 final Consumer<Throwable> onFailure) throws Exception;

    void replace(final TYPE document, final DocumentReplaceOptions options,
                 final Consumer<DocumentUpdateEntity<TYPE>> onSuccess,
                 final Consumer<Throwable> onFailure) throws Exception;

    void delete(final Consumer<DocumentDeleteEntity<Void>> onSuccess,
                final Consumer<Throwable> onFailure) throws Exception;

    void delete(final DocumentDeleteOptions options,
                final Consumer<DocumentDeleteEntity<Void>> onSuccess,
                final Consumer<Throwable> onFailure) throws Exception;

    void exists(final Consumer<Boolean> onSuccess,
                final Consumer<Throwable> onFailure) throws Exception;
}
