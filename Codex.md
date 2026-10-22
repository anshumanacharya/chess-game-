# Development workflow

- Do all experimentation, implementation, use-case work, and module changes in the local checkout.
- GitHub remains the single source of truth. Keep local changes reviewable through pull requests.
- Each work session that produces changes should end with a pull request. A bare push does not satisfy session completion.
- Proactively prompt the user about preparing or opening the pull request when wrapping up a session or whenever necessary. This workflow records that expectation; it does not grant blanket permission to create a PR, merge, or deploy.

# Issue tracker

## Fixed: Board exceeds viewport height on narrow landscape screens

- **Status:** Fixed locally; PR and deployment verification pending (23 October 2026).
- **Scope:** Web responsive layout (`web/`)
- **Observed:** 23 October 2026, in the local browser build
- **Implementation:** Compact responsive layout on `ui/compact-responsive-game`; original reproduction measurements below are retained as history.

### Reproduction

1. Serve `web/` locally and open the app (tested at `http://127.0.0.1:8765/`).
2. Choose **Pass and play**, select **Unlimited time**, and start a game.
3. Set the browser viewport to **390 × 844 CSS pixels** (portrait).
4. Inspect the board with the side panel open, close the panel using the right-edge toggle, and reopen it.
5. Repeat at **740 × 360 CSS pixels** (narrow landscape).
6. With the panel hidden, check whether the entire board fits without scrolling and whether the toggle can reopen the panel.

### Measured observations

| Viewport | Board with panel open | Board with panel hidden |
| --- | --- | --- |
| 390 × 844 portrait | 327 × 327 px | 342 × 342 px |
| 740 × 360 landscape | 677 × 677 px | 677 × 677 px |

- The board remained square in both orientations.
- Portrait was width-limited. Closing the panel removed its content and vertical space; the larger board used the available width with the existing 24 px margins. Blank space below the square was expected.
- Landscape remained width-sized despite the much shorter viewport. The board extended below the visible area, requiring vertical scrolling to see it all.
- At 740 × 360 with the panel hidden, document height was **725 px**. The measured document client width was **725 px** with a vertical scrollbar, while the configured viewport width was 740 px.
- The toggle remained reachable in both states, and reopening the panel worked.
- These are browser viewport checks, not physical-device verification.

### Expected behavior and proposed sizing target

The board should remain square and maximize its size within **both available width and height**. With the panel hidden, the entire board should be visible without clipping or unnecessary page scrolling, and the reopen toggle should remain usable.

**Calculated target, not an observed result:** At a 740 × 360 viewport, retaining 24 px top and bottom margins leaves `360 - 2 × 24 = 312 px` of height. A board of approximately **312 × 312 px** could fit. The future implementation must account for actual layout constraints and toggle space rather than treating this calculation as a measured fixed requirement.

### Acceptance checks for the future fix

- [ ] At 390 × 844 portrait, test panel open → hidden → open; board remains square, uses available space, and does not clip horizontally.
- [ ] At 740 × 360 landscape, test both panel states; board sizing accounts for viewport height as well as width.
- [ ] With the panel hidden in landscape, all eight ranks and files fit on screen without vertical scrolling or clipping; no hidden-panel layout space remains.
- [ ] The toggle stays visible, reachable, and usable in both orientations and panel states, without covering playable squares.
- [ ] Reopening restores clocks, captured pieces, material advantage, move history, and controls without changing game state.
- [ ] Resize or rotate between portrait and landscape during a game; board stays square and adapts correctly.
- [ ] Check nearby narrow sizes and the desktop layout for regressions.

The original acceptance checklist above records the reported issue. The session below records the replacement layout and its verification; narrow screens now keep compact game information visible rather than offering a panel-hide mode.

# Session record

## 23 October 2026 (Asia/Calcutta)

- Cloned the GitHub repository into the local `chess-game-` checkout and served its checked-in `web/` build at `http://127.0.0.1:8765/` using a local static server.
- Checked unlimited-time local two-player setup, board and piece rendering, controls, turn rotation, and side-panel closing/reopening. Verified captured pieces for both sides and material scores through `1. e4 d5 2. exd5 Qxd5 3. Nc3 Qxd2 4. Kxd2`: White +1, equal material, Black +1, then White +8 (queen and pawn captured versus two pawns).
- Discovered the landscape overflow tracked above: portrait boards measured 327 × 327 px open / 342 × 342 px hidden at 390 × 844; landscape remained 677 × 677 px in either state at 740 × 360, with 725 px page height when hidden. The landscape issue remains open and is outside this UI change.
- Updated `web/style.css` to remove the contrasting colored rectangle behind captured icons, add subtle theme-aware silhouette outlines, and use the theme's main text color with tabular numerals for the material score. Kept piece artwork, counts, scoring, and turn behavior intact.
- Repeated fresh, live dark-mode browser capture checks at 1280 × 800, 390 × 844, and 740 × 360. Captures, equal/nonzero scores, icon loading, transparent backgrounds, turn rotation, and panel close/reopen preservation passed. Narrow portrait stacks controls below the board; the known landscape overflow persists.
- Checked light-mode styling using a snapshot of an actual captured game with the light palette applied. This was a visual snapshot check, not a live light-mode gameplay test or physical-device test.
- Kept browser checks and screenshots local and outside version control; test scripts, snapshots, and screenshots are deliberately excluded from the PR. Final validation includes `git diff --check` and checking the changed-file list.
- Documentation PR #7 tracks the initial workflow and landscape issue. The separate UI PR includes the CSS implementation and this updated session record; PR #7 is preserved without merging or closing it.

## 23 October 2026: Compact responsive game layout

- At widths up to 820 px, removed the right-edge panel toggle and replaced the stacked desktop panel with two compact clock/capture blocks, a two-column grid containing all four game controls, and an expandable Moves section below the board. Clocks face the screen reader in this compact layout; board pieces retain pass-and-play turn rotation. Wide screens retain the side panel, hide/show toggle, and always-visible move history.
- The square board now uses the smaller of available content width and viewport height minus 24 px. Short landscape fits the whole board; secondary information and controls scroll below it rather than forcing an oversized board or clipping it. Narrow layout restores its information even when the desktop panel was previously hidden.
- Local geometry checks: 1280 × 800 → 752 × 752 board; 390 × 844 → 366 × 366; 740 × 360 → 336 × 336; 820 × 500 → 476 × 476; 821 × 500 → 452 × 452 (desktop layout); 320 × 568 → 296 × 296. No horizontal page overflow was observed. Landscape controls intentionally require scrolling below the height-limited board.
- Fresh interactive dark-mode capture sequences at 1280 × 800, 390 × 844, and 740 × 360 verified White +1, equal material, Black +1, White +8, both sides' captured artwork, unchanged transparent backgrounds, and turn rotation. Verified narrow toggle absence, Moves expand/collapse and persistence across a move, desktop panel close/reopen, and hidden desktop panel restoration after narrowing. Bot setup and an e4/e5 bot reply worked; Takeback remains disabled against the bot.
- Light-mode interactive testing remains limited by the previously denied temporary browser origin; no security workaround was attempted. Existing theme tokens and the PR #8 captured-piece outline/score styles are preserved. This session does not claim live light-mode gameplay verification.
- `node --check web/app.js` and `git diff --check` pass. Browser evidence and test artifacts remain outside the repository. PR #7 remains untouched. User authorized PR, normal merge after CI, Pages deployment, and public browser verification for this change; final results will be reported with the PR.
