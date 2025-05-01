package dev.nautchkafe.arangodb;

import com.arangodb.ArangoDBAsync;
import com.arangodb.ArangoDatabaseAsync;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

final class ArangoDatabase implements ArangoDatabaseOperation {

    private final ArangoDBAsync arangoDriver;
    private final ArangoDatabaseAsync databaseDriver;
    private final ExecutorService executorService;

    ArangoDatabase(final ArangoDBAsync arangoDriver, final ArangoDatabaseAsync databaseDriver, final ExecutorService executorService) {
        this.arangoDriver = arangoDriver;
        this.databaseDriver = databaseDriver;
        this.executorService = executorService;
    }

    @Override
    public <TYPE> ArangoCollectionOperation<TYPE> collection(final String name, final Class<TYPE> documentClazz) {
        return ArangoValidation.combine(
                ArangoValidation.requireNonNull(name, "Collection name cannot be null"),
                ArangoValidation.requireNonNull(documentClazz, "Document type cannot be null")
        ).flatMap(valid -> ArangoTry.of(() ->
                new ArangoDatabaseCollection<>(
                        databaseDriver.collection(name),
                        documentClazz,
                        executorService
                ))).getOrElseThrow(e -> new IllegalArgumentException("Failed to create collection operation", e));
    }

    @Override
    public <TYPE> ArangoTry<Void> executeAql(final String query,
                                             final Map<String, Object> bindVars,
                                             final Class<TYPE> clazz,
                                             final Consumer<List<TYPE>> onSuccess, final Consumer<Throwable> onFailure) {
        return ArangoValidation.combine(
                ArangoValidation.requireNonNull(query, "Query cannot be null"),
                ArangoValidation.requireNonNull(clazz, "Result type cannot be null"),
                ArangoValidation.requireNonNull(onSuccess, "Success callback cannot be null"),
                ArangoValidation.requireNonNull(onFailure, "Failure callback cannot be null")
        ).flatMap(valid -> ArangoTry.run(() ->
                executeQuery(query, bindVars, clazz, onSuccess, onFailure)
        ));
    }

    private <TYPE> void executeQuery(
            final String query,
            final Map<String, Object> bindVars,
            final Class<TYPE> clazz,
            final Consumer<List<TYPE>> onSuccess,
            final Consumer<Throwable> onFailure
    ) {
        executorService.execute(() -> {
            ArangoTry.of(() -> databaseDriver.query(query, bindVars, null, clazz))
                    .flatMap(cursor ->
                            ArangoTry.of(cursor::asListRemaining)
                                    .andThen(() -> ArangoTry.run(cursor::close))
                    )
                    .fold(
                            onSuccess,
                            ex -> onFailure.accept(unwrapException(ex))
                    );
        });
    }

    @Override
    public CompletableFuture<Void> close() {
        return arangoDriver.shutdown();
    }

    private Throwable unwrapException(final Throwable ex) {
        return (ex instanceof CompletionException && ex.getCause() != null)
                ? ex.getCause()
                : ex;

    }
