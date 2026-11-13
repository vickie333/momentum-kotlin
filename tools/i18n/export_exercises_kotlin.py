#!/usr/bin/env python3
"""Export validated Spanish and Portuguese exercise text for Kotlin."""
import json
import sys
from pathlib import Path

from exercises import ES, PT
from export_exercises import english_steps, validate

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'data/i18n/exercises.json'


def main():
    english = english_steps()
    if not english or any(not steps for steps in english.values()):
        sys.exit('no exercises found in data/exercises.json')
    errors = validate(english)
    if errors:
        sys.exit('\n'.join(errors))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as output:
        output.write(json.dumps({'es': ES, 'pt': PT}, ensure_ascii=False, sort_keys=True, indent=1) + '\n')
    print(f'wrote {OUT.relative_to(ROOT)}: {len(ES)} ES, {len(PT)} PT exercises')


if __name__ == '__main__':
    main()
