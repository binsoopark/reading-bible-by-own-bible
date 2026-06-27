# Bible Workspace

Lifove Bible 호환 성경 데이터(`.bdf`, `.lfa`, `.lfb`)를 **사용자가 이미 보유한 파일**로 읽기 위한 멀티플랫폼 성경 리더 워크스페이스입니다.

## 이 프로젝트의 특성

### 무엇을 하는가

- **내 성경 읽기** (Reading My Bible) — Lifove Bible에서 쓰던 데이터 포맷을 읽는 **독립적인** 성경 리더
- 앱 번들에 **성경 번역본을 포함하지 않음** — 사용자가 선택·보유한 폴더의 파일만 읽음
- Lifove Bible 앱을 **복제하지 않음** — 현대적 UI와 아키텍처로 새로 구현

### 무엇을 읽을 수 있는가

| 포맷 | 설명 |
| --- | --- |
| `.bdf` | 7분할 텍스트 (`kornkrv1.bdf` ~ `kornkrv7.bdf`) |
| `.lfa` | ZIP 아카이브 (`kornkrv.lfa`, 내부 `.lfb` 장 파일) |
| `.lfb` | LFA ZIP 내부 또는 단독 장 파일 |

역본 코드 예: `kornkrv`(개역개정), `korhrv`(개역한글), `engNIV`, `engkjv` 등

### 누구를 위한 것인가

- 예전 **Lifove Bible**을 쓰며 `.bdf` / `.lfa` 데이터를 PC·기기에 보관해 둔 사용자
- 자신의 성경 데이터 **폴더를 직접 지정**해 읽고 싶은 사용자
- Lifove와 무관하게, **호환 포맷만** 맞으면 읽기를 원하는 사용자

### 데이터는 어디서 오는가

1. **권장**: 이미 보유한 bdf/lfa 폴더를 앱에서 선택
2. **보조**: 성경 데이터를 구할 수 없을 때만, 설정의 **성경 데이터 다운로드**(GitHub Release `bible.zip`, 1회 제한)

앱은 번역본 **배포·판매 앱이 아닙니다.** 데이터 출처와 이용 조건은 사용자 책임입니다.

## 폴더 구조

```text
bible/
├── docs/       크로스플랫폼 제품 요구사항 (다른 플랫폼 구현 시 기준 문서)
├── android/    Android 앱 (Kotlin, Jetpack Compose, v1.1.1)
├── ios/        iOS 앱 (SwiftUI, MVVM, Clean Architecture, v1.1.0)
├── data/       개발·테스트용 샘플 성경 데이터 (로컬, git 제외)
└── reference/  Lifove Bible 6.2.8 역공학 참고 자료 (로컬, git 제외)
```

## 빠른 시작

| 목적 | 방법 |
| --- | --- |
| Android 앱 개발 | Android Studio에서 `android/` 열기 |
| iOS 앱 개발 | `cd ios && xcodegen generate && open ReadingMyBible.xcodeproj` |
| 요구사항·포맷 명세 | [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) |
| 테스트 데이터 | `data/bible/` 을 기기로 복사하거나 앱에서 폴더 선택 |
| Lifove 포맷·알고리즘 참고 | `reference/LifoveBibleReconstructed/docs/` |

## 앱 공통 기능

- 5탭: **읽기** · **검색** · **메모** · **개인메모** · **설정**
- 다중 역본 비교, 북마크·하이라이트·구절 메모·개인메모·읽음 체크
- 전체 성경 검색, SQLite 장 캐시, 읽기 팔레트·글자 크기
- 성경 파일 진단, 기록 JSON·개인메모 백업/복원
- (Android/iOS) 핀치 줌, 스와이프 장 이동

## 앱 정보

| 항목 | 값 |
| --- | --- |
| 한글 이름 | 내 성경 읽기 |
| 영문 이름 | Reading My Bible |
| 패키지 / Bundle ID | `com.soobinpark.appcraft.readingbible` |
| Android 버전 | 1.1.1 |
| iOS 버전 | 1.1.0 |

## 라이선스·주의

- 저장소의 `data/bible/` 및 다운로드 zip은 **개발·테스트 편의**용일 수 있습니다.
- 상용 배포 시 각 성경 **저작권·이용약관**을 반드시 확인하세요.
- `reference/`는 역공학 참고용이며, 원본 Lifove Bible과 동일 동작을 보장하지 않습니다.

## 관련 문서

- [크로스플랫폼 요구사항](docs/REQUIREMENTS.md)
- [Android README](android/README.md)
- [iOS README](ios/README.md)
