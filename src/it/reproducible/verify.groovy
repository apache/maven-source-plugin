
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

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.apache.commons.compress.archivers.zip.*;

File deployDir = new File( basedir, 'target/repo/org/apache/maven/its/reproducible/1.0' )

assert deployDir.exists()

File sha1File = new File( deployDir, 'reproducible-1.0-sources.jar.sha1' )

assert deployDir.exists()

String sha1 = sha1File.text

StringBuffer r = new StringBuffer()
r.append( "reproducible-1.0-sources.jar sha1 = $sha1\n\n" )

File jarFile = new File( deployDir, 'reproducible-1.0-sources.jar' )
ZipFile zipFile = new ZipFile( jarFile );

r.append( 'encoding: ' + zipFile.getEncoding()  + '\n' )
r.append( 'timezone offset (minutes): ' + new Date().getTimezoneOffset() + '\n' )
r.append( 'M size (cmp) crc      java time     date       time           zip time   mode   name -comment; extra\n' )

String describeExtra( ZipExtraField[] extras )
{
  StringBuffer b = new StringBuffer()
  b.append( extras.length )
  for( ZipExtraField extra : extras )
  {
    b.append( sprintf( " [%s]%x", extra.getHeaderId().toString(), getLocalFileDataData() ) )
  }
  return b.toString()
}

long javaToDosTime( Date d )
{
    int year = d.getYear() + 1900;
    if ( year < 1980 )
    {
        return ZipEntry.DOSTIME_BEFORE_1980;
    }
    return ( year - 1980 ) << 25 | ( d.getMonth() + 1 ) << 21 |
           d.getDate() << 16 | d.getHours() << 11 | d.getMinutes() << 5 |
           d.getSeconds() >> 1;
}

// Normalize to UTC
long millis = Instant.parse( "2019-08-21T18:28:52Z" ).toEpochMilli();
Calendar cal = Calendar.getInstance();
cal.setTimeInMillis( millis );
millis = millis - ( cal.get( Calendar.ZONE_OFFSET ) + cal.get( Calendar.DST_OFFSET ) );
FileTime timestamp = FileTime.fromMillis( millis );

for ( ZipArchiveEntry zae : zipFile.getEntries() )
{
    r.append( sprintf( "%d %4d (%3d) %8x %d %<tF %<tT %<tz %d %6o %s %s; %s\n", zae.getMethod(), zae.getSize(), zae.getCompressedSize(), zae.getCrc(), zae.getTime(), javaToDosTime( zae.getLastModifiedDate() ), zae.getUnixMode(), zae.getName(), ( zae.getComment() == null ) ? '-' : zae.getComment(), describeExtra( zae.getExtraFields() ) ) )
    assert timestamp.equals( zae.getLastModifiedTime() );
}
zipFile.close();

String buf = r.toString()
println buf

assert buf.startsWith( "reproducible-1.0-sources.jar sha1 = f159379802c1f0dc1083af21352286b09d364519" )
