# 이미지 속 문자 제거

기본 내장 `image_gen` 편집 도구와 `imagegen` 스킬을 사용했습니다.
결과는 요청에 따라 다음 기존 파일에 덮어썼습니다.

- `resources/room3/images/desk.png`: 책 오른쪽 페이지의 필기와 붉은 봉인 도장의 이니셜 제거.
- `resources/room3/images/candle.png`: 받침 명판의 숫자 1~5 제거. 촛불 위치·높이와 투명 배경 유지.
- `resources/room3/images/book.png`: 확인 시 이미 문자 없는 삽화였으므로 보존.

퍼즐 정답·좌표·판정은 변경하지 않았습니다. 이미지 로더는 현재 폴더 구조에 맞춰 `/room3/images/`를 사용합니다.

## 사용 프롬프트

### 책상 1차

Use case: precise-object-edit. Edit target: gothic desk game background. Remove only the handwritten text and all letter-like marks on the open book's right page. Reconstruct natural blank aged parchment matching the page lighting and paper texture, preserve the red wax seal and bookmark ribbons. Remove any readable letters or numbers elsewhere if present. Keep the left-page illustration, all ornaments and symbolic drawings, the five candles, book positions, drawer handles, lantern, feather, potion shelf and its exact bottle positions/colors, desk geometry, gothic lighting, original wide composition and camera unchanged. No new text or numerals. This is an image backdrop for pixel-aligned click regions: preserve every object's position and silhouette and original 1670x941 wide framing. Output the complete edited backdrop.

### 책상 최종 보정

Use case: precise-object-edit. This gothic desk backdrop already has the handwritten text removed. Make one tiny final edit: remove the letter-like monogram/initials stamped into the round RED WAX SEAL at the lower right of the open book. Reconstruct a smooth plain red wax center, retaining the wax seal round shape, rim, lighting and two red bookmark ribbons. All book pages must remain free of handwriting and all letters, numerals, words and initials. Keep every other pixel composition invariant: exact object positions, candle heights, drawer handles, book illustration on left page, lantern, potion shelf arrangement, textures, wide 1670x941 framing. Do not add new text or objects. Output complete edited desk backdrop.

### 촛불

Use case: precise-object-edit. Edit target: existing five-candle puzzle asset. Remove ONLY the five black numerals 1, 2, 3, 4, 5 from the five brass plaques on the front of the stone pedestal. Leave all five plaques in place with natural uninterrupted aged brass texture, borders, rivets and lighting; no black patches or text. Keep the exact candle heights left to right: medium, shortest, slightly shorter than first, second tallest, tallest. Preserve all original candle and wick positions, all five candle-holder shapes and widths, the stone pedestal, the original 1774x887 wide canvas framing. All five wicks must remain UNLIT. No text, numbers or lettering anywhere. Preserve existing transparent and semitransparent background; do not add a solid background or cast shadows outside the existing composition. Do not crop or move any objects. Output full edited PNG with alpha.
