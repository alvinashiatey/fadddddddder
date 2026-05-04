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
      var dry, sceneA, sceneB, sig,
      aThru, aFilter, aEQ, aMod, aSpace, aTexture, aDelay,
      bThru, bFilter, bEQ, bMod, bSpace, bTexture, bDelay,
      aEffect, bEffect, aLeft, aRight, bLeft, bRight;
      dry = [In.ar(inL), In.ar(inR)] * Lag.kr(inputAmp, 0.05);

      aEffect = Lag.kr(sceneAEffect.clip(0, 6), 0.05);
      bEffect = Lag.kr(sceneBEffect.clip(0, 6), 0.05);

      aThru = FaddThru.ar(dry, sceneAAmount, sceneAParam1, sceneAParam2);
      aFilter = FaddMacroFilter.ar(dry, sceneAAmount, sceneAParam1, sceneAParam2, sceneAParam3, sceneAParam4);
      aEQ = FaddMacroEQ.ar(dry, sceneAAmount, sceneAParam1, sceneAParam2, sceneAParam3, sceneAParam4);
      aMod = FaddMacroMod.ar(dry, sceneAAmount, sceneAParam1, sceneAParam2, sceneAParam3, sceneAParam4);
      aSpace = FaddMacroSpace.ar(dry, sceneAAmount, sceneAParam1, sceneAParam2, sceneAParam3, sceneAParam4);
      aTexture = FaddMacroTexture.ar(dry, sceneAAmount, sceneAParam1, sceneAParam2, sceneAParam3, sceneAParam4);
      aDelay = FaddMacroDelay.ar(dry, sceneAAmount, sceneAParam1, sceneAParam2, sceneAParam3, sceneAParam4);

      bThru = FaddThru.ar(dry, sceneBAmount, sceneBParam1, sceneBParam2);
      bFilter = FaddMacroFilter.ar(dry, sceneBAmount, sceneBParam1, sceneBParam2, sceneBParam3, sceneBParam4);
      bEQ = FaddMacroEQ.ar(dry, sceneBAmount, sceneBParam1, sceneBParam2, sceneBParam3, sceneBParam4);
      bMod = FaddMacroMod.ar(dry, sceneBAmount, sceneBParam1, sceneBParam2, sceneBParam3, sceneBParam4);
      bSpace = FaddMacroSpace.ar(dry, sceneBAmount, sceneBParam1, sceneBParam2, sceneBParam3, sceneBParam4);
      bTexture = FaddMacroTexture.ar(dry, sceneBAmount, sceneBParam1, sceneBParam2, sceneBParam3, sceneBParam4);
      bDelay = FaddMacroDelay.ar(dry, sceneBAmount, sceneBParam1, sceneBParam2, sceneBParam3, sceneBParam4);

      aLeft = SelectX.ar(aEffect, [aThru[0], aFilter[0], aEQ[0], aMod[0], aSpace[0], aTexture[0], aDelay[0]]);
      aRight = SelectX.ar(aEffect, [aThru[1], aFilter[1], aEQ[1], aMod[1], aSpace[1], aTexture[1], aDelay[1]]);
      bLeft = SelectX.ar(bEffect, [bThru[0], bFilter[0], bEQ[0], bMod[0], bSpace[0], bTexture[0], bDelay[0]]);
      bRight = SelectX.ar(bEffect, [bThru[1], bFilter[1], bEQ[1], bMod[1], bSpace[1], bTexture[1], bDelay[1]]);

      sceneA = [aLeft, aRight];
      sceneB = [bLeft, bRight];
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
