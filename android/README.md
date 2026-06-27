# 내 성경 읽기 — Android

**Reading My Bible** Android 앱입니다. Lifove Bible 호환 `.bdf` / `.lfa` / `.lfb` 파일을 사용자가 지정한 폴더에서 읽습니다.

## 프로젝트 성격

- **BYOD (Bring Your Own Data)**: 앱에 성경 본문이 없고, 사용자 데이터 폴더만 읽습니다.
- **Lifove 호환, Lifove 복제 아님**: 포맷·카탈로그는 Lifove 계열을 따르지만 UI·코드는 새로 작성했습니다.
- **오프라인 우선**: 읽기·검색·기록은 네트워크 없이 동작합니다 (선택적 데이터 다운로드 제외).

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| 언어 | Kotlin |
| UI | Jetpack Compose, Material 3 |
| 아키텍처 | MVVM + Clean Architecture |
| 캐시 | SQLite (`BibleChapterCache`) |
| 최소 SDK | 26 |

## Android Studio

모노레포에서 이 `android/` 폴더를 프로젝트 루트로 엽니다.

## 성경 데이터 준비

1. **권장**: Lifove Bible에서 쓰던 bdf/lfa가 들어 있는 폴더를  
   설정 → **bdf/lfa 폴더 선택** (SAF)
2. **보조**: 데이터가 전혀 없을 때만  
   설정 → **성경 데이터 다운로드** → 펼치기 → 다운로드 및 적용  
   (이미 폴더가 있거나 역본이 감지되면 다운로드 불가)

로컬 개발 시 워크스페이스 `data/bible/` 을 기기 `/sdcard/bible` 등으로 복사해도 됩니다.

## 주요 패키지

```text
com.soobinpark.appcraft.readingbible/
├── app/           AppViewModel, 탭 shell
├── feature/       reader, search, records, settings, library
├── domain/        model, repository, usecase
└── data/          parser, cache, preference, repository
```

## 빌드

```bash
./gradlew assembleDebug
```

Release: GitHub Actions + 서명 Secrets ([`../.github/workflows/android-release.yml`](../.github/workflows/android-release.yml))

## 문서

- [작업 계획 및 변경 이력](docs/PROJECT_PLAN.md)
- [크로스플랫폼 요구사항](../docs/REQUIREMENTS.md)
