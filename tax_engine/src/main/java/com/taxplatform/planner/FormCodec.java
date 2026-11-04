package com.taxplatform.planner;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded flat UTF-8 form adapter. Not JSON deserialization or trusted-domain authority. */
public final class FormCodec {
    private FormCodec(){ }
    public static Map<String,String> decode(byte[] bytes) {
        if(bytes.length==0 || bytes.length>16384)throw new IllegalArgumentException("bounded form required");
        String form=decodeUtf8(bytes);String[] pairs=form.split("&",-1);
        if(pairs.length>40)throw new IllegalArgumentException("too many fields");
        Map<String,String> result=new LinkedHashMap<>();
        for(String pair:pairs) {
            String[] bits=pair.split("=",2);if(bits.length!=2)throw new IllegalArgumentException("name/value pair required");
            String key=component(bits[0]),value=component(bits[1]);
            if(!key.matches("[a-z_][a-z_0-9]{0,39}") || value.length()>1024 || result.putIfAbsent(key,value)!=null)
                throw new IllegalArgumentException("invalid or duplicate field");
        }
        return Map.copyOf(result);
    }
    private static String component(String text) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(int i=0;i<text.length();) {
            char c=text.charAt(i);
            if(c=='%') {
                if(i+2>=text.length())throw new IllegalArgumentException("invalid percent escape");
                int a=hex(text.charAt(i+1)),b=hex(text.charAt(i+2));if(a<0 || b<0)throw new IllegalArgumentException("invalid percent escape");
                out.write(a*16+b);i+=3;
            } else if(c=='+'){out.write(' ');i++;}
            else {int cp=text.codePointAt(i);byte[] utf8=new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8);out.writeBytes(utf8);i+=Character.charCount(cp);}
        }
        return decodeUtf8(out.toByteArray());
    }
    private static int hex(char c){return c>='0'&&c<='9'?c-'0':c>='a'&&c<='f'?c-'a'+10:c>='A'&&c<='F'?c-'A'+10:-1;}
    private static String decodeUtf8(byte[] bytes) {
        try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
        catch(CharacterCodingException invalid){throw new IllegalArgumentException("invalid UTF-8");}
    }
}
