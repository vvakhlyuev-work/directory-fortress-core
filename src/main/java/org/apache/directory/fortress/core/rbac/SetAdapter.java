/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.fortress.core.rbac;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA.
 * User: Shawn McKinney
 * Date: 1/21/12
 * Time: 7:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class SetAdapter extends XmlAdapter<ArrayList<String>, Set<String>>
{
    public Set<String> unmarshal(ArrayList<String> val) throws Exception
    {
        Set<String> members = null;
        if(val != null)
        {
            members = new TreeSet<>();
            for(String member : val)
            {
                members.add(member);
            }
        }
        return members;
    }

    public ArrayList<String> marshal(Set<String> val) throws Exception
    {
        ArrayList<String> members = null;
        if(val != null)
        {
            members = new ArrayList<>();
            for(String member : val)
            {
                members.add(member);
            }
        }
        return members;
    }
}


/*
    public char[] unmarshal(String val) throws Exception
    {
        return val.toCharArray();
    }

    public String marshal(char[] val) throws Exception
    {
        return val.toString();
    }

 */