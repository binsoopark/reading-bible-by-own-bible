# 기여 가이드

Reading My Bible에 관심을 가져주셔서 감사합니다. 작은 버그 수정부터 문서와 접근성
개선까지 환영합니다.

## 시작하기

1. 저장소를 Fork하고 작업 브랜치를 만듭니다.
2. 관련 이슈가 있으면 연결하고, 큰 변경은 구현 전에 이슈에서 방향을 논의합니다.
3. Android와 iOS의 공통 동작을 변경할 때는 가능한 한 두 플랫폼을 함께 반영합니다.
4. 변경 범위에 맞는 빌드와 테스트를 수행합니다.
5. Pull Request 템플릿의 확인 항목을 작성합니다.

## 로컬 검증

Android:

```bash
cd android
./gradlew --no-daemon assembleDebug
```

iOS:

```bash
cd ios
xcodebuild -project ReadingMyBible.xcodeproj \
  -scheme ReadingMyBible \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build
```

iOS 기기 실행이나 배포 서명이 필요하면 Xcode에서 자신의 Team을 선택하세요. 배포용
Bundle ID와 인증서는 각 기여자가 소유한 값으로 교체해야 합니다.

## 데이터와 비밀정보

- 성경 본문, 찬송가, 교독문, 원본 APK 및 역공학 산출물을 커밋하거나 첨부하지 마세요.
- 테스트에는 직접 작성한 짧은 가상 데이터나 재배포가 명확히 허용된 자료만 사용하세요.
- keystore, 인증서 개인키, Provisioning Profile, API 키, 서비스 계정 JSON,
  `local.properties` 및 개인 경로를 커밋하지 마세요.
- 실제 사용자 메모·북마크·폴더 경로가 포함된 로그나 백업 파일을 첨부하지 마세요.

## 코드와 커밋

- 기존 Kotlin/Swift 스타일과 Android·iOS의 대칭 구조를 유지합니다.
- 사용자 데이터 저장 키나 포맷을 바꿀 때는 기존 버전에서의 마이그레이션을 포함합니다.
- 하나의 커밋에는 관련된 변경만 담고, 무엇을 바꿨는지 알 수 있는 메시지를 사용합니다.
- 새 의존성은 유지보수 상태와 라이선스를 확인하고 추가 이유를 PR에 적습니다.

## 기여 라이선스

별도 합의가 없는 한 프로젝트에 제출한 기여는 루트 `LICENSE`의 Apache License 2.0
조건으로 제공됩니다. 앱 브랜딩과 성경 데이터는 `OPEN_SOURCE.md`의 별도 권리 범위를
따릅니다.
