package com.taxplatform.planner;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Small explicit projection encoder. No automatic serialization of domain records. */
public final class JsonOutput {
    private JsonOutput(){ }
    public static String encode(Object value){return encode(value,0);}
    private static String encode(Object value,int depth){
        if(depth>12)throw new IllegalArgumentException("projection depth exceeded");
        if(value instanceof String text)return quote(text);
        if(value instanceof Boolean flag)return flag.toString();
        if(value instanceof Integer n)return n.toString();
        if(value instanceof Map<?,?> map){TreeMap<String,Object> ordered=new TreeMap<>();map.forEach((k,v)->{if(!(k instanceof String key))throw new IllegalArgumentException("string keys required");ordered.put(key,v);});
            return "{"+ordered.entrySet().stream().map(e->quote(e.getKey())+":"+encode(e.getValue(),depth+1)).collect(java.util.stream.Collectors.joining(","))+"}";}
        if(value instanceof List<?> list)return "["+list.stream().map(v->encode(v,depth+1)).collect(java.util.stream.Collectors.joining(","))+"]";
        throw new IllegalArgumentException("explicit safe projection required");
    }
    private static String quote(String text){StringBuilder out=new StringBuilder("\"");for(int i=0;i<text.length();i++){char c=text.charAt(i);
        if(c=='\"'||c=='\\')out.append('\\').append(c);else if(c<0x20)out.append(String.format(java.util.Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}return out.append('"').toString();}
}
