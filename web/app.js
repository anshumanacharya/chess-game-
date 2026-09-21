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

  // ︎ (variation selector-15) forces the text presentation of these glyphs rather than
  // an emoji-style colored one. Without it, iOS Safari renders them as solid black regardless
  // of any CSS color/-webkit-text-fill-color, so "white" pieces come out looking black too.
  var PIECE_GLYPH = {
    king: "♚︎",
    queen: "♛︎",
    rook: "♜︎",
    bishop: "♝︎",
    knight: "♞︎",
    pawn: "♟︎"
  };

  var GAME_OVER_TEXT = {
    checkmate: "Checkmate",
    stalemate: "Stalemate",
    draw_fifty_move: "Draw – 50-move rule",
    draw_repetition: "Draw – threefold repetition",
    draw_insufficient_material: "Draw – insufficient material",
    white_time_out: "White ran out of time",
    black_time_out: "Black ran out of time",
    white_resigned: "White resigned",
    black_resigned: "Black resigned"
  };

  // ---- app state ----
  var screen = "setup"; // "setup" | "game"
  var game = null;
  var clock = null;
  var selected = null; // { file, rank }
  var pendingPromotion = null; // { fromFile, fromRank, toFile, toRank, options }
  var gameOverReason = null; // string | null
  var showResignConfirm = false;
  var tickHandle = null;
  var lastTickAt = 0;
  var lastConfig = null;

  var setupState = {
    mode: "timed", // "timed" | "unlimited"
    presetIndex: 3,
    useCustom: false,
    customMinutes: 15,
    customIncrement: 0
  };

  var root = document.getElementById("app");

  function render() {
    root.innerHTML = "";
    if (screen === "setup") {
      root.appendChild(renderSetupScreen());
    } else {
      root.appendChild(renderGameScreen());
      if (pendingPromotion) root.appendChild(renderPromotionOverlay());
      else if (gameOverReason) root.appendChild(renderGameOverOverlay());
      else if (showResignConfirm) root.appendChild(renderResignOverlay());
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
    game = new JsGame();
    clock = new JsClock(config.minutes, config.increment, config.unlimited);
    clock.start("white");
    selected = null;
    pendingPromotion = null;
    gameOverReason = null;
    showResignConfirm = false;
    moveLog = [];
    screen = "game";
    startTicker();
    render();
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
    if (gameOverReason) return;
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
    var nextColor = game.sideToMove();
    clock.onMoveCompleted(movedColor, nextColor);
    moveLog.push(move.algebraic);
    selected = null;
    pendingPromotion = null;

    var status = game.status();
    if (status === "checkmate" || status === "stalemate" || status === "draw_fifty_move" ||
        status === "draw_repetition" || status === "draw_insufficient_material") {
      endGame(status);
    }
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
    gameOverReason = reason;
    selected = null;
    pendingPromotion = null;
    render();
  }

  function resign(color) {
    endGame(color === "white" ? "white_resigned" : "black_resigned");
  }

  function backToSetup() {
    stopTicker();
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
    boardPane.appendChild(renderPlayerBar("black", true));
    boardPane.appendChild(renderBoardWrap());
    boardPane.appendChild(renderPlayerBar("white", false));
    screenEl.appendChild(boardPane);

    screenEl.appendChild(renderSidePanel());

    return screenEl;
  }

  function renderPlayerBar(color, facingAway) {
    var bar = el("div", "player-bar" + (facingAway ? " facing-away" : ""));

    var info = el("div", "player-info");
    info.appendChild(el("div", "player-name", color === "white" ? "White" : "Black"));
    var capturedRow = el("div", "captured-row");
    var opponent = color === "white" ? "black" : "white";
    game.capturedPieces(opponent).forEach(function (type) {
      capturedRow.appendChild(el("span", "piece-glyph", PIECE_GLYPH[type]));
    });
    info.appendChild(capturedRow);
    bar.appendChild(info);

    var isActive = clock.activeColor() === color && !gameOverReason;
    var isFlagged = clock.flaggedColor() === color;
    var pill = el("div", "clock-pill" + (isFlagged ? " flagged" : isActive ? " active" : ""));
    pill.id = "clock-pill-" + color;
    pill.appendChild(el("div", "clock-label", color.toUpperCase()));
    var timeEl = el("div", "clock-time", clock.isUnlimited() ? "∞" : formatClockTime(clock.remainingMillis(color)));
    timeEl.id = "clock-time-" + color;
    pill.appendChild(timeEl);
    bar.appendChild(pill);

    return bar;
  }

  /** Cheap in-place update for a plain clock tick, so a full re-render (and its DOM teardown)
   *  doesn't happen ten times a second while nothing about the game itself has changed. */
  function updateClockDisplays() {
    ["white", "black"].forEach(function (color) {
      var pill = document.getElementById("clock-pill-" + color);
      var timeEl = document.getElementById("clock-time-" + color);
      if (!pill || !timeEl) return;
      var isActive = clock.activeColor() === color && !gameOverReason;
      var isFlagged = clock.flaggedColor() === color;
      pill.className = "clock-pill" + (isFlagged ? " flagged" : isActive ? " active" : "");
      timeEl.textContent = clock.isUnlimited() ? "∞" : formatClockTime(clock.remainingMillis(color));
    });
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
    var pieceRotated = sideToMove === "black";
    var status = game.status();
    var kingInCheck = (status === "check" || status === "checkmate") ? game.sideToMoveKingSquareIfInCheck() : null;
    var lastMove = game.lastMove();
    var legalTargets = selected ? game.legalMovesFrom(selected.file, selected.rank) : [];
    var squares = game.boardSquares();

    for (var displayRow = 0; displayRow < 8; displayRow++) {
      for (var displayCol = 0; displayCol < 8; displayCol++) {
        var file = displayCol;
        var rank = 7 - displayRow;
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
      var glyph = el("span", "piece-glyph " + (piece.pieceColor === "white" ? "piece-white" : "piece-black") +
        (pieceRotated ? " rotated" : ""), PIECE_GLYPH[piece.pieceType]);
      square.appendChild(glyph);
    }

    return square;
  }

  function renderSidePanel() {
    var panel = el("div", "side-panel");
    panel.appendChild(el("div", "moves-title", "Moves"));

    var movesList = el("div", "moves-list");
    var history = moveHistoryPairs();
    history.forEach(function (pair, index) {
      var row = el("div", "move-row");
      row.appendChild(el("span", "move-num", (index + 1) + "."));
      row.appendChild(el("span", "move-w", pair[0] || ""));
      row.appendChild(el("span", null, pair[1] || ""));
      movesList.appendChild(row);
    });
    panel.appendChild(movesList);

    var resignButton = el("button", "outlined-button", "Resign");
    resignButton.disabled = !!gameOverReason;
    resignButton.addEventListener("click", function () {
      showResignConfirm = true;
      render();
    });
    panel.appendChild(resignButton);

    var newSetupButton = el("button", "outlined-button", "New setup");
    newSetupButton.addEventListener("click", backToSetup);
    panel.appendChild(newSetupButton);

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
    var card = el("div", "overlay-card");
    card.appendChild(el("div", "overlay-title", "Promote pawn to…"));
    var choices = el("div", "promotion-choices");
    ["queen", "rook", "bishop", "knight"].forEach(function (type) {
      var button = el("button", null, PIECE_GLYPH[type]);
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
    var resigningColor = game.sideToMove();
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
