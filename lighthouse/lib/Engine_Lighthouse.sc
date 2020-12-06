Engine_Lighthouse : CroneEngine {
  var bModulator, bTrig;
  var <sModulator, <sFilter;

  var seq;
  var seqDur = 0.25;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Lighthouse alloc".postln;

    bModulator = Bus.audio(context.server, 1);
    bTrig = Bus.control(context.server, 1);

    bTrig.set(0);
  
    context.server.sync;

    // main melodic pattern
    seq = Pn(
      Pwrand([
        Pseq([1, 0]),
        Pshuf(
          [
            Pseq([1, 0.5, 0]),
            Pseq([-0.5, 0, 0, 0]),
          ],
          Pxrand([1, 3, 4]),
        ),
      ], [0.1, 0.9]),
      inf
    ).asStream;
    Routine.new({
      loop {
        var param = seq.next * 0.05;
        sModulator.set(\vowelOffset, param);
        bTrig.set(1);
        seqDur.wait;
        bTrig.set(0);
      }
    }).play;

    SynthDef.new(\bgfModulator,
      { arg inBus = 2, outBus = 0, inAmp = 1, wvAmp = 0, index = 12, noise = 0, vowel = 0, vowelScale = 1, vowelOffset = 0;
        var in = In.ar(inBus, 1);

      var v1 = Vowel(\i, \tenor);
      var v2 = Vowel(\e, \tenor);
      // var v3 = Vowel(\a, \tenor);
      var v = v1.blend(v2, vowel);

      var sound = DriveNoise.ar(in, noise, multi: 1.5);
      sound = Mix.ar([
        sound * inAmp,
        Mix.ar(Resonz.ar(
          (sound * 3).clip + WhiteNoise.ar(0.05),
          freq: v.freqs * (vowelScale + vowelOffset),
          bwr: v.rqs,
          mul: v.amps
        )) * wvAmp * 10
      ]);

        Out.ar(outBus, sound);
      }
    ).add;

    SynthDef.new(\bgfFilter,
      { arg inBus = 2, outBus = 0, modBus = 2, gateBus = 0, basis = 0, amp = 0;
        var mod = In.ar(modBus, 1);
        var gate = In.kr(gateBus, 1);
  
        var sound = Mix.ar(
          // cool with mid lopass on korg, >0 basis,
          // gets squeezed digital sounds that are still in tune.
          WaveletDaub.ar(mod, which: basis),
        );

        Out.ar(outBus, Pan2.ar(
          sound,
          LFTri.kr(0.2, mul: basis.linlin(0, 4, 0, 1).clip(0, 0.8))
        ) * amp);
      }
    ).add;

    context.server.sync;
    
    sFilter = Synth.new(\bgfFilter, [
      \modBus, bModulator,
      \gateBus, bTrig,
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
    this.addCommand("vowel", "f", {|msg|
      sModulator.set(\vowel, msg[1]);
    });
    this.addCommand("vowelScale", "f", {|msg|
      sModulator.set(\vowelScale, msg[1]);
    });
    this.addCommand("noise", "f", {|msg|
      sModulator.set(\noise, msg[1]);
    });
    this.addCommand("ana", "f", {|msg|
      sModulator.set(\inAmp, 1 - msg[1]);
      sModulator.set(\wvAmp, msg[1]);
    });
    this.addCommand("basis", "f", {|msg|
      sFilter.set(\basis, msg[1]);
    });

    this.addCommand("noteOn", "i", {|msg|
      bTrig.set(1);
    });
    this.addCommand("noteOff", "i", {|msg|
      bTrig.set(0);
    });
  }

  free {
    sModulator.free;
    sFilter.free;
  }

} 
