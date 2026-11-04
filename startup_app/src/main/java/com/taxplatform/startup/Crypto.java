package com.taxplatform.startup;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class Crypto {
    static final SecureRandom RANDOM=new SecureRandom();
    static final int ITERATIONS=600_000;
    private Crypto(){ }
    static String nonce(){return Base64.getUrlEncoder().withoutPadding().encodeToString(random(32));}
    static byte[] random(int count){byte[] bytes=new byte[count];RANDOM.nextBytes(bytes);return bytes;}
    static String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
    static boolean equal(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),b.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    static Map<String,Object> password(String password){validatePassword(password);String salt=Base64.getEncoder().encodeToString(random(16));return Map.of("salt",salt,"iterations",ITERATIONS,"hash",derive(password,salt,ITERATIONS));}
    static void validatePassword(String value){if(value==null||value.length()<12||value.length()>128||value.chars().anyMatch(c->c<32))throw new Workspace.Problem(400,"PASSWORD_LENGTH_12_TO_128");}
    static boolean verifies(String password,Map<String,Object> stored){return equal((String)stored.get("hash"),derive(password,(String)stored.get("salt"),((Number)stored.get("iterations")).intValue()));}
    private static String derive(String password,String salt,int iterations){char[] chars=password.toCharArray();PBEKeySpec spec=new PBEKeySpec(chars,Base64.getDecoder().decode(salt),iterations,256);try{return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());}catch(GeneralSecurityException e){throw new IllegalStateException(e);}finally{spec.clearPassword();Arrays.fill(chars,'\0');}}
}
