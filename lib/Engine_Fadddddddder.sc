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
    inputBus = Bus.audio(context.server, 2);
    sceneABus = Bus.audio(context.server, 2);
    sceneBBus = Bus.audio(context.server, 2);

    SynthDef(\fadddddddderInput, { |inL = 0, inR = 1, out = 0, amp = 1|
      var sig;
      sig = [In.ar(inL), In.ar(inR)] * Lag.kr(amp, 0.05);
      Out.ar(out, LeakDC.ar(sig));
    }).add;

    SynthDef(\fadddddddderScene, { |inBus = 0, out = 0, effect = 0, amount = 0|
      var dry, a, eff, thru, lowpass, highpass, dub, micro, freeze, left, right, sig;
      dry = In.ar(inBus, 2);
      a = Lag.kr(amount.clip(0, 1), 0.08);
      eff = Lag.kr(effect.clip(0, 5), 0.05);

      thru = FaddThru.ar(dry, a);
      lowpass = FaddLowpass.ar(dry, a);
      highpass = FaddHighpass.ar(dry, a);
      dub = FaddDub.ar(dry, a);
      micro = FaddMicroloop.ar(dry, a);
      freeze = FaddFreeze.ar(dry, a);

      left = SelectX.ar(eff, [thru[0], lowpass[0], highpass[0], dub[0], micro[0], freeze[0]]);
      right = SelectX.ar(eff, [thru[1], lowpass[1], highpass[1], dub[1], micro[1], freeze[1]]);
      sig = LeakDC.ar(Limiter.ar([left, right], 0.98));
      Out.ar(out, sig);
    }).add;

    SynthDef(\fadddddddderMix, { |inABus = 0, inBBus = 2, out = 0, xfade = 0.5, amp = 1|
      var sceneA, sceneB, pos, sig;
      sceneA = In.ar(inABus, 2);
      sceneB = In.ar(inBBus, 2);
      pos = Lag.kr(xfade.clip(0, 1), 0.05);
      sig = XFade2.ar(sceneA, sceneB, (pos * 2) - 1) * Lag.kr(amp, 0.05);
      Out.ar(out, LeakDC.ar(Limiter.ar(sig, 0.98)));
    }).add;

    context.server.sync;

    inputStage = Synth.new(\fadddddddderInput, [
      \inL, this.getInL,
      \inR, this.getInR,
      \out, inputBus.index,
      \amp, inputAmp
    ], context.xg);

    sceneAStage = Synth.new(\fadddddddderScene, [
      \inBus, inputBus.index,
      \out, sceneABus.index,
      \effect, 0,
      \amount, 0
    ], inputStage, \addAfter);

    sceneBStage = Synth.new(\fadddddddderScene, [
      \inBus, inputBus.index,
      \out, sceneBBus.index,
      \effect, 3,
      \amount, 0.55
    ], sceneAStage, \addAfter);

    mixStage = Synth.new(\fadddddddderMix, [
      \inABus, sceneABus.index,
      \inBBus, sceneBBus.index,
      \out, context.out_b.index,
      \xfade, 0,
      \amp, outputAmp
    ], sceneBStage, \addAfter);

    this.addCommand("set_scene_a_effect", "f", { |msg| sceneAStage.set(\effect, msg[1]); });
    this.addCommand("set_scene_a_amount", "f", { |msg| sceneAStage.set(\amount, msg[1]); });
    this.addCommand("set_scene_b_effect", "f", { |msg| sceneBStage.set(\effect, msg[1]); });
    this.addCommand("set_scene_b_amount", "f", { |msg| sceneBStage.set(\amount, msg[1]); });
    this.addCommand("set_xfade", "f", { |msg| mixStage.set(\xfade, msg[1]); });
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
    inputStage.set(\inL, this.getInL, \inR, this.getInR);
  }

  setInputAmp { |amp|
    inputAmp = amp;
    inputStage.set(\amp, inputAmp);
  }

  setOutputAmp { |amp|
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
