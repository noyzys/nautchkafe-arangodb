package dev.nautchkafe.arangodb;

import com.arangodb.ArangoCollectionAsync;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.model.DocumentCreateOptions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class ArangoDatabaseCollection<TYPE> implements ArangoCollectionOperation<TYPE> {

    private final ArangoCollectionAsync collectionAsync;
    private final Class<TYPE> documentClazz;
    private final ExecutorService executorService;

    ArangoDatabaseCollection(final ArangoCollectionAsync collectionAsync,
                             final Class<TYPE> documentClazz,
                             final ExecutorService executorService) {
        this.collectionAsync = collectionAsync;
        this.documentClazz = documentClazz;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void save(final TYPE document,
                     final Consumer<DocumentCreateEntity<TYPE>> onSuccess,
                     final Consumer<Throwable> onFailure) {
        save(document, new DocumentCreateOptions(), onSuccess, onFailure);
    }

    @Override
    public void save(final TYPE document, final DocumentCreateOptions options,
                     final Consumer<DocumentCreateEntity<TYPE>> onSuccess,
                     final Consumer<Throwable> onFailure) {
        validateAndExecute(
                document, onSuccess, onFailure,
                validDocument -> collectionAsync.insertDocument(validDocument, options, documentClazz));
    }

    @Override
    public ArangoDatabaseDocumentOperation<TYPE> document(final String key) {
        return ArangoValidation.requireNonBlank(key, "Document key cannot be blank")
                .flatMap(validKey -> ArangoTry.of(() -> new ArangoDatabaseDocument<>(
                        collectionAsync,
                        validKey,
                        documentClazz,
                        executorService
                )))
                .getOrElseThrow(e -> new IllegalArgumentException("Failed to create document operation", e));
    }

    private void validateAndExecute(final TYPE document,
                                    final Consumer<DocumentCreateEntity<TYPE>> onSuccess,
                                    final Consumer<Throwable> onFailure,
                                    final Function<TYPE, CompletableFuture<DocumentCreateEntity<TYPE>>> operation) {
        ArangoValidation.requireNonNull(document, "Document cannot be null")
                .flatMap(doc -> ArangoValidation.requireNonNull(onSuccess, "Success callback cannot be null")
                        .flatMap(os -> ArangoValidation.requireNonNull(onFailure, "Failure callback cannot be null")))
                .fold(valid -> executeOperation(() -> operation.apply(document), onSuccess, onFailure),
                        onFailure::accept);
    }

    private <RESULT> void executeOperation(final Supplier<CompletableFuture<RESULT>> operation,
                                           final Consumer<RESULT> onSuccess,
                                           final Consumer<Throwable> onFailure
    ) {
        executorService.execute(() -> {
            ArangoTry.of(operation)
                    .fold(future -> future.whenComplete((result, ex) -> {
                                // :(
                                if (ex != null) {
                                    onFailure.accept(unwrapException(ex));
                                } else {
                                    onSuccess.accept(result);
                                }
                            }), onFailure::accept
                    );
        });
    }

    private Throwable unwrapException(final Throwable ex) {
        return (ex instanceof CompletionException && ex.getCause() != null)
                ? ex.getCause()
                : ex;
    }
}


