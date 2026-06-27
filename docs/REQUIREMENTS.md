# 내 성경 읽기 — 크로스플랫폼 제품 요구사항

> **문서 목적**: Android 구현(`android/`)을 기준으로, iOS·Web·Desktop 등 다른 플랫폼에서 동일한 제품을 구현할 수 있도록 **플랫폼 독립적** 요구사항을 정의합니다.
>
> **앱 이름**: 내 성경 읽기 (Reading My Bible)
>
> **버전 기준**: Android 1.0.0

---

## 1. 제품 개요

### 1.1 목적

기존 **Lifove Bible**에서 사용하던 `.bdf`, `.lfa`, `.lfb` 형식의 성경 데이터 파일을 **이미 보유한 사용자**가 자신의 파일을 불러와 읽을 수 있게 하는 **독립적인 성경 리더**입니다.

### 1.2 핵심 원칙

| 원칙 | 설명 |
| --- | --- |
| Lifove Bible 미복제 | 원본 앱 UI/기능을 그대로 복제하지 않음 |
| 데이터 미포함 | 앱 번들에 성경 번역본 데이터를 포함하지 않음 |
| 사용자 데이터 소유 | 사용자가 선택한 폴더·파일만 읽음 |
| 점진적 확장 | MVP 이후 기능을 단계적으로 추가 (Android 1.0.0은 전체 MVP 완료 상태) |

### 1.3 앱 정보 필수 문구

설정 > 앱 정보 화면에 다음 의미를 반드시 표시:

```text
이 앱은 기존 Lifove Bible에서 사용되던 bdf/lfa 형식의 성경 데이터 파일을 보유한 사용자가
자신의 파일을 불러와 읽을 수 있도록 만든 독립적인 성경 읽기 앱입니다.
이 앱 자체는 성경 번역 데이터를 포함하지 않습니다.
```

---

## 2. 아키텍처 요구사항

### 2.1 권장 레이어

모든 플랫폼에서 다음 3계층 분리를 권장합니다.

```text
Presentation  →  UI, ViewModel/Presenter, 네비게이션
Domain        →  UseCase, Repository 인터페이스, 도메인 모델
Data          →  파일 파서, 캐시, 영구 저장소, 플랫폼 파일 접근
```

### 2.2 플랫폼별 분리 지점

| 계층 | 플랫폼 공통 | 플랫폼 종속 |
| --- | --- | --- |
| Domain | `BibleCatalog`, `BibleVerse`, UseCase 로직 | — |
| Data | BDF/LFA 파서, 검색 알고리즘, 캐시 스키마 | 파일 시스템 접근, SAF/DocumentPicker, 위젯 |
| Presentation | 화면 흐름, 상태 모델 | Compose/SwiftUI/Web UI |

### 2.3 핵심 Repository 인터페이스

```typescript
// 의사 코드 — 플랫폼 무관
interface BibleRepository {
  scanVersions(dataRoot: DataRoot): Promise<BibleVersion[]>
  readChapter(version: BibleVersion, book: BibleBook, chapter: number): Promise<BibleVerse[]>
}
```

추가 capability (Android 구현 기준):

- `warmUpVersion(version, onProgress)` — 역본 전체 장 캐시 예열
- `search(version, query, onProgress)` — 하이브리드 DB/파일 검색
- `diagnose(dataRoot)` — 파일 진단

---

## 3. 도메인 모델

### 3.1 BibleSourceType

| 값 | 설명 |
| --- | --- |
| `BdfSplit` | 7개 `.bdf` 분할 파일 |
| `LfaArchive` | 단일 `.lfa` ZIP 아카이브 |

### 3.2 BibleVersion

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `code` | string | 파일명 접두사 (예: `kornkrv`, `engNIV`) |
| `displayName` | string | 사용자 표시명 (코드→이름 매핑 적용) |
| `sourceType` | BibleSourceType | BDF 또는 LFA |
| `fileRoot` | path \| null | 로컬 디렉터리 경로 |
| `treeUri` | uri \| null | 플랫폼별 문서 트리 URI (Android SAF 등) |

### 3.3 BibleBook

66권 고정 카탈로그. `BibleCatalog` 객체에 정의.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `index` | int (0–65) | 0-based 책 인덱스 |
| `koreanName` | string | 한글 책 이름 |
| `englishName` | string | 영문 책 이름 |
| `chapterCount` | int | 해당 책의 장 수 |

**책 index → BDF 파일 index 매핑** (고정):

| bookIndex 범위 | fileIndex |
| --- | --- |
| 0 – 3 | 1 |
| 4 – 9 | 2 |
| 10 – 16 | 3 |
| 17 – 22 | 4 |
| 23 – 38 | 5 |
| 39 – 42 | 6 |
| 43 – 65 | 7 |

**책 번호 (파일 내부 식별자)**: `(bookIndex + 1)`을 2자리 zero-pad (예: index 0 → `"01"`, index 39 → `"40"`).

### 3.4 BibleVerse

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `versionCode` | string | 역본 코드 |
| `bookIndex` | int | 0-based |
| `chapter` | int | 1-based |
| `verse` | int | 1-based |
| `text` | string | 절 본문 |

**고유 키**: `{versionCode}:{bookIndex}:{chapter}:{verse}`

### 3.5 ReadingSelection

현재 읽기 위치: `version`, `book`, `chapter`.

### 3.6 ReadingProgress (영구 저장)

| 필드 | 기본값 | 설명 |
| --- | --- | --- |
| `versionCode` | null | 마지막 역본 |
| `bookIndex` | 0 | 마지막 책 |
| `chapter` | 1 | 마지막 장 |

### 3.7 VerseBookmark (구절 기록)

한 레코드에 북마크·하이라이트·메모·읽음 상태를 통합 관리.

| 필드 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `versionCode` | string | — | 역본 코드 |
| `bookIndex` | int | — | 책 index |
| `chapter` | int | — | 장 |
| `verse` | int | — | 절 |
| `text` | string | — | 저장 시점 본문 스냅샷 |
| `note` | string | `""` | 사용자 메모 |
| `isBookmarked` | bool | `true` | 북마크 여부 |
| `highlight` | VerseHighlight | `None` | 하이라이트 색 |
| `isRead` | bool | `false` | 읽음 체크 |
| `createdAtMillis` | long | — | 생성/갱신 시각 |

**표시 조건**: `isBookmarked || note != "" || highlight != None || isRead` 중 하나라도 true이면 기록 탭에 표시.

### 3.8 VerseHighlight

| 값 | 라벨 |
| --- | --- |
| `None` | 없음 |
| `Yellow` | 노랑 |
| `Mint` | 민트 |
| `Blue` | 파랑 |
| `Pink` | 분홍 |
| `Lavender` | 라벤더 |
| `Orange` | 주황 |

### 3.9 ReadingStyle

| 필드 | 범위/기본값 | 설명 |
| --- | --- | --- |
| `fontSizeSp` | 12 – 28, 기본 18 | 본문 글자 크기 |
| `lineHeightMultiplier` | 1.2 – 2.2, 기본 1.55 | 줄 간격 배율 |
| `palette` | `Paper` | 읽기 팔레트 (아래 참조) |
| `keepScreenOn` | false | 화면 꺼짐 방지 |
| `multitouchZoomEnabled` | true | 핀치 줌 |
| `boldTextEnabled` | false | 볼드체 |
| `showNotesInReader` | true | 메모 있는 절 아이콘 표시 |

### 3.10 ReadingPalette

| 값 | 라벨 | 용도 |
| --- | --- | --- |
| `Paper` | 종이 | 기본 밝은 테마 |
| `Evening` | 저녁 | 저조도 |
| `Oled` | OLED | 순수 검정 배경 |
| `HighContrast` | 고대비 | 접근성 |
| `WarmLight` | 따뜻한 빛 | 세피아 톤 |

팔레트는 **본문뿐 아니라 앱 전체 색상 scheme**에 적용되어야 함.

### 3.11 BibleSearchResult

| 필드 | 설명 |
| --- | --- |
| `verse` | BibleVerse |
| `book` | BibleBook |
| `snippet` | 결과 표시용 본문 (검색어 강조 전 원문) |

### 3.12 BibleFileDiagnostic

| 필드 | 설명 |
| --- | --- |
| `rootLabel` | 스캔 대상 경로 표시명 |
| `scannedFileCount` | 전체 파일 수 |
| `lfaCount`, `lfbCount`, `bdfFileCount` | 포맷별 개수 |
| `completeBdfVersionCount` | 7분할 완성 BDF 역본 수 |
| `mp3Count` | MP3 오디오 파일 수 |
| `unknownFileCount` | 미지원 확장자 수 |
| `versions` | 감지된 역본 목록 |
| `audioFiles` | MP3 파일 목록 |
| `issues` | 진단 이슈 목록 |

**BibleFileIssue**: `title`, `detail`, `severity` (`Info` | `Warning` | `Error`)

---

## 4. 데이터 파일 포맷

### 4.1 공통 규칙

- 성경 데이터 **루트 폴더** 하나에 모든 역본 파일을 둠
- 역본 코드는 **파일명 접두사** (대소문자 혼용 가능, 스캔 시 case-insensitive dedup)
- 앱은 `.bdf`, `.lfa`, `.lfb`(LFA 내부), `.mp3`(오디오)를 인식

### 4.2 BDF (7분할 텍스트)

**파일명**: `{code}{fileIndex}.bdf` (fileIndex = 1..7)

**역본 감지 조건**: `{code}1.bdf` ~ `{code}7.bdf` 7개 파일이 **모두** 존재.

**읽기 알고리즘**:

1. `bookIndex`로 `fileIndex` 결정 (섹션 3.3 매핑)
2. `{code}{fileIndex}.bdf` 파일 열기
3. `bookNumber = pad2(bookIndex + 1)`, `chapterNeedle = " {chapter}:"`
4. 각 줄에 대해:
   - 줄이 `bookNumber`로 시작하는지 확인
   - `chapterNeedle` 포함 여부 확인
   - `:` 이후 `{verseNumber} {text}` 파싱

**줄 형식 예**:

```text
01 1:1 태초에 하나님이 천지를 창조하시니라
01 1:2 땅이 혼돈하고...
```

**문자셋 fallback 순서**: `MS949` → `UTF-8` → `UTF-16` → `ISO-8859-7` (첫 성공 charset 사용)

### 4.3 LFA (ZIP 아카이브)

**파일명**: `{code}.lfa`

**역본 감지 조건**: `.lfa` 파일 존재 (BDF와 code 중복 시 case-insensitive dedup으로 하나만 표시)

**ZIP 내부 장 파일 (LFB)**: `{code}{bookNumber}_{chapter}.lfb`

예: `kornkrv01_1.lfb` = 개역개정 창세기 1장

**읽기 알고리즘**:

1. `{code}.lfa` ZIP 열기
2. entry `{code}{bookNumber}_{chapter}.lfb` 추출
3. 텍스트 디코딩 후 BDF와 동일한 `parseVerseLine` 적용

**문자셋 (LFA/LFB)**:

1. UTF-8 디코딩
2. `\uFFFD` (replacement char) 3개 초과 시 MS949 재시도

### 4.4 역본 코드 → 표시명 매핑

주요 매핑 (전체는 Android `BibleVersionNames` 참조):

| code | displayName |
| --- | --- |
| `kornkrv` | 개역개정 |
| `korhrv` | 개역한글 |
| `kornrsv` | 새번역 |
| `koreasy` | 쉬운성경 |
| `engNIV` / `engniv` | NIV |
| `engkjv` | KJV |
| `engNASB` / `engnasb` | NASB |
| `hymns` | 찬송가 |
| `versicles` | 교독문 |

매핑 없으면: `kor`/`eng` 접두사 제거 후 대문자 표시, 또는 원본 code 유지.

### 4.5 MP3 오디오 (선택)

- 데이터 폴더 내 `.mp3` 파일 감지
- 진단 화면에 목록 표시
- 탭 시 **외부 플레이어**로 재생 (앱 내장 플레이어 불필요)

---

## 5. 데이터 접근

### 5.1 데이터 루트 우선순위

1. 사용자가 선택한 **문서 트리/폴더** (Android: SAF URI)
2. 앱 내부 **다운로드 폴더** (GitHub Release `bible.zip` 적용 결과)
3. **기본 fallback**: `/sdcard/bible` (Android) — 다른 플랫폼은 OS별 적절한 기본 경로

### 5.2 데이터 다운로드 (선택 기능)

| 항목 | 값 |
| --- | --- |
| 소스 | GitHub Release asset `bible.zip` |
| 검증 | SHA-256 체크섬 일치 필수 |
| 설치 | zip 해제 → `{appData}/downloaded-bible/bible/` (또는 zip 루트) |
| 실패 시 | 이전 데이터 루트 유지, 오류 메시지 표시 |

### 5.3 폴더 변경 UX

- 폴더 선택 후 **읽기 탭으로 자동 전환**
- 폴더 변경 시 역본/장 캐시 무효화 및 재스캔
- 선택 URI/경로 영구 저장

---

## 6. 기능 요구사항

### 6.1 네비게이션 (4탭)

| 탭 | ID | 핵심 역할 |
| --- | --- | --- |
| 읽기 | Reader | 본문 표시, 역본/책/장/절 선택 |
| 검색 | Search | 전체 성경 본문 검색 |
| 기록 | Records | 북마크·하이라이트·메모·읽음 목록 |
| 설정 | Settings | 데이터 폴더, 읽기 스타일, 앱 정보 |

### 6.2 읽기 (Reader)

#### 6.2.1 역본 선택

- Dialog 또는 Sheet로 역본 목록 표시
- **언어별 그룹** 정렬 (한국어 / 영어 / 기타)
- 표시명 중심, code는 보조 정보
- **비교 역본** 1개 추가 선택 가능 → 절 카드에 병렬 표시
- 비교 역본 본문 앞에 역본 ID 접두사 **불필요** (상단 칩으로 표시)

#### 6.2.2 책/장/절 선택

- **책/장 Dialog**: 좌측 66권 목록 + 우측 선택 책의 장 목록 (2단)
- 각 패널 독립 세로 스크롤
- 구약/신약 segmented 필터 (선택)
- **절 선택 Dialog**: 해당 장 절 번호 목록

#### 6.2.3 본문 표시

- 절 단위 카드: **절 번호**(본문보다 크게) + 본문
- 하이라이트된 절: 팔레트별 배경색
- 메모 있는 절: 아이콘 (`showNotesInReader` on 시)
- 읽음 체크된 절: 시각적 표시

#### 6.2.4 장 이동

- 상단 **이전/다음 장** 버튼
- **좌우 스와이프** 장 이동
- 책 경계 자동 처리 (창세기 50 → 출애굽기 1)
- 장 이동 시 절 선택 초기화, **1절로 스크롤**
- 짧은 페이지 전환 애니메이션

#### 6.2.5 스크롤

- 우측 **드래그 스크롤바** (절 2개 이상일 때 항상 표시)
- 스크롤바 thumb는 터치 영역 충분히 확보

#### 6.2.6 절 상호작용

| 동작 | 결과 |
| --- | --- |
| 북마크 토글 | VerseBookmark 생성/해제 |
| 하이라이트 토글 | Yellow 기본; **길게 누르기**로 6색 선택 |
| 읽음 토글 | `isRead` 변경 |
| **길게 누르기** | 절 임시 선택 (테두리+배경) |
| 선택 상태에서 탭 | 선택 추가/해제 |
| 선택 후 복사 | `{book} {chapter}장\n{n}절 {text}\n...` |
| 선택 후 공유 | OS 공유 시트 |

**복사/공유 형식**:

```text
창세기 1장
1절 태초에 하나님이...
3절 하나님이 이르시되...
```

#### 6.2.7 글자 크기

- 설정 slider: 12 – 28sp
- **핀치 줌** (`multitouchZoomEnabled`): 동일 범위
- 줌 시 현재 크기 Toast/힌트 (연속 줌 시 갱신, `sp` 단위 미표시)
- 글자 크기에 따라 절 간격·카드 padding·아이콘 크기 **동적 축소**

#### 6.2.8 초기 로딩

단계별 로딩 메시지:

1. 데이터 폴더에서 BDF/LFA 역본 검색
2. 마지막 읽은 역본/책/장 복원
3. DB 캐시 예열 (진행률 배너)

#### 6.2.9 캐시 예열 UI

- 선택 역본 전체(1,189장) 백그라운드 예열
- 상단 배너: 진행률 + 현재 책/장
- 완료된 역본은 재실행 시 **건너뜀** (warmup key 기반)

### 6.3 검색 (Search)

#### 6.3.1 입력

- 최소 **2글자** 이상
- IME/search 키 = 검색 실행
- 역본 선택: Dialog (Reader 마지막 선택 역본과 **동기화**)

#### 6.3.2 검색 알고리즘 (하이브리드)

1. **DB 캐시 경로**: `chapters.search_text`에 SQLite `LIKE` (escape: `\`, `%`, `_`)
2. **미캐시 장**: 원본 파일 파싱 후 절 단위 `contains` (case-insensitive)
3. 결과 **성경 순서** 정렬 (bookIndex → chapter → verse)
4. **페이지네이션**: 100개 단위 (전체 개수 표시)
5. 검색어 본문 **굵게 강조**

#### 6.3.3 검색 UI

- 검색창 + 결과가 함께 스크롤
- 준비 중: 진행률 + 현재 검색 중인 책/장
- 결과 스크롤 중 상단에 현재 권/장 표시
- 우측 드래그 스크롤바; 드래그 중 **권/장 말풍선**
- 결과 **길게 누르기** → 읽기 탭 전환 + 해당 절 스크롤

### 6.4 기록 (Records)

- VerseBookmark 목록 (최신순)
- 각 카드: 위치, 본문 스냅샷, 북마크/하이라이트/읽음 상태, **메모 입력란**
- 메모 변경 즉시 저장

### 6.5 설정 (Settings)

| 섹션 | 항목 |
| --- | --- |
| 데이터 | 폴더 선택, GitHub 데이터 다운로드, 성경 파일 확인(진단) |
| 읽기 스타일 | 글자 크기, 줄 간격, 팔레트, 화면 유지, 핀치 줌, 볼드, 메모 아이콘 |
| 기록 | JSON 내보내기 / 가져오기 |
| 앱 정보 | 필수 문구, **앱 버전** |
| 온보딩 | 첫 실행 시 데이터 폴더 선택·포맷 안내 카드 |

### 6.6 홈 화면 위젯 (플랫폼 선택)

- 마지막 읽은 위치 또는 고정 구절 표시
- 탭 시 앱 실행 → 해당 위치

---

## 7. 영구 저장 (Persistence)

### 7.1 사용자 설정 (Key-Value)

SharedPreferences equivalent. Namespace: `reading_bible_preferences`.

| Key | Type | Default | 설명 |
| --- | --- | --- | --- |
| `last_version_code` | string? | null | ReadingProgress |
| `last_book_index` | int | 0 | ReadingProgress |
| `last_chapter` | int | 1 | ReadingProgress |
| `reading_font_size_sp` | float | 18 | 12–28 |
| `reading_line_height_multiplier` | float | 1.55 | 1.2–2.2 |
| `reading_palette` | string | `Paper` | ReadingPalette enum name |
| `reading_keep_screen_on` | bool | false | |
| `reading_multitouch_zoom_enabled` | bool | true | |
| `reading_bold_text_enabled` | bool | false | |
| `reading_show_notes_in_reader` | bool | true | |
| `verse_bookmarks` | string (JSON) | `[]` | VerseBookmark 배열 |
| `data_folder_tree_uri` | string? | null | SAF URI (Android) |
| `data_folder_local_root` | string? | null | 다운로드/로컬 경로 |

### 7.2 구절 기록 JSON 스키마

```json
[
  {
    "versionCode": "kornkrv",
    "bookIndex": 0,
    "chapter": 1,
    "verse": 1,
    "text": "태초에 하나님이...",
    "note": "",
    "isBookmarked": true,
    "highlight": "Yellow",
    "isRead": false,
    "createdAtMillis": 1715900000000
  }
]
```

- Import: 기존 기록과 **key 기준 merge**, `createdAtMillis` 내림차순
- Export: 전체 기록 JSON 문자열

### 7.3 장 캐시 DB (SQLite)

Database: `bible_chapter_cache.db`, version **5**.

#### `chapters`

| Column | Type | 설명 |
| --- | --- | --- |
| `cache_key` | TEXT PK | `{root}:{code}:{sourceType}:{stamp}:{bookIndex}:{chapter}` |
| `version_code` | TEXT | |
| `book_index` | INTEGER | |
| `chapter` | INTEGER | |
| `cached_at` | INTEGER | epoch ms |
| `verses_blob` | TEXT | 장 단위 인코딩 blob |
| `search_text` | TEXT | 소문자 normalize된 검색용 평문 |

**verses_blob 형식** (한 줄 = 한 절):

```text
{verseNumber}\t{Base64(UTF-8 text)}
```

**search_text**: 해당 장 모든 절 text를 `\n`으로 join.

#### `verses` (legacy fallback)

| Column | Type |
| --- | --- |
| `cache_key`, `verse` | PK |
| `text` | TEXT |

신규 write는 `chapters.verses_blob` 우선. 읽기 시 blob 없으면 `verses` fallback.

#### `versions`

| Column | Type |
| --- | --- |
| `scan_key`, `code`, `source_type` | PK |
| `display_name`, `position` | |

#### `warmups`

| Column | Type |
| --- | --- |
| `warmup_key` | TEXT PK |
| `completed_at` | INTEGER |

**warmup_key**: `{root}:{code}:{sourceType}:{versionFileStamps}:full`

**cache invalidation**: 원본 파일 `lastModified + length` stamp 변경 시 해당 장/역본 캐시 무효.

### 7.4 3단계 읽기 캐시

```text
1. In-memory (ConcurrentHashMap) — 프로세스 내
2. SQLite persistent cache — 재실행 후
3. Original file parse — 캐시 miss
```

---

## 8. 성능 요구사항

| 항목 | 목표 |
| --- | --- |
| 앱 시작 | 마지막 장 즉시 표시, 전체 예열은 백그라운드 |
| 장 읽기 (캐시 hit) | 체감 즉시 |
| 전체 예열 | 1,189장 bulk transaction, BDF 7파일/LFA 1 ZIP 각 1회만 읽기 |
| 검색 (캐시 complete) | DB LIKE 우선, 절 단위 필터 |
| 검색 (partial cache) | 하이브리드 — 캐시 장 + 파일 장 병합 |
| 동시 write | 프로세스 공용 lock으로 SQLite 충돌 방지 |

---

## 9. 오류 처리

| 상황 | 동작 |
| --- | --- |
| 손상된 LFA/ZIP | 빈 결과 반환, 앱 크래시 금지 |
| BDF 7분할 미완 | 해당 역본 스캔 제외, 진단 Warning |
| 장 entry 없음 | Reader에 "파일 형식 또는 손상 확인" 메시지 |
| 캐시 write 실패 | 본문 읽기는 계속 (캐시 실패 ≠ 읽기 실패) |
| 중복 절 번호 | CONFLICT_REPLACE로 저장 |
| 다운로드 checksum 불일치 | 설치 중단, 오류 표시 |

---

## 10. UI/UX 공통 요구사항

- **Material 3** 또는 플랫폼 네이티브 equivalent 디자인 시스템
- **Floating bottom tab**: pill 형태, overlay 배치, 하단 fade gradient
- 선택 탭: pill 배경 + semi-bold 라벨
- 리스트 하단 padding: floating tab에 가려지지 않도록
- Dialog 등장: fade + scale transition
- 다크/팔레트: ReadingPalette가 앱 전체에 적용

---

## 11. 비기능 요구사항

| 항목 | 요구 |
| --- | --- |
| 오프라인 | 모든 핵심 기능 오프라인 동작 (다운로드 제외) |
| 프라이버시 | 사용자 데이터 외부 전송 없음 |
| 접근성 | 고대비 팔레트, 글자 크기 조절 |
| 국제화 | UI 한국어 기본; 책명 한/영 catalog 내장 |
| 라이선스 | 앱에 성경 번역본 데이터 미포함 — 사용자 책임 |

---

## 12. Android / iOS 구현 매핑 (참고)

| 요구사항 영역 | Android | iOS |
| --- | --- | --- |
| Repository | `data/repository/FileBibleRepository.kt` | `Data/Repositories/FileBibleRepository.swift` |
| BDF Parser | `data/parser/BdfBibleFileParser.kt` | `Data/Parsers/BdfBibleFileParser.swift` |
| LFA Parser | `data/parser/LfaBibleFileParser.kt` | `Data/Parsers/LfaBibleFileParser.swift` |
| Cache | `data/cache/BibleChapterCache.kt` | `Data/Cache/BibleChapterCache.swift` |
| Catalog | `domain/model/BibleCatalog.kt` | `Domain/Models/BibleCatalog.swift` |
| Reader UI | `feature/reader/ReaderRoute.kt` | `Presentation/Features/Reader/ReaderView.swift` |
| Search | `feature/search/SearchViewModel.kt` | `Presentation/Features/Search/SearchViewModel.swift` |
| Settings | `feature/settings/SettingsRoute.kt` | `Presentation/Features/Settings/SettingsView.swift` |

---

## 13. 다른 플랫폼 구현 체크리스트

### Phase 1 — Core

- [ ] `BibleCatalog` 66권 고정 데이터
- [ ] BDF/LFA 파서 (문자셋 fallback 포함)
- [ ] 역본 스캔 (BDF 7분할 완성 검사)
- [ ] 장 읽기 + 3단계 캐시
- [ ] 읽기 UI (역본/책/장 선택, 본문, 장 이동)

### Phase 2 — User Data

- [ ] ReadingProgress 저장/복원
- [ ] VerseBookmark CRUD + JSON import/export
- [ ] ReadingStyle 설정

### Phase 3 — Search & Diagnostics

- [ ] 하이브리드 검색 + search_text 인덱스
- [ ] 파일 진단 화면
- [ ] 캐시 warm-up

### Phase 4 — Polish

- [ ] 다중 역본 비교
- [ ] 핀치 줌, 스와이프, 스크롤바
- [ ] 데이터 다운로드 (선택)
- [ ] 홈 위젯 (플랫폼 지원 시)

---

## 14. 변경 이력

| 날짜 | 변경 |
| --- | --- |
| 2026-06-27 | 워크스pace 재구조화에 따른 초版 작성 (Android 1.0.0 기준) |
