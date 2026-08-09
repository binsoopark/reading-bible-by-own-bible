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

## 보안 원칙

- keystore, 비밀번호, 서비스 계정 JSON과 로컬 절대 경로는 문서나 저장소에 기록하지 않습니다.
- 자격증명은 GitHub Actions Secrets 또는 개인 비밀 관리 도구에만 보관합니다.
- 업로드 인증서 지문과 키 재설정 이력은 Play Console의 **앱 무결성** 화면에서 확인합니다.
- 외부 기여자는 자신의 패키지 이름과 서명키로 빌드해야 하며 프로젝트 배포 자격증명에 접근할 수 없습니다.
