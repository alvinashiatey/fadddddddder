FaddHighpass {
  *ar { |dry, amount|
    var cutoff, rq, wet;
    cutoff = LinExp.kr(amount, 0, 1, 35, 5500);
    rq = LinLin.kr(amount, 0, 1, 0.65, 0.2);
    wet = RHPF.ar(dry, cutoff, rq);
    ^XFade2.ar(dry, wet, (amount * 2) - 1);
  }
}
