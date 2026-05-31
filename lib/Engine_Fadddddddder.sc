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
      var dry, sig, renderPreset;
      var aAmt, aP1, aP2, aP3, aP4, aEff, sceneA;
      var bAmt, bP1, bP2, bP3, bP4, bEff, sceneB;

      renderPreset = { |input, amount, p1, p2, p3, p4, preset|
        var stage1Id, stage2Id, trims, dryLevel, wetLevel;
        var stage1, stage2;
        var hp, bp, drive, chorus, ring, delay, dub, freeze, darkverb, spring, widen, lofi, comb, eq;

        stage1Id = Select.kr(preset, [0, 1, 1, 2, 3, 12, 5, 13, 1, 2, 1, 14]);
        stage2Id = Select.kr(preset, [0, 11, 9, 10, 6, 4, 9, 4, 8, 7, 5, 3]);
        trims = Select.kr(preset, [1.0, 1.0, 0.92, 0.92, 0.84, 0.9, 0.8, 0.88, 0.86, 0.88, 0.82, 0.86]);

        hp = RHPF.ar(input, p1.linexp(0, 1, 35, 9000), p2.linlin(0, 1, 0.9, 0.12)) * p4.linlin(0, 1, 0.9, 1.2);
        bp = BPF.ar(input, p1.linexp(0, 1, 80, 5000), p2.linlin(0, 1, 0.9, 0.1)) * 1.6;
        drive = LPF.ar((input * (p2.linexp(0, 1, 1.0, 28) * amount.linlin(0, 1, 0.9, 1.6))).tanh, p4.linexp(0, 1, 900, 12000));
        chorus = [
          DelayC.ar(input[0], 0.06, Clip.kr(0.008 + SinOsc.kr(p2.linlin(0, 1, 0.05, 3.4), 0, p3.linlin(0, 1, 0.001, 0.016)), 0.002, 0.05)),
          DelayC.ar(input[1], 0.06, Clip.kr(0.012 + SinOsc.kr(p2.linlin(0, 1, 0.05, 3.4), 1.5708, p3.linlin(0, 1, 0.001, 0.016)), 0.002, 0.05))
        ];
        chorus = [
          chorus[0] + ((chorus[1] - chorus[0]) * p4 * 0.2),
          chorus[1] + ((chorus[0] - chorus[1]) * p4 * 0.2)
        ];
        ring = LPF.ar(input * XFade2.ar(SinOsc.ar(p3.linexp(0, 1, 24, 2400)), LFPulse.ar(p3.linexp(0, 1, 24, 2400)), (p2 * 2) - 1), p4.linexp(0, 1, 900, 12000));
        delay = LPF.ar(CombC.ar(input, 1.2, p3.linexp(0, 1, 0.05, 0.85), p2.max(0.001).linexp(0.001, 1, 0.25, 7.5)), p4.linexp(0, 1, 900, 12000));
        dub = LPF.ar(CombC.ar(HPF.ar(input, 50), 1.2, p3.linexp(0, 1, 0.08, 0.95), p2.max(0.001).linexp(0.001, 1, 0.4, 9.0)).tanh, p4.linexp(0, 1, 800, 10000));
        freeze = LPF.ar(CombC.ar(input * p3.linlin(0, 1, 1.0, 0.05), 0.6, p3.linexp(0, 1, 0.45, 0.04), p2.max(0.001).linexp(0.001, 1, 1.2, 22)).softclip, p4.linexp(0, 1, 900, 11000));
        darkverb = LPF.ar(FreeVerb2.ar(input[0], input[1], 1.0, p3.linlin(0, 1, 0.2, 0.98), p4.linlin(0, 1, 0.15, 0.92)), p4.linexp(0, 1, 1200, 10500));
        spring = LPF.ar(AllpassC.ar(CombC.ar(input, 0.09, p3.linlin(0, 1, 0.015, 0.06), p3.linlin(0, 1, 0.7, 3.2)), 0.07, [0.019, 0.027], 1.9), p4.linexp(0, 1, 1200, 10000));
        widen = {
          var delayed, mid, side;
          delayed = input + (DelayC.ar(input.reverse, 0.03, p3.linlin(0, 1, 0.0, 0.018)) * 0.35);
          mid = (delayed[0] + delayed[1]) * 0.5;
          side = (delayed[0] - delayed[1]) * (0.2 + (p4 * 1.3));
          [mid + side, mid - side];
        }.value;
        lofi = LPF.ar(Latch.ar(input, Impulse.ar(p1.linexp(0, 1, 1500, 32000))).softclip, p3.linexp(0, 1, 1500, 11000)) * p2.linlin(0, 1, 0.8, 1.45);
        comb = LPF.ar(CombC.ar(input, 0.12, p1.linexp(0, 1, 0.004, 0.09), p2.linlin(0, 1, 0.2, 4.6)), p3.linexp(0, 1, 1200, 10500));
        eq = BPeakEQ.ar(input, p1.linexp(0, 1, 90, 6000), p3.linexp(0, 1, 0.4, 5), p2.linlin(0, 1, -12, 12)) * p4.linlin(0, 1, 0.9, 1.2);

        stage1 = SelectX.ar(stage1Id, [input, hp, bp, drive, chorus, ring, delay, dub, freeze, darkverb, spring, widen, lofi, comb, eq]);

        hp = RHPF.ar(stage1, p1.linexp(0, 1, 35, 9000), p2.linlin(0, 1, 0.9, 0.12)) * p4.linlin(0, 1, 0.9, 1.2);
        bp = BPF.ar(stage1, p1.linexp(0, 1, 80, 5000), p2.linlin(0, 1, 0.9, 0.1)) * 1.6;
        drive = LPF.ar((stage1 * (p2.linexp(0, 1, 1.0, 28) * amount.linlin(0, 1, 0.9, 1.6))).tanh, p4.linexp(0, 1, 900, 12000));
        chorus = [
          DelayC.ar(stage1[0], 0.06, Clip.kr(0.008 + SinOsc.kr(p2.linlin(0, 1, 0.05, 3.4), 0, p3.linlin(0, 1, 0.001, 0.016)), 0.002, 0.05)),
          DelayC.ar(stage1[1], 0.06, Clip.kr(0.012 + SinOsc.kr(p2.linlin(0, 1, 0.05, 3.4), 1.5708, p3.linlin(0, 1, 0.001, 0.016)), 0.002, 0.05))
        ];
        chorus = [
          chorus[0] + ((chorus[1] - chorus[0]) * p4 * 0.2),
          chorus[1] + ((chorus[0] - chorus[1]) * p4 * 0.2)
        ];
        ring = LPF.ar(stage1 * XFade2.ar(SinOsc.ar(p3.linexp(0, 1, 24, 2400)), LFPulse.ar(p3.linexp(0, 1, 24, 2400)), (p2 * 2) - 1), p4.linexp(0, 1, 900, 12000));
        delay = LPF.ar(CombC.ar(stage1, 1.2, p3.linexp(0, 1, 0.05, 0.85), p2.max(0.001).linexp(0.001, 1, 0.25, 7.5)), p4.linexp(0, 1, 900, 12000));
        dub = LPF.ar(CombC.ar(HPF.ar(stage1, 50), 1.2, p3.linexp(0, 1, 0.08, 0.95), p2.max(0.001).linexp(0.001, 1, 0.4, 9.0)).tanh, p4.linexp(0, 1, 800, 10000));
        freeze = LPF.ar(CombC.ar(stage1 * p3.linlin(0, 1, 1.0, 0.05), 0.6, p3.linexp(0, 1, 0.45, 0.04), p2.max(0.001).linexp(0.001, 1, 1.2, 22)).softclip, p4.linexp(0, 1, 900, 11000));
        darkverb = LPF.ar(FreeVerb2.ar(stage1[0], stage1[1], 1.0, p3.linlin(0, 1, 0.2, 0.98), p4.linlin(0, 1, 0.15, 0.92)), p4.linexp(0, 1, 1200, 10500));
        spring = LPF.ar(AllpassC.ar(CombC.ar(stage1, 0.09, p3.linlin(0, 1, 0.015, 0.06), p3.linlin(0, 1, 0.7, 3.2)), 0.07, [0.019, 0.027], 1.9), p4.linexp(0, 1, 1200, 10000));
        widen = {
          var delayed, mid, side;
          delayed = stage1 + (DelayC.ar(stage1.reverse, 0.03, p3.linlin(0, 1, 0.0, 0.018)) * 0.35);
          mid = (delayed[0] + delayed[1]) * 0.5;
          side = (delayed[0] - delayed[1]) * (0.2 + (p4 * 1.3));
          [mid + side, mid - side];
        }.value;
        lofi = LPF.ar(Latch.ar(stage1, Impulse.ar(p1.linexp(0, 1, 1500, 32000))).softclip, p3.linexp(0, 1, 1500, 11000)) * p2.linlin(0, 1, 0.8, 1.45);
        comb = LPF.ar(CombC.ar(stage1, 0.12, p1.linexp(0, 1, 0.004, 0.09), p2.linlin(0, 1, 0.2, 4.6)), p3.linexp(0, 1, 1200, 10500));
        eq = BPeakEQ.ar(stage1, p1.linexp(0, 1, 90, 6000), p3.linexp(0, 1, 0.4, 5), p2.linlin(0, 1, -12, 12)) * p4.linlin(0, 1, 0.9, 1.2);

        stage2 = SelectX.ar(stage2Id, [stage1, hp, bp, drive, chorus, ring, delay, dub, freeze, darkverb, spring, widen, lofi, comb, eq]);
        dryLevel = amount.linlin(0, 1, 1.0, 0.32);
        wetLevel = amount.linlin(0, 1, 0.0, 1.0);
        ^((input * dryLevel) + (stage2 * wetLevel)) * trims;
      };

      dry = [In.ar(inL), In.ar(inR)] * Lag.kr(inputAmp, 0.05);

      aAmt = Lag.kr(sceneAAmount.clip(0, 1), 0.08);
      aP1 = Lag.kr(sceneAParam1.clip(0, 1), 0.08);
      aP2 = Lag.kr(sceneAParam2.clip(0, 1), 0.08);
      aP3 = Lag.kr(sceneAParam3.clip(0, 1), 0.08);
      aP4 = Lag.kr(sceneAParam4.clip(0, 1), 0.08);
      aEff = Lag.kr(sceneAEffect.clip(0, 11), 0.05);
      sceneA = renderPreset.value(dry, aAmt, aP1, aP2, aP3, aP4, aEff);

      bAmt = Lag.kr(sceneBAmount.clip(0, 1), 0.08);
      bP1 = Lag.kr(sceneBParam1.clip(0, 1), 0.08);
      bP2 = Lag.kr(sceneBParam2.clip(0, 1), 0.08);
      bP3 = Lag.kr(sceneBParam3.clip(0, 1), 0.08);
      bP4 = Lag.kr(sceneBParam4.clip(0, 1), 0.08);
      bEff = Lag.kr(sceneBEffect.clip(0, 11), 0.05);
      sceneB = renderPreset.value(dry, bAmt, bP1, bP2, bP3, bP4, bEff);

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
