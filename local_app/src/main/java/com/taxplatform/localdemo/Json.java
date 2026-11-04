package com.taxplatform.localdemo;

import java.util.List;
import java.util.Map;

/** Small encoder for bounded demo projections; it accepts no client JSON. */
final class Json {
    private Json() { }
    static String encode(Object value) { StringBuilder out=new StringBuilder();append(value,out,0);return out.toString(); }
    private static void append(Object value,StringBuilder out,int depth) {
        if(depth>12)throw new IllegalArgumentException("projection nesting limit");
        if(value instanceof String text) {
            out.append('"');for(int i=0;i<text.length();i++){
                char c=text.charAt(i);
                if(c=='"'||c=='\\')out.append('\\').append(c);
                else if(c<0x20 || c=='\u2028' || c=='\u2029')out.append(String.format("\\u%04x",(int)c));
                else out.append(c);
            }out.append('"');
        } else if(value instanceof Boolean b)out.append(b);
        else if(value instanceof Integer count && count>=0 && count<=1000000)out.append(count);
        else if(value instanceof Map<?,?> map) {
            out.append('{');boolean first=true;for(Map.Entry<?,?> entry:map.entrySet()){
                if(!(entry.getKey() instanceof String))throw new IllegalArgumentException("nontext projection key");
                if(!first)out.append(',');first=false;append(entry.getKey(),out,depth+1);out.append(':');append(entry.getValue(),out,depth+1);
            }out.append('}');
        } else if(value instanceof List<?> list){out.append('[');boolean first=true;for(Object item:list){if(!first)out.append(',');first=false;append(item,out,depth+1);}out.append(']');}
        else throw new IllegalArgumentException("unsupported demo projection type");
    }
}
