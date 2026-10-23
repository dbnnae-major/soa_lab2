#!/usr/bin/env bash
set -euo pipefail
ROOT="$HOME/soa-lab2"
export JAVA_HOME=/usr/local/openjdk17
export _JAVA_OPTIONS='-Xmx128m -XX:MaxMetaspaceSize=128m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC -Xss256k'
cp "$ROOT/downloads/dragon-service.war" "$ROOT/servers/wildfly-35.0.1.Final/standalone/deployments/dragon-service.war"
touch "$ROOT/servers/wildfly-35.0.1.Final/standalone/deployments/dragon-service.war.dodeploy"
"$ROOT/servers/payara6/bin/asadmin" --host localhost --port 32448 deploy --force=true "$ROOT/downloads/killer-service.war"
cp "$ROOT/src/frontend/"* "$HOME/public_html/soa-lab2/"
chmod 644 "$HOME/public_html/soa-lab2/"*
for attempt in {1..40}; do
    status=$(curl -s --max-time 2 --cacert "$ROOT/config/dragon.crt" -o /dev/null -w '%{http_code}' https://localhost:31443/dragon-service/api/dragons || true)
    if [ "$status" = 200 ]; then
        echo 'Deployment responds. Run check.sh to verify all operations.'
        exit 0
    fi
    sleep 1
done
echo 'Dragon deployment is not ready; inspect server.log' >&2
exit 1
