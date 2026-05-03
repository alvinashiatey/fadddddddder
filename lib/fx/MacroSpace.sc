FaddMacroSpace {
  *ar { |dry, amount, sizeMacro, dampMacro, typeMacro, widthMacro|
    var width, spread, spatial, plate, spring, dark, left, right, wet, room, damp;
    width = LinLin.kr(widthMacro, 0, 1, 0.0, 1.0);
    spread = [dry[0] + (dry[1] * width * 0.35), dry[1] + (dry[0] * width * 0.35)];
    spatial = Balance2.ar(spread[0], spread[1], 0, 1);
    room = LinLin.kr(sizeMacro, 0, 1, 0.2, 0.98);
    damp = LinLin.kr(dampMacro, 0, 1, 0.15, 0.92);
    plate = FreeVerb2.ar(dry[0], dry[1], 1.0, room, damp);
    spring = AllpassC.ar(CombC.ar(dry, 0.08, LinLin.kr(sizeMacro, 0, 1, 0.015, 0.055), 2.5), 0.06, [0.019, 0.027], 1.8);
    dark = LPF.ar(FreeVerb2.ar(dry[0], dry[1], 1.0, room, damp), LinExp.kr(dampMacro, 0, 1, 9000, 850));
    left = SelectX.ar(typeMacro * 3, [spatial[0], plate[0], spring[0], dark[0]]);
    right = SelectX.ar(typeMacro * 3, [spatial[1], plate[1], spring[1], dark[1]]);
    wet = [left, right];
    ^(dry * LinLin.kr(amount, 0, 1, 1.0, 0.2)) + (wet * LinLin.kr(amount, 0, 1, 0.05, 0.95));
  }
}
