FaddMacroFilter {
  *ar { |dry, amount, cutoffMacro, resMacro, modeMacro, slopeMacro|
    var cutoff, rq, bpRq, lp12, lp24, bp12, bp24, hp12, hp24, lp, bp, hp, left, right, wet;
    cutoff = LinExp.kr(cutoffMacro, 0, 1, 45, 12000);
    rq = LinLin.kr(resMacro, 0, 1, 0.9, 0.06);
    bpRq = Clip.kr(rq, 0.08, 1.0);
    lp12 = RLPF.ar(dry, cutoff, rq);
    lp24 = RLPF.ar(lp12, cutoff, rq);
    bp12 = BPF.ar(dry, cutoff, bpRq);
    bp24 = BPF.ar(bp12, cutoff, bpRq);
    hp12 = RHPF.ar(dry, cutoff, rq);
    hp24 = RHPF.ar(hp12, cutoff, rq);
    lp = XFade2.ar(lp12, lp24, (slopeMacro * 2) - 1);
    bp = XFade2.ar(bp12, bp24, (slopeMacro * 2) - 1);
    hp = XFade2.ar(hp12, hp24, (slopeMacro * 2) - 1);
    left = SelectX.ar(modeMacro * 2, [lp[0], bp[0], hp[0]]);
    right = SelectX.ar(modeMacro * 2, [lp[1], bp[1], hp[1]]);
    wet = [left, right];
    ^XFade2.ar(dry, wet, (amount * 2) - 1);
  }
}
