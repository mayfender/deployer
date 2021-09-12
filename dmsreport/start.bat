@ECHO OFF
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Xms1G -Xmx2G -Duser.language=en -Duser.region=UK
Rem copy javaw to javaw-DMS_Report for easy to determine
start "" "C:\Program Files\Java\jdk1.8.0_151\bin\javaw-DMS_Report" %JAVA_OPTS% -jar dmsreport.jar