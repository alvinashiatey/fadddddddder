// Engine_Fadddddddder
// Live-input two-scene crossfader for norns / Crone.
//
// Effect index mapping (SelectX on sceneAEffect / sceneBEffect):
//   0 = thru
//   1 = filter  (RLPF / BPF / RHPF, mode and slope via params)
//   2 = eq      (BPeakEQ parametric)
//   3 = mod     (chorus/flanger DelayC + SinOsc)
//   4 = space   (FreeVerb2)
//   5 = texture    (AM ring-mod via SinOsc, tanh saturation)
//   6 = delay      (CombC + LPF tone control)
//   7 = resonator  (comb / karplus-style metallic ringing)
//   8 = foldFilter (multimode filter with folded resonance path)
//   9 = formant    (stacked bandpasses / vowel-like tone)
//  10 = tremolo    (sine to square rhythmic amplitude gating)
//  11 = crusher    (bit/sample-rate reduction)
//  12 = freqShift  (single-sideband frequency shifter)
//  13 = granular   (live-input grain freeze / scatter)
//
// param1–param4 are always normalised 0–1; each effect branch maps them
// to musically useful ranges internally.

Engine_Fadddddddder : CroneEngine {

  var mainStage;
  var inputAmp       = 1;
  var outputAmp      = 1;
  var numInputChannels = 2;
  // Tempo state for delay sync (set via set_bpm / set_delay_sync)
  var currentBpm     = 120;
  var delaySyncMode  = 1;   // 0 = free, 1 = sync

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback)
  }

  alloc {
    SynthDef(\fadddddddderMain, { |
      inL = 0, inR = 1, out = 0,
      inputAmp  = 1, outputAmp = 1,
      xfade     = 0,

      sceneAEffect = 0,
      sceneAAmount = 0, sceneAParam1 = 0.5, sceneAParam2 = 0.5, sceneAParam3 = 0.5, sceneAParam4 = 0.5,

      sceneBEffect = 0,
      sceneBAmount = 0, sceneBParam1 = 0.5, sceneBParam2 = 0.5, sceneBParam3 = 0.5, sceneBParam4 = 0.5,

      // Delay time in seconds, updated by the Lua side when BPM or sync changes.
      delayTime = 0.5
    |
      var dry, xfadeLag, sceneA, sceneB, sig;

      dry = [In.ar(inL), In.ar(inR)] * Lag.kr(inputAmp, 0.05);

      // ---------- scene A --------------------------------------------------
      sceneA = {
        var amt, p1, p2, p3, p4, eff;
        var cutoff, rq, bpRq, lp12, lp24, bp12, bp24, hp12, hp24, mode, aFilter;
        var aEq, aMod, aSpace, aTexture, aDelay, aResonator, aFoldFilter, aFormant, aTremolo, aCrusher, aFreqShift, aGranular;
        var excite, decay, tune, tone, combA, combB, base, folded, foldAmt;
        var formA, formB, formC, spread, focus;
        var tremRate, tremDepth, tremShape, tremBias, tremWave;
        var crushRate, crushBits, crushMix;
        var shiftFreq, shiftSpread;
        var grainDensity, grainSize, grainScatter, grainRate, grainPan;
        var wetL, wetR;

        amt = Lag.kr(sceneAAmount.clip(0, 1), 0.08);
        p1  = Lag.kr(sceneAParam1.clip(0, 1), 0.08);
        p2  = Lag.kr(sceneAParam2.clip(0, 1), 0.08);
        p3  = Lag.kr(sceneAParam3.clip(0, 1), 0.08);
        p4  = Lag.kr(sceneAParam4.clip(0, 1), 0.08);
        // Do NOT lag the effect index — interpolating between integer slots
        // causes SelectX to blend between unrelated algorithms and produces
        // unpredictable artefacts.  Round to the nearest integer instead.
        eff = sceneAEffect.clip(0, 13).round(1);

        // filter: cutoff, resonance, mode (lp/bp/hp), slope (12/24dB)
        cutoff = LinExp.kr(p1, 0, 1, 45, 12000);
        rq     = LinLin.kr(p2, 0, 1, 0.9, 0.06);
        bpRq   = rq.clip(0.08, 1.0);
        lp12   = RLPF.ar(dry, cutoff, rq);
        lp24   = RLPF.ar(lp12, cutoff, rq);
        bp12   = BPF.ar(dry, cutoff, bpRq);
        bp24   = BPF.ar(bp12, cutoff, bpRq);
        hp12   = RHPF.ar(dry, cutoff, rq);
        hp24   = RHPF.ar(hp12, cutoff, rq);
        mode   = p3 * 2;   // 0 = LP, 1 = BP, 2 = HP (continuous)
        aFilter = [
          SelectX.ar(mode, [XFade2.ar(lp12[0], lp24[0], (p4 * 2) - 1), XFade2.ar(bp12[0], bp24[0], (p4 * 2) - 1), XFade2.ar(hp12[0], hp24[0], (p4 * 2) - 1)]),
          SelectX.ar(mode, [XFade2.ar(lp12[1], lp24[1], (p4 * 2) - 1), XFade2.ar(bp12[1], bp24[1], (p4 * 2) - 1), XFade2.ar(hp12[1], hp24[1], (p4 * 2) - 1)])
        ];

        // eq: freq, gain, Q (p4 unused / reserved)
        aEq = BPeakEQ.ar(dry,
          LinExp.kr(p1, 0, 1, 90, 9000),
          LinExp.kr(p3, 0, 1, 0.4, 5),
          LinLin.kr(p2, 0, 1, -15, 15)
        );

        // mod: chorus/flanger — p1=rate, p2=depth, p3/p4 reserved
        aMod = DelayC.ar(dry, 0.06,
          SinOsc.kr(
            LinLin.kr(p1, 0, 1, 0.05, 4),
            [0, 1.5708],
            LinLin.kr(p2, 0, 1, 0.001, 0.018),
            0.006
          )
        );

        // space: FreeVerb2 — p1=room size, p2=damping
        aSpace = FreeVerb2.ar(dry[0], dry[1], 1,
          LinLin.kr(p1, 0, 1, 0.15, 0.95),
          LinLin.kr(p2, 0, 1, 0.1,  0.85)
        );

        // texture: AM ring mod with tanh soft clip + DC removal
        // p1 maps modulator frequency; DC from one-sided AM is removed per stage.
        aTexture = LeakDC.ar(
          (dry * SinOsc.ar(LinExp.kr(p1, 0, 1, 18, 2200))).tanh
        );

        // delay: CombC with feedback and tone filter
        // p1=time, p2=feedback, p3=freeze (not wired here, reserved), p4=tone
        aDelay = LPF.ar(
          CombC.ar(dry, 1.2,
            delayTime.clip(0.02, 1.19),
            LinExp.kr(p2.clip(0.001, 1), 0.001, 1, 0.25, 8)
          ),
          LinExp.kr(p4, 0, 1, 900, 12000)
        );

        // resonator: tuned metallic ringing / karplus textures
        tune   = LinExp.kr(p1, 0, 1, 0.003, 0.045);
        decay  = LinLin.kr(p2, 0, 1, 0.25, 6.5);
        excite = LinLin.kr(p3, 0, 1, 0.15, 1.4);
        tone   = LinExp.kr(p4, 0, 1, 900, 12000);
        combA  = CombC.ar(HPF.ar(dry * excite, 50), 0.08, tune, decay);
        combB  = CombC.ar(HPF.ar(dry * (excite * 0.8), 50), 0.08, (tune * 1.37).clip(0.003, 0.079), decay * 0.72);
        aResonator = LeakDC.ar(LPF.ar((combA + (combB * 0.7)).softclip, tone));

        // folded filter: multimode filter plus folded resonance path
        base = [
          SelectX.ar(mode, [lp24[0], bp24[0], hp24[0]]),
          SelectX.ar(mode, [lp24[1], bp24[1], hp24[1]])
        ];
        foldAmt = LinLin.kr(p4, 0, 1, 0.2, 8.0);
        folded = Fold.ar(BPF.ar(dry, cutoff, bpRq) * LinLin.kr(p2, 0, 1, 0.6, 10.0) * foldAmt, -1, 1);
        aFoldFilter = LeakDC.ar((base * 0.8) + (folded * 0.55));

        // formant: 3 staggered bandpasses for vowel-like tone
        formA  = LinExp.kr(p1, 0, 1, 250, 900);
        spread = LinLin.kr(p2, 0, 1, 1.2, 3.1);
        focus  = LinLin.kr(p3, 0, 1, 0.22, 0.05);
        formB  = (formA * spread).clip(400, 2800);
        formC  = (formB * 1.75).clip(700, 5200);
        aFormant = LeakDC.ar(LPF.ar(
          (BPF.ar(dry, formA, focus) * 1.2)
          + (BPF.ar(dry, formB, focus * 0.8) * 0.95)
          + (BPF.ar(dry, formC, focus * 0.65) * 0.7),
          LinExp.kr(p4, 0, 1, 1400, 12000)
        ));

        // tremolo: sine to square amplitude gating
        tremRate  = LinLin.kr(p1, 0, 1, 0.08, 18);
        tremDepth = LinLin.kr(p2, 0, 1, 0.0, 1.0);
        tremShape = p3;
        tremBias  = LinLin.kr(p4, 0, 1, 0.15, 0.85);
        tremWave  = XFade2.kr(SinOsc.kr(tremRate), LFPulse.kr(tremRate, 0, tremBias), (tremShape * 2) - 1).range(0, 1);
        aTremolo  = dry * ((1 - tremDepth) + (tremWave * tremDepth));

        // crusher: sample-rate + bit reduction
        crushRate = LinExp.kr(p1, 0, 1, 800, 22050);
        crushBits = LinLin.kr(p2, 0, 1, 4, 16);
        crushMix  = LinLin.kr(p3, 0, 1, 0.2, 1.0);
        aCrusher  = LPF.ar(Decimator.ar(dry, crushRate, crushBits), LinExp.kr(p4, 0, 1, 1200, 12000)) * crushMix;

        // frequency shifter: subtle shimmer to robot
        shiftFreq   = LinLin.kr(p1, 0, 1, 0.5, 800);
        shiftSpread = LinLin.kr(p2, 0, 1, 0, 20);
        aFreqShift  = LeakDC.ar([
          FreqShift.ar(dry[0], shiftFreq - shiftSpread),
          FreqShift.ar(dry[1], shiftFreq + shiftSpread)
        ]);
        aFreqShift  = LPF.ar(aFreqShift, LinExp.kr(p4, 0, 1, 1500, 12000)) * LinLin.kr(p3, 0, 1, 0.25, 1.0);

        // granular: freeze/scatter live input into grains
        grainDensity = LinExp.kr(p1, 0, 1, 2, 40);
        grainSize    = LinLin.kr(p2, 0, 1, 0.03, 0.22);
        grainScatter = LinLin.kr(p3, 0, 1, 0.0, 1.0);
        grainRate    = LinLin.kr(p3, 0, 1, 0.85, 1.35);
        grainPan     = SinOsc.kr(LinLin.kr(p3, 0, 1, 0.05, 4), [0, 1.5708], grainScatter * 0.75);
        aGranular    = GrainIn.ar(2, Dust.kr(grainDensity), grainSize, dry, grainRate, grainPan, -1, 24);
        aGranular    = LPF.ar(LeakDC.ar(aGranular), LinExp.kr(p4, 0, 1, 1200, 12000));

        wetL = Select.ar(eff, [dry[0], aFilter[0], aEq[0], aMod[0], aSpace[0], aTexture[0], aDelay[0], aResonator[0], aFoldFilter[0], aFormant[0], aTremolo[0], aCrusher[0], aFreqShift[0], aGranular[0]]);
        wetR = Select.ar(eff, [dry[1], aFilter[1], aEq[1], aMod[1], aSpace[1], aTexture[1], aDelay[1], aResonator[1], aFoldFilter[1], aFormant[1], aTremolo[1], aCrusher[1], aFreqShift[1], aGranular[1]]);

        // Wet/dry mix: amt 0 = dry, amt 1 = fully wet
        XFade2.ar(dry, [wetL, wetR], (amt * 2) - 1)
      }.value;

      // ---------- scene B --------------------------------------------------
      sceneB = {
        var amt, p1, p2, p3, p4, eff;
        var cutoff, rq, bpRq, lp12, lp24, bp12, bp24, hp12, hp24, mode, bFilter;
        var bEq, bMod, bSpace, bTexture, bDelay, bResonator, bFoldFilter, bFormant, bTremolo, bCrusher, bFreqShift, bGranular;
        var excite, decay, tune, tone, combA, combB, base, folded, foldAmt;
        var formA, formB, formC, spread, focus;
        var tremRate, tremDepth, tremShape, tremBias, tremWave;
        var crushRate, crushBits, crushMix;
        var shiftFreq, shiftSpread;
        var grainDensity, grainSize, grainScatter, grainRate, grainPan;
        var wetL, wetR;

        amt = Lag.kr(sceneBAmount.clip(0, 1), 0.08);
        p1  = Lag.kr(sceneBParam1.clip(0, 1), 0.08);
        p2  = Lag.kr(sceneBParam2.clip(0, 1), 0.08);
        p3  = Lag.kr(sceneBParam3.clip(0, 1), 0.08);
        p4  = Lag.kr(sceneBParam4.clip(0, 1), 0.08);
        eff = sceneBEffect.clip(0, 13).round(1);

        cutoff = LinExp.kr(p1, 0, 1, 45, 12000);
        rq     = LinLin.kr(p2, 0, 1, 0.9, 0.06);
        bpRq   = rq.clip(0.08, 1.0);
        lp12   = RLPF.ar(dry, cutoff, rq);
        lp24   = RLPF.ar(lp12, cutoff, rq);
        bp12   = BPF.ar(dry, cutoff, bpRq);
        bp24   = BPF.ar(bp12, cutoff, bpRq);
        hp12   = RHPF.ar(dry, cutoff, rq);
        hp24   = RHPF.ar(hp12, cutoff, rq);
        mode   = p3 * 2;
        bFilter = [
          SelectX.ar(mode, [XFade2.ar(lp12[0], lp24[0], (p4 * 2) - 1), XFade2.ar(bp12[0], bp24[0], (p4 * 2) - 1), XFade2.ar(hp12[0], hp24[0], (p4 * 2) - 1)]),
          SelectX.ar(mode, [XFade2.ar(lp12[1], lp24[1], (p4 * 2) - 1), XFade2.ar(bp12[1], bp24[1], (p4 * 2) - 1), XFade2.ar(hp12[1], hp24[1], (p4 * 2) - 1)])
        ];

        bEq = BPeakEQ.ar(dry,
          LinExp.kr(p1, 0, 1, 90, 9000),
          LinExp.kr(p3, 0, 1, 0.4, 5),
          LinLin.kr(p2, 0, 1, -15, 15)
        );

        bMod = DelayC.ar(dry, 0.06,
          SinOsc.kr(
            LinLin.kr(p1, 0, 1, 0.05, 4),
            [0, 1.5708],
            LinLin.kr(p2, 0, 1, 0.001, 0.018),
            0.006
          )
        );

        bSpace = FreeVerb2.ar(dry[0], dry[1], 1,
          LinLin.kr(p1, 0, 1, 0.15, 0.95),
          LinLin.kr(p2, 0, 1, 0.1,  0.85)
        );

        bTexture = LeakDC.ar(
          (dry * SinOsc.ar(LinExp.kr(p1, 0, 1, 18, 2200))).tanh
        );

        bDelay = LPF.ar(
          CombC.ar(dry, 1.2,
            delayTime.clip(0.02, 1.19),
            LinExp.kr(p2.clip(0.001, 1), 0.001, 1, 0.25, 8)
          ),
          LinExp.kr(p4, 0, 1, 900, 12000)
        );

        tune   = LinExp.kr(p1, 0, 1, 0.003, 0.045);
        decay  = LinLin.kr(p2, 0, 1, 0.25, 6.5);
        excite = LinLin.kr(p3, 0, 1, 0.15, 1.4);
        tone   = LinExp.kr(p4, 0, 1, 900, 12000);
        combA  = CombC.ar(HPF.ar(dry * excite, 50), 0.08, tune, decay);
        combB  = CombC.ar(HPF.ar(dry * (excite * 0.8), 50), 0.08, (tune * 1.37).clip(0.003, 0.079), decay * 0.72);
        bResonator = LeakDC.ar(LPF.ar((combA + (combB * 0.7)).softclip, tone));

        base = [
          SelectX.ar(mode, [lp24[0], bp24[0], hp24[0]]),
          SelectX.ar(mode, [lp24[1], bp24[1], hp24[1]])
        ];
        foldAmt = LinLin.kr(p4, 0, 1, 0.2, 8.0);
        folded = Fold.ar(BPF.ar(dry, cutoff, bpRq) * LinLin.kr(p2, 0, 1, 0.6, 10.0) * foldAmt, -1, 1);
        bFoldFilter = LeakDC.ar((base * 0.8) + (folded * 0.55));

        formA  = LinExp.kr(p1, 0, 1, 250, 900);
        spread = LinLin.kr(p2, 0, 1, 1.2, 3.1);
        focus  = LinLin.kr(p3, 0, 1, 0.22, 0.05);
        formB  = (formA * spread).clip(400, 2800);
        formC  = (formB * 1.75).clip(700, 5200);
        bFormant = LeakDC.ar(LPF.ar(
          (BPF.ar(dry, formA, focus) * 1.2)
          + (BPF.ar(dry, formB, focus * 0.8) * 0.95)
          + (BPF.ar(dry, formC, focus * 0.65) * 0.7),
          LinExp.kr(p4, 0, 1, 1400, 12000)
        ));

        tremRate  = LinLin.kr(p1, 0, 1, 0.08, 18);
        tremDepth = LinLin.kr(p2, 0, 1, 0.0, 1.0);
        tremShape = p3;
        tremBias  = LinLin.kr(p4, 0, 1, 0.15, 0.85);
        tremWave  = XFade2.kr(SinOsc.kr(tremRate), LFPulse.kr(tremRate, 0, tremBias), (tremShape * 2) - 1).range(0, 1);
        bTremolo  = dry * ((1 - tremDepth) + (tremWave * tremDepth));

        crushRate = LinExp.kr(p1, 0, 1, 800, 22050);
        crushBits = LinLin.kr(p2, 0, 1, 4, 16);
        crushMix  = LinLin.kr(p3, 0, 1, 0.2, 1.0);
        bCrusher  = LPF.ar(Decimator.ar(dry, crushRate, crushBits), LinExp.kr(p4, 0, 1, 1200, 12000)) * crushMix;

        shiftFreq   = LinLin.kr(p1, 0, 1, 0.5, 800);
        shiftSpread = LinLin.kr(p2, 0, 1, 0, 20);
        bFreqShift  = LeakDC.ar([
          FreqShift.ar(dry[0], shiftFreq - shiftSpread),
          FreqShift.ar(dry[1], shiftFreq + shiftSpread)
        ]);
        bFreqShift  = LPF.ar(bFreqShift, LinExp.kr(p4, 0, 1, 1500, 12000)) * LinLin.kr(p3, 0, 1, 0.25, 1.0);

        grainDensity = LinExp.kr(p1, 0, 1, 2, 40);
        grainSize    = LinLin.kr(p2, 0, 1, 0.03, 0.22);
        grainScatter = LinLin.kr(p3, 0, 1, 0.0, 1.0);
        grainRate    = LinLin.kr(p3, 0, 1, 0.85, 1.35);
        grainPan     = SinOsc.kr(LinLin.kr(p3, 0, 1, 0.05, 4), [0, 1.5708], grainScatter * 0.75);
        bGranular    = GrainIn.ar(2, Dust.kr(grainDensity), grainSize, dry, grainRate, grainPan, -1, 24);
        bGranular    = LPF.ar(LeakDC.ar(bGranular), LinExp.kr(p4, 0, 1, 1200, 12000));

        wetL = Select.ar(eff, [dry[0], bFilter[0], bEq[0], bMod[0], bSpace[0], bTexture[0], bDelay[0], bResonator[0], bFoldFilter[0], bFormant[0], bTremolo[0], bCrusher[0], bFreqShift[0], bGranular[0]]);
        wetR = Select.ar(eff, [dry[1], bFilter[1], bEq[1], bMod[1], bSpace[1], bTexture[1], bDelay[1], bResonator[1], bFoldFilter[1], bFormant[1], bTremolo[1], bCrusher[1], bFreqShift[1], bGranular[1]]);

        XFade2.ar(dry, [wetL, wetR], (amt * 2) - 1)
      }.value;

      // ---------- crossfade and output -------------------------------------
      xfadeLag = Lag.kr(xfade.clip(0, 1), 0.05);
      sig = XFade2.ar(sceneA, sceneB, (xfadeLag * 2) - 1);
      sig = sig * Lag.kr(outputAmp, 0.05);
      Out.ar(out, LeakDC.ar(Limiter.ar(sig, 0.98)));

    }).add;

    context.server.sync;

    mainStage = Synth.new(\fadddddddderMain, [
      \inL,        this.getInL,
      \inR,        this.getInR,
      \out,        context.out_b.index,
      \inputAmp,   inputAmp,
      \outputAmp,  outputAmp,
      \xfade,      0,
      \delayTime,  this.beatToSeconds(currentBpm, 1)
    ], context.xg);

    // Scene A
    this.addCommand("set_scene_a_effect", "f", { |msg| mainStage.set(\sceneAEffect, msg[1]) });
    this.addCommand("set_scene_a_amount", "f", { |msg| mainStage.set(\sceneAAmount, msg[1]) });
    this.addCommand("set_scene_a_param1", "f", { |msg| mainStage.set(\sceneAParam1, msg[1]) });
    this.addCommand("set_scene_a_param2", "f", { |msg| mainStage.set(\sceneAParam2, msg[1]) });
    this.addCommand("set_scene_a_param3", "f", { |msg| mainStage.set(\sceneAParam3, msg[1]) });
    this.addCommand("set_scene_a_param4", "f", { |msg| mainStage.set(\sceneAParam4, msg[1]) });

    // Scene B
    this.addCommand("set_scene_b_effect", "f", { |msg| mainStage.set(\sceneBEffect, msg[1]) });
    this.addCommand("set_scene_b_amount", "f", { |msg| mainStage.set(\sceneBAmount, msg[1]) });
    this.addCommand("set_scene_b_param1", "f", { |msg| mainStage.set(\sceneBParam1, msg[1]) });
    this.addCommand("set_scene_b_param2", "f", { |msg| mainStage.set(\sceneBParam2, msg[1]) });
    this.addCommand("set_scene_b_param3", "f", { |msg| mainStage.set(\sceneBParam3, msg[1]) });
    this.addCommand("set_scene_b_param4", "f", { |msg| mainStage.set(\sceneBParam4, msg[1]) });

    // Crossfader
    this.addCommand("set_xfade", "f", { |msg| mainStage.set(\xfade, msg[1]) });

    // I/O
    this.addCommand("set_input_amp",          "f", { |msg| this.setInputAmp(msg[1]) });
    this.addCommand("set_output_amp",         "f", { |msg| this.setOutputAmp(msg[1]) });
    this.addCommand("set_num_input_channels", "i", { |msg| this.setNumInputChannels(msg[1]) });

    // Delay tempo — set_bpm and set_delay_sync are both required so the Lua
    // param actions don't silently fail.  The engine recomputes delayTime
    // whenever either changes.
    this.addCommand("set_bpm", "f", { |msg|
      currentBpm = msg[1].clip(20, 300);
      this.updateDelayTime;
    });
    this.addCommand("set_delay_sync", "i", { |msg|
      delaySyncMode = msg;
      this.updateDelayTime;
    });
  }

  // -------------------------------------------------------------------------

  beatToSeconds { |bpm, beats|
    ^(beats * 60) / bpm
  }

  updateDelayTime {
    var t;
    if (delaySyncMode == 1) {
      // One beat at the current BPM, clamped to the CombC buffer size.
      t = this.beatToSeconds(currentBpm, 1).clip(0.02, 1.19);
    } {
      // Free mode: half a beat gives a shorter slap-back style time.
      t = this.beatToSeconds(currentBpm, 0.5).clip(0.02, 1.19);
    };
    mainStage.set(\delayTime, t);
  }

  getInL {
    ^context.in_b[0].index
  }

  getInR {
    ^if (numInputChannels == 1) {
      context.in_b[0].index
    } {
      context.in_b[1].index
    }
  }

  setNumInputChannels { |n|
    numInputChannels = n;
    mainStage.set(\inL, this.getInL, \inR, this.getInR);
  }

  setInputAmp { |amp|
    inputAmp = amp;
    mainStage.set(\inputAmp, amp);
  }

  setOutputAmp { |amp|
    outputAmp = amp;
    mainStage.set(\outputAmp, amp);
  }

  free {
    mainStage.free;
  }
}
