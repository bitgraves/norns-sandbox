Engine_Forces : CroneEngine {
  var bCarrier, bModulator, bTrig, bPerc;
  var <sCarrier, <sModulator, <sFilter, <sPerc;
  var <pKick, <pNoise, <pFilter;
  
  var gKickGain = 0;
  var gNoiseGain = 0;

  var seq;
  var seqDur = 0.05;
  var kickSeqMul = 1;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Forces alloc".postln;

    bCarrier = Bus.audio(context.server, 1);
    bModulator = Bus.audio(context.server, 1);
    bTrig = Bus.control(context.server, 1);
    bPerc = Bus.audio(context.server, 1);

    bTrig.set(0);
    TempoClock.tempo = 1 / seqDur;
  
    context.server.sync;
    
    pKick = Pbind(
      \instrument, \bgfBump,
      \outBus, bPerc,
      \dur, Pn(
        Pconst(16, Pwhite(2, 3))
      ) * Pfunc({ kickSeqMul }),
      \release, Pn(
        Pwrand([2, 4], [0.8, 0.2])
      ) * Pfunc({ seqDur }),
      \n, Pn(
        Pwrand([1, 4, -7], [0.8, 0.1, 0.1])
      ),
      \gain, Pfunc({ gKickGain }),
    ).play(TempoClock.default);
    SynthDef.new(\bgfBump,
      { |inBus = 2, outBus = 0, gain = 1, n = 0, release = 1|
        var beat = IRand(1, 7);
        var osc = Mix.ar(
          SinOsc.ar(
            freq: [
              XLine.ar(900, 44 * n.midiratio, 0.01),
              XLine.ar(901, (44 + beat) * n.midiratio, 0.01)
            ],
          );
        ).tanh;
        var mix = Mix.ar([
          osc, // bass
          LPF.ar(WhiteNoise.ar(mul: 0.7), XLine.ar(8000, 200, 0.01)), //click
        ]);
        var env = EnvGen.ar(
          Env.perc(0.01, release),
          doneAction: Done.freeSelf
        );
        Out.ar(outBus, mix * env * gain);
      }
    ).add;
    
    pNoise = Pbind(
      \instrument, \bgfNoise,
      \outBus, bPerc,
      \dur, 1,
      \release, Pn(
        Pwrand([0.1, 0.2], [0.8, 0.2])
      ) * Pfunc({ seqDur }),
      \freq, Pn(
        Pwrand([2000, 1500], [0.8, 0.2])
      ),
      \gain, Pn(
        Pwrand([1, 0.2], [0.8, 0.2])
      ) * Pfunc({ gNoiseGain }),
    ).play(TempoClock.default);
    SynthDef.new(\bgfNoise,
      { |inBus = 2, outBus = 0, gain = 1, n = 0, release = 0.01, freq = 10000|
        var snd = WhiteNoise.ar();
        var flt = Resonz.ar(snd, freq, 0.25);
        var env = EnvGen.ar(
          Env.perc(0.001, release),
          doneAction: Done.freeSelf
        );
        Out.ar(outBus, flt * env * gain * 2);
      }
    ).add;

    // main melodic pattern
    seq = Pbind(
      \freq, Pn(
        Pwrand([
          Pseq([3, 2, 1, 0, 3, 2, 0]),
          Pseq([-1, 0, 0.5, 1]),
          Pshuf(
            [
              Pseq([3, 2, 1, Pwrand([0, -1], [0.75, 0.25]), 0]),
              Pseq([Pwrand([3, 2.5], [0.6, 0.4]), 2, 2, 1, 0]),
            ],
            Pxrand([1, 3, 4]),
          ),
        ], [0.1, 0.1, 0.8]),
        inf
      ),
      \gate, Pn(
        Pwrand([0, 1], [0.9, 0.1])
      , inf),
    ).asStream;
    pFilter = Routine.new({
      loop {
        var e = seq.next(());
        sFilter.set(\freqmult, 2.pow(e.freq));
        if(e.gate == 1, {
          bTrig.set(1);
        }, nil);
        1.wait;
        bTrig.set(0);
      }
    }).play(TempoClock.default);

    SynthDef.new(\bgfModulator,
      { arg inBus = 2, outBus = 0, inAmp = 0, wvAmp = 1, index = 17, wvBend = 0, noise = 0;
        var in = In.ar(inBus, 1);
        var mix = Mix.ar([
          in * inAmp,
        PitShift.ar(
          in: in,
          shift: (index + 12).midiratio,
          mul: wvAmp * 0.4
        ),
        PitchShift.ar(
          in: in,
          pitchRatio: index.midiratio,
          mul: wvAmp * 0.4,
        ),
        GrayNoise.ar(noise),
        ]);
        Out.ar(outBus, mix);
      }
    ).add;

    SynthDef.new(\bgfFilter,
      { arg inBus = 2, outBus = 0, modBus = 2, gateBus = 0, percBus = 0, freqbase = 1, freqmult = 1, peaks = 32, amp = 0;
        var in = In.ar(inBus, 1);
        var mod = In.ar(modBus, 1);
        var gate = In.kr(gateBus, 1);
        var perc = In.ar(percBus, 1);

        var fft = FFT(LocalBuf(1024, 1), mod, wintype: 0);
        var sound = Mix.ar(
          TPV.ar(fft, 1024, 512, 32, peaks, freqbase * freqmult, mul: 1.8),
        );
        
        var duck = EnvGen.ar(
          Env.asr(0.01, 1, 0.01),
          gate,
          levelScale: -1,
          levelBias: 1,
        );
        
        sound = Compander.ar(
          sound,
          perc,
          thresh: 0.5,
          slopeAbove: 0.1,
          clampTime: 0.01,
          relaxTime: 0.5,
        );

        Out.ar(outBus, Pan2.ar(
          sound * duck, Demand.kr(gate, 0, Dwhite.new(-1, 1))
        ) * amp);
      }
    ).add;
    
    SynthDef.new(\bgfPerc,
      { |inBus = 2, outBus = 0, gain = 1, noise = 0.1|
        var in = In.ar(inBus, 1);
        var sound = DriveNoise.ar(in, noise, 2);
        Out.ar(outBus, Pan2.ar(sound * gain, 0));
      }
    ).add;

    context.server.sync;
  
    sPerc = Synth.new(\bgfPerc, [
      \inBus, bPerc,
      \outBus, context.out_b.index],
    context.xg);
    sFilter = Synth.new(\bgfFilter, [
      \inBus, bCarrier,
      \modBus, bModulator,
      \gateBus, bTrig,
      \percBus, bPerc,
      \outBus, context.out_b.index],
    context.xg);
    sModulator = Synth.new(\bgfModulator, [
      \inBus, context.in_b[0].index,
      \outBus, bModulator],
    context.xg);

    // commands
    this.addCommand("amp", "f", {|msg|
      sFilter.set(\amp, msg[1]);
    });
    this.addCommand("peaks", "f", {|msg|
      sFilter.set(\peaks, msg[1].round);
    });
    this.addCommand("noise", "f", {|msg|
      sModulator.set(\noise, msg[1].round);
    });
    this.addCommand("ana", "f", {|msg|
      sModulator.set(\inAmp, msg[1]);
      sModulator.set(\wvAmp, 1 - msg[1]);
    });
    this.addCommand("freqbase", "f", {|msg|
      sFilter.set(\freqbase, msg[1]);
    });
    this.addCommand("seqDur", "f", {|msg|
      seqDur = msg[1];
      TempoClock.tempo = 1 / seqDur;
    });
    this.addCommand("kickGain", "f", {|msg|
      gKickGain = msg[1];
    });
    this.addCommand("noiseGain", "f", {|msg|
      gNoiseGain = msg[1];
    });
    this.addCommand("kickSeqMul", "f", {|msg|
      kickSeqMul = msg[1];
    });
    this.addCommand("filterNoteOn", "i", {|msg|
      var index = msg[1];
      var param = 2.pow(index - 4);
      sFilter.set(\freqmult, param);
      bTrig.set(1);
    });
    this.addCommand("filterNoteOff", "i", {|msg|
      bTrig.set(0);
    });
  }

  free {
    sCarrier.free;
    sModulator.free;
    sFilter.free;
    pKick.stop;
    pNoise.stop;
    pFilter.stop;
  }

} 
