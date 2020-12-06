Engine_Forces : CroneEngine {
  var bCarrier, bModulator, bTrig, bContraTrig;
  var <sCarrier, <sModulator, <sFilter, <sContra;

  var seq;
  var seqDur = 0.15;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Forces alloc".postln;

    bCarrier = Bus.audio(context.server, 1);
    bModulator = Bus.audio(context.server, 1);
    bTrig = Bus.control(context.server, 1);
    bContraTrig = Bus.control(context.server, 1);

    bTrig.set(0);
    bContraTrig.set(0);
  
    context.server.sync;

    // main melodic pattern
    seq = Pn(
      Pwrand([
        Pseq([3, 2, 1, 0, 3, 2, 0]),
        Pshuf(
          [
            Pseq([3, 2, 1, Pwrand([0, -1], [0.75, 0.25]), 0]),
            Pseq([Pwrand([3, 2.5], [0.8, 0.2]), 2, 2, 1, 0]),
          ],
          Pxrand([1, 3, 4]),
        ),
      ], [0.1, 0.9]),
      inf
    ).asStream;
    Routine.new({
      loop {
        var param = 2.pow(seq.next);
        sFilter.set(\freqmult, param);
        bTrig.set(1);
        seqDur.wait;
        bTrig.set(0);
      }
    }).play;

    SynthDef.new(\bgfModulator,
      { arg inBus = 2, outBus = 0, inAmp = 0, wvAmp = 1, index = 17, wvBend = 0, noise = 0;
        var in = In.ar(inBus, 1);
        var mix = Mix.ar([
          in * inAmp,
        PitShift.ar(
          in: in,
          shift: (index + 12).midiratio,
          mul: wvAmp * 0.5
        ),
        PitchShift.ar(
          in: in,
          pitchRatio: index.midiratio,
          mul: wvAmp * 0.5,
        ),
        GrayNoise.ar(noise),
        ]);
        Out.ar(outBus, mix);
      }
    ).add;

    SynthDef.new(\bgfFilter,
      { arg inBus = 2, outBus = 0, modBus = 2, gateBus = 0, freqbase = 1, freqmult = 1, peaks = 32, amp = 0;
        var in = In.ar(inBus, 1);
        var mod = In.ar(modBus, 1);
        var gate = In.kr(gateBus, 1);

      var fft = FFT(LocalBuf(1024, 1), mod, wintype: 0);
      var sound = Mix.ar(
        TPV.ar(fft, 1024, 512, 32, peaks, freqbase * freqmult, mul: 1.8),
      );

        Out.ar(outBus, Pan2.ar(
        sound, Demand.kr(gate, 0, Dwhite.new(-1, 1))
      ) * amp);
      }
    ).add;

    SynthDef.new(\bgfContra,
      { arg inBus = 2, gateBus = 0, outBus = 0, amp = 1, index = 0;
        var in = In.ar(inBus, 1);
      var gate = In.kr(gateBus, 1);

      var freq = 440 * index.midiratio
      * TWChoose.kr(Dust.kr(10), [1, 0.5, 0.7, 1.5, 2], [10, 1, 1, 1, 1].normalizeSum); // glitch a bit
      var env = EnvGen.ar(
        Env.perc(3, 10),
        gate
      );
      var sound = Mix.ar([
        in,
        SinOsc.ar(freq) * 0.003,
      ]);

      sound = Mix.ar(
        Ringz.ar(
          sound,
          DelayN.kr(
            [freq, freq * 2, freq * 3, freq * 5],
            0.3,
            [0, 0.1, 0.2, 0.3],
          ),
          decaytime: 0.5,
          mul: 0.005 * [1.0, 0.9, 0.8, 0.7],
        ),
      );

        Out.ar(outBus,
        Pan2.ar(sound, 0) * amp * env
      );
      }
    ).add;

    context.server.sync;
  
    sFilter = Synth.new(\bgfFilter, [
      \inBus, bCarrier,
      \modBus, bModulator,
      \gateBus, bTrig,
      \outBus, context.out_b.index],
    context.xg);
    sContra = Synth.new(\bgfContra, [
      \inBus, bModulator,
      \gateBus, bContraTrig,
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
    this.addCommand("contraNoteOn", "i", {|msg|
      var index = msg[1];
      sContra.set(\index, index);
      bContraTrig.set(1);
    });
    this.addCommand("contraNoteOff", "i", {|msg|
      bContraTrig.set(0);
    });
  }

  free {
    sCarrier.free;
    sModulator.free;
    sFilter.free;
    sContra.free;
  }

} 
