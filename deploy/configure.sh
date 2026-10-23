#!/usr/bin/env bash
set -euo pipefail
umask 077
ROOT="$HOME/soa-lab2"
WF="$ROOT/servers/wildfly-35.0.1.Final"
PA="$ROOT/servers/payara6"
DOMAIN="$PA/glassfish/domains/domain1"
export JAVA_HOME=/usr/local/openjdk17
export PATH="$JAVA_HOME/bin:$PATH"
export _JAVA_OPTIONS='-Xmx384m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2 -Xss256k'
: "${LAB_DB_PASSWORD:?Enter PostgreSQL password in the calling shell}"
export LAB_DB_PASSWORD
export PGPASSWORD="$LAB_DB_PASSWORD"
psql -w -v ON_ERROR_STOP=1 -h pg -U s412958 -d studs -f "$ROOT/src/dragon-service/db/dragon.sql"
psql -w -v ON_ERROR_STOP=1 -h pg -U s412958 -d studs -f "$ROOT/src/killer-service/db/killer_team.sql"
unset PGPASSWORD
curl -fsSL --retry 2 --connect-timeout 15 --max-time 90 -o "$ROOT/downloads/postgresql.jar" https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.5/postgresql-42.7.5.jar
cp "$ROOT/downloads/postgresql.jar" "$DOMAIN/lib/postgresql.jar"
mkdir -p "$WF/modules/org/postgresql/main"
cp "$ROOT/downloads/postgresql.jar" "$WF/modules/org/postgresql/main/postgresql.jar"
cat > "$WF/modules/org/postgresql/main/module.xml" <<'XML'
<module xmlns="urn:jboss:module:1.9" name="org.postgresql">
  <resources><resource-root path="postgresql.jar"/></resources>
  <dependencies><module name="java.se"/></dependencies>
</module>
XML
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -validity 365 -dname 'CN=helios.cs.ifmo.ru' -ext 'SAN=dns:helios.cs.ifmo.ru,dns:se.ifmo.ru,dns:localhost,ip:127.0.0.1' -storetype JKS -keystore "$WF/standalone/configuration/application.keystore" -storepass password -keypass password
keytool -exportcert -rfc -alias server -keystore "$WF/standalone/configuration/application.keystore" -storepass password -file "$ROOT/config/dragon.crt"
keytool -genkeypair -alias lab2 -keyalg RSA -keysize 2048 -validity 365 -dname 'CN=helios.cs.ifmo.ru' -ext 'SAN=dns:helios.cs.ifmo.ru,dns:se.ifmo.ru,dns:localhost,ip:127.0.0.1' -storetype PKCS12 -keystore "$DOMAIN/config/keystore.p12" -storepass changeit -keypass changeit
keytool -exportcert -rfc -alias lab2 -keystore "$DOMAIN/config/keystore.p12" -storepass changeit -file "$ROOT/config/killer.crt"
keytool -importcert -noprompt -alias dragon-lab2 -file "$ROOT/config/dragon.crt" -keystore "$DOMAIN/config/cacerts.p12" -storepass changeit
cat > "$ROOT/config/wildfly.cli" <<'CLI'
embed-server --server-config=standalone.xml --std-out=echo
/subsystem=elytron/credential-store=lab2:add(location=lab2.cs,relative-to=jboss.server.config.dir,create=true,credential-reference={clear-text="lab2-local-store"})
/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-class-name=org.postgresql.Driver)
/subsystem=datasources/data-source=SoaLab2:add(jndi-name=java:jboss/datasources/SoaLab2,driver-name=postgresql,connection-url="jdbc:postgresql://pg:5432/studs?connectTimeout=5&socketTimeout=10",user-name=s412958,credential-reference={store=lab2,alias=database},min-pool-size=0,max-pool-size=5,blocking-timeout-wait-millis=5000,check-valid-connection-sql="SELECT 1",validate-on-match=true)
/subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=connector-ref,value=https)
/subsystem=undertow/server=default-server/http-listener=default:remove
/socket-binding-group=standard-sockets/socket-binding=https:write-attribute(name=port,value=31443)
/socket-binding-group=standard-sockets/socket-binding=management-http:write-attribute(name=port,value=31490)
stop-embedded-server
CLI
"$WF/bin/jboss-cli.sh" --file="$ROOT/config/wildfly.cli"
printf '%s\n%s\n' "$LAB_DB_PASSWORD" "$LAB_DB_PASSWORD" | "$WF/bin/elytron-tool.sh" credential-store --location "$WF/standalone/configuration/lab2.cs" --password lab2-local-store --add database
perl -pi -e 's/<core-threads count="50"\/>/<core-threads count="4"\/>/g; s/<max-threads count="50"\/>/<max-threads count="8"\/>/g' "$WF/standalone/configuration/standalone.xml"
# Domain shipped with the distribution is private to this laboratory.
perl -0777 -pi -e 's/port="8080"/port="32080" enabled="false"/g; s/port="8181"/port="32443"/g; s/port="4848"/port="32448" address="127.0.0.1"/g; s/port="3700"/port="32037"/g; s/port="3820"/port="32038"/g; s/port="3920"/port="32039"/g; s/port="8686"/port="32086"/g; s/port="7676"/port="32076"/g; s/cert-nickname="s1as"/cert-nickname="lab2"/g; s#</java-config>#<jvm-options>-Ddragon.api.url=https://localhost:31443/dragon-service/api</jvm-options></java-config>#g' "$DOMAIN/config/domain.xml"
"$PA/bin/asadmin" start-domain domain1
"$PA/bin/asadmin" --host localhost --port 32448 set-hazelcast-configuration --enabled=false --dynamic=true
"$PA/bin/asadmin" --host localhost --port 32448 set 'configs.config.server-config.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size=16'
"$PA/bin/asadmin" --host localhost --port 32448 set 'configs.config.server-config.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size=2'
printf 'AS_ADMIN_PASSWORD=\nAS_ADMIN_ALIASPASSWORD=%s\n' "$LAB_DB_PASSWORD" | "$PA/bin/asadmin" --host localhost --port 32448 --passwordfile /dev/stdin create-password-alias lab2db
unset LAB_DB_PASSWORD
"$PA/bin/asadmin" --host localhost --port 32448 create-jdbc-connection-pool --datasourceclassname org.postgresql.ds.PGSimpleDataSource --restype javax.sql.DataSource --steadypoolsize 0 --maxpoolsize 5 --maxwait 5000 --isconnectvalidatereq true --validationmethod auto-commit --property 'serverName=pg:portNumber=5432:databaseName=studs:user=s412958:password=${ALIAS=lab2db}:connectTimeout=5:socketTimeout=10' SoaLab2Pool
"$PA/bin/asadmin" --host localhost --port 32448 create-jdbc-resource --connectionpoolid SoaLab2Pool jdbc/soaLab2
"$PA/bin/asadmin" --host localhost --port 32448 ping-connection-pool SoaLab2Pool
"$PA/bin/asadmin" --host localhost --port 32448 stop-domain domain1
printf 'Configuration completed.\n'
