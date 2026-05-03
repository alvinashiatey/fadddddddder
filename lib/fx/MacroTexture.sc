FaddMacroTexture {
  *ar { |dry, amount, colorMacro, damageMacro, typeMacro, motionMacro|
    var comb, comp, rate, held, bits, steps, lofi, ring, freq, mod, left, right, wet;
    comb = CombC.ar(dry, 0.12, LinExp.kr(colorMacro, 0, 1, 0.004, 0.09), LinLin.kr(damageMacro, 0, 1, 0.2, 4.5));
    comp = Compander.ar(dry, dry, LinLin.kr(colorMacro, 0, 1, 0.08, 0.6), 1, LinLin.kr(damageMacro, 0, 1, 0.5, 0.08), 0.01, 0.12) * LinLin.kr(damageMacro, 0, 1, 1.0, 1.8);
    rate = LinExp.kr(colorMacro, 0, 1, 900, 44100);
    held = Latch.ar(dry, Impulse.ar(rate));
    bits = LinLin.kr(damageMacro, 0, 1, 12, 3);
    steps = 2.pow(bits);
    lofi = ((held * steps).round / steps).tanh;
    freq = LinExp.kr(colorMacro, 0, 1, 18, 2600);
    mod = SinOsc.ar(freq + SinOsc.kr(LinLin.kr(motionMacro, 0, 1, 0.1, 8), 0, freq * 0.15));
    ring = dry * mod;
    left = SelectX.ar(typeMacro * 3, [comb[0], comp[0], lofi[0], ring[0]]);
    right = SelectX.ar(typeMacro * 3, [comb[1], comp[1], lofi[1], ring[1]]);
    wet = [left, right];
    ^(dry * LinLin.kr(amount, 0, 1, 1.0, 0.25)) + (wet * LinLin.kr(amount, 0, 1, 0.05, 1.0));
  }
}
