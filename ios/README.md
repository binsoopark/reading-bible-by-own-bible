# 내 성경 읽기 — iOS

**Reading My Bible** iOS 앱입니다. Android와 동일한 [크로스플랫폼 요구사항](../docs/REQUIREMENTS.md)을 따릅니다.

## 프로젝트 성격

- 사용자가 **직접 선택한 폴더** 또는 **1회성 앱 내 다운로드** 데이터만 읽습니다.
- 앱 번들에 성경 번역본을 포함하지 않습니다.
- SwiftUI + MVVM + Clean Architecture로 Android와 대칭 구조를 유지합니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| UI | SwiftUI |
| 최소 OS | iOS 17 |
| 캐시 | SQLite |
| ZIP (LFA) | ZIPFoundation |

## Xcode에서 열기

```bash
cd ios
xcodegen generate
open ReadingMyBible.xcodeproj
```

## 소스 구조

```text
Sources/
├── App/                 ReadingMyBibleApp, AppEnvironment
├── Domain/              Models, BibleRepository, UseCases
├── Data/                Parsers, Cache, Preferences, FileBibleRepository
└── Presentation/        Reader, Search, Records, Settings
```

## 성경 데이터 준비

1. **권장**: Files 앱 등으로 Mac의 `data/bible/` 을 기기에 두고  
   설정 → **성경 데이터 폴더 선택**
2. **보조**: 데이터가 없을 때만  
   설정 → **성경 데이터 다운로드** → 펼치기 → 다운로드 및 적용

## 빌드

```bash
xcodebuild -scheme ReadingMyBible -destination 'platform=iOS Simulator,name=iPhone 17' build
```

## Android와의 차이 (현재)

| 기능 | Android | iOS |
| --- | --- | --- |
| 핀치 줌 / 스와이프 장 이동 | ✅ | ✅ |
| 데이터 다운로드 | ✅ | ✅ |
| 홈 화면 위젯 | ✅ | — |

## 문서

- [워크스페이스 README](../README.md)
- [요구사항 명세](../docs/REQUIREMENTS.md)
