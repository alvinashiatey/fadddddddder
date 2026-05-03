FaddDrive {
  *ar { |dry, amount, param1, param2|
    var gain, tone, wet, dryLevel, wetLevel;
    gain = LinExp.kr(param1, 0, 1, 1.2, 42) * LinLin.kr(amount, 0, 1, 0.7, 1.5);
    tone = LinExp.kr(param2, 0, 1, 900, 9000);
    wet = LPF.ar((dry * gain).tanh, tone);
    dryLevel = LinLin.kr(amount, 0, 1, 1.0, 0.2);
    wetLevel = LinLin.kr(amount, 0, 1, 0.1, 1.0);
    ^(dry * dryLevel) + (wet * wetLevel);
  }
}
