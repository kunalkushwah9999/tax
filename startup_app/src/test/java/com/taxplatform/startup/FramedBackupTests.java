package com.taxplatform.startup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

final class FramedBackupTests {
    private static int passed;private FramedBackupTests(){ }
    @FunctionalInterface interface Test {void run()throws Exception;}
    private static void check(String name,Test test)throws Exception{test.run();passed++;System.out.println("PASS "+name);}
    private static void eq(Object a,Object b){if(!java.util.Objects.equals(a,b))throw new AssertionError("framed archive assertion");}
    private static void reject(Test test)throws Exception{try{test.run();}catch(java.io.IOException|java.security.GeneralSecurityException expected){return;}throw new AssertionError("unsafe framed archive accepted");}
    public static void main(String[] args)throws Exception{
        Path folder=Files.createTempDirectory("framed-backup-tests-");try{
            Path archive=folder.resolve("snapshots/source.txb"),backupKey=folder.resolve("backup-keys/key");Map<String,Object> expected;
            try(VaultStore store=new VaultStore(folder.resolve("data"),folder.resolve("data-keys/key"))){
                Workspace w=new Workspace(store,Clock.systemUTC());String owner=(String)w.register(Map.of("username","frame_owner","display_name","Synthetic frame owner","password","Synthetic-"+Workspace.id())).get("id");w.createCase(owner,Map.of("request_id",Workspace.id(),"title","Synthetic frame example"));expected=store.read(s->Json.object(Json.bytes(s)));
                check("framed export authenticates a real legacy aggregate without changing it",()->{FramedBackup.write(store,archive,backupKey);eq(FramedBackup.verify(archive,backupKey).get("status"),"FRAMED_BACKUP_AUTHENTICATED");eq(store.read(s->Json.object(Json.bytes(s))),expected);});
                check("streamed records reconstruct all namespaces and original audit rows",()->{Map<String,Object> actual=new LinkedHashMap<>();for(String name:ConcurrentMysqlStore.NAMES)actual.put(name,new LinkedHashMap<String,Object>());FramedBackup.read(archive,backupKey,(ns,id,row)->Workspace.table(actual,ns).put(id,row));for(String name:RecoveryArchive.TABLES)eq(actual.get(name),expected.get(name));eq(Workspace.table(actual,"audit").size(),Workspace.auditRows(expected).size());});
                check("new archives use independent bindings and ciphertext for the same source",()->{Path other=folder.resolve("snapshots/other.txb");FramedBackup.write(store,other,backupKey);eq(Arrays.equals(Files.readAllBytes(archive),Files.readAllBytes(other)),false);});
                check("existing destinations are never replaced",()->{byte[] old=Files.readAllBytes(archive);reject(()->FramedBackup.write(store,archive,backupKey));eq(Arrays.equals(old,Files.readAllBytes(archive)),true);});
            }
            check("tamper truncated footer and trailing bytes fail verification",()->{byte[] original=Files.readAllBytes(archive);for(int at:new int[]{0,9,27,original.length/2,original.length-1}){byte[] copy=original.clone();copy[at]^=1;Path p=folder.resolve("bad-"+at);Files.write(p,copy);reject(()->FramedBackup.verify(p,backupKey));}Path p=folder.resolve("truncated");Files.write(p,Arrays.copyOf(original,original.length-1));reject(()->FramedBackup.verify(p,backupKey));Files.write(p,Arrays.copyOf(original,original.length+1));reject(()->FramedBackup.verify(p,backupKey));});
            check("oversized frame length rejects before allocation",()->{byte[] bytes=Files.readAllBytes(archive);java.nio.ByteBuffer.wrap(bytes,24,4).putInt(Integer.MAX_VALUE);Path p=folder.resolve("huge");Files.write(p,bytes);reject(()->FramedBackup.verify(p,backupKey));});
            check("missing verification key is not created",()->{Path missing=folder.resolve("missing/key");reject(()->FramedBackup.verify(archive,missing));eq(Files.exists(missing),false);});
            check("wrong key and a truncated archive header reject",()->{Path wrong=folder.resolve("wrong/key");try(RecordCrypto k=new RecordCrypto(wrong,true)){k.encrypt("test","test",1,new byte[]{1});}reject(()->FramedBackup.verify(archive,wrong));Path p=folder.resolve("short");Files.write(p,new byte[]{1,2,3});reject(()->FramedBackup.verify(p,backupKey));});
        }finally{try(var files=Files.walk(folder)){for(Path file:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}}
        System.out.println(Json.encode(Map.of("framed_backup_unit_groups",passed,"real_mysql_tested_by_this_suite",false,"production_recovery_approved",false)));
    }
}
