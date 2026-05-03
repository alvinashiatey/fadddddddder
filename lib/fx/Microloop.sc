FaddMicroloop {
  *ar { |dry, amount|
    var time, rate, depth, tapA, tapB, wet, dryLevel, wetLevel, color;
    time = LinExp.kr(amount, 0, 1, 0.18, 0.02);
    rate = LinLin.kr(amount, 0, 1, 0.25, 9);
    depth = time * LinLin.kr(amount, 0, 1, 0.05, 0.45);
    tapA = DelayC.ar(dry, 0.25, Clip.kr(time + SinOsc.kr(rate, 0, depth), 0.005, 0.24));
    tapB = DelayC.ar(dry, 0.25, Clip.kr((time * 0.62) + 0.004, 0.005, 0.24));
    color = LinExp.kr(amount, 0, 1, 11000, 1700);
    wet = LPF.ar((tapA + tapB) * 0.65, color).softclip;
    dryLevel = LinLin.kr(amount, 0, 1, 1.0, 0.2);
    wetLevel = LinLin.kr(amount, 0, 1, 0.15, 1.0);
    ^(dry * dryLevel) + (wet * wetLevel);
  }
}
