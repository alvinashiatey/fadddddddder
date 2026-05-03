FaddMacroEQ {
  *ar { |dry, amount, freqMacro, gainMacro, qMacro, styleMacro|
    var freq, gain, q, param, low, mid, high, killLow, killMid, killHigh, kill, left, right, wet;
    freq = LinExp.kr(freqMacro, 0, 1, 80, 9000);
    gain = LinLin.kr(gainMacro, 0, 1, -18, 18);
    q = LinExp.kr(qMacro, 0, 1, 0.25, 8);
    param = BPeakEQ.ar(dry, freq, q, gain);
    low = LPF.ar(dry, 250) * (1 - freqMacro);
    mid = BPF.ar(dry, 1000, 0.8) * (1 - gainMacro);
    high = HPF.ar(dry, 2500) * (1 - qMacro);
    kill = low + mid + high;
    killLow = XFade2.ar(param, kill, (styleMacro * 2) - 1);
    left = killLow[0];
    right = killLow[1];
    wet = [left, right];
    ^XFade2.ar(dry, wet, (amount * 2) - 1);
  }
}
