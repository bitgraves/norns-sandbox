Engine_Stux : CroneEngine {
  var bCarrier;
  var <sCarrier, <sPerc, <sPad;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Stux alloc".postln;

    bCarrier = Bus.audio(context.server, 1);
    context.server.sync;

    SynthDef.new(\stxCarrier,
      { arg inBus = 2, outBus = 0;
        var in = In.ar(inBus, 1);
        var mix = Mix.ar([
          in,
          PitchShift.ar(in, pitchRatio: 0.5),
          PitchShift.ar(in, pitchRatio: 2),
        ]);
        Out.ar(outBus, mix);
      }
    ).add;

    SynthDef.new(\stxDistant,
      { arg inBus = 2, outBus = 0, amp = 0.5, trig = 0;
        var in = In.ar(inBus, 1);
        var env = EnvGen.kr(
          Env.perc(0.01, 10.0),
          gate: trig,
          levelScale: -1.0,
          levelBias: 1.0
        );
        var lpf = RLPF.ar(in, 1500);
        var shiftMess = FreeVerb.ar(
          Mix.ar(
            PitchShift.ar(lpf, pitchRatio: [4, 2], timeDispersion: 0.9, pitchDispersion: 0.05)
          ) * env,
          mix: 0.8,
        );
        Out.ar(outBus, Pan2.ar(shiftMess, 0) * amp);
      }
    ).add;
  
    SynthDef.new(\stxPoly,
      { arg inBus = 2, outBus = 0, amp = 0, percAmp = 1, trig = 0, attack = 0.01, release = 0.01, note = 0, fine = 0, rhythm = 0;
        var in = In.ar(inBus, 1);
        var envGate = Impulse.kr(Demand.kr(Dust.kr(2) + trig, 0, Diwhite.new(0, rhythm)));
        var allTrig = envGate + trig;
        var baseFreq = 55 * (note + fine).midiratio;
          var env = Env.perc(attack, release);
        var fs = FreqShift.ar(
          in,
          freq: SinOsc.ar(
            freq: baseFreq,
            mul: EnvGen.ar(
              env,
              levelScale: 8000 * percAmp,
              gate: allTrig,
            ),
          )
        );
        Out.ar(outBus, Pan2.ar(fs, 0) * amp/* * EnvGen.kr(env, allTrig) */);
      }
    ).add;
        
    context.server.sync;
    
    sPad = Synth.new(\stxDistant, [
      \inBus, context.in_b[0].index,
      \outBus, context.out_b.index],
    context.xg);
    sPerc = Synth.new(\stxPoly, [
      \inBus, bCarrier,
      \outBus, context.out_b.index],
    context.xg);
    sCarrier = Synth.new(\stxCarrier, [
      \inBus, context.in_b[0].index,
      \outBus, bCarrier],
    context.xg);

    // commands
    this.addCommand("amp", "f", {|msg|
      sPerc.set(\amp, msg[1] * 0.6);
    });
    this.addCommand("percAmp", "f", {|msg|
      sPerc.set(\percAmp, msg[1]);
    });
    this.addCommand("attack", "f", {|msg|
      sPerc.set(\attack, msg[1]);
    });
    this.addCommand("release", "f", {|msg|
      sPerc.set(\release, msg[1]);
    });
    this.addCommand("rhythm", "f", {|msg|
      sPerc.set(\rhythm, msg[1]);
    });
    this.addCommand("trig", "i", {|msg|
      sPerc.set(\trig, msg[1]);
      sPad.set(\trig, msg[1]);
    });
    this.addCommand("note", "i", {|msg|
      sPerc.set(\note, msg[1]);
    });
  }

  free {
    sCarrier.free;
    sPerc.free;
    sPad.free;
  }

} 