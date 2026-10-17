# 스터디포지 Android 원본 소스 · a1 / 코드 17

교재·수업 자료·문제집·족보·녹취로 개념 정리와 연습문제를 만들고, 풀이·오답·범위별 현황을 관리하는 Android 시제품입니다. 현재 구현은 ChatGPT 연결을 사용합니다. Gemini 연결과 완전한 웹앱 이식은 구현되어 있지 않습니다.

이 폴더의 실행 코드·테스트·빌드 설정은 a1 코드 17 원본과 같습니다. README, 검증 설명, 제외 규칙만 공개 배포용으로 정리했습니다. 문서 정리 이전의 정확한 소스 스냅샷은 [원본 ZIP](../source/StudyForge-a1-code17-source.zip)에 있습니다.

## 사용 흐름

1. 앱을 설치합니다. 로그인 없이 예시 5문제의 풀이·해설·오답·통계를 체험할 수 있습니다.
2. 교재 PDF, PDF로 변환한 PPT, 사진·TXT·문제집·족보, 클로바노트 전사본 공유 링크, 교수 강조·미강조 내용과 시험 범위를 추가합니다.
3. 실제 AI 작업은 본인 ChatGPT를 연결하고 서버가 제공하는 모델을 선택해 실행합니다. **자료 분석 시작 → 분석 결과 저장 → 사용자가 별도로 문제 만들기** 순서입니다. 연결 권한·사용량은 서비스가 실제로 허용하는 범위를 따릅니다.
4. 근거와 보기별 해설을 보며 풀고, 누적 오답·헷갈림·범위별 통계로 복습합니다.

## 포함 기능

- 전체 PDF·긴 TXT 자동 분할, 최대 3개 구간의 제한된 동시 분석, 진행률·중단·저장 구간부터 재개.
- PDF·UTF-8 TXT 파일당 최대 512MB, 사진 파일당 20MB. PPTX 직접 읽기나 녹음 파일의 직접 음성 인식은 포함하지 않습니다.
- 선택한 모델로 문항 생성 후 독립된 2차 검수. 문항·정답·보기별 해설·범위·근거 인용을 최종 검사하고 통과한 결과만 저장합니다.
- 원문 반영과 족보 추론·재구성·개념 확장을 구분합니다. 추론은 원래 기출의 확정 복원이라고 표시하지 않습니다.
- 교수 학습 지시, 원본 근거 페이지 보기, 안정적인 문항 ID와 이전 판 기록, 범위별 풀이·오답률.
- 누적 오답 재풀이, 헷갈림 모아보기, 확인 후 진행 초기화, HTML 학습실 내보내기·JSON 백업.
- 화면 설정 제안·미리보기·적용·되돌리기 및 격리된 개인 기능 코드 생성·검수·실행.
- 기기 내 자료·기록 암호화. 개발자 서버의 자동 동기화는 포함하지 않습니다.

## 화면만 실행해 보기

Node.js와 npm이 설치된 환경에서 이 `android` 폴더를 기준으로 실행합니다.

```sh
npm ci
npm test
npm run preview
```

브라우저에서 `http://127.0.0.1:8772`를 엽니다. **로그인 없이 체험하기**로 풀이 화면을 확인할 수 있습니다. 이 미리보기는 실제 로그인·AI 분석·Android 파일 선택 기능을 제공하지 않습니다. `index.html`만 웹서버에 올린다고 전체 앱 기능이 작동하는 구조가 아닙니다.

## Android 개발 빌드

JDK 17, Android SDK 36과 해당 SDK 구성요소를 설치합니다. `ANDROID_HOME`으로 SDK 위치를 지정하거나 개인 환경에 맞는 `local.properties`를 작성합니다. 개인 경로 파일은 커밋하지 않습니다.

Windows PowerShell:

```powershell
./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

macOS/Linux:

```sh
chmod +x gradlew
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

결과 APK: `app/build/outputs/apk/debug/app-debug.apk`. Gradle wrapper는 8.11.1, Android API 대상은 36, 최소 Android API는 26입니다. 앱 표시 버전은 `a1`, versionCode는 `17`, applicationId는 `com.studyforge.app`입니다.

자기 배포판을 만들 때는 applicationId·브랜드·개인정보처리방침·서명 설정을 자기 서비스에 맞게 바꾸고 콜백 연결도 함께 확인하세요. 원본 Play 업로드 키는 공개하지 않습니다. 공개 소스로 만든 개발용 APK는 기존 배포 APK와 서명이 다르므로 그 위에 바로 업데이트 설치할 수 있다고 가정하면 안 됩니다. 데이터 보존을 위해 기존 앱을 먼저 삭제하지 마세요.

`scripts/build-play-bundle.ps1`는 개인 서명 키를 외부에 보관해 release 빌드를 만드는 도구입니다. 원저자의 업로드 키·암호·개인 로컬 설정은 이 저장소에 포함되어 있지 않습니다. 기존 Play 앱 업데이트에는 기존 업로드 키가 필요합니다.

## 구조

| 역할 | 주요 파일 |
|---|---|
| 화면·풀이·통계·체험 | `app/src/main/assets/index.html`, `app.js`, `quiz.js`, `style.css` |
| Android 명령 라우팅 | `MainActivity.java` |
| 인증·모델 목록·AI HTTP/SSE | `ChatGptConnection.java`, `OAuthSecurity.java`, `ModelCatalog.java`, `Api.java`, `Sse.java` |
| 원본 가져오기·암호화 | `SourceImporter.java`, `DocumentLibrary.java`, `EncryptedBlocks.java`, `SecureVault.java` |
| 전체 분석·체크포인트 | `BatchPlan.java`, `AnalysisPipeline.java`, `AnalysisStatus.java`, `PreparedStudy.java` |
| 생성·독립 검수·최종 검증 | `StudyGenerator.java`, `GenerationReview.java`, `QuestionValidator.java`, `PastExpansion.java` |
| 기록·범위별 현황 | `StudyRecords.java`, `quiz.js` |
| 클로바 전사본 | `NoteImportActivity.java`, `NoteTranscript.java` |
| 화면 맞춤·개인 기능 | `UiConfig.java`, `FeatureBuilder.java`, `FeatureController.java`, `FeatureActivity.java`, `customize.js`, `features.js` |

Java 파일은 `app/src/main/java/app/studyforge/mobile/`에 있습니다. 기존 동작을 파악한 뒤 제공자별 인증·모델 호출을 분리하는 것이 Gemini 이식의 출발점입니다. [기능 이식 안내와 전달용 프롬프트](../GEMINI_HANDOFF.md)를 참고하세요.

## 확인한 범위

코드 17: Java 검사 79개 + UI 검사 39개, Release Lint 오류 0, APK/AAB 서명·번들·실행 자산 일치 검증 통과. Android 실시간 AI 실행은 이전 코드 15의 관측이며 코드 17에서 다시 측정한 결과는 아닙니다. 코드 17의 작은 화면 검수는 모바일 브라우저 미리보기로 진행했습니다. 자세한 내용은 [VERIFICATION.md](VERIFICATION.md)와 [검증 기록](../source/a1-code17-verification.json)을 참고하세요.

제공 자료에는 개인 교재·족보 원본·녹음·전사본·학습 기록·계정 세션·업로드 키가 없습니다. 예시는 직접 작성한 고정 테스트 데이터입니다. 개인정보 처리와 실제 배포 조건은 앱 소스와 [공개 방침](https://itsmemanright.github.io/studyforge-downloads/privacy.html)을 확인하세요.
