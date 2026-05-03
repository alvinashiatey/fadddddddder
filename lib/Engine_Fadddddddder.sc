Engine_Fadddddddder : CroneEngine {
  var inputBus;
  var sceneABus;
  var sceneBBus;
  var inputStage;
  var sceneAStage;
  var sceneBStage;
  var mixStage;
  var inputAmp = 1;
  var outputAmp = 1;
  var numInputChannels = 2;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    SynthDef(\fadddddddderInput, { |inL = 0, inR = 1, out = 0, amp = 1|
      var sig;
      sig = [In.ar(inL), In.ar(inR)] * Lag.kr(amp, 0.05);
      Out.ar(out, LeakDC.ar(sig));
    }).add;

    SynthDef(\fadddddddderScene, { |inL = 0, inR = 1, out = 0, effect = 0, amount = 0|
      var dry, fb, a, eff, lpCutoff, hpCutoff, filterRq, dubTime, dubFeedback, dubColor,
      microTime, microFeedback, microRate, microDepth, microTimeB, microColor,
      freezeTime, freezeInput, freezeFeedback, freezeColor,
      lpWet, hpWet, dubCore, dubWet, microSource, microTapA, microTapB, microWet,
      freezeLoop, freezeWet,
      thruOut, lpOut, hpOut, dubOut, microOut, freezeOut,
      selectedL, selectedR, fbL, fbR;

      dry = [In.ar(inL), In.ar(inR)];
      fb = LocalIn.ar(2);
      a = Lag.kr(amount.clip(0, 1), 0.08);
      eff = Lag.kr(effect.clip(0, 5), 0.06);

      lpCutoff = LinExp.kr(a, 0, 1, 12000, 220);
      hpCutoff = LinExp.kr(a, 0, 1, 40, 5500);
      filterRq = LinLin.kr(a, 0, 1, 0.18, 0.85);

      dubTime = LinLin.kr(a, 0, 1, 0.16, 0.9);
      dubFeedback = LinLin.kr(a, 0, 1, 0.2, 0.92);
      dubColor = LinExp.kr(a, 0, 1, 10000, 1200);

      microTime = LinExp.kr(a, 0, 1, 0.16, 0.025);
      microFeedback = LinLin.kr(a, 0, 1, 0.2, 0.9);
      microRate = LinLin.kr(a, 0, 1, 0.25, 8);
      microDepth = microTime * LinLin.kr(a, 0, 1, 0.08, 0.4);
      microTimeB = Clip.kr((microTime * 0.63) + 0.004, 0.004, 0.25);
      microColor = LinExp.kr(a, 0, 1, 11000, 1800);

      freezeTime = LinExp.kr(a, 0, 1, 0.35, 0.08);
      freezeInput = LinLin.kr(a, 0, 1, 1.0, 0.0);
      freezeFeedback = LinLin.kr(a, 0, 1, 0.55, 0.995);
      freezeColor = LinExp.kr(a, 0, 1, 11000, 900);

      lpWet = RLPF.ar(dry, lpCutoff, filterRq);
      hpWet = RHPF.ar(dry, hpCutoff, filterRq);

      dubCore = DelayC.ar(dry + (fb * dubFeedback), 1.5, dubTime);
      dubWet = LPF.ar(HPF.ar(dubCore, 60), dubColor).tanh;

      microSource = HPF.ar(dry + (fb * microFeedback), 100);
      microTapA = DelayC.ar(
        microSource,
        0.25,
        Clip.kr(microTime + SinOsc.kr(microRate, 0, microDepth), 0.005, 0.24)
      );
      microTapB = DelayC.ar(microSource, 0.25, microTimeB);
      microWet = LPF.ar(((microTapA + microTapB) * 0.5) * 1.2, microColor).softclip;

      freezeLoop = DelayC.ar((dry * freezeInput) + (fb * freezeFeedback), 0.5, freezeTime);
      freezeWet = LPF.ar(HPF.ar(freezeLoop, 50), freezeColor).softclip;

      thruOut = dry;
      lpOut = XFade2.ar(dry, lpWet, (a * 2) - 1);
      hpOut = XFade2.ar(dry, hpWet, (a * 2) - 1);
      dubOut = (dry * LinLin.kr(a, 0, 1, 1.0, 0.45)) + (dubWet * LinLin.kr(a, 0, 1, 0.15, 1.0));
      microOut = (dry * LinLin.kr(a, 0, 1, 1.0, 0.18)) + (microWet * LinLin.kr(a, 0, 1, 0.2, 1.0));
      freezeOut = (dry * LinLin.kr(a, 0, 1, 1.0, 0.1)) + (freezeWet * LinLin.kr(a, 0, 1, 0.15, 1.0));

      selectedL = SelectX.ar(eff, [
        thruOut[0], lpOut[0], hpOut[0], dubOut[0], microOut[0], freezeOut[0]
      ]);
      selectedR = SelectX.ar(eff, [
        thruOut[1], lpOut[1], hpOut[1], dubOut[1], microOut[1], freezeOut[1]
      ]);

      fbL = SelectX.ar(eff, [0, 0, 0, dubWet[0] * dubFeedback, microWet[0] * microFeedback, freezeWet[0] * freezeFeedback]);
      fbR = SelectX.ar(eff, [0, 0, 0, dubWet[1] * dubFeedback, microWet[1] * microFeedback, freezeWet[1] * freezeFeedback]);

      LocalOut.ar(LeakDC.ar(Limiter.ar([fbL, fbR], 0.95)));
      Out.ar(out, LeakDC.ar(Limiter.ar([selectedL, selectedR], 0.98)));
    }).add;

    SynthDef(\fadddddddderMix, { |inAL = 0, inAR = 1, inBL = 2, inBR = 3, out = 0, xfade = 0.5, amp = 1|
      var sceneA, sceneB, sig, pos;
      sceneA = [In.ar(inAL), In.ar(inAR)];
      sceneB = [In.ar(inBL), In.ar(inBR)];
      pos = Lag.kr(xfade.clip(0, 1), 0.05);
      sig = XFade2.ar(sceneA, sceneB, (pos * 2) - 1) * Lag.kr(amp, 0.05);
      Out.ar(out, LeakDC.ar(Limiter.ar(sig, 0.98)));
    }).add;

    context.server.sync;

    inputBus = Bus.audio(context.server, 2);
    sceneABus = Bus.audio(context.server, 2);
    sceneBBus = Bus.audio(context.server, 2);

    inputStage = Synth.new(\fadddddddderInput, [
      \inL, this.getInL,
      \inR, this.getInR,
      \out, inputBus.index,
      \amp, inputAmp
    ], context.xg);

    sceneAStage = Synth.new(\fadddddddderScene, [
      \inL, inputBus.index,
      \inR, inputBus.index + 1,
      \out, sceneABus.index,
      \effect, 0,
      \amount, 0
    ], inputStage, \addAfter);

    sceneBStage = Synth.new(\fadddddddderScene, [
      \inL, inputBus.index,
      \inR, inputBus.index + 1,
      \out, sceneBBus.index,
      \effect, 3,
      \amount, 0.55
    ], sceneAStage, \addAfter);

    mixStage = Synth.new(\fadddddddderMix, [
      \inAL, sceneABus.index,
      \inAR, sceneABus.index + 1,
      \inBL, sceneBBus.index,
      \inBR, sceneBBus.index + 1,
      \out, context.out_b.index,
      \xfade, 0.5,
      \amp, outputAmp
    ], sceneBStage, \addAfter);

    this.addCommand("set_scene_a_effect", "f", {|msg| sceneAStage.set(\effect, msg[1]); });
    this.addCommand("set_scene_a_amount", "f", {|msg| sceneAStage.set(\amount, msg[1]); });
    this.addCommand("set_scene_b_effect", "f", {|msg| sceneBStage.set(\effect, msg[1]); });
    this.addCommand("set_scene_b_amount", "f", {|msg| sceneBStage.set(\amount, msg[1]); });
    this.addCommand("set_xfade", "f", {|msg| mixStage.set(\xfade, msg[1]); });
    this.addCommand("set_input_amp", "f", {|msg| this.setInputAmp(msg[1]); });
    this.addCommand("set_output_amp", "f", {|msg| this.setOutputAmp(msg[1]); });
    this.addCommand("set_num_input_channels", "i", {|msg| this.setNumInputChannels(msg[1]); });
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

  setNumInputChannels {|numChannelsArg|
    numInputChannels = numChannelsArg;
    inputStage.set(\inL, this.getInL, \inR, this.getInR);
  }

  setInputAmp {|amp|
    inputAmp = amp;
    inputStage.set(\amp, inputAmp);
  }

  setOutputAmp {|amp|
    outputAmp = amp;
    mixStage.set(\amp, outputAmp);
  }

  free {
    mixStage.free;
    sceneBStage.free;
    sceneAStage.free;
    inputStage.free;
    sceneBBus.free;
    sceneABus.free;
    inputBus.free;
  }
}
