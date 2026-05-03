FaddReverb {
  *ar { |dry, amount, param1, param2|
    var room, damp, wet, dryLevel, wetLevel;
    room = LinLin.kr(param1, 0, 1, 0.2, 0.98);
    damp = LinLin.kr(param2, 0, 1, 0.1, 0.85);
    wet = FreeVerb2.ar(dry[0], dry[1], 1.0, room, damp);
    dryLevel = LinLin.kr(amount, 0, 1, 1.0, 0.25);
    wetLevel = LinLin.kr(amount, 0, 1, 0.05, 0.95);
    ^(dry * dryLevel) + (wet * wetLevel);
  }
}
