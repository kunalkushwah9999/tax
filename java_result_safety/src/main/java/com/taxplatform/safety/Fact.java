package com.taxplatform.safety;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Unknown facts have no value slot. These are server domain objects, not HTTP DTOs. */
public sealed interface Fact<T>
        permits Fact.Confirmed, Fact.Unknown, Fact.Estimated, Fact.Conflict {
    enum State { CONFIRMED, UNKNOWN, ESTIMATED, CONFLICT }

    State state();

    default Optional<T> confirmedValue() { return Optional.empty(); }

    record Confirmed<T>(T value) implements Fact<T> {
        public Confirmed { Objects.requireNonNull(value, "confirmed value"); }
        @Override public State state() { return State.CONFIRMED; }
        @Override public Optional<T> confirmedValue() { return Optional.of(value); }
        @Override public String toString() { return "Confirmed[redacted]"; }
    }

    record Unknown<T>() implements Fact<T> {
        @Override public State state() { return State.UNKNOWN; }
    }

    record Estimated<T>(T value) implements Fact<T> {
        public Estimated { Objects.requireNonNull(value, "estimated value"); }
        @Override public State state() { return State.ESTIMATED; }
        @Override public String toString() { return "Estimated[redacted]"; }
    }

    record Conflict<T>(List<T> candidates) implements Fact<T> {
        public Conflict {
            Objects.requireNonNull(candidates, "conflict candidates");
            if (candidates.size() < 2 || candidates.size() > 8) {
                throw new IllegalArgumentException("between two and eight conflict candidates required");
            }
            candidates = List.copyOf(candidates);
            if (candidates.size() < 2 || candidates.size() > 8) throw new IllegalArgumentException("conflict size changed");
        }
        @Override public State state() { return State.CONFLICT; }
        @Override public String toString() { return "Conflict[redacted]"; }
    }

    static <T> Fact<T> confirmed(T value) { return new Confirmed<>(value); }
    static <T> Fact<T> unknown() { return new Unknown<>(); }
    static <T> Fact<T> estimated(T value) { return new Estimated<>(value); }
    static <T> Fact<T> conflict(List<T> candidates) { return new Conflict<>(candidates); }
}
