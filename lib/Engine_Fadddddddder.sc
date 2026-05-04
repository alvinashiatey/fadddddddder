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
      var dry, sig;
      dry = [In.ar(inL), In.ar(inR)] * Lag.kr(inputAmp, 0.05);

      // Recovery build: prove the norns input -> engine -> output path first.
      // FX controls stay wired so Lua/psets do not break while DSP is reintroduced.
      sig = dry * Lag.kr(outputAmp, 0.05);
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
