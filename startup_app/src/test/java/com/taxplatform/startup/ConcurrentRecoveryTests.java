package com.taxplatform.startup;
import java.nio.file.*;
import java.util.*;
final class ConcurrentRecoveryTests {
 private static int passed;private ConcurrentRecoveryTests(){ }
 @FunctionalInterface interface Test {void run()throws Exception;}
 static void check(String name,Test test)throws Exception{test.run();passed++;System.out.println("PASS "+name);}
 static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError("concurrent recovery assertion");}
 static void reject(Test test)throws Exception{try{test.run();}catch(java.io.IOException|java.security.GeneralSecurityException|java.sql.SQLException expected){return;}throw new AssertionError("unsafe restore accepted");}
 public static void main(String[] args)throws Exception{
  var config=SqlConfig.load(Path.of(args[0]));Path key=Path.of(args[1]);var target=SqlConfig.load(Path.of(args[2]));Path targetKey=Path.of(args[3]),folder=Path.of(args[4]),report=Path.of(args[5]);Path archive=folder.resolve("backups/framed.txb"),backupKey=folder.resolve("backup-keys/key");Map<String,Object> expected;
  try(ConcurrentMysqlStore store=new ConcurrentMysqlStore(config,key)){
   expected=store.read(state->Json.object(Json.bytes(state),25_165_824));
   check("framed archive streams a real SQL snapshot larger than 16 MiB",()->{var result=FramedBackup.write(store,archive,backupKey);eq(result.get("status"),"ENCRYPTED_FRAMED_BACKUP_CREATED");eq(Files.size(archive)>16_777_216,true);});
   check("offline verification authenticates footer counts without exposing records",()->{var result=FramedBackup.verify(archive,backupKey);eq(result.get("status"),"FRAMED_BACKUP_AUTHENTICATED");eq(result.get("restore_policy"),"ISOLATED_ONLY");eq(Json.map(result.get("record_counts")).get("operations"),Long.valueOf(Workspace.table(expected,"operations").size()));});
   check("backup never overwrites an existing destination",()->reject(()->FramedBackup.write(store,archive,backupKey)));
  }
  check("tamper truncate archive binding and extra bytes all reject",()->{byte[] original=Files.readAllBytes(archive);for(int at:new int[]{0,10,40,original.length/2,original.length-1}){byte[] data=original.clone();data[at]^=1;Path p=folder.resolve("corrupt-"+at);Files.write(p,data);reject(()->FramedBackup.verify(p,backupKey));Files.delete(p);}Path p=folder.resolve("truncated");Files.write(p,Arrays.copyOf(original,original.length-1));reject(()->FramedBackup.verify(p,backupKey));Files.write(p,Arrays.copyOf(original,original.length+1));reject(()->FramedBackup.verify(p,backupKey));Files.delete(p);});
  check("wrong backup key is an authenticated rejection",()->{Path wrong=folder.resolve("wrong/key");try(RecordCrypto c=new RecordCrypto(wrong,true)){c.encrypt("test","test",1,new byte[]{1});}reject(()->FramedBackup.verify(archive,wrong));});
  check("missing backup key is not silently created",()->{Path missing=folder.resolve("missing/key");reject(()->FramedBackup.verify(archive,missing));eq(Files.exists(missing),false);});
  check("framed restore uses a fresh isolated database and fresh record key",()->{var result=ConcurrentMysqlStore.restoreFramed(target,targetKey,archive,backupKey);eq(result.get("status"),"RESTORED_ISOLATED");eq(result.get("application_start_allowed"),false);});
  check("all restored business rows audit and shard accounting match snapshot",()->eq(ConcurrentMysqlStore.inspectIsolated(target,targetKey),expected));
  check("normal concurrent server and initializer refuse restored data",()->{reject(()->{try(ConcurrentMysqlStore ignored=new ConcurrentMysqlStore(target,targetKey)){ignored.backend();}});reject(()->ConcurrentMysqlStore.initialize(target,targetKey));});
  check("existing target and reused source key cannot be overwritten",()->{reject(()->ConcurrentMysqlStore.restoreFramed(target,targetKey,archive,backupKey));reject(()->ConcurrentMysqlStore.restoreFramed(config,key,archive,backupKey));eq(ConcurrentMysqlStore.inspectIsolated(target,targetKey),expected);});
  try(ConcurrentMysqlStore source=new ConcurrentMysqlStore(config,key)){source.transaction(state->{for(int at=0;at<90;at++)Workspace.table(state,"operations").remove("large-record-"+at);for(int at=0;at<2;at++)Workspace.table(state,"quotes").remove("capacity-"+at);return null;});}
  Map<String,Object> result=Map.of("concurrent_restore_groups",passed,"actual_mysql",true,"framed_backup_larger_than_16mib",true,"restored_full_state_equal",true,"restore_stays_isolated",true,"production_recovery_approved",false);Files.write(report,Json.bytes(result));System.out.println(Json.encode(result));
 }
}
