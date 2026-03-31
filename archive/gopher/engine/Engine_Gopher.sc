Engine_Gopher : CroneEngine {
  var bEffects;
  var <sSines;
  var <sEffects;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Gopher alloc".postln;

    bEffects = Bus.audio(context.server, 2);
    context.server.sync;
    
    SynthDef.new(\gopherEffects,
      { arg inBus, out, amp = 1, bend = 0, gate = 0, release = 3;
        var in = In.ar(inBus, 2);
        var env = EnvGen.kr(
           Env.perc(0.01, release),
           gate
        );
        Out.ar(out,in * env);
      }
    ).add;

    SynthDef.new(\gopherSines,
      { arg inL, inR, out, freq = 110, amp1 = 1, amp2 = 1, harm1 = 1, harm2 = 1;
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

    sEffects = Synth.new(\gopherEffects, [
      \inBus, bEffects,
      \out, context.out_b.index,
      \amp, 1],
    context.xg);

    sSines = Synth.new(\gopherSines, [
      \inL, context.in_b[0].index,      
      \inR, context.in_b[1].index,
      \out, bEffects,
      \amp, 0],
    context.xg);

    // commands

    this.addCommand("gopherAmp", "f", {|msg|
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
    this.addCommand("release", "f", {|msg|
      sEffects.set(\release, msg[1]);
    });
  }

  free {
    sSines.free;
    sEffects.free;
  }

} 