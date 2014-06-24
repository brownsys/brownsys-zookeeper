brownsys additions:

RETRO:
1. Download and install aspectj from http://www.eclipse.org/aspectj/downloads.php
Install it using, eg: java -jar aspectj-1.8.0.jar
Then make sure the 'bin' directory of wherever you install it to is on your path.
2. Copy the following jars from <aspectj install folder>/lib to your ant lib directory (eg. ~/.ant/lib):
aspectjrt.jar
aspectjtools.jar
aspectjweaver.jar
org.aspectj.matcher.jar
3. Check out the retro branch
4. Build using: ant -Djavac.args="-Xlint -Xmaxwarns 1000" clean jar
5. Run with bin/zkServer
Jon: Instrumentation contains some debug stuff and is more verbose than necessary.  AspectJ dependencies are in the src/java/lib folder.  I don't know how to use ANT particularly well, so for now that's where they live and require manual updating.


For the latest information about ZooKeeper, please visit our website at:

   http://zookeeper.apache.org/

and our wiki, at:

   https://cwiki.apache.org/confluence/display/ZOOKEEPER

Full documentation for this release can also be found in docs/index.html

---------------------------
Packaging/release artifacts

The release artifact contains the following jar file at the toplevel:

zookeeper-<version>.jar         - legacy jar file which contains all classes
                                  and source files. Prior to version 3.3.0 this
                                  was the only jar file available. It has the 
                                  benefit of having the source included (for
                                  debugging purposes) however is also larger as
                                  a result

The release artifact contains the following jar files in "dist-maven" directory:

zookeeper-<version>.jar         - bin (binary) jar - contains only class (*.class) files
zookeeper-<version>-sources.jar - contains only src (*.java) files
zookeeper-<version>-javadoc.jar - contains only javadoc files

These bin/src/javadoc jars were added specifically to support Maven/Ivy which have 
the ability to pull these down automatically as part of your build process. 
The content of the legacy jar and the bin+sources jar are the same.

As of version 3.3.0 bin/sources/javadoc jars contained in dist-maven directory
are deployed to the Apache Maven repository after the release has been accepted
by Apache:
  http://people.apache.org/repo/m2-ibiblio-rsync-repository/
