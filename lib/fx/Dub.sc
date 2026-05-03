FaddDub {
  *ar { |dry, amount|
    var time, decay, color, wet, dryLevel, wetLevel;
    time = LinLin.kr(amount, 0, 1, 0.12, 0.85);
    decay = LinLin.kr(amount, 0, 1, 0.6, 5.5);
    color = LinExp.kr(amount, 0, 1, 10000, 1000);
    wet = CombC.ar(dry, 1.2, time, decay);
    wet = LPF.ar(HPF.ar(wet, 55), color).tanh;
    dryLevel = LinLin.kr(amount, 0, 1, 1.0, 0.45);
    wetLevel = LinLin.kr(amount, 0, 1, 0.08, 0.9);
    ^(dry * dryLevel) + (wet * wetLevel);
  }
}
