Engine_Lighthouse : CroneEngine {
  var bModulator, bTrig;
  var <sModulator, <sFilter;
  var <pKick, <pClick, <pClap, <pVowel;
  var mOut, mapping, initMidiPatterns, stopMidiPatterns;

  var seq;
  var gKickGain = 0, gClickGain = 0, gClapGain = 0;
  var tPercClock;
  
  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Lighthouse alloc".postln;

    bModulator = Bus.audio(context.server, 1);
    bTrig = Bus.control(context.server, 1);

    bTrig.set(0);
    tPercClock = TempoClock.new(1 / 0.8);
    TempoClock.tempo = 4; // default (non-perc)
    
    MIDIClient.init;
    mapping = ();
  
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
    
    initMidiPatterns = {
      // midi perc out
      pKick = Pchain(
        Ppar([
          Pbind(
            \midicmd, \noteOn,
            \chan, mapping.chan,
            \note, mapping.bd,
            \amp, Pfunc({
              if(Pkey(\dur) == 3, { gKickGain * 0.5 }, { gKickGain })
            }),
          ),
      
          Pbind(
            \midicmd, \control,
            \chan, mapping.controlChan,
            \ctlNum, mapping.bd1_tune,
            \control, Pn(
              Pwrand([72, 104], [0.9, 0.1])
            ),
          ),
        ]),
        Pbind(
          \type, \midi,
          \midiout, mOut,
          \dur, Pn( // TODO: is this running separately for each child?
            Pwrand([
              12,
              Pseq([13, 3]),
              8,
            ], [7, 3, 1].normalizeSum),
            inf
          ) * 0.1,
        ),
      ).play(tPercClock);
        
      pClick = Pchain(
        Ppar([
          Pbind(
            \midicmd, \noteOn,
            \chan, mapping.chan,
            \note, mapping.hh,
            \amp, Pn(
              Pshuf([
                Prand([1, 0.7, 0.5, 0.25], 4),
                Pwrand([1, 0], [0.75, 0.25], 4),
                Pseq([1, 1, 1, 1]),
              ]),
              inf
            ) * Pfunc({ gClickGain }),
          ),
      
          Pbind(
            \midicmd, \control,
            \chan, mapping.controlChan,
            \ctlNum, mapping.hh_tune,
            \control, Pn(
              Pwrand([127, 64], [0.9, 0.1])
            ),
          ),
        ]),
        Pbind(
          \type, \midi,
          \midiout, mOut,
          \dur, Pn(
              Pshuf([
                Pn(0.1, 16),
                Pn(0.1, 8),
                Pn(0.05, 12),
                Pn(0.15, 4),
              ])
            ),
        ),
      ).play(tPercClock);
      
      pClap = Pchain(
        Ppar([
          Pbind(
            \midicmd, \noteOn,
            \chan, mapping.chan,
            \note, mapping.cp,
            \amp, Pn(
              Pshuf([
                Prand([1, 0.7, 0.5, 0.25], 4),
                Pwrand([1, 0], [0.75, 0.25], 4),
                Pseq([1, 1, 1, 1]),
              ]),
              inf
            ) * Pfunc({ gClapGain }),
          ),
          Pbind(
            \midicmd, \control,
            \chan, mapping.controlChan,
            \ctlNum, mapping.cp_filter,
            \control, Pn(
              Pwrand([0.35, 0.25], [0.5, 0.5]) * 127
            ),
          ),
          Pbind(
            \midicmd, \control,
            \chan, mapping.controlChan,
            \ctlNum, mapping.cp_release,
            \control, Pn(
              Pwrand([0.08, 0.14], [0.9, 0.1]) * 127
            ),
          ),
        ]),
        Pbind(
          \type, \midi,
          \midiout, mOut,
          \dur, Pn(
            Pshuf([
              Pn(0.8, 2),
              Pn(0.8, 1),
              Pn(0.4, 3),
              Pn(0.6, 2),
            ])
          ),
        ),
      ).play(tPercClock);
      nil
    };
    
    stopMidiPatterns = {
      pKick.stop;
      pClick.stop;
      pClap.stop;
    };

    SynthDef.new(\bgfModulator,
      { arg inBus = 2, outBus = 0, inAmp = 1, wvAmp = 0, index = 12, noise = 0, vowel = 0, vowelScale = 1, gateBus = 2, sustain = 1;
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
          (sound * 3).clip + WhiteNoise.ar(0.05),
          freq: v.freqs * vowelScale,
          bwr: v.rqs,
          mul: v.amps
        )) * wvAmp * 10
      ]);

        Out.ar(outBus, sound);
      }
    ).add;

    SynthDef.new(\bgfFilter,
      { arg inBus = 2, outBus = 0, modBus = 2, basis = 0, amp = 0;
        var mod = In.ar(modBus, 1);

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
      \outBus, context.out_b.index],
    context.xg);
    sModulator = Synth.new(\bgfModulator, [
      \inBus, context.in_b[0].index,
      \gateBus, bTrig,
      \outBus, bModulator],
    context.xg);
  
    // commands
    this.addCommand("connectMidi", "i", {|msg|
      var destination = msg[1];
      try {
        // in case already connnected, reset
        stopMidiPatterns.value;
        mOut.disconnect;
      } { };
      
      try {
        mOut = MIDIOut.new(0); //.latency_(context.server.latency);
        mOut.connect(destination);
        initMidiPatterns.value;
      } { |error|
        "connect midi failed: %".format(error.species.name).postln;
      }
    });
    this.addCommand("addMidiMapping", "si", {|msg|
      "add midi mapping: % -> %".format(msg[1], msg[2]).postln;
      mapping.put(msg[1], msg[2]);
    });
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
    pVowel.stop;
    bModulator.free;
    bTrig.free;
    try {
      stopMidiPatterns.value;
      mOut.disconnect;
    } { |error|
      "disconnect midi failed (possibly never connected): %".format(error.species.name).postln;
    }
  }

} 
