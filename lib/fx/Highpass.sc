FaddHighpass {
  *ar { |dry, amount, param1, param2|
    var cutoff, rq, wet;
    cutoff = LinExp.kr(amount, 0, 1, 35, 5500);
    rq = LinLin.kr(param1, 0, 1, 0.9, 0.08);
    wet = RHPF.ar(dry, cutoff, rq) * LinLin.kr(param2, 0, 1, 0.65, 1.35);
    ^XFade2.ar(dry, wet, (amount * 2) - 1);
  }
}
