package com.taxplatform.localdemo;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** A bounded form decoder: duplicate fields, malformed percent escapes and malformed UTF-8 reject. */
final class StrictForm {
    private StrictForm() { }
    static Map<String,String> parse(byte[] bytes) {
        if(bytes.length==0 || bytes.length>4096)throw new IllegalArgumentException("invalid form size");
        String raw=decode(bytes);Map<String,String> result=new LinkedHashMap<>();
        for(String pair:raw.split("&",-1)){
            int equals=pair.indexOf('=');if(equals<1)throw new IllegalArgumentException("invalid form field");
            String name=component(pair.substring(0,equals));String value=component(pair.substring(equals+1));
            if(!name.matches("[a-z_]{1,32}") || value.length()>256 || result.putIfAbsent(name,value)!=null)throw new IllegalArgumentException("invalid or duplicate form field");
        }
        if(result.size()>8)throw new IllegalArgumentException("too many form fields");
        return Map.copyOf(result);
    }
    private static String component(String text) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(int i=0;i<text.length();){
            char c=text.charAt(i);
            if(c=='%'){
                if(i+2>=text.length())throw new IllegalArgumentException("bad percent escape");
                int hi=hex(text.charAt(i+1)),lo=hex(text.charAt(i+2));
                if(hi<0||lo<0)throw new IllegalArgumentException("bad percent escape");out.write(hi*16+lo);i+=3;
            } else if(c=='+'){out.write(' ');i++;}
            else {
                int cp=text.codePointAt(i);byte[] encoded=new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8);out.writeBytes(encoded);i+=Character.charCount(cp);
            }
        }
        return decode(out.toByteArray());
    }
    private static int hex(char c){if(c>='0'&&c<='9')return c-'0';if(c>='a'&&c<='f')return c-'a'+10;if(c>='A'&&c<='F')return c-'A'+10;return -1;}
    private static String decode(byte[] bytes) {
        try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
        catch(CharacterCodingException invalid){throw new IllegalArgumentException("invalid UTF-8",invalid);}
    }
}
