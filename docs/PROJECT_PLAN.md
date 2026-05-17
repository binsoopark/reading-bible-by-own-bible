# 내가 가진 성경 읽기 프로젝트 계획

## 1. 프로젝트 목표

새 Android 앱 `내가 가진 성경 읽기`는 기존 Lifove Bible에서 사용하던 `.bdf`, `.lfa`, `.lfb` 성경 데이터 파일을 보유한 사용자가 자신의 파일을 불러와 읽을 수 있도록 만드는 독립적인 성경 리더입니다.

영문 앱 이름은 `Reading Bible by Own Bible`입니다.

패키지 이름:

```text
com.soobinpark.appcraft.readingbible
```

## 2. 핵심 방향

- 기존 Lifove Bible 앱을 복제하지 않는다.
- Lifove Bible 호환 데이터 파일을 읽는 현대적 Android 앱을 새로 만든다.
- 첫 MVP는 “보유한 성경 파일을 찾아 읽는 것”에 집중한다.
- 기능은 작은 단계로 확장한다.
- UI는 Jetpack Compose, Material 3, BottomTab 구조를 사용한다.
- 구조는 MVVM + Clean Architecture를 따른다.

## 3. 기술 스택

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- MVVM
- Clean Architecture
- Gradle Android Plugin
- 향후 확장 후보:
  - Room
  - DataStore
  - WorkManager
  - SQLite FTS

## 4. 앱 정보 문구

앱 정보 화면에는 다음 의미를 명확히 표시한다.

```text
이 앱은 기존 Lifove Bible에서 사용되던 bdf/lfa 형식의 성경 데이터 파일을 보유한 사용자가
자신의 파일을 불러와 읽을 수 있도록 만든 독립적인 성경 읽기 앱입니다.
이 앱 자체는 성경 번역 데이터를 포함하지 않습니다.
```

## 5. BottomTab 구성

| 탭 | MVP 포함 여부 | 역할 |
| --- | --- | --- |
| 읽기 | 포함 | 선택한 역본/책/장의 본문 표시 |
| 성경 | 포함 | 데이터 파일 스캔, 역본 선택, 책/장 선택 |
| 검색 | 껍데기부터 | 본문 검색. MVP 이후 구현 |
| 기록 | 껍데기부터 | 북마크, 밑줄, 메모, 읽음 체크 |
| 설정 | 포함 | 데이터 폴더, 앱 정보, 표시 설정 |

## 6. MVP 1단계 범위

- [x] 새 Android Studio 프로젝트 생성
- [x] 패키지명 적용
- [x] 한국어/영어 앱 이름 리소스 추가
- [x] Compose + Material 3 설정
- [x] BottomTab shell 구현
- [x] Clean Architecture 패키지 기본 구조 생성
- [x] 성경 데이터 파일 스캔 모델 작성
- [x] `.bdf` parser MVP 작성
- [x] `.lfa` zip parser MVP 작성
- [x] 기본 책/장 선택 상태 모델 작성
- [x] 읽기 화면에서 샘플 또는 실제 파일 본문 표시
- [x] 앱 정보 화면에 Lifove 호환 포맷 안내 표시
- [x] debug 빌드 성공 확인

## 7. MVP 2단계 후보

- [ ] 다중 역본 병렬 표시
- [x] 역본 선택 bottom sheet
- [ ] 책 선택 segmented UI
- [x] 장 선택 adaptive grid
- [ ] 절 선택 bottom sheet
- [x] 글자 크기 / 줄 간격 / 팔레트 설정
- [x] 최근 읽은 위치 저장

## 8. MVP 3단계 후보

- [x] `.bdf` 검색
- [x] `.lfa` 검색
- [x] 북마크
- [x] 밑줄 / 하이라이트
- [x] 메모
- [ ] 읽음 체크

## 9. MVP 4단계 후보

- [ ] 홈 화면 위젯
- [ ] 데이터 import/export
- [ ] FTS 검색 index
- [ ] MP3 파일 감지 및 재생
- [ ] 데이터 파일 진단 화면

## 10. 추천 추가 기능

- [ ] 데이터 파일 진단: 누락 파일, 깨진 zip, 알 수 없는 역본 코드, 인코딩 문제 표시
- [ ] 빠른 역본 비교
- [ ] 읽기 팔레트 preset: 종이, 저녁, OLED, 고대비, 따뜻한 빛
- [ ] 첫 실행 onboarding: 데이터 폴더 선택과 지원 포맷 안내
- [ ] 검색 index 생성

## 11. 작업 로그

### 2026-05-17

- [x] 프로젝트 방향 합의
- [x] 패키지명과 앱 이름 확정
- [x] MVP 중심 단계별 구현 전략 작성
- [x] 프로젝트 scaffold 생성
- [x] Compose + Material 3 + BottomTab shell 구현
- [x] Clean Architecture 기본 패키지 생성
- [x] Lifove 호환 `.bdf` / `.lfa` parser MVP 작성
- [x] 기본 `/sdcard/bible` 스캔 및 창세기 1장 읽기 흐름 구현
- [x] 앱 정보 화면에 Lifove 호환 포맷 안내 문구 추가
- [x] 첫 빌드 검증

빌드 결과:

```text
app/build/outputs/apk/debug/app-debug.apk
```

다음 작업 후보:

- [x] Android 13+ 저장소 접근 UX 설계: SAF 기반 폴더 선택
- [ ] 실제 데이터 파일 진단 화면 구현
- [ ] 역본 선택 bottom sheet 검색/정렬 개선
- [ ] 책/장 선택을 adaptive grid UX로 개선
- [ ] 다중 역본 병렬 표시 구현

### SAF 폴더 선택 단계

- [x] `ACTION_OPEN_DOCUMENT_TREE` 기반 성경 데이터 폴더 선택
- [x] 선택한 폴더 URI 영구 권한 저장
- [x] 선택한 URI를 SharedPreferences에 저장
- [x] `DocumentFile` 기반 `.bdf` / `.lfa` 스캔
- [x] `DocumentFile` 기반 `.bdf` parser
- [x] `DocumentFile` 기반 `.lfa` zip parser
- [x] 읽기 탭이 선택된 SAF 폴더를 우선 사용
- [x] 설정 탭에서 현재 데이터 폴더 상태 표시

### 2026-05-17 SAF 폴더 선택 구현

- [x] 설정 탭에 `bdf/lfa 폴더 선택` 버튼 추가
- [x] `OpenDocumentTree` launcher로 Android 13+ 호환 폴더 선택 구현
- [x] 선택 URI를 `DataFolderPreferences`에 저장
- [x] 앱 전역 `AppViewModel`에서 선택 URI 공유
- [x] `SafBibleFileScanner` 추가
- [x] `SafBdfBibleFileParser` 추가
- [x] `SafLfaBibleFileParser` 추가
- [x] `ReaderViewModel`이 SAF 폴더를 우선 스캔하고 없으면 `/sdcard/bible` fallback
- [x] `assembleDebug` 빌드 성공

### 하이라이트 MVP 단계

- [x] 하이라이트 MVP 작업 시작
- [x] 구절 기록 모델에 하이라이트 상태 추가
- [x] 저장된 북마크 JSON에 하이라이트 저장/복원
- [x] Reader 절 카드에 하이라이트 토글 추가
- [x] 기록 탭에 하이라이트 상태 표시
- [x] `assembleDebug` 빌드 검증

### 2026-05-17 하이라이트 MVP 구현

- [x] `VerseHighlight` 모델 추가
- [x] `VerseBookmark`에 `isBookmarked`, `highlight` 필드 추가
- [x] 기존 북마크 JSON은 `isBookmarked=true`, `highlight=None`으로 호환 처리
- [x] `AppViewModel.toggleHighlight` 추가
- [x] Reader 절 카드에 하이라이트 토글 버튼 추가
- [x] 하이라이트된 절은 팔레트별 강조 배경으로 표시
- [x] 기록 탭 북마크 카드에 하이라이트 상태 표시
- [x] `assembleDebug` 빌드 성공

### 메모 MVP 단계

- [x] 메모 MVP 작업 시작
- [x] 북마크 모델에 메모 필드 추가
- [x] 저장된 북마크 JSON에 메모 저장/복원
- [x] 기록 탭에서 메모 입력 UI 제공
- [x] 메모 저장 함수 전역 연결
- [x] `assembleDebug` 빌드 검증

### 2026-05-17 메모 MVP 구현

- [x] `VerseBookmark.note` 필드 추가
- [x] 기존 북마크 JSON에 `note`가 없어도 빈 메모로 읽히도록 처리
- [x] `BookmarkPreferences`가 메모를 저장/복원하도록 확장
- [x] `AppViewModel.updateBookmarkNote` 추가
- [x] 기록 탭 북마크 카드에 메모 입력란 추가
- [x] `assembleDebug` 빌드 성공

### 북마크 MVP 단계

- [x] 북마크 MVP 작업 시작
- [x] 북마크 도메인 모델 추가
- [x] 북마크 preference 저장소 추가
- [x] Reader 절 카드에 북마크 토글 추가
- [x] 기록 탭에 북마크 목록 표시
- [x] `assembleDebug` 빌드 검증

### 2026-05-17 북마크 MVP 구현

- [x] `VerseBookmark` 모델 추가
- [x] `BookmarkPreferences` 추가
- [x] 앱 전역 `AppViewModel`에서 북마크 목록 공유
- [x] Reader 절 카드에 북마크 추가/해제 버튼 추가
- [x] 기록 탭에 저장된 북마크 목록 표시
- [x] 북마크에 역본 코드, 책, 장, 절, 본문 스냅샷, 저장 시각 보존
- [x] `assembleDebug` 빌드 성공

### 본문 검색 MVP 단계

- [x] 본문 검색 MVP 작업 시작
- [x] 검색 결과 도메인 모델 추가
- [x] 현재 데이터 폴더의 역본 스캔 연결
- [x] 선택한 역본 전체 장 순회 검색 구현
- [x] 검색 탭 UI 구현
- [x] `.bdf` / `.lfa` 검색 경로 검증
- [x] `assembleDebug` 빌드 검증

### 2026-05-17 본문 검색 MVP 구현

- [x] `BibleSearchResult` 모델 추가
- [x] `SearchBibleUseCase` 추가
- [x] `SearchViewModel` 추가
- [x] 검색 탭에서 SAF 폴더 또는 기본 `/sdcard/bible` 역본 스캔
- [x] 선택한 역본의 전체 책/장을 순회해 검색
- [x] 검색 결과를 최대 100개까지 표시
- [x] 검색 결과에 책, 장, 절, 역본 코드 표시
- [x] `assembleDebug` 빌드 성공

### 읽기 스타일 설정 단계

- [x] 읽기 스타일 설정 작업 시작
- [x] 글자 크기 preference 모델 추가
- [x] 줄 간격 preference 모델 추가
- [x] 읽기 팔레트 preference 모델 추가
- [x] 설정 탭에 스타일 컨트롤 추가
- [x] Reader 본문에 스타일 반영
- [x] `assembleDebug` 빌드 검증

### 2026-05-17 읽기 스타일 설정 구현

- [x] `ReadingStyle`, `ReadingPalette` 모델 추가
- [x] `ReadingStylePreferences` 추가
- [x] 앱 전역 `AppViewModel`에서 읽기 스타일 상태 공유
- [x] 설정 탭에 글자 크기 slider 추가
- [x] 설정 탭에 줄 간격 slider 추가
- [x] 설정 탭에 종이/저녁/OLED/고대비/따뜻한 빛 팔레트 선택 추가
- [x] Reader 본문 카드에 팔레트, 글자 크기, 줄 간격 적용
- [x] `assembleDebug` 빌드 성공

### 최근 읽은 위치 저장 단계

- [x] 최근 읽은 위치 저장 작업 시작
- [x] 최근 읽은 역본 코드/책/장 preference 모델 추가
- [x] 앱 시작 시 저장된 위치 복원
- [x] 역본/책/장 변경 시 최근 위치 저장
- [x] `assembleDebug` 빌드 검증

### 2026-05-17 최근 읽은 위치 저장 구현

- [x] `ReadingProgress` 모델 추가
- [x] `ReadingProgressPreferences` 추가
- [x] Reader 초기 스캔 후 저장된 역본 코드/책/장을 복원
- [x] 역본 또는 책/장 변경 시 최근 위치 저장
- [x] 저장된 역본이 현재 폴더에 없으면 첫 번째 감지 역본으로 fallback
- [x] `assembleDebug` 빌드 성공

다음 작업 후보:

- [ ] 선택한 폴더의 파일 진단 UI 추가
- [ ] SAF `.lfa` parser의 charset 처리 개선
- [ ] 폴더 변경 후 Reader 탭으로 자동 이동 UX 검토
- [ ] 역본 선택 sheet에 검색과 정렬 추가

### 읽기 선택 UX 개선 단계

- [x] Reader 선택 UI 작업 시작
- [x] 역본 선택 sheet 검색 입력 추가
- [x] 역본 목록 정렬 개선
- [x] 책/장 선택 sheet를 adaptive grid 기반으로 개선
- [x] `assembleDebug` 빌드 검증

### 2026-05-17 읽기 선택 UX 개선 구현

- [x] 역본 선택 bottom sheet에 검색 필드 추가
- [x] 역본 목록을 포맷 유형과 코드 기준으로 정렬
- [x] 책 선택 후 장을 grid로 바로 선택하는 bottom sheet 구현
- [x] 장 grid가 화면 폭에 맞춰 adaptive column으로 표시되도록 개선
- [x] `assembleDebug` 빌드 성공

### 데이터 파일 진단 단계

- [x] 진단 화면 작업 시작
- [x] 일반 파일 폴더 진단 모델 추가
- [x] SAF 폴더 진단 모델 추가
- [x] `.bdf` 분할 파일 누락 여부 표시
- [x] `.lfa` / `.lfb` / 알 수 없는 파일 수 표시
- [x] 감지된 역본 목록 표시
- [x] 성경 탭에 진단 UI 연결
- [x] `assembleDebug` 빌드 검증

### 2026-05-17 데이터 파일 진단 구현

- [x] `BibleFileDiagnostic`, `BibleFileIssue` 모델 추가
- [x] `BibleFileDiagnosticsReader`로 일반 파일 폴더와 SAF 폴더 진단 통합
- [x] `.bdf` 1~7 분할 세트 완성 여부 검사
- [x] `.lfa`, `.lfb`, 알 수 없는 파일 수 집계
- [x] 성경 탭에 요약 카드, 감지 역본, 진단 이슈 목록 추가
- [x] `LibraryViewModel` 추가
- [x] `assembleDebug` 빌드 성공
