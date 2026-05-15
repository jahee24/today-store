# [2026-05-13] 로컬 커밋 의도치 않은 생성 및 취소

### 증상

`git pull --rebase origin develop` 실행 중 충돌 발생 후,
`git rebase --continue` 명령이 충돌 해결 내용을 새 커밋으로 생성함.
결과적으로 `origin/develop`보다 1 커밋 앞선 상태가 됨.

### 원인

Rebase 중 `--continue` 시 충돌 해결 내용이 자동으로 커밋됨.
`GIT_EDITOR="true"` 옵션으로 편집기 없이 기존 커밋 메시지를 재사용하게 하면
커밋이 즉시 확정되어버림.

### 해결

```bash
# 커밋 취소, 변경사항은 워킹 디렉터리에 보존
git reset HEAD~1
```

> **주의**: push 전에만 가능. 이미 push된 커밋은 `git revert` 사용.

---
> **참고**: Rebase 작업 중에는 항상 `git status`로 현재 상태를 확인하는 습관이 중요함.
