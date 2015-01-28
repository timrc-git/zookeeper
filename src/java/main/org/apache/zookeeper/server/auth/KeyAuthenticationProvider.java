
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

package org.apache.zookeeper.server.auth;

import java.io.UnsupportedEncodingException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.ServerCnxn;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.DataNode;
import org.apache.zookeeper.data.Stat;

/* 
 * This class is a sample implementation of being passed the ZooKeeperServer
 * handle in the constructor, and reading data from zknodes to authenticate.
 * At a minimum, a real Auth provider would need to override validate() to 
 * e.g. perform certificate validation of auth based a public key.
 */
public class KeyAuthenticationProvider implements AuthenticationProvider {
    private ZooKeeperServer zks = null;
    public KeyAuthenticationProvider(ZooKeeperServer server) {
        zks = server;
        System.out.println("KeyAuthenticationProvider plugin loaded.\n");
    }

    public String getScheme() {
        return "key";
    }

    public byte[] getKey() {
        ZKDatabase db = zks.getZKDatabase();
        if (db!=null) {
            try {
                Stat stat = new Stat();
                return db.getData("/key", stat, null);
            } catch (NoNodeException e) {
            }
        }
        return null;
    }

    public boolean validate(byte[] key, byte[] auth) {
        // perform arbitrary function (auth is a multiple of key)
        try {
            String keyStr = new String(key, "UTF-8");
            String authStr = new String(auth, "UTF-8");
            int keyVal = Integer.parseInt(keyStr);
            int authVal = Integer.parseInt(authStr);
            if (keyVal!=0 && ((authVal % keyVal) != 0)) {
              return false;
            }
        } catch (UnsupportedEncodingException e) {
        }
        return true;
    }

    public KeeperException.Code handleAuthentication(ServerCnxn cnxn, byte[] authData) {
        byte[] key = getKey();
        String authStr = "";
        String keyStr = "";
        try {
          authStr = new String(authData, "UTF-8");
        } catch (Exception e) {
          // empty authData
        }
        if (key != null) {
            if (!validate(key, authData)) {
                try { 
                  keyStr = new String(key, "UTF-8"); 
                } catch (Exception e) { 
                    // empty key
                    keyStr = authStr;
                }
                System.out.println("KeyAuthenticationProvider handleAuthentication ("+keyStr+", "+authStr+") -> FAIL.\n");
                return KeeperException.Code.AUTHFAILED;
            }
        } else {
            System.out.println("KeyAuthenticationProvider handleAuthentication -> NO_KEY.\n");
        }
        // default to allow, so the key can be initially written
        System.out.println("KeyAuthenticationProvider handleAuthentication -> OK.\n");
        // TODO keyStr in addAuthInfo() sticks with the created node ACLs.
        //   For transient keys or certificates, this presents a problem.
        //   Replace it with something non-ephemeral (or punt with null).
        // Need BOTH addAuthInfo and OK return-code
        cnxn.addAuthInfo(new Id(getScheme(), keyStr));
        //cnxn.addAuthInfo(new Id(getScheme(), authStr));
        return KeeperException.Code.OK;
    }


    public boolean matches(String id, String aclExpr) {
        boolean ok = id.equals(aclExpr);
        System.out.println("KeyAuthenticationProvider matches ("+id+", "+aclExpr+") -> ("+ok+").\n");
        return ok;
    }

    public boolean isAuthenticated() {
        boolean ok = true;
        System.out.println("KeyAuthenticationProvider isAuthenticated -> ("+ok+").\n");
        return ok;
    }

    public boolean isValid(String id) {
        boolean ok = true;
        System.out.println("KeyAuthenticationProvider isValid ("+id+") -> ("+ok+").\n");
        return ok;
    }
}
