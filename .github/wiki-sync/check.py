"""Claude 가 고친 .ai 작업 트리를 push 전에 기계적으로 검사한다(SCRUM-588).

- 자동 반영 허용 경로 밖(에이전트 지시문, 스킬, 결정 기록 등)을 바꿨으면 실패
- 대량 삭제(문서당 30줄, 전체 100줄 초과)면 실패
- 추가된 줄에 토큰처럼 보이는 문자열이 있으면 실패
- 가운뎃점, em 대시, 커밋 해시 도장은 경고만 남긴다
결과는 GITHUB_STEP_SUMMARY 에도 쓴다. 이 스크립트는 BackEnd 저장소 파일이라 Claude 단계가 고칠 수 없다.
"""

import os
import re
import subprocess
import sys

WIKI = ".ai"
ALLOWED = ("backend/", "runbooks/", "api-spec.md", "api-design.md", "taxonomy.md")
MAX_DELETED_PER_FILE = 30
MAX_DELETED_TOTAL = 100
SECRET = re.compile(r"sk-ant-|gh[pousr]_[A-Za-z0-9]{20,}|github_pat_|x-access-token|AKIA[0-9A-Z]{16}|BEGIN [A-Z ]*PRIVATE KEY")
HASH_STAMP = re.compile(r"`[0-9a-f]{7,8}`")


def git(*args: str) -> str:
    return subprocess.run(["git", "-C", WIKI, *args], check=True, capture_output=True, text=True).stdout


def main() -> int:
    git("add", "-A", "--intent-to-add")  # 새 파일도 diff 에 잡히게 한다(내용은 스테이징하지 않음)
    changed = [line for line in git("diff", "--name-only").splitlines() if line]
    errors, warnings = [], []

    outside = [p for p in changed if not p.startswith(ALLOWED)]
    if outside:
        errors.append("자동 반영 허용 경로 밖 변경: " + ", ".join(outside))

    total_deleted = 0
    for line in git("diff", "--numstat").splitlines():
        added, deleted, path = line.split("\t", 2)
        deleted_n = int(deleted) if deleted.isdigit() else 0
        total_deleted += deleted_n
        if deleted_n > MAX_DELETED_PER_FILE:
            errors.append(f"{path}: {deleted_n}줄 삭제(문서당 상한 {MAX_DELETED_PER_FILE})")
    if total_deleted > MAX_DELETED_TOTAL:
        errors.append(f"전체 {total_deleted}줄 삭제(상한 {MAX_DELETED_TOTAL})")

    for line in git("diff", "-U0").splitlines():
        if not line.startswith("+") or line.startswith("+++"):
            continue
        text = line[1:]
        if SECRET.search(text):
            errors.append("추가된 줄에 토큰처럼 보이는 문자열이 있음")
        if "·" in text or "—" in text:
            warnings.append(f"가운뎃점/em 대시: {text[:80]}")
        if HASH_STAMP.search(text):
            warnings.append(f"커밋 해시 표기: {text[:80]}")

    report = ["## 위키 변경 검사", "", f"바뀐 파일 {len(changed)}개, 삭제 {total_deleted}줄", ""]
    report += [f"- {p}" for p in changed]
    if errors:
        report += ["", "### 실패", *[f"- {e}" for e in sorted(set(errors))]]
    if warnings:
        report += ["", "### 경고", *[f"- {w}" for w in warnings[:20]]]
    text = "\n".join(report) + "\n"
    print(text)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as f:
            f.write(text)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
