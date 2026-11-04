package com.taxplatform.startup;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded JSON transport. Money is always a string; numeric tokens are integers only. */
public final class Json {
    private Json(){ }
    public static Map<String,Object> object(byte[] bytes){
        return object(bytes,8_388_608);
    }
    static Map<String,Object> object(byte[] bytes,int limit){
        if(limit<1||limit>25_165_824||bytes.length==0||bytes.length>limit)throw new IllegalArgumentException("invalid JSON size");
        String text;try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
        catch(java.nio.charset.CharacterCodingException e){throw new IllegalArgumentException("invalid UTF8");}
        Parser parser=new Parser(text);Object value=parser.read(0);parser.space();if(parser.at!=text.length()||!(value instanceof Map<?,?>))throw new IllegalArgumentException("object required");return map(value);
    }
    public static Map<String,Object> map(Object value){if(!(value instanceof Map<?,?> input))throw new IllegalArgumentException("object required");Map<String,Object> out=new LinkedHashMap<>();input.forEach((k,v)->{if(!(k instanceof String s))throw new IllegalArgumentException("string key required");out.put(s,v);});return out;}
    public static List<Object> list(Object value){if(!(value instanceof List<?> input))throw new IllegalArgumentException("array required");return new ArrayList<>(input);}
    public static byte[] bytes(Object value){return encode(value).getBytes(StandardCharsets.UTF_8);}
    public static String encode(Object value){StringBuilder out=new StringBuilder();write(value,out,0);return out.toString();}
    private static void write(Object value,StringBuilder out,int depth){
        if(depth>32)throw new IllegalArgumentException("depth");
        if(value==null){out.append("null");return;}if(value instanceof String text){out.append('"');for(int i=0;i<text.length();i++){char c=text.charAt(i);switch(c){case '"'->out.append("\\\"");case '\\'->out.append("\\\\");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");default->{if(c<32)out.append(String.format("\\u%04x",(int)c));else out.append(c);}}}out.append('"');return;}
        if(value instanceof Boolean||value instanceof Long||value instanceof Integer){out.append(value);return;}
        if(value instanceof Map<?,?> map){out.append('{');boolean first=true;for(var e:map.entrySet()){if(!first)out.append(',');first=false;write((String)e.getKey(),out,depth+1);out.append(':');write(e.getValue(),out,depth+1);}out.append('}');return;}
        if(value instanceof List<?> list){out.append('[');boolean first=true;for(Object row:list){if(!first)out.append(',');first=false;write(row,out,depth+1);}out.append(']');return;}
        throw new IllegalArgumentException("unsupported JSON value");
    }
    private static final class Parser {
        final String text;int at;
        Parser(String text){this.text=text;}
        void space(){while(at<text.length()&&" \t\r\n".indexOf(text.charAt(at))>=0)at++;}
        char next(){if(at>=text.length())throw new IllegalArgumentException("truncated JSON");return text.charAt(at++);}
        Object read(int depth){space();if(depth>32)throw new IllegalArgumentException("depth");char c=next();
            if(c=='"')return string();if(c=='{'){Map<String,Object> out=new LinkedHashMap<>();space();if(at<text.length()&&text.charAt(at)=='}'){at++;return out;}while(true){space();if(next()!='"')throw new IllegalArgumentException("key required");String k=string();space();if(next()!=':'||out.containsKey(k))throw new IllegalArgumentException("duplicate or invalid key");out.put(k,read(depth+1));if(out.size()>10000)throw new IllegalArgumentException("too many keys");space();char end=next();if(end=='}')return out;if(end!=',')throw new IllegalArgumentException("separator");}}
            if(c=='['){List<Object> out=new ArrayList<>();space();if(at<text.length()&&text.charAt(at)==']'){at++;return out;}while(true){out.add(read(depth+1));if(out.size()>20000)throw new IllegalArgumentException("too many items");space();char end=next();if(end==']')return out;if(end!=',')throw new IllegalArgumentException("separator");}}
            for(String literal:List.of("true","false","null"))if(c==literal.charAt(0)&&text.startsWith(literal,at-1)){at+=literal.length()-1;return literal.equals("null")?null:Boolean.valueOf(literal);}
            int start=at-1;while(at<text.length()&&Character.isDigit(text.charAt(at)))at++;String token=text.substring(start,at);if(!token.matches("-?(?:0|[1-9][0-9]*)"))throw new IllegalArgumentException("integer required");try{return Long.valueOf(token);}catch(NumberFormatException e){throw new IllegalArgumentException("integer range");}
        }
        String string(){StringBuilder out=new StringBuilder();while(true){char c=next();if(c=='"')break;if(c<32)throw new IllegalArgumentException("control");if(c=='\\'){c=next();switch(c){case '"','\\','/'->out.append(c);case 'b'->out.append('\b');case 'f'->out.append('\f');case 'n'->out.append('\n');case 'r'->out.append('\r');case 't'->out.append('\t');case 'u'->{if(at+4>text.length())throw new IllegalArgumentException("escape");String hex=text.substring(at,at+4);if(!hex.matches("[0-9a-fA-F]{4}"))throw new IllegalArgumentException("escape");out.append((char)Integer.parseInt(hex,16));at+=4;}default->throw new IllegalArgumentException("escape");}}else out.append(c);if(out.length()>262144)throw new IllegalArgumentException("string size");}
            for(int i=0;i<out.length();i++){char c=out.charAt(i);if(Character.isHighSurrogate(c)){if(++i>=out.length()||!Character.isLowSurrogate(out.charAt(i)))throw new IllegalArgumentException("surrogate");}else if(Character.isLowSurrogate(c))throw new IllegalArgumentException("surrogate");}return out.toString();}
    }
}
