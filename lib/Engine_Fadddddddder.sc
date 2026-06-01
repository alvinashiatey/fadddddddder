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
      inputAmp = 1, outputAmp = 1, xfade = 0,
      sceneAEffect = 0, sceneAAmount = 0, sceneAParam1 = 0.5, sceneAParam2 = 0.5, sceneAParam3 = 0.5, sceneAParam4 = 0.5,
      sceneBEffect = 0, sceneBAmount = 0, sceneBParam1 = 0.5, sceneBParam2 = 0.5, sceneBParam3 = 0.5, sceneBParam4 = 0.5
    |
      var dry, sig,
      aAmt, aP1, aP2, aP3, aP4, aEff, aThru, aFilter, aEq, aMod, aSpace, aTexture, aDelay, aL, aR, sceneA,
      bAmt, bP1, bP2, bP3, bP4, bEff, bThru, bFilter, bEq, bMod, bSpace, bTexture, bDelay, bL, bR, sceneB,
      aCutoff, aRq, aBpRq, aLp12, aLp24, aBp12, aBp24, aHp12, aHp24, aMode,
      bCutoff, bRq, bBpRq, bLp12, bLp24, bBp12, bBp24, bHp12, bHp24, bMode;

      dry = [In.ar(inL), In.ar(inR)] * Lag.kr(inputAmp, 0.05);

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
      aMod = DelayC.ar(dry, 0.06, SinOsc.kr(LinLin.kr(aP1, 0, 1, 0.05, 4), [0, 1.5708], LinLin.kr(aP2, 0, 1, 0.001, 0.018), 0.006));
      aSpace = FreeVerb2.ar(dry[0], dry[1], 1, LinLin.kr(aP1, 0, 1, 0.15, 0.95), LinLin.kr(aP2, 0, 1, 0.1, 0.85));
      aTexture = (dry * SinOsc.ar(LinExp.kr(aP1, 0, 1, 18, 2200))).tanh;
      aDelay = LPF.ar(CombC.ar(dry, 1.2, LinExp.kr(aP1, 0, 1, 0.05, 0.9), LinExp.kr(aP2.clip(0.001, 1), 0.001, 1, 0.25, 8)), LinExp.kr(aP4, 0, 1, 900, 12000));
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
      bMod = DelayC.ar(dry, 0.06, SinOsc.kr(LinLin.kr(bP1, 0, 1, 0.05, 4), [0, 1.5708], LinLin.kr(bP2, 0, 1, 0.001, 0.018), 0.006));
      bSpace = FreeVerb2.ar(dry[0], dry[1], 1, LinLin.kr(bP1, 0, 1, 0.15, 0.95), LinLin.kr(bP2, 0, 1, 0.1, 0.85));
      bTexture = (dry * SinOsc.ar(LinExp.kr(bP1, 0, 1, 18, 2200))).tanh;
      bDelay = LPF.ar(CombC.ar(dry, 1.2, LinExp.kr(bP1, 0, 1, 0.05, 0.9), LinExp.kr(bP2.clip(0.001, 1), 0.001, 1, 0.25, 8)), LinExp.kr(bP4, 0, 1, 900, 12000));
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
