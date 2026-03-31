Engine_Noop : CroneEngine {
  var <sNoise, <sPerc;
  var pKick, pBd, pSnare;
  var bNoiseGate, bPerc;
  var gKkGain = 0, gRampKick = 0;
  var mOut, mapping, initMidiPatterns, stopMidiPatterns;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Noop alloc".postln;

    TempoClock.tempo = 1.5;
    bNoiseGate = Bus.control(context.server, 1);
    bPerc = Bus.audio(context.server, 1);
    
    MIDIClient.init;
    mapping = ();
  
    context.server.sync;
    
    initMidiPatterns = {
      /* pBd = Pchain(
        Ppar([
          Pbind(
            \midicmd, \noteOn,
            \chan, mapping.chan,
            \note, mapping.bd,
            \amp, 1 * Pfunc({ gKkGain }),
          ),
        ]),
        Pbind(
          \type, \midi,
          \midiout, mOut,
          \dur, 8,
        ),
      ).play(TempoClock.default); */
      
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
            ),
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
          Pseq([
            0.75,
            Prand([Pseq([1.25, 0.5]), Pseq([1.3, 0.45])]),
            1
          ]),
        ),
        \attack, 0.01,
        \release, Pn(
          Pseq([1, 1, 0.5, 1]),
        ),
        \amp, Pn(
          Pseq([
            Pwrand([0.9, 0.8], [0.9, 0.1]),
            Pwrand([0.85, 0.5, 0.5], [0.8, 0.1, 0.1]),
            1
          ]),
        ) * Pfunc({ gKkGain }),
        \outGateBus, bNoiseGate,
      ).play(TempoClock.default);
      nil
    };
    
    stopMidiPatterns = {
      // pBd.stop;
      pKick.stop;
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
          LPF.ar(WhiteNoise.ar(mul: 0.7), XLine.ar(8000, 100, 0.01)), //click
          SoftClipAmp4.ar(
            Mix.ar([
              SinOsc.ar(440 * -25.midiratio, mul: 0.25),
              SinOsc.ar(440 * -37.midiratio, mul: 0.5),
            ])
          ),
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
      { | inBus = 2, outBus = 0, clip = 1, gateBus = 0, amp = 0, duckAmount = 0, rez = 0, harm = 1, poly = 0 |
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
      { |inBus = 2, monitorBus = 2, monitorGain = 1, outBus = 0, gain = 1, lpf = 80|
        var in = In.ar(inBus, 1);
        var monitor = In.ar(monitorBus, 1) * monitorGain;
        var sound = Mix.ar([in, monitor]); //DriveNoise.ar(in, noise, 2);
        sound = RLPF.ar(sound, lpf.clip(80, 20000));
        Out.ar(outBus, Pan2.ar(sound * gain, 0));
      }
    ).add;

    context.server.sync;
    
    sPerc = Synth.new(\bgnPerc, [
      \inBus, bPerc,
      \monitorBus, context.in_b[1].index, // optionally pass thru norns 2nd audio input
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
    this.addCommand("drumsMonitorGain", "f", {|msg|
      sPerc.set(\monitorGain, msg[1]);
    });
    this.addCommand("kickRamp", "f", {|msg|
      gRampKick = msg[1];
    });
    this.addCommand("percLpf", "f", {|msg|
      sPerc.set(\lpf, msg[1]);
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
