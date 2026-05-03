FaddLowpass {
  *ar { |dry, amount|
    var cutoff, rq, wet;
    cutoff = LinExp.kr(amount, 0, 1, 12000, 220);
    rq = LinLin.kr(amount, 0, 1, 0.65, 0.18);
    wet = RLPF.ar(dry, cutoff, rq);
    ^XFade2.ar(dry, wet, (amount * 2) - 1);
  }
}
