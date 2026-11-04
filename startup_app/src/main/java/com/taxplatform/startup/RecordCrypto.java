package com.taxplatform.startup;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Per-record AEAD and separate purpose-derived username lookup key, both outside MySQL. */
final class RecordCrypto implements AutoCloseable {
    record Encrypted(byte[] nonce,byte[] ciphertext){ }
    private final byte[] aes,lookup;
    RecordCrypto(Path keyFile,boolean allowCreate)throws IOException,GeneralSecurityException{
        if(Files.isSymbolicLink(keyFile))throw new IOException("SQL data key symlink denied");
        if(!Files.exists(keyFile)){
            if(!allowCreate)throw new IOException("SQL data key unavailable");Path parent=keyFile.toAbsolutePath().getParent();if(Files.isSymbolicLink(parent))throw new IOException("SQL key directory symlink denied");Files.createDirectories(parent);
            if(Files.getFileStore(parent).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(parent,PosixFilePermissions.fromString("rwx------"));
            byte[] secret=Crypto.random(32);try(FileChannel out=FileChannel.open(keyFile,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){if(Files.getFileStore(keyFile).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(keyFile,PosixFilePermissions.fromString("rw-------"));ByteBuffer b=ByteBuffer.wrap(secret);while(b.hasRemaining())out.write(b);out.force(true);}finally{Arrays.fill(secret,(byte)0);}
        }
        if(Files.size(keyFile)!=32)throw new IOException("invalid SQL data key");if(Files.getFileStore(keyFile).supportsFileAttributeView("posix")&&Files.getPosixFilePermissions(keyFile).stream().anyMatch(p->p.name().startsWith("GROUP_")||p.name().startsWith("OTHERS_")))throw new IOException("SQL data key must be private");
        byte[] master=Files.readAllBytes(keyFile);try{aes=hmac(master,"tax-platform/mysql-record-aes/v1");lookup=hmac(master,"tax-platform/mysql-user-lookup/v1");}finally{Arrays.fill(master,(byte)0);}
    }
    static byte[] hmac(byte[] key,String text)throws GeneralSecurityException{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static byte[] aad(String namespace,String id,long revision){return Json.bytes(java.util.List.of("mysql-record.v1",namespace,id,revision));}
    Encrypted encrypt(String ns,String id,long version,byte[] plaintext)throws GeneralSecurityException{byte[] nonce=Crypto.random(12);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(aes,"AES"),new GCMParameterSpec(128,nonce));c.updateAAD(aad(ns,id,version));return new Encrypted(nonce,c.doFinal(plaintext));}
    byte[] decrypt(String ns,String id,long version,byte[] nonce,byte[] bytes)throws GeneralSecurityException{if(nonce.length!=12||version<1)throw new GeneralSecurityException("record binding invalid");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(aes,"AES"),new GCMParameterSpec(128,nonce));c.updateAAD(aad(ns,id,version));return c.doFinal(bytes);}
    byte[] usernameTag(String username)throws GeneralSecurityException{return hmac(lookup,username);}
    @Override public void close(){Arrays.fill(aes,(byte)0);Arrays.fill(lookup,(byte)0);}
}
