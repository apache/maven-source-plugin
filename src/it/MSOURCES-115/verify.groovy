/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.*;
import java.util.jar.*;

try
{
    File clientJarFile = new File( basedir, "client/target/client-1.0-SNAPSHOT-sources.jar" );
    File serverJarFile = new File( basedir, "server/target/server-1.0-SNAPSHOT-sources.jar" );

    System.out.println( "Checking for existence of " + clientJarFile );
    if ( !clientJarFile.isFile() )
    {
        System.out.println( "FAILURE!" );
        return false;
    }

    System.out.println( "Checking for existence of " + serverJarFile );
    if ( !serverJarFile.isFile() )
    {
        System.out.println( "FAILURE!" );
        return false;
    }

    JarFile clientJar = new JarFile( clientJarFile );
    JarFile serverJar = new JarFile( serverJarFile );

    System.out.println( "Checking client sources jar contains its own module's source" );
    if ( clientJar.getEntry( "ClientThing.java" ) == null )
    {
        System.out.println( "FAILURE!" );
        return false;
    }

    System.out.println( "Checking client sources jar does not contain the unrelated server module's source" );
    if ( clientJar.getEntry( "ServerThing.java" ) != null )
    {
        System.out.println( "FAILURE!" );
        return false;
    }

    System.out.println( "Checking server sources jar contains its own module's source" );
    if ( serverJar.getEntry( "ServerThing.java" ) == null )
    {
        System.out.println( "FAILURE!" );
        return false;
    }

    System.out.println( "Checking server sources jar does not contain the unrelated client module's source" );
    if ( serverJar.getEntry( "ClientThing.java" ) != null )
    {
        System.out.println( "FAILURE!" );
        return false;
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
