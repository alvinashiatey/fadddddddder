FaddMacroDelay {
  *ar { |dry, amount, timeMacro, feedbackMacro, freezeMacro, toneMacro|
    var time, decay, input, wet, tone, dryLevel, wetLevel;
    time = LinExp.kr(timeMacro, 0, 1, 0.045, 1.2);
    decay = LinExp.kr(feedbackMacro, 0, 1, 0.25, 12) * LinLin.kr(freezeMacro, 0, 1, 1.0, 3.0);
    input = dry * LinLin.kr(freezeMacro, 0, 1, 1.0, 0.05);
    wet = CombC.ar(input, 1.5, time, decay);
    tone = LinExp.kr(toneMacro, 0, 1, 700, 12000);
    wet = LPF.ar(HPF.ar(wet, 45), tone).softclip;
    dryLevel = LinLin.kr(amount, 0, 1, 1.0, 0.35);
    wetLevel = LinLin.kr(amount, 0, 1, 0.08, 1.0);
    ^(dry * dryLevel) + (wet * wetLevel);
  }
}
