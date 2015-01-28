/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;

import org.junit.Assert;
import org.junit.Test;

public class KeyAuthClientTest extends ClientBase {
    static final ArrayList<ACL> KeyACLs = new ArrayList<ACL>(
                Collections.singletonList(new ACL(Perms.ALL, new Id("key","anyone"))));
    static {
        System.setProperty("zookeeper.authProvider.1","org.apache.zookeeper.server.auth.KeyAuthenticationProvider");
    }

    public void createNodePrintAcl(ZooKeeper zk, String path, String testName) {
      try {
        System.out.println("KeyAuthenticationProvider Creating Test Node:"+path+".\n");
        zk.create(path, null, Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT);
        List<ACL> acls = zk.getACL(path, null);
        System.out.println("Node: "+path+" Test:"+testName+" ACLs:");
        for (ACL acl : acls) {
          System.out.println("  "+acl.toString());
        } 
      } catch (Exception e) {
          System.out.println("  EXCEPTION THROWN");
      }
    }
    public void testPreAuth() throws Exception {
        ZooKeeper zk = createClient();
        zk.addAuthInfo("key", "25".getBytes());
        try {
            createNodePrintAcl(zk, "/pre", "testPreAuth");
            System.out.println("KeyAuthenticationProvider setACL(/).\n");
            //zk.setACL("/", KeyACLs, -1);
            zk.setACL("/", Ids.CREATOR_ALL_ACL, -1);
            //zk.setACL("/", Ids.OPEN_ACL_UNSAFE, -1);
            System.out.println("KeyAuthenticationProvider getChildren(/).\n");
            zk.getChildren("/", false);
            //zk.create("/abc", null, KeyACLs, CreateMode.PERSISTENT);
            zk.create("/abc", null, Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT);
            zk.setData("/abc", "testData1".getBytes(), -1);
            System.out.println("KeyAuthenticationProvider create(/key).\n");
            //zk.create("/key", null, KeyACLs, CreateMode.PERSISTENT);
            zk.create("/key", null, Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT);
            System.out.println("KeyAuthenticationProvider setData(/key).\n");
            zk.setData("/key", "5".getBytes(), -1);
            Thread.sleep(1000);
        } catch (KeeperException e) {
            Assert.fail("test failed :" + e);
        }
        finally {
            zk.close();
        }
    }

    public void testMissingAuth() throws Exception {
        ZooKeeper zk = createClient();
        try {
            zk.getData("/abc", false, null);
            Assert.fail("Should not be able to get data");
        } catch (KeeperException e) {
        }
        try {
            zk.setData("/abc", "testData2".getBytes(), -1);
            Assert.fail("Should not be able to set data");
        } catch (KeeperException e) {
        }
        finally {
            zk.close();
        }
    }

    public void testValidAuth() throws Exception {
        ZooKeeper zk = createClient();
        // any multiple of 5 will do...
        zk.addAuthInfo("key", "25".getBytes());
        try {
            createNodePrintAcl(zk, "/valid", "testValidAuth");
            zk.getData("/abc", false, null);
            zk.setData("/abc", "testData3".getBytes(), -1);
        } catch (KeeperException.AuthFailedException e) {
            Assert.fail("test failed :" + e);
        }
        finally {
            zk.close();
        }
    }

    public void testValidAuth2() throws Exception {
        ZooKeeper zk = createClient();
        // any multiple of 5 will do...
        zk.addAuthInfo("key", "125".getBytes());
        try {
            createNodePrintAcl(zk, "/valid2", "testValidAuth2");
            zk.getData("/abc", false, null);
            zk.setData("/abc", "testData3".getBytes(), -1);
        } catch (KeeperException.AuthFailedException e) {
            Assert.fail("test failed :" + e);
        }
        finally {
            zk.close();
        }
    }


    @Test
    public void testAuth() throws Exception {
        testPreAuth();
        testMissingAuth();
        testValidAuth();
        testValidAuth2();
    }

}
