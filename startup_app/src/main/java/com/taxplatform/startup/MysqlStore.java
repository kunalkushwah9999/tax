package com.taxplatform.startup;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/** Transactional pilot adapter: encrypted records, lazy reads and a database-wide write guard. */
public final class MysqlStore implements DataStore {
    private static final List<String> NAMES=List.of("users","cases","grants","tickets","prices","quotes","receipts","operations","audit");
    private static final byte[] PROBE="tax-platform/mysql-key/v1".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    @FunctionalInterface interface CommitHook {void acknowledged(Connection connection)throws SQLException;}
    private static final class DatabaseFailure extends RuntimeException {
        private static final long serialVersionUID=1L;
        DatabaseFailure(Exception cause){super("database operation unavailable",cause);}
    }
    private record Original(long version,String json,int bytes){ }
    private final SqlConfig config;private final RecordCrypto crypto;private final CommitHook hook;private final ReentrantReadWriteLock lifecycle=new ReentrantReadWriteLock();private boolean closed;private final boolean isolated;
    public MysqlStore(SqlConfig config,Path key)throws IOException,GeneralSecurityException,SQLException{this(config,key,c->{ });}
    MysqlStore(SqlConfig config,Path key,CommitHook hook)throws IOException,GeneralSecurityException,SQLException{this(config,key,hook,false);}
    private MysqlStore(SqlConfig config,Path key,CommitHook hook,boolean isolated)throws IOException,GeneralSecurityException,SQLException{
        this.isolated=isolated;this.config=config;this.crypto=new RecordCrypto(key,false);this.hook=hook;
        try(Connection connection=config.connect()){supported(connection);operationsSchema(connection);verifyMeta(connection,crypto,false);recoveryState(connection,isolated);}catch(SQLException|GeneralSecurityException failure){crypto.close();throw failure;}
    }
    private static void supported(Connection c)throws SQLException{if(!c.getMetaData().getDatabaseProductName().equals("MySQL")||!c.getMetaData().getDatabaseProductVersion().startsWith("8.4."))throw new SQLException("This profile requires MySQL 8.4 LTS");}
    private static void operationsSchema(Connection c)throws SQLException{try(Statement query=c.createStatement();ResultSet result=query.executeQuery("SHOW CREATE TABLE workspace_records")){if(!result.next()||!result.getString(2).contains("'operations'"))throw new SQLException("explicit startup operations migration required");}}
    public static void initialize(SqlConfig config,Path key)throws IOException,GeneralSecurityException,SQLException{initialize(config,key,false);}
    private static void initialize(SqlConfig config,Path key,boolean isolated)throws IOException,GeneralSecurityException,SQLException{
        try(Connection c=config.connect()){
            supported(c);
            if(isolated)try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()")){r.next();if(r.getLong(1)!=0)throw new SQLException("restore requires a completely empty target database");}
            for(String resource:List.of("V001__encrypted_records.sql","V002__recovery_isolation.sql","V003__startup_operations.sql")){String script;try(var input=MysqlStore.class.getResourceAsStream("/db/"+resource)){if(input==null)throw new IOException("migration unavailable");script=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}for(String sql:script.split(";"))if(!sql.isBlank())try(Statement statement=c.createStatement()){statement.execute(sql);}}
            boolean exists;try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM workspace_meta")){r.next();exists=r.getLong(1)>0;}
            if(!exists)try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM workspace_records")){r.next();if(r.getLong(1)!=0)throw new SQLException("existing records require their original key metadata");}
            try(RecordCrypto crypto=new RecordCrypto(key,!exists)){
                c.setAutoCommit(false);try{
                    if(exists)verifyMeta(c,crypto,true);
                    else{RecordCrypto.Encrypted encrypted=crypto.encrypt("meta","key",1,PROBE);try(PreparedStatement s=c.prepareStatement("INSERT INTO workspace_meta(id,schema_version,key_nonce,key_probe,payload_bytes) VALUES(1,1,?,?,0)")){s.setBytes(1,encrypted.nonce());s.setBytes(2,encrypted.ciphertext());s.executeUpdate();}}
                    try(PreparedStatement statement=c.prepareStatement("INSERT IGNORE INTO workspace_recovery(id,state) VALUES(1,?)")){statement.setString(1,isolated?"ISOLATED":"ACTIVE");statement.executeUpdate();}
                    recoveryState(c,isolated);c.commit();
                }catch(SQLException|GeneralSecurityException failure){rollback(c);throw failure;}
            }
        }
    }
    private static void recoveryState(Connection c,boolean isolated)throws SQLException{try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT state FROM workspace_recovery WHERE id=1")){if(!r.next()||!r.getString(1).equals(isolated?"ISOLATED":"ACTIVE"))throw new SQLException("restore isolation or schema upgrade prevents application startup");}}
    public static Map<String,Object> restoreIsolated(SqlConfig config,Path key,Map<String,Object> payload)throws IOException,GeneralSecurityException,SQLException{
        Map<String,Object> source=RecoveryArchive.validate(payload);if(java.nio.file.Files.exists(key)||java.nio.file.Files.isSymbolicLink(key))throw new IOException("restore requires a fresh target key path");initialize(config,key,true);
        try(MysqlStore store=new MysqlStore(config,key,c->{ },true)){
            store.transaction(state->{for(String name:RecoveryArchive.TABLES){Map<String,Object> target=Workspace.table(state,name);if(!target.isEmpty())throw new Workspace.Problem(409,"RESTORE_TARGET_NOT_EMPTY");target.putAll(Json.map(source.get(name)));}Workspace.auditRows(state).addAll(Json.list(source.get("audit")));return null;});
        }
        return Map.of("status","RESTORED_ISOLATED","application_start_allowed",false,"deletion_revocation_reconciled",false,"production_recovery_approved",false);
    }
    static Map<String,Object> inspectIsolated(SqlConfig config,Path key)throws IOException,GeneralSecurityException,SQLException{try(MysqlStore store=new MysqlStore(config,key,c->{ },true)){return store.read(state->Json.object(Json.bytes(state),25_165_824));}}
    private static long verifyMeta(Connection c,RecordCrypto crypto,boolean write)throws SQLException,GeneralSecurityException{
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT schema_version,key_nonce,key_probe,payload_bytes FROM workspace_meta WHERE id=1"+(write?" FOR UPDATE":""))){if(!r.next()||r.getInt(1)!=1)throw new SQLException("schema initialization required");byte[] probe=crypto.decrypt("meta","key",1,r.getBytes(2),r.getBytes(3));if(!java.security.MessageDigest.isEqual(probe,PROBE))throw new GeneralSecurityException("SQL key binding invalid");return r.getLong(4);}
    }
    @Override public String backend(){return "MYSQL_ENCRYPTED_RECORDS_PILOT";}
    @Override public <T>T read(Function<Map<String,Object>,T> work){return unit(false,work);}
    @Override public <T>T transaction(Function<Map<String,Object>,T> work){return unit(true,work);}
    private <T>T unit(boolean write,Function<Map<String,Object>,T> work){
        lifecycle.readLock().lock();try{
            if(closed)throw new Workspace.Problem(503,"STORAGE_CLOSED");
            boolean commitStarted=false;try(Connection c=config.connect()){
                c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setReadOnly(!write);c.setAutoCommit(false);
                try{
                    try(Statement s=c.createStatement()){s.execute("SET SESSION innodb_lock_wait_timeout=3");}
                    // The write guard must precede any consistent SELECT that establishes the MVCC snapshot.
                    long currentBytes=verifyMeta(c,crypto,write);recoveryState(c,isolated);SqlState state=new SqlState(c,write);T result=work.apply(state.root);
                    if(write){long total=currentBytes+state.flush();if(total<0||total>16_777_216)throw new Workspace.Problem(409,"MYSQL_PILOT_CAPACITY");try(PreparedStatement s=c.prepareStatement("UPDATE workspace_meta SET payload_bytes=? WHERE id=1")){s.setLong(1,total);if(s.executeUpdate()!=1)throw new SQLException("metadata unavailable");}}
                    commitStarted=true;c.commit();if(write)hook.acknowledged(c);return result;
                }catch(Workspace.Problem failure){rollback(c);throw failure;}
                catch(SQLException|GeneralSecurityException|DatabaseFailure failure){rollback(c);throw new Workspace.Problem(503,write&&commitStarted?"STORAGE_OUTCOME_UNKNOWN":"MYSQL_STORAGE_UNAVAILABLE");}
                catch(RuntimeException failure){rollback(c);throw failure;}
            }catch(SQLException failure){throw new Workspace.Problem(503,write&&commitStarted?"STORAGE_OUTCOME_UNKNOWN":"MYSQL_STORAGE_UNAVAILABLE");}
        }finally{lifecycle.readLock().unlock();}
    }
    private static void rollback(Connection c){try{c.rollback();}catch(SQLException ignored){/* A lost commit acknowledgement is never treated as an authoritative rollback. */}}
    @Override public void close(){lifecycle.writeLock().lock();try{closed=true;crypto.close();}finally{lifecycle.writeLock().unlock();}}
    private final class SqlState {
        final Map<String,Object> root=new LinkedHashMap<>();final Map<String,SqlTable> tables=new LinkedHashMap<>();List<Object> events;
        SqlState(Connection c,boolean write){root.put("schema",1L);for(String name:NAMES){SqlTable table=new SqlTable(c,name,write);tables.put(name,table);if(!name.equals("audit"))root.put(name,table);}root.put("audit",new java.util.AbstractList<Object>(){
            private List<Object> values(){if(events==null)events=new ArrayList<>(tables.get("audit").values());return events;}
            @Override public Object get(int index){return values().get(index);}@Override public int size(){return values().size();}
            @Override public boolean add(Object value){if(!write)throw new IllegalStateException("read-only audit");return values().add(value);}
            @Override public Object remove(int index){if(!write)throw new IllegalStateException("read-only audit");return values().remove(index);}
        });}
        long flush()throws SQLException,GeneralSecurityException{
            if(events!=null){SqlTable audit=tables.get("audit");Map<String,Object> desired=new LinkedHashMap<>();for(Object event:events)desired.put((String)Json.map(event).get("id"),event);audit.values.clear();audit.values.putAll(desired);}
            long bytes=0;for(SqlTable table:tables.values())bytes+=table.flush();return bytes;
        }
    }
    private final class SqlTable extends AbstractMap<String,Object> {
        final Connection connection;final String namespace;final boolean write;final Map<String,Object> values=new LinkedHashMap<>();final Map<String,Original> original=new LinkedHashMap<>();final Set<String> missing=new LinkedHashSet<>();boolean complete;
        SqlTable(Connection connection,String namespace,boolean write){this.connection=connection;this.namespace=namespace;this.write=write;}
        private void accept(ResultSet r)throws SQLException,GeneralSecurityException{String id=r.getString("record_id");if(values.containsKey(id)||original.containsKey(id))return;long revision=r.getLong("db_revision");byte[] plaintext=crypto.decrypt(namespace,id,revision,r.getBytes("nonce"),r.getBytes("ciphertext"));Map<String,Object> value=Json.object(plaintext);original.put(id,new Original(revision,Json.encode(value),r.getInt("nbytes")));values.put(id,value);java.util.Arrays.fill(plaintext,(byte)0);}
        @Override public Object get(Object raw){if(!(raw instanceof String id)||id.length()>180)return null;if(values.containsKey(id)||complete||missing.contains(id)||original.containsKey(id))return values.get(id);try(PreparedStatement s=connection.prepareStatement("SELECT record_id,db_revision,nonce,ciphertext,OCTET_LENGTH(ciphertext) nbytes FROM workspace_records WHERE namespace=? AND record_id=?")){s.setString(1,namespace);s.setString(2,id);try(ResultSet r=s.executeQuery()){if(r.next())accept(r);else missing.add(id);}return values.get(id);}catch(SQLException|GeneralSecurityException failure){throw new DatabaseFailure(failure);}}
        private void all(){if(complete)return;try(PreparedStatement s=connection.prepareStatement("SELECT record_id,db_revision,nonce,ciphertext,OCTET_LENGTH(ciphertext) nbytes FROM workspace_records WHERE namespace=? ORDER BY ordinal")){s.setString(1,namespace);try(ResultSet r=s.executeQuery()){while(r.next())accept(r);}complete=true;}catch(SQLException|GeneralSecurityException failure){throw new DatabaseFailure(failure);}}
        @Override public Set<Entry<String,Object>> entrySet(){all();return values.entrySet();}
        @Override public Object put(String id,Object value){if(!write)throw new IllegalStateException("read-only table");if(id==null||id.length()>180||!id.matches("[A-Za-z0-9._:/-]+")||!(value instanceof Map<?,?>))throw new IllegalArgumentException("invalid record");Object old=get(id);values.put(id,value);missing.remove(id);return old;}
        @Override public Object remove(Object id){if(!write)throw new IllegalStateException("read-only table");Object old=get(id);values.remove(id);return old;}
        long flush()throws SQLException,GeneralSecurityException{
            long delta=0;Set<String> ids=new LinkedHashSet<>(original.keySet());ids.addAll(values.keySet());for(String id:ids){Original old=original.get(id);Object value=values.get(id);if(value==null){if(old!=null)try(PreparedStatement s=connection.prepareStatement("DELETE FROM workspace_records WHERE namespace=? AND record_id=? AND db_revision=?")){s.setString(1,namespace);s.setString(2,id);s.setLong(3,old.version());if(s.executeUpdate()!=1)throw new SQLException("record deletion conflict");delta-=old.bytes();}continue;}
                String json=Json.encode(value);if(old!=null&&Json.map(value).equals(Json.object(old.json().getBytes(java.nio.charset.StandardCharsets.UTF_8))))continue;byte[] plaintext=json.getBytes(java.nio.charset.StandardCharsets.UTF_8);if(plaintext.length>262144)throw new Workspace.Problem(409,"MYSQL_RECORD_LIMIT");long revision=old==null?1:old.version()+1;RecordCrypto.Encrypted encrypted=crypto.encrypt(namespace,id,revision,plaintext);java.util.Arrays.fill(plaintext,(byte)0);byte[] tag=namespace.equals("users")?crypto.usernameTag((String)Json.map(value).get("username")):null;
                if(old==null)try(PreparedStatement s=connection.prepareStatement("INSERT INTO workspace_records(namespace,record_id,db_revision,lookup_tag,nonce,ciphertext) VALUES(?,?,?,?,?,?)")){s.setString(1,namespace);s.setString(2,id);s.setLong(3,revision);s.setBytes(4,tag);s.setBytes(5,encrypted.nonce());s.setBytes(6,encrypted.ciphertext());s.executeUpdate();}
                else try(PreparedStatement s=connection.prepareStatement("UPDATE workspace_records SET db_revision=?,lookup_tag=?,nonce=?,ciphertext=? WHERE namespace=? AND record_id=? AND db_revision=?")){s.setLong(1,revision);s.setBytes(2,tag);s.setBytes(3,encrypted.nonce());s.setBytes(4,encrypted.ciphertext());s.setString(5,namespace);s.setString(6,id);s.setLong(7,old.version());if(s.executeUpdate()!=1)throw new SQLException("record update conflict");}
                delta+=encrypted.ciphertext().length-(old==null?0:old.bytes());
            }return delta;
        }
    }
}
