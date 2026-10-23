#!/usr/bin/env bash
set -euo pipefail
ROOT="$HOME/soa-lab2"
export JAVA_HOME=/usr/local/openjdk17
export _JAVA_OPTIONS='-Xmx384m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2 -Xss256k'
service="${1:?Use dragon or killer}"
action="${2:?Use start, stop, restart or status}"
WF="$ROOT/servers/wildfly-35.0.1.Final"
PA="$ROOT/servers/payara6"
case "$service" in dragon|killer) ;; *) exit 2;; esac
running() {
    if [ "$service" = dragon ]; then
        [ -f "$ROOT/run/dragon.pid" ] && kill -0 "$(cat "$ROOT/run/dragon.pid")" 2>/dev/null && ps -ww -p "$(cat "$ROOT/run/dragon.pid")" -o command= | grep -F "$WF/bin/standalone.sh" > /dev/null
    else
        [ -f "$PA/glassfish/domains/domain1/config/pid" ] && kill -0 "$(cat "$PA/glassfish/domains/domain1/config/pid")" 2>/dev/null && ps -ww -p "$(cat "$PA/glassfish/domains/domain1/config/pid")" -o command= | grep -F "$PA/glassfish" > /dev/null
    fi
}
case "$action" in
start)
    if running; then echo "$service already running"; exit 0; fi
    if [ "$service" = dragon ]; then
        JAVA_OPTS='-Xms64m -Xmx384m -XX:MaxMetaspaceSize=256m -Djava.net.preferIPv4Stack=true -Dorg.jboss.as.server.boot.maxThreads=2' nohup "$WF/bin/standalone.sh" -b 0.0.0.0 -bmanagement 127.0.0.1 > "$ROOT/logs/dragon-console.log" 2>&1 < /dev/null &
        echo $! > "$ROOT/run/dragon.pid"
    else
        "$PA/bin/asadmin" start-domain domain1
    fi
    ;;
stop)
    if ! running; then echo "$service stopped"; exit 0; fi
    if [ "$service" = dragon ]; then
        wrapper=$(cat "$ROOT/run/dragon.pid")
        server=$(pgrep -P "$wrapper" -f 'org.jboss.as.standalone')
        kill -TERM "$server"
    else
        kill -TERM "$(cat "$PA/glassfish/domains/domain1/config/pid")"
    fi
    for attempt in {1..30}; do
        if ! running; then exit 0; fi
        sleep 1
    done
    echo "$service did not stop within 30 seconds" >&2
    exit 1
    ;;
restart) "$0" "$service" stop; "$0" "$service" start ;;
status) if running; then echo "$service running"; else echo "$service stopped"; exit 1; fi ;;
*) exit 2;;
esac
