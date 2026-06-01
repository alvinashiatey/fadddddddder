// Engine_Fadddddddder
// Live-input two-scene crossfader for norns / Crone.
//
// Effect index mapping (SelectX on sceneAEffect / sceneBEffect):
//   0 = thru
//   1 = filter  (RLPF / BPF / RHPF, mode and slope via params)
//   2 = eq      (BPeakEQ parametric)
//   3 = mod     (chorus/flanger DelayC + SinOsc)
//   4 = space   (FreeVerb2)
//   5 = texture (AM ring-mod via SinOsc, tanh saturation)
//   6 = delay   (CombC + LPF tone control)
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
        var aEq, aMod, aSpace, aTexture, aDelay;
        var wetL, wetR;

        amt = Lag.kr(sceneAAmount.clip(0, 1), 0.08);
        p1  = Lag.kr(sceneAParam1.clip(0, 1), 0.08);
        p2  = Lag.kr(sceneAParam2.clip(0, 1), 0.08);
        p3  = Lag.kr(sceneAParam3.clip(0, 1), 0.08);
        p4  = Lag.kr(sceneAParam4.clip(0, 1), 0.08);
        // Do NOT lag the effect index — interpolating between integer slots
        // causes SelectX to blend between unrelated algorithms and produces
        // unpredictable artefacts.  Round to the nearest integer instead.
        eff = sceneAEffect.clip(0, 6).round(1);

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

        wetL = Select.ar(eff, [dry[0], aFilter[0], aEq[0], aMod[0], aSpace[0], aTexture[0], aDelay]);
        wetR = Select.ar(eff, [dry[1], aFilter[1], aEq[1], aMod[1], aSpace[1], aTexture[1], aDelay]);

        // Wet/dry mix: amt 0 = dry, amt 1 = fully wet
        XFade2.ar(dry, [wetL, wetR], (amt * 2) - 1)
      }.value;

      // ---------- scene B --------------------------------------------------
      sceneB = {
        var amt, p1, p2, p3, p4, eff;
        var cutoff, rq, bpRq, lp12, lp24, bp12, bp24, hp12, hp24, mode, bFilter;
        var bEq, bMod, bSpace, bTexture, bDelay;
        var wetL, wetR;

        amt = Lag.kr(sceneBAmount.clip(0, 1), 0.08);
        p1  = Lag.kr(sceneBParam1.clip(0, 1), 0.08);
        p2  = Lag.kr(sceneBParam2.clip(0, 1), 0.08);
        p3  = Lag.kr(sceneBParam3.clip(0, 1), 0.08);
        p4  = Lag.kr(sceneBParam4.clip(0, 1), 0.08);
        eff = sceneBEffect.clip(0, 6).round(1);

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

        wetL = Select.ar(eff, [dry[0], bFilter[0], bEq[0], bMod[0], bSpace[0], bTexture[0], bDelay]);
        wetR = Select.ar(eff, [dry[1], bFilter[1], bEq[1], bMod[1], bSpace[1], bTexture[1], bDelay]);

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
