package com.taxplatform.safety;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static com.taxplatform.safety.FactEdits.*;
import static com.taxplatform.safety.FactEditResult.Status.*;
import static com.taxplatform.safety.SafetyInput.*;

/** Pure bounded fact transitions; no storage, authority upgrade, tax arithmetic or default zero. */
final class FactEditReducer {
    private FactEditReducer() { }
    record Decision(CaseFacts facts, FactEditResult rejection, String operation, String field) { }

    static Decision decide(CaseFacts facts, Command command) {
        if(command instanceof ProposeAmount propose) {
            HumanMoneyParser.Outcome parsed=new HumanMoneyParser().parse(propose.text(),propose.unit());
            if(parsed instanceof HumanMoneyParser.Rejected bad) return rejected(FactEditResult.invalid(bad.problem()));
            return money(facts,propose.field(),((HumanMoneyParser.Parsed)parsed).asUnconfirmedFact(),"PROPOSE_AMOUNT");
        }
        if(command instanceof ConfirmAmount confirm) {
            Fact<Money> existing=amount(facts,confirm.field());
            if(!(existing instanceof Fact.Estimated<?> estimate)) return rejected(FactEditResult.rejected(REVIEW_STATE_CHANGED));
            if(!confirm.displayedAmount().equals(estimate.value())) return rejected(FactEditResult.rejected(CONFIRMATION_VALUE_CHANGED));
            return money(facts,confirm.field(),Fact.confirmed(confirm.displayedAmount()),"CONFIRM_AMOUNT");
        }
        if(command instanceof ResolveConflict resolve) {
            Fact<Money> existing=amount(facts,resolve.field());
            if(!(existing instanceof Fact.Conflict<?> conflict)) return rejected(FactEditResult.rejected(REVIEW_STATE_CHANGED));
            if(!conflict.candidates().contains(resolve.selectedAmount())) return rejected(FactEditResult.rejected(CONFLICT_VALUE_NOT_PRESENT));
            return money(facts,resolve.field(),Fact.confirmed(resolve.selectedAmount()),"RESOLVE_CONFLICT");
        }
        if(command instanceof ClearAmount clear) return money(facts,clear.field(),Fact.unknown(),"CLEAR_AMOUNT");
        if(command instanceof ReviewInventory review) {
            if(facts.scope()!=Scope.SUPPORTED) return rejected(FactEditResult.rejected(UNSUPPORTED_SCOPE));
            if(!allAmountsConfirmed(facts) || (review.inventory()!=Inventory.INCOME
                    && !facts.incomeInventoryComplete().confirmedValue().orElse(false))) {
                return rejected(FactEditResult.rejected(INPUT_REVIEW_INCOMPLETE));
            }
            return inventory(facts,review.inventory(),Fact.confirmed(true),"REVIEW_INVENTORY");
        }
        if(command instanceof UnreviewInventory unreview) return inventory(facts,unreview.inventory(),Fact.unknown(),"UNREVIEW_INVENTORY");
        throw new IllegalStateException("unhandled sealed edit command");
    }

    private static Decision rejected(FactEditResult rejection) { return new Decision(null,rejection,"",""); }
    private static Fact<Money> amount(CaseFacts facts,AmountField field) {
        return switch(field) {
            case SALARY -> facts.salary(); case SAVINGS_INTEREST -> facts.savingsInterest();
            case DEPOSIT_INTEREST -> facts.qualifyingDepositInterest(); case OTHER_INTEREST -> facts.otherInterest();
        };
    }
    private static boolean allAmountsConfirmed(CaseFacts facts) {
        return List.of(facts.salary(),facts.savingsInterest(),facts.qualifyingDepositInterest(),facts.otherInterest())
                .stream().allMatch(f->f.state()==Fact.State.CONFIRMED);
    }
    private static Decision money(CaseFacts facts,AmountField field,Fact<Money> replacement,String operation) {
        if(amount(facts,field).equals(replacement)) return new Decision(facts,null,operation,field.name());
        CaseFacts next=new CaseFacts(facts.scope(),field==AmountField.SALARY?replacement:facts.salary(),
                field==AmountField.SAVINGS_INTEREST?replacement:facts.savingsInterest(),
                field==AmountField.DEPOSIT_INTEREST?replacement:facts.qualifyingDepositInterest(),
                field==AmountField.OTHER_INTEREST?replacement:facts.otherInterest(),
                Fact.unknown(),Map.of(Regime.NEW,Fact.unknown(),Regime.OLD,Fact.unknown()));
        return new Decision(next,null,operation,field.name());
    }
    private static Decision inventory(CaseFacts facts,Inventory inventory,Fact<Boolean> value,String operation) {
        Map<Regime,Fact<Boolean>> deductions=new EnumMap<>(facts.deductionInventoryComplete());
        Fact<Boolean> income=facts.incomeInventoryComplete();
        if(inventory==Inventory.INCOME) {
            income=value;
            if(!value.confirmedValue().orElse(false)) { deductions.put(Regime.NEW,Fact.unknown()); deductions.put(Regime.OLD,Fact.unknown()); }
        } else deductions.put(inventory==Inventory.NEW_DEDUCTIONS?Regime.NEW:Regime.OLD,value);
        return new Decision(new CaseFacts(facts.scope(),facts.salary(),facts.savingsInterest(),facts.qualifyingDepositInterest(),
                facts.otherInterest(),income,deductions),null,operation,inventory.name());
    }

    /** Exact bounded request binding. This hash is private metadata, not anonymization or authentication. */
    static String fingerprint(Request request) {
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream(); DataOutputStream out=new DataOutputStream(bytes);
            text(out,"tax-platform/fact-edit-request/v1");
            CalculationKey k=request.expectedKey();
            for(String part:List.of(k.tenantId(),k.profileId(),k.caseId(),k.snapshotId(),k.periodKey(),k.ruleBundleHash(),
                    k.engineDigest(),k.verifierDigest(),k.contractVersion(),k.settingsHash())) text(out,part);
            Command c=request.command();
            text(out,c.getClass().getSimpleName());
            if(c instanceof ProposeAmount p) {
                text(out,p.field().name()); out.writeBoolean(p.text()!=null);
                if(p.text()!=null) text(out,p.text()); text(out,p.unit()==null?"NO_UNIT":p.unit().name());
            } else if(c instanceof ConfirmAmount p) { text(out,p.field().name()); text(out,p.displayedAmount().decimal()); }
            else if(c instanceof ResolveConflict p) { text(out,p.field().name()); text(out,p.selectedAmount().decimal()); }
            else if(c instanceof ClearAmount p) text(out,p.field().name());
            else if(c instanceof ReviewInventory p) text(out,p.inventory().name());
            else if(c instanceof UnreviewInventory p) text(out,p.inventory().name());
            out.flush();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch(IOException impossible) { throw new IllegalStateException("in-memory request binding failed",impossible); }
        catch(NoSuchAlgorithmException unavailable) { throw new IllegalStateException("SHA-256 unavailable",unavailable); }
    }
    private static void text(DataOutputStream out,String value) throws IOException {
        byte[] bytes=value.getBytes(StandardCharsets.UTF_8); out.writeInt(bytes.length); out.write(bytes);
    }
}
