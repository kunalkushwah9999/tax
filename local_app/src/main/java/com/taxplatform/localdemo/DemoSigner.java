package com.taxplatform.localdemo;

import com.taxplatform.safety.CandidateCanonicalizer;
import com.taxplatform.safety.CandidateProofVerifier;
import com.taxplatform.safety.SignedCandidate;
import com.taxplatform.safety.SafetyInput.Candidate;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Local-process demonstration credentials only. No private key is exported or persisted. */
final class DemoSigner {
    private static final String KEY_ID="local-demo-only";
    private final KeyPair key;
    private final Clock clock;
    private final Instant started;
    DemoSigner(Clock clock)throws GeneralSecurityException{
        this.clock=clock;started=clock.instant();key=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }
    CandidateProofVerifier verifier(){
        return new CandidateProofVerifier(id->id.equals(KEY_ID)?Optional.of(new CandidateProofVerifier.TrustedKey(KEY_ID,key.getPublic(),
                CandidateProofVerifier.Purpose.CALCULATION_PRODUCER,CandidateProofVerifier.KeyStatus.ACTIVE,started.minusSeconds(60),Instant.MAX)):Optional.empty(),
                clock,Duration.ofMinutes(10),Duration.ofSeconds(5));
    }
    SignedCandidate sign(Candidate candidate)throws GeneralSecurityException{
        Instant now=clock.instant();SignedCandidate.Header header=new SignedCandidate.Header(KEY_ID,now,now.plusSeconds(300));
        Signature signature=Signature.getInstance("Ed25519");signature.initSign(key.getPrivate());signature.update(CandidateCanonicalizer.signingBytes(candidate,header));
        return new SignedCandidate(candidate,header,signature.sign());
    }
}
