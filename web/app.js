(function () {
  "use strict";

  var ChessEngine = window["web-engine"];
  var JsGame = ChessEngine.JsGame;
  var JsClock = ChessEngine.JsClock;

  var PRESETS = [
    { label: "1 min", minutes: 1, increment: 0 },
    { label: "3 | 2", minutes: 3, increment: 2 },
    { label: "5 min", minutes: 5, increment: 0 },
    { label: "10 min", minutes: 10, increment: 0 },
    { label: "15 | 10", minutes: 15, increment: 10 },
    { label: "30 min", minutes: 30, increment: 0 }
  ];

  // Real piece artwork (the Cburnett set — pieces/README.md has the credit/license) instead of
  // Unicode glyphs: clearer at a glance, scales cleanly to any board size, and sidesteps the
  // iOS-only rendering bug the old glyph-based pieces needed a workaround for.
  var PIECE_TYPE_CODE = { king: "K", queen: "Q", rook: "R", bishop: "B", knight: "N", pawn: "P" };

  function pieceImageSrc(type, color) {
    return "pieces/" + (color === "white" ? "w" : "b") + PIECE_TYPE_CODE[type] + ".svg";
  }

  function pieceImage(type, color, className) {
    var img = document.createElement("img");
    img.className = className;
    img.src = pieceImageSrc(type, color);
    img.alt = color + " " + type;
    img.draggable = false;
    return img;
  }

  var PIECE_POINTS = { queen: 9, rook: 5, bishop: 3, knight: 3, pawn: 1 };

  function materialValue(capturedTypes) {
    return capturedTypes.reduce(function (sum, type) { return sum + (PIECE_POINTS[type] || 0); }, 0);
  }

  var GAME_OVER_TEXT = {
    checkmate: "Checkmate",
    stalemate: "Stalemate",
    draw_fifty_move: "Draw – 50-move rule",
    draw_repetition: "Draw – threefold repetition",
    draw_insufficient_material: "Draw – insufficient material",
    white_time_out: "White ran out of time",
    black_time_out: "Black ran out of time",
    white_resigned: "White resigned",
    black_resigned: "Black resigned",
    draw_agreed: "Draw agreed"
  };

  // ---- app state ----
  var screen = "setup"; // "setup" | "game"
  var game = null;
  var clock = null;
  var selected = null; // { file, rank }
  var pendingPromotion = null; // { fromFile, fromRank, toFile, toRank, options }
  var gameOverReason = null; // string | null
  var showResignConfirm = false;
  var showDrawOfferConfirm = false;
  var tickHandle = null;
  var lastTickAt = 0;
  var lastConfig = null;

  var setupState = {
    mode: "timed", // "timed" | "unlimited"
    presetIndex: 3,
    useCustom: false,
    customMinutes: 15,
    customIncrement: 0,
    opponent: "human", // "human" | "computer"
    humanPlaysWhite: true
  };

  var botMoveTimeout = null;
  var isBotThinking = false;
  var currentBotColor = null; // "white" | "black" | null

  var root = document.getElementById("app");

  function render() {
    root.innerHTML = "";
    if (screen === "setup") {
      root.appendChild(renderSetupScreen());
    } else {
      root.appendChild(renderGameScreen());
      // Every render rebuilds the list from scratch (scrolled to the top), so pin it to the
      // latest move — matching Android's MoveHistoryList auto-scroll.
      var movesList = root.querySelector(".moves-list");
      if (movesList) movesList.scrollTop = movesList.scrollHeight;
      if (pendingPromotion) root.appendChild(renderPromotionOverlay());
      else if (gameOverReason) root.appendChild(renderGameOverOverlay());
      else if (showResignConfirm) root.appendChild(renderResignOverlay());
      else if (showDrawOfferConfirm) root.appendChild(renderDrawOfferOverlay());
    }
  }

  // ---------------------------------------------------------------------
  // Setup screen
  // ---------------------------------------------------------------------

  function renderSetupScreen() {
    var screenEl = el("div", "setup-screen");
    var card = el("div", "setup-card");
    screenEl.appendChild(card);

    card.appendChild(el("div", "setup-title", "Local Chess"));
    card.appendChild(el("div", "setup-subtitle", "Two players, one screen – web test build"));

    card.appendChild(el("div", "section-header", "Opponent"));
    var opponentRow = el("div", "chip-row");
    opponentRow.appendChild(chip("Pass and play", setupState.opponent === "human", function () {
      setupState.opponent = "human";
      render();
    }));
    opponentRow.appendChild(chip("Computer (~1000 Elo)", setupState.opponent === "computer", function () {
      setupState.opponent = "computer";
      render();
    }));
    card.appendChild(opponentRow);

    if (setupState.opponent === "computer") {
      var colorRow = el("div", "chip-row");
      colorRow.appendChild(chip("Play as White", setupState.humanPlaysWhite, function () {
        setupState.humanPlaysWhite = true;
        render();
      }));
      colorRow.appendChild(chip("Play as Black", !setupState.humanPlaysWhite, function () {
        setupState.humanPlaysWhite = false;
        render();
      }));
      card.appendChild(colorRow);
    }

    card.appendChild(el("div", "section-header", "Chess clock"));
    var modeRow = el("div", "chip-row");
    modeRow.appendChild(chip("Timed", setupState.mode === "timed", function () {
      setupState.mode = "timed";
      render();
    }));
    modeRow.appendChild(chip("Unlimited time", setupState.mode === "unlimited", function () {
      setupState.mode = "unlimited";
      render();
    }));
    card.appendChild(modeRow);

    if (setupState.mode === "timed") {
      var presetGrid = el("div", "preset-grid");
      PRESETS.forEach(function (preset, index) {
        presetGrid.appendChild(chip(preset.label, !setupState.useCustom && setupState.presetIndex === index, function () {
          setupState.useCustom = false;
          setupState.presetIndex = index;
          render();
        }));
      });
      presetGrid.appendChild(chip("Custom", setupState.useCustom, function () {
        setupState.useCustom = true;
        render();
      }));
      card.appendChild(presetGrid);

      if (setupState.useCustom) {
        card.appendChild(stepper("Minutes per side", setupState.customMinutes, 1, 90, function (v) {
          setupState.customMinutes = v;
          render();
        }));
        card.appendChild(stepper("Increment (seconds)", setupState.customIncrement, 0, 60, function (v) {
          setupState.customIncrement = v;
          render();
        }));
      }
    }

    var startButton = el("button", "primary-button", "Start game");
    startButton.addEventListener("click", function () {
      var config;
      if (setupState.mode === "unlimited") {
        config = { minutes: 0, increment: 0, unlimited: true };
      } else if (setupState.useCustom) {
        config = { minutes: setupState.customMinutes, increment: setupState.customIncrement, unlimited: false };
      } else {
        var preset = PRESETS[setupState.presetIndex];
        config = { minutes: preset.minutes, increment: preset.increment, unlimited: false };
      }
      config.botColor = setupState.opponent === "computer"
        ? (setupState.humanPlaysWhite ? "black" : "white")
        : null;
      startGame(config);
    });
    card.appendChild(startButton);

    return screenEl;
  }

  function chip(label, selectedFlag, onClick) {
    var button = el("button", "chip" + (selectedFlag ? " selected" : ""), label);
    button.type = "button";
    button.addEventListener("click", onClick);
    return button;
  }

  function stepper(label, value, min, max, onChange) {
    var row = el("div", "stepper-row");
    row.appendChild(el("div", null, label));
    var controls = el("div", "stepper-controls");
    var minus = el("button", null, "−");
    minus.type = "button";
    minus.addEventListener("click", function () {
      if (value - 1 >= min) onChange(value - 1);
    });
    var valueEl = el("div", "stepper-value", String(value));
    var plus = el("button", null, "+");
    plus.type = "button";
    plus.addEventListener("click", function () {
      if (value + 1 <= max) onChange(value + 1);
    });
    controls.appendChild(minus);
    controls.appendChild(valueEl);
    controls.appendChild(plus);
    row.appendChild(controls);
    return row;
  }

  // ---------------------------------------------------------------------
  // Game lifecycle
  // ---------------------------------------------------------------------

  function startGame(config) {
    lastConfig = config;
    currentBotColor = config.botColor || null;
    game = new JsGame();
    game.setBot(currentBotColor);
    clock = new JsClock(config.minutes, config.increment, config.unlimited);
    clock.start("white");
    selected = null;
    pendingPromotion = null;
    gameOverReason = null;
    showResignConfirm = false;
    showDrawOfferConfirm = false;
    isBotThinking = false;
    moveLog = [];
    screen = "game";
    startTicker();
    render();
    maybeTriggerBotMove();
  }

  function rematch() {
    startGame(lastConfig);
  }

  function startTicker() {
    stopTicker();
    lastTickAt = performance.now();
    tickHandle = setInterval(function () {
      var now = performance.now();
      var elapsed = now - lastTickAt;
      lastTickAt = now;
      if (gameOverReason) {
        stopTicker();
        return;
      }
      clock.tick(elapsed);
      var flagged = clock.flaggedColor();
      if (flagged) {
        endGame(flagged === "white" ? "white_time_out" : "black_time_out");
        return;
      }
      updateClockDisplays();
    }, 100);
  }

  function stopTicker() {
    if (tickHandle !== null) {
      clearInterval(tickHandle);
      tickHandle = null;
    }
  }

  document.addEventListener("visibilitychange", function () {
    if (document.hidden) {
      stopTicker();
    } else if (screen === "game" && !gameOverReason && clock && !clock.isUnlimited() && clock.activeColor()) {
      startTicker();
    }
  });

  function onSquareTapped(file, rank) {
    if (gameOverReason || game.isBotTurn()) return;
    var piece = pieceAt(file, rank);
    var sideToMove = game.sideToMove();

    if (!selected) {
      if (piece && piece.pieceColor === sideToMove) {
        selected = { file: file, rank: rank };
        render();
      }
      return;
    }

    if (selected.file === file && selected.rank === rank) {
      selected = null;
      render();
      return;
    }

    var moves = game.legalMovesFrom(selected.file, selected.rank).filter(function (m) {
      return m.toFile === file && m.toRank === rank;
    });

    if (moves.length === 0) {
      selected = (piece && piece.pieceColor === sideToMove) ? { file: file, rank: rank } : null;
    } else if (moves.length > 1) {
      pendingPromotion = { fromFile: selected.file, fromRank: selected.rank, toFile: file, toRank: rank, options: moves };
    } else {
      applyMove(moves[0]);
    }
    render();
  }

  function applyMove(move) {
    var movedColor = game.sideToMove();
    game.applyMoveExact(move.fromFile, move.fromRank, move.toFile, move.toRank, move.promotion || null);
    selected = null;
    pendingPromotion = null;
    afterMoveApplied(movedColor, move);
  }

  /** Shared post-move bookkeeping (clock, move log, game-over/bot-turn check) for both a human
   *  move (applyMove) and a bot move (the maybeTriggerBotMove timeout below), so the two paths
   *  can't drift out of sync. */
  function afterMoveApplied(movedColor, move) {
    var nextColor = game.sideToMove();
    clock.onMoveCompleted(movedColor, nextColor);
    moveLog.push(move.algebraic);

    var status = game.status();
    if (status === "checkmate" || status === "stalemate" || status === "draw_fifty_move" ||
        status === "draw_repetition" || status === "draw_insufficient_material") {
      endGame(status);
      return;
    }
    render();
    maybeTriggerBotMove();
  }

  /** If it's the bot's turn, picks its move after a short delay so its reply doesn't feel
   *  instantaneous, then applies it exactly like a human move. Re-checks isBotTurn() after the
   *  delay in case the game ended (e.g. the human resigned) while it was "thinking". */
  function maybeTriggerBotMove() {
    if (!game.hasBot() || !game.isBotTurn()) return;
    isBotThinking = true;
    render();
    if (botMoveTimeout !== null) clearTimeout(botMoveTimeout);
    botMoveTimeout = setTimeout(function () {
      botMoveTimeout = null;
      if (gameOverReason || !game.isBotTurn()) {
        isBotThinking = false;
        render();
        return;
      }
      var movedColor = game.sideToMove();
      var move = game.playBotMove();
      isBotThinking = false;
      if (!move) {
        render();
        return;
      }
      afterMoveApplied(movedColor, move);
    }, 500);
  }

  function choosePromotion(promotion) {
    var pending = pendingPromotion;
    if (!pending) return;
    var move = pending.options.filter(function (m) {
      return m.promotion === promotion;
    })[0];
    pendingPromotion = null;
    if (move) applyMove(move);
    render();
  }

  function endGame(reason) {
    stopTicker();
    if (botMoveTimeout !== null) {
      clearTimeout(botMoveTimeout);
      botMoveTimeout = null;
    }
    isBotThinking = false;
    gameOverReason = reason;
    selected = null;
    pendingPromotion = null;
    render();
  }

  function resign(color) {
    endGame(color === "white" ? "white_resigned" : "black_resigned");
  }

  // The human's color when playing against the bot; null in pass-and-play.
  function humanColor() {
    if (!currentBotColor) return null;
    return currentBotColor === "white" ? "black" : "white";
  }

  function agreeToDraw() {
    endGame("draw_agreed");
  }

  function backToSetup() {
    stopTicker();
    if (botMoveTimeout !== null) {
      clearTimeout(botMoveTimeout);
      botMoveTimeout = null;
    }
    isBotThinking = false;
    screen = "setup";
    render();
  }

  function winnerColor() {
    switch (gameOverReason) {
      case "checkmate":
        return game.sideToMove() === "white" ? "black" : "white";
      case "white_time_out":
      case "white_resigned":
        return "black";
      case "black_time_out":
      case "black_resigned":
        return "white";
      default:
        return null;
    }
  }

  // boardSquares() has one entry per square, including empty ones (pieceType/pieceColor null),
  // so this returns null for an empty square rather than that always-truthy empty-square entry.
  function pieceAt(file, rank, squares) {
    squares = squares || game.boardSquares();
    for (var i = 0; i < squares.length; i++) {
      var s = squares[i];
      if (s.file === file && s.rank === rank) return s.pieceType ? s : null;
    }
    return null;
  }

  // ---------------------------------------------------------------------
  // Game screen
  // ---------------------------------------------------------------------

  function renderGameScreen() {
    var screenEl = el("div", "game-screen");

    var boardPane = el("div", "board-pane");
    boardPane.appendChild(renderBoardWrap());
    if (isBotThinking) {
      boardPane.appendChild(el("div", "bot-thinking", "Computer is thinking…"));
    }
    screenEl.appendChild(boardPane);

    screenEl.appendChild(renderSidePanel());

    return screenEl;
  }

  // Whether color's clock should be rotated to face that seat: only Black's, and only in
  // pass-and-play (both players share one device without moving it); against the bot there's
  // just one human seat, so neither clock rotates. Shared by the initial render and the ticker's
  // in-place update below so the two can never drift out of sync with each other.
  function clockFacesOpponentSeat(color) {
    return color === "black" && !currentBotColor;
  }

  function clockPillClassName(color) {
    var isActive = clock.activeColor() === color && !gameOverReason;
    var isFlagged = clock.flaggedColor() === color;
    return "clock-pill" + (isFlagged ? " flagged" : isActive ? " active" : "");
  }

  // No WHITE/BLACK label: position (top/bottom, matching the board orientation) and the
  // captured-piece colors already say which clock is whose.
  function renderClockPill(color) {
    var pill = el("div", clockPillClassName(color));
    pill.id = "clock-pill-" + color;
    var timeEl = el("div", "clock-time", clock.isUnlimited() ? "∞" : formatClockTime(clock.remainingMillis(color)));
    timeEl.id = "clock-time-" + color;
    pill.appendChild(timeEl);
    return pill;
  }

  /** Cheap in-place update for a plain clock tick, so a full re-render (and its DOM teardown)
   *  doesn't happen ten times a second while nothing about the game itself has changed. Only
   *  ever touches the pill/time elements, never the rotated .side-clock-block wrapper around
   *  them, so a fast-ticking timed clock can never reset that rotation mid-game. */
  function updateClockDisplays() {
    ["white", "black"].forEach(function (color) {
      var pill = document.getElementById("clock-pill-" + color);
      var timeEl = document.getElementById("clock-time-" + color);
      if (!pill || !timeEl) return;
      pill.className = clockPillClassName(color);
      timeEl.textContent = clock.isUnlimited() ? "∞" : formatClockTime(clock.remainingMillis(color));
    });
  }

  // Captured pieces (with the classic +N material-advantage count for whichever side is ahead)
  // shown right next to that side's clock in the side panel.
  function renderCapturedRow(color) {
    var opponent = color === "white" ? "black" : "white";
    var ownCaptures = game.capturedPieces(opponent); // pieces THIS color has captured
    var theirCaptures = game.capturedPieces(color); // pieces the opponent has captured from them

    var row = el("div", "captured-row");
    if (ownCaptures.length > 0) {
      var pieces = el("span", "captured-pieces");
      ownCaptures.forEach(function (type) {
        pieces.appendChild(pieceImage(type, opponent, "piece-glyph"));
      });
      row.appendChild(pieces);
    }
    var advantage = materialValue(ownCaptures) - materialValue(theirCaptures);
    if (advantage > 0) {
      row.appendChild(el("span", "material-advantage", "+" + advantage));
    }
    return row;
  }

  function renderSideClockBlock(color) {
    var classes = "side-clock-block" + (clockFacesOpponentSeat(color) ? " facing-away" : "");
    var block = el("div", classes);
    block.appendChild(renderClockPill(color));
    block.appendChild(renderCapturedRow(color));
    return block;
  }

  function formatClockTime(millis) {
    var clamped = Math.max(0, millis);
    if (clamped < 10000) {
      var seconds = Math.floor(clamped / 1000);
      var tenths = Math.floor((clamped % 1000) / 100);
      return "0:" + pad2(seconds) + "." + tenths;
    }
    var totalSeconds = Math.floor(clamped / 1000);
    var minutes = Math.floor(totalSeconds / 60);
    var seconds2 = totalSeconds % 60;
    return minutes + ":" + pad2(seconds2);
  }

  function pad2(n) {
    return n < 10 ? "0" + n : String(n);
  }

  function renderBoardWrap() {
    var wrap = el("div", "board-wrap");
    var grid = el("div", "board-grid");

    var sideToMove = game.sideToMove();
    var fixedSeat = humanColor();
    // Against the bot as Black, the whole board is permanently flipped (like flipping the board
    // on lichess/chess.com) so Black's own pieces sit at the bottom, near the human — pieces are
    // drawn upright either way, only their screen position changes, so no per-piece rotation is
    // needed here (unlike pass-and-play below).
    var boardFlipped = fixedSeat === "black";
    // Pass-and-play: pieces rotate with whoever's turn it is, so both players share one device
    // without moving it. Against the bot there's only one human in one seat the whole game, so
    // pieces are never individually rotated — the board orientation above already handles it.
    var pieceRotated = fixedSeat ? false : sideToMove === "black";
    var status = game.status();
    var kingInCheck = (status === "check" || status === "checkmate") ? game.sideToMoveKingSquareIfInCheck() : null;
    var lastMove = game.lastMove();
    var legalTargets = selected ? game.legalMovesFrom(selected.file, selected.rank) : [];
    var squares = game.boardSquares();

    for (var displayRow = 0; displayRow < 8; displayRow++) {
      for (var displayCol = 0; displayCol < 8; displayCol++) {
        var file = boardFlipped ? 7 - displayCol : displayCol;
        var rank = boardFlipped ? displayRow : 7 - displayRow;
        grid.appendChild(renderSquare(file, rank, pieceRotated, kingInCheck, lastMove, legalTargets, squares));
      }
    }
    wrap.appendChild(grid);
    return wrap;
  }

  function renderSquare(file, rank, pieceRotated, kingInCheck, lastMove, legalTargets, squares) {
    var isLight = (file + rank) % 2 === 1;
    var square = el("div", "square " + (isLight ? "light" : "dark"));
    square.addEventListener("click", function () {
      onSquareTapped(file, rank);
    });

    var piece = pieceAt(file, rank, squares);
    var isSelected = !!(selected && selected.file === file && selected.rank === rank);
    var isCheck = !!(kingInCheck && kingInCheck.file === file && kingInCheck.rank === rank);
    var isLastMove = !!(lastMove && (
      (lastMove.fromFile === file && lastMove.fromRank === rank) ||
      (lastMove.toFile === file && lastMove.toRank === rank)
    ));
    var isLegalTarget = legalTargets.some(function (m) {
      return m.toFile === file && m.toRank === rank;
    });

    if (isLastMove) square.appendChild(el("div", "square-overlay last-move"));
    if (isCheck) square.appendChild(el("div", "square-overlay check"));
    if (isSelected) square.appendChild(el("div", "square-overlay selected"));

    if (isLegalTarget) {
      if (piece) {
        square.appendChild(el("div", "legal-ring"));
      } else {
        square.appendChild(el("div", "legal-dot"));
      }
    }

    if (piece) {
      var glyph = pieceImage(
        piece.pieceType,
        piece.pieceColor,
        "piece-glyph" + (pieceRotated ? " rotated" : "")
      );
      square.appendChild(glyph);
    }

    return square;
  }

  // Mirrors the board-pane's own top/bottom structure (opponent's bar, board, player's bar) so
  // the side panel reads as the same kind of sandwich and is naturally the same height:
  // whichever color's clock is on top/bottom matches where that color's pieces sit on the board.
  // In pass-and-play there's no single human seat, so this defaults to Black-top/White-bottom;
  // against the bot, the human's own color is always on the bottom, matching the board flip in
  // renderBoardWrap.
  function renderSidePanel() {
    var panel = el("div", "side-panel");

    var humanIsBlack = humanColor() === "black";
    var topColor = humanIsBlack ? "white" : "black";
    var bottomColor = humanIsBlack ? "black" : "white";

    panel.appendChild(renderSideClockBlock(topColor));

    var middle = el("div", "side-panel-middle");

    middle.appendChild(el("div", "moves-title", "Moves"));

    var movesList = el("div", "moves-list");
    var history = moveHistoryPairs();
    history.forEach(function (pair, index) {
      var row = el("div", "move-row");
      row.appendChild(el("span", "move-num", (index + 1) + "."));
      row.appendChild(el("span", "move-w", pair[0] || ""));
      row.appendChild(el("span", null, pair[1] || ""));
      movesList.appendChild(row);
    });
    middle.appendChild(movesList);

    var buttonRow = el("div", "button-row");

    var drawButton = el("button", "outlined-button", "Offer draw");
    drawButton.disabled = !!gameOverReason || !!currentBotColor;
    drawButton.addEventListener("click", function () {
      showDrawOfferConfirm = true;
      render();
    });
    buttonRow.appendChild(drawButton);

    var resignButton = el("button", "outlined-button", "Resign");
    resignButton.disabled = !!gameOverReason;
    resignButton.addEventListener("click", function () {
      showResignConfirm = true;
      render();
    });
    buttonRow.appendChild(resignButton);

    middle.appendChild(buttonRow);

    var newSetupButton = el("button", "outlined-button", "New setup");
    newSetupButton.addEventListener("click", backToSetup);
    middle.appendChild(newSetupButton);

    panel.appendChild(middle);

    panel.appendChild(renderSideClockBlock(bottomColor));

    return panel;
  }

  // The engine facade only exposes the single most recent move (lastMove()), not the whole
  // history, so the side panel's numbered move list is built from our own running log instead.
  var moveLog = [];

  function moveHistoryPairs() {
    var pairs = [];
    for (var i = 0; i < moveLog.length; i += 2) {
      pairs.push([moveLog[i], moveLog[i + 1]]);
    }
    return pairs;
  }

  // ---------------------------------------------------------------------
  // Overlays
  // ---------------------------------------------------------------------

  function renderPromotionOverlay() {
    var backdrop = el("div", "overlay-backdrop");
    var card = el("div", "overlay-card promotion-card");
    card.appendChild(el("div", "overlay-title", "Promote pawn to…"));
    var choices = el("div", "promotion-choices");
    var promotingColor = game.sideToMove();
    ["queen", "rook", "bishop", "knight"].forEach(function (type) {
      var button = el("button", null, null);
      button.appendChild(pieceImage(type, promotingColor, "piece-glyph"));
      button.addEventListener("click", function () {
        choosePromotion(type);
      });
      choices.appendChild(button);
    });
    card.appendChild(choices);
    backdrop.appendChild(card);
    return backdrop;
  }

  function renderGameOverOverlay() {
    var backdrop = el("div", "overlay-backdrop");
    var card = el("div", "overlay-card");
    var winner = winnerColor();
    var title = winner === "white" ? "White wins" : winner === "black" ? "Black wins" : "Draw";
    card.appendChild(el("div", "overlay-title", title));
    card.appendChild(el("div", "overlay-text", GAME_OVER_TEXT[gameOverReason] || ""));
    var actions = el("div", "overlay-actions");
    var rematchButton = el("button", "primary-button", "Rematch");
    rematchButton.addEventListener("click", rematch);
    var newSetupButton = el("button", "outlined-button", "New setup");
    newSetupButton.addEventListener("click", backToSetup);
    actions.appendChild(rematchButton);
    actions.appendChild(newSetupButton);
    card.appendChild(actions);
    backdrop.appendChild(card);
    return backdrop;
  }

  function renderResignOverlay() {
    var backdrop = el("div", "overlay-backdrop");
    var card = el("div", "overlay-card");
    var resigningColor = humanColor() || game.sideToMove();
    card.appendChild(el("div", "overlay-title", "Resign?"));
    card.appendChild(el("div", "overlay-text", (resigningColor === "white" ? "White" : "Black") + " will lose the game."));
    var actions = el("div", "overlay-actions");
    var confirmButton = el("button", "primary-button", "Resign");
    confirmButton.addEventListener("click", function () {
      showResignConfirm = false;
      resign(resigningColor);
    });
    var cancelButton = el("button", "outlined-button", "Cancel");
    cancelButton.addEventListener("click", function () {
      showResignConfirm = false;
      render();
    });
    actions.appendChild(confirmButton);
    actions.appendChild(cancelButton);
    card.appendChild(actions);
    backdrop.appendChild(card);
    return backdrop;
  }

  function renderDrawOfferOverlay() {
    var backdrop = el("div", "overlay-backdrop");
    var card = el("div", "overlay-card");
    var offeringColor = game.sideToMove();
    var decidingColor = offeringColor === "white" ? "black" : "white";
    var offeringName = offeringColor === "white" ? "White" : "Black";
    var decidingName = decidingColor === "white" ? "White" : "Black";
    card.appendChild(el("div", "overlay-title", "Draw offered"));
    card.appendChild(el("div", "overlay-text", offeringName + " offers a draw. " + decidingName + ", do you accept?"));
    var actions = el("div", "overlay-actions");
    var acceptButton = el("button", "primary-button", "Accept");
    acceptButton.addEventListener("click", function () {
      showDrawOfferConfirm = false;
      agreeToDraw();
    });
    var declineButton = el("button", "outlined-button", "Decline");
    declineButton.addEventListener("click", function () {
      showDrawOfferConfirm = false;
      render();
    });
    actions.appendChild(acceptButton);
    actions.appendChild(declineButton);
    card.appendChild(actions);
    backdrop.appendChild(card);
    return backdrop;
  }

  // ---------------------------------------------------------------------
  // DOM helper
  // ---------------------------------------------------------------------

  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
  }

  render();
})();
