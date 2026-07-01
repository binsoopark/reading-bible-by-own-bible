# 문서

이 폴더는 **플랫폼에 독립적인 제품 요구사항**을 정의합니다. Android, iOS, Web, Desktop 등 다른 플랫폼에서 동일한 동작을 구현할 때 이 문서를 기준으로 삼습니다.

## 문서 목록

| 문서 | 설명 |
| --- | --- |
| [REQUIREMENTS.md](REQUIREMENTS.md) | 전체 제품 요구사항 (기능, 데이터 모델, 파일 포맷, 캐시, 검색, UI 동작) |
| [privacy-policy.html](privacy-policy.html) | 앱 개인정보처리방침 (GitHub Pages 공개 URL) |

## Android 구현 참고

현재 Android 구현은 `../android/`에 있습니다.

## iOS 구현 참고

iOS 구현은 `../ios/`에 있습니다. XcodeGen으로 프로젝트를 생성합니다.

```bash
cd ios && xcodegen generate && open ReadingMyBible.xcodeproj
```

## 참고 자료

Lifove Bible 6.2.8 역공학 자료는 `../reference/`에 있습니다.

- `reference/LifoveBibleReconstructed/docs/TECHNICAL_SPEC.md`
- `reference/LifoveBibleReconstructed/docs/ALGORITHMS_AND_DATA.md`
