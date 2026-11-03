#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
command -v java >/dev/null || { echo "Java 21+ is required." >&2; exit 1; }
command -v mvn >/dev/null || { echo "Maven 3.9+ is required." >&2; exit 1; }
command -v node >/dev/null || { echo "Node.js 20+ is required." >&2; exit 1; }
if [ ! -d "$ROOT/frontend/node_modules" ]; then (cd "$ROOT/frontend" && npm install); fi
(cd "$ROOT/backend" && mvn spring-boot:run) & BACKEND_PID=$!
(cd "$ROOT/frontend" && npm run dev) & FRONTEND_PID=$!
cleanup(){ kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true; wait "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true; }
trap cleanup EXIT INT TERM
while kill -0 "$BACKEND_PID" 2>/dev/null && kill -0 "$FRONTEND_PID" 2>/dev/null; do
  sleep 1
done
