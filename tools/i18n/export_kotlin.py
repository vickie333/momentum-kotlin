#!/usr/bin/env python3
"""Export all string tables for Kotlin and validate statically used Kotlin keys."""
import argparse
from collections import Counter
import json
import re
import sys
from pathlib import Path

from export import PLACEHOLDER, load_modules

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'data/i18n/strings.json'
STR = r'"((?:[^"\\]|\\.)*)"'
CALLS = (
    re.compile(r'(?<![\w.])L\(\s*' + STR),
    re.compile(r'(?<![\w.])Loc\.text\(\s*' + STR),
    re.compile(r'(?<![\w.])text\(\s*' + STR + r'\s*,\s*listOf\('),
    re.compile(r'(?<![\w.])Lp\([^\"]*?(?:one\s*=\s*)?' + STR + r'\s*,\s*(?:other\s*=\s*)?' + STR, re.DOTALL),
)
SITES = re.compile(r'(?<![\w.])(?:Loc\.text|Lp|L|text)\(')
TEMPLATE = re.compile(r'(?<!\\)\$[A-Za-z_{]')
ESCAPE = {'t': '\t', 'r': '\r', 'b': '\b', "'": "'", '"': '"', '\\': '\\', 'n': '\n', '$': '$'}


def unescape(value):
    def replace(match):
        code = match.group(1)
        if code.startswith('u') and len(code) == 5:
            return chr(int(code[1:], 16))
        if code not in ESCAPE:
            raise ValueError(f'unknown escape \\{code}')
        return ESCAPE[code]
    return re.sub(r'\\(u[0-9a-fA-F]{4}|.)', replace, value)


def kotlin_keys():
    keys, problems = set(), []
    shared = ROOT / 'shared/src/commonMain/kotlin'
    if not shared.is_dir():
        problems.append(f'error: Kotlin source directory missing: {shared.relative_to(ROOT)}')
    for directory in (shared, ROOT / 'androidApp/src/main/kotlin'):
        if not directory.is_dir():
            continue
        for path in directory.rglob('*.kt'):
            source = path.read_text(encoding='utf-8')
            # Preserve offsets and line numbers while removing comments and wrapper definitions.
            source = re.sub(r'/\*.*?\*/', lambda m: re.sub(r'[^\n]', ' ', m.group()), source, flags=re.DOTALL)
            source = re.sub(r'^\s*//[^\n]*', lambda m: ' ' * len(m.group()), source, flags=re.MULTILINE)
            source = re.sub(r'^\s*fun (?:L|Lp|text)\([^\n]*', lambda m: ' ' * len(m.group()), source, flags=re.MULTILINE)
            covered = set()
            for pattern in CALLS:
                for match in pattern.finditer(source):
                    # A literal followed by concatenation is a dynamic key.
                    if re.match(r"\s*\+", source[match.end():]):
                        continue
                    covered.add(match.start())
                    for raw in match.groups():
                        line = source.count('\n', 0, match.start()) + 1
                        location = f'{path.relative_to(ROOT)}:{line}'
                        if TEMPLATE.search(raw):
                            problems.append(f'error: template localization key at {location}: {raw!r}')
                            continue
                        try:
                            keys.add(unescape(raw))
                        except ValueError as exc:
                            problems.append(f'error: {exc} at {location}: {raw!r}')
            for site in SITES.finditer(source):
                if site.start() not in covered:
                    line = source.count('\n', 0, site.start()) + 1
                    snippet = source[site.start():source.find('\n', site.start()) if '\n' in source[site.start():] else len(source)].strip()
                    problems.append(f'error: non-literal localization call at {path.relative_to(ROOT)}:{line}: {snippet}')
    if not keys:
        problems.append('error: no Kotlin localization keys extracted')
    return keys, problems


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--strict', action='store_true', help='fail for missing keys or invalid tables')
    args = parser.parse_args()
    keys, problems = kotlin_keys()
    merged = {'ES': {}, 'PT': {}}
    for name, module in load_modules():
        for language in merged:
            for key, value in getattr(module, language, {}).items():
                if key in merged[language] and merged[language][key] != value:
                    problems.append(f'error: [{language}] conflicting redefinition in {name}: {key!r}')
                merged[language][key] = value
    es_keys, pt_keys = set(merged['ES']), set(merged['PT'])
    for key in sorted(es_keys ^ pt_keys):
        problems.append(f'error: key present in only one language: {key!r}')
    out = {}
    for language in ('ES', 'PT'):
        table = merged[language]
        for key in sorted(keys - set(table)):
            problems.append(f'error: [{language}] missing Kotlin key: {key!r}')
        bad = set()
        for key, value in table.items():
            if not isinstance(value, str) or not value.strip():
                problems.append(f'error: [{language}] blank or non-string value: {key!r} -> {value!r}')
                bad.add(key)
            elif Counter(PLACEHOLDER.findall(key)) != Counter(PLACEHOLDER.findall(value)):
                problems.append(f'error: [{language}] placeholder mismatch: {key!r} -> {value!r}')
                bad.add(key)
        out[language.lower()] = {key: value for key, value in table.items() if key not in bad}
        print(f'{language}: {len(out[language.lower()])} entries, {len(keys & set(table))} used in Kotlin', file=sys.stderr)
    for problem in problems:
        print(problem, file=sys.stderr)
    if args.strict and problems:
        return 1
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as output:
        output.write(json.dumps(out, ensure_ascii=False, sort_keys=True, indent=1) + '\n')
    return 0


if __name__ == '__main__':
    sys.exit(main())
