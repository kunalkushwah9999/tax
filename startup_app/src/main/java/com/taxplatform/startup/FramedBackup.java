package com.taxplatform.startup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Version-2 logical archive: bounded authenticated frames, archive binding and an authenticated final digest. */
public final class FramedBackup {
    private static final byte[] MAGIC="TAXFRM02".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final int FRAME_MAX=1_048_576,RECORD_MAX=100_000;
    private static final long FILE_MAX=1_073_741_824L;
    private FramedBackup(){ }
    @FunctionalInterface interface Visitor {void accept(String namespace,String id,Map<String,Object> row)throws IOException,GeneralSecurityException;}
    private static void privateParent(Path path)throws IOException{
        Path parent=path.getParent();if(Files.isSymbolicLink(parent)||Files.exists(path)||Files.isSymbolicLink(path))throw new IOException("new private backup target required");
        if(!Files.exists(parent)){Files.createDirectories(parent);if(Files.getFileStore(parent).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(parent,PosixFilePermissions.fromString("rwx------"));}
        if(Files.getFileStore(parent).supportsFileAttributeView("posix")&&Files.getPosixFilePermissions(parent).stream().anyMatch(p->p.name().startsWith("GROUP_")||p.name().startsWith("OTHERS_")))throw new IOException("backup parent must be private");
    }
    private static final class Writer implements AutoCloseable {
        final DataOutputStream out;final RecordCrypto key;final String binding;final MessageDigest digest;long sequence,bytes=24,count;final Map<String,Long> counts=new LinkedHashMap<>();
        Writer(Path file,Path keyFile,String backend)throws IOException,GeneralSecurityException{
            key=new RecordCrypto(keyFile,true);byte[] id=Crypto.random(16);binding=HexFormat.of().formatHex(id);digest=MessageDigest.getInstance("SHA-256");out=new DataOutputStream(Files.newOutputStream(file,StandardOpenOption.WRITE));out.write(MAGIC);out.write(id);for(String ns:ConcurrentMysqlStore.NAMES)counts.put(ns,0L);
            frame(Map.of("kind","BEGIN","format","taxdesk-framed-backup.v2","created_at",Instant.now().toString(),"source_backend",backend,"restore_policy","ISOLATED_ONLY","schema",1L),false);
        }
        void frame(Map<String,Object> value,boolean last)throws IOException,GeneralSecurityException{
            byte[] plain=Json.bytes(value);RecordCrypto.Encrypted encrypted;try{if(plain.length>FRAME_MAX-32)throw new IOException("backup frame too large");encrypted=key.encrypt("backup-frames",binding+":"+sequence,1,plain);}finally{Arrays.fill(plain,(byte)0);}
            int length=12+encrypted.ciphertext().length;bytes+=4L+length;if(bytes>FILE_MAX)throw new IOException("framed backup file bound exceeded");out.writeInt(length);out.write(encrypted.nonce());out.write(encrypted.ciphertext());if(!last)digest.update(java.nio.ByteBuffer.allocate(4+length).putInt(length).put(encrypted.nonce()).put(encrypted.ciphertext()).array());sequence++;
        }
        void record(String ns,String id,Map<String,Object> row)throws IOException,GeneralSecurityException{if(!counts.containsKey(ns)||!id.matches("[A-Za-z0-9._:/-]{1,180}")||++count>RECORD_MAX)throw new IOException("backup record bound");frame(Map.of("kind","RECORD","namespace",ns,"id",id,"value",row),false);counts.put(ns,counts.get(ns)+1);}
        void finish()throws IOException,GeneralSecurityException{frame(Map.of("kind","END","record_count",count,"record_counts",counts,"prior_frames_sha256",HexFormat.of().formatHex(digest.digest())),true);out.flush();}
        @Override public void close()throws IOException{try{out.close();}finally{key.close();}}
    }
    public static Map<String,Object> write(DataStore source,Path target,Path backupKey)throws Exception{
        Path destination=target.toAbsolutePath().normalize();if(destination.equals(backupKey.toAbsolutePath().normalize()))throw new IOException("separate backup key required");privateParent(destination);Path temporary=Files.createTempFile(destination.getParent(),"framed-",".pending");
        try{
            if(Files.getFileStore(temporary).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(temporary,PosixFilePermissions.fromString("rw-------"));
            try(Writer writer=new Writer(temporary,backupKey,source.backend())){
                if(source instanceof ConcurrentMysqlStore concurrent)concurrent.visitSnapshot(writer::record);
                else{
                    // Legacy stores are already bounded aggregates. New schema-2 exports stream their JDBC cursor.
                    try{source.read(state->{try{for(String ns:RecoveryArchive.TABLES)for(var entry:Workspace.table(state,ns).entrySet())writer.record(ns,entry.getKey(),Json.map(entry.getValue()));for(Object raw:Workspace.auditRows(state)){Map<String,Object> event=Json.map(raw);writer.record("audit",(String)event.get("id"),event);}return null;}catch(IOException|GeneralSecurityException e){throw new BackupFailure(e);}});}catch(BackupFailure e){throw e.getCause();}
                }
                writer.finish();
            }
            try(var channel=java.nio.channels.FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}Files.createLink(destination,temporary);
        }finally{Files.deleteIfExists(temporary);}
        return Map.of("status","ENCRYPTED_FRAMED_BACKUP_CREATED","format","taxdesk-framed-backup.v2","restore_policy","ISOLATED_ONLY","production_recovery_approved",false);
    }
    private static final class BackupFailure extends RuntimeException {private static final long serialVersionUID=1L;BackupFailure(Exception e){super(e);}@Override public synchronized Exception getCause(){return (Exception)super.getCause();}}
    static Map<String,Object> read(Path archive,Path keyFile,Visitor visitor)throws IOException,GeneralSecurityException{
        if(Files.isSymbolicLink(archive)||!Files.isRegularFile(archive)||Files.size(archive)>FILE_MAX)throw new IOException("bounded regular archive required");
        try(DataInputStream in=new DataInputStream(Files.newInputStream(archive));RecordCrypto key=new RecordCrypto(keyFile,false)){
            if(!Arrays.equals(in.readNBytes(8),MAGIC))throw new IOException("framed backup required");byte[] id=in.readNBytes(16);if(id.length!=16)throw new EOFException("archive binding missing");String binding=HexFormat.of().formatHex(id);MessageDigest digest=MessageDigest.getInstance("SHA-256");long sequence=0,count=0,bytes=24;Set<String> seen=new LinkedHashSet<>();Map<String,Long> counts=new LinkedHashMap<>();for(String ns:ConcurrentMysqlStore.NAMES)counts.put(ns,0L);Map<String,Object> header=null;
            while(true){int length;try{length=in.readInt();}catch(EOFException e){throw new IOException("authenticated footer missing",e);}if(length<28||length>FRAME_MAX||(bytes+=4L+length)>FILE_MAX)throw new IOException("invalid frame length");byte[] wire=in.readNBytes(length);if(wire.length!=length)throw new IOException("truncated frame");byte[] plain=key.decrypt("backup-frames",binding+":"+sequence,1,Arrays.copyOf(wire,12),Arrays.copyOfRange(wire,12,wire.length));Map<String,Object> value;try{value=Json.object(plain,FRAME_MAX);}finally{Arrays.fill(plain,(byte)0);}String kind=Workspace.text(value,"kind",1,10);
                if(sequence==0){Workspace.fields(value,"kind","format","created_at","source_backend","restore_policy","schema");if(!"BEGIN".equals(kind)||!"taxdesk-framed-backup.v2".equals(value.get("format"))||!"ISOLATED_ONLY".equals(value.get("restore_policy"))||!Long.valueOf(1).equals(value.get("schema")))throw new IOException("invalid backup header");Instant.parse(Workspace.text(value,"created_at",1,64));Workspace.text(value,"source_backend",1,80);header=value;}
                else if(kind.equals("RECORD")){Workspace.fields(value,"kind","namespace","id","value");String ns=Workspace.text(value,"namespace",1,16),recordId=Workspace.text(value,"id",1,180);if(!counts.containsKey(ns)||!recordId.matches("[A-Za-z0-9._:/-]{1,180}")||!seen.add(ns+":"+recordId)||++count>RECORD_MAX)throw new IOException("duplicate or invalid record");Map<String,Object> row=Json.map(value.get("value"));if(Json.bytes(row).length>262144)throw new IOException("restored record bound");counts.put(ns,counts.get(ns)+1);if(visitor!=null)visitor.accept(ns,recordId,row);}
                else if(kind.equals("END")){Workspace.fields(value,"kind","record_count","record_counts","prior_frames_sha256");if(Workspace.number(value,"record_count")!=count||!Json.map(value.get("record_counts")).equals(counts)||!Crypto.equal(HexFormat.of().formatHex(digest.digest()),Workspace.text(value,"prior_frames_sha256",64,64))||in.read()!=-1)throw new IOException("footer count digest or EOF mismatch");return Map.of("status","FRAMED_BACKUP_AUTHENTICATED","format","taxdesk-framed-backup.v2","created_at",header.get("created_at"),"source_backend",header.get("source_backend"),"record_counts",counts,"restore_policy","ISOLATED_ONLY","production_recovery_approved",false);}
                else throw new IOException("invalid frame order");
                digest.update(java.nio.ByteBuffer.allocate(4+length).putInt(length).put(wire).array());sequence++;
            }
        }catch(Workspace.Problem|IllegalArgumentException e){throw new IOException("invalid framed backup payload",e);}
    }
    public static Map<String,Object> verify(Path archive,Path keyFile)throws IOException,GeneralSecurityException{return read(archive,keyFile,null);}
    public static boolean framed(Path archive)throws IOException{try(var in=Files.newInputStream(archive)){return Arrays.equals(in.readNBytes(8),MAGIC);}}
}
