package com.taxplatform.planner;

import com.taxplatform.safety.Money;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/** Explicit operator-only private workings. Never wired to the anonymous public result response. */
public final class PlannerCli {
    private PlannerCli(){ }
    public static void main(String[] args)throws Exception {
        if(args.length<1 || args.length>2){System.err.println("usage: REQUEST.form [DECLARED_ADDITIONAL_80C_CAPACITY_INR]");System.exit(2);return;}
        Path path=Path.of(args[0]);if(Files.size(path)>16384)throw new IllegalArgumentException("request too large");
        PlannerInput input=PlannerForms.parse(FormCodec.decode(Files.readAllBytes(path)));
        Map<String,Object> response=new TreeMap<>(new DualPlanner().privateReview(input));
        if(args.length==2)response.put("break_even_sensitivity",new BreakEven().analyze(input,Money.parse(args[1])).reviewProjection());
        System.out.println(JsonOutput.encode(response));
    }
}
