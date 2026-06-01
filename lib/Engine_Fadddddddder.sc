// Engine_Fadddddddder
// Live-input two-scene crossfader for norns / Crone.
//
// Effect index mapping (SelectX on sceneAEffect / sceneBEffect):
//   0 = thru
//   1 = filter  (RLPF / BPF / RHPF, mode and slope via params)
//   2 = eq      (BPeakEQ parametric)
//   3 = mod     (chorus/flanger DelayC + SinOsc)
//   4 = space   (FreeVerb2)
//   5 = texture    (AM ring-mod via SinOsc, tanh saturation)
//   6 = delay      (CombC + LPF tone control)
//   7 = resonator  (comb / karplus-style metallic ringing)
//   8 = foldFilter (multimode filter with folded resonance path)
//   9 = formant    (stacked bandpasses / vowel-like tone)
//  10 = tremolo    (sine to square rhythmic amplitude gating)
//  11 = crusher    (bit/sample-rate reduction)
//  12 = freqShift  (single-sideband frequency shifter)
//  13 = granular   (live-input grain freeze / scatter)
//  14 = plate      (brighter, tighter synthetic plate)
//  15 = early      (early reflections cluster, no long tail)
//  16 = haas       (short inter-channel delay pseudo stereo)
//  17 = tapeSat    (soft-knee tape-ish saturation)
//  18 = transient  (attack/sustain contour shaper)
//  19 = multiclip  (multiband soft clipper)
//  20 = pitchShift   (PitchShift harmoniser / detune / octave)
//  21 = reverse      (LocalBuf record + reverse playback)
//  22 = slapback     (single short echo, no feedback)
//  23 = convReverb   (Convolution2 with short generated impulse)
//  24 = spectralFreeze(FFT PV_MagFreeze smear / freeze)
//  25 = autoWah      (amplitude follower into filter cutoff)
//
// param1–param4 are always normalised 0–1; each effect branch maps them
// to musically useful ranges internally.

Engine_Fadddddddder : CroneEngine {

  var mainStage;
  var inputAmp       = 1;
  var outputAmp      = 1;
  var numInputChannels = 2;
  // Tempo state for delay sync (set via set_bpm / set_delay_sync)
  var currentBpm     = 120;
  var delaySyncMode  = 1;   // 0 = free, 1 = sync

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback)
  }

  alloc {
    SynthDef(\fadddddddderMain, { |
      inL = 0, inR = 1, out = 0,
      inputAmp  = 1, outputAmp = 1,
      xfade     = 0,

      sceneAEffect = 0,
      sceneAAmount = 0, sceneAParam1 = 0.5, sceneAParam2 = 0.5, sceneAParam3 = 0.5, sceneAParam4 = 0.5,

      sceneBEffect = 0,
      sceneBAmount = 0, sceneBParam1 = 0.5, sceneBParam2 = 0.5, sceneBParam3 = 0.5, sceneBParam4 = 0.5,

      // Delay time in seconds, updated by the Lua side when BPM or sync changes.
      delayTime = 0.5
    |
      var dry, xfadeLag, sceneA, sceneB, sig;

      dry = [In.ar(inL), In.ar(inR)] * Lag.kr(inputAmp, 0.05);

      // ---------- scene A --------------------------------------------------
      sceneA = {
        var amt, p1, p2, p3, p4, eff;
        var cutoff, rq, bpRq, lp12, lp24, bp12, bp24, hp12, hp24, mode, aFilter;
        var aEq, aMod, aSpace, aTexture, aDelay, aResonator, aFoldFilter, aFormant, aTremolo, aCrusher, aFreqShift, aGranular, aPlate, aEarly, aHaas, aTapeSat, aTransient, aMulticlip;
        var aPitch, aReverse, aSlapback, aConv, aSpectral, aAutoWah;
        var psPitch, psWindow, psDisp, psWet;
        var revBuf, revPhase, revTrig, revRate, revMix;
        var slapTime, slapTone, slapMix;
        var convBuf, convDecay, convDens, convWet;
        var fftBuf, fftChain, freezeAmt, smearAmt, specWet;
        var awEnv, awFloor, awRange, awCut, awRes, awMix; 
        var excite, decay, tune, tone, combA, combB, base, folded, foldAmt;
        var formA, formB, formC, spread, focus;
        var tremRate, tremDepth, tremShape, tremBias, tremWave;
        var crushRate, crushBits, crushMix;
        var shiftFreq, shiftSpread;
        var grainDensity, grainSize, grainScatter, grainPitch, grainPan;
        var pre, room, damp, earlyA, earlyB, earlyC, earlyD, haasTime, haasTilt, plateIn;
        var satDrive, satBias, fastEnv, slowEnv, attackCtrl, sustainCtrl, transientGain;
        var low, mid, high, lowDrive, midDrive, highDrive, tilt;
        var wetL, wetR;

        amt = Lag.kr(sceneAAmount.clip(0, 1), 0.08);
        p1  = Lag.kr(sceneAParam1.clip(0, 1), 0.08);
        p2  = Lag.kr(sceneAParam2.clip(0, 1), 0.08);
        p3  = Lag.kr(sceneAParam3.clip(0, 1), 0.08);
        p4  = Lag.kr(sceneAParam4.clip(0, 1), 0.08);
        // Do NOT lag the effect index — interpolating between integer slots
        // causes SelectX to blend between unrelated algorithms and produces
        // unpredictable artefacts.  Round to the nearest integer instead.
        eff = sceneAEffect.clip(0, 25).round(1);

        // ---- 20 pitchShift: harmoniser / detune / octave ----
// p1 = pitch ratio (octave down → octave up), p2 = detune spread,
// p3 = dry+harmony blend (parallel interval), p4 = tone
psPitch  = LinExp.kr(p1, 0, 1, 0.5, 2.0);          // -1oct .. +1oct
psWindow = 0.2;
psDisp   = LinLin.kr(p2, 0, 1, 0.0, 0.05);
aPitch   = PitchShift.ar(dry, psWindow, psPitch, psDisp, psDisp * 0.5);
// optional parallel harmony voice mixed in via p3
psWet    = aPitch + (PitchShift.ar(dry, psWindow,
                (psPitch * 1.5).clip(0.5, 2.0), psDisp, psDisp) * LinLin.kr(p3, 0, 1, 0.0, 0.9));
aPitch   = LPF.ar(LeakDC.ar(psWet), LinExp.kr(p4, 0, 1, 1200, 12000));

// ---- 21 reverse: record into LocalBuf, play backwards ----
// p1 = segment length, p2 = playback rate, p4 = tone
revBuf   = LocalBuf(SampleRate.ir * 1.0, 2).clear;
revRate  = LinLin.kr(p2, 0, 1, 0.5, 1.5);
revTrig  = Impulse.kr(LinExp.kr(p1, 0, 1, 6, 0.7));   // segment rate
RecordBuf.ar(dry, revBuf, 0, 1, 0, 1, 1, 1, revTrig);
revPhase = Phasor.ar(revTrig,
    BufRateScale.kr(revBuf) * revRate * -1,
    0, BufFrames.kr(revBuf),
    BufFrames.kr(revBuf));
aReverse = BufRd.ar(2, revBuf, revPhase, 1, 4);
aReverse = LPF.ar(LeakDC.ar(aReverse), LinExp.kr(p4, 0, 1, 1200, 12000));
aReverse = aReverse.clip2(1.0);

// ---- 22 slapback: single short repeat, no feedback ----
// p1 = time (40-180ms), p4 = tone, p3 = repeat level
slapTime = LinLin.kr(p1, 0, 1, 0.04, 0.18);
slapTone = LinExp.kr(p4, 0, 1, 1200, 9000);
aSlapback = dry + (LPF.ar(DelayC.ar(dry, 0.25, slapTime), slapTone)
            * LinLin.kr(p3, 0, 1, 0.3, 0.95));
aSlapback = LeakDC.ar(aSlapback);

// ---- 23 convReverb: Convolution2 with short generated impulse ----
// p1 = impulse decay (size), p2 = density, p4 = tone
// ---- 23 convReverb: Convolution2 with short generated impulse ----
convDecay = LinLin.kr(p1, 0, 1, 0.05, 0.4);
convDens  = LinLin.kr(p2, 0, 1, 0.3, 1.0);
convBuf   = LocalBuf(2048, 1).clear;
RecordBuf.ar(
    WhiteNoise.ar * EnvGen.ar(Env.perc(0.001, convDecay), Impulse.ar(0)),
    convBuf, 0, 1, 0, 1, 0, 0);
aConv = Convolution2.ar(Mix.ar(dry) * convDens, convBuf, Impulse.kr(0), 2048);
aConv = LPF.ar(LeakDC.ar(aConv ! 2), LinExp.kr(p4, 0, 1, 1500, 11000));

// ---- 24 spectralFreeze: PV_MagFreeze smear / freeze ----
// p1 = freeze gate, p2 = smear amount (PV_MagSmear), p4 = tone
freezeAmt = (p1 > 0.5);    // > 50% = frozen
smearAmt  = LinLin.kr(p2, 0, 1, 0, 16).round(1);
fftBuf    = LocalBuf(2048, 1);
fftChain  = FFT(fftBuf, Mix.ar(dry));
fftChain  = PV_MagSmear(fftChain, smearAmt);
fftChain  = PV_MagFreeze(fftChain, freezeAmt);
aSpectral = IFFT(fftChain).dup;
aSpectral = LPF.ar(LeakDC.ar(aSpectral), LinExp.kr(p4, 0, 1, 1400, 12000));

// ---- 25 autoWah: amplitude follower into filter cutoff ----
// p1 = sensitivity, p2 = range, p3 = resonance, p4 = base cutoff
awEnv   = Amplitude.kr(Mix.ar(dry), 0.01, 0.15) * LinLin.kr(p1, 0, 1, 2, 30);
awFloor = LinExp.kr(p4, 0, 1, 120, 800);
awRange = LinExp.kr(p2, 0, 1, 400, 6000);
awCut   = (awFloor + (awEnv * awRange)).clip(80, 12000);
awRes   = LinLin.kr(p3, 0, 1, 0.6, 0.08);
aAutoWah = LeakDC.ar(RLPF.ar(dry, Lag.kr(awCut, 0.02), awRes) * 1.4);

        // filter: cutoff, resonance, mode (lp/bp/hp), slope (12/24dB)
        cutoff = LinExp.kr(p1, 0, 1, 45, 12000);
        rq     = LinLin.kr(p2, 0, 1, 0.9, 0.06);
        bpRq   = rq.clip(0.08, 1.0);
        lp12   = RLPF.ar(dry, cutoff, rq);
        lp24   = RLPF.ar(lp12, cutoff, rq);
        bp12   = BPF.ar(dry, cutoff, bpRq);
        bp24   = BPF.ar(bp12, cutoff, bpRq);
        hp12   = RHPF.ar(dry, cutoff, rq);
        hp24   = RHPF.ar(hp12, cutoff, rq);
        mode   = p3 * 2;   // 0 = LP, 1 = BP, 2 = HP (continuous)
        aFilter = [
          SelectX.ar(mode, [XFade2.ar(lp12[0], lp24[0], (p4 * 2) - 1), XFade2.ar(bp12[0], bp24[0], (p4 * 2) - 1), XFade2.ar(hp12[0], hp24[0], (p4 * 2) - 1)]),
          SelectX.ar(mode, [XFade2.ar(lp12[1], lp24[1], (p4 * 2) - 1), XFade2.ar(bp12[1], bp24[1], (p4 * 2) - 1), XFade2.ar(hp12[1], hp24[1], (p4 * 2) - 1)])
        ];

        // eq: freq, gain, Q (p4 unused / reserved)
        aEq = BPeakEQ.ar(dry,
          LinExp.kr(p1, 0, 1, 90, 9000),
          LinExp.kr(p3, 0, 1, 0.4, 5),
          LinLin.kr(p2, 0, 1, -15, 15)
        );

        // mod: chorus/flanger — p1=rate, p2=depth, p3/p4 reserved
        aMod = DelayC.ar(dry, 0.06,
          SinOsc.kr(
            LinLin.kr(p1, 0, 1, 0.05, 4),
            [0, 1.5708],
            LinLin.kr(p2, 0, 1, 0.001, 0.018),
            0.006
          )
        );

        // space: FreeVerb2 — p1=room size, p2=damping
        aSpace = FreeVerb2.ar(dry[0], dry[1], 1,
          LinLin.kr(p1, 0, 1, 0.15, 0.95),
          LinLin.kr(p2, 0, 1, 0.1,  0.85)
        );

        // texture: AM ring mod with tanh soft clip + DC removal
        // p1 maps modulator frequency; DC from one-sided AM is removed per stage.
        aTexture = LeakDC.ar(
          (dry * SinOsc.ar(LinExp.kr(p1, 0, 1, 18, 2200))).tanh
        );

        // delay: CombC with feedback and tone filter
        // p1=time, p2=feedback, p3=freeze (not wired here, reserved), p4=tone
        aDelay = LPF.ar(
          CombC.ar(dry, 1.2,
            delayTime.clip(0.02, 1.19),
            LinExp.kr(p2.clip(0.001, 1), 0.001, 1, 0.25, 8)
          ),
          LinExp.kr(p4, 0, 1, 900, 12000)
        );

        // resonator: tuned metallic ringing / karplus textures
        tune   = LinExp.kr(p1, 0, 1, 0.003, 0.045);
        decay  = LinLin.kr(p2, 0, 1, 0.25, 6.5);
        excite = LinLin.kr(p3, 0, 1, 0.15, 1.4);
        tone   = LinExp.kr(p4, 0, 1, 900, 12000);
        combA  = CombC.ar(HPF.ar(dry * excite, 50), 0.08, tune, decay);
        combB  = CombC.ar(HPF.ar(dry * (excite * 0.8), 50), 0.08, (tune * 1.37).clip(0.003, 0.079), decay * 0.72);
        aResonator = LeakDC.ar(LPF.ar((combA + (combB * 0.7)).softclip, tone));

        // folded filter: multimode filter plus folded resonance path
        base = [
          SelectX.ar(mode, [lp24[0], bp24[0], hp24[0]]),
          SelectX.ar(mode, [lp24[1], bp24[1], hp24[1]])
        ];
        foldAmt = LinLin.kr(p4, 0, 1, 0.2, 8.0);
        folded = Fold.ar(BPF.ar(dry, cutoff, bpRq) * LinLin.kr(p2, 0, 1, 0.6, 10.0) * foldAmt, -1, 1);
        aFoldFilter = LeakDC.ar((base * 0.8) + (folded * 0.55));

        // formant: 3 staggered bandpasses for vowel-like tone
        formA  = LinExp.kr(p1, 0, 1, 250, 900);
        spread = LinLin.kr(p2, 0, 1, 1.2, 3.1);
        focus  = LinLin.kr(p3, 0, 1, 0.22, 0.05);
        formB  = (formA * spread).clip(400, 2800);
        formC  = (formB * 1.75).clip(700, 5200);
        aFormant = LeakDC.ar(LPF.ar(
          (BPF.ar(dry, formA, focus) * 1.2)
          + (BPF.ar(dry, formB, focus * 0.8) * 0.95)
          + (BPF.ar(dry, formC, focus * 0.65) * 0.7),
          LinExp.kr(p4, 0, 1, 1400, 12000)
        ));

        // tremolo: sine to square amplitude gating
        tremRate  = LinLin.kr(p1, 0, 1, 0.08, 18);
        tremDepth = LinLin.kr(p2, 0, 1, 0.0, 1.0);
        tremShape = p3;
        tremBias  = LinLin.kr(p4, 0, 1, 0.15, 0.85);
        tremWave  = XFade2.kr(SinOsc.kr(tremRate), LFPulse.kr(tremRate, 0, tremBias), (tremShape * 2) - 1).range(0, 1);
        aTremolo  = dry * ((1 - tremDepth) + (tremWave * tremDepth));

        // crusher: sample-rate + bit reduction
        crushRate = LinExp.kr(p1, 0, 1, 800, 22050);
        crushBits = LinLin.kr(p2, 0, 1, 4, 16);
        crushMix  = LinLin.kr(p3, 0, 1, 0.2, 1.0);
        aCrusher  = LPF.ar(Decimator.ar(dry, crushRate, crushBits), LinExp.kr(p4, 0, 1, 1200, 12000)) * crushMix;

        // frequency shifter: subtle shimmer to robot
        shiftFreq   = LinLin.kr(p1, 0, 1, 0.5, 800);
        shiftSpread = LinLin.kr(p2, 0, 1, 0, 20);
        aFreqShift  = LeakDC.ar([
          FreqShift.ar(dry[0], shiftFreq - shiftSpread),
          FreqShift.ar(dry[1], shiftFreq + shiftSpread)
        ]);
        aFreqShift  = LPF.ar(aFreqShift, LinExp.kr(p4, 0, 1, 1500, 12000)) * LinLin.kr(p3, 0, 1, 0.25, 1.0);

        // granular: freeze/scatter live input into grains
        grainDensity = LinExp.kr(p1, 0, 1, 3, 65);
        grainSize    = LinLin.kr(p2, 0, 1, 0.045, 0.28);
        grainScatter = LinLin.kr(p3, 0, 1, 0.0, 1.0);
        grainPitch   = LinLin.kr(p3, 0, 1, 0.7, 1.65);
        grainPan     = SinOsc.kr(LinLin.kr(p3, 0, 1, 0.08, 6), [0, 1.5708], grainScatter * 0.95);
        aGranular    = GrainIn.ar(2, Dust.kr(grainDensity), grainSize, HPF.ar(dry, 35), grainPan, -1, 96);
        aGranular    = PitchShift.ar(aGranular, 0.18, grainPitch, grainScatter * 0.08, grainScatter * 0.04);
        aGranular    = LPF.ar(LeakDC.ar((aGranular * 1.8).softclip), LinExp.kr(p4, 0, 1, 1000, 12000));

        // plate: brighter, tighter synthetic plate-style tail
        pre     = LinLin.kr(p3, 0, 1, 0.0, 0.055);
        room    = LinLin.kr(p1, 0, 1, 0.12, 0.9);
        damp    = LinLin.kr(p2, 0, 1, 0.35, 4.8);
        plateIn = DelayC.ar(dry, 0.06, pre);
        aPlate  = JPverb.ar(plateIn, room, damp, LinExp.kr(p4, 0, 1, 1500, 12000), 0.4, 0.7);
        aPlate  = LPF.ar(LeakDC.ar(aPlate), LinExp.kr(p4, 0, 1, 1600, 12000));

        // early reflections only: short clustered delays, no diffuse tail
        earlyA = DelayC.ar(dry, 0.08, LinLin.kr(p1, 0, 1, 0.004, 0.028));
        earlyB = DelayC.ar(dry.reverse, 0.08, LinLin.kr(p2, 0, 1, 0.009, 0.041));
        earlyC = DelayC.ar(dry, 0.08, LinLin.kr(p3, 0, 1, 0.013, 0.057));
        earlyD = DelayC.ar(dry.reverse, 0.08, (LinLin.kr(p1, 0, 1, 0.004, 0.028) + LinLin.kr(p3, 0, 1, 0.013, 0.057) * 0.5).clip(0.002, 0.078));
        aEarly = LPF.ar(LeakDC.ar((earlyA * 0.95) + (earlyB * 0.7) + (earlyC * 0.55) + (earlyD * 0.4)), LinExp.kr(p4, 0, 1, 1500, 12000));

        // haas spread: tiny channel offset for pseudo stereo from mono sources
        haasTime = LinLin.kr(p1, 0, 1, 0.001, 0.028);
        haasTilt = LinLin.kr(p3, 0, 1, -0.85, 0.85);
        aHaas = [
          DelayC.ar(dry[0], 0.03, ((haasTime * (1 - haasTilt.max(0))) + 0.0002).clip(0.0002, 0.029)),
          DelayC.ar(dry[1], 0.03, ((haasTime * (1 + haasTilt.min(0).abs)) + 0.0002).clip(0.0002, 0.029))
        ];
        aHaas = Balance2.ar(LPF.ar(aHaas[0], LinExp.kr(p4, 0, 1, 1500, 12000)), LPF.ar(aHaas[1], LinExp.kr(p4, 0, 1, 1500, 12000)), LinLin.kr(p2, 0, 1, -0.8, 0.8), 1);

        // tape saturation: soft knee with bias and HF rolloff
        satDrive = LinExp.kr(p1, 0, 1, 1.2, 10);
        satBias  = LinLin.kr(p2, 0, 1, -0.18, 0.18);
        aTapeSat = dry + satBias;
        aTapeSat = ((aTapeSat * satDrive) / (1 + ((aTapeSat * satDrive).abs))) * 1.2;
        aTapeSat = HPF.ar(LPF.ar(aTapeSat, LinExp.kr(p4, 0, 1, 1400, 10000)), 30) * LinLin.kr(p3, 0, 1, 0.35, 1.0);

        // transient shaper: attack suppression / sustain boost from envelope contrast
        fastEnv = Amplitude.kr(Mix.ar(dry.abs), 0.001, 0.018);
        slowEnv = Amplitude.kr(Mix.ar(dry.abs), 0.02, 0.22);
        attackCtrl = (fastEnv - slowEnv).max(0) * LinLin.kr(p1, 0, 1, 0, 2.5);
        sustainCtrl = slowEnv * LinLin.kr(p2, 0, 1, 0, 1.8);
        transientGain = ((1 - attackCtrl) + sustainCtrl).clip(0.15, 3.0);
        aTransient = LPF.ar((dry * transientGain) * LinLin.kr(p3, 0, 1, 0.35, 1.0), LinExp.kr(p4, 0, 1, 1200, 12000));

        // multiband soft clipper: split and clip each band separately
        low = LPF.ar(dry, 220);
        high = HPF.ar(dry, 2200);
        mid = dry - low - high;
        tilt = LinLin.kr(p2, 0, 1, -0.8, 0.8);
        lowDrive = LinExp.kr((p1 + tilt * 0.25).clip(0, 1), 0, 1, 1.0, 8.0);
        midDrive = LinExp.kr(p1, 0, 1, 1.0, 10.0);
        highDrive = LinExp.kr((p1 - tilt * 0.25).clip(0, 1), 0, 1, 1.0, 7.0);
        aMulticlip = ((low * lowDrive).softclip * 0.9) + ((mid * midDrive).softclip) + ((high * highDrive).softclip * 0.85);
        aMulticlip = LPF.ar(aMulticlip * LinLin.kr(p3, 0, 1, 0.3, 1.0), LinExp.kr(p4, 0, 1, 1300, 12000));

        wetL = Select.ar(eff, [dry[0], aFilter[0], aEq[0], aMod[0], aSpace[0], aTexture[0], aDelay[0], aResonator[0], aFoldFilter[0], aFormant[0], aTremolo[0], aCrusher[0], aFreqShift[0], aGranular[0], aPlate[0], aEarly[0], aHaas[0], aTapeSat[0], aTransient[0], aMulticlip[0], aPitch[0], aReverse[0], aSlapback[0], aConv[0], aSpectral[0], aAutoWah[0]]);
        wetR = Select.ar(eff, [dry[1], aFilter[1], aEq[1], aMod[1], aSpace[1], aTexture[1], aDelay[1], aResonator[1], aFoldFilter[1], aFormant[1], aTremolo[1], aCrusher[1], aFreqShift[1], aGranular[1], aPlate[1], aEarly[1], aHaas[1], aTapeSat[1], aTransient[1], aMulticlip[1], aPitch[1], aReverse[1], aSlapback[1], aConv[1], aSpectral[1], aAutoWah[1]]);

        // Wet/dry mix: amt 0 = dry, amt 1 = fully wet
        XFade2.ar(dry, [wetL, wetR], (amt * 2) - 1)
      }.value;

      // ---------- scene B --------------------------------------------------
      sceneB = {
        var amt, p1, p2, p3, p4, eff;
        var cutoff, rq, bpRq, lp12, lp24, bp12, bp24, hp12, hp24, mode, bFilter;
        var bEq, bMod, bSpace, bTexture, bDelay, bResonator, bFoldFilter, bFormant, bTremolo, bCrusher, bFreqShift, bGranular, bPlate, bEarly, bHaas, bTapeSat, bTransient, bMulticlip;
        var bPitch, bReverse, bSlapback, bConv, bSpectral, bAutoWah;
        var psPitch, psWindow, psDisp, psWet;
        var revBuf, revPhase, revTrig, revRate, revMix;
        var slapTime, slapTone, slapMix;
        var convBuf, convDecay, convDens, convWet;
        var fftBuf, fftChain, freezeAmt, smearAmt, specWet;
        var awEnv, awFloor, awRange, awCut, awRes, awMix;
        var excite, decay, tune, tone, combA, combB, base, folded, foldAmt;
        var formA, formB, formC, spread, focus;
        var tremRate, tremDepth, tremShape, tremBias, tremWave;
        var crushRate, crushBits, crushMix;
        var shiftFreq, shiftSpread;
        var grainDensity, grainSize, grainScatter, grainPitch, grainPan;
        var pre, room, damp, earlyA, earlyB, earlyC, earlyD, haasTime, haasTilt, plateIn;
        var satDrive, satBias, fastEnv, slowEnv, attackCtrl, sustainCtrl, transientGain;
        var low, mid, high, lowDrive, midDrive, highDrive, tilt;
        var wetL, wetR;

        amt = Lag.kr(sceneBAmount.clip(0, 1), 0.08);
        p1  = Lag.kr(sceneBParam1.clip(0, 1), 0.08);
        p2  = Lag.kr(sceneBParam2.clip(0, 1), 0.08);
        p3  = Lag.kr(sceneBParam3.clip(0, 1), 0.08);
        p4  = Lag.kr(sceneBParam4.clip(0, 1), 0.08);
        eff = sceneBEffect.clip(0, 25).round(1);

        // ---- 20 pitchShift: harmoniser / detune / octave ----
        // p1 = pitch ratio (octave down → octave up), p2 = detune spread,
        // p3 = dry+harmony blend (parallel interval), p4 = tone
        psPitch  = LinExp.kr(p1, 0, 1, 0.5, 2.0);          // -1oct .. +1oct
        psWindow = 0.2;
        psDisp   = LinLin.kr(p2, 0, 1, 0.0, 0.05);
        bPitch   = PitchShift.ar(dry, psWindow, psPitch, psDisp, psDisp * 0.5);
        // optional parallel harmony voice mixed in via p3
        psWet    = bPitch + (PitchShift.ar(dry, psWindow,
                        (psPitch * 1.5).clip(0.5, 2.0), psDisp, psDisp) * LinLin.kr(p3, 0, 1, 0.0, 0.9));
        bPitch   = LPF.ar(LeakDC.ar(psWet), LinExp.kr(p4, 0, 1, 1200, 12000));

        // ---- 21 reverse: record into LocalBuf, play backwards ----
        // p1 = segment length, p2 = playback rate, p4 = tone
        revBuf   = LocalBuf(SampleRate.ir * 1.0, 2).clear;
        revRate  = LinLin.kr(p2, 0, 1, 0.5, 1.5);
        revTrig  = Impulse.kr(LinExp.kr(p1, 0, 1, 6, 0.7));  // segment rate
        RecordBuf.ar(dry, revBuf, 0, 1, 0, 1, 1, 1, revTrig);
        revPhase = Phasor.ar(revTrig,
            BufRateScale.kr(revBuf) * revRate * -1,
            0, BufFrames.kr(revBuf),
            BufFrames.kr(revBuf));
        bReverse = BufRd.ar(2, revBuf, revPhase, 1, 4);
        bReverse = LPF.ar(LeakDC.ar(bReverse), LinExp.kr(p4, 0, 1, 1200, 12000));
        bReverse = bReverse.clip2(1.0);

        // ---- 22 slapback: single short repeat, no feedback ----
        // p1 = time (40-180ms), p4 = tone, p3 = repeat level
        slapTime = LinLin.kr(p1, 0, 1, 0.04, 0.18);
        slapTone = LinExp.kr(p4, 0, 1, 1200, 9000);
        bSlapback = dry + (LPF.ar(DelayC.ar(dry, 0.25, slapTime), slapTone)
                    * LinLin.kr(p3, 0, 1, 0.3, 0.95));
        bSlapback = LeakDC.ar(bSlapback);

        // ---- 23 convReverb: Convolution2 with short generated impulse ----
        // p1 = impulse decay (size), p2 = density, p4 = tone

        convDecay = LinLin.kr(p1, 0, 1, 0.05, 0.4);
        convDens  = LinLin.kr(p2, 0, 1, 0.3, 1.0);
        convBuf   = LocalBuf(2048, 1).clear;
        RecordBuf.ar(
            WhiteNoise.ar * EnvGen.ar(Env.perc(0.001, convDecay), Impulse.ar(0)),
            convBuf, 0, 1, 0, 1, 0, 0);
        bConv = Convolution2.ar(Mix.ar(dry) * convDens, convBuf, Impulse.kr(0), 2048);
        bConv = LPF.ar(LeakDC.ar(bConv ! 2), LinExp.kr(p4, 0, 1, 1500, 11000));

        // ---- 24 spectralFreeze: PV_MagFreeze smear / freeze ----
        // p1 = freeze gate, p2 = smear amount (PV_MagSmear), p4 = tone
        freezeAmt = (p1 > 0.5);   // > 50% = frozen, < 50% = live
        smearAmt = LinLin.kr(p2, 0, 1, 0, 16).round(1);
        fftBuf = LocalBuf(2048, 1);
        fftChain = FFT(fftBuf, Mix.ar(dry));
        fftChain = PV_MagSmear(fftChain, smearAmt);
        fftChain = PV_MagFreeze(fftChain, freezeAmt);
        bSpectral = IFFT(fftChain).dup;
        bSpectral = LPF.ar(LeakDC.ar(bSpectral), LinExp.kr(p4, 0, 1, 1400, 12000));

        // ---- 25 autoWah: amplitude follower into filter cutoff ----
        // p1 = sensitivity, p2 = range, p3 = resonance, p4 = base cutoff
        awEnv   = Amplitude.kr(Mix.ar(dry), 0.01, 0.15) * LinLin.kr(p1, 0, 1, 2, 30);
        awFloor = LinExp.kr(p4, 0, 1, 120, 800);
        awRange = LinExp.kr(p2, 0, 1, 400, 6000);
        awCut   = (awFloor + (awEnv * awRange)).clip(80, 12000);
        awRes  = LinLin.kr(p3, 0, 1, 0.6, 0.08);
        bAutoWah = LeakDC.ar(RLPF.ar(dry, Lag.kr(awCut, 0.02), awRes) * 1.4);

        cutoff = LinExp.kr(p1, 0, 1, 45, 12000);
        rq     = LinLin.kr(p2, 0, 1, 0.9, 0.06);
        bpRq   = rq.clip(0.08, 1.0);
        lp12   = RLPF.ar(dry, cutoff, rq);
        lp24   = RLPF.ar(lp12, cutoff, rq);
        bp12   = BPF.ar(dry, cutoff, bpRq);
        bp24   = BPF.ar(bp12, cutoff, bpRq);
        hp12   = RHPF.ar(dry, cutoff, rq);
        hp24   = RHPF.ar(hp12, cutoff, rq);
        mode   = p3 * 2;
        bFilter = [
          SelectX.ar(mode, [XFade2.ar(lp12[0], lp24[0], (p4 * 2) - 1), XFade2.ar(bp12[0], bp24[0], (p4 * 2) - 1), XFade2.ar(hp12[0], hp24[0], (p4 * 2) - 1)]),
          SelectX.ar(mode, [XFade2.ar(lp12[1], lp24[1], (p4 * 2) - 1), XFade2.ar(bp12[1], bp24[1], (p4 * 2) - 1), XFade2.ar(hp12[1], hp24[1], (p4 * 2) - 1)])
        ];

        bEq = BPeakEQ.ar(dry,
          LinExp.kr(p1, 0, 1, 90, 9000),
          LinExp.kr(p3, 0, 1, 0.4, 5),
          LinLin.kr(p2, 0, 1, -15, 15)
        );

        bMod = DelayC.ar(dry, 0.06,
          SinOsc.kr(
            LinLin.kr(p1, 0, 1, 0.05, 4),
            [0, 1.5708],
            LinLin.kr(p2, 0, 1, 0.001, 0.018),
            0.006
          )
        );

        bSpace = FreeVerb2.ar(dry[0], dry[1], 1,
          LinLin.kr(p1, 0, 1, 0.15, 0.95),
          LinLin.kr(p2, 0, 1, 0.1,  0.85)
        );

        bTexture = LeakDC.ar(
          (dry * SinOsc.ar(LinExp.kr(p1, 0, 1, 18, 2200))).tanh
        );

        bDelay = LPF.ar(
          CombC.ar(dry, 1.2,
            delayTime.clip(0.02, 1.19),
            LinExp.kr(p2.clip(0.001, 1), 0.001, 1, 0.25, 8)
          ),
          LinExp.kr(p4, 0, 1, 900, 12000)
        );

        tune   = LinExp.kr(p1, 0, 1, 0.003, 0.045);
        decay  = LinLin.kr(p2, 0, 1, 0.25, 6.5);
        excite = LinLin.kr(p3, 0, 1, 0.15, 1.4);
        tone   = LinExp.kr(p4, 0, 1, 900, 12000);
        combA  = CombC.ar(HPF.ar(dry * excite, 50), 0.08, tune, decay);
        combB  = CombC.ar(HPF.ar(dry * (excite * 0.8), 50), 0.08, (tune * 1.37).clip(0.003, 0.079), decay * 0.72);
        bResonator = LeakDC.ar(LPF.ar((combA + (combB * 0.7)).softclip, tone));

        base = [
          SelectX.ar(mode, [lp24[0], bp24[0], hp24[0]]),
          SelectX.ar(mode, [lp24[1], bp24[1], hp24[1]])
        ];
        foldAmt = LinLin.kr(p4, 0, 1, 0.2, 8.0);
        folded = Fold.ar(BPF.ar(dry, cutoff, bpRq) * LinLin.kr(p2, 0, 1, 0.6, 10.0) * foldAmt, -1, 1);
        bFoldFilter = LeakDC.ar((base * 0.8) + (folded * 0.55));

        formA  = LinExp.kr(p1, 0, 1, 250, 900);
        spread = LinLin.kr(p2, 0, 1, 1.2, 3.1);
        focus  = LinLin.kr(p3, 0, 1, 0.22, 0.05);
        formB  = (formA * spread).clip(400, 2800);
        formC  = (formB * 1.75).clip(700, 5200);
        bFormant = LeakDC.ar(LPF.ar(
          (BPF.ar(dry, formA, focus) * 1.2)
          + (BPF.ar(dry, formB, focus * 0.8) * 0.95)
          + (BPF.ar(dry, formC, focus * 0.65) * 0.7),
          LinExp.kr(p4, 0, 1, 1400, 12000)
        ));

        tremRate  = LinLin.kr(p1, 0, 1, 0.08, 18);
        tremDepth = LinLin.kr(p2, 0, 1, 0.0, 1.0);
        tremShape = p3;
        tremBias  = LinLin.kr(p4, 0, 1, 0.15, 0.85);
        tremWave  = XFade2.kr(SinOsc.kr(tremRate), LFPulse.kr(tremRate, 0, tremBias), (tremShape * 2) - 1).range(0, 1);
        bTremolo  = dry * ((1 - tremDepth) + (tremWave * tremDepth));

        crushRate = LinExp.kr(p1, 0, 1, 800, 22050);
        crushBits = LinLin.kr(p2, 0, 1, 4, 16);
        crushMix  = LinLin.kr(p3, 0, 1, 0.2, 1.0);
        bCrusher  = LPF.ar(Decimator.ar(dry, crushRate, crushBits), LinExp.kr(p4, 0, 1, 1200, 12000)) * crushMix;

        shiftFreq   = LinLin.kr(p1, 0, 1, 0.5, 800);
        shiftSpread = LinLin.kr(p2, 0, 1, 0, 20);
        bFreqShift  = LeakDC.ar([
          FreqShift.ar(dry[0], shiftFreq - shiftSpread),
          FreqShift.ar(dry[1], shiftFreq + shiftSpread)
        ]);
        bFreqShift  = LPF.ar(bFreqShift, LinExp.kr(p4, 0, 1, 1500, 12000)) * LinLin.kr(p3, 0, 1, 0.25, 1.0);

        grainDensity = LinExp.kr(p1, 0, 1, 3, 65);
        grainSize    = LinLin.kr(p2, 0, 1, 0.045, 0.28);
        grainScatter = LinLin.kr(p3, 0, 1, 0.0, 1.0);
        grainPitch   = LinLin.kr(p3, 0, 1, 0.7, 1.65);
        grainPan     = SinOsc.kr(LinLin.kr(p3, 0, 1, 0.08, 6), [0, 1.5708], grainScatter * 0.95);
        bGranular    = GrainIn.ar(2, Dust.kr(grainDensity), grainSize, HPF.ar(dry, 35), grainPan, -1, 96);
        bGranular    = PitchShift.ar(bGranular, 0.18, grainPitch, grainScatter * 0.08, grainScatter * 0.04);
        bGranular    = LPF.ar(LeakDC.ar((bGranular * 1.8).softclip), LinExp.kr(p4, 0, 1, 1000, 12000));

        pre     = LinLin.kr(p3, 0, 1, 0.0, 0.055);
        room    = LinLin.kr(p1, 0, 1, 0.12, 0.9);
        damp    = LinLin.kr(p2, 0, 1, 0.35, 4.8);
        plateIn = DelayC.ar(dry, 0.06, pre);
        bPlate  = JPverb.ar(plateIn, room, damp, LinExp.kr(p4, 0, 1, 1500, 12000), 0.4, 0.7);
        bPlate  = LPF.ar(LeakDC.ar(bPlate), LinExp.kr(p4, 0, 1, 1600, 12000));

        earlyA = DelayC.ar(dry, 0.08, LinLin.kr(p1, 0, 1, 0.004, 0.028));
        earlyB = DelayC.ar(dry.reverse, 0.08, LinLin.kr(p2, 0, 1, 0.009, 0.041));
        earlyC = DelayC.ar(dry, 0.08, LinLin.kr(p3, 0, 1, 0.013, 0.057));
        earlyD = DelayC.ar(dry.reverse, 0.08, (LinLin.kr(p1, 0, 1, 0.004, 0.028) + LinLin.kr(p3, 0, 1, 0.013, 0.057) * 0.5).clip(0.002, 0.078));
        bEarly = LPF.ar(LeakDC.ar((earlyA * 0.95) + (earlyB * 0.7) + (earlyC * 0.55) + (earlyD * 0.4)), LinExp.kr(p4, 0, 1, 1500, 12000));

        haasTime = LinLin.kr(p1, 0, 1, 0.001, 0.028);
        haasTilt = LinLin.kr(p3, 0, 1, -0.85, 0.85);
        bHaas = [
          DelayC.ar(dry[0], 0.03, ((haasTime * (1 - haasTilt.max(0))) + 0.0002).clip(0.0002, 0.029)),
          DelayC.ar(dry[1], 0.03, ((haasTime * (1 + haasTilt.min(0).abs)) + 0.0002).clip(0.0002, 0.029))
        ];
        bHaas = Balance2.ar(LPF.ar(bHaas[0], LinExp.kr(p4, 0, 1, 1500, 12000)), LPF.ar(bHaas[1], LinExp.kr(p4, 0, 1, 1500, 12000)), LinLin.kr(p2, 0, 1, -0.8, 0.8), 1);

        satDrive = LinExp.kr(p1, 0, 1, 1.2, 10);
        satBias  = LinLin.kr(p2, 0, 1, -0.18, 0.18);
        bTapeSat = dry + satBias;
        bTapeSat = ((bTapeSat * satDrive) / (1 + ((bTapeSat * satDrive).abs))) * 1.2;
        bTapeSat = HPF.ar(LPF.ar(bTapeSat, LinExp.kr(p4, 0, 1, 1400, 10000)), 30) * LinLin.kr(p3, 0, 1, 0.35, 1.0);

        fastEnv = Amplitude.kr(Mix.ar(dry.abs), 0.001, 0.018);
        slowEnv = Amplitude.kr(Mix.ar(dry.abs), 0.02, 0.22);
        attackCtrl = (fastEnv - slowEnv).max(0) * LinLin.kr(p1, 0, 1, 0, 2.5);
        sustainCtrl = slowEnv * LinLin.kr(p2, 0, 1, 0, 1.8);
        transientGain = ((1 - attackCtrl) + sustainCtrl).clip(0.15, 3.0);
        bTransient = LPF.ar((dry * transientGain) * LinLin.kr(p3, 0, 1, 0.35, 1.0), LinExp.kr(p4, 0, 1, 1200, 12000));

        low = LPF.ar(dry, 220);
        high = HPF.ar(dry, 2200);
        mid = dry - low - high;
        tilt = LinLin.kr(p2, 0, 1, -0.8, 0.8);
        lowDrive = LinExp.kr((p1 + tilt * 0.25).clip(0, 1), 0, 1, 1.0, 8.0);
        midDrive = LinExp.kr(p1, 0, 1, 1.0, 10.0);
        highDrive = LinExp.kr((p1 - tilt * 0.25).clip(0, 1), 0, 1, 1.0, 7.0);
        bMulticlip = ((low * lowDrive).softclip * 0.9) + ((mid * midDrive).softclip) + ((high * highDrive).softclip * 0.85);
        bMulticlip = LPF.ar(bMulticlip * LinLin.kr(p3, 0, 1, 0.3, 1.0), LinExp.kr(p4, 0, 1, 1300, 12000));

        wetL = Select.ar(eff, [dry[0], bFilter[0], bEq[0], bMod[0], bSpace[0], bTexture[0], bDelay[0], bResonator[0], bFoldFilter[0], bFormant[0], bTremolo[0], bCrusher[0], bFreqShift[0], bGranular[0], bPlate[0], bEarly[0], bHaas[0], bTapeSat[0], bTransient[0], bMulticlip[0], bPitch[0], bReverse[0], bSlapback[0], bConv[0], bSpectral[0], bAutoWah[0]]);
        wetR = Select.ar(eff, [dry[1], bFilter[1], bEq[1], bMod[1], bSpace[1], bTexture[1], bDelay[1], bResonator[1], bFoldFilter[1], bFormant[1], bTremolo[1], bCrusher[1], bFreqShift[1], bGranular[1], bPlate[1], bEarly[1], bHaas[1], bTapeSat[1], bTransient[1], bMulticlip[1], bPitch[1], bReverse[1], bSlapback[1], bConv[1], bSpectral[1], bAutoWah[1]]);

        XFade2.ar(dry, [wetL, wetR], (amt * 2) - 1)
      }.value;

      // ---------- crossfade and output -------------------------------------
      xfadeLag = Lag.kr(xfade.clip(0, 1), 0.05);
      sig = XFade2.ar(sceneA, sceneB, (xfadeLag * 2) - 1);
      sig = sig * Lag.kr(outputAmp, 0.05);
      sig = LeakDC.ar(Limiter.ar(sig, 0.98));
      sig = Select.ar(CheckBadValues.ar(sig, 0, 0) > 0, [sig, DC.ar(0)]);
      Out.ar(out, sig);

    }).add;

    context.server.sync;

    mainStage = Synth.new(\fadddddddderMain, [
      \inL,        this.getInL,
      \inR,        this.getInR,
      \out,        context.out_b.index,
      \inputAmp,   inputAmp,
      \outputAmp,  outputAmp,
      \xfade,      0,
      \delayTime,  this.beatToSeconds(currentBpm, 1)
    ], context.xg);

    // Scene A
    this.addCommand("set_scene_a_effect", "f", { |msg| mainStage.set(\sceneAEffect, msg[1]) });
    this.addCommand("set_scene_a_amount", "f", { |msg| mainStage.set(\sceneAAmount, msg[1]) });
    this.addCommand("set_scene_a_param1", "f", { |msg| mainStage.set(\sceneAParam1, msg[1]) });
    this.addCommand("set_scene_a_param2", "f", { |msg| mainStage.set(\sceneAParam2, msg[1]) });
    this.addCommand("set_scene_a_param3", "f", { |msg| mainStage.set(\sceneAParam3, msg[1]) });
    this.addCommand("set_scene_a_param4", "f", { |msg| mainStage.set(\sceneAParam4, msg[1]) });

    // Scene B
    this.addCommand("set_scene_b_effect", "f", { |msg| mainStage.set(\sceneBEffect, msg[1]) });
    this.addCommand("set_scene_b_amount", "f", { |msg| mainStage.set(\sceneBAmount, msg[1]) });
    this.addCommand("set_scene_b_param1", "f", { |msg| mainStage.set(\sceneBParam1, msg[1]) });
    this.addCommand("set_scene_b_param2", "f", { |msg| mainStage.set(\sceneBParam2, msg[1]) });
    this.addCommand("set_scene_b_param3", "f", { |msg| mainStage.set(\sceneBParam3, msg[1]) });
    this.addCommand("set_scene_b_param4", "f", { |msg| mainStage.set(\sceneBParam4, msg[1]) });

    // Crossfader
    this.addCommand("set_xfade", "f", { |msg| mainStage.set(\xfade, msg[1]) });

    // I/O
    this.addCommand("set_input_amp",          "f", { |msg| this.setInputAmp(msg[1]) });
    this.addCommand("set_output_amp",         "f", { |msg| this.setOutputAmp(msg[1]) });
    this.addCommand("set_num_input_channels", "i", { |msg| this.setNumInputChannels(msg[1]) });

    // Delay tempo — set_bpm and set_delay_sync are both required so the Lua
    // param actions don't silently fail.  The engine recomputes delayTime
    // whenever either changes.
    this.addCommand("set_bpm", "f", { |msg|
      currentBpm = msg[1].clip(20, 300);
      this.updateDelayTime;
    });
    this.addCommand("set_delay_sync", "i", { |msg|
      delaySyncMode = msg;
      this.updateDelayTime;
    });
  }

  // -------------------------------------------------------------------------

  beatToSeconds { |bpm, beats|
    ^(beats * 60) / bpm
  }

  updateDelayTime {
    var t;
    if (delaySyncMode == 1) {
      // One beat at the current BPM, clamped to the CombC buffer size.
      t = this.beatToSeconds(currentBpm, 1).clip(0.02, 1.19);
    } {
      // Free mode: half a beat gives a shorter slap-back style time.
      t = this.beatToSeconds(currentBpm, 0.5).clip(0.02, 1.19);
    };
    mainStage.set(\delayTime, t);
  }

  getInL {
    ^context.in_b[0].index
  }

  getInR {
    ^if (numInputChannels == 1) {
      context.in_b[0].index
    } {
      context.in_b[1].index
    }
  }

  setNumInputChannels { |n|
    numInputChannels = n;
    mainStage.set(\inL, this.getInL, \inR, this.getInR);
  }

  setInputAmp { |amp|
    inputAmp = amp;
    mainStage.set(\inputAmp, amp);
  }

  setOutputAmp { |amp|
    outputAmp = amp;
    mainStage.set(\outputAmp, amp);
  }

  free {
    mainStage.free;
  }
}
