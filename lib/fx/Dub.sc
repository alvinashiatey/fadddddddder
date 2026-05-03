FaddDub {
  *ar { |dry, amount, param1, param2|
    var time, decay, color, wet, dryLevel, wetLevel;
    time = LinLin.kr(param1, 0, 1, 0.08, 0.95);
    decay = LinLin.kr(param2, 0, 1, 0.35, 7.5) * LinLin.kr(amount, 0, 1, 0.5, 1.25);
    color = LinExp.kr(amount, 0, 1, 10000, 1000);
    wet = CombC.ar(dry, 1.2, time, decay);
    wet = LPF.ar(HPF.ar(wet, 55), color).tanh;
    dryLevel = LinLin.kr(amount, 0, 1, 1.0, 0.45);
    wetLevel = LinLin.kr(amount, 0, 1, 0.08, 0.9);
    ^(dry * dryLevel) + (wet * wetLevel);
  }
}
