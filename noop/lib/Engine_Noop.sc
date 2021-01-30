Engine_Noop : CroneEngine {
  var <sNoise, <sPerc;
  var pKick, pBlip1, pBlip2, pSnare;
  var bNoiseGate, bPerc;
  var gKkGain = 0, gBlipGain = 0, gRampKick = 0;
  var mOut, mapping, initMidiPatterns, stopMidiPatterns;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Noop alloc".postln;

    TempoClock.tempo = 1.8;
    bNoiseGate = Bus.control(context.server, 1);
    bPerc = Bus.audio(context.server, 1);
    
    MIDIClient.init;
    mapping = ();
  
    context.server.sync;
    
    initMidiPatterns = {
      pBlip1 = Pchain(
        Ppar([
          Pbind(
            \midicmd, \noteOn,
            \chan, mapping.chan,
            \note, mapping.htc,
            \amp, Pn(
              Pseq([
                Pn(0, 6),
                Prand([1, 0.5, 0.25, 0], 4),
                Pn(0, 2),
              ]),
            ) * Pfunc({ gBlipGain }),
          ),
          Pbind(
            \midicmd, \control,
            \chan, mapping.controlChan,
            \ctlNum, mapping.hh_tune,
            \control, Pn(
              Pwrand([127, 92], [0.8, 0.2])
            ),
          ),
        ]),
        Pbind(
          \type, \midi,
          \midiout, mOut,
          \dur, 0.25,
        ),
      ).play(TempoClock.default);
      
      pBlip2 = Pchain(
        Ppar([
          Pbind(
            \midicmd, \noteOn,
            \chan, mapping.chan,
            \note, mapping.hh,
            \amp, Pn(
              Pseq([1, 0.1, 0.1]),
            ) * Pfunc({ gBlipGain }),
          ),
        ]),
        Pbind(
          \type, \midi,
          \midiout, mOut,
          \dur, Pn(
            Pseq([0.27, 0.25, 0.23])
          )
        ),
      ).play(TempoClock.default);
      
      pSnare = Pchain(
        Ppar([
          Pbind(
            \midicmd, \noteOn,
            \chan, mapping.chan,
            \note, mapping.sd,
            \amp, Pn(
              Pseq([
                Pn(0, 5),
                Pseq([1, 0, 1, 0, 0.5, 0.25]),
              ]),
            ) * Pfunc({ gBlipGain }),
          ),
        ]),
        Pbind(
          \type, \midi,
          \midiout, mOut,
          \dur, 0.25,
        ),
      ).play(TempoClock.default);
      
      // filtered bass pattern (synth - no midi)
      pKick = Pbind(
        \instrument, \bgnHit,
        \inBus, context.in_b[0].index,
        \outBus, bPerc,
        \group, context.xg,
        \n, Pgate(Pwhite(0, 24), key: \oct),
        \oct, Pn(Pwrand([0, 1], [0.9, 0.1])),
        \dur, Pn(
          Pwrand([
            Pseq([0.5, 0.5, 2]),
            Pseq([0.75, 0.75, 1.5]),
          ], [0.95, 0.05]),
        ),
        \attack, Pn(
          Pseq([
            0.01,
            Pif(
              Pfunc({ gRampKick >= 1 }),
              Prand([0.01, 0.05, 0.1, 0.25]),
              0.01
            ),
            0.01
          ]),
        ),
        \release, Pn(
          Pseq([0.5, 0.5, 1.5]),
        ),
        \amp, Pn(
          Pseq([
            Pwrand([0.9, 0], [0.9, 0.1]),
            Pwrand([0.85, 0.5, 0], [0.8, 0.1, 0.1]),
            1
          ]),
        ) * Pfunc({ gKkGain }),
        \outGateBus, bNoiseGate,
      ).play(TempoClock.default);
      nil
    };
    
    stopMidiPatterns = {
      pKick.stop;
      pBlip1.stop;
      pBlip2.stop;
      pSnare.stop;
    };
    
    SynthDef.new(\bgnHit,
      { | inBus = 2, outBus = 0, amp = 1, n = 0, attack = 0.01, release = 1, outGateBus |
        var in = In.ar(inBus, 1);
        var beat = IRand(1, 7);
        var nClamp =  n.clip(0, 48);
        var osc = Mix.ar(
          RLPF.ar(
            in,
            freq: [
              XLine.ar(900, 110 * nClamp.midiratio, 0.01),
              XLine.ar(901, (110 + beat) * nClamp.midiratio, 0.01),
              XLine.ar(1800, 220 * nClamp.midiratio, 0.01, mul: 0.5),
            ],
            rq: 0.5,
          );
        ).tanh;
        var mix = Mix.ar([
          osc, // bass
          LPF.ar(WhiteNoise.ar(mul: 0.7), XLine.ar(8000, 200, 0.01)), //click
        ]);
        var env = EnvGen.ar(
          Env.perc(attack, release),
          doneAction: Done.freeSelf
        );
        Out.ar(outBus, mix * env * amp);
        Out.kr(outGateBus, Impulse.kr(0) * amp);
      }
    ).add;

    SynthDef.new(\bgnNoise,
      { | inBus = 2, outBus = 0, clip = 0, gateBus = 0, amp = 0, duckAmount = 0, rez = 0, harm = 1, poly = 0 |
        var in = In.ar(inBus, 1);
        var gate = In.kr(gateBus, 1);
    
        var duck = EnvGen.kr(
          Env.perc(0.001, 2),
          gate: gate,
          levelScale: -1.25 * duckAmount,
          levelBias: 1.0,
        ).clip;
    
        var sound = DelayN.ar(in, 0.048);
        sound = Mix.ar([
          sound,
          SinOsc.ar(440 * -13.midiratio * harm, mul: rez),
        ]);
        sound = DriveNoise.ar(sound, 1, 1.5);
        sound = Mix.ar(Array.fill(5, { |i|
          CombL.ar(
            sound,
            0.05,
            LFNoise1.kr(0.05.rand, 0.02, 0.03),
            8,
            mul: poly.linlin(i * 0.2, (i + 1) * 0.2, 0, 0.33)
          )
        }));
        sound = sound + WhiteNoise.ar(0.05);
        sound = Mix.ar([
          sound * (1 - clip),
          SoftClipper4.ar(sound) * clip,
        ]);
        sound = HPF.ar(sound, 100);
        sound = sound * duck;
    
        Out.ar(outBus, Pan2.ar(sound) * amp);
      }
    ).add;
    
    SynthDef.new(\bgnPerc,
      { |inBus = 2, outBus = 0, gain = 1, noise = 0.1|
        var in = In.ar(inBus, 1);
        var sound = in; //DriveNoise.ar(in, noise, 2);
        Out.ar(outBus, Pan2.ar(sound * gain, 0));
      }
    ).add;

    context.server.sync;
    
    sPerc = Synth.new(\bgnPerc, [
      \inBus, bPerc,
      \outBus, context.out_b.index],
    context.xg);
    sNoise = Synth.new(\bgnNoise, [
      \inBus, context.in_b[0].index,
      \outBus, context.out_b.index,
      \gateBus, bNoiseGate],
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
    this.addCommand("harm", "f", {|msg|
      sNoise.set(\harm, msg[1]);
    });
    this.addCommand("rez", "f", {|msg|
      var param = msg[1];
      sNoise.set(\rez, param);
    });
    this.addCommand("clip", "f", {|msg|
      var param = msg[1];
      sNoise.set(\clip, param);
    });
    this.addCommand("polyNoise", "f", {|msg|
      var param = msg[1];
      sNoise.set(\poly, param);
    });
    this.addCommand("duck", "f", {|msg|
      var param = msg[1];
      sNoise.set(\duckAmount, param);
    });
    this.addCommand("amp", "f", {|msg|
      sNoise.set(\amp, msg[1]);
    });
    this.addCommand("kick", "f", {|msg|
      gKkGain = msg[1];
    });
    this.addCommand("click", "f", {|msg|
      gBlipGain = msg[1];
    });
    this.addCommand("kickRamp", "f", {|msg|
      gRampKick = msg[1];
    });
  }

  free {
    sNoise.free;
    bNoiseGate.free;
    bPerc.free;
    try {
      stopMidiPatterns.value;
      mOut.disconnect;
    } { |error|
      "disconnect midi failed (possibly never connected): %".format(error.species.name).postln;
    }
  }

} 
