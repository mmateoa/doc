"""Run real Android builds/tests; never substitute source stubs for APK verification."""
from pathlib import Path
import os
import subprocess
root = Path(__file__).resolve().parents[1]
wrapper = root / ("gradlew.bat" if os.name == "nt" else "gradlew")
raise SystemExit(subprocess.run([str(wrapper), "assembleDebug", "testDebugUnitTest"], cwd=root).returncode)
