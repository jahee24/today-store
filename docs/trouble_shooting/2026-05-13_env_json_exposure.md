# [2026-05-13] env.json Git 커밋 노출

### 증상

팀원이 `env.json` (환경변수 파일)을 커밋에 포함하여 GitHub에 push함.
해당 파일에는 Naver Map Client ID/Secret이 포함되어 있었음.

### 해결 조치

1. **즉시**: 노출된 키 소유자(팀원)에게 알려 네이버 클라우드 콘솔에서 재발급 요청
2. **`.gitignore` 확인**: `env.json`이 이미 포함되어 있었으나 팀원이 무시하고 커밋

### 히스토리 완전 삭제 방법 (참고용)

```bash
# git filter-repo 설치
pip install git-filter-repo

# 모든 히스토리에서 파일 제거
git filter-repo --path frontend-app/frontend/env.json --invert-paths --force

# 강제 push (팀원 전원 재클론 필요)
git push origin develop --force-with-lease
```

> **중요**: force push 전 반드시 팀 전체에 공지. 팀원들은 `git clone` 새로 해야 함.
> 키 재발급이 1순위, 히스토리 삭제는 선택사항.

---
> **교훈**: 보안 민감 파일은 `.gitignore`에 등록하는 것뿐만 아니라, `git status`를 통해 의도치 않은 파일이 스테이징되지 않았는지 항상 확인해야 함.
