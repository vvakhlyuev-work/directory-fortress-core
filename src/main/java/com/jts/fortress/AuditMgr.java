/*
 * Copyright (c) 2009-2012. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress;

import com.jts.fortress.audit.AuthZ;
import com.jts.fortress.audit.Mod;
import com.jts.fortress.audit.UserAudit;
import com.jts.fortress.audit.Bind;

import java.util.List;

/**
 * This interface prescribes methods used to search <a href="http://www.openldap.org/">OpenLDAP</a>'s slapd access log.  The access log events are
 * persisted in <a href="http://www.oracle.com/technetwork/database/berkeleydb/overview/index.html">BDB</a> and available for inquiry via common LDAP protocols.
 * Audit entries stored on behalf of Fortress operations correspond to runtime authentication {@link Bind}, authorization {@link com.jts.fortress.audit.AuthZ} and modification {@link com.jts.fortress.audit.Mod}
 * events as they occur automatically on the server when audit is enabled.
 * <h4>Audit Interrogator</h4>
 * Provides an OpenLDAP access log retrieval mechanism that enables security event monitoring.
 * <ol>
 * <li>Authentication events:
 * <li>Session enablement events
 * <li>Authorization events
 * <li>Entity mods and deletes
 * </li>
 * </ol>
 * <img src="../../../images/Audit.png">
 * <p/>
 * All events include Fortress context, see {@link FortEntity}.
 * <p/>
 * <h4>
 * The following APIs generate events subsequently stored in this access log:
 * </h4>
 * <ul>
 * <li> {@link com.jts.fortress.AccessMgr}
 * <li> {@link com.jts.fortress.AdminMgr}
 * <li> {@link com.jts.fortress.AdminMgr}
 * <li> {@link DelegatedAdminMgr}
 * <li> {@link com.jts.fortress.configuration.ConfigMgr}
 * <li> {@link PswdPolicyMgr}
 * </ul>
 * <h4>
 * The following reports are supported using search input: {@link com.jts.fortress.audit.UserAudit}
 * </h4>
 * <ul>
 * <li>User Authentications:     <code>List<{@link com.jts.fortress.audit.Bind}>  {@link AuditMgr#searchBinds(com.jts.fortress.audit.UserAudit)}</code>
 * <li>Invalid User AuthN:       <code>List<{@link com.jts.fortress.audit.Bind}>  {@link AuditMgr#searchInvalidUsers(com.jts.fortress.audit.UserAudit)} </code>
 * <li>User Authorizations 1:    <code>List<{@link com.jts.fortress.audit.AuthZ}> {@link AuditMgr#getUserAuthZs(com.jts.fortress.audit.UserAudit)} </code>
 * <li>User Authorizations 2:    <code>List<{@link com.jts.fortress.audit.AuthZ}> {@link AuditMgr#searchAuthZs(com.jts.fortress.audit.UserAudit)} </code>
 * <li>User Session Activations: <code>List<{@link com.jts.fortress.audit.Mod}>   {@link AuditMgr#searchUserSessions(com.jts.fortress.audit.UserAudit)} </code>
 * <li>Entity Modifications:     <code>List<{@link com.jts.fortress.audit.Mod}>   {@link AuditMgr#searchAdminMods(com.jts.fortress.audit.UserAudit)} </code>
 * </ul>
 * <p/>
 *
 * @author Shawn McKinney
 * @created April 2, 2010
 */
public interface AuditMgr extends com.jts.fortress.Authorizable
{
    /**
     * This method returns a list of authorization events for a particular user {@link com.jts.fortress.audit.UserAudit#userId}
     * and given timestamp field {@link com.jts.fortress.audit.UserAudit#beginDate}.<BR>
     * Method also can discriminate between all events or failed only by setting {@link com.jts.fortress.audit.UserAudit#failedOnly}.
     * <h4>optional parameters</h4>
     * <ul>
     * <li>{@link UserAudit#userId} - contains the target userId</li>
     * <li>{@link UserAudit#beginDate} - contains the date in which to begin search</li>
     * <li>{@link UserAudit#failedOnly} - if set to 'true', return only failed authorization events</li>
     * </ul>
     *
     * @param uAudit This entity is instantiated and populated before invocation.
     * @return a List of objects of type AuthZ.  Each AuthZ object contains one authorization event.
     * @throws SecurityException if a runtime system error occurs.
     */
    public List<AuthZ> getUserAuthZs(UserAudit uAudit)
        throws com.jts.fortress.SecurityException;

    /**
     * This method returns a list of authorization events for a particular user {@link com.jts.fortress.audit.UserAudit#userId},
     * object {@link com.jts.fortress.audit.UserAudit#objName}, and given timestamp field {@link com.jts.fortress.audit.UserAudit#beginDate}.<BR>
     * Method also can discriminate between all events or failed only by setting flag {@link UserAudit#failedOnly}..
     * <h4>required parameters</h4>
     * <ul>
     * <li>{@link UserAudit#userId} - contains the target userId<</li>
     * <li>{@link UserAudit#objName} - contains the object (authorization resource) name</li>
     * </ul>
     * <h4>optional parameters</h4>
     * <ul>
     * <li>{@link UserAudit#beginDate} - contains the date in which to begin search</li>
     * <li>{@link UserAudit#failedOnly} - if set to 'true', return only failed authorization events</li>
     * </ul>
     *
     * @param uAudit This entity is instantiated and populated before invocation.
     * @return a List of objects of type AuthZ.  Each AuthZ object contains one authorization event.
     * @throws com.jts.fortress.SecurityException
     *          if a runtime system error occurs.
     */
    public List<AuthZ> searchAuthZs(UserAudit uAudit)
        throws com.jts.fortress.SecurityException;

    /**
     * This method returns a list of authentication audit events for a particular user {@link com.jts.fortress.audit.UserAudit#userId},
     * and given timestamp field {@link com.jts.fortress.audit.UserAudit#beginDate}.<BR>
     * <h4>optional parameters</h4>
     * <ul>
     * <li>{@link UserAudit#userId} - contains the target userId<</li>
     * <li>{@link UserAudit#beginDate} - contains the date in which to begin search</li>
     * <li>{@link UserAudit#failedOnly} - if set to 'true', return only failed authorization events</li>
     * </ul>
     *
     * @param uAudit This entity is instantiated and populated before invocation.
     * @return a List of objects of type Bind.  Each Bind object contains one bind event.
     * @throws com.jts.fortress.SecurityException
     *          if a runtime system error occurs.
     */
    public List<Bind> searchBinds(UserAudit uAudit)
        throws com.jts.fortress.SecurityException;

    /**
     * This method returns a list of sessions created for a given user {@link UserAudit#userId},
     * and timestamp {@link com.jts.fortress.audit.UserAudit#beginDate}.<BR>
     * <h4>required parameters</h4>
     * <ul>
     * <li>{@link UserAudit#userId} - contains the target userId<</li>
     * </ul>
     * <h4>optional parameters</h4>
     * <ul>
     * <li>{@link UserAudit#beginDate} - contains the date in which to begin search</li>
     * </ul>
     *
     * @param uAudit This entity is instantiated and populated before invocation.
     * @return a List of objects of type Mod.  Each Mod object in list corresponds to one update or delete event on directory.
     * @throws SecurityException if a runtime system error occurs.
     */
    public List<Mod> searchUserSessions(UserAudit uAudit)
        throws com.jts.fortress.SecurityException;

    /**
     * This method returns a list of admin operations events for a particular entity {@link com.jts.fortress.audit.UserAudit#dn},
     * object {@link UserAudit#objName} and timestamp {@link com.jts.fortress.audit.UserAudit#beginDate}.  If the internal
     * userId {@link com.jts.fortress.audit.UserAudit#internalUserId} is set it will limit search by that field.
     * <h4>optional parameters</h4>
     * <ul>
     * <li>{@link UserAudit#dn} - contains the LDAP distinguished name for the updated object.  For example if caller
     * wants to find out what changes were made to John Doe's user object this would be 'uid=jdoe,ou=People,dc=example,dc=com'</li>
     * <li>{@link UserAudit#objName} - contains the object (authorization resource) name corresponding to the event.  For example if caller
     * wants to return events where User object was modified, this would be 'updateUser'</li>
     * <li>{@link UserAudit#internalUserId} - maps to the internalUserId of user who changed the record in LDAP.  This maps to {@link com.jts.fortress.rbac.User#internalId}.</li>
     * <li>{@link UserAudit#beginDate} - contains the date in which to begin search</li>
     * <li>{@link UserAudit#endDate} - contains the date in which to end search</li>
     * </ul>
     *
     * @param uAudit This entity is instantiated and populated before invocation.
     * @return a List of objects of type Mod.  Each Mod object in list corresponds to one update or delete event on directory.
     * @throws com.jts.fortress.SecurityException
     *          if a runtime system error occurs.
     */
    public List<Mod> searchAdminMods(UserAudit uAudit)
        throws SecurityException;

    /**
     * This method returns a list of failed authentication attempts on behalf of an invalid identity {@link com.jts.fortress.audit.UserAudit#userId},
     * and given timestamp {@link UserAudit#beginDate}.  If the {@link com.jts.fortress.audit.UserAudit#failedOnly} is true it will
     * return only authentication attempts made with invalid userId.  This event represents either User incorrectly entering userId during signon or
     * possible fraudulent logon attempt by hostile agent.
     * </p>
     * This event is generated when Fortress looks up User record prior to LDAP bind operation.
     * <h4>optional parameters</h4>
     * <ul>
     * <li>{@link UserAudit#userId} - contains the target userId</li>
     * <li>{@link UserAudit#beginDate} - contains the date in which to begin search</li>
     * <li>{@link UserAudit#failedOnly} - if set to 'true', return only failed authorization events</li>
     * </ul>
     *
     * @param uAudit This entity is instantiated and populated before invocation.
     * @return a List of objects of type AuthZ.  Each AuthZ object contains one failed authentication event.
     * @throws com.jts.fortress.SecurityException
     *          if a runtime system error occurs.
     */
    public List<AuthZ> searchInvalidUsers(UserAudit uAudit)
        throws com.jts.fortress.SecurityException;

}
