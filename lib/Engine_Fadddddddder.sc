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
      var dry, sceneA, sceneB, sig, processScene;
      dry = [In.ar(inL), In.ar(inR)] * Lag.kr(inputAmp, 0.05);

      processScene = { |effect, amount, param1, param2, param3, param4|
        var a, p1, p2, p3, p4, eff,
        thru, filterFx, eqFx, modFx, spaceFx, textureFx, delayFx,
        cutoff, rq, modePos, slopePos, lp12, lp24, bp12, bp24, hp12, hp24, filtL, filtR,
        eqFreq, eqGain, eqQ, paramEq, killEq,
        rate, depth, phaser, flanger, chorus, modL, modR,
        room, damp, spatial, plate, spring, dark, spaceL, spaceR,
        comb, comp, ring, texL, texR,
        delayTime, decay, freezeInput, echo, delayL, delayR,
        left, right;

        a = Lag.kr(amount.clip(0, 1), 0.08);
        p1 = Lag.kr(param1.clip(0, 1), 0.08);
        p2 = Lag.kr(param2.clip(0, 1), 0.08);
        p3 = Lag.kr(param3.clip(0, 1), 0.08);
        p4 = Lag.kr(param4.clip(0, 1), 0.08);
        eff = Lag.kr(effect.clip(0, 6), 0.05);

        thru = XFade2.ar(dry, LPF.ar(dry, LinExp.kr(p1, 0, 1, 900, 12000)) * LinLin.kr(p2, 0, 1, 0.6, 1.25), (a * 2) - 1);

        cutoff = LinExp.kr(p1, 0, 1, 45, 12000);
        rq = LinLin.kr(p2, 0, 1, 0.85, 0.08);
        modePos = p3 * 2;
        slopePos = (p4 * 2) - 1;
        lp12 = RLPF.ar(dry, cutoff, rq);
        lp24 = RLPF.ar(lp12, cutoff, rq);
        bp12 = BPF.ar(dry, cutoff, Clip.kr(rq, 0.1, 1));
        bp24 = BPF.ar(bp12, cutoff, Clip.kr(rq, 0.1, 1));
        hp12 = RHPF.ar(dry, cutoff, rq);
        hp24 = RHPF.ar(hp12, cutoff, rq);
        filtL = SelectX.ar(modePos, [XFade2.ar(lp12[0], lp24[0], slopePos), XFade2.ar(bp12[0], bp24[0], slopePos), XFade2.ar(hp12[0], hp24[0], slopePos)]);
        filtR = SelectX.ar(modePos, [XFade2.ar(lp12[1], lp24[1], slopePos), XFade2.ar(bp12[1], bp24[1], slopePos), XFade2.ar(hp12[1], hp24[1], slopePos)]);
        filterFx = XFade2.ar(dry, [filtL, filtR], (a * 2) - 1);

        eqFreq = LinExp.kr(p1, 0, 1, 80, 9000);
        eqGain = LinLin.kr(p2, 0, 1, -18, 18);
        eqQ = LinExp.kr(p3, 0, 1, 0.3, 6);
        paramEq = BPeakEQ.ar(dry, eqFreq, eqQ, eqGain);
        killEq = (LPF.ar(dry, 250) * (1 - p1)) + (BPF.ar(dry, 1000, 0.8) * (1 - p2)) + (HPF.ar(dry, 2500) * (1 - p3));
        eqFx = XFade2.ar(dry, XFade2.ar(paramEq, killEq, (p4 * 2) - 1), (a * 2) - 1);

        rate = LinLin.kr(p1, 0, 1, 0.04, 5.5);
        depth = LinLin.kr(p2, 0, 1, 0.001, 0.018);
        phaser = AllpassC.ar(dry, 0.04, SinOsc.kr(rate, [0, 1.5708], depth, 0.006), 0.35);
        flanger = DelayC.ar(dry, 0.05, SinOsc.kr(rate, [0, 1.5708], depth, 0.004 + depth));
        chorus = Mix.fill(5, { |i| DelayC.ar(dry, 0.07, SinOsc.kr(rate * (0.55 + (i * 0.18)), i, depth, 0.012 + (i * 0.006))) }) * 0.2;
        modL = SelectX.ar(p3 * 2, [phaser[0], flanger[0], chorus[0]]);
        modR = SelectX.ar(p3 * 2, [phaser[1], flanger[1], chorus[1]]);
        modFx = (dry * LinLin.kr(a, 0, 1, 1.0, 0.45)) + ([modL, modR] * LinLin.kr(a, 0, 1, 0.05, 0.9));

        room = LinLin.kr(p1, 0, 1, 0.2, 0.98);
        damp = LinLin.kr(p2, 0, 1, 0.15, 0.9);
        spatial = [dry[0] + (dry[1] * p4 * 0.4), dry[1] + (dry[0] * p4 * 0.4)];
        plate = FreeVerb2.ar(dry[0], dry[1], 1, room, damp);
        spring = AllpassC.ar(CombC.ar(dry, 0.08, LinLin.kr(p1, 0, 1, 0.015, 0.055), 2.5), 0.06, [0.019, 0.027], 1.8);
        dark = LPF.ar(plate, LinExp.kr(p2, 0, 1, 9000, 850));
        spaceL = SelectX.ar(p3 * 3, [spatial[0], plate[0], spring[0], dark[0]]);
        spaceR = SelectX.ar(p3 * 3, [spatial[1], plate[1], spring[1], dark[1]]);
        spaceFx = (dry * LinLin.kr(a, 0, 1, 1.0, 0.2)) + ([spaceL, spaceR] * LinLin.kr(a, 0, 1, 0.05, 0.95));

        comb = CombC.ar(dry, 0.12, LinExp.kr(p1, 0, 1, 0.004, 0.09), LinLin.kr(p2, 0, 1, 0.2, 4.5));
        comp = Compander.ar(dry, dry, LinLin.kr(p1, 0, 1, 0.08, 0.6), 1, LinLin.kr(p2, 0, 1, 0.5, 0.08), 0.01, 0.12) * LinLin.kr(p2, 0, 1, 1.0, 1.8);
        ring = dry * SinOsc.ar(LinExp.kr(p1, 0, 1, 18, 2600) + SinOsc.kr(LinLin.kr(p4, 0, 1, 0.1, 8), 0, 90));
        texL = SelectX.ar(p3 * 2, [comb[0], comp[0], ring[0]]);
        texR = SelectX.ar(p3 * 2, [comb[1], comp[1], ring[1]]);
        textureFx = (dry * LinLin.kr(a, 0, 1, 1.0, 0.25)) + ([texL, texR] * LinLin.kr(a, 0, 1, 0.05, 1.0));

        delayTime = LinExp.kr(p1, 0, 1, 0.045, 1.2);
        decay = LinExp.kr(p2, 0, 1, 0.25, 12) * LinLin.kr(p3, 0, 1, 1.0, 3.0);
        freezeInput = dry * LinLin.kr(p3, 0, 1, 1.0, 0.05);
        echo = CombC.ar(freezeInput, 1.5, delayTime, decay);
        echo = LPF.ar(HPF.ar(echo, 45), LinExp.kr(p4, 0, 1, 700, 12000)).softclip;
        delayL = echo[0];
        delayR = echo[1];
        delayFx = (dry * LinLin.kr(a, 0, 1, 1.0, 0.35)) + ([delayL, delayR] * LinLin.kr(a, 0, 1, 0.08, 1.0));

        left = SelectX.ar(eff, [thru[0], filterFx[0], eqFx[0], modFx[0], spaceFx[0], textureFx[0], delayFx[0]]);
        right = SelectX.ar(eff, [thru[1], filterFx[1], eqFx[1], modFx[1], spaceFx[1], textureFx[1], delayFx[1]]);
        [left, right];
      };

      sceneA = processScene.value(sceneAEffect, sceneAAmount, sceneAParam1, sceneAParam2, sceneAParam3, sceneAParam4);
      sceneB = processScene.value(sceneBEffect, sceneBAmount, sceneBParam1, sceneBParam2, sceneBParam3, sceneBParam4);
      sig = XFade2.ar(sceneA, sceneB, (Lag.kr(xfade.clip(0, 1), 0.05) * 2) - 1) * Lag.kr(outputAmp, 0.05);
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
