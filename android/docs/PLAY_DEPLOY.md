# Android Play Store 배포 (Fastlane + GitHub Actions)

## 개요

`v*` 태그를 push하면 GitHub Actions가 다음을 수행합니다.

1. 서명된 AAB/APK 빌드
2. Fastlane으로 **Google Play 공개 테스트(Open Testing, `beta` 트랙)** 업로드
3. GitHub Release에 AAB/APK 첨부

로컬에서 동일하게 실행하려면 `android/` 디렉터리에서:

```bash
export ANDROID_KEYSTORE_PATH=/path/to/upload.jks
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_ALIAS=...
export ANDROID_KEY_PASSWORD=...
export PLAY_STORE_JSON_KEY_PATH=/path/to/service-account.json

bundle install
bundle exec fastlane deploy_open_testing
```

## GitHub Secrets (필수)

| Secret | 설명 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 업로드 keystore `.jks` 파일을 base64 인코딩한 값 |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 비밀번호 |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key 비밀번호 |
| `PLAY_SERVICE_ACCOUNT_JSON` | Google Play API 서비스 계정 JSON **전체 내용** |

### 서비스 계정 설정

1. [Google Cloud Console](https://console.cloud.google.com/) → IAM → 서비스 계정 생성
2. **Google Play Android Developer API** 사용 설정
3. JSON 키 다운로드
4. [Play Console](https://play.google.com/console) → **사용자 및 권한** → 서비스 계정 초대
5. 권한: **릴리스 관리** (테스트 트랙 출시 포함), 앱 `com.soobinpark.appcraft.readingbible` 접근
6. JSON 전체를 GitHub Secret `PLAY_SERVICE_ACCOUNT_JSON`에 저장

## 릴리스 노트 (changelog)

버전 코드별 파일:

```text
android/fastlane/metadata/android/ko-KR/changelogs/{versionCode}.txt
```

예: `10003.txt` → `app/build.gradle`의 `versionCode`와 일치해야 합니다.

## 트랙

| Fastlane `track` | Play Console |
| --- | --- |
| `beta` | 공개 테스트 (Open testing) |
| `internal` | 내부 테스트 |
| `production` | 프로덕션 |

현재 CI는 **공개 테스트(`beta`)** 로 배포합니다.

## 태그로 배포

```bash
git tag -a v1.2.0 -m "Reading My Bible 1.2.0"
git push origin v1.2.0
```

또는 GitHub Actions → **Android Release** → **Run workflow** (`deploy_to_play=true`).

---

## 현재 상태 (2026-06-28)

배포는 **업로드 키 재설정 승인 대기** 중이며, Play 업로드는 보류합니다.

### 준비 완료

| 항목 | 상태 |
| --- | --- |
| GitHub Secrets (`ANDROID_*`, `PLAY_SERVICE_ACCOUNT_JSON`) | 등록됨 |
| Fastlane + GitHub Actions (`android-release.yml`) | 설정됨 (CI bundle 이슈 수정 완료) |
| Keystore alias `bible` | 생성됨 |
| 업로드 키 재설정 PEM | `android/upload_certificate.pem` |
| Notion 자격증명 기록 | [내 정보들 → Reading My Bible 배포 Secrets](https://app.notion.com/p/38cf2a4fe89981e591cef9146ba2e6f1) |

### 대기 중

~~Play Console → **설정** → **앱 무결성** → **업로드 키 재설정**에 `upload_certificate.pem` 제출 후 Google 승인.~~

**2026-06-29**: 업로드 키 재등록 완료. Google 측 **활성화 전파 대기** 중.

최근 CI 오류:
```text
The upload certificate this APK is signed with is not yet valid because it has been recently reset.
```

- 서명 키(`bible`) 자체는 올바름 (이전 "wrong key" 오류 해소됨)
- Google이 새 업로드 키를 활성화할 때까지 보통 **수 시간 ~ 48시간** 소요
- Play Console **앱 무결성**에서 상태 확인, 활성화 확인 이메일 대기

| | SHA1 |
| --- | --- |
| 새 키 (`bible`, PEM) | `31:35:0A:2E:AC:28:F7:7A:C4:E3:84:F0:4D:66:0F:23:4C:B9:2E:C5` |
| Play가 기대하는 기존 키 | `74:29:1C:9C:11:9E:A5:4B:48:0F:FC:57:A2:30:DB:0F:B0:A7:93:5E` |

### 배포 재개 시

1. Play Console에서 업로드 키 재설정 **완료** 확인
2. GitHub Actions **Android Release** 워크플로 실행 (`deploy_to_play=true`) 또는 `v1.2.0` 태그 재배포
3. Play **공개 테스트(beta)** 트랙에서 빌드 확인

### 로컬 자격증명 (참고)

- Keystore: `/Users/soobinpark/Workspaces/AndroidWorkspace/TrendCheckApp/keystore/google_play_upload.jks`
- Alias: `bible` / Key password: Notion 페이지 참고
- 서비스 계정 JSON: `/Users/soobinpark/Downloads/binsoopark-d862e568ea57.json`
