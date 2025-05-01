package dev.nautchkafe.arangodb;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

sealed interface ArangoTry<TYPE> permits ArangoTry.Success, ArangoTry.Failure {

    static <TYPE> ArangoTry<TYPE> of(final Supplier<TYPE> supplier) {
        try {
            return new Success<>(supplier.get());
        } catch (final Exception e) {
            return new Failure<>(e);
        }
    }

    static ArangoTry<Void> allOf(final ArangoTry<?>... tries) {
        ArangoValidation.requireNonNull(tries, "Tries array cannot be null");

        return Arrays.stream(tries)
                .filter(ArangoTry::isFailure)
                .findFirst()
                .<ArangoTry<Void>>map(failure -> ArangoTry.failure(failure.getFailure()))
                .orElseGet(() -> ArangoTry.success(null));
    }

    @SafeVarargs
    static <TYPE> ArangoTry<List<TYPE>> allOfSameType(final ArangoTry<TYPE>... tries) {
        ArangoValidation.requireNonNull(tries, "Tries array cannot be null");

        final List<Exception> errors = Arrays.stream(tries)
                .filter(ArangoTry::isFailure)
                .map(ArangoTry::getFailure)
                .toList();

        if (!errors.isEmpty()) {
            return failure(errors.getFirst());
        }

        final List<TYPE> values = Arrays.stream(tries)
                .map(typeArango -> ((Success<TYPE>) typeArango).value())
                .toList();

        return success(values);
    }

    public static <TYPE> ArangoTry<TYPE> run(final Runnable runnable) {
        try {
            runnable.run();
            return ArangoTry.success(null);
        } catch (final Exception e) {
            return ArangoTry.failure(e);
        }
    }

    static <TYPE> ArangoTry<TYPE> success(final TYPE value) {
        return new Success<>(value);
    }

    static <TYPE> ArangoTry<TYPE> failure(final Exception error) {
        return new Failure<>(error);
    }

    <RESULT> ArangoTry<RESULT> map(final Function<TYPE, RESULT> mapper);
    <RESULT> ArangoTry<RESULT> flatMap(final Function<TYPE, ArangoTry<RESULT>> mapper);

    ArangoTry<TYPE> recover(final Function<Exception, TYPE> recovery);
    ArangoTry<TYPE> recoverWith(final Function<Exception, ArangoTry<TYPE>> recovery);

    void fold(final Consumer<TYPE> onSucess, final Consumer<Exception> onFailure);

    TYPE orElse(final TYPE defaultValue);
    TYPE orElseThrow() throws Exception;

    boolean isSuccess();
    boolean isFailure();

    Exception getFailure();

    default Exception getFailureOrElse(final Exception defaultValue) {
        return isFailure() ? getFailure() : defaultValue;
    }

    default TYPE orElseThrowUnchecked() {
        try {
            return orElseThrow();
        } catch (final Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }

    default TYPE getOrElseThrow(final Function<Exception, RuntimeException> exceptionMapper) {
        try {
            return orElseThrow();
        } catch (Exception e) {
            throw exceptionMapper.apply(e);
        }
    }

    default ArangoTry<TYPE> ifFailure(final Consumer<Exception> action) {
        if (isFailure()) {
            action.accept(getFailure());
        }

        return this;
    }

    record Success<TYPE>(TYPE value) implements ArangoTry<TYPE> {

        @Override
        public <RESULT> ArangoTry<RESULT> map(final Function<TYPE, RESULT> mapper) {
            return ArangoTry.of((() -> mapper.apply(value)));
        }

        @Override
        public <RESULT> ArangoTry<RESULT> flatMap(final Function<TYPE, ArangoTry<RESULT>> mapper) {
            try {
                return mapper.apply(value);
            } catch (final Exception e) {
                return new Failure<>(e);
            }
        }

        @Override
        public ArangoTry<TYPE> recover(final Function<Exception, TYPE> recovery) {
            return this;
        }

        @Override
        public ArangoTry<TYPE> recoverWith(final Function<Exception, ArangoTry<TYPE>> recovery) {
            return this;
        }

        @Override
        public void fold(final Consumer<TYPE> onSucess, final Consumer<Exception> onFailure) {
            onSucess.accept(value);
        }

        @Override
        public TYPE orElse(final TYPE defaultValue) {
            return value;
        }

        @Override
        public TYPE orElseThrow() throws Exception {
            return value;
        }

        @Override
        public Exception getFailure() {
            throw new NoSuchElementException("Cannot call #getFailure() on Success");
        }

        @Override
        public Exception getFailureOrElse(final Exception defaultValue) {
            return defaultValue;
        }

        @Override
        public ArangoTry<TYPE> ifFailure(final Consumer<Exception> action) {
            return this;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }
    }

    record Failure<TYPE>(Exception error) implements ArangoTry<TYPE> {

        @Override
        public <RESULT> ArangoTry<RESULT> map(final Function<TYPE, RESULT> mapper) {
            return new Failure<>(error);
        }

        @Override
        public <RESULT> ArangoTry<RESULT> flatMap(final Function<TYPE, ArangoTry<RESULT>> mapper) {
            return new Failure<>(error);
        }

        @Override
        public ArangoTry<TYPE> recover(final Function<Exception, TYPE> recovery) {
            return ArangoTry.of(() -> recovery.apply(error));
        }

        @Override
        public ArangoTry<TYPE> recoverWith(final Function<Exception, ArangoTry<TYPE>> recovery) {
            try {
                return recovery.apply(error);
            } catch (final Exception e) {
                return new Failure<>(e);
            }
        }

        @Override
        public void fold(final Consumer<TYPE> onSucess, final Consumer<Exception> onFailure) {
            onFailure.accept(error);
        }

        @Override
        public TYPE orElse(final TYPE defaultValue) {
            return defaultValue;
        }

        @Override
        public TYPE orElseThrow() throws Exception {
            throw error;
        }

        @Override
        public Exception getFailure() {
            return error;
        }

        @Override
        public Exception getFailureOrElse(final Exception defaultValue) {
            return error;
        }

        @Override
        public ArangoTry<TYPE> ifFailure(final Consumer<Exception> action) {
            action.accept(error);
            return this;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }
    }
}
