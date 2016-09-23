package org.apache.directory.fortress.core.impl;

import junit.framework.TestCase;
import org.apache.directory.fortress.core.*;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.model.Group;
import org.apache.directory.fortress.core.model.Role;
import org.apache.directory.fortress.core.model.User;
import org.apache.directory.fortress.core.model.UserRole;
import org.apache.directory.fortress.core.util.LogUtil;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;


public class GroupMgrImplTest extends TestCase {
    private static final String CLS_NM = AdminMgrImplTest.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger( CLS_NM );
    private GroupMgr groupMgr;

    public GroupMgrImplTest( String name )
    {
        super( name );
    }

    public void testAddGroup()
    {
        addGroups( "ADD-GRP TG1", GroupTestData.TEST_GROUP1 );
        addGroups( "ADD-GRP TG2", GroupTestData.TEST_GROUP2 );
        addGroups( "ADD-GRP TG3", GroupTestData.TEST_GROUP3 );
    }

    private void addGroups( String message, Group group )
    {
        LogUtil.logIt( message );
        try
        {
            groupMgr = GroupMgrFactory.createInstance(TestUtils.getContext());
            groupMgr.add( group );
            LOG.debug( "addGroup group [" + group.getName() + "] successful" );
        }
        catch ( SecurityException ex )
        {
            ex.printStackTrace();
            LOG.error("addGroup: caught SecurityException rc=" + ex.getErrorId() + ", msg=" + ex.getMessage(), ex);
            fail(ex.getMessage());
        }
    }

    public void testDeleteGroup()
    {
        deleteGroups( "DEL-GRP TG1", GroupTestData.TEST_GROUP1 );
        deleteGroups( "DEL-GRP TG2", GroupTestData.TEST_GROUP2 );
        deleteGroups( "DEL-GRP TG3", GroupTestData.TEST_GROUP3 );
    }

    private void deleteGroups( String message, Group group )
    {
        LogUtil.logIt( message );
        try
        {
            groupMgr = GroupMgrFactory.createInstance(TestUtils.getContext());
            Group nameOnlyGroup = new Group( group.getName() );
            groupMgr.delete( nameOnlyGroup );
            LOG.debug( "addGroup group [" + group.getName() + "] successful" );
        }
        catch ( SecurityException ex )
        {
            ex.printStackTrace();
            LOG.error("addGroup: caught SecurityException rc=" + ex.getErrorId() + ", msg=" + ex.getMessage(), ex);
            fail(ex.getMessage());
        }
    }

    public void testAssignGroupRoleMember()
    {
        assignRoleMember( "ASGN-GRP TG1 TR2", GroupTestData.TEST_GROUP1, RoleTestData.ROLES_TR2 );
        assignRoleMember( "ASGN-GRP TG2 TR3", GroupTestData.TEST_GROUP2, RoleTestData.ROLES_TR3 );
    }

    private void assignRoleMember( String message, Group group, String[][] roles )
    {
        LogUtil.logIt( message );
        try
        {
            groupMgr = GroupMgrFactory.createInstance( TestUtils.getContext() );

            for ( String[] roleArray : roles )
            {
                Role role = RoleTestData.getRole( roleArray );
                groupMgr.assign(group, role.getName() );
            }
            LOG.debug( "assignRoleMember group [" + group.getName() + "] successful" );

            int countOfOldRoles = group.getMembers().size();
            int countOfNewRoles = roles.length;
            // read from LDAP and get count of members
            Group groupFromLdap = groupMgr.read(group);
            int actualAmountOfMembers = groupFromLdap.getMembers().size();
            assertEquals( CLS_NM + ".assignRoleMember failed members size check",
                    countOfOldRoles + countOfNewRoles, actualAmountOfMembers);
        }
        catch ( SecurityException ex )
        {
            ex.printStackTrace();
            LOG.error("assignRoleMember: caught SecurityException rc=" + ex.getErrorId() + ", msg=" + ex.getMessage(), ex);
            fail(ex.getMessage());
        }
    }

    public void testAssignGroupUserMember()
    {
        assignUserMember( "ASGN-GRP TG3 TU2", GroupTestData.TEST_GROUP3, UserTestData.USERS_TU2);
    }

    private void assignUserMember( String message, Group group, String[][] users )
    {
        LogUtil.logIt( message );
        try
        {
            groupMgr = GroupMgrFactory.createInstance( TestUtils.getContext() );

            for ( String[] userArray : users )
            {
                User user = UserTestData.getUser( userArray );
                groupMgr.assign(group, user.getUserId() );
            }
            LOG.debug( "assignUserMember group [" + group.getName() + "] successful" );

            int countOfOldUsers = group.getMembers().size();
            int countOfNewUsers = users.length;
            // read from LDAP and get count of members
            Group groupFromLdap = groupMgr.read(group);
            int actualAmountOfMembers = groupFromLdap.getMembers().size();
            assertEquals( CLS_NM + ".assignUserMember failed members size check",
                    countOfOldUsers + countOfNewUsers, actualAmountOfMembers);
        }
        catch ( SecurityException ex )
        {
            ex.printStackTrace();
            LOG.error("assignUserMember: caught SecurityException rc=" + ex.getErrorId() + ", msg=" + ex.getMessage(), ex);
            fail(ex.getMessage());
        }
    }

    public void testDeassignGroupRoleMember()
    {
        deassignRoleMember( "DEASGN-GRP TG1 TR2", GroupTestData.TEST_GROUP1, RoleTestData.ROLES_TR2 );
        deassignRoleMember( "DEASGN-GRP TG2 TR3", GroupTestData.TEST_GROUP2, RoleTestData.ROLES_TR3 );
    }

    private void deassignRoleMember( String message, Group group, String[][] roles )
    {
        LogUtil.logIt( message );
        try
        {
            groupMgr = GroupMgrFactory.createInstance( TestUtils.getContext() );

            for ( String[] roleArray : roles )
            {
                Role role = RoleTestData.getRole( roleArray );
                groupMgr.deassign(group, role.getName() );
            }
            LOG.debug( "deassignRoleMember group [" + group.getName() + "] successful" );

            int countOfOldRoles = group.getMembers().size();
            // read from LDAP and get count of members
            Group groupFromLdap = groupMgr.read(group);
            int actualAmountOfMembers = groupFromLdap.getMembers().size();
            assertEquals( CLS_NM + ".deassignRoleMember failed members size check",
                    countOfOldRoles, actualAmountOfMembers);
        }
        catch ( SecurityException ex )
        {
            ex.printStackTrace();
            LOG.error("deassignRoleMember: caught SecurityException rc=" + ex.getErrorId() + ", msg=" + ex.getMessage(), ex);
            fail(ex.getMessage());
        }
    }

    public void testDeassignGroupUserMember()
    {
        deassignUserMember( "DEASGN-GRP TG3 TU2", GroupTestData.TEST_GROUP3, UserTestData.USERS_TU2);
    }

    private void deassignUserMember( String message, Group group, String[][] users )
    {
        LogUtil.logIt( message );
        try
        {
            groupMgr = GroupMgrFactory.createInstance( TestUtils.getContext() );

            for ( String[] userArray : users )
            {
                User user = UserTestData.getUser( userArray );
                groupMgr.deassign(group, user.getUserId() );
            }
            LOG.debug( "deassignUserMember group [" + group.getName() + "] successful" );

            int countOfOldUsers = group.getMembers().size();
            // read from LDAP and get count of members after deassignment
            Group groupFromLdap = groupMgr.read(group);
            int actualAmountOfMembers = groupFromLdap.getMembers().size();
            assertEquals( CLS_NM + ".deassignUserMember failed members size check",
                    countOfOldUsers, actualAmountOfMembers);
        }
        catch ( SecurityException ex )
        {
            ex.printStackTrace();
            LOG.error("deassignUserMember: caught SecurityException rc=" + ex.getErrorId() + ", msg=" + ex.getMessage(), ex);
            fail(ex.getMessage());
        }
    }

    public void testGroupRoles()
    {
        groupRoles( "GRP-RLS TG1 TR1+TR2", GroupTestData.TEST_GROUP1, RoleTestData.ROLES_TR2 );
        groupRoles( "GRP-RLS TG2 TR2+TR3", GroupTestData.TEST_GROUP2, RoleTestData.ROLES_TR3 );
    }

    private void groupRoles( String message, Group group, String[][] addedRoles )
    {
        LogUtil.logIt( message );
        try
        {
            groupMgr = GroupMgrFactory.createInstance( TestUtils.getContext() );
            List<UserRole> actualRoles = groupMgr.groupRoles(group);
            LOG.debug( "groupRoles group [" + group.getName() + "] successful" );

            int initialRolesSize = group.getMembers().size();
            int addedRolesSize = addedRoles.length;
            assertEquals( CLS_NM + ".groupRoles failed members size check",
                    initialRolesSize + addedRolesSize, actualRoles.size() );
        }
        catch ( SecurityException ex )
        {
            ex.printStackTrace();
            LOG.error("groupRoles: caught SecurityException rc=" + ex.getErrorId() + ", msg=" + ex.getMessage(), ex);
            fail(ex.getMessage());
        }
    }

    public void testRoleGroups()
    {
        roleGroups( "RLE-GRPS TR2 TG1+TG2", RoleTestData.ROLES_TR2,
                Arrays.asList( GroupTestData.TEST_GROUP1, GroupTestData.TEST_GROUP2 ));
    }

    private void roleGroups( String message, String[][] roles, List<Group> expectedGroups )
    {
        LogUtil.logIt( message );
        try
        {
            groupMgr = GroupMgrFactory.createInstance( TestUtils.getContext() );
            for ( String[] roleArray : roles )
            {
                Role role = RoleTestData.getRole(roleArray);
                List<Group> actualGroups = groupMgr.roleGroups(role);
                LOG.debug( "roleGroups role [" + role.getName() + "] successful" );

                assertEquals( CLS_NM + ".roleGroups failed", expectedGroups, actualGroups);
            }
        }
        catch ( SecurityException ex )
        {
            ex.printStackTrace();
            LOG.error("groupRoles: caught SecurityException rc=" + ex.getErrorId() + ", msg=" + ex.getMessage(), ex);
            fail(ex.getMessage());
        }
    }
}