Engine_Marbles : CroneEngine {
  var bCarrier, bModulator, bTrig;
  var <sCarrier ,<sModulator, <sFilter, <sLowMonitor;
  var cMaxLag = 2;
  var seq, dur;
  var seqDurQuant = 0.7;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Marbles alloc".postln;

    bCarrier = Bus.audio(context.server, 1);
    bModulator = Bus.audio(context.server, 1);
    bTrig = Bus.control(context.server, 1);

    bTrig.set(0);
  
    context.server.sync;

    // digital modulator pattern
    seq = Pn(
      Pwrand([
        Pseq([Prand([19, 22]), 0, 0, 0, 0, 0]),
        Pshuf(
          [
            Pseq([12, 0, 0]),
            Pseq([0, 0]),
            Pseq([-12, 0, 0, 0]),
          ],
          Pxrand([1, 3, 4]),
        ),
      ], [0.2, 0.8]),
      inf
    ).asStream;
  
    dur = Pn(
      Pwrand([1, 0.5], [0.9, 0.1], inf),
      inf
    ).asStream;
  
    Routine.new({
      loop {
        var param = seq.next.midiratio;
        sModulator.set(\wvRatio, param);
        sFilter.set(\digiRatio, param);
        bTrig.set(1);
        (dur.next * seqDurQuant).wait;
        bTrig.set(0);
      }
    }).play;
  
    SynthDef.new(\bgfCarrier,
      { arg inBus = 2, outBus = 0, amp = 1, gateBus = 0, noiseAmp = 0, sustain = 0.9;
        var in = In.ar(inBus, 1);
        var mix = DriveNoise.ar(in, noiseAmp, multi: 1.5);
        var gate = In.kr(gateBus, 1);
        var env = EnvGen.kr(
          Env.perc(0.04, 5.0),
          gate: gate,
          levelScale: 1 - sustain,
          levelBias: sustain,
        );
        Out.ar(outBus, mix * env * amp);
      }
    ).add;

    SynthDef.new(\bgfModulator,
      { arg inBus = 2, outBus = 0, inAmp = 0, wvAmp = 1, wvFreq = 220, wvRatio = 0;
        var in = In.ar(inBus, 1);
        var freq = wvFreq * wvRatio;
        var mix = Mix.ar([
          in * inAmp,
          SinOsc.ar(freq, mul: wvAmp),
        ]);
        Out.ar(outBus, mix);
      }
    ).add;
  
    SynthDef.new(\bgfFilter,
      { arg inBus = 2, outBus = 0, modBus = 2, digiFreq = 110, digiRatio = 1.0, lag = 0.1, spread = 1, amp = 0;
        var freq, hasFreq, sound, ampEnv;
        var in = In.ar(inBus, 1);
        var mod = In.ar(modBus, 1);
        var sustain = 0.8;
        var env;
  
        var lags = 9.collect({ |i| 0.6 - (i * 0.05) });
        var basePan, freqTrig;
        # freq, hasFreq = Pitch.kr(
          mod,
          initFreq: 55.0,
          maxFreq: 880.0,
          ampThreshold: 0.01
        );
        freqTrig = Changed.kr(Lag.kr(freq, 0.02));
        env = EnvGen.kr(
          Env.perc(0.01, 0.3),
          freqTrig,
          levelScale: -1,
          levelBias: 1,
        );
        basePan = Demand.kr(
          freqTrig,
          0,
          Dwhite.new(-1, 1)
        );
        sound = Mix.ar(Pan2.ar(
          BBandPass.ar(
            in + mod * 0.2,
            DelayL.kr(
              digiFreq * digiRatio * spread * [1, 2, 3, 7, 8, 9, 10, 11, 13],
              cMaxLag * lags,
              lag * lags,
            ),
            bw: [0.3, 0.2, 0.1, 0.09, 0.08, 0.07, 0.07, 0.06, 0.06],
            mul: 9.collect({ |i| 0.5 + (i * 0.1) }),
          ),
          basePan * -1.series(-0.8, 1))
        );
        Out.ar(outBus, sound * amp * env);
      }
    ).add;
  
    SynthDef.new(\bgfLowMonitor,
      { arg inBus = 2, outBus = 0, lpf = 20000, amp = 0;
        var in = In.ar(inBus, 1);
        in = BLPF.ar(in, freq: lpf);
        Out.ar(outBus, Pan2.ar(in, 0) * amp);
      }
    ).add;

    context.server.sync;
    
    sFilter = Synth.new(\bgfFilter, [
      \inBus, bCarrier,
      \modBus, bModulator,
      \outBus, context.out_b.index],
    context.xg);
    sCarrier = Synth.new(\bgfCarrier, [
      \inBus, context.in_b[0].index,
      \outBus, bCarrier,
      \gateBus, bTrig],
    context.xg);
    sModulator = Synth.new(\bgfModulator, [
      \inBus, context.in_b[0].index,
      \outBus, bModulator],
    context.xg);
    sLowMonitor = Synth.new(\bgfLowMonitor, [
      \outBus, context.out_b.index],
    context.xg);

  
    // commands
    this.addCommand("amp", "f", {|msg|
      sFilter.set(\amp, msg[1]);
    });
    this.addCommand("lowMonitorLpf", "f", {|msg|
      sLowMonitor.set(\lpf, msg[1]);
    });
    this.addCommand("lowMonitorAmp", "f", {|msg|
      sLowMonitor.set(\amp, msg[1]);
    });
    this.addCommand("carrierNoise", "f", {|msg|
      sCarrier.set(\noiseAmp, msg[1]);
    });
    this.addCommand("ana", "f", {|msg|
      sModulator.set(\inAmp, msg[1]);
      sModulator.set(\wvAmp, 1 - msg[1]);
    });
    this.addCommand("lag", "f", {|msg|
        var param = msg[1].linlin(0, 1, 0.1, cMaxLag);
        sFilter.set(\lag, param);
    });
    this.addCommand("freq", "f", {|msg|
      sModulator.set(\wvFreq, msg[1]);
      sFilter.set(\digiFreq, msg[1]);
    });
    this.addCommand("spread", "f", {|msg|
      sFilter.set(\spread, msg[1]);
    });

    this.addCommand("noteOn", "i", {|msg|
      bTrig.set(1);
    });
    this.addCommand("noteOff", "i", {|msg|
      bTrig.set(0);
    });
  }

  free {
    sCarrier.free;
    sModulator.free;
    sFilter.free;
    sLowMonitor.free;
  }

} 
