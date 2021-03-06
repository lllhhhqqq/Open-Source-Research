/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
* Copyright (c) 2004, Open Cloud Limited.
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/core/v3/SimpleQuery.java,v 1.14 2009/04/20 21:44:09 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.core.v3;

import org.postgresql.core.*;
import java.lang.ref.PhantomReference;

/**
 * V3 Query implementation for a single-statement query.
 * This also holds the state of any associated server-side
 * named statement. We use a PhantomReference managed by
 * the QueryExecutor to handle statement cleanup.
 * 
 * @author Oliver Jowett (oliver@opencloud.com)
 */
class SimpleQuery implements V3Query {
	private static my.Debug DEBUG=new my.Debug(my.Debug.QueryExecutor);//我加上的

    SimpleQuery(String[] fragments, ProtocolConnectionImpl protoConnection)
    {
        try {//我加上的
		DEBUG.P(this,"SimpleQuery(2)");
		DEBUG.PA("fragments",fragments);

        this.fragments = fragments; //按?号截取后的sql语句片段(通常比?号的个数多1)
        this.protoConnection = protoConnection;

		}finally{//我加上的
		DEBUG.P(0,this,"SimpleQuery(2)");
		}
    }

    public ParameterList createParameterList() {
        if (fragments.length == 1)
            return NO_PARAMETERS;

		//?号个数总是等于fragments的长度减1
        return new SimpleParameterList(fragments.length - 1, protoConnection);
    }

    public String toString(ParameterList parameters) {
        StringBuffer sbuf = new StringBuffer(fragments[0]);
        for (int i = 1; i < fragments.length; ++i)
        {
            if (parameters == null)
                sbuf.append('?');
            else
                sbuf.append(parameters.toString(i));
            sbuf.append(fragments[i]);
        }
        return sbuf.toString();
    }

    public String toString() {
        return toString(null);
    }

    public void close() {
        unprepare();
    }

    //
    // V3Query
    //

    public SimpleQuery[] getSubqueries() {
        return null;
    }

    //
    // Implementation guts
    //

    String[] getFragments() {
        return fragments;
    }

  
    
    void setStatementName(String statementName) {
        this.statementName = statementName;
        this.encodedStatementName = Utils.encodeUTF8(statementName);
    }

    void setStatementTypes(int[] paramTypes) {
        this.preparedTypes = paramTypes;
    }

    int[] getStatementTypes() {
        return preparedTypes;
    }

    String getStatementName() {
        return statementName;
    }

    boolean isPreparedFor(int[] paramTypes) {
		try {//我加上的
		DEBUG.P(this,"isPreparedFor(1)");
		DEBUG.PA("paramTypes",paramTypes);

		DEBUG.P("statementName="+statementName);

        if (statementName == null)
            return false; // Not prepared.

        // Check for compatible types.
        for (int i = 0; i < paramTypes.length; ++i)
            if (paramTypes[i] != Oid.UNSPECIFIED && paramTypes[i] != preparedTypes[i])
                return false;

        return true;

		}finally{//我加上的
		DEBUG.P(0,this,"isPreparedFor(1)");
		}
    }

    boolean hasUnresolvedTypes() {
        if (preparedTypes == null)
            return true;

        for (int i=0; i<preparedTypes.length; i++) {
            if (preparedTypes[i] == Oid.UNSPECIFIED)
                return true;
        }

        return false;
    }

    byte[] getEncodedStatementName() {
        return encodedStatementName;
    }

    void setFields(Field[] fields) {
        this.fields = fields;
    }
    Field[] getFields() {
        return fields;
    }

    // Have we sent a Describe Portal message for this query yet?
    boolean isPortalDescribed() {
        return portalDescribed;
    }
    void setPortalDescribed(boolean portalDescribed) {
        this.portalDescribed = portalDescribed;
    }

    // Have we sent a Describe Statement message for this query yet?
    // Note that we might not have need to, so this may always be false.
    boolean isStatementDescribed() {
        return statementDescribed;
    }
    void setStatementDescribed(boolean statementDescribed) {
        this.statementDescribed = statementDescribed;
    }

    void setCleanupRef(PhantomReference cleanupRef) {
        if (this.cleanupRef != null) {
            this.cleanupRef.clear();
            this.cleanupRef.enqueue();
        }
        this.cleanupRef = cleanupRef;
    }

    void unprepare() {
        if (cleanupRef != null)
        {
            cleanupRef.clear();
            cleanupRef.enqueue();
            cleanupRef = null;
        }

        statementName = null;
        encodedStatementName = null;
        fields = null;
        portalDescribed = false;
        statementDescribed = false;
    }

    private final String[] fragments;
    private final ProtocolConnectionImpl protoConnection;
    private String statementName;
    private byte[] encodedStatementName;
    private Field[] fields;
    private boolean portalDescribed;
    private boolean statementDescribed;
    private PhantomReference cleanupRef;
    private int[] preparedTypes;

    final static SimpleParameterList NO_PARAMETERS = new SimpleParameterList(0, null);
}


