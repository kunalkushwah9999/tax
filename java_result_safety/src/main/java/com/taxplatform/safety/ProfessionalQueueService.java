package com.taxplatform.safety;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.taxplatform.safety.ProfessionalReview.*;
import static com.taxplatform.safety.PublicResult.*;
import static com.taxplatform.safety.SafetyInput.*;

/** Read-only review triage, with fresh per-case professional grants plus existing signed result gates. */
public final class ProfessionalQueueService {
    public static final int MAX_CASES=50;
    private final Source source;
    private final SignedResultService results;
    private final Clock clock;

    public ProfessionalQueueService(Source source,SafeResultService.ControlPlane controls,CandidateProofVerifier verifier,Clock clock) {
        this.source=Objects.requireNonNull(source); this.results=new SignedResultService(controls,verifier); this.clock=Objects.requireNonNull(clock);
    }
    public ProfessionalQueue deliver(Actor reviewer,List<CalculationKey> requestedCases) {
        Objects.requireNonNull(reviewer); Objects.requireNonNull(requestedCases);
        if(requestedCases.isEmpty() || requestedCases.size()>MAX_CASES) throw new IllegalArgumentException("between 1 and 50 unique cases required");
        List<CalculationKey> cases=List.copyOf(requestedCases);
        Set<List<String>> unique=new HashSet<>();
        for(CalculationKey key:cases) {
            if(!unique.add(List.of(key.tenantId(),key.profileId(),key.caseId()))) throw new IllegalArgumentException("duplicate case resource");
        }
        List<Map<String,Object>> rows=new ArrayList<>(); Map<Route,Integer> counts=new EnumMap<>(Route.class);
        for(int i=0;i<cases.size();i++) {
            Map<String,Object> row=caseRow(reviewer,cases.get(i),i);
            rows.add(row); counts.merge(Route.valueOf((String)row.get("route")),1,Integer::sum);
        }
        Map<String,Object> countResponse=new LinkedHashMap<>();
        for(Route route:Route.values()) countResponse.put(route.name(),Integer.toString(counts.getOrDefault(route,0)));
        return new ProfessionalQueue(rows,countResponse);
    }

    private Map<String,Object> caseRow(Actor reviewer,CalculationKey expected,int index) {
        if(!reviewer.tenantId().equals(expected.tenantId())) return row(index,Route.ACCESS_DENIED,List.of());
        try {
            Optional<Access> resolved=source.currentFor(reviewer,expected);
            if(resolved==null) return row(index,Route.UNAVAILABLE,List.of());
            if(resolved.isEmpty()) return row(index,Route.ACCESS_DENIED,List.of());
            Access access=resolved.get(); Instant now=clock.instant();
            if(!access.assignedReviewer().equals(reviewer) || !access.grantedCase().sameResource(expected)
                    || !access.grantedCase().periodKey().equals(expected.periodKey()) || access.grantState()!=GrantState.ACTIVE
                    || access.credentialState()!=CredentialState.VERIFIED || now.isBefore(access.validFrom())
                    || !now.isBefore(access.validUntil())) return row(index,Route.ACCESS_DENIED,List.of());
            if(!access.featureReleased()) return row(index,Route.FEATURE_DISABLED,List.of());
            if(!expected.equals(access.signedCandidate().candidate().key()) || !expected.equals(access.evidenceSnapshot())) {
                return row(index,Route.STALE_CASE,List.of());
            }
            PublicResult result=results.deliver(reviewer,access.signedCandidate());
            if(result instanceof Denied) return row(index,Route.ACCESS_DENIED,List.of());
            if(result instanceof Blocked) return row(index,Route.RESULT_BLOCKED,List.of());
            // Detailed evidence readiness is only shown for a currently authorized, signed, policy-accepted case.
            List<Map<String,Object>> evidence=evidenceRows(access.evidence());
            return row(index,route(access,result),evidence);
        } catch(SafeResultService.ControlStateUnavailable unavailable) { return row(index,Route.UNAVAILABLE,List.of()); }
    }

    private static Route route(Access access,PublicResult result) {
        if(access.workKind()==WorkKind.STATUTORY_PROFESSIONAL_ACTION || access.workKind()==WorkKind.SPECIALIST_REVIEW) return Route.PROFESSIONAL_ACTION_REQUIRED;
        if(access.workKind()==WorkKind.UNKNOWN || result.reasons().contains(Reason.UNSUPPORTED_SCOPE) || result.reasons().contains(Reason.SCOPE_UNKNOWN)) return Route.SCOPE_REVIEW_REQUIRED;
        if(result.reasons().stream().anyMatch(r->List.of(Reason.UNKNOWN_INCOME,Reason.ESTIMATED_INCOME,Reason.CONFLICTING_INCOME,Reason.INCOME_INVENTORY_INCOMPLETE).contains(r))) return Route.CLIENT_INFORMATION_REQUIRED;
        if(access.evidence().containsValue(EvidenceState.CONFLICT)) return Route.EVIDENCE_CONFLICT;
        if(access.evidence().values().stream().anyMatch(s->s!=EvidenceState.REVIEWED && s!=EvidenceState.NOT_REQUIRED)) return Route.EVIDENCE_REQUIRED;
        if(result.reasons().contains(Reason.REGIME_AVAILABILITY_UNKNOWN) || result.reasons().contains(Reason.REGIME_NOT_AVAILABLE)) return Route.ELIGIBILITY_REVIEW_REQUIRED;
        if(result.reasons().contains(Reason.DEDUCTIONS_INCOMPLETE)) return Route.DEDUCTION_REVIEW_REQUIRED;
        if(result.reasons().contains(Reason.COMPARISON_CALCULATION_MISSING)) return Route.CALCULATION_PENDING;
        return result instanceof Calculation ? Route.READY_FOR_REVIEW : Route.CLIENT_INFORMATION_REQUIRED;
    }

    private static List<Map<String,Object>> evidenceRows(Map<EvidenceKind,EvidenceState> states) {
        List<Map<String,Object>> rows=new ArrayList<>();
        for(EvidenceKind kind:EvidenceKind.values()) rows.add(GuidedResult.frozen(Map.of("kind",kind.name(),"state",states.get(kind).name())));
        return List.copyOf(rows);
    }
    private static Map<String,Object> row(int index,Route route,List<Map<String,Object>> evidence) {
        String action=switch(route) {
            case ACCESS_DENIED -> "Check current case access and assignment";
            case FEATURE_DISABLED -> "Wait for professional workspace release";
            case UNAVAILABLE -> "Retry when current control state is available";
            case STALE_CASE -> "Refresh the case and evidence snapshot";
            case RESULT_BLOCKED -> "Resolve the system validation block before using a result";
            case PROFESSIONAL_ACTION_REQUIRED -> "Use the applicable qualified professional workflow";
            case SCOPE_REVIEW_REQUIRED -> "Review scope and the appropriate workflow";
            case CLIENT_INFORMATION_REQUIRED -> "Request or confirm the missing client information";
            case EVIDENCE_CONFLICT -> "Resolve conflicting evidence before completing review";
            case EVIDENCE_REQUIRED -> "Collect and review the required evidence";
            case ELIGIBILITY_REVIEW_REQUIRED -> "Review regime availability for the case";
            case DEDUCTION_REVIEW_REQUIRED -> "Complete the relevant deduction review";
            case CALCULATION_PENDING -> "Wait for the required checked calculation";
            case READY_FOR_REVIEW -> "Review the prepared case; approval has not been recorded";
        };
        Map<String,Object> row=new LinkedHashMap<>(); row.put("request_index",Integer.toString(index));row.put("route",route.name());
        row.put("next_action",action);row.put("professional_review_status","NOT_RECORDED");
        if(!evidence.isEmpty()) row.put("evidence_checklist",evidence);
        return GuidedResult.frozen(row);
    }
}
