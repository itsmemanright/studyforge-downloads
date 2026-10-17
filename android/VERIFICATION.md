# a1 코드 17 검증 범위

기준일: 2026-10-17. 이 문서는 자동 검사·관측 결과와 미확인 영역을 구분합니다.

- Java unit tests: 79 passed; UI/JavaScript tests: 39 passed. Failures: 0.
- Release Lint: 0 errors. Release APK and AAB build completed.
- APK signature and AAB JAR signature verified; existing upload certificate retained.
- bundletool validate passed; applicationId `com.studyforge.app`, versionCode `17`, versionName `a1`, target API 36, minimum API 26, release non-debuggable.
- Every packaged asset and every DEX file matched between the APK, AAB and the reviewed source.
- Code 17 no-login demo tests cover answer/explanation/uncertain/range statistics, no writes to real workspace, no native authentication/AI commands, native refresh isolation, workspace restoration, and refusal to enter demo during real jobs.
- Mobile browser preview verified demo entry, question answering and range statistics. At the checked narrow viewport document width equaled viewport width after fixing study control overflow. This is not a physical Android device test.
- Google Play accepted code 17 and it was published to the existing internal-testing track. This does not mean production approval or policy-review approval.

## Prior live observation

Code 15 on Android 16 emulator used a locally authored 12-page PDF, prepared evidence and the selected GPT-6.1-Sol. Five questions plus independent review took 116.617 seconds in one observed run; 25 option explanations and 19 question quotation occurrences were retained, quotations matched the PDF, and 32 prepared-summary quotations matched. This is a prior single-run observation, not a speed or quality guarantee for code 17, other models or hundreds-page books.

## Limits

- Code 17 was not rerun end-to-end on a physical phone or emulator.
- Whole-book 500-page automated tests use simulated model responses; they do not establish real textbook reading accuracy or live timing.
- Live subscription feature generation, every Clova shared-link permission case, every customization and all device/browser combinations are not fully covered.
- Demo is fixed sample data, not real AI output. It does not provide reviewers with live AI account access.
- No Gemini adapter or full browser-only implementation is included.

Public source preparation preserves code/tests/build settings from the original ZIP. Only public documentation and ignore rules are updated. Binary verification and original-source SHA-256 values are in `../source/a1-code17-verification.json` and `../source/PUBLIC_SOURCE_MANIFEST.json`.
