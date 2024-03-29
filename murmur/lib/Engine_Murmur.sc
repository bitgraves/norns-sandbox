Engine_Murmur : CroneEngine {
  var bFx;
  var sVox, sSamp, sFx;
  var bufSample;
  var voxSeq, rVox;
  var baseTempo;
  var tSeqClock;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Murmur alloc".postln;
    
    baseTempo = 1 / 0.4;
    tSeqClock = TempoClock.new(baseTempo);

    bFx = Bus.audio(context.server, 2);
    bufSample = Buffer.read(context.server, "/home/we/dust/code/bitgraves/samples/20220304-yt-drone-pesticide.wav");

    context.server.sync;
    
    voxSeq = Pbind(
      \dur, 0.2,
      \n, Pn(
        Pseq([
          Prand([
            Pseq([0, 12, 0]),
            Pseq([12, 0, 12]),
            Pseq([0, 0, 12]),
            Pseq([0, 12]),
            Pseq([0, 10, 0, 12]),
          ], 6),
          Prand([
            Pseq([12 + 5, 0, 12 + 7]),
            Pseq([12 + 5, 0, 12 + 5]),
            Pseq([0, 0, 12 + 7]),
            Pseq([12 + 7, 0, 0]),
            Pseq([12 + 5, -2]),
          ]),
        ]),
      ),
    ).asStream;
    rVox = Routine.new({
      loop {
        var e = voxSeq.next(());
        sVox.set(\n, e.n);
        e.dur.wait;
      }
    }).play(tSeqClock);

    SynthDef(\mmrVox,
      { arg inBus = 2, outBus = 0, amp = 1, vowel = 0, morphFreq = 5.8, noise = 0.1, trig = 0, harmonic = 1, vowelScale = 0.206, n = 0;
        var in = In.ar(inBus, 1);
        var sound = DriveNoise.ar(in, noise, multi: 1.5);
  
        // burst of sound decaying stepwise and changing pan less often.
        // var vowelHarmonic = TIRand.kr(1, harmonic, Impulse.kr(morphFreq));
        var v1 = Vowel(\e, \bass);
        var v2 = Vowel(\o, \bass);
        var v = v1.blend(v2, vowel.fold(0, 1));
  
        var pan = TRand.kr(-1, 1, Impulse.kr(morphFreq / 5.0));
        var env = EnvGen.kr(
          Env.asr(0.01, 1.0, 5.0),
          gate: trig,
        );
  
        sound = (Mix.ar(Resonz.ar(
          (sound * 3).clip + WhiteNoise.ar(0.1),
          freq: v.freqs * vowelScale.clip(0.2, 1) * n.midiratio,
          bwr: v.rqs * 0.9,
          mul: v.amps
        )) * 10).tanh;
  
        // remove some freqs for ENHANCEMENT
        sound = Mix.ar(BRF.ar(sound, vowel.linexp(0, 1, 220, 1760) * [1, 1.3, 1.6], 0.8, mul: 0.6));
        sound = HPF.ar(sound, amp.linexp(0, 1, 19000, 200));
        sound = Mix.ar([
          sound,
          MedianTriggered.ar(sound, Impulse.ar(1000)) * noise,
        ]);
  
        Out.ar(outBus, Pan2.ar(sound, pan)* env);
      }
    ).add;
    
    SynthDef.new(\mmrSamp, { |out = 0, bufnum, hpfFreq = 60, amp = 0|
      var sig = PlayBuf.ar(2, bufnum, BufRateScale.kr(bufnum), loop: 1.0);
      var hpf = HPF.ar(sig, hpfFreq.clip(60, 9000));
      Out.ar(out, hpf * amp);
    }).add;
  
    SynthDef.new(\mmrFx,
      { arg inBus = 2, outBus = 0, gate = 0, envDepth = 1, amp = 0;
        var in = In.ar(inBus, 2);
        Out.ar(outBus, in * amp);
      }
    ).add;

    context.server.sync;
    
    sFx = Synth.new(\mmrFx, [
      \inBus, bFx,
      \outBus, context.out_b.index],
    context.xg);
    sVox = Synth.new(\mmrVox, [
      \inBus, context.in_b[0].index,
      \outBus, bFx],
    context.xg);
    sSamp = Synth.new(\mmrSamp, [
      \bufnum, bufSample,
      \outBus, bFx],
    context.xg);
      
    // commands
    this.addCommand("amp", "f", {|msg|
      sFx.set(\amp, msg[1]);
    });
    this.addCommand("vowel", "f", {|msg|
      sVox.set(\vowel, msg[1]);
    });
    this.addCommand("noise", "f", {|msg|
      sVox.set(\noise, msg[1]);
    });
    this.addCommand("harmonic", "i", {|msg|
      sVox.set(\harmonic, msg[1]);
    });
    this.addCommand("scale", "f", {|msg|
      sVox.set(\vowelScale, msg[1]);
    });
    this.addCommand("morphFreq", "f", {|msg|
      sVox.set(\morphFreq, msg[1]);
    });
    this.addCommand("samp", "f", {|msg|
      sSamp.set(\amp, msg[1]);
    });
    this.addCommand("sampHpf", "f", {|msg|
      sSamp.set(\hpfFreq, msg[1]);
    });

    this.addCommand("noteOn", "i", {|msg|
      sVox.set(\trig, 1);
      sFx.set(\gate, 1);
      tSeqClock.play({
        var param = 1 + ((msg[1] - 4) * 0.15);
        tSeqClock.tempo = baseTempo * param;
      });
    });
    this.addCommand("noteOff", "i", {|msg|
      sVox.set(\trig, 0);
      sFx.set(\gate, 0);
    });
  }

  free {
    sVox.free;
    sFx.free;
    sSamp.free;
    bufSample.free;
    bFx.free;
    rVox.stop;
    voxSeq.free;
    tSeqClock.free;
  }

} 
