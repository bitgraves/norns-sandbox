Engine_Noop : CroneEngine {
  var <sNoise;
  var pKick, pBlip1, pBlip2;
  var bNoiseGate;
  var gKkGain = 0, gBlipGain = 0, gRampKick = 0;
  var mOut, mapping, initMidiPatterns, stopMidiPatterns;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Noop alloc".postln;

    TempoClock.tempo = 1.8;
    bNoiseGate = Bus.control(context.server, 1);
    
    MIDIClient.init;
    mapping = ();
  
    context.server.sync;
    
    initMidiPatterns = {
      // midi perc out
      // TODO: BEN
      nil
    };
    
    stopMidiPatterns = {
      pKick.stop;
      pBlip1.stop;
      pBlip2.stop;
    };

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

    context.server.sync;
    
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
    sNoise.fre;
    bNoiseGate.free;
    try {
      stopMidiPatterns.value;
      mOut.disconnect;
    } { |error|
      "disconnect midi failed (possibly never connected): %".format(error.species.name).postln;
    }
  }

} 
