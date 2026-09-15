#!/usr/bin/env python3
"""
PR 변경분(diff) 커버리지 계산 + 게이트.

변경된 src/main 코틀린 파일의 '추가/수정된 라인' 중 JaCoCo 가 계측한 라인만 대상으로
covered/total 을 계산한다(주석·선언·공백 등 미계측 라인은 제외). 신규 코드가 테스트로
덮였는지를 PR 단위로 강제하기 위한 지표다.

usage: diff_coverage.py <base_ref> <jacoco_xml> <threshold> [out_json]
  base_ref  : 비교 기준 (예: origin/develop)
  jacoco_xml: jacocoTestReport.xml 경로
  threshold : 변경분 최소 커버리지 % (미달 시 exit 1)

출력: 사람이 읽는 요약(stdout) + out_json(JSON). 임계 미달이면 exit 1.
"""
import json
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

base_ref = sys.argv[1]
xml_path = sys.argv[2]
threshold = float(sys.argv[3])
out_json = sys.argv[4] if len(sys.argv) > 4 else None


def changed_lines_by_file(base):
    """git diff 로 변경 파일별 '새 파일 기준 추가/수정 라인번호 집합' 을 얻는다."""
    diff = subprocess.run(
        ["git", "diff", "--unified=0", "--no-color", f"{base}...HEAD", "--", "src/main"],
        capture_output=True, text=True, check=True,
    ).stdout
    files = {}
    cur = None
    hunk_re = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@")
    for line in diff.splitlines():
        if line.startswith("+++ "):
            p = line[4:].strip()
            cur = None if p == "/dev/null" else p[2:] if p.startswith("b/") else p
        elif line.startswith("@@") and cur:
            m = hunk_re.match(line)
            if not m:
                continue
            start = int(m.group(1))
            count = int(m.group(2)) if m.group(2) is not None else 1
            if count == 0:
                continue  # 순수 삭제 hunk — 새 라인 없음
            files.setdefault(cur, set()).update(range(start, start + count))
    return files


def jacoco_line_cov(xml):
    """{파일경로: {라인번호: covered(bool)}} — jacoco 계측 라인만."""
    root = ET.parse(xml).getroot()
    cov = {}
    for pkg in root.iter("package"):
        pkg_name = pkg.get("name")  # com/example/backend/...
        for sf in pkg.findall("sourcefile"):
            path = f"src/main/kotlin/{pkg_name}/{sf.get('name')}"
            lines = {}
            for ln in sf.findall("line"):
                lines[int(ln.get("nr"))] = int(ln.get("ci")) > 0
            cov[path] = lines
    return cov


def main():
    changed = changed_lines_by_file(base_ref)
    only_kt = {f: ls for f, ls in changed.items() if f.endswith(".kt")}
    cov = jacoco_line_cov(xml_path)

    total = covered = 0
    rows = []
    for path in sorted(only_kt):
        inst = cov.get(path, {})
        c = t = 0
        for nr in only_kt[path]:
            if nr in inst:  # jacoco 계측된 라인만 집계
                t += 1
                if inst[nr]:
                    c += 1
        if t == 0:
            continue
        total += t
        covered += c
        rows.append((path, c, t))

    pct = 100.0 * covered / total if total else 100.0
    result = {
        "pct": round(pct, 1),
        "covered": covered,
        "total": total,
        "threshold": threshold,
        "pass": total == 0 or pct >= threshold,
        "files": [{"path": p, "covered": c, "total": t, "pct": round(100.0 * c / t, 1)} for p, c, t in rows],
    }
    if out_json:
        with open(out_json, "w") as f:
            json.dump(result, f, ensure_ascii=False)

    print(f"변경분 커버리지: {pct:.1f}%  ({covered}/{total} 계측 라인), 임계 {threshold:.0f}%")
    for p, c, t in rows:
        print(f"  {100.0 * c / t:5.1f}%  {c}/{t}  {p}{'' if c == t else '  ⚠️'}")
    if total == 0:
        print("변경된 계측 라인 없음 — 게이트 통과(N/A).")
        return 0
    if pct < threshold:
        print(f"::error::변경분 커버리지 {pct:.1f}% < 임계 {threshold:.0f}%")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
