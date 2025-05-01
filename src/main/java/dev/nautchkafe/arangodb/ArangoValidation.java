package dev.nautchkafe.arangodb;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class ArangoValidation {

    private ArangoValidation() {
    }

    static <TYPE> ArangoTry<TYPE> validate(final TYPE value,
                                           final Predicate<TYPE> validator,
                                           final Supplier<Exception> error) {
        return validator.test(value)
                ? ArangoTry.success(value)
                : ArangoTry.failure(error.get());
    }

    static <TYPE> ArangoTry<TYPE> requireNonNull(final TYPE value, final String message) {
        return validate(value, Objects::nonNull, () -> new IllegalArgumentException(message));
    }

    static ArangoTry<String> requireNonBlank(final String value, final String message) {
        return validate(value, string -> string != null && !string.isBlank(),
                () -> new IllegalArgumentException(message));
    }

    static <TYPE>Function<TYPE, ArangoTry<TYPE>> validator(final Predicate<TYPE> validator,
                                                           final Supplier<Exception> error) {
        return value -> validate(value, validator, error);
    }

    @SafeVarargs
    static ArangoTry<Void> combineEach(final ArangoTry<?>... validations) {
        return ArangoTry.allOf(validations);
    }

    @SafeVarargs
    static ArangoTry<Void> combine(final ArangoTry<?>... validations) {
        for (final ArangoTry<?> validation : validations) {
            if (validation.isFailure()) {
                return ArangoTry.failure(validation.getFailure());
            }
        }

        return ArangoTry.success(null);
    }

    static <TYPE> ArangoTry<TYPE> ifSuccessOrElse(
            final ArangoTry<TYPE> tryResult,
            final Consumer<TYPE> onSuccess,
            final Runnable onFailure) {
        if (tryResult.isSuccess()) {
            tryResult.fold(onSuccess, e -> {});
        }

        onFailure.run();
        return tryResult;
    }
}
