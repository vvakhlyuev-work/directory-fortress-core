/*
 * Copyright (c) 2009-2014, JoshuaTree. All Rights Reserved.
 */
/*
 * Copyright (c) 2009-2014, JoshuaTree. All Rights Reserved.
 */

package us.jts.fortress.rbac.dao.unboundid;


import java.util.ArrayList;
import java.util.List;

import us.jts.fortress.FinderException;
import us.jts.fortress.GlobalErrIds;
import us.jts.fortress.GlobalIds;
import us.jts.fortress.ObjectFactory;
import us.jts.fortress.cfg.Config;
import us.jts.fortress.ldap.UnboundIdDataProvider;
import us.jts.fortress.rbac.AuthZ;
import us.jts.fortress.rbac.Bind;
import us.jts.fortress.rbac.Mod;
import us.jts.fortress.rbac.UserAudit;
import us.jts.fortress.util.attr.AttrHelper;
import us.jts.fortress.util.attr.VUtil;

import com.unboundid.ldap.sdk.migrate.ldapjdk.LDAPConnection;
import com.unboundid.ldap.sdk.migrate.ldapjdk.LDAPEntry;
import com.unboundid.ldap.sdk.migrate.ldapjdk.LDAPException;
import com.unboundid.ldap.sdk.migrate.ldapjdk.LDAPSearchResults;


/**
 * This class performs data access for OpenLDAP synch repl log data
 * <p/>
 * <h3>1. Binds</h3>
 * <p/>
 * The auditBind Structural object class is used to store authentication events that can later be queried via ldap API.<br />
 * <code># The Bind class includes the reqVersion attribute which contains the LDAP</code>
 * <code># protocol version specified in the Bind as well as the reqMethod attribute</code>
 * <code># which contains the Bind Method used in the Bind. This will be the string</code>
 * <code># SIMPLE for LDAP Simple Binds or SASL(mech) for SASL Binds. Note that unless</code>
 * <code># configured as a global overlay, only Simple Binds using DNs that reside in</code>
 * <code># the current database will be logged:</code>
 * <ul>
 * <li>  ------------------------------------------
 * <li> <code>objectclass (  1.3.6.1.4.1.4203.666.11.5.2.6 NAME 'auditBind'</code>
 * <li> <code>DESC 'Bind operation'</code>
 * <li> <code>SUP auditObject STRUCTURAL</code>
 * <li> <code>MUST ( reqVersion $ reqMethod ) )</code>
 * <li> ------------------------------------------
 * </ul>
 * <h3>2. Authorizations</h3>
 * <code>For  the  Search class the reqScope attribute contains the scope of the</code><br />
 * <code>original search request, using the values specified for  the  LDAP  URL</code><br />
 * <code>format. I.e.  base, one, sub, or subord.  The reqDerefAliases attribute</code><br />
 * <code>is one of never, finding, searching, or always,  denoting  how  aliases</code><br />
 * <code>will  be  processed during the search.  The reqAttrsOnly attribute is a</code><br />
 * <code>Boolean value showing TRUE if only attribute names were  requested,  or</code><br />
 * <code>FALSE  if  attributes  and  their values were requested.  The reqFilter</code><br />
 * <code>attribute carries the filter used in the search request.   The  reqAttr</code><br />
 * <code>attribute  lists  the  requested attributes if specific attributes were</code><br />
 * <code>requested.  The reqEntries attribute is the integer count of  how  many</code><br />
 * <code>entries  were  returned  by  this search request.  The reqSizeLimit and</code><br />
 * <code>reqTimeLimit attributes indicate what  limits  were  requested  on  the</code><br />
 * <code>search operation.</code><br />
 * <ul>
 * <li>  ------------------------------------------
 * <li> <code>objectclass  (  1.3.6.1.4.1.4203.666.11.5.2.11</code>
 * <li> <code>NAME 'auditSearch'</code>
 * <li> <code>DESC 'Search operation'</code>
 * <li> <code>SUP auditReadObject STRUCTURAL</code>
 * <li> <code>MUST ( reqScope $ reqDerefAliases $ reqAttrsOnly )</code>
 * <li> <code>MAY ( reqFilter $ reqAttr $ reqEntries $ reqSizeLimit $</code>
 * <li> <code>reqTimeLimit ) )</code>
 * <li> ------------------------------------------
 * </ul>
 * <p/>
 * <p/>
 * <h3>3. Modifications</h3>
 * The auditModify Structural object class is used to store Fortress update and delete events that can later be queried via ldap API.<br />
 * The deletions can be recorded in this manner and associated with Fortress context because deletions will perform a modification first
 * if audit is enabled.
 * <p/>
 * <code>The Modify operation contains a description  of  modifications  in  the</code><br />
 * <code>reqMod  attribute,  which  was  already  described  above  in  the  Add</code><br />
 * <code>operation. It may optionally  contain  the  previous  contents  of  any</code><br />
 * <code>modified  attributes  in the reqOld attribute, using the same format as</code><br />
 * <code>described above for the Delete operation.  The reqOld attribute is only</code><br />
 * <code>populated  if  the  entry  being modified matches the configured logold</code><br />
 * <code>filter.</code><br />
 * <ul>
 * <li>  ------------------------------------------
 * <li> <code>objectclass (  1.3.6.1.4.1.4203.666.11.5.2.9</code>
 * <li> <code>NAME 'auditModify'</code>
 * <li> <code>DESC 'Modify operation'</code>
 * <li> <code>SUP auditWriteObject STRUCTURAL</code>
 * <li> <code>MAY reqOld MUST reqMod )</code>
 * <li> ------------------------------------------
 * </ul>
 * <p/>
 * Note this class used descriptions pulled from man pages on slapd access log.
 * <p/>
 * This class is thread safe.
 *
 * @author Shawn McKinney
 */
public final class AuditDAO extends UnboundIdDataProvider implements us.jts.fortress.rbac.dao.AuditDAO
{
    private static final String CLS_NM = AuditDAO.class.getName();
    private static final String CREATETIMESTAMP = "createTimestamp";
    private static final String CREATORSNAME = "creatorsName";
    private static final String ENTRYCSN = "entryCSN";
    private static final String ENTRYDN = "entryDN";
    private static final String ENTRYUUID = "entryUUID";
    private static final String HASSUBORDINATES = "hasSubordinates";
    private static final String MODIFIERSNAME = "modifiersName";
    private static final String MODIFYTIMESTAMP = "modifyTimestamp";
    private static final String OBJECTCLASS = "objectClass";
    private static final String REQUAUTHZID = "reqAuthzID";
    private static final String REQCONTROLS = "reqControls";
    private static final String REQDN = "reqDN";
    private static final String REQEND = "reqEnd";
    private static final String REQMETHOD = "reqMethod";
    private static final String REQRESULT = "reqResult";
    private static final String REQSESSION = "reqSession";
    private static final String REQSTART = "reqStart";
    private static final String REQTYPE = "reqType";
    private static final String REQVERSION = "reqVersion";
    private static final String REQMOD = "reqMod";
    private static final String STRUCTURALOBJECTCLASS = "structuralObjectClass";
    private static final String SUBSCHEMAENTRY = "subschemaSubentry";
    private static final String REQATTR = "reqAttr";
    private static final String REQATTRSONLY = "reqAttrsOnly";
    private static final String REQDREFALIASES = "reqDerefAliases";
    private static final String REQENTRIES = "reqEntries";
    private static final String REQFILTER = "reqFilter";
    private static final String REQSCOPE = "reqScope";
    private static final String REQSIZELIMIT = "reqSizeLimit";
    private static final String REQTIMELIMIT = "reqTimeLimit";
    private static final String REQASSERTION = "reqAssertion";
    private static final String ACCESS_BIND_CLASS_NM = "auditBind";
    //private static final String ACCESS_AUTHZ_CLASS_NM = "auditSearch";
    private static final String ACCESS_AUTHZ_CLASS_NM = "auditCompare";
    private static final String ACCESS_MOD_CLASS_NM = "auditModify";
    private static final String ACCESS_ADD_CLASS_NM = "auditAdd";
    private static final String AUDIT_ROOT = "audit.root";

    private static final String[] AUDIT_AUTHZ_ATRS =
        {
            CREATETIMESTAMP, CREATORSNAME, ENTRYCSN, ENTRYDN, ENTRYUUID, HASSUBORDINATES, MODIFIERSNAME,
            MODIFYTIMESTAMP, OBJECTCLASS, REQATTR, REQATTRSONLY, REQUAUTHZID, REQCONTROLS, REQDN, REQDREFALIASES,
            REQEND, REQENTRIES, REQFILTER, REQRESULT, REQSCOPE, REQSESSION, REQSIZELIMIT, REQSTART, REQTIMELIMIT,
            REQTYPE, STRUCTURALOBJECTCLASS, SUBSCHEMAENTRY
    };

    private static final String[] AUDIT_BIND_ATRS =
        {
            CREATETIMESTAMP, CREATORSNAME, ENTRYCSN, ENTRYDN, ENTRYUUID, HASSUBORDINATES, MODIFIERSNAME,
            MODIFYTIMESTAMP, OBJECTCLASS, REQUAUTHZID, REQCONTROLS, REQDN, REQEND, REQMETHOD, REQRESULT,
            REQSESSION, REQSTART, REQTYPE, REQVERSION, STRUCTURALOBJECTCLASS, SUBSCHEMAENTRY
    };

    private static final String[] AUDIT_MOD_ATRS =
        {
            OBJECTCLASS, REQUAUTHZID, REQDN, REQEND, REQRESULT, REQSESSION, REQSTART, REQTYPE, REQMOD
    };


    /**
     * Package private default constructor.
     */
    public AuditDAO()
    {
    }


    /**
     * This method returns failed authentications where the userid is not present in the directory.  This
     * is possible because Fortress performs read on user before the bind.
     * User:
     * dn: reqStart=20101014235402.000000Z, cn=log
     * reqStart: 20101014235402.000000Z
     * reqEnd: 20101014235402.000001Z
     * reqAuthzID: cn=Manager,dc=jts,dc=com
     * reqDerefAliases: never
     * reqSession: 84
     * reqAttrsOnly: FALSE
     * reqSizeLimit: -1
     * objectClass: auditSearch
     * reqResult: 32
     * reqAttr: ftId
     * reqAttr: uid
     * reqAttr: userpassword
     * reqAttr: description
     * reqAttr: ou
     * reqAttr: cn
     * reqAttr: sn
     * reqAttr: ftRoleCstr
     * reqAttr: ftCstr
     * reqAttr: ftRoleAsgn
     * reqAttr: pwdReset
     * reqAttr: pwdAccountLockedTime
     * reqAttr: ftProps
     * reqEntries: 0
     * reqFilter: (|(objectClass=*)(?objectClass=ldapSubentry))
     * reqType: search
     * reqDN: uid=foo,ou=People,dc=jts,dc=com        /cal/cal2.jsp
     * reqTimeLimit: -1
     * reqScope: base
     *
     * @param audit
     * @return
     * @throws us.jts.fortress.FinderException
     *
     */
    public final List<AuthZ> searchInvalidAuthNs( UserAudit audit )
        throws FinderException
    {
        List<AuthZ> auditList = new ArrayList<>();
        LDAPConnection ld = null;
        LDAPSearchResults searchResults;
        String auditRoot = Config.getProperty( AUDIT_ROOT );
        String userRoot = Config.getProperty( GlobalIds.USER_ROOT );
        try
        {
            // use wildcard for user if not passed in:
            //reqDN: uid=foo,ou=People,dc=jts,dc=com
            //(&
            //  (objectclass=auditSearch)
            //      (reqDN=uid=*,ou=People,dc=jts,dc=com)
            //      (reqAuthzID=cn=Manager,dc=jts,dc=com)
            //      (reqEntries=0)
            // )

            String filter = GlobalIds.FILTER_PREFIX + ACCESS_AUTHZ_CLASS_NM + ")(";
            String userId;
            if ( VUtil.isNotNullOrEmpty( audit.getUserId() ) )
            {
                userId = audit.getUserId();
                filter += REQDN + "=" + GlobalIds.UID + "=" + userId + "," + userRoot + ")(" +
                    REQUAUTHZID + "=" + "cn=Manager," + Config.getProperty( GlobalIds.SUFFIX ) + ")";
            }
            else
            {
                // pull back all failed authN attempts for all users:
                filter += REQATTR + "=" + GlobalIds.UID + ")(" +
                    REQUAUTHZID + "=" + "cn=Manager," + Config.getProperty( GlobalIds.SUFFIX ) + ")";
            }

            if ( audit.isFailedOnly() )
            {
                filter += "(" + REQENTRIES + "=" + 0 + ")";
            }
            if ( audit.getBeginDate() != null )
            {
                String szTime = AttrHelper.encodeGeneralizedTime( audit.getBeginDate() );
                filter += "(" + REQEND + ">=" + szTime + ")";
            }
            filter += ")";

            //log.warn("filter=" + filter);
            ld = getLogConnection();
            searchResults = search( ld, auditRoot,
                LDAPConnection.SCOPE_ONE, filter, AUDIT_AUTHZ_ATRS, false, GlobalIds.BATCH_SIZE );
            long sequence = 0;
            while ( searchResults.hasMoreElements() )
            {
                AuthZ authZ = getAuthzEntityFromLdapEntry( searchResults.next(), sequence++ );
                // todo: fix this workaround. This search will return failed role assign searches as well.  
                // Work around is to remove the ou=People failed searches from user failed searches on authN.
                if ( !AttrHelper.getAuthZId( authZ.getReqDN() ).equalsIgnoreCase( "People" ) )
                    auditList.add( authZ );
            }
        }
        catch ( LDAPException e )
        {
            String error = "LDAPException in AuditDAO.searchAuthZs id=" + e.getLDAPResultCode() + " msg="
                + e.getMessage();
            throw new FinderException( GlobalErrIds.AUDT_AUTHN_INVALID_FAILED, error, e );
        }
        finally
        {
            closeLogConnection( ld );
        }
        return auditList;
    }


    /**
     * @param audit
     * @return
     * @throws us.jts.fortress.FinderException
     *
     */
    public final List<AuthZ> searchAuthZs( UserAudit audit )
        throws FinderException
    {
        List<AuthZ> auditList = new ArrayList<>();
        LDAPConnection ld = null;
        LDAPSearchResults searchResults;
        String auditRoot = Config.getProperty( AUDIT_ROOT );
        String permRoot = getRootDn( audit.isAdmin(), audit.getContextId() );
        String userRoot = getRootDn( audit.getContextId(), GlobalIds.USER_ROOT );
        try
        {
            String reqDn = PermDAO.getOpRdn( audit.getOpName(), audit.getObjId() ) + "," + GlobalIds.POBJ_NAME + "="
                + audit.getObjName() + "," + permRoot;
            String filter = GlobalIds.FILTER_PREFIX + ACCESS_AUTHZ_CLASS_NM + ")(" + REQDN + "=" +
                reqDn + ")(" + REQUAUTHZID + "=" + GlobalIds.UID + "=" + audit.getUserId() + "," + userRoot + ")";
            if ( audit.isFailedOnly() )
            {
                filter += "(!(" + REQRESULT + "=" + 6 + "))";
            }
            if ( audit.getBeginDate() != null )
            {
                String szTime = AttrHelper.encodeGeneralizedTime( audit.getBeginDate() );
                filter += "(" + REQEND + ">=" + szTime + ")";
            }
            filter += ")";

            //System.out.println("filter=" + filter);
            ld = getLogConnection();
            searchResults = search( ld, auditRoot,
                LDAPConnection.SCOPE_ONE, filter, AUDIT_AUTHZ_ATRS, false, GlobalIds.BATCH_SIZE );
            long sequence = 0;
            while ( searchResults.hasMoreElements() )
            {
                auditList.add( getAuthzEntityFromLdapEntry( searchResults.next(), sequence++ ) );
            }
        }
        catch ( LDAPException e )
        {
            String error = "LDAPException in AuditDAO.searchAuthZs id=" + e.getLDAPResultCode() + " msg="
                + e.getMessage();
            throw new FinderException( GlobalErrIds.AUDT_AUTHZ_SEARCH_FAILED, error, e );
        }
        finally
        {
            closeLogConnection( ld );
        }
        return auditList;
    }


    private String getRootDn( boolean isAdmin, String contextId )
    {
        String dn;
        if ( isAdmin )
        {
            dn = getRootDn( contextId, GlobalIds.ADMIN_PERM_ROOT );
        }
        else
        {
            dn = getRootDn( contextId, GlobalIds.PERM_ROOT );
        }
        return dn;
    }


    /**
     * @param audit
     * @return
     * @throws us.jts.fortress.FinderException
     *
     */
    public final List<AuthZ> getAllAuthZs( UserAudit audit )
        throws FinderException
    {
        List<AuthZ> auditList = new ArrayList<>();
        LDAPConnection ld = null;
        LDAPSearchResults searchResults;
        String auditRoot = Config.getProperty( AUDIT_ROOT );
        String userRoot = getRootDn( audit.getContextId(), GlobalIds.USER_ROOT );

        try
        {
            String filter = GlobalIds.FILTER_PREFIX + ACCESS_AUTHZ_CLASS_NM + ")(";
            if (VUtil.isNotNullOrEmpty( audit.getUserId() ) )
            {
                filter += REQUAUTHZID + "=" + GlobalIds.UID + "=" + audit.getUserId() + "," + userRoot + ")";
            }
            else
            {
                // have to limit the query to only authorization entries.
                // TODO: determine why the cn=Manager user is showing up in this search:
                filter += REQUAUTHZID + "=*)(!(" + REQUAUTHZID + "=cn=Manager," + Config.getProperty(GlobalIds.SUFFIX) + "))";
            }
            //if( VUtil.isNotNullOrEmpty( audit.getObjName() ) && VUtil.isNotNullOrEmpty( audit.getOpName() ) )
            if( VUtil.isNotNullOrEmpty( audit.getDn() ) )
            {
                //filter += "(" + REQDN + "=" + GlobalIds.POP_NAME + "=" + audit.getOpName() + "," + GlobalIds.POBJ_NAME + "=" + audit.getObjName() + ",*)";
                filter += "(" + REQDN + "=" + audit.getDn() + ")";
            }
            if (audit.isFailedOnly())
            {
                filter += "(!(" + REQRESULT + "=" + 6 + "))";
            }
            if ( audit.getBeginDate() != null )
            {
                String szTime = AttrHelper.encodeGeneralizedTime( audit.getBeginDate() );
                filter += "(" + REQEND + ">=" + szTime + ")";
            }
            if (audit.getEndDate() != null)
            {
                String szTime = AttrHelper.encodeGeneralizedTime(audit.getEndDate());
                filter += "(" + REQEND + "<=" + szTime + ")";
            }
            filter += ")";

            //log.warn("filter=" + filter);
            ld = getLogConnection();
            searchResults = search( ld, auditRoot,
                LDAPConnection.SCOPE_ONE, filter, AUDIT_AUTHZ_ATRS, false, GlobalIds.BATCH_SIZE );
            long sequence = 0;
            while ( searchResults.hasMoreElements() )
            {
                auditList.add( getAuthzEntityFromLdapEntry( searchResults.next(), sequence++ ) );
            }
        }
        catch ( LDAPException e )
        {
            String error = "LDAPException in AuditDAO.getAllAuthZs id=" + e.getLDAPResultCode() + " msg="
                + e.getMessage();
            throw new FinderException( GlobalErrIds.AUDT_AUTHZ_SEARCH_FAILED, error, e );
        }
        finally
        {
            closeLogConnection( ld );
        }
        return auditList;
    }


    /**
     * @param audit
     * @return
     * @throws us.jts.fortress.FinderException
     *
     */
    public final List<Bind> searchBinds( UserAudit audit )
        throws FinderException
    {
        List<Bind> auditList = new ArrayList<>();
        LDAPConnection ld = null;
        LDAPSearchResults searchResults;
        String auditRoot = Config.getProperty( AUDIT_ROOT );
        String userRoot = getRootDn( audit.getContextId(), GlobalIds.USER_ROOT );

        try
        {
            String filter;
            if ( audit.getUserId() != null && audit.getUserId().length() > 0 )
            {
                filter = GlobalIds.FILTER_PREFIX + ACCESS_BIND_CLASS_NM + ")(" +
                    REQDN + "=" + GlobalIds.UID + "=" + audit.getUserId() + "," + userRoot + ")";
                if ( audit.isFailedOnly() )
                {
                    filter += "(" + REQRESULT + ">=" + 1 + ")";
                }
                if ( audit.getBeginDate() != null )
                {
                    String szTime = AttrHelper.encodeGeneralizedTime( audit.getBeginDate() );
                    filter += "(" + REQEND + ">=" + szTime + ")";
                }
                filter += ")";
            }
            else
            {
                filter = GlobalIds.FILTER_PREFIX + ACCESS_BIND_CLASS_NM + ")";
                if ( audit.isFailedOnly() )
                {
                    filter += "(" + REQRESULT + ">=" + 1 + ")";
                }
                if ( audit.getBeginDate() != null )
                {
                    String szTime = AttrHelper.encodeGeneralizedTime( audit.getBeginDate() );
                    filter += "(" + REQEND + ">=" + szTime + ")";
                }
                filter += ")";
            }
            //log.warn("filter=" + filter);
            ld = getLogConnection();
            searchResults = search( ld, auditRoot,
                LDAPConnection.SCOPE_ONE, filter, AUDIT_BIND_ATRS, false, GlobalIds.BATCH_SIZE );
            long sequence = 0;
            while ( searchResults.hasMoreElements() )
            {
                auditList.add( getBindEntityFromLdapEntry( searchResults.next(), sequence++ ) );
            }
        }
        catch ( LDAPException e )
        {
            String error = "LDAPException in AuditDAO.searchBinds id=" + e.getLDAPResultCode() + " msg="
                + e.getMessage();
            throw new FinderException( GlobalErrIds.AUDT_BIND_SEARCH_FAILED, error, e );
        }
        finally
        {
            closeLogConnection( ld );
        }
        return auditList;
    }


    /**
     * @param audit
     * @return
     * @throws us.jts.fortress.FinderException
     *
     */
    public final List<Mod> searchUserMods( UserAudit audit )
        throws FinderException
    {
        List<Mod> modList = new ArrayList<>();
        LDAPConnection ld = null;
        LDAPSearchResults searchResults;
        String auditRoot = Config.getProperty( AUDIT_ROOT );

        String userRoot = getRootDn( audit.getContextId(), GlobalIds.USER_ROOT );
        try
        {
            String filter = GlobalIds.FILTER_PREFIX + ACCESS_MOD_CLASS_NM + ")(" +
                REQDN + "=" + GlobalIds.UID + "=" + audit.getUserId() + "," + userRoot + ")";
            if ( audit.getBeginDate() != null )
            {
                String szTime = AttrHelper.encodeGeneralizedTime( audit.getBeginDate() );
                filter += "(" + REQEND + ">=" + szTime + ")";
            }
            filter += ")";
            //log.warn("filter=" + filter);
            ld = getLogConnection();
            searchResults = search( ld, auditRoot,
                LDAPConnection.SCOPE_ONE, filter, AUDIT_MOD_ATRS, false, GlobalIds.BATCH_SIZE );
            long sequence = 0;
            while ( searchResults.hasMoreElements() )
            {
                modList.add( getModEntityFromLdapEntry( searchResults.next(), sequence++ ) );
            }
        }
        catch ( LDAPException e )
        {
            String error = "searchUserMods caught LDAPException id=" + e.getLDAPResultCode() + " msg=" + e.getMessage();
            throw new FinderException( GlobalErrIds.AUDT_MOD_SEARCH_FAILED, error, e );
        }
        finally
        {
            closeLogConnection( ld );
        }
        return modList;
    }


    /**
     * @param audit
     * @return
     * @throws FinderException
     */
    public final List<Mod> searchAdminMods( UserAudit audit )
        throws FinderException
    {
        List<Mod> modList = new ArrayList<>();
        LDAPConnection ld = null;
        LDAPSearchResults searchResults;
        String auditRoot = Config.getProperty( AUDIT_ROOT );

        try
        {
            /*
                # 20110117075053.000006Z, log
                dn: reqStart=20110117075053.000006Z,cn=log
                objectClass: auditModify
                reqStart: 20110117075053.000006Z
                reqEnd: 20110117075053.000007Z
                reqType: modify
                reqSession: 12
                reqAuthzID: cn=Manager,dc=jts,dc=com
                reqDN: ftObjId=004+ftOpNm=TOP2_4,ftObjNm=TOB2_3,ou=Permissions,ou=RBAC,dc=m
                 ims,dc=com
                reqResult: 0
                reqMod: ftRoles:- ftT2ROLE5
                reqMod: ftModifier:= -42f31b5d:12d92f18440:-7eb8
                reqMod: ftModCode:= AdminMgrImpl.revokePermission
                reqMod: ftModId:= -42f31b5d:12d92f18440:-6674
                reqMod: entryCSN:= 20110117075053.093893Z#000000#000#000000
                reqMod: modifiersName:= cn=Manager,dc=jts,dc=com
                reqMod: modifyTimestamp:= 20110117075053Z

                ldapsearch -x -D "cn=Manager,cn=log" -w secret -b 'cn=log' -s SUB -h localhost -p 389 '(&(objectclass=auditModify)(reqMod=ftModCode:= AdminMgrImpl.addDescendant)(reqMod=ftModifier:= -6a20c261:12d92e15581:-7eb8))'

                # limit search by dn works:
                ldapsearch -x -D "cn=Manager,cn=log" -w secret -b 'cn=log' -s SUB -h localhost -p 389 '(&(objectclass=auditModify)(reqDN=cn=Hierarchies,ou=Roles,ou=RBAC,dc=jts,dc=com))'

                # wild card works on reqMod:
                ldapsearch -x -D "cn=Manager,cn=log" -w secret -b 'cn=log' -s SUB -h localhost -p 389 '(&(objectclass=auditModify)(reqMod=ftModCode:= AdminMgrImpl.add*)(reqMod=ftModifier:= -6a20c261:12d92e15581:-7eb8))'

             */
            String filter = "(&(|(objectclass=" + ACCESS_MOD_CLASS_NM + ")";
            filter += "(objectclass=" + ACCESS_ADD_CLASS_NM + "))";
            if ( VUtil.isNotNullOrEmpty( audit.getDn() ) )
            {
                filter += "(" + REQDN + "=" + audit.getDn() + ")";
            }
            if ( VUtil.isNotNullOrEmpty( audit.getObjName() ) )
            {
                filter += "(|(" + REQMOD + "=" + GlobalIds.FT_MODIFIER_CODE + ":= " + audit.getObjName() + ".";
                if ( VUtil.isNotNullOrEmpty( audit.getOpName() ) )
                {
                    filter += audit.getOpName();
                }
                filter += "*)";
                filter += "(" + REQMOD + "=" + GlobalIds.FT_MODIFIER_CODE + ":+ " + audit.getObjName() + ".";
                if ( VUtil.isNotNullOrEmpty( audit.getOpName() ) )
                {
                    filter += audit.getOpName();
                }
                filter += "*))";
            }
            if ( VUtil.isNotNullOrEmpty( audit.getInternalUserId() ) )
            {
                filter += "(|(" + REQMOD + "=" + GlobalIds.FT_MODIFIER + ":= " + audit.getInternalUserId() + ")";
                filter += "(" + REQMOD + "=" + GlobalIds.FT_MODIFIER + ":+ " + audit.getInternalUserId() + "))";
            }
            if ( audit.getBeginDate() != null )
            {
                String szTime = AttrHelper.encodeGeneralizedTime( audit.getBeginDate() );
                filter += "(" + REQEND + ">=" + szTime + ")";
            }
            if ( audit.getEndDate() != null )
            {
                String szTime = AttrHelper.encodeGeneralizedTime( audit.getEndDate() );
                filter += "(" + REQEND + "<=" + szTime + ")";
            }

            filter += ")";
            //log.warn("filter=" + filter);
            ld = getLogConnection();
            searchResults = search( ld, auditRoot,
                LDAPConnection.SCOPE_ONE, filter, AUDIT_MOD_ATRS, false, GlobalIds.BATCH_SIZE );
            long sequence = 0;
            while ( searchResults.hasMoreElements() )
            {
                modList.add( getModEntityFromLdapEntry( searchResults.next(), sequence++ ) );
            }
        }
        catch ( LDAPException e )
        {
            String error = "searchAdminMods caught LDAPException id=" + e.getLDAPResultCode() + " msg="
                + e.getMessage();
            throw new FinderException( GlobalErrIds.AUDT_MOD_ADMIN_SEARCH_FAILED, error, e );
        }
        finally
        {
            closeLogConnection( ld );
        }
        return modList;
    }


    /**
     * @param le
     * @return
     * @throws LDAPException
     */
    private Bind getBindEntityFromLdapEntry( LDAPEntry le, long sequence )
    {

        /*
        public class Bind
            private String createTimestamp;
            private String creatorsName;
            private String entryCSN;
            private String entryDN;
            private String entryUUID;
            private String hasSubordinates;
            private String modifiersName;
            private String modifyTimestamp;
            private String objectClass;
            private String reqAuthzID;
            private String reqControls;
            private String reqDN;
            private String reqEnd;
            private String reqMethod;
            private String reqResult;
            private String reqSession;
            private String reqStart;
            private String reqType;
            private String reqVersion;
            private String structuralObjectClass;
            */

        Bind auditBind = new ObjectFactory().createBind();
        auditBind.setSequenceId( sequence );
        auditBind.setCreateTimestamp( getAttribute( le, CREATETIMESTAMP ) );
        auditBind.setCreatorsName( getAttribute( le, CREATORSNAME ) );
        auditBind.setEntryCSN( getAttribute( le, ENTRYCSN ) );
        auditBind.setEntryDN( getAttribute( le, ENTRYDN ) );
        auditBind.setEntryUUID( getAttribute( le, ENTRYUUID ) );
        auditBind.setHasSubordinates( getAttribute( le, HASSUBORDINATES ) );
        auditBind.setModifiersName( getAttribute( le, MODIFIERSNAME ) );
        auditBind.setModifyTimestamp( getAttribute( le, MODIFYTIMESTAMP ) );
        auditBind.setObjectClass( getAttribute( le, OBJECTCLASS ) );
        auditBind.setReqAuthzID( getAttribute( le, REQUAUTHZID ) );
        auditBind.setReqControls( getAttribute( le, REQCONTROLS ) );
        auditBind.setReqDN( getAttribute( le, REQDN ) );
        auditBind.setReqEnd( getAttribute( le, REQEND ) );
        auditBind.setReqMethod( getAttribute( le, REQMETHOD ) );
        auditBind.setReqResult( getAttribute( le, REQRESULT ) );
        auditBind.setReqSession( getAttribute( le, REQSESSION ) );
        auditBind.setReqStart( getAttribute( le, REQSTART ) );
        auditBind.setReqType( getAttribute( le, REQTYPE ) );
        auditBind.setReqVersion( getAttribute( le, REQVERSION ) );
        auditBind.setStructuralObjectClass( getAttribute( le, STRUCTURALOBJECTCLASS ) );
        return auditBind;
    }


    /**
     * @param le
     * @return
     * @throws LDAPException
     */
    private AuthZ getAuthzEntityFromLdapEntry( LDAPEntry le, long sequence )
    {

        /*
        public class AuthZ
        {
            private String createTimestamp;
            private String creatorsName;
            private String entryCSN;
            private String entryDN;
            private String entryUUID;
            private String hasSubordinates;
            private String modifiersName;
            private String modifyTimestamp;
            private String objectClass;
            private String reqAttr;
            private String reqAttrsOnly;
            private String reqAuthzID;
            private String reqControls;
            private String reqDN;
            private String reqDerefAliases;
            private String reqEnd;
            private String reqEntries;
            private String reqFilter;
            private String reqResult;
            private String reqScope;
            private String reqSession;
            private String reqSizeLimit;
            private String reqStart;
            private String reqTimeLimit;
            private String reqType;
            private String structuralObjectClass;
            private String subschemaSubentry;
        }*/
        // these attrs also on audit bind OC:
        AuthZ authZ = new ObjectFactory().createAuthZ();
        authZ.setSequenceId( sequence );
        authZ.setCreateTimestamp( getAttribute( le, CREATETIMESTAMP ) );
        authZ.setCreatorsName( getAttribute( le, CREATORSNAME ) );
        authZ.setEntryCSN( getAttribute( le, ENTRYCSN ) );
        authZ.setEntryDN( getAttribute( le, ENTRYDN ) );
        authZ.setEntryUUID( getAttribute( le, ENTRYUUID ) );
        authZ.setHasSubordinates( getAttribute( le, HASSUBORDINATES ) );
        authZ.setModifiersName( getAttribute( le, MODIFIERSNAME ) );
        authZ.setModifyTimestamp( getAttribute( le, MODIFYTIMESTAMP ) );
        authZ.setObjectClass( getAttribute( le, OBJECTCLASS ) );
        authZ.setReqAuthzID( getAttribute( le, REQUAUTHZID ) );
        authZ.setReqControls( getAttribute( le, REQCONTROLS ) );
        authZ.setReqDN( getAttribute( le, REQDN ) );
        authZ.setReqEnd( getAttribute( le, REQEND ) );
        authZ.setReqResult( getAttribute( le, REQRESULT ) );
        authZ.setReqSession( getAttribute( le, REQSESSION ) );
        authZ.setReqStart( getAttribute( le, REQSTART ) );
        authZ.setReqType( getAttribute( le, REQTYPE ) );
        authZ.setStructuralObjectClass( getAttribute( le, STRUCTURALOBJECTCLASS ) );

        // these attrs only on audit search OC:
        authZ.setReqAttr( getAttribute( le, REQATTR ) );
        authZ.setReqAttrsOnly( getAttribute( le, REQATTRSONLY ) );
        authZ.setReqDerefAliases( getAttribute( le, REQDREFALIASES ) );
        authZ.setReqEntries( getAttribute( le, REQENTRIES ) );
        authZ.setReqFilter( getAttribute( le, REQFILTER ) );
        authZ.setReqScope( getAttribute( le, REQSCOPE ) );
        authZ.setReqSizeLimit( getAttribute( le, REQSIZELIMIT ) );
        authZ.setReqTimeLimit( getAttribute( le, REQTIMELIMIT ) );
        return authZ;
    }


    private Mod getModEntityFromLdapEntry( LDAPEntry le, long sequence )
    {
        /*
        public class Mod
        {
            private String reqSession;
            private String objectClass;
            private String reqAuthzID;
            private String reqDN;
            private String reqResult;
            private String reqStart;
            private String reqEnd;
            private String reqType;
            private String[] reqMod;
        }
        */

        Mod mod = new ObjectFactory().createMod();
        mod.setSequenceId( sequence );
        mod.setObjectClass( getAttribute( le, OBJECTCLASS ) );
        mod.setReqAuthzID( getAttribute( le, REQUAUTHZID ) );
        mod.setReqDN( getAttribute( le, REQDN ) );
        mod.setReqEnd( getAttribute( le, REQEND ) );
        mod.setReqResult( getAttribute( le, REQRESULT ) );
        mod.setReqSession( getAttribute( le, REQSESSION ) );
        mod.setReqStart( getAttribute( le, REQSTART ) );
        mod.setReqType( getAttribute( le, REQTYPE ) );
        mod.setReqMod( getAttributes( le, REQMOD ) );
        return mod;
    }
}