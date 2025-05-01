package dev.nautchkafe.arangodb;

import com.arangodb.ArangoCollectionAsync;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.DocumentUpdateOptions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

final class ArangoDatabaseDocument<TYPE> implements ArangoDatabaseDocumentOperation<TYPE> {

    private final ArangoCollectionAsync collectionDriver;
    private final String key;
    private final Class<TYPE> documentClazz;
    private final ExecutorService executorService;

    ArangoDatabaseDocument(final ArangoCollectionAsync collectionDriver,
                           final String key,
                           final Class<TYPE> documentClazz,
                           final ExecutorService executorService) {
        ArangoValidation.combine(
                ArangoValidation.requireNonNull(collectionDriver, "Collection driver cannot be null"),
                ArangoValidation.requireNonBlank(key, "Document key cannot be blank"),
                ArangoValidation.requireNonNull(documentClazz, "Document type cannot be null")
        ).orElseThrowUnchecked();

        this.collectionDriver = collectionDriver;
        this.key = key;
        this.documentClazz = documentClazz;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    private <VALUE> void safeAccept(final Consumer<VALUE> valuer, final VALUE value) {
        try {
            valuer.accept(value);
        } catch (final Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private <RESULT> void peekFuture(final CompletableFuture<RESULT> future,
                                     final Consumer<RESULT> onSuccess,
                                     final Consumer<Throwable> onFailure) {
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                safeAccept(onFailure, unwrapException(throwable));
            }
            safeAccept(onSuccess, result);
        });
    }

    private Throwable unwrapException(final Throwable ex) {
        return (ex instanceof CompletionException && ex.getCause() != null)
                ? ex.getCause()
                : ex;
    }

    @Override
    public void fetch(final Consumer<TYPE> onSuccess, final Consumer<Throwable> onFailure) {
        ArangoValidation.requireNonNull(onSuccess, "onSuccess callback cannot be null")
                .ifFailure(e -> { throw new IllegalArgumentException(e.getMessage(), e); });
        ArangoValidation.requireNonNull(onFailure, "onFailure callback cannot be null")
                .ifFailure(e -> { throw new IllegalArgumentException(e.getMessage(), e); });

        final CompletableFuture<TYPE> future = collectionDriver.getDocument(key, documentClazz);
        peekFuture(future, onSuccess, onFailure);
    }

    @Override
    public void update(final TYPE document,
                       final Consumer<DocumentUpdateEntity<TYPE>> onSuccess,
                       final Consumer<Throwable> onFailure) throws Exception {
        update(document, null, onSuccess, onFailure);
    }

    @Override
    public void update(final TYPE document, final DocumentUpdateOptions options,
                       final Consumer<DocumentUpdateEntity<TYPE>> onSuccess, final Consumer<Throwable> onFailure) {
        ArangoValidation.combine(
                ArangoValidation.requireNonNull(document, "Document for update cannot be null"),
                ArangoValidation.requireNonNull(onSuccess, "onSuccess callback cannot be null"),
                ArangoValidation.requireNonNull(onFailure, "onFailure callback cannot be null")
        ).ifFailure(e -> { throw new IllegalArgumentException(e.getMessage(), e); });

        final CompletableFuture<DocumentUpdateEntity<TYPE>> future = collectionDriver.updateDocument(key, document, options);
        peekFuture(future, onSuccess, onFailure);
    }

    @Override
    public void replace(final TYPE document,
                        final Consumer<DocumentUpdateEntity<TYPE>> onSuccess,
                        final Consumer<Throwable> onFailure) {
        replace(document, null, onSuccess, onFailure);
    }

    @Override
    public void replace(final TYPE document, final DocumentReplaceOptions options,
                        final Consumer<DocumentUpdateEntity<TYPE>> onSuccess,
                        final Consumer<Throwable> onFailure) {
        ArangoValidation.combine(
                ArangoValidation.requireNonNull(document, "Document for replace cannot be null"),
                ArangoValidation.requireNonNull(onSuccess, "onSuccess callback cannot be null"),
                ArangoValidation.requireNonNull(onFailure, "onFailure callback cannot be null")
        ).ifFailure(e -> { throw new IllegalArgumentException(e.getMessage(), e); });

        final CompletableFuture<DocumentUpdateEntity<TYPE>> future = collectionDriver.replaceDocument(key, document, options);
        peekFuture(future, onSuccess, onFailure);
    }

    @Override
    public void delete(final Consumer<DocumentDeleteEntity<Void>> onSuccess, final Consumer<Throwable> onFailure) {
        delete(null, onSuccess, onFailure);
    }

    @Override
    public void delete(final DocumentDeleteOptions options,
                       final Consumer<DocumentDeleteEntity<Void>> onSuccess,
                       Consumer<Throwable> onFailure) {
        ArangoValidation.combine(
                ArangoValidation.requireNonNull(onSuccess, "onSuccess callback cannot be null"),
                ArangoValidation.requireNonNull(onFailure, "onFailure callback cannot be null")
        ).ifFailure(e -> { throw new IllegalArgumentException(e.getMessage(), e); });

        final CompletableFuture<DocumentDeleteEntity<Void>> future = collectionDriver.deleteDocument(key, null, Void.class);
        peekFuture(future, onSuccess, onFailure);
    }

    @Override
    public void exists(final Consumer<Boolean> onSuccess, final Consumer<Throwable> onFailure) {
        ArangoValidation.combine(
                ArangoValidation.requireNonNull(onSuccess, "onSuccess callback cannot be null"),
                ArangoValidation.requireNonNull(onFailure, "onFailure callback cannot be null")
        ).ifFailure(e -> { throw new IllegalArgumentException(e.getMessage(), e); });


        final CompletableFuture<Boolean> future = collectionDriver.documentExists(key);
        peekFuture(future, onSuccess, onFailure);
    }
}
