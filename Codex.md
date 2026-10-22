# Development workflow

- Do all experimentation, implementation, use-case work, and module changes in the local checkout.
- GitHub remains the single source of truth. Keep local changes reviewable through pull requests.
- Each work session that produces changes should end with a pull request. A bare push does not satisfy session completion.
- Proactively prompt the user about preparing or opening the pull request when wrapping up a session or whenever necessary. This workflow records that expectation; it does not grant blanket permission to create a PR, merge, or deploy.

# Issue tracker

## Open: Board exceeds viewport height on narrow landscape screens

- **Status:** Open
- **Scope:** Web responsive layout (`web/`)
- **Observed:** 23 October 2026, in the local browser build
- **Bug implementation / fix PR:** Pending; this entry documents testing only.

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

This entry tracks the issue only; it does not implement the fix.
