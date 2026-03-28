Engine_Seqbass : CroneEngine {
  var <sSynth, <sVox;
  var bufSample;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Seqbass alloc".postln;

    bufSample = Buffer.read(context.server, "/home/we/dust/code/bitgraves/samples/20260201-kuon-god.wav");
    context.server.sync;

    SynthDef.new(\bgseedbass, {
      var in = In.ar(context.in_b[0].index) * 15.dbamp;
      var seqArr = \sequence.kr([4, 2, 2]);
      var phaseArr = [0, 0.5];
      var tempo = Impulse.kr(1);
      var trig = Impulse.kr(
        Demand.kr(tempo, 0, Dseq(seqArr, inf)),
        Demand.kr(tempo, 0, Dseq(phaseArr, inf))
      );
      var env = \env.kr(0);
      var mix = \mix.kr(0);
      var atk = \attack.kr(0.001);
      var latchFreq = Demand.kr(Impulse.kr(12.reciprocal), 0, Drand(Array.interpolation(16, 8000, 16000), inf));
      var fold = Lag.kr(\fold.kr(1) * Demand.kr(tempo, 0, Dseq([1, 1, 0.5, 0.1], inf)), 0.05);

      var snd = (in * (1.0 - mix)) + SinOsc.ar(EnvGen.kr(Env.perc(0.001, 0.01), trig, LFNoise2.kr(2, 200, 800), 32.midicps), 0, mix * 0.9);
      snd = (RLPF.ar(snd, fold.linexp(1, 20, 16000, 2000), 0.7) * fold.dbamp).fold2;
      snd = snd * (-0.3 * fold).dbamp;
      snd = snd * EnvGen.ar(Env.adsr(atk, releaseTime: atk.linexp(0.001, 0.2, 0.7, 1.0), peakLevel: LFNoise2.ar(1, 0.04, env)), trig);
      snd = snd + Latch.ar((Hasher.ar(Sweep.ar(trig), 0.24) * EnvGen.ar(Env.perc(0.04, level: env), trig, -0.99, 1)), Impulse.ar(latchFreq));
      snd = RHPF.ar(snd, \hpf.kr(18000));

      Out.ar(\out.kr(0), snd ! 2);
    }).add;

    SynthDef(\bgseedvox, {
      var snd;
      var bufnum = \bufnum.kr(0);
      var trigFreq = \speed.kr(0).linlin(0, 1, 0.5, 2);
      var combFreq = (44 + \combnote.kr(0)).midicps;
      var combMix = \combMix.kr(0);

      var segment = \segment.kr(0.04);
      var sweepFreq = (BufDur.ir(bufnum) * segment).reciprocal * trigFreq;
      var len = BufFrames.ir(bufnum) * segment * \tone.kr(1) * trigFreq.reciprocal;
      var add = Sweep.ar(1, 0.2) + Rand(0, 10);

      snd = BufRd.ar(2, bufnum, LFSaw.ar(sweepFreq, add: add).range(0, len));
      snd = snd + Mix.ar(Pulse.ar({ combFreq * 2 * LFNoise2.kr(2, 0.05, 1) } ! 3, mul: 0.02));
      snd = (snd * -5.dbamp) + Mix.ar(CombC.ar(snd, [combFreq.reciprocal, (combFreq * 2).reciprocal], combFreq.reciprocal)) * -10.dbamp;
      snd = HPF.ar(snd, 150, -5.dbamp);
      snd = Compander.ar(snd, thresh: -2.dbamp, slopeAbove: 0.5, relaxTime: 0.01);
      snd = [snd, DelayC.ar(snd, SinOsc.kr(2, 0, 0.005, 0.01), 0.015)];

      Out.ar(\out.kr(0), snd * \gain.kr(0));
    }).add;

    context.server.sync;

    sSynth = Synth(\bgseedbass, [
      \sequence, [6, 6, 6],
      \out, context.out_b.index],
    context.xg);

    sVox = Synth(\bgseedvox, [
      \bufnum, bufSample,
      \out, context.out_b.index],
    context.xg);

    // bass commands
    this.addCommand("attack", "f", {|msg|
      sSynth.set(\attack, msg[1]);
    });
    this.addCommand("fold", "f", {|msg|
      sSynth.set(\fold, msg[1]);
    });
    this.addCommand("env", "f", {|msg|
      sSynth.set(\env, msg[1]);
    });
    this.addCommand("mix", "f", {|msg|
      sSynth.set(\mix, msg[1]);
    });
    this.addCommand("hpf", "f", {|msg|
      sSynth.set(\hpf, msg[1]);
    });

    // vox commands
    this.addCommand("voxGain", "f", {|msg|
      sVox.set(\gain, msg[1]);
    });
    this.addCommand("voxTone", "f", {|msg|
      sVox.set(\tone, msg[1]);
    });
    this.addCommand("voxSegment", "f", {|msg|
      sVox.set(\segment, msg[1]);
    });
    this.addCommand("voxCombnote", "i", {|msg|
      sVox.set(\combnote, msg[1]);
    });
  }

  free {
    sSynth.free;
    sVox.free;
    bufSample.free;
  }

}
