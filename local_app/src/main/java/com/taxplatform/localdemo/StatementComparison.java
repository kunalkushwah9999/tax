package com.taxplatform.localdemo;

import com.taxplatform.safety.Money;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Normalized sample observations only. No native AIS/26AS parser, tax credit or source authentication. */
final class StatementComparison {
    enum Source { MANUAL, AIS, FORM26AS, BANK, FORM16 }
    enum Category { SALARY, SAVINGS_INTEREST, DEPOSIT_INTEREST, OTHER_INTEREST, OTHER_INCOME }
    enum Coverage { ANNUAL, TRANSACTION }
    enum Kind { REPORTED, PROCESSED, ACCEPTED }
    enum Status { AGREEMENT, AMOUNT_CONFLICT, VALUE_MISSING, SINGLE_SOURCE, REPEATED_SOURCE_OBSERVATIONS, SOURCE_VARIANTS }
    record Observation(String id,Source source,Category category,String payer,String period,Coverage coverage,
            String reference,Optional<Money> gross,Optional<Money> tds,Kind kind) {
        Observation {
            token(id);token(payer);token(reference);
            if(!period.equals("FY2025-26"))throw new IllegalArgumentException("comparison period outside starter");
            java.util.Objects.requireNonNull(source);java.util.Objects.requireNonNull(category);java.util.Objects.requireNonNull(coverage);
            java.util.Objects.requireNonNull(gross);java.util.Objects.requireNonNull(tds);java.util.Objects.requireNonNull(kind);
            if(source!=Source.AIS && kind!=Kind.REPORTED)throw new IllegalArgumentException("source value kind not supported");
        }
        Map<String,Object> projection(){return Map.of("observation_id",id,"source",source.name(),"value_kind",kind.name(),
                "gross_amount",gross.map(Money::decimal).orElse("UNKNOWN"),"tds_amount",tds.map(Money::decimal).orElse("UNKNOWN"));}
        @Override public String toString(){return "StatementObservation[redacted]";}
    }
    record Group(String id,Category category,String payer,String period,Coverage coverage,String reference,
            List<Observation> observations,Status incomeStatus,Status tdsStatus) {
        Group {observations=List.copyOf(observations);}
        @Override public String toString(){return "StatementGroup[redacted]";}
    }
    static final List<String> HEADER=List.of("observation_id","source","category","payer_alias","period","coverage","reference","gross_amount","tds_amount","value_kind");
    private StatementComparison(){ }
    static List<Observation> parse(byte[] bytes) {
        if(bytes.length==0 || bytes.length>32768)throw new IllegalArgumentException("bounded CSV required");
        String text;
        try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
        catch(java.nio.charset.CharacterCodingException invalid){throw new IllegalArgumentException("valid UTF8 required");}
        if(text.startsWith("\uFEFF"))text=text.substring(1);
        List<List<String>> rows=csv(text);
        if(rows.size()<2 || rows.size()>201 || !rows.get(0).equals(HEADER))throw new IllegalArgumentException("exact CSV header and one to two hundred observations required");
        List<Observation> result=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(int i=1;i<rows.size();i++) {
            List<String> c=rows.get(i);if(c.size()!=10 || !ids.add(c.get(0)))throw new IllegalArgumentException("duplicate ID or invalid row width");
            result.add(new Observation(c.get(0),Source.valueOf(c.get(1)),Category.valueOf(c.get(2)),c.get(3),c.get(4),Coverage.valueOf(c.get(5)),
                    c.get(6),amount(c.get(7)),amount(c.get(8)),Kind.valueOf(c.get(9))));
        }
        return List.copyOf(result);
    }
    private static Optional<Money> amount(String value){return value.equals("UNKNOWN")?Optional.empty():Optional.of(Money.parse(value));}
    private static void token(String value){if(value==null || !value.matches("[A-Za-z][A-Za-z0-9_-]{0,47}"))throw new IllegalArgumentException("bounded ASCII alias required");}
    private static List<List<String>> csv(String text) {
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();
        boolean quoted=false,closed=false,atStart=true;
        for(int i=0;i<text.length();i++) {
            char ch=text.charAt(i);
            if(quoted) {
                if(ch=='"'){if(i+1<text.length() && text.charAt(i+1)=='"'){cell.append('"');i++;}else{quoted=false;closed=true;}}
                else if(ch=='\r'||ch=='\n')throw new IllegalArgumentException("multiline fields not supported");else cell.append(ch);
            } else if(ch=='"') {
                if(!atStart || closed)throw new IllegalArgumentException("unexpected quote");quoted=true;atStart=false;
            } else if(ch==',' || ch=='\n' || ch=='\r') {
                row.add(cell.toString());cell.setLength(0);atStart=true;closed=false;
                if(row.size()>10)throw new IllegalArgumentException("too many columns");
                if(ch!=',') {if(ch=='\r'){if(i+1>=text.length() || text.charAt(i+1)!='\n')throw new IllegalArgumentException("bare CR unsupported");i++;}
                    rows.add(List.copyOf(row));row.clear();if(rows.size()>201)throw new IllegalArgumentException("too many rows");}
            } else {if(closed || ch<0x20 || ch==0x7f)throw new IllegalArgumentException("invalid cell character");cell.append(ch);atStart=false;}
            if(cell.length()>64)throw new IllegalArgumentException("cell too long");
        }
        if(quoted)throw new IllegalArgumentException("unclosed quote");
        if(!row.isEmpty() || cell.length()>0 || closed){row.add(cell.toString());rows.add(List.copyOf(row));}
        return List.copyOf(rows);
    }
    static List<Group> group(List<Observation> input) {
        if(input.isEmpty() || input.size()>200)throw new IllegalArgumentException("bounded observations required");
        Map<String,List<Observation>> grouped=new TreeMap<>();Set<String> ids=new HashSet<>();
        for(Observation row:input){if(!ids.add(row.id()))throw new IllegalArgumentException("duplicate observation ID");
            String key=String.join("/",row.category().name(),row.payer(),row.period(),row.coverage().name(),row.reference());
            grouped.computeIfAbsent(key,k->new ArrayList<>()).add(row);}
        List<Group> result=new ArrayList<>();
        grouped.forEach((key,values)->{List<Observation> sorted=values.stream().sorted(Comparator.comparing(Observation::id)).toList();Observation first=sorted.get(0);
            result.add(new Group(hash(key),first.category(),first.payer(),first.period(),first.coverage(),first.reference(),sorted,status(sorted,true),status(sorted,false)));});
        return List.copyOf(result);
    }
    private static Status status(List<Observation> rows,boolean income) {return flags(rows,income).get(0);}
    static List<Status> flags(List<Observation> rows,boolean income) {
        Set<String> identities=new HashSet<>();Set<Source> sources=new HashSet<>();Set<Kind> kinds=new HashSet<>();List<Status> flags=new ArrayList<>();boolean repeated=false;
        for(Observation row:rows){if(!identities.add(row.source().name()+"/"+row.kind().name()))repeated=true;sources.add(row.source());kinds.add(row.kind());}
        if(repeated)flags.add(Status.REPEATED_SOURCE_OBSERVATIONS);
        if(rows.stream().anyMatch(r->(income?r.gross():r.tds()).isEmpty()))flags.add(Status.VALUE_MISSING);
        if(rows.stream().map(r->income?r.gross():r.tds()).flatMap(Optional::stream).distinct().count()>1)flags.add(Status.AMOUNT_CONFLICT);
        if(kinds.size()>1)flags.add(Status.SOURCE_VARIANTS);
        if(sources.size()<2)flags.add(Status.SINGLE_SOURCE);
        if(flags.isEmpty())flags.add(Status.AGREEMENT);
        return List.copyOf(flags);
    }
    static String fingerprint(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private static String hash(String text){return fingerprint(text.getBytes(StandardCharsets.UTF_8));}
}
