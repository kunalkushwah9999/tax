package com.taxplatform.startup;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 4226/6238 building block. Account state owns replay prevention and throttling. */
final class Totp {
    private static final String ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private Totp(){ }
    static String secret(){return base32(Crypto.random(20));}
    static String base32(byte[] value){StringBuilder s=new StringBuilder();int buffer=0,bits=0;for(byte b:value){buffer=(buffer<<8)|(b&255);bits+=8;while(bits>=5){bits-=5;s.append(ALPHABET.charAt((buffer>>>bits)&31));}}if(bits>0)s.append(ALPHABET.charAt((buffer<<(5-bits))&31));return s.toString();}
    static byte[] decode(String value){if(!value.matches("[A-Z2-7]{32}"))throw new IllegalArgumentException("TOTP secret format");byte[] out=new byte[20];int buffer=0,bits=0,index=0;for(char c:value.toCharArray()){buffer=(buffer<<5)|ALPHABET.indexOf(c);bits+=5;if(bits>=8){bits-=8;out[index++]=(byte)(buffer>>>bits);}}return out;}
    static String at(String secret,long step,int digits){if(step<0||digits<6||digits>8)throw new IllegalArgumentException("TOTP parameters");byte[] key=decode(secret),digest=null;try{Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(key,"HmacSHA1"));digest=mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());int offset=digest[digest.length-1]&15;int n=((digest[offset]&127)<<24)|((digest[offset+1]&255)<<16)|((digest[offset+2]&255)<<8)|(digest[offset+3]&255);int mod=digits==6?1_000_000:digits==7?10_000_000:100_000_000;return String.format(java.util.Locale.ROOT,"%0"+digits+"d",n%mod);}catch(GeneralSecurityException e){throw new IllegalStateException(e);}finally{Arrays.fill(key,(byte)0);if(digest!=null)Arrays.fill(digest,(byte)0);}}
    static long verify(String secret,String code,Instant now,long lastStep){if(code==null||!code.matches("[0-9]{6}"))return -1;long current=now.getEpochSecond()/30,matched=-1;for(long step=Math.max(0,current-1);step<=current+1;step++){boolean equal=Crypto.equal(at(secret,step,6),code);if(equal&&step>lastStep&&matched<0)matched=step;}return matched;}
}
