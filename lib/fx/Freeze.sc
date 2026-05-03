FaddFreeze {
  *ar { |dry, amount, param1, param2|
    var time, decay, inputLevel, wet, color, dryLevel, wetLevel;
    time = LinExp.kr(param1, 0, 1, 0.5, 0.035);
    decay = LinExp.kr(param2, 0, 1, 0.8, 30) * LinLin.kr(amount, 0, 1, 0.5, 1.2);
    inputLevel = LinLin.kr(amount, 0, 1, 1.0, 0.12);
    wet = CombC.ar(dry * inputLevel, 0.5, time, decay);
    color = LinExp.kr(amount, 0, 1, 11000, 850);
    wet = LPF.ar(HPF.ar(wet, 45), color).softclip;
    dryLevel = LinLin.kr(amount, 0, 1, 1.0, 0.08);
    wetLevel = LinLin.kr(amount, 0, 1, 0.12, 1.0);
    ^(dry * dryLevel) + (wet * wetLevel);
  }
}
