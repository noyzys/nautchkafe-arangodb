package dev.nautchkafe.arangodb;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

interface ArangoDatabaseOperation {

    <TYPE> ArangoCollectionOperation<TYPE> collection(final String name, final Class<TYPE> documentClazz);

    <TYPE> ArangoTry<Void> executeAql(final String query,
                                        final Map<String, Object> bindVars,
                                        final Class<TYPE> clazz,
                                        final Consumer<List<TYPE>> onSuccess,
                                        final Consumer<Throwable> onFailure);

    CompletableFuture<Void> close();
}
