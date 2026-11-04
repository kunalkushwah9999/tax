package com.taxplatform.startup;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Operator-only encrypted logical snapshot. Restored state is never automatically served. */
public final class RecoveryArchive {
    static final List<String> TABLES=List.of("users","cases","grants","tickets","prices","quotes","receipts","operations");
    private static final byte[] MAGIC="TAXBKP01".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final int MAX=25_165_824;
    private RecoveryArchive(){ }
    static Map<String,Object> validate(Map<String,Object> payload){
        if(!payload.keySet().equals(Set.of("format","created_at","source_backend","data","restore_policy"))||!"taxdesk-logical-backup.v1".equals(payload.get("format"))||!"ISOLATED_ONLY".equals(payload.get("restore_policy")))throw new IllegalArgumentException("unsupported backup");
        Instant.parse((String)payload.get("created_at"));Map<String,Object> state=Json.map(payload.get("data"));state.putIfAbsent("operations",new LinkedHashMap<String,Object>());Set<String> names=new java.util.HashSet<>(TABLES);names.add("schema");names.add("audit");
        if(!state.keySet().equals(names)||!(state.get("schema") instanceof Number n)||n.longValue()!=1)throw new IllegalArgumentException("backup schema");
        for(String name:TABLES)for(var entry:Json.map(state.get(name)).entrySet()){if(!entry.getKey().matches("[A-Za-z0-9._:/-]{1,180}"))throw new IllegalArgumentException("backup record key");Json.map(entry.getValue());}
        for(Object event:Json.list(state.get("audit")))Json.map(event);
        return state;
    }
    public static Map<String,Object> write(DataStore store,Path target,Path backupKey)throws IOException,GeneralSecurityException{
        byte[] plain=store.read(state->Json.bytes(Map.of("format","taxdesk-logical-backup.v1","created_at",Instant.now().toString(),"source_backend",store.backend(),"data",state,"restore_policy","ISOLATED_ONLY")));
        if(plain.length>MAX){Arrays.fill(plain,(byte)0);throw new IOException("backup exceeds logical snapshot bound");}
        Path destination=target.toAbsolutePath().normalize(),parent=destination.getParent();if(destination.equals(backupKey.toAbsolutePath().normalize())||Files.isSymbolicLink(parent)||Files.exists(destination)||Files.isSymbolicLink(destination))throw new IOException("backup requires a new file in a private directory");
        if(!Files.exists(parent)){Files.createDirectories(parent);if(Files.getFileStore(parent).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(parent,PosixFilePermissions.fromString("rwx------"));}
        if(Files.getFileStore(parent).supportsFileAttributeView("posix")&&Files.getPosixFilePermissions(parent).stream().anyMatch(p->p.name().startsWith("GROUP_")||p.name().startsWith("OTHERS_")))throw new IOException("backup directory must be private");
        RecordCrypto.Encrypted encrypted;try(RecordCrypto crypto=new RecordCrypto(backupKey,true)){encrypted=crypto.encrypt("backup","logical-workspace",1,plain);}finally{Arrays.fill(plain,(byte)0);}
        Path temporary=Files.createTempFile(parent,"backup-",".pending");try{
            if(Files.getFileStore(temporary).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(temporary,PosixFilePermissions.fromString("rw-------"));
            try(FileChannel out=FileChannel.open(temporary,StandardOpenOption.WRITE)){for(byte[] bytes:List.of(MAGIC,encrypted.nonce(),encrypted.ciphertext())){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())out.write(buffer);}out.force(true);}
            Files.createLink(destination,temporary);
        }finally{Files.deleteIfExists(temporary);}
        return Map.of("status","ENCRYPTED_BACKUP_CREATED","restore_policy","ISOLATED_ONLY","production_recovery_approved",false);
    }
    public static Map<String,Object> load(Path archive,Path backupKey)throws IOException,GeneralSecurityException{
        if(Files.isSymbolicLink(archive)||!Files.isRegularFile(archive))throw new IOException("backup requires a regular file");byte[] bytes;try(var in=Files.newInputStream(archive)){bytes=in.readNBytes(MAX+37);}
        if(bytes.length<36||bytes.length>MAX+36||!Arrays.equals(MAGIC,Arrays.copyOf(bytes,8)))throw new IOException("invalid backup framing");
        byte[] plain;try(RecordCrypto crypto=new RecordCrypto(backupKey,false)){plain=crypto.decrypt("backup","logical-workspace",1,Arrays.copyOfRange(bytes,8,20),Arrays.copyOfRange(bytes,20,bytes.length));}
        try{Map<String,Object> payload=Json.object(plain,MAX);validate(payload);return payload;}finally{Arrays.fill(plain,(byte)0);Arrays.fill(bytes,(byte)0);}
    }
    public static Map<String,Object> summary(Map<String,Object> payload){Map<String,Object> state=validate(payload),counts=new LinkedHashMap<>();for(String name:TABLES)counts.put(name,Json.map(state.get(name)).size());counts.put("audit",Json.list(state.get("audit")).size());return Map.of("status","BACKUP_AUTHENTICATED","created_at",payload.get("created_at"),"source_backend",payload.get("source_backend"),"record_counts",counts,"restore_policy","ISOLATED_ONLY","production_recovery_approved",false);}
}
