### isaac-rest-api 

Simplified ISAAC APIs for REST access

### Support:
This implementation is provided and supported by VetsEZ (Veterans EZ Info, Inc.) in combination with Sagebits LLC.
This implementation only supports READ APIs.   
As part of our suite of terminology offerings, we also have an extended version that supports WRITE APIs, in addition 
to a full graphical web-based editor.

Reach out to us at contact@vetsez.com, https://vetsez.com/ for more details.

### Building
This code has a dependency on https://github.com/OSEHRA/ISAAC

The develop branch of this code tracks the develop branch of ISAAC, so to build this code, you must first check out, and do a 'mvn install' on the develop branch 
of ISAAC.  This branch is for Java 11.  There is another branch for Java 8.

After you have built ISAAC, and installed it into your local repository, then you can build this code with maven.  'mvn clean package' will get you a deployable war file.

### Launching:
- To run in Eclipse, execute the class net.sagebits.tmp.isaac.rest.LocalGrizzlyRunner.  You may optionally set a system property called -DisaacDatabaseLocation pointing 
to the location of the .data file. For example, on my system the path is -DisaacDatabaseLocation=c:\temp\database\vhat-2016.01.07-1.0-SNAPSHOT-all.data. 
In Eclipse, put this in the VM Argument tab under the Run Configurations menu.

To have it download a particular DB during the startup sequence, edit the file
```
isaac-rest\src\test\resources\uts-rest-api.properties
```
and specify the "nexus_..." parameters, and the "db_..." parameters.  If the 'nexus_pwd' parameter is encrypted, then place the decryption password in a file named:
```
isaac-rest\decryption.password
```
or, place the password file in another location, and specify the location of the password decryption file via the environment variable DECRYPTION_FILE


- To run from the command line - with a full build:
```
mvn clean test-compile -Pstart-server
```
- To run from the command line - after having already built the code with 'mvn clean package'
```
mvn -Prun exec:exec
```

### Helping with development
This code may be found on several git servers, however, if you have additions, please submit them as pull requests to to https://github.com/OSEHRA/isaac-rest-api/pulls

### Bug tracker
You may file bugs at https://github.com/OSEHRA/isaac-rest-api/issues

### To enable reading content from an additional maven repo during a bitbucket pipelines run, set the environment variables:
```
DEPLOYMENT_REPO
REPO_USERNAME
REPO_PASSWORD
```

### Notes on server support:

- Tomcat - works

- Grizzly - works - see 'LocalGrizzlyRunner' in the src/test/java folder.

- GlassFish - Fatal incompatibility: https://github.com/javaee/glassfish/issues/21509

- WildFly / JBoss - Annoyances with JavaFX support (need custom JBoss files - see commit history in WEB-INF folder), never got it to work 
with a current version of JAX-RS.  Issues with Array type support (due to old version of JAX-RS).  Haven't retested in a couple of years.

- WebLogic - Better than WildFly - but still issues with using an old version of JAX-RS.  See commit history for (unsuccessful) attempts
to upgrade it.  Haven't retested in a couple of years.
