package com.taxplatform.safety;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.taxplatform.safety.SafetyInput.*;

/** Versioned length-prefixed signing format. Not JSON, not encryption, no custom crypto algorithm. */
public final class CandidateCanonicalizer {
    public static final String FORMAT = "tax-platform/signed-candidate/v1";
    public static final int MAX_BYTES = 8192;

    private CandidateCanonicalizer() { }

    public static byte[] signingBytes(Candidate candidate, SignedCandidate.Header header) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(2048);
            DataOutputStream out = new DataOutputStream(bytes);
            text(out, FORMAT);
            text(out, header.producerKeyId());
            out.writeLong(header.issuedAt().getEpochSecond()); out.writeInt(header.issuedAt().getNano());
            out.writeLong(header.expiresAt().getEpochSecond()); out.writeInt(header.expiresAt().getNano());
            CalculationKey key = candidate.key();
            for (String field : List.of(key.tenantId(), key.profileId(), key.caseId(), key.snapshotId(),
                    key.periodKey(), key.ruleBundleHash(), key.engineDigest(), key.verifierDigest(),
                    key.contractVersion(), key.settingsHash())) text(out, field);
            CaseFacts facts = candidate.facts();
            text(out, facts.scope().name());
            moneyFact(out, facts.salary()); moneyFact(out, facts.savingsInterest());
            moneyFact(out, facts.qualifyingDepositInterest()); moneyFact(out, facts.otherInterest());
            booleanFact(out, facts.incomeInventoryComplete());
            for (Regime regime : Regime.values()) {
                text(out, regime.name()); booleanFact(out, facts.deductionInventoryComplete().get(regime));
            }
            for (Regime regime : Regime.values()) {
                text(out, regime.name());
                RegimeCalculation calculation = candidate.calculations().get(regime);
                out.writeBoolean(calculation != null);
                if (calculation != null) {
                    text(out, calculation.regime().name()); text(out, calculation.annualTaxBeforeCredits().decimal());
                    text(out, calculation.verification().name());
                }
            }
            out.flush();
            if (bytes.size() > MAX_BYTES) throw new IllegalArgumentException("candidate exceeds signing format bound");
            return bytes.toByteArray();
        } catch (IOException impossibleForMemoryStream) {
            throw new IllegalStateException("in-memory signing format failed", impossibleForMemoryStream);
        }
    }

    private static void text(DataOutputStream out, String value) throws IOException {
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        if (utf8.length > 256) throw new IllegalArgumentException("signing field exceeds bound");
        out.writeInt(utf8.length); out.write(utf8);
    }

    private static void moneyFact(DataOutputStream out, Fact<Money> fact) throws IOException {
        text(out, fact.state().name());
        if (fact instanceof Fact.Confirmed<?> known) writeMoney(out, known.value());
        else if (fact instanceof Fact.Estimated<?> estimate) writeMoney(out, estimate.value());
        else if (fact instanceof Fact.Conflict<?> conflict) {
            out.writeInt(conflict.candidates().size());
            for (Object value : conflict.candidates()) writeMoney(out, value);
        }
    }

    private static void writeMoney(DataOutputStream out, Object value) throws IOException {
        if (!(value instanceof Money money)) throw new IllegalArgumentException("invalid monetary fact type");
        text(out, money.decimal());
    }

    private static void booleanFact(DataOutputStream out, Fact<Boolean> fact) throws IOException {
        text(out, fact.state().name());
        if (fact instanceof Fact.Confirmed<?> known) writeBoolean(out, known.value());
        else if (fact instanceof Fact.Estimated<?> estimate) writeBoolean(out, estimate.value());
        else if (fact instanceof Fact.Conflict<?> conflict) {
            out.writeInt(conflict.candidates().size());
            for (Object value : conflict.candidates()) writeBoolean(out, value);
        }
    }

    private static void writeBoolean(DataOutputStream out, Object value) throws IOException {
        if (!(value instanceof Boolean bool)) throw new IllegalArgumentException("invalid boolean fact type");
        out.writeBoolean(bool);
    }
}
