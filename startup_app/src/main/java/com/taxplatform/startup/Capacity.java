package com.taxplatform.startup;

import java.util.Map;

/** A capacity predicate, not a dependency on an exact global count. */
final class Capacity {
    private Capacity(){ }
    interface Checked {void requireSpace(long maximum,String code);}
    static void requireSpace(Map<String,Object> rows,long maximum,String code){if(rows instanceof Checked checked)checked.requireSpace(maximum,code);else if(rows.size()>=maximum)throw new Workspace.Problem(409,code);}
}
