FaddMacroMod {
  *ar { |dry, amount, rateMacro, depthMacro, typeMacro, stagesMacro|
    var rate, depth, ph2, ph6, ph10, phaser, flanger, c1, c2, c3, c4, c5, tap3, tap4, tap5, chorus, left, right, wet;
    rate = LinLin.kr(rateMacro, 0, 1, 0.04, 5.5);
    depth = LinLin.kr(depthMacro, 0, 1, 0.0008, 0.018);
    ph2 = AllpassC.ar(dry, 0.04, SinOsc.kr(rate, 0, depth, 0.006), 0.25);
    ph6 = AllpassC.ar(AllpassC.ar(AllpassC.ar(ph2, 0.04, SinOsc.kr(rate * 0.9, 1, depth, 0.008), 0.25), 0.04, SinOsc.kr(rate * 1.1, 2, depth, 0.01), 0.25), 0.04, SinOsc.kr(rate * 1.3, 3, depth, 0.012), 0.25);
    ph10 = AllpassC.ar(AllpassC.ar(ph6, 0.04, SinOsc.kr(rate * 1.5, 4, depth, 0.014), 0.25), 0.04, SinOsc.kr(rate * 1.7, 5, depth, 0.016), 0.25);
    phaser = [
      SelectX.ar(stagesMacro * 2, [ph2[0], ph6[0], ph10[0]]),
      SelectX.ar(stagesMacro * 2, [ph2[1], ph6[1], ph10[1]])
    ];
    flanger = DelayC.ar(dry, 0.05, SinOsc.kr(rate, [0, 1.5708], depth, 0.004 + depth));
    c1 = DelayC.ar(dry, 0.07, SinOsc.kr(rate * 0.7, 0, depth, 0.011));
    c2 = DelayC.ar(dry, 0.07, SinOsc.kr(rate * 0.9, 1, depth, 0.017));
    c3 = DelayC.ar(dry, 0.07, SinOsc.kr(rate * 1.1, 2, depth, 0.023));
    c4 = DelayC.ar(dry, 0.07, SinOsc.kr(rate * 1.3, 3, depth, 0.031));
    c5 = DelayC.ar(dry, 0.07, SinOsc.kr(rate * 1.5, 4, depth, 0.041));
    tap3 = LinLin.kr(stagesMacro, 0, 1, 0, 1);
    tap4 = LinLin.kr(stagesMacro, 0.33, 1, 0, 1).clip(0, 1);
    tap5 = LinLin.kr(stagesMacro, 0.66, 1, 0, 1).clip(0, 1);
    chorus = (c1 + c2 + (c3 * tap3) + (c4 * tap4) + (c5 * tap5)) / (2 + tap3 + tap4 + tap5);
    left = SelectX.ar(typeMacro * 2, [phaser[0], flanger[0], chorus[0]]);
    right = SelectX.ar(typeMacro * 2, [phaser[1], flanger[1], chorus[1]]);
    wet = [left, right];
    ^(dry * LinLin.kr(amount, 0, 1, 1.0, 0.45)) + (wet * LinLin.kr(amount, 0, 1, 0.05, 0.9));
  }
}
