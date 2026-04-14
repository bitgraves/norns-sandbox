Engine_Gormant : CroneEngine {
  var <sSynth, sSample;
  var bufSample;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Gormant alloc".postln;
    
    bufSample = Buffer.read(context.server, "/home/we/dust/code/bitgraves/samples/20260414-kurz-sample-looped.wav");
    context.server.sync;

    SynthDef.new(\gormant, {
      var in, snd, snd2, n, freqs;
      in = In.ar(context.in_b[0].index, 1);

      n = (1..16);
      freqs = \freq.kr(55 * -2.midiratio) * n * \spread.kr(1);
      freqs = freqs * (SinOsc.ar(ExpRand(0.3, 8)) * Rand(0, 0.2)).midiratio; // tape wobble
      snd2 = SinOsc.ar(freqs);
      snd2 = snd2 * ((n * 2pi * Line.kr(Rand(0, 0.3), Rand(0, 0.3), ExpRand(0.01, 0.5))) + Rand(0, 2pi)).cos;
      snd2 = snd2 / n;
      snd2 = snd2.sum;
      snd = in + (snd2 * \padGain.kr(0));

      snd = RLPF.ar(snd, \lpf.kr(16000), 0.1);
      snd = Latch.ar(snd, Impulse.ar(SinOsc.kr(1/20, 0, 200.0, 2000.0)));

      snd = snd ! 2;
      Out.ar(\out.kr(0), snd * \gain.kr(0));
    }).add;
    
    SynthDef.new(\gormantSample, {
      var snd;
      var bufnum = \bufnum.kr(0);
      var freq = BufDur.ir(bufnum).reciprocal;
      var rate = BufRateScale.kr(bufnum);
      snd = PlayBuf.ar(2, bufnum, rate: rate, trigger: Impulse.kr(freq), loop: 1) * LFSaw.kr(freq);
      snd = snd + (PlayBuf.ar(2, bufnum, rate: rate, trigger: Impulse.kr(freq, 0.5), loop: 1) * LFSaw.kr(freq, 1));
      snd = HPF.ar(snd, 150);
      Out.ar(\out.kr(0), snd * \gain.kr(0));
    }).add;

    context.server.sync;

    sSynth = Synth(\gormant, [
      \out, context.out_b.index],
    context.xg);
    
    sSample = Synth(\gormantSample, [
      \out, context.out_b.index],
    context.xg);

    this.addCommand("lpf", "f", {|msg|
      sSynth.set(\lpf, msg[1]);
    });
    this.addCommand("padGain", "f", {|msg|
      sSynth.set(\padGain, msg[1]);
    });
    this.addCommand("spread", "f", {|msg|
      sSynth.set(\spread, msg[1]);
    });
    
    this.addCommand("gain", "f", {|msg|
      sSynth.set(\gain, msg[1]);
    });
    
    this.addCommand("kurz", "f", {|msg|
      sSample.set(\gain, msg[1]);
    });
  }

  free {
    sSynth.free;
    sSample.free;
    bufSample.free;
  }

}
