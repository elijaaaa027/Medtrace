#!/bin/bash
while true; do
  if ! curl -fsS http://localhost:3000/health >/dev/null 2>&1; then
    cd /app/applet && nohup node dev-server.cjs >> /tmp/dev-server.log 2>&1 &
  fi
  sleep 3
done
