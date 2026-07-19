# iOS App Store / TestFlight 배포

## 개요

Android [`PLAY_DEPLOY.md`](../android/docs/PLAY_DEPLOY.md)와 대칭으로, iOS 앱을 TestFlight → App Store에 올리기 위한 준비 문서입니다.

| 항목 | 값 |
| --- | --- |
| Bundle ID | `com.soobinpark.appcraft.readingbible` |
| Team ID | `3Z2KXXULNQ` |
| 현재 버전 | 1.2.1 (10004) |
| 개인정보처리방침 | https://binsoopark.github.io/reading-bible-by-own-bible/privacy-policy.html |

자격증명·API 키는 Notion **Reading My Bible — iOS 배포 Secrets** 페이지를 참고하세요.

---

## Phase 1 — Apple Developer / App Store Connect ✅

1. **Identifiers**: App ID `com.soobinpark.appcraft.readingbible`
2. **Certificates**: Apple Distribution
3. **Profiles**: App Store용 Provisioning Profile
4. **App Store Connect API Key** (.p8, Key ID, Issuer ID) → Notion 기록

---

## Phase 2 — 저장소 준비 ✅

| 항목 | 위치 |
| --- | --- |
| 버전 (1.2.1 / 10004) | `ios/project.yml` |
| Development Team | `DEVELOPMENT_TEAM: 3Z2KXXULNQ` |
| 공유 Scheme | `project.yml` → `schemes:` (xcodegen 생성) |
| App Icon | `ios/Resources/Assets.xcassets/AppIcon.appiconset/` |
| Privacy Manifest | `ios/PrivacyInfo.xcprivacy` |
| Export Compliance | `ITSAppUsesNonExemptEncryption: NO` |

프로젝트 재생성:

```bash
cd ios
xcodegen generate
open ReadingMyBible.xcodeproj
```

---

## Phase 3 — 수동 TestFlight (첫 업로드)

1. Xcode → **Any iOS Device (arm64)**
2. **Product → Archive**
3. **Distribute App → App Store Connect → Upload**
4. App Store Connect → **TestFlight**에서 빌드 처리 대기
5. 내부 테스트 그룹 추가 후 설치 확인

### App Store Connect 메타데이터

- 스크린샷 (6.7", 6.5" 등)
- 앱 설명, 키워드, 연령 등급
- **앱 개인정보 보호** 설문: 개인정보 수집 없음
- TestFlight "테스트할 내용": Android changelog와 동일 (`10004.txt` 참고)

---

## Phase 4 — 자동화 (예정)

GitHub Secrets (Notion 참고):

| Secret | 설명 |
| --- | --- |
| `APP_STORE_CONNECT_API_KEY` | .p8 파일 전체 |
| `APP_STORE_CONNECT_KEY_ID` | Key ID |
| `APP_STORE_CONNECT_ISSUER_ID` | Issuer ID |

추가 예정:

- `ios/fastlane/Fastfile` — `build_app` + `upload_to_testflight`
- `.github/workflows/ios-release.yml` — `macos-latest`, `v*` 태그

---

## 로컬 빌드 확인

```bash
cd ios
xcodegen generate
xcodebuild -scheme ReadingMyBible \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  build
```

Release Archive:

```bash
xcodebuild -scheme ReadingMyBible \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  archive -archivePath build/ReadingMyBible.xcarchive
```
