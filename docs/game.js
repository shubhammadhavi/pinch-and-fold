(() => {
  "use strict";

  const canvas = document.getElementById("gameCanvas");
  const ctx = canvas.getContext("2d");

  // ---------------------------------------------------------------------------
  // Web Audio Synthesizer + Haptics
  // ---------------------------------------------------------------------------
  class SoundEngine {
    constructor() {
      this.ctx = null;
      this.muted = false;
    }

    unlock() {
      if (!this.ctx) {
        const AudioCtx = window.AudioContext || window.webkitAudioContext;
        if (AudioCtx) this.ctx = new AudioCtx();
      }
      if (this.ctx && this.ctx.state === "suspended") {
        this.ctx.resume().catch(() => {});
      }
    }

    vibrate(ms) {
      if (navigator.vibrate) {
        try { navigator.vibrate(ms); } catch (_) {}
      }
    }

    _noiseBuffer(duration) {
      const sr = this.ctx.sampleRate;
      const len = Math.floor(sr * duration);
      const buf = this.ctx.createBuffer(1, len, sr);
      const data = buf.getChannelData(0);
      for (let i = 0; i < len; i++) data[i] = Math.random() * 2 - 1;
      return buf;
    }

    playSnap(vol = 0.85) {
      if (this.muted || !this.ctx) return;
      const now = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = "triangle";
      osc.frequency.setValueAtTime(880, now);
      osc.frequency.exponentialRampToValueAtTime(140, now + 0.14);
      gain.gain.setValueAtTime(0.32 * vol, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.16);
      osc.connect(gain).connect(this.ctx.destination);
      osc.start(now);
      osc.stop(now + 0.17);

      const noise = this.ctx.createBufferSource();
      noise.buffer = this._noiseBuffer(0.06);
      const nGain = this.ctx.createGain();
      nGain.gain.setValueAtTime(0.18 * vol, now);
      nGain.gain.exponentialRampToValueAtTime(0.001, now + 0.06);
      noise.connect(nGain).connect(this.ctx.destination);
      noise.start(now);
    }

    playHit(intensity = 1.0) {
      if (this.muted || !this.ctx) return;
      const vol = Math.max(0.25, Math.min(1.0, intensity));
      const now = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = "sine";
      osc.frequency.setValueAtTime(220, now);
      osc.frequency.exponentialRampToValueAtTime(58, now + 0.18);
      gain.gain.setValueAtTime(0.42 * vol, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.2);
      osc.connect(gain).connect(this.ctx.destination);
      osc.start(now);
      osc.stop(now + 0.21);

      const noise = this.ctx.createBufferSource();
      noise.buffer = this._noiseBuffer(0.11);
      const nGain = this.ctx.createGain();
      nGain.gain.setValueAtTime(0.22 * vol, now);
      nGain.gain.exponentialRampToValueAtTime(0.001, now + 0.11);
      noise.connect(nGain).connect(this.ctx.destination);
      noise.start(now);
    }

    playRopeBounce(intensity = 0.8) {
      if (this.muted || !this.ctx) return;
      const vol = Math.max(0.2, Math.min(0.95, intensity));
      const now = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = "sine";
      osc.frequency.setValueAtTime(240, now);
      osc.frequency.linearRampToValueAtTime(390, now + 0.06);
      osc.frequency.exponentialRampToValueAtTime(190, now + 0.24);
      gain.gain.setValueAtTime(0.26 * vol, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
      osc.connect(gain).connect(this.ctx.destination);
      osc.start(now);
      osc.stop(now + 0.26);
    }

    playStretchTick() {
      if (this.muted || !this.ctx) return;
      const now = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = "triangle";
      osc.frequency.setValueAtTime(520 + Math.random() * 140, now);
      osc.frequency.linearRampToValueAtTime(760, now + 0.035);
      gain.gain.setValueAtTime(0.09, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.04);
      osc.connect(gain).connect(this.ctx.destination);
      osc.start(now);
      osc.stop(now + 0.045);
    }

    playRingOut() {
      if (this.muted || !this.ctx) return;
      const now = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = "sine";
      osc.frequency.setValueAtTime(680, now);
      osc.frequency.exponentialRampToValueAtTime(110, now + 0.45);
      gain.gain.setValueAtTime(0.26, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.48);
      osc.connect(gain).connect(this.ctx.destination);
      osc.start(now);
      osc.stop(now + 0.5);

      const bell = this.ctx.createOscillator();
      const bGain = this.ctx.createGain();
      bell.type = "triangle";
      bell.frequency.setValueAtTime(880, now);
      bGain.gain.setValueAtTime(0.2, now);
      bGain.gain.exponentialRampToValueAtTime(0.001, now + 0.42);
      bell.connect(bGain).connect(this.ctx.destination);
      bell.start(now);
      bell.stop(now + 0.43);
    }

    playWin() {
      if (this.muted || !this.ctx) return;
      const notes = [523.25, 659.25, 783.99];
      notes.forEach((freq, i) => {
        const start = this.ctx.currentTime + i * 0.1;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = "triangle";
        osc.frequency.setValueAtTime(freq, start);
        gain.gain.setValueAtTime(0.22, start);
        gain.gain.exponentialRampToValueAtTime(0.001, start + 0.25);
        osc.connect(gain).connect(this.ctx.destination);
        osc.start(start);
        osc.stop(start + 0.26);
      });
    }

    playUiTap() {
      if (this.muted || !this.ctx) return;
      const now = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = "triangle";
      osc.frequency.setValueAtTime(440, now);
      gain.gain.setValueAtTime(0.18, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.05);
      osc.connect(gain).connect(this.ctx.destination);
      osc.start(now);
      osc.stop(now + 0.055);
    }
  }

  // ---------------------------------------------------------------------------
  // Game Models & Archetypes
  // ---------------------------------------------------------------------------
  const MODE_SOLO = "SOLO";
  const MODE_2P = "2P";

  const STATE_MENU = "MENU";
  const STATE_PLAYING = "PLAYING";
  const STATE_ROUND_OVER = "ROUND_OVER";
  const STATE_GAME_OVER = "GAME_OVER";

  const ARCHETYPES = {
    PLAYER_TEAL: { label: "YOU", base: "#0FB5CC", fold: "#2BD4EA", rScale: 1.0, mass: 1.0 },
    PLAYER_CORAL: { label: "P2", base: "#F25F3A", fold: "#FF8662", rScale: 1.0, mass: 1.0 },
    LIGHT_ORIGAMI: { label: "FLIMSY", base: "#F25F3A", fold: "#FF8662", rScale: 0.92, mass: 0.82 },
    HEAVY_CARDBOARD: { label: "BRUTE", base: "#B8723E", fold: "#D6945C", rScale: 1.24, mass: 1.85 },
    NINJA_STAR: { label: "DART", base: "#A855F7", fold: "#C484FC", rScale: 0.84, mass: 0.78 },
    GOLD_BOSS: { label: "CHAMP", base: "#EAB308", fold: "#FDE047", rScale: 1.32, mass: 2.2 }
  };

  function makeSlingshot(team) {
    return {
      team,
      pointerIdA: null, ax: 0, ay: 0,
      pointerIdB: null, bx: 0, by: 0,
      singleStartX: 0, singleStartY: 0,
      lastTickDistance: 0,
      get activeCount() {
        return (this.pointerIdA !== null ? 1 : 0) + (this.pointerIdB !== null ? 1 : 0);
      },
      reset() {
        this.pointerIdA = null;
        this.pointerIdB = null;
        this.lastTickDistance = 0;
      }
    };
  }

  // ---------------------------------------------------------------------------
  // Game Engine
  // ---------------------------------------------------------------------------
  const audio = new SoundEngine();

  const engine = {
    width: 540,
    height: 960,
    ringCenterX: 270,
    ringCenterY: 508,
    ringRadius: 220,

    screenState: STATE_MENU,
    gameMode: MODE_SOLO,
    wave: 1,
    scoreP1: 0,
    scoreP2: 0,
    bestWave: Number(localStorage.getItem("pinchAndFoldBestWave") || "1"),
    hasTouchedInPlay: false,

    roundBannerText: "",
    roundWinnerTeam: 0,
    roundTimer: 0,

    shakeIntensity: 0,
    shakeX: 0,
    shakeY: 0,
    hitStopTimer: 0,
    timeElapsed: 0,
    demoTimer: 0,
    nextId: 1,

    wrestlers: [],
    particles: [],
    popups: [],
    ropeFlexes: Array.from({ length: 4 }, () => ({ flexOffset: 0, flexVel: 0 })),

    slingshotP1: makeSlingshot(0),
    slingshotP2: makeSlingshot(1),

    btnSolo: { x: 0, y: 0, w: 0, h: 0 },
    btnTwoPlayer: { x: 0, y: 0, w: 0, h: 0 },
    btnMute: { x: 0, y: 0, w: 0, h: 0 },
    btnHome: { x: 0, y: 0, w: 0, h: 0 },
    btnPlayAgain: { x: 0, y: 0, w: 0, h: 0 }
  };

  function inRect(r, x, y) {
    return x >= r.x && x <= r.x + r.w && y >= r.y && y <= r.y + r.h;
  }

  function makeWrestler(team, arch, x, y, baseR) {
    return {
      id: engine.nextId++,
      team,
      arch,
      x, y,
      vx: 0, vy: 0,
      radius: baseR * arch.rScale,
      mass: arch.mass,
      angle: 0,
      angularVel: 0,
      squashX: 1,
      squashY: 1,
      shockTimer: 0,
      aiCooldown: 0.65 + Math.random() * 0.55,
      alive: true,
      fallScale: 1,
      fallAlpha: 1
    };
  }

  function setupDemoRing() {
    engine.wrestlers = [];
    const r = engine.ringRadius * 0.135;
    const w1 = makeWrestler(0, ARCHETYPES.PLAYER_TEAL, engine.ringCenterX - engine.ringRadius * 0.35, engine.ringCenterY + engine.ringRadius * 0.25, r);
    w1.vx = engine.ringRadius * 0.45;
    w1.vy = -engine.ringRadius * 0.35;
    const w2 = makeWrestler(1, ARCHETYPES.LIGHT_ORIGAMI, engine.ringCenterX + engine.ringRadius * 0.35, engine.ringCenterY - engine.ringRadius * 0.25, r);
    w2.vx = -engine.ringRadius * 0.42;
    w2.vy = engine.ringRadius * 0.38;
    engine.wrestlers.push(w1, w2);
  }

  function startNewGame(mode) {
    engine.gameMode = mode;
    engine.wave = 1;
    engine.scoreP1 = 0;
    engine.scoreP2 = 0;
    engine.particles = [];
    engine.popups = [];
    engine.screenState = STATE_PLAYING;
    audio.playUiTap();
    spawnRound();
  }

  function spawnRound() {
    engine.wrestlers = [];
    engine.slingshotP1.reset();
    engine.slingshotP2.reset();
    const baseR = engine.ringRadius * 0.135;

    engine.wrestlers.push(
      makeWrestler(0, ARCHETYPES.PLAYER_TEAL, engine.ringCenterX, engine.ringCenterY + engine.ringRadius * 0.42, baseR)
    );

    if (engine.gameMode === MODE_2P) {
      engine.wrestlers.push(
        makeWrestler(1, ARCHETYPES.PLAYER_CORAL, engine.ringCenterX, engine.ringCenterY - engine.ringRadius * 0.42, baseR)
      );
      return;
    }

    const w = engine.wave;
    const addE = (arch, nx, ny) => engine.wrestlers.push(
      makeWrestler(1, arch, engine.ringCenterX + engine.ringRadius * nx, engine.ringCenterY + engine.ringRadius * ny, baseR)
    );

    if (w === 1) addE(ARCHETYPES.LIGHT_ORIGAMI, 0, -0.42);
    else if (w === 2) addE(ARCHETYPES.HEAVY_CARDBOARD, 0, -0.42);
    else if (w === 3) { addE(ARCHETYPES.LIGHT_ORIGAMI, -0.35, -0.36); addE(ARCHETYPES.NINJA_STAR, 0.35, -0.36); }
    else if (w === 4) { addE(ARCHETYPES.HEAVY_CARDBOARD, -0.28, -0.38); addE(ARCHETYPES.NINJA_STAR, 0.32, -0.34); }
    else if (w % 5 === 0) { addE(ARCHETYPES.GOLD_BOSS, 0, -0.42); addE(ARCHETYPES.NINJA_STAR, 0.42, -0.22); }
    else {
      const count = Math.min(3, 2 + Math.floor(w / 4));
      const pool = [ARCHETYPES.LIGHT_ORIGAMI, ARCHETYPES.HEAVY_CARDBOARD, ARCHETYPES.NINJA_STAR];
      for (let i = 0; i < count; i++) {
        const arch = pool[(w + i) % pool.length];
        const offset = (i - (count - 1) * 0.5) * 0.45;
        addE(arch, offset, -0.38);
      }
    }
  }

  function triggerRopeHit(angleRad, speed) {
    let deg = (angleRad * 180) / Math.PI;
    if (deg < 0) deg += 360;
    const idx = Math.max(0, Math.min(3, Math.floor(((deg + 45) % 360) / 90)));
    engine.ropeFlexes[idx].flexVel = Math.max(engine.ropeFlexes[idx].flexVel, speed * 0.55);
  }

  function spawnConfetti(x, y, count, primary) {
    const palette = [primary, "#0FB5CC", "#F25F3A", "#FDE047", "#FFFFFF"];
    for (let i = 0; i < count; i++) {
      const ang = Math.random() * Math.PI * 2;
      const spd = engine.ringRadius * (0.4 + Math.random() * 1.2);
      const life = 0.45 + Math.random() * 0.45;
      engine.particles.push({
        x, y,
        vx: Math.cos(ang) * spd,
        vy: Math.sin(ang) * spd,
        size: engine.ringRadius * (0.025 + Math.random() * 0.03),
        angle: Math.random() * 360,
        spin: (Math.random() - 0.5) * 720,
        color: palette[i % palette.length],
        life, maxLife: life
      });
    }
  }

  function addPopup(text, x, y, color, scale = 1.15) {
    engine.popups.push({
      text, x, y,
      vy: -engine.ringRadius * 0.25,
      color, scale,
      life: 0.75, maxLife: 0.75
    });
  }

  function update(dtRaw) {
    const dt = Math.max(0.001, Math.min(0.033, dtRaw));
    engine.timeElapsed += dt;

    if (engine.shakeIntensity > 0.2) {
      engine.shakeX = (Math.random() * 2 - 1) * engine.shakeIntensity;
      engine.shakeY = (Math.random() * 2 - 1) * engine.shakeIntensity;
      engine.shakeIntensity *= 0.84;
    } else {
      engine.shakeIntensity = 0;
      engine.shakeX = 0;
      engine.shakeY = 0;
    }

    // Particles & popups
    for (let i = engine.particles.length - 1; i >= 0; i--) {
      const p = engine.particles[i];
      p.life -= dt;
      if (p.life <= 0) engine.particles.splice(i, 1);
      else {
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.vx *= 0.94;
        p.vy *= 0.94;
        p.angle += p.spin * dt;
      }
    }

    for (let i = engine.popups.length - 1; i >= 0; i--) {
      const pop = engine.popups[i];
      pop.life -= dt;
      if (pop.life <= 0) engine.popups.splice(i, 1);
      else {
        pop.y += pop.vy * dt;
        pop.vy *= 0.92;
      }
    }

    // Rope spring physics
    for (const rope of engine.ropeFlexes) {
      const acc = -180 * rope.flexOffset - 11 * rope.flexVel;
      rope.flexVel += acc * dt;
      rope.flexOffset += rope.flexVel * dt;
      rope.flexOffset = Math.max(-engine.ringRadius * 0.12, Math.min(engine.ringRadius * 0.22, rope.flexOffset));
    }

    if (engine.hitStopTimer > 0) {
      engine.hitStopTimer -= dt;
      return;
    }

    if (engine.screenState === STATE_MENU) {
      engine.demoTimer += dt;
      if (engine.demoTimer > 2.2) {
        engine.demoTimer = 0;
        for (const w of engine.wrestlers) {
          if (!w.alive) continue;
          const ang = Math.atan2(engine.ringCenterY - w.y, engine.ringCenterX - w.x) + (Math.random() - 0.5) * 0.9;
          const spd = engine.ringRadius * 1.4;
          w.vx += Math.cos(ang) * spd;
          w.vy += Math.sin(ang) * spd;
        }
      }
      updatePhysics(dt);
      if (engine.wrestlers.filter(w => w.alive).length < 2) setupDemoRing();
      return;
    }

    if (engine.screenState === STATE_ROUND_OVER) {
      engine.roundTimer -= dt;
      updatePhysics(dt * 0.7);
      if (engine.roundTimer <= 0) {
        if (engine.gameMode === MODE_SOLO) {
          if (engine.roundWinnerTeam === 0) {
            engine.wave++;
            if (engine.wave > engine.bestWave) {
              engine.bestWave = engine.wave;
              localStorage.setItem("pinchAndFoldBestWave", String(engine.bestWave));
            }
            engine.screenState = STATE_PLAYING;
            spawnRound();
          } else {
            engine.screenState = STATE_GAME_OVER;
          }
        } else {
          if (engine.scoreP1 >= 5 || engine.scoreP2 >= 5) {
            engine.screenState = STATE_GAME_OVER;
          } else {
            engine.screenState = STATE_PLAYING;
            spawnRound();
          }
        }
      }
      return;
    }

    if (engine.screenState === STATE_GAME_OVER) {
      updatePhysics(dt * 0.5);
      return;
    }

    if (engine.gameMode === MODE_SOLO) updateCpuAi(dt);
    updatePhysics(dt);
    checkRoundEnd();
  }

  function updateCpuAi(dt) {
    const player = engine.wrestlers.find(w => w.team === 0 && w.alive);
    if (!player) return;

    for (const cpu of engine.wrestlers) {
      if (cpu.team !== 1 || !cpu.alive || cpu.shockTimer > 0) continue;
      cpu.aiCooldown -= dt;
      if (cpu.aiCooldown <= 0) {
        const tx = player.x + player.vx * 0.14;
        const ty = player.y + player.vy * 0.14;
        const dx = tx - cpu.x;
        const dy = ty - cpu.y;

        let power = engine.ringRadius * 2.0;
        if (cpu.arch === ARCHETYPES.NINJA_STAR) power = engine.ringRadius * 2.55;
        else if (cpu.arch === ARCHETYPES.HEAVY_CARDBOARD) power = engine.ringRadius * 1.85;
        else if (cpu.arch === ARCHETYPES.GOLD_BOSS) power = engine.ringRadius * 2.35;

        const jitter = Math.max(0.05, 0.35 - engine.wave * 0.04);
        const aimAngle = Math.atan2(dy, dx) + (Math.random() * 2 - 1) * jitter;
        cpu.vx += Math.cos(aimAngle) * power;
        cpu.vy += Math.sin(aimAngle) * power;
        cpu.squashX = 1.25;
        cpu.squashY = 0.78;
        cpu.aiCooldown = cpu.arch === ARCHETYPES.NINJA_STAR
          ? 0.75 + Math.random() * 0.35
          : Math.max(0.75, 1.35 - engine.wave * 0.06) + Math.random() * 0.45;
        audio.playSnap(0.45);
      }
    }
  }

  function updatePhysics(dt) {
    const friction = 0.968;
    const ringOutSpeed = engine.ringRadius * 1.65;
    const damp = Math.pow(friction, dt * 60);

    for (const w of engine.wrestlers) {
      if (!w.alive) {
        w.x += w.vx * dt;
        w.y += w.vy * dt;
        w.angle += w.angularVel * dt;
        w.fallScale = Math.max(0, w.fallScale - dt * 2.1);
        w.fallAlpha = w.fallScale;
        continue;
      }

      if (w.shockTimer > 0) w.shockTimer = Math.max(0, w.shockTimer - dt);
      w.squashX += (1 - w.squashX) * Math.min(1, dt * 12);
      w.squashY += (1 - w.squashY) * Math.min(1, dt * 12);

      w.x += w.vx * dt;
      w.y += w.vy * dt;
      w.vx *= damp;
      w.vy *= damp;

      const spd = Math.hypot(w.vx, w.vy);
      w.angle += (w.angularVel + spd * 0.04) * dt;
      w.angularVel *= damp;

      const dx = w.x - engine.ringCenterX;
      const dy = w.y - engine.ringCenterY;
      const dist = Math.hypot(dx, dy);
      const maxDist = engine.ringRadius - w.radius;

      if (dist > maxDist && dist > 0.001) {
        const nx = dx / dist;
        const ny = dy / dist;
        const outwardVel = w.vx * nx + w.vy * ny;
        const breaksRopes = outwardVel > ringOutSpeed || (w.shockTimer > 0.15 && outwardVel > ringOutSpeed * 0.55);

        if (breaksRopes && engine.screenState === STATE_PLAYING) {
          w.alive = false;
          w.vx = nx * Math.max(outwardVel, engine.ringRadius * 1.1);
          w.vy = ny * Math.max(outwardVel, engine.ringRadius * 1.1);
          w.angularVel = (Math.random() < 0.5 ? 1 : -1) * 320;
          triggerRopeHit(Math.atan2(ny, nx), outwardVel);
          spawnConfetti(w.x, w.y, 18, w.arch.base);
          addPopup("RING OUT!", engine.ringCenterX + nx * engine.ringRadius * 0.72, engine.ringCenterY + ny * engine.ringRadius * 0.72, w.arch.fold, 1.25);
          engine.shakeIntensity = Math.max(engine.shakeIntensity, 22);
          audio.playRingOut();
          audio.vibrate(70);
        } else {
          w.x = engine.ringCenterX + nx * maxDist;
          w.y = engine.ringCenterY + ny * maxDist;
          if (outwardVel > 0) {
            w.vx -= 1.72 * outwardVel * nx;
            w.vy -= 1.72 * outwardVel * ny;
            w.squashX = 0.76;
            w.squashY = 1.26;
            if (outwardVel > engine.ringRadius * 0.28) {
              triggerRopeHit(Math.atan2(ny, nx), outwardVel);
              audio.playRopeBounce(outwardVel / (engine.ringRadius * 1.5));
              audio.vibrate(15);
            }
          }
        }
      }
    }

    // Wrestler-vs-Wrestler collisions
    for (let i = 0; i < engine.wrestlers.length; i++) {
      const a = engine.wrestlers[i];
      if (!a.alive) continue;
      for (let j = i + 1; j < engine.wrestlers.length; j++) {
        const b = engine.wrestlers[j];
        if (!b.alive) continue;

        const dx = b.x - a.x;
        const dy = b.y - a.y;
        const dist = Math.hypot(dx, dy);
        const minDist = a.radius + b.radius;

        if (dist < minDist && dist > 0.0001) {
          const nx = dx / dist;
          const ny = dy / dist;
          const overlap = minDist - dist;
          const totalMass = a.mass + b.mass;

          a.x -= nx * overlap * (b.mass / totalMass);
          a.y -= ny * overlap * (b.mass / totalMass);
          b.x += nx * overlap * (a.mass / totalMass);
          b.y += ny * overlap * (a.mass / totalMass);

          const velAlongNormal = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny;
          if (velAlongNormal < 0) {
            const restitution = 1.18;
            const impulse = -(1 + restitution) * velAlongNormal / (1 / a.mass + 1 / b.mass);
            const ix = impulse * nx;
            const iy = impulse * ny;

            a.vx -= ix / a.mass;
            a.vy -= iy / a.mass;
            b.vx += ix / b.mass;
            b.vy += iy / b.mass;

            const impactSpeed = -velAlongNormal;
            if (impactSpeed > engine.ringRadius * 0.45) {
              const midX = (a.x + b.x) * 0.5;
              const midY = (a.y + b.y) * 0.5;
              a.squashX = 0.72; a.squashY = 1.32;
              b.squashX = 0.72; b.squashY = 1.32;
              a.shockTimer = 0.38; b.shockTimer = 0.38;

              const heavy = impactSpeed > engine.ringRadius * 1.1;
              spawnConfetti(midX, midY, heavy ? 14 : 7, "#FFFFFF");
              if (heavy) {
                engine.hitStopTimer = 0.038;
                engine.shakeIntensity = Math.max(engine.shakeIntensity, 16);
                addPopup(Math.random() < 0.5 ? "SNAP!" : "POW!", midX, midY - a.radius * 0.6, "#FDE047", 1.15);
                audio.vibrate(40);
              } else {
                engine.shakeIntensity = Math.max(engine.shakeIntensity, 6);
                audio.vibrate(18);
              }
              audio.playHit(impactSpeed / (engine.ringRadius * 1.6));
            }
          }
        }
      }
    }
  }

  function checkRoundEnd() {
    const p1Alive = engine.wrestlers.some(w => w.team === 0 && w.alive);
    const p2Alive = engine.wrestlers.some(w => w.team === 1 && w.alive);
    if (!p1Alive || !p2Alive) {
      engine.screenState = STATE_ROUND_OVER;
      engine.roundTimer = 1.45;
      if (p1Alive && !p2Alive) {
        engine.roundWinnerTeam = 0;
        engine.scoreP1++;
        engine.roundBannerText = engine.gameMode === MODE_SOLO ? `WAVE ${engine.wave} CLEAR!` : "YOU SCORE!";
        audio.playWin();
      } else {
        engine.roundWinnerTeam = 1;
        engine.scoreP2++;
        engine.roundBannerText = engine.gameMode === MODE_SOLO ? "KNOCKED OUT!" : "P2 SCORES!";
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Pointer / Multi-Touch Input
  // ---------------------------------------------------------------------------
  function getCanvasCoords(e) {
    const rect = canvas.getBoundingClientRect();
    return {
      x: ((e.clientX - rect.left) / rect.width) * engine.width,
      y: ((e.clientY - rect.top) / rect.height) * engine.height
    };
  }

  function stretchDistance(sling) {
    if (sling.activeCount === 2) return Math.hypot(sling.bx - sling.ax, sling.by - sling.ay);
    if (sling.activeCount === 1) return Math.hypot(sling.singleStartX - sling.ax, sling.singleStartY - sling.ay);
    return 0;
  }

  function selectSlingshot(py) {
    if (engine.gameMode === MODE_2P && py < engine.ringCenterY) return engine.slingshotP2;
    return engine.slingshotP1;
  }

  function updateMove(sling, id, mx, my) {
    let moved = false;
    if (sling.pointerIdA === id) { sling.ax = mx; sling.ay = my; moved = true; }
    else if (sling.pointerIdB === id) { sling.bx = mx; sling.by = my; moved = true; }
    if (!moved) return;

    const dist = stretchDistance(sling);
    if (Math.abs(dist - sling.lastTickDistance) > engine.ringRadius * 0.14) {
      sling.lastTickDistance = dist;
      audio.playStretchTick();
      audio.vibrate(8);
    }

    if (sling.activeCount === 2 && dist < engine.ringRadius * 0.24) {
      const midX = (sling.ax + sling.bx) * 0.5;
      const midY = (sling.ay + sling.by) * 0.5;
      const enemy = engine.wrestlers.find(w => w.team !== sling.team && w.alive && Math.hypot(w.x - midX, w.y - midY) < w.radius * 1.45);
      if (enemy) {
        const player = engine.wrestlers.find(w => w.team === sling.team && w.alive);
        const ang = player ? Math.atan2(enemy.y - player.y, enemy.x - player.x) : Math.atan2(enemy.y - engine.ringCenterY, enemy.x - engine.ringCenterX);
        const spd = (engine.ringRadius * 2.4) / enemy.mass;
        enemy.vx += Math.cos(ang) * spd;
        enemy.vy += Math.sin(ang) * spd;
        enemy.shockTimer = 0.5;
        enemy.squashX = 0.7; enemy.squashY = 1.35;
        addPopup("PINCH!", enemy.x, enemy.y - enemy.radius, "#2BD4EA", 1.15);
        spawnConfetti(enemy.x, enemy.y, 10, enemy.arch.base);
        audio.playSnap(1.0);
        audio.vibrate(35);
        sling.reset();
      }
    }
  }

  function releasePointer(sling, id) {
    if (sling.pointerIdA !== id && sling.pointerIdB !== id) return;
    const player = engine.wrestlers.find(w => w.team === sling.team && w.alive);

    if (sling.activeCount === 2 && player) {
      const anchorX = sling.pointerIdA === id ? sling.bx : sling.ax;
      const anchorY = sling.pointerIdA === id ? sling.by : sling.ay;
      const stretch = Math.hypot(sling.bx - sling.ax, sling.by - sling.ay);
      const normPower = Math.max(0.25, Math.min(1.35, stretch / (engine.ringRadius * 0.95)));
      const ang = Math.atan2(anchorY - player.y, anchorX - player.x);
      const spd = engine.ringRadius * 2.35 * normPower;
      player.vx += Math.cos(ang) * spd;
      player.vy += Math.sin(ang) * spd;
      player.squashX = 1.32; player.squashY = 0.74;
      audio.playSnap(0.95);
      audio.vibrate(25);

      if (sling.pointerIdA === id) {
        sling.pointerIdA = sling.pointerIdB;
        sling.ax = sling.bx;
        sling.ay = sling.by;
      }
      sling.pointerIdB = null;
      sling.singleStartX = sling.ax;
      sling.singleStartY = sling.ay;
    } else if (sling.activeCount === 1 && player) {
      const pullX = sling.singleStartX - sling.ax;
      const pullY = sling.singleStartY - sling.ay;
      const pullDist = Math.hypot(pullX, pullY);
      if (pullDist > engine.ringRadius * 0.08) {
        const normPower = Math.max(0.2, Math.min(1.3, pullDist / (engine.ringRadius * 0.75)));
        const ang = Math.atan2(pullY, pullX);
        const spd = engine.ringRadius * 2.25 * normPower;
        player.vx += Math.cos(ang) * spd;
        player.vy += Math.sin(ang) * spd;
        player.squashX = 1.28; player.squashY = 0.76;
        audio.playSnap(0.85);
        audio.vibrate(20);
      }
      sling.reset();
    } else {
      sling.reset();
    }
  }

  canvas.addEventListener("pointerdown", (e) => {
    e.preventDefault();
    audio.unlock();
    try { canvas.setPointerCapture(e.pointerId); } catch (_) {}
    const { x, y } = getCanvasCoords(e);

    if (inRect(engine.btnMute, x, y)) {
      audio.muted = !audio.muted;
      audio.playUiTap();
      return;
    }
    if (engine.screenState !== STATE_MENU && inRect(engine.btnHome, x, y)) {
      audio.playUiTap();
      engine.screenState = STATE_MENU;
      setupDemoRing();
      return;
    }

    if (engine.screenState === STATE_MENU) {
      if (inRect(engine.btnSolo, x, y)) startNewGame(MODE_SOLO);
      else if (inRect(engine.btnTwoPlayer, x, y)) startNewGame(MODE_2P);
      return;
    }

    if (engine.screenState === STATE_GAME_OVER) {
      if (inRect(engine.btnPlayAgain, x, y)) startNewGame(engine.gameMode);
      return;
    }

    if (engine.screenState !== STATE_PLAYING) return;
    engine.hasTouchedInPlay = true;

    const sling = selectSlingshot(y);
    if (sling.pointerIdA === null) {
      sling.pointerIdA = e.pointerId;
      sling.ax = x; sling.ay = y;
      sling.singleStartX = x; sling.singleStartY = y;
      sling.lastTickDistance = 0;
    } else if (sling.pointerIdB === null) {
      sling.pointerIdB = e.pointerId;
      sling.bx = x; sling.by = y;
      sling.lastTickDistance = Math.hypot(sling.bx - sling.ax, sling.by - sling.ay);
    }
  });

  canvas.addEventListener("pointermove", (e) => {
    if (engine.screenState !== STATE_PLAYING) return;
    const { x, y } = getCanvasCoords(e);
    updateMove(engine.slingshotP1, e.pointerId, x, y);
    updateMove(engine.slingshotP2, e.pointerId, x, y);
  });

  const onPointerEnd = (e) => {
    releasePointer(engine.slingshotP1, e.pointerId);
    releasePointer(engine.slingshotP2, e.pointerId);
  };
  canvas.addEventListener("pointerup", onPointerEnd);
  canvas.addEventListener("pointercancel", onPointerEnd);

  // ---------------------------------------------------------------------------
  // Canvas Renderer
  // ---------------------------------------------------------------------------
  function roundRectPath(x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  }

  function render() {
    const w = engine.width;
    const h = engine.height;
    const cx = engine.ringCenterX;
    const cy = engine.ringCenterY;
    const r = engine.ringRadius;

    // 1. Charcoal mat background
    const bgGrad = ctx.createRadialGradient(cx, cy, r * 0.2, cx, cy, r * 1.9);
    bgGrad.addColorStop(0, "#2A313A");
    bgGrad.addColorStop(1, "#191D22");
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, w, h);

    ctx.save();
    ctx.translate(engine.shakeX, engine.shakeY);

    // 2. Cardboard & Wood Ring
    ctx.fillStyle = "rgba(0,0,0,0.45)";
    ctx.beginPath();
    ctx.arc(cx, cy + r * 0.045, r * 1.09, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = "#A87648";
    ctx.beginPath();
    ctx.arc(cx, cy, r * 1.07, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = "#C6905C";
    ctx.beginPath();
    ctx.arc(cx, cy, r * 1.02, 0, Math.PI * 2);
    ctx.fill();

    const woodGrad = ctx.createRadialGradient(cx, cy, r * 0.05, cx, cy, r);
    woodGrad.addColorStop(0, "#F3C892");
    woodGrad.addColorStop(1, "#DAA56B");
    ctx.fillStyle = woodGrad;
    ctx.beginPath();
    ctx.arc(cx, cy, r * 0.96, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = "rgba(140,82,32,0.16)";
    ctx.lineWidth = 2;
    ctx.beginPath(); ctx.arc(cx, cy, r * 0.72, 0, Math.PI * 2); ctx.stroke();
    ctx.beginPath(); ctx.arc(cx, cy, r * 0.44, 0, Math.PI * 2); ctx.stroke();

    // Masking tape strips
    ctx.fillStyle = "rgba(245,232,204,0.68)";
    const tapeW = r * 0.11;
    const tapeH = r * 0.16;
    for (let i = 0; i < 4; i++) {
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate((i * Math.PI) / 2);
      ctx.fillRect(-tapeW * 0.5, -r * 1.03, tapeW, tapeH);
      ctx.restore();
    }

    if (engine.gameMode === MODE_2P) {
      ctx.save();
      ctx.setLineDash([12, 10]);
      ctx.strokeStyle = "rgba(110,68,30,0.3)";
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(cx - r * 0.88, cy);
      ctx.lineTo(cx + r * 0.88, cy);
      ctx.stroke();
      ctx.restore();
    }

    // 3. Elastic Slingshot
    if (engine.screenState === STATE_PLAYING) {
      drawSlingshot(engine.slingshotP1);
      if (engine.gameMode === MODE_2P) drawSlingshot(engine.slingshotP2);
    }

    // 4. Origami Wrestlers
    for (const wr of engine.wrestlers) drawWrestler(wr);

    // 5. Rubber-band Ropes & Posts
    drawRopesAndPosts();

    // 6. Confetti & Popups
    for (const p of engine.particles) {
      ctx.save();
      ctx.globalAlpha = Math.max(0, p.life / p.maxLife);
      ctx.translate(p.x, p.y);
      ctx.rotate((p.angle * Math.PI) / 180);
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.moveTo(0, -p.size);
      ctx.lineTo(p.size * 0.85, p.size * 0.75);
      ctx.lineTo(-p.size * 0.85, p.size * 0.75);
      ctx.closePath();
      ctx.fill();
      ctx.restore();
    }

    for (const pop of engine.popups) {
      const alpha = Math.min(1, (pop.life / pop.maxLife) * 2);
      ctx.save();
      ctx.globalAlpha = alpha;
      ctx.font = `900 ${Math.round(r * 0.14 * pop.scale)}px system-ui, sans-serif`;
      ctx.textAlign = "center";
      ctx.fillStyle = "#111827";
      ctx.fillText(pop.text, pop.x + 3, pop.y + 4);
      ctx.fillStyle = pop.color;
      ctx.fillText(pop.text, pop.x, pop.y);
      ctx.restore();
    }

    ctx.restore();

    // 7. HUD & Overlays
    drawHud();
    if (engine.screenState === STATE_MENU) drawMenu();
    else if (engine.screenState === STATE_ROUND_OVER) drawRoundBanner();
    else if (engine.screenState === STATE_GAME_OVER) drawGameOver();
    else if (engine.screenState === STATE_PLAYING && !engine.hasTouchedInPlay && engine.wave === 1) {
      drawTutorialHint();
    }
  }

  function drawSlingshot(sling) {
    if (sling.activeCount === 0) return;
    const player = engine.wrestlers.find(w => w.team === sling.team && w.alive);
    if (!player) return;
    const touchR = engine.ringRadius * 0.14;

    if (sling.activeCount === 2) {
      ctx.save();
      ctx.setLineDash([14, 10]);
      ctx.strokeStyle = "rgba(254,240,138,0.85)";
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.moveTo(sling.ax, sling.ay);
      ctx.lineTo(sling.bx, sling.by);
      ctx.stroke();
      ctx.restore();

      ctx.strokeStyle = "#FEF9C3";
      ctx.lineWidth = 7;
      ctx.lineCap = "round";
      ctx.beginPath();
      ctx.moveTo(player.x, player.y); ctx.lineTo(sling.ax, sling.ay);
      ctx.moveTo(player.x, player.y); ctx.lineTo(sling.bx, sling.by);
      ctx.stroke();

      drawTouchRing(sling.ax, sling.ay, touchR);
      drawTouchRing(sling.bx, sling.by, touchR);

      const midX = (sling.ax + sling.bx) * 0.5;
      const midY = (sling.ay + sling.by) * 0.5;
      drawAimArrow(player.x, player.y, Math.atan2(midY - player.y, midX - player.x), engine.ringRadius * 0.38);
    } else {
      const pullX = sling.singleStartX - sling.ax;
      const pullY = sling.singleStartY - sling.ay;
      const pullDist = Math.hypot(pullX, pullY);

      ctx.strokeStyle = "#FEF9C3";
      ctx.lineWidth = 6;
      ctx.lineCap = "round";
      ctx.beginPath();
      ctx.moveTo(player.x, player.y); ctx.lineTo(sling.ax, sling.ay);
      ctx.stroke();

      drawTouchRing(sling.ax, sling.ay, touchR * 0.9);
      if (pullDist > engine.ringRadius * 0.08) {
        drawAimArrow(player.x, player.y, Math.atan2(pullY, pullX), Math.min(pullDist, engine.ringRadius * 0.42));
      }
    }
  }

  function drawTouchRing(x, y, r) {
    ctx.fillStyle = "rgba(255,255,255,0.2)";
    ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = "rgba(255,255,255,0.85)";
    ctx.lineWidth = 3.5;
    ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.stroke();
    ctx.strokeStyle = "rgba(255,255,255,0.45)";
    ctx.lineWidth = 2;
    ctx.beginPath(); ctx.arc(x, y, r * 0.65, 0, Math.PI * 2); ctx.stroke();
  }

  function drawAimArrow(x, y, ang, len) {
    const sx = x + Math.cos(ang) * (len * 0.38);
    const sy = y + Math.sin(ang) * (len * 0.38);
    const tx = x + Math.cos(ang) * (len * 1.15);
    const ty = y + Math.sin(ang) * (len * 1.15);

    ctx.strokeStyle = "#FEF08A";
    ctx.lineWidth = 6;
    ctx.lineCap = "round";
    ctx.beginPath();
    ctx.moveTo(sx, sy); ctx.lineTo(tx, ty);
    const head = len * 0.28;
    ctx.moveTo(tx, ty); ctx.lineTo(tx + Math.cos(ang + 2.55) * head, ty + Math.sin(ang + 2.55) * head);
    ctx.moveTo(tx, ty); ctx.lineTo(tx + Math.cos(ang - 2.55) * head, ty + Math.sin(ang - 2.55) * head);
    ctx.stroke();
  }

  function drawWrestler(w) {
    if (w.fallAlpha <= 0) return;
    ctx.save();
    ctx.globalAlpha = w.fallAlpha;
    ctx.translate(w.x, w.y);
    if (!w.alive) ctx.scale(w.fallScale, w.fallScale);
    else ctx.scale(w.squashX, w.squashY);
    ctx.rotate((w.angle * Math.PI) / 180);

    const r = w.radius;

    // Shadow
    ctx.fillStyle = "rgba(0,0,0,0.35)";
    ctx.beginPath(); ctx.arc(r * 0.1, r * 0.16, r * 0.96, 0, Math.PI * 2); ctx.fill();

    // Folded body
    ctx.fillStyle = w.arch.base;
    ctx.beginPath();
    ctx.moveTo(-r * 0.72, -r * 0.45);
    ctx.lineTo(0, -r * 0.92);
    ctx.lineTo(r * 0.72, -r * 0.45);
    ctx.lineTo(r * 0.86, r * 0.32);
    ctx.lineTo(r * 0.38, r * 0.85);
    ctx.lineTo(-r * 0.38, r * 0.85);
    ctx.lineTo(-r * 0.86, r * 0.32);
    ctx.closePath();
    ctx.fill();

    // Origami top fold
    ctx.fillStyle = w.arch.fold;
    ctx.beginPath();
    ctx.moveTo(-r * 0.72, -r * 0.45);
    ctx.lineTo(0, -r * 0.92);
    ctx.lineTo(r * 0.72, -r * 0.45);
    ctx.lineTo(0, r * 0.08);
    ctx.closePath();
    ctx.fill();

    // Crease lines
    ctx.strokeStyle = "rgba(17,24,39,0.7)";
    ctx.lineWidth = Math.max(2, r * 0.06);
    ctx.beginPath();
    ctx.moveTo(-r * 0.72, -r * 0.45); ctx.lineTo(0, r * 0.08);
    ctx.lineTo(r * 0.72, -r * 0.45);
    ctx.stroke();

    // Googly eyes
    const eyeR = r * 0.21;
    const eyeY = -r * 0.24;
    const eyeOff = r * 0.27;
    const gazeX = Math.max(-0.45, Math.min(0.45, w.vx / (engine.ringRadius * 2))) * eyeR;
    const gazeY = Math.max(-0.45, Math.min(0.45, w.vy / (engine.ringRadius * 2))) * eyeR;
    const shock = w.shockTimer > 0 ? 1.25 : 1;

    ctx.fillStyle = "#FFFFFF";
    ctx.beginPath(); ctx.arc(-eyeOff, eyeY, eyeR * shock, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(eyeOff, eyeY, eyeR * shock, 0, Math.PI * 2); ctx.fill();

    ctx.fillStyle = "#111827";
    ctx.beginPath(); ctx.arc(-eyeOff + gazeX, eyeY + gazeY, eyeR * 0.48, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(eyeOff + gazeX, eyeY + gazeY, eyeR * 0.48, 0, Math.PI * 2); ctx.fill();

    ctx.restore();
  }

  function drawRopesAndPosts() {
    const cx = engine.ringCenterX;
    const cy = engine.ringCenterY;
    const r = engine.ringRadius * 0.98;
    const angles = [45, 135, 225, 315];
    const posts = angles.map(a => {
      const rad = (a * Math.PI) / 180;
      return { x: cx + Math.cos(rad) * r, y: cy + Math.sin(rad) * r };
    });

    for (let i = 0; i < 4; i++) {
      const prev = posts[(i + 3) % 4];
      const curr = posts[i];
      const midRad = (i * Math.PI) / 2;
      const ctrlR = r * 1.15 + engine.ropeFlexes[i].flexOffset * 1.8;
      const ctrlX = cx + Math.cos(midRad) * ctrlR;
      const ctrlY = cy + Math.sin(midRad) * ctrlR;

      ctx.strokeStyle = "rgba(245,238,224,0.75)";
      ctx.lineWidth = r * 0.022;
      ctx.lineCap = "round";
      ctx.beginPath();
      ctx.moveTo(prev.x, prev.y);
      ctx.quadraticCurveTo(ctrlX, ctrlY, curr.x, curr.y);
      ctx.stroke();

      ctx.strokeStyle = "rgba(255,255,255,0.45)";
      ctx.lineWidth = r * 0.008;
      ctx.stroke();
    }

    const ps = r * 0.085;
    for (const p of posts) {
      ctx.fillStyle = "rgba(0,0,0,0.35)";
      ctx.fillRect(p.x - ps * 0.5 + 3, p.y - ps * 0.5 + 4, ps, ps);
      ctx.fillStyle = "#9A6C42";
      ctx.fillRect(p.x - ps * 0.5, p.y - ps * 0.5, ps, ps);
      ctx.fillStyle = "#C29262";
      ctx.fillRect(p.x - ps * 0.36, p.y - ps * 0.36, ps * 0.72, ps * 0.72);
    }
  }

  function drawPaperTag(x, y, w, h, stripe, label, value) {
    ctx.fillStyle = "rgba(0,0,0,0.38)";
    ctx.fillRect(x + 4, y + 6, w, h);
    ctx.fillStyle = "#F6F4EE";
    ctx.fillRect(x, y, w, h);
    ctx.fillStyle = stripe;
    ctx.fillRect(x, y, w, h * 0.16);

    ctx.textAlign = "center";
    ctx.fillStyle = stripe;
    ctx.font = `800 ${Math.round(h * 0.24)}px system-ui, sans-serif`;
    ctx.fillText(label, x + w * 0.5, y + h * 0.46);

    ctx.fillStyle = "#1E293B";
    ctx.font = `900 ${Math.round(h * 0.42)}px system-ui, sans-serif`;
    ctx.fillText(value, x + w * 0.5, y + h * 0.87);
  }

  function drawHud() {
    const w = engine.width;
    const h = engine.height;
    const topY = h * 0.03;
    const tagW = w * 0.25;
    const tagH = h * 0.095;

    if (engine.screenState !== STATE_MENU) {
      drawPaperTag(w * 0.04, topY, tagW, tagH, "#0FB5CC", "YOU", String(engine.scoreP1));
      const cTitle = engine.gameMode === MODE_SOLO ? "WAVE" : "FIRST TO 5";
      const cVal = engine.gameMode === MODE_SOLO ? String(engine.wave) : "VS";
      drawPaperTag(w * 0.5 - tagW * 0.42, topY, tagW * 0.84, tagH * 0.86, "#94A3B8", cTitle, cVal);
      drawPaperTag(w - w * 0.04 - tagW, topY, tagW, tagH, "#F25F3A", engine.gameMode === MODE_SOLO ? "CPU" : "P2", String(engine.scoreP2));
    }

    const btnW = w * 0.22;
    const btnH = h * 0.05;
    const btnY = h - btnH - h * 0.028;

    engine.btnMute = { x: w * 0.05, y: btnY, w: btnW, h: btnH };
    drawSmallBtn(engine.btnMute, audio.muted ? "SFX: OFF" : "SFX: ON");

    if (engine.screenState !== STATE_MENU) {
      engine.btnHome = { x: w - w * 0.05 - btnW, y: btnY, w: btnW, h: btnH };
      drawSmallBtn(engine.btnHome, "MENU");
    }
  }

  function drawSmallBtn(r, text) {
    ctx.fillStyle = "#334155";
    roundRectPath(r.x, r.y, r.w, r.h, 12);
    ctx.fill();
    ctx.strokeStyle = "rgba(255,255,255,0.35)";
    ctx.lineWidth = 1.5;
    ctx.stroke();

    ctx.fillStyle = "#FFFFFF";
    ctx.textAlign = "center";
    ctx.font = `700 ${Math.round(r.h * 0.4)}px system-ui, sans-serif`;
    ctx.fillText(text, r.x + r.w * 0.5, r.y + r.h * 0.65);
  }

  function drawActionCard(r, label, accent) {
    ctx.fillStyle = "rgba(0,0,0,0.45)";
    roundRectPath(r.x + 4, r.y + 6, r.w, r.h, 14);
    ctx.fill();

    ctx.fillStyle = "#F6F4EE";
    roundRectPath(r.x, r.y, r.w, r.h, 14);
    ctx.fill();

    ctx.fillStyle = accent;
    roundRectPath(r.x, r.y, r.w * 0.08, r.h, 10);
    ctx.fill();

    ctx.fillStyle = "#0F172A";
    ctx.textAlign = "center";
    ctx.font = `900 ${Math.round(r.h * 0.38)}px system-ui, sans-serif`;
    ctx.fillText(label, r.x + r.w * 0.52, r.y + r.h * 0.63);
  }

  function drawMenu() {
    const w = engine.width;
    const h = engine.height;

    ctx.textAlign = "center";
    ctx.font = `900 ${Math.round(w * 0.125)}px system-ui, sans-serif`;
    ctx.fillStyle = "rgba(0,0,0,0.55)";
    ctx.fillText("PINCH & FOLD", w * 0.5 + 3, h * 0.14 + 4);
    ctx.fillStyle = "#FEF08A";
    ctx.fillText("PINCH & FOLD", w * 0.5, h * 0.14);

    ctx.fillStyle = "#CBD5E1";
    ctx.font = `700 ${Math.round(w * 0.034)}px system-ui, sans-serif`;
    ctx.fillText("2-FINGER ELASTIC WRESTLING", w * 0.5, h * 0.185);

    const btnW = w * 0.72;
    const btnH = h * 0.082;
    engine.btnSolo = { x: w * 0.5 - btnW * 0.5, y: h * 0.71, w: btnW, h: btnH };
    drawActionCard(engine.btnSolo, "SOLO WAVES", "#0FB5CC");

    engine.btnTwoPlayer = { x: w * 0.5 - btnW * 0.5, y: h * 0.815, w: btnW, h: btnH };
    drawActionCard(engine.btnTwoPlayer, "2P SAME PHONE", "#F25F3A");

    ctx.fillStyle = "#94A3B8";
    ctx.font = `600 ${Math.round(w * 0.029)}px system-ui, sans-serif`;
    ctx.fillText("Stretch 2 fingers & lift 1 to SNAP • Drag 1 finger to SLING", w * 0.5, h * 0.935);
  }

  function drawTutorialHint() {
    const w = engine.width;
    const h = engine.height;
    const pulse = 0.55 + 0.45 * Math.sin(engine.timeElapsed * 4);
    ctx.save();
    ctx.globalAlpha = pulse;
    ctx.fillStyle = "#FEF08A";
    ctx.textAlign = "center";
    ctx.font = `800 ${Math.round(w * 0.038)}px system-ui, sans-serif`;
    ctx.fillText("STRETCH 2 FINGERS & LIFT 1 TO SNAP!", w * 0.5, h * 0.855);
    ctx.restore();
  }

  function drawRoundBanner() {
    const w = engine.width;
    const h = engine.height;
    ctx.fillStyle = "rgba(15,23,42,0.72)";
    ctx.fillRect(0, h * 0.42, w, h * 0.14);
    ctx.fillStyle = "#FEF08A";
    ctx.textAlign = "center";
    ctx.font = `900 ${Math.round(w * 0.09)}px system-ui, sans-serif`;
    ctx.fillText(engine.roundBannerText, w * 0.5, h * 0.505);
  }

  function drawGameOver() {
    const w = engine.width;
    const h = engine.height;
    ctx.fillStyle = "rgba(15,23,42,0.72)";
    ctx.fillRect(0, 0, w, h);

    const title = engine.gameMode === MODE_SOLO
      ? "KNOCKED OUT!"
      : (engine.scoreP1 >= 5 ? "TEAL WINS!" : "CORAL WINS!");

    ctx.textAlign = "center";
    ctx.fillStyle = "#FEF08A";
    ctx.font = `900 ${Math.round(w * 0.105)}px system-ui, sans-serif`;
    ctx.fillText(title, w * 0.5, h * 0.39);

    const sub = engine.gameMode === MODE_SOLO
      ? `REACHED WAVE ${engine.wave}  •  BEST ${engine.bestWave}`
      : `FINAL SCORE  ${engine.scoreP1} - ${engine.scoreP2}`;
    ctx.fillStyle = "#E2E8F0";
    ctx.font = `700 ${Math.round(w * 0.042)}px system-ui, sans-serif`;
    ctx.fillText(sub, w * 0.5, h * 0.45);

    const btnW = w * 0.66;
    const btnH = h * 0.082;
    engine.btnPlayAgain = { x: w * 0.5 - btnW * 0.5, y: h * 0.54, w: btnW, h: btnH };
    drawActionCard(engine.btnPlayAgain, "PLAY AGAIN", "#0FB5CC");
  }

  // ---------------------------------------------------------------------------
  // Responsive Resize & Main Loop
  // ---------------------------------------------------------------------------
  function resizeCanvas() {
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const targetAspect = 9 / 16;
    let cw = vw;
    let ch = vh;
    if (vw / vh > targetAspect) {
      cw = Math.round(vh * targetAspect);
    } else {
      ch = Math.round(vw / targetAspect);
    }

    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    canvas.style.width = `${cw}px`;
    canvas.style.height = `${ch}px`;
    canvas.width = Math.round(cw * dpr);
    canvas.height = Math.round(ch * dpr);

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    engine.width = cw;
    engine.height = ch;
    engine.ringCenterX = cw * 0.5;
    engine.ringCenterY = ch * 0.53;
    engine.ringRadius = Math.min(cw * 0.43, ch * 0.32);

    if (engine.wrestlers.length === 0) setupDemoRing();
  }

  window.addEventListener("resize", resizeCanvas);
  resizeCanvas();

  let lastTime = performance.now();
  function loop(now) {
    const dt = (now - lastTime) / 1000;
    lastTime = now;
    update(dt);
    render();
    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);
})();
