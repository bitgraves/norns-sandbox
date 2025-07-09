Engine_Sideb : CroneEngine {
  var bEffects;
  var <sSines;
  var <sEffects;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Sideb alloc".postln;

    bEffects = Bus.audio(context.server, 2);
    context.server.sync;
    
    SynthDef.new(\sidebEffects,
      { arg inBus, out, amp = 1, envAmount = 1, envFreq = 6, noiseGain = 0;
        var in = In.ar(inBus, 2);
        var gate = Impulse.kr(envFreq);
        var release = (1.0 / envFreq) * 3.0;
        var accents = envAmount * Demand.kr(gate, 0, Dseq([1, 0.5, 0.5, 0.5, 0.5], inf));
        var env = EnvGen.kr(
           Env.perc(0.01, release),
           gate,
           levelScale: accents,
           levelBias: 1.0 - envAmount,
        );
        
        var noise = HPF.ar(
          PinkNoise.ar(mul: noiseGain),
          TIRand.ar(800, 2000, gate),
        );
        var snd = Mix.ar([
          in * env,
          noise * (1.0 - env),
        ]);
        
        Out.ar(out,snd);
      }
    ).add;

    SynthDef.new(\sidebSines,
      { arg inL, inR, out, freq = 110, amp1 = 0, amp2 = 0, harm1 = 1, harm2 = 1;
        var in = In.ar(inL);
        
        var mod = SinOsc.ar(
          Lag2.kr(freq * harm1),
          in,
          amp1
        );
        var carrier = SinOsc.ar(
          Lag2.kr(freq * harm2),
          mod,
          amp2
        );
        
        Out.ar(out, Pan2.ar(carrier, 0.5));
      }
    ).add;
        
    context.server.sync;

    sEffects = Synth.new(\sidebEffects, [
      \inBus, bEffects,
      \out, context.out_b.index,
      \amp, 1],
    context.xg);

    sSines = Synth.new(\sidebSines, [
      \inL, context.in_b[0].index,      
      \inR, context.in_b[1].index,
      \out, bEffects,
      \amp, 0],
    context.xg);

    // commands

    this.addCommand("sidebAmp", "f", {|msg|
      sEffects.set(\amp, msg[1]);
    });
    this.addCommand("sineFreq", "f", {|msg|
      sSines.set(\freq, msg[1]);
    });
    this.addCommand("sineAmp1", "f", {|msg|
      sSines.set(\amp1, msg[1]);
    });
    this.addCommand("sineHarm1", "f", {|msg|
      sSines.set(\harm1, msg[1].round);
    });
    this.addCommand("sineAmp2", "f", {|msg|
      sSines.set(\amp2, msg[1]);
    });
    this.addCommand("sineHarm2", "f", {|msg|
      sSines.set(\harm2, msg[1].round);
    });
    this.addCommand("gate", "i", {|msg|
      sEffects.set(\gate, msg[1]);
    });
    this.addCommand("envAmount", "f", {|msg|
      sEffects.set(\envAmount, msg[1]);
    });
    this.addCommand("envFreq", "f", {|msg|
      sEffects.set(\envFreq, msg[1]);
    });
    this.addCommand("noiseGain", "f", {|msg|
      sEffects.set(\noiseGain, msg[1]);
    });
  }

  free {
    sSines.free;
    sEffects.free;
  }

} 