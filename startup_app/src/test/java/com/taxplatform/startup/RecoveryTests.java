package com.taxplatform.startup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class RecoveryTests {
    private static int passed;
    private RecoveryTests(){ }
    @FunctionalInterface private interface Test{void run()throws Exception;}
    private static void check(String name,Test test)throws Exception{test.run();passed++;System.out.println("PASS "+name);}
    private static void eq(Object a,Object b){if(!java.util.Objects.equals(a,b))throw new AssertionError("recovery assertion failed");}
    private static void rejected(Test test)throws Exception{try{test.run();}catch(java.io.IOException|java.security.GeneralSecurityException|IllegalArgumentException expected){return;}throw new AssertionError("unsafe backup accepted");}
    public static void main(String[] args)throws Exception{
        Path directory=Files.createTempDirectory("logical-recovery-unit-");try{
            Path archive=directory.resolve("backups/snapshot.txb"),backupKey=directory.resolve("backup-keys/key");
            try(VaultStore store=new VaultStore(directory.resolve("data"),directory.resolve("data-keys/key"))){Workspace w=new Workspace(store,Clock.systemUTC());String owner=(String)w.register(SelfServiceTests.body("username","backup_owner","display_name","Synthetic owner","password","Synthetic-"+Workspace.id())).get("id");String id=(String)w.createCase(owner,SelfServiceTests.body("title","Synthetic snapshot","request_id",Workspace.id())).get("id");w.gstCommand(owner,id,SelfServiceTests.importBody(w,owner,id,SelfServiceTests.books(),SelfServiceTests.statement()));
                check("encrypted backup authenticates complete consistent business state",()->{RecoveryArchive.write(store,archive,backupKey);var payload=RecoveryArchive.load(archive,backupKey);eq(RecoveryArchive.validate(payload),store.read(state->Json.object(Json.bytes(state))));eq(RecoveryArchive.summary(payload).get("restore_policy"),"ISOLATED_ONLY");});
                check("legacy archive without operations upgrades only to an empty namespace",()->{var payload=RecoveryArchive.load(archive,backupKey);var legacy=Json.map(payload.get("data"));legacy.remove("operations");payload.put("data",legacy);var upgraded=RecoveryArchive.validate(payload);eq(Json.map(upgraded.get("operations")).size(),0);eq(upgraded.get("cases"),legacy.get("cases"));});
                check("on-disk archive does not contain source aliases invoice or password verifier",()->{String raw=new String(Files.readAllBytes(archive),java.nio.charset.StandardCharsets.ISO_8859_1);for(String secret:List.of("backup_owner","SUPPLIER_A","INV/01","1000.00","credential"))eq(raw.contains(secret),false);});
                check("existing backup is never overwritten",()->{byte[] bytes=Files.readAllBytes(archive);rejected(()->RecoveryArchive.write(store,archive,backupKey));eq(Arrays.equals(bytes,Files.readAllBytes(archive)),true);});
                check("fresh nonce makes equal-source backup ciphertext distinct",()->{Path other=directory.resolve("backups/other.txb");RecoveryArchive.write(store,other,backupKey);eq(Arrays.equals(Files.readAllBytes(archive),Files.readAllBytes(other)),false);});
                check("missing backup key cannot be recreated during verification",()->{Path missing=directory.resolve("missing-backup-key");rejected(()->RecoveryArchive.load(archive,missing));eq(Files.exists(missing),false);});
                check("wrong backup key rejected cryptographically",()->{Path wrong=directory.resolve("wrong-key/key");try(RecordCrypto crypto=new RecordCrypto(wrong,true)){crypto.encrypt("test","test",1,new byte[]{1});}rejected(()->RecoveryArchive.load(archive,wrong));});
                check("tampering truncation and unrelated framing reject",()->{byte[] bytes=Files.readAllBytes(archive);for(int at:new int[]{0,8,20,bytes.length-1}){byte[] corrupt=bytes.clone();corrupt[at]^=1;Path target=directory.resolve("corrupt-"+at);Files.write(target,corrupt);rejected(()->RecoveryArchive.load(target,backupKey));}Path truncated=directory.resolve("truncated");Files.write(truncated,Arrays.copyOf(bytes,bytes.length-1));rejected(()->RecoveryArchive.load(truncated,backupKey));});
                check("restore contract cannot assert an active or promoted snapshot",()->{var payload=RecoveryArchive.load(archive,backupKey);payload.put("restore_policy","ACTIVE");rejected(()->RecoveryArchive.validate(payload));});
            }
        }finally{try(var paths=Files.walk(directory)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
        System.out.println(Json.encode(Map.of("recovery_unit_groups",passed,"mysql_restore_exercised",false,"production_recovery_approved",false)));
    }
}
