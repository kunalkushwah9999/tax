package com.taxplatform.startup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class MysqlRestoreTests {
    private static int passed;
    private MysqlRestoreTests(){ }
    @FunctionalInterface private interface Test{void run()throws Exception;}
    private static void check(String name,Test test)throws Exception{test.run();passed++;System.out.println("PASS "+name);}
    private static void eq(Object a,Object b){if(!java.util.Objects.equals(a,b))throw new AssertionError("MySQL restore assertion failed");}
    private static String cli(Path launcher,Path config,Path key,Path driver,boolean success,String... extra)throws Exception{
        List<String> command=new ArrayList<>(List.of("python3",launcher.toString(),"--storage","mysql","--mysql-config",config.toString(),"--key-file",key.toString(),"--jdbc-jar",driver.toString()));command.addAll(List.of(extra));Process process=new ProcessBuilder(command).redirectErrorStream(true).start();
        if(!process.waitFor(25,TimeUnit.SECONDS)){process.destroyForcibly();throw new AssertionError("recovery CLI did not terminate");}String text=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);eq(process.exitValue()==0,success);if(!success)eq(text.contains("STARTUP_URL="),false);return text;
    }
    public static void main(String[] args)throws Exception{
        Path sourceConfig=Path.of(args[0]),sourceKey=Path.of(args[1]),targetConfig=Path.of(args[2]),targetKey=Path.of(args[3]),driver=Path.of(args[4]),launcher=Path.of(args[5]),report=Path.of(args[6]),folder=Path.of(args[7]);
        Path archive=folder.resolve("backups/logical.txb"),backupKey=folder.resolve("backup-secrets/backup.key");SqlConfig source=SqlConfig.load(sourceConfig),target=SqlConfig.load(targetConfig);String owner,id,grantId;Map<String,Object> snapshot;
        try(MysqlStore store=new MysqlStore(source,sourceKey)){Workspace w=new Workspace(store,Clock.systemUTC());owner=(String)w.register(SelfServiceTests.body("username","restore_sample_owner","display_name","Synthetic restore owner","password","Synthetic-"+Workspace.id())).get("id");w.createOperator("restore_sample_preparer","Synthetic restore preparer","Synthetic-"+Workspace.id(),"PREPARER");id=(String)w.createCase(owner,SelfServiceTests.body("title","Synthetic restore case","request_id",Workspace.id())).get("id");w.gstCommand(owner,id,SelfServiceTests.importBody(w,owner,id,SelfServiceTests.books(),SelfServiceTests.statement()));
            w.grant(owner,id,SelfServiceTests.body("request_id",Workspace.id(),"expected_revision",w.readCase(owner,id).get("revision"),"preparer_username","restore_sample_preparer","expires_days",7L,"consent",true));grantId=(String)w.grants(owner,id).get(0).get("id");
            check("actual launcher creates encrypted logical backup from real MySQL",()->{String result=cli(launcher,sourceConfig,sourceKey,driver,true,"--backup-to",archive.toString(),"--backup-key",backupKey.toString());eq(result.contains("ENCRYPTED_BACKUP_CREATED"),true);eq(new String(Files.readAllBytes(archive),java.nio.charset.StandardCharsets.ISO_8859_1).contains("SUPPLIER_A"),false);});
            snapshot=RecoveryArchive.validate(RecoveryArchive.load(archive,backupKey));
            check("logical backup is equal to a real consistent SQL business snapshot",()->eq(snapshot,store.read(state->Json.object(Json.bytes(state),25_165_824))));
            check("verification through launcher returns counts without private input data",()->{String text=cli(launcher,sourceConfig,sourceKey,driver,true,"--verify-backup",archive.toString(),"--backup-key",backupKey.toString());eq(text.contains("BACKUP_AUTHENTICATED"),true);for(String secret:List.of("SUPPLIER_A","INV/01","1000.00","restore_sample_owner"))eq(text.contains(secret),false);});
            w.revoke(owner,id,SelfServiceTests.body("request_id",Workspace.id(),"expected_revision",w.readCase(owner,id).get("revision"),"grant_id",grantId));w.deleteCase(owner,id,SelfServiceTests.body("expected_revision",w.readCase(owner,id).get("revision"),"confirm","DELETE"));
            check("source stays deleted and revoked after snapshot",()->{try{w.readCase(owner,id);throw new AssertionError("deleted case visible");}catch(Workspace.Problem e){eq(e.status,404);eq(e.code,"CASE_NOT_FOUND");}eq(store.read(state->Workspace.table(state,"grants").get(grantId)),null);});
        }
        Map<String,Object> expected=snapshot;
        check("actual launcher restores to a fresh MySQL database in isolation",()->{String text=cli(launcher,targetConfig,targetKey,driver,true,"--restore-backup",archive.toString(),"--backup-key",backupKey.toString());eq(text.contains("RESTORED_ISOLATED"),true);eq(text.contains("\"application_start_allowed\":false"),true);});
        check("SQL restored accounts cases grants quotes receipts audit and GST agree with snapshot",()->eq(MysqlStore.inspectIsolated(target,targetKey),expected));
        check("normal server and operator initializer refuse the restored database",()->{cli(launcher,targetConfig,targetKey,driver,false,"--port","0");cli(launcher,targetConfig,targetKey,driver,false,"--init-mysql");});
        check("existing active or isolated database can never be overwritten by restore",()->{cli(launcher,sourceConfig,sourceKey,driver,false,"--restore-backup",archive.toString(),"--backup-key",backupKey.toString());cli(launcher,targetConfig,targetKey,driver,false,"--restore-backup",archive.toString(),"--backup-key",backupKey.toString());eq(MysqlStore.inspectIsolated(target,targetKey),expected);});
        check("missing backup key does not create data key or initialize the target",()->{cli(launcher,targetConfig,targetKey,driver,false,"--verify-backup",archive.toString(),"--backup-key",folder.resolve("missing-backup-key").toString());eq(Files.exists(folder.resolve("missing-backup-key")),false);});
        Map<String,Object> result=Map.of("mysql_restore_groups",passed,"actual_mysql_restore",true,"real_launcher_backup_restore",true,"restored_business_state_equal",true,"restore_isolation_enforced",true,"deleted_or_revoked_snapshot_not_served",true,"current_deletion_revocation_reconciled",false,"production_recovery_approved",false);Files.writeString(report,Json.encode(result)+"\n");System.out.println(Json.encode(result));
    }
}
