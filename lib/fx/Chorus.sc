FaddChorus {
  *ar { |dry, amount, param1, param2|
    var rate, depth, base, delay, wet, dryLevel, wetLevel;
    rate = LinLin.kr(param1, 0, 1, 0.05, 2.8);
    depth = LinLin.kr(param2, 0, 1, 0.001, 0.018);
    base = LinLin.kr(amount, 0, 1, 0.006, 0.025);
    delay = Clip.kr(base + SinOsc.kr(rate, [0, 1.5708], depth), 0.002, 0.05);
    wet = DelayC.ar(dry, 0.05, delay);
    dryLevel = LinLin.kr(amount, 0, 1, 1.0, 0.55);
    wetLevel = LinLin.kr(amount, 0, 1, 0.1, 0.85);
    ^(dry * dryLevel) + (wet * wetLevel);
  }
}
