# 오늘의 가게 - 트러블슈팅 사례 모음

프로젝트 개발 과정에서 발생한 주요 오류와 해결 과정을 기록합니다. 각 문서는 개별 파일로 관리됩니다.

| 날짜 | 제목 | 관련 태그 |
|:---:|:---|:---|
| 2026-05-15 | [이메일 변경 후 JWT 무효화로 인한 세션 붕괴](./2026-05-15_email_change_jwt_invalidation.md) | `Bug`, `Auth`, `JWT` |
| 2026-05-15 | [모델 필드명 불일치로 인한 컴파일 에러](./2026-05-15_model_field_mismatch_compilation_error.md) | `Bug`, `Model`, `Flutter` |
| 2026-05-15 | [iOS/iPad 공유 위치(Origin) 누락 크래시](./2026-05-15_share_position_origin_missing_error.md) | `Bug`, `iOS`, `Share` |
| 2026-05-15 | [AI 이미지 8종 일괄 저장 기능 구현](./2026-05-15_bulk_image_save_implementation.md) | `Feature`, `Gal`, `Gallery` |
| 2026-05-15 | [프로필 수정 시 이름 필드 누락 오류](./2026-05-15_profile_update_validation_error.md) | `Bug`, `Validation`, `Auth` |
| 2026-05-15 | [채널별 독립 콘텐츠 관리 및 스마트 재생성](./2026-05-15_channel_specific_content_management.md) | `Refactor`, `State Management` |
| 2026-05-14 | [인증 토큰 갱신 레이스 컨디션](./2026-05-14_auth_token_refresh_race_condition.md) | `Auth`, `Network`, `Concurrency` |
| 2026-05-13 | [이미지 재생성 폴링 타임아웃](./2026-05-13_image_generation_polling_timeout.md) | `AI`, `Polling`, `UX` |
| 2026-05-13 | [Spring Boot Cold Start 401](./2026-05-13_spring_boot_cold_start_401.md) | `Backend`, `Cloud Run`, `Infra` |
| 2026-05-13 | [env.json Git 커밋 노출](./2026-05-13_env_json_exposure.md) | `Security`, `Git` |
| 2026-05-13 | [로컬 커밋 의도치 않은 생성 및 취소](./2026-05-13_git_rebase_local_commit.md) | `Git`, `Rebase` |
| 2026-05-13 | [이력 화면 캐시 및 Provider 오류](./2026-05-13_history_cache_and_provider_error.md) | `Riverpod`, `State Management` |

---
> **작성 가이드**: 새로운 트러블슈팅 사례가 발생하면 `YYYY-MM-DD_short_description.md` 형식으로 파일을 생성하고 위 표에 추가해 주세요.
