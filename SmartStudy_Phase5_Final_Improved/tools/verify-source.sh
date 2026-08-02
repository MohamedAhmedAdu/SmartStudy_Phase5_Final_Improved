#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
for p in Path('src/main/resources/fxml').glob('*.fxml'):
    ET.parse(p)
    print('FXML OK:', p.name)
PY
rm -rf /tmp/smartstudy_core && mkdir -p /tmp/smartstudy_core
javac -d /tmp/smartstudy_core \
  src/main/java/com/smartstudy/model/TaskStatus.java \
  src/main/java/com/smartstudy/model/TaskType.java \
  src/main/java/com/smartstudy/model/AcademicTask.java \
  src/main/java/com/smartstudy/model/StudySession.java \
  src/main/java/com/smartstudy/service/ScheduleGenerator.java \
  src/main/java/com/smartstudy/util/Validation.java \
  tools/CoreLogicSmokeTest.java
java -cp /tmp/smartstudy_core CoreLogicSmokeTest
