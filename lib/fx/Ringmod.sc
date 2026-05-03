FaddRingmod {
  *ar { |dry, amount, param1, param2|
    var freq, shape, mod, wet, dryLevel, wetLevel;
    freq = LinExp.kr(param1, 0, 1, 18, 2600);
    shape = LinLin.kr(param2, 0, 1, 0.0, 1.0);
    mod = XFade2.ar(SinOsc.ar(freq), LFPulse.ar(freq), (shape * 2) - 1);
    wet = dry * mod;
    dryLevel = LinLin.kr(amount, 0, 1, 1.0, 0.25);
    wetLevel = LinLin.kr(amount, 0, 1, 0.1, 1.0);
    ^(dry * dryLevel) + (wet * wetLevel);
  }
}
