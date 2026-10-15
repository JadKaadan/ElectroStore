#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/profit-tests
compiler=(javac)
if [ -n "${ECJ:-}" ]; then compiler=(java -jar "$ECJ" -source 8 -target 8); fi
"${compiler[@]}" -encoding UTF-8 -d build/profit-tests \
 app/src/main/java/com/electrostore/app/ProfitTotals.java \
 app/src/main/java/com/electrostore/app/ProfitDates.java tools/tests/ProfitChecks.java
java -cp build/profit-tests com.electrostore.app.ProfitChecks
python3 tools/tests/check_profit_queries.py
