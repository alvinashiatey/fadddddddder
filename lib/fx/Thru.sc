FaddThru {
  *ar { |dry, amount, param1, param2|
    var tone, gain, sig;
    tone = LinExp.kr(param1, 0, 1, 800, 12000);
    gain = LinLin.kr(param2, 0, 1, 0.5, 1.35);
    sig = LPF.ar(dry, tone) * gain;
    ^XFade2.ar(dry, sig, (amount * 2) - 1);
  }
}
