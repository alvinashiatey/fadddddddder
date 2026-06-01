Engine_Fadddddddder : CroneEngine {
  var mainStage;
  var inputAmp = 1;
  var outputAmp = 1;
  var numInputChannels = 2;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    SynthDef(\fadddddddderMain, { |
      inL = 0, inR = 1, out = 0,
      inputAmp = 1, outputAmp = 1, xfade = 0, bpm = 120, delaySync = 1,
      sceneAEffect = 0, sceneAAmount = 0, sceneAParam1 = 0.5, sceneAParam2 = 0.5, sceneAParam3 = 0.5, sceneAParam4 = 0.5,
      sceneBEffect = 0, sceneBAmount = 0, sceneBParam1 = 0.5, sceneBParam2 = 0.5, sceneBParam3 = 0.5, sceneBParam4 = 0.5
    |
      var dry, sig,
      aAmt, aP1, aP2, aP3, aP4, aEff, aThru, aFilter, aEq, aMod, aSpace, aTexture, aDelay, aL, aR, sceneA,
      bAmt, bP1, bP2, bP3, bP4, bEff, bThru, bFilter, bEq, bMod, bSpace, bTexture, bDelay, bL, bR, sceneB,
      aCutoff, aRq, aBpRq, aLp12, aLp24, aBp12, aBp24, aHp12, aHp24, aMode,
      bCutoff, bRq, bBpRq, bLp12, bLp24, bBp12, bBp24, bHp12, bHp24, bMode,
      aRate, aDepth, aPh2, aPh6, aPh10, aPhaser, aFlanger, aC1, aC2, aC3, aC4, aC5, aTap3, aTap4, aTap5, aChorus,
      bRate, bDepth, bPh2, bPh6, bPh10, bPhaser, bFlanger, bC1, bC2, bC3, bC4, bC5, bTap3, bTap4, bTap5, bChorus,
      quarter, syncBlend, aDelayFreeTime, aDelaySyncTime, bDelayFreeTime, bDelaySyncTime,
      aComb, aComp, aLimit, aLofi, aFreq, aRing, aMicro, aGrain,
      bComb, bComp, bLimit, bLofi, bFreq, bRing, bMicro, bGrain;

      dry = [In.ar(inL), In.ar(inR)] * Lag.kr(inputAmp, 0.05);
      quarter = 60 / Lag.kr(bpm.clip(20, 300), 0.1);
      syncBlend = Lag.kr(delaySync.clip(0, 1), 0.05);

      aAmt = Lag.kr(sceneAAmount.clip(0, 1), 0.08);
      aP1 = Lag.kr(sceneAParam1.clip(0, 1), 0.08);
      aP2 = Lag.kr(sceneAParam2.clip(0, 1), 0.08);
      aP3 = Lag.kr(sceneAParam3.clip(0, 1), 0.08);
      aP4 = Lag.kr(sceneAParam4.clip(0, 1), 0.08);
      aEff = Lag.kr(sceneAEffect.clip(0, 6), 0.05);

      aThru = dry;
      aCutoff = LinExp.kr(aP1, 0, 1, 45, 12000);
      aRq = LinLin.kr(aP2, 0, 1, 0.9, 0.06);
      aBpRq = Clip.kr(aRq, 0.08, 1.0);
      aLp12 = RLPF.ar(dry, aCutoff, aRq);
      aLp24 = RLPF.ar(aLp12, aCutoff, aRq);
      aBp12 = BPF.ar(dry, aCutoff, aBpRq);
      aBp24 = BPF.ar(aBp12, aCutoff, aBpRq);
      aHp12 = RHPF.ar(dry, aCutoff, aRq);
      aHp24 = RHPF.ar(aHp12, aCutoff, aRq);
      aMode = aP3 * 2;
      aFilter = [
        SelectX.ar(aMode, [XFade2.ar(aLp12[0], aLp24[0], (aP4 * 2) - 1), XFade2.ar(aBp12[0], aBp24[0], (aP4 * 2) - 1), XFade2.ar(aHp12[0], aHp24[0], (aP4 * 2) - 1)]),
        SelectX.ar(aMode, [XFade2.ar(aLp12[1], aLp24[1], (aP4 * 2) - 1), XFade2.ar(aBp12[1], aBp24[1], (aP4 * 2) - 1), XFade2.ar(aHp12[1], aHp24[1], (aP4 * 2) - 1)])
      ];
      aEq = BPeakEQ.ar(dry, LinExp.kr(aP1, 0, 1, 90, 9000), LinExp.kr(aP3, 0, 1, 0.4, 5), LinLin.kr(aP2, 0, 1, -15, 15));
      aRate = LinLin.kr(aP1, 0, 1, 0.04, 5.5);
      aDepth = LinLin.kr(aP2, 0, 1, 0.0008, 0.018);
      aPh2 = AllpassC.ar(dry, 0.04, SinOsc.kr(aRate, 0, aDepth, 0.006), 0.25);
      aPh6 = AllpassC.ar(AllpassC.ar(AllpassC.ar(aPh2, 0.04, SinOsc.kr(aRate * 0.9, 1, aDepth, 0.008), 0.25), 0.04, SinOsc.kr(aRate * 1.1, 2, aDepth, 0.01), 0.25), 0.04, SinOsc.kr(aRate * 1.3, 3, aDepth, 0.012), 0.25);
      aPh10 = AllpassC.ar(AllpassC.ar(aPh6, 0.04, SinOsc.kr(aRate * 1.5, 4, aDepth, 0.014), 0.25), 0.04, SinOsc.kr(aRate * 1.7, 5, aDepth, 0.016), 0.25);
      aPhaser = [
        SelectX.ar(aP4 * 2, [aPh2[0], aPh6[0], aPh10[0]]),
        SelectX.ar(aP4 * 2, [aPh2[1], aPh6[1], aPh10[1]])
      ];
      aFlanger = DelayC.ar(dry, 0.05, SinOsc.kr(aRate, [0, 1.5708], aDepth, 0.004 + aDepth));
      aC1 = DelayC.ar(dry, 0.07, SinOsc.kr(aRate * 0.7, 0, aDepth, 0.011));
      aC2 = DelayC.ar(dry, 0.07, SinOsc.kr(aRate * 0.9, 1, aDepth, 0.017));
      aC3 = DelayC.ar(dry, 0.07, SinOsc.kr(aRate * 1.1, 2, aDepth, 0.023));
      aC4 = DelayC.ar(dry, 0.07, SinOsc.kr(aRate * 1.3, 3, aDepth, 0.031));
      aC5 = DelayC.ar(dry, 0.07, SinOsc.kr(aRate * 1.5, 4, aDepth, 0.041));
      aTap3 = LinLin.kr(aP4, 0, 1, 0, 1);
      aTap4 = LinLin.kr(aP4, 0.33, 1, 0, 1).clip(0, 1);
      aTap5 = LinLin.kr(aP4, 0.66, 1, 0, 1).clip(0, 1);
      aChorus = (aC1 + aC2 + (aC3 * aTap3) + (aC4 * aTap4) + (aC5 * aTap5)) / (2 + aTap3 + aTap4 + aTap5);
      aMod = [
        SelectX.ar(aP3 * 2, [aPhaser[0], aFlanger[0], aChorus[0]]),
        SelectX.ar(aP3 * 2, [aPhaser[1], aFlanger[1], aChorus[1]])
      ];
      aSpace = FreeVerb2.ar(dry[0], dry[1], 1, LinLin.kr(aP1, 0, 1, 0.15, 0.95), LinLin.kr(aP2, 0, 1, 0.1, 0.85));
      aComb = CombC.ar(dry, 0.12, LinExp.kr(aP1, 0, 1, 0.004, 0.09), LinLin.kr(aP2, 0, 1, 0.2, 4.5));
      aComp = Compander.ar(dry, dry, LinLin.kr(aP1, 0, 1, 0.08, 0.6), 1, LinLin.kr(aP2, 0, 1, 0.5, 0.08), 0.01, LinLin.kr(aP4, 0, 1, 0.05, 0.2)) * LinLin.kr(aP2, 0, 1, 1.0, 1.8);
      aLimit = Limiter.ar((dry * LinLin.kr(aP1, 0, 1, 1.0, 2.2)).tanh, LinLin.kr(aP2, 0, 1, 0.95, 0.35), LinLin.kr(aP4, 0, 1, 0.002, 0.04));
      aLofi = Latch.ar(dry, Impulse.ar(LinExp.kr(aP1, 0, 1, 900, 44100)));
      aFreq = LinExp.kr(aP1, 0, 1, 18, 2600);
      aRing = dry * SinOsc.ar(aFreq + SinOsc.kr(LinLin.kr(aP4, 0, 1, 0.1, 8), 0, aFreq * 0.15));
      aMicro = {
        var time, rate, depth, tapA, tapB, color;
        time = LinExp.kr(aP1, 0, 1, 0.22, 0.012) * LinLin.kr(aAmt, 0, 1, 1.0, 0.55);
        rate = LinLin.kr(aP4, 0, 1, 0.2, 12);
        depth = time * LinLin.kr(aAmt, 0, 1, 0.04, 0.5);
        tapA = DelayC.ar(dry, 0.25, Clip.kr(time + SinOsc.kr(rate, 0, depth), 0.005, 0.24));
        tapB = DelayC.ar(dry, 0.25, Clip.kr((time * 0.62) + 0.004, 0.005, 0.24));
        color = LinExp.kr(aP2, 0, 1, 11000, 1700);
        LPF.ar((tapA + tapB) * 0.65, color).softclip;
      }.value;
      aGrain = GrainIn.ar(2, Dust.kr(LinExp.kr(aP1, 0, 1, 3, 45)), LinLin.kr(aP2, 0, 1, 0.03, 0.22), dry, SinOsc.kr(LinLin.kr(aP4, 0, 1, 0.03, 1.5)), -1, 24);
      aTexture = [
        SelectX.ar(aP3 * 6, [aComb[0], aComp[0], aLimit[0], aLofi[0], aRing[0], aMicro[0], aGrain[0]]),
        SelectX.ar(aP3 * 6, [aComb[1], aComp[1], aLimit[1], aLofi[1], aRing[1], aMicro[1], aGrain[1]])
      ];
      aDelayFreeTime = LinExp.kr(aP1, 0, 1, 0.05, 0.9);
      aDelaySyncTime = Select.kr((aP1 * 7.999).floor.clip(0, 7), [quarter * 0.25, quarter * 0.5, quarter * 0.75, quarter * (2 / 3), quarter, quarter * 1.5, quarter * 2, quarter * 4]);
      aDelay = LPF.ar(CombC.ar(dry, 4.5, SelectX.kr(syncBlend, [aDelayFreeTime, aDelaySyncTime]), LinExp.kr(aP2.clip(0.001, 1), 0.001, 1, 0.25, 8)), LinExp.kr(aP4, 0, 1, 900, 12000));
      aL = SelectX.ar(aEff, [aThru[0], aFilter[0], aEq[0], aMod[0], aSpace[0], aTexture[0], aDelay[0]]);
      aR = SelectX.ar(aEff, [aThru[1], aFilter[1], aEq[1], aMod[1], aSpace[1], aTexture[1], aDelay[1]]);
      sceneA = XFade2.ar(dry, [aL, aR], (aAmt * 2) - 1);

      bAmt = Lag.kr(sceneBAmount.clip(0, 1), 0.08);
      bP1 = Lag.kr(sceneBParam1.clip(0, 1), 0.08);
      bP2 = Lag.kr(sceneBParam2.clip(0, 1), 0.08);
      bP3 = Lag.kr(sceneBParam3.clip(0, 1), 0.08);
      bP4 = Lag.kr(sceneBParam4.clip(0, 1), 0.08);
      bEff = Lag.kr(sceneBEffect.clip(0, 6), 0.05);

      bThru = dry;
      bCutoff = LinExp.kr(bP1, 0, 1, 45, 12000);
      bRq = LinLin.kr(bP2, 0, 1, 0.9, 0.06);
      bBpRq = Clip.kr(bRq, 0.08, 1.0);
      bLp12 = RLPF.ar(dry, bCutoff, bRq);
      bLp24 = RLPF.ar(bLp12, bCutoff, bRq);
      bBp12 = BPF.ar(dry, bCutoff, bBpRq);
      bBp24 = BPF.ar(bBp12, bCutoff, bBpRq);
      bHp12 = RHPF.ar(dry, bCutoff, bRq);
      bHp24 = RHPF.ar(bHp12, bCutoff, bRq);
      bMode = bP3 * 2;
      bFilter = [
        SelectX.ar(bMode, [XFade2.ar(bLp12[0], bLp24[0], (bP4 * 2) - 1), XFade2.ar(bBp12[0], bBp24[0], (bP4 * 2) - 1), XFade2.ar(bHp12[0], bHp24[0], (bP4 * 2) - 1)]),
        SelectX.ar(bMode, [XFade2.ar(bLp12[1], bLp24[1], (bP4 * 2) - 1), XFade2.ar(bBp12[1], bBp24[1], (bP4 * 2) - 1), XFade2.ar(bHp12[1], bHp24[1], (bP4 * 2) - 1)])
      ];
      bEq = BPeakEQ.ar(dry, LinExp.kr(bP1, 0, 1, 90, 9000), LinExp.kr(bP3, 0, 1, 0.4, 5), LinLin.kr(bP2, 0, 1, -15, 15));
      bRate = LinLin.kr(bP1, 0, 1, 0.04, 5.5);
      bDepth = LinLin.kr(bP2, 0, 1, 0.0008, 0.018);
      bPh2 = AllpassC.ar(dry, 0.04, SinOsc.kr(bRate, 0, bDepth, 0.006), 0.25);
      bPh6 = AllpassC.ar(AllpassC.ar(AllpassC.ar(bPh2, 0.04, SinOsc.kr(bRate * 0.9, 1, bDepth, 0.008), 0.25), 0.04, SinOsc.kr(bRate * 1.1, 2, bDepth, 0.01), 0.25), 0.04, SinOsc.kr(bRate * 1.3, 3, bDepth, 0.012), 0.25);
      bPh10 = AllpassC.ar(AllpassC.ar(bPh6, 0.04, SinOsc.kr(bRate * 1.5, 4, bDepth, 0.014), 0.25), 0.04, SinOsc.kr(bRate * 1.7, 5, bDepth, 0.016), 0.25);
      bPhaser = [
        SelectX.ar(bP4 * 2, [bPh2[0], bPh6[0], bPh10[0]]),
        SelectX.ar(bP4 * 2, [bPh2[1], bPh6[1], bPh10[1]])
      ];
      bFlanger = DelayC.ar(dry, 0.05, SinOsc.kr(bRate, [0, 1.5708], bDepth, 0.004 + bDepth));
      bC1 = DelayC.ar(dry, 0.07, SinOsc.kr(bRate * 0.7, 0, bDepth, 0.011));
      bC2 = DelayC.ar(dry, 0.07, SinOsc.kr(bRate * 0.9, 1, bDepth, 0.017));
      bC3 = DelayC.ar(dry, 0.07, SinOsc.kr(bRate * 1.1, 2, bDepth, 0.023));
      bC4 = DelayC.ar(dry, 0.07, SinOsc.kr(bRate * 1.3, 3, bDepth, 0.031));
      bC5 = DelayC.ar(dry, 0.07, SinOsc.kr(bRate * 1.5, 4, bDepth, 0.041));
      bTap3 = LinLin.kr(bP4, 0, 1, 0, 1);
      bTap4 = LinLin.kr(bP4, 0.33, 1, 0, 1).clip(0, 1);
      bTap5 = LinLin.kr(bP4, 0.66, 1, 0, 1).clip(0, 1);
      bChorus = (bC1 + bC2 + (bC3 * bTap3) + (bC4 * bTap4) + (bC5 * bTap5)) / (2 + bTap3 + bTap4 + bTap5);
      bMod = [
        SelectX.ar(bP3 * 2, [bPhaser[0], bFlanger[0], bChorus[0]]),
        SelectX.ar(bP3 * 2, [bPhaser[1], bFlanger[1], bChorus[1]])
      ];
      bSpace = FreeVerb2.ar(dry[0], dry[1], 1, LinLin.kr(bP1, 0, 1, 0.15, 0.95), LinLin.kr(bP2, 0, 1, 0.1, 0.85));
      bComb = CombC.ar(dry, 0.12, LinExp.kr(bP1, 0, 1, 0.004, 0.09), LinLin.kr(bP2, 0, 1, 0.2, 4.5));
      bComp = Compander.ar(dry, dry, LinLin.kr(bP1, 0, 1, 0.08, 0.6), 1, LinLin.kr(bP2, 0, 1, 0.5, 0.08), 0.01, LinLin.kr(bP4, 0, 1, 0.05, 0.2)) * LinLin.kr(bP2, 0, 1, 1.0, 1.8);
      bLimit = Limiter.ar((dry * LinLin.kr(bP1, 0, 1, 1.0, 2.2)).tanh, LinLin.kr(bP2, 0, 1, 0.95, 0.35), LinLin.kr(bP4, 0, 1, 0.002, 0.04));
      bLofi = Latch.ar(dry, Impulse.ar(LinExp.kr(bP1, 0, 1, 900, 44100)));
      bFreq = LinExp.kr(bP1, 0, 1, 18, 2600);
      bRing = dry * SinOsc.ar(bFreq + SinOsc.kr(LinLin.kr(bP4, 0, 1, 0.1, 8), 0, bFreq * 0.15));
      bMicro = {
        var time, rate, depth, tapA, tapB, color;
        time = LinExp.kr(bP1, 0, 1, 0.22, 0.012) * LinLin.kr(bAmt, 0, 1, 1.0, 0.55);
        rate = LinLin.kr(bP4, 0, 1, 0.2, 12);
        depth = time * LinLin.kr(bAmt, 0, 1, 0.04, 0.5);
        tapA = DelayC.ar(dry, 0.25, Clip.kr(time + SinOsc.kr(rate, 0, depth), 0.005, 0.24));
        tapB = DelayC.ar(dry, 0.25, Clip.kr((time * 0.62) + 0.004, 0.005, 0.24));
        color = LinExp.kr(bP2, 0, 1, 11000, 1700);
        LPF.ar((tapA + tapB) * 0.65, color).softclip;
      }.value;
      bGrain = GrainIn.ar(2, Dust.kr(LinExp.kr(bP1, 0, 1, 3, 45)), LinLin.kr(bP2, 0, 1, 0.03, 0.22), dry, SinOsc.kr(LinLin.kr(bP4, 0, 1, 0.03, 1.5)), -1, 24);
      bTexture = [
        SelectX.ar(bP3 * 6, [bComb[0], bComp[0], bLimit[0], bLofi[0], bRing[0], bMicro[0], bGrain[0]]),
        SelectX.ar(bP3 * 6, [bComb[1], bComp[1], bLimit[1], bLofi[1], bRing[1], bMicro[1], bGrain[1]])
      ];
      bDelayFreeTime = LinExp.kr(bP1, 0, 1, 0.05, 0.9);
      bDelaySyncTime = Select.kr((bP1 * 7.999).floor.clip(0, 7), [quarter * 0.25, quarter * 0.5, quarter * 0.75, quarter * (2 / 3), quarter, quarter * 1.5, quarter * 2, quarter * 4]);
      bDelay = LPF.ar(CombC.ar(dry, 4.5, SelectX.kr(syncBlend, [bDelayFreeTime, bDelaySyncTime]), LinExp.kr(bP2.clip(0.001, 1), 0.001, 1, 0.25, 8)), LinExp.kr(bP4, 0, 1, 900, 12000));
      bL = SelectX.ar(bEff, [bThru[0], bFilter[0], bEq[0], bMod[0], bSpace[0], bTexture[0], bDelay[0]]);
      bR = SelectX.ar(bEff, [bThru[1], bFilter[1], bEq[1], bMod[1], bSpace[1], bTexture[1], bDelay[1]]);
      sceneB = XFade2.ar(dry, [bL, bR], (bAmt * 2) - 1);

      sig = XFade2.ar(sceneA, sceneB, (Lag.kr(xfade.clip(0, 1), 0.05) * 2) - 1);
      sig = sig * Lag.kr(outputAmp, 0.05);
      Out.ar(out, LeakDC.ar(Limiter.ar(sig, 0.98)));
    }).add;

    context.server.sync;

    mainStage = Synth.new(\fadddddddderMain, [
      \inL, this.getInL,
      \inR, this.getInR,
      \out, context.out_b.index,
      \inputAmp, inputAmp,
      \outputAmp, outputAmp,
      \xfade, 0
    ], context.xg);

    this.addCommand("set_scene_a_effect", "f", { |msg| mainStage.set(\sceneAEffect, msg[1]); });
    this.addCommand("set_scene_a_amount", "f", { |msg| mainStage.set(\sceneAAmount, msg[1]); });
    this.addCommand("set_scene_a_param1", "f", { |msg| mainStage.set(\sceneAParam1, msg[1]); });
    this.addCommand("set_scene_a_param2", "f", { |msg| mainStage.set(\sceneAParam2, msg[1]); });
    this.addCommand("set_scene_a_param3", "f", { |msg| mainStage.set(\sceneAParam3, msg[1]); });
    this.addCommand("set_scene_a_param4", "f", { |msg| mainStage.set(\sceneAParam4, msg[1]); });
    this.addCommand("set_scene_b_effect", "f", { |msg| mainStage.set(\sceneBEffect, msg[1]); });
    this.addCommand("set_scene_b_amount", "f", { |msg| mainStage.set(\sceneBAmount, msg[1]); });
    this.addCommand("set_scene_b_param1", "f", { |msg| mainStage.set(\sceneBParam1, msg[1]); });
    this.addCommand("set_scene_b_param2", "f", { |msg| mainStage.set(\sceneBParam2, msg[1]); });
    this.addCommand("set_scene_b_param3", "f", { |msg| mainStage.set(\sceneBParam3, msg[1]); });
    this.addCommand("set_scene_b_param4", "f", { |msg| mainStage.set(\sceneBParam4, msg[1]); });
    this.addCommand("set_xfade", "f", { |msg| mainStage.set(\xfade, msg[1]); });
    this.addCommand("set_bpm", "f", { |msg| mainStage.set(\bpm, msg[1]); });
    this.addCommand("set_delay_sync", "f", { |msg| mainStage.set(\delaySync, msg[1]); });
    this.addCommand("set_input_amp", "f", { |msg| this.setInputAmp(msg[1]); });
    this.addCommand("set_output_amp", "f", { |msg| this.setOutputAmp(msg[1]); });
    this.addCommand("set_num_input_channels", "i", { |msg| this.setNumInputChannels(msg[1]); });
  }

  getInL {
    ^context.in_b[0].index;
  }

  getInR {
    if (numInputChannels == 1, {
      ^context.in_b[0].index;
    }, {
      ^context.in_b[1].index;
    });
  }

  setNumInputChannels { |numChannelsArg|
    numInputChannels = numChannelsArg;
    mainStage.set(\inL, this.getInL, \inR, this.getInR);
  }

  setInputAmp { |amp|
    inputAmp = amp;
    mainStage.set(\inputAmp, inputAmp);
  }

  setOutputAmp { |amp|
    outputAmp = amp;
    mainStage.set(\outputAmp, outputAmp);
  }

  free {
    mainStage.free;
  }
}
