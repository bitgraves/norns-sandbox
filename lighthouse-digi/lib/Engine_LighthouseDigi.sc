Engine_LighthouseDigi : CroneEngine {
  var bModulator, bTrig, bPerc;
  var <sModulator, <sFilter, <sPerc;
  var <pKick, <pClick, <pClap, <pVowel;

  var seq;
  var gKickGain = 0, gClickGain = 0, gClapGain = 0;
  var tPercClock;
  
  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "LighthouseDigi alloc".postln;

    bModulator = Bus.audio(context.server, 1);
    bTrig = Bus.control(context.server, 1);
    bPerc = Bus.audio(context.server, 1);

    bTrig.set(0);
    tPercClock = TempoClock.new(1 / 0.8);
    TempoClock.tempo = 4; // default (non-perc)
    
    context.server.sync;

    // main melodic pattern
    seq = Pbind(
     \dur, Pn(
        Pshuf([
                Pn(1, 2),
                Pn(2, 1),
                Pn(0.5, 1),
                Pn(0.3, 2),
        ])
      )
    ).asStream;
    pVowel = Routine.new({
      loop {
        var e = seq.next(());
        bTrig.set(1);
        (e.dur * 0.1).wait;
        bTrig.set(0);
        (e.dur * 0.9).wait;
      }
    }).play(tPercClock);

    SynthDef.new(\bgfModulator,
      { arg inBus = 2, outBus = 0, inAmp = 1, wvAmp = 0, index = 12, noise = 0.2, vowel = 0, vowelScale = 1, gateBus = 2, sustain = 1;
        var in = In.ar(inBus, 1);

        var env = EnvGen.kr(
          Env.perc(0.01, 0.65),
          gate: In.kr(gateBus, 1),
          levelScale: 1 - sustain,
          levelBias: sustain,
        );

        var v1 = Vowel(\i, \bass);
        var v2 = Vowel(\a, \bass);
        // var v3 = Vowel(\a, \tenor);
        var v = v1.blend(v2, (vowel + SinOsc.kr(0.2, mul: 0.35 * env)).fold(0, 1));

        var sound = DriveNoise.ar(in, noise, multi: 1.5);
        
        sound = Mix.ar([
          sound * inAmp,
          Mix.ar(Resonz.ar(
            (sound * 3).clip + WhiteNoise.ar(0.1),
            freq: v.freqs * vowelScale,
            bwr: v.rqs,
            mul: v.amps
          )) * wvAmp * 10
        ]);
        sound = Mix.ar(sound);

        Out.ar(outBus, sound);
      }
    ).add;

    SynthDef.new(\bgfFilter,
      { arg inBus = 2, outBus = 0, modBus = 2, percBus = 0, basis = 0, amp = 0;
        var mod = In.ar(modBus, 1);
        var perc = In.ar(percBus, 1);

        var sound = Mix.ar(
          // cool with mid lopass on korg, >0 basis,
          // gets squeezed digital sounds that are still in tune.
          WaveletDaub.ar(mod, which: basis),
        );

        sound = Compander.ar(
          sound,
          perc,
          thresh: 0.35,
          slopeAbove: 0.1,
          clampTime: 0.005,
          relaxTime: 0.4,
        );

        Out.ar(outBus, Pan2.ar(
          sound,
          LFTri.kr(0.2, mul: basis.linlin(0, 4, 0, 1).clip(0, 0.8))
        ) * amp);
      }
    ).add;
    
    // rhythmic pattern
    pKick = Pbind(
      \instrument, \bgfBump,
      \outBus, bPerc,
      \dur, 0.1,
      \n, Pn(
        Pwrand([1, 12], [0.9, 0.1])
      ),
      \gain, Pn(
        Pwrand([
          Pseq([1, Pn(0, 11)]),
          Pseq([1, Pn(0, 12), 0.5, 0, 0]),
          Pseq([1, Pn(0, 7)]),
        ], [7, 3, 1].normalizeSum),
        inf
      ) * Pfunc({ gKickGain }),
    ).play(tPercClock);
    pClick = Pbind(
      \instrument, \bgfScatter,
      \outBus, bPerc,
      \dur, Pn(
        Pshuf([
          Pn(0.1, 16),
          Pn(0.1, 8),
          Pn(0.05, 12),
          Pn(0.15, 4),
        ])
      ),
      \gain, Pn(
        Pshuf([
          Prand([1, 0.7, 0.5, 0.25], 4),
          Pwrand([1, 0], [0.75, 0.25], 4),
          Pseq([1, 1, 1, 1]),
        ]),
        inf
      ) * Pfunc({ gClickGain }),
      \freq, Pn(
        Pwrand([10000, 8000], [0.9, 0.1])
      ),
    ).play(tPercClock);
    pClap = Pbind(
      \instrument, \bgfSnr,
      \outBus, bPerc,
      \release, Pn(
        Pwrand([0.08, 0.14], [0.9, 0.1])
      ),
      \dur, Pn(
        Pshuf([
          Pn(0.8, 2),
          Pn(0.8, 1),
          Pn(0.4, 3),
          Pn(0.6, 2),
        ])
      ),
      \gain, Pn(
        Pshuf([
          Prand([1, 0.7, 0.5, 0.25], 4),
          Pwrand([1, 0], [0.75, 0.25], 4),
          Pseq([1, 1, 1, 1]),
        ]),
        inf
      ) * Pfunc({ gClapGain }),
      \freq, Pn(
        Pwrand([1200, 500], [0.9, 0.1])
      ),
    ).play(tPercClock);

    SynthDef.new(\bgfBump,
      { |inBus = 2, outBus = 0, gain = 1, n = 0, release = 1|
        var beat = IRand(1, 5);
        var osc = Mix.ar(
          SinOsc.ar(
            freq: [
              XLine.ar(800, 40 * n.midiratio, 0.01),
              XLine.ar(801, (40 + beat) * n.midiratio, 0.01)
            ],
          );
        ).tanh;
        var mix = Mix.ar([
          osc,
          PinkNoise.ar()
        ]);
        var env = EnvGen.ar(
          Env.perc(0.01, release),
          doneAction: Done.freeSelf
        );
        Out.ar(outBus, osc * env * gain);
      }
    ).add;

    SynthDef.new(\bgfScatter,
      { |inBus = 2, outBus = 0, gain = 1, n = 0, release = 0.01, freq = 10000|
        var snd = Impulse.ar();// WhiteNoise.ar();
        var flt = (BPF.ar(snd, freq, 0.4) * gain * 4).tanh;
        var env = EnvGen.ar(
          Env.perc(0.001, release),
          doneAction: Done.freeSelf
        );
        Out.ar(outBus, flt * env);
      }
    ).add;

    SynthDef.new(\bgfSnr,
      { |inBus = 2, outBus = 0, gain = 1, n = 0, release = 0.01, freq = 10000|
        var snd = GrayNoise.ar();
        var flt = BPF.ar(
          snd + DelayN.ar(snd, 0.05, 0.05),
          freq: [freq, freq * 2.2, freq * 3.1],
          mul: [1, 0.8, 0.6],
          rq: 0.4
        );
        var env = EnvGen.ar(
          Env.perc(0.001, release),
          doneAction: Done.freeSelf
        );
        flt = Clip.ar(flt * 8) * 0.8;
        Out.ar(outBus, flt * env * gain);
      }
    ).add;

    SynthDef.new(\bgfPerc,
      { |inBus = 2, outBus = 0, gain = 0.9, noise = 0.1|
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
      \modBus, bModulator,
       \percBus, bPerc,
      \outBus, context.out_b.index],
    context.xg);
    sModulator = Synth.new(\bgfModulator, [
      \inBus, context.in_b[0].index,
      \gateBus, bTrig,
      \outBus, bModulator],
    context.xg);
  
    // commands
    this.addCommand("amp", "f", {|msg|
      sFilter.set(\amp, msg[1]);
    });
    this.addCommand("sustain", "f", {|msg|
      sModulator.set(\sustain, msg[1]);
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
    this.addCommand("kick", "f", {|msg|
      gKickGain = msg[1];
    });
    this.addCommand("click", "f", {|msg|
      gClickGain = msg[1];
    });
    this.addCommand("clap", "f", {|msg|
      gClapGain = msg[1];
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
    sPerc.free;
    pVowel.stop;
    pClap.stop;
    pClick.stop;
    pKick.stop;
    bModulator.free;
    bTrig.free;
  }

} 
