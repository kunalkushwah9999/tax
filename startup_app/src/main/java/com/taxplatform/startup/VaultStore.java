package com.taxplatform.startup;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Single-writer encrypted local development store. Not a SQL or multi-node adapter. */
public final class VaultStore implements DataStore {
    private static final byte[] AAD="tax-platform/startup-vault/v1".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private final Path file;private final byte[] key;private final FileChannel lockChannel;private final FileLock lock;
    private Map<String,Object> state;
    public VaultStore(Path directory,Path keyFile)throws IOException,GeneralSecurityException{
        privateDirectory(directory);privateDirectory(keyFile.toAbsolutePath().getParent());
        file=directory.resolve("workspace.vault");rejectLink(file);rejectLink(keyFile);Path lockPath=directory.resolve("writer.lock");rejectLink(lockPath);
        lockChannel=FileChannel.open(lockPath,StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock acquired;
        try{acquired=lockChannel.tryLock();}catch(java.nio.channels.OverlappingFileLockException e){lockChannel.close();throw new IOException("workspace already open");}
        if(acquired==null){lockChannel.close();throw new IOException("workspace already open");}lock=acquired;
        try{
            if(Files.exists(keyFile)){key=Files.readAllBytes(keyFile);if(key.length!=32)throw new IOException("invalid vault key");}
            else{if(Files.exists(file))throw new IOException("vault key unavailable");key=Crypto.random(32);try(FileChannel out=FileChannel.open(keyFile,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){privateFile(keyFile);ByteBuffer buffer=ByteBuffer.wrap(key);while(buffer.hasRemaining())out.write(buffer);out.force(true);}}
            if(Files.exists(file)){if(Files.size(file)>8_388_700)throw new IOException("vault size exceeded");byte[] bytes=Files.readAllBytes(file);if(bytes.length<29||bytes[0]!=1)throw new IOException("invalid vault format");Cipher cipher=cipher(Cipher.DECRYPT_MODE,java.util.Arrays.copyOfRange(bytes,1,13));state=Json.object(cipher.doFinal(java.util.Arrays.copyOfRange(bytes,13,bytes.length)));}
            else{state=empty();persist(state);}
            if(!Long.valueOf(1).equals(state.get("schema")))throw new IOException("unsupported schema");state.putIfAbsent("operations",new LinkedHashMap<String,Object>());
        }catch(IOException|GeneralSecurityException|RuntimeException failure){lock.release();lockChannel.close();throw failure;}
    }
    private static Map<String,Object> empty(){Map<String,Object> data=new LinkedHashMap<>();data.put("schema",1L);for(String field:List.of("users","cases","grants","tickets","prices","quotes","receipts","operations"))data.put(field,new LinkedHashMap<String,Object>());data.put("audit",new java.util.ArrayList<>());return data;}
    public synchronized <T>T read(Function<Map<String,Object>,T> work){return work.apply(copy(state));}
    public synchronized <T>T transaction(Function<Map<String,Object>,T> work){Map<String,Object> next=copy(state);T result=work.apply(next);try{persist(next);}catch(IOException|GeneralSecurityException e){throw new Workspace.Problem(503,"STORAGE_WRITE_FAILED");}state=next;return result;}
    private static Map<String,Object> copy(Map<String,Object> data){return Json.object(Json.bytes(data));}
    private Cipher cipher(int mode,byte[] nonce)throws GeneralSecurityException{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(mode,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));cipher.updateAAD(AAD);return cipher;}
    private void persist(Map<String,Object> data)throws IOException,GeneralSecurityException{
        byte[] plaintext=Json.bytes(data);if(plaintext.length>8_388_608)throw new IOException("vault capacity");byte[] nonce=Crypto.random(12),encrypted=cipher(Cipher.ENCRYPT_MODE,nonce).doFinal(plaintext);
        Path temp=Files.createTempFile(file.getParent(),"workspace-",".pending");try{privateFile(temp);try(FileChannel out=FileChannel.open(temp,StandardOpenOption.WRITE)){ByteBuffer bytes=ByteBuffer.allocate(13+encrypted.length).put((byte)1).put(nonce).put(encrypted);bytes.flip();while(bytes.hasRemaining())out.write(bytes);out.force(true);}Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);try(FileChannel directory=FileChannel.open(file.getParent(),StandardOpenOption.READ)){directory.force(true);}catch(IOException|UnsupportedOperationException e){/* The atomic replacement has committed; directory sync is platform-dependent. */}}
        finally{Files.deleteIfExists(temp);java.util.Arrays.fill(plaintext,(byte)0);}
    }
    private static void rejectLink(Path path)throws IOException{if(Files.isSymbolicLink(path))throw new IOException("symlinks not supported");}
    private static void privateDirectory(Path path)throws IOException{rejectLink(path);Files.createDirectories(path);if(!Files.isDirectory(path,LinkOption.NOFOLLOW_LINKS))throw new IOException("directory required");if(Files.getFileStore(path).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(path,PosixFilePermissions.fromString("rwx------"));}
    private static void privateFile(Path path)throws IOException{if(Files.getFileStore(path).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(path,PosixFilePermissions.fromString("rw-------"));}
    @Override public String backend(){return "LOCAL_ENCRYPTED_VAULT";}
    @Override public synchronized void close()throws IOException{java.util.Arrays.fill(key,(byte)0);lock.release();lockChannel.close();}
}
