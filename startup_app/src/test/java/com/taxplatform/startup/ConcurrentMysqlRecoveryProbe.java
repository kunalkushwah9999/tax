package com.taxplatform.startup;

import java.nio.file.Path;
import java.util.Map;

final class ConcurrentMysqlRecoveryProbe {
    private ConcurrentMysqlRecoveryProbe(){ }
    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("SQL configuration and key paths required");
        try(ConcurrentMysqlStore store=new ConcurrentMysqlStore(SqlConfig.load(Path.of(args[0])),Path.of(args[1]))){Object marker=store.read(state->Workspace.table(state,"receipts").get("crash-probe"));if(!Map.of("marker","COMMITTED_BEFORE_MYSQL_PROCESS_CRASH").equals(marker))throw new AssertionError("committed marker missing after real mysqld crash");System.out.println("MYSQL_COMMITTED_MARKER_RECOVERED");}
    }
}
