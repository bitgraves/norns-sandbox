Engine_Rabies : CroneEngine {
  var bEffects;
  var <sNoise;
  var <sNoiseSub;
  var <sSines;
  var <sEffects;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Rabies alloc".postln;

    bEffects = Bus.audio(context.server, 2);
    context.server.sync;
    
    SynthDef.new(\rabiesEffects,
      { arg inBus, out, amp = 1, bend = 0;
        var in = In.ar(inBus, 2);
        Out.ar(out,
          PitchShift.ar(
            in,
            pitchRatio: bend.midiratio,
            mul: amp
          )
        );
      }
    ).add;
    
    SynthDef.new(\rabiesSeq,
      { arg inL, inR, out, seqFreq = 10.0, oscFreq = 110.0, amp = 1;
        var in = [In.ar(inL), In.ar(inR)];
        var mult = Demand.kr(
          trig: Impulse.kr(seqFreq),
          reset: 0,
          demandUGens: Dseq.new([0, 1, 2, 3, 4, 5], inf)
        );
        var voice = DiodeRingMod.ar(
          car: in * amp,
          mod: SinOsc.ar(oscFreq * mult)
        );
        Out.ar(out, voice.dup);
      }
    ).add;

    SynthDef.new(\rabiesSines,
      { arg inL, inR, out, amp = 1, freq = 440;
        var in = [In.ar(inL), In.ar(inR)];
	var voice = SinOsc.ar(freq);
	Out.ar(out, Pan2.ar(voice, 0.5) * amp);
      }
    ).add;

    SynthDef.new(\rabiesNoise,
      { arg inL, inR, out, amp = 1, freq = 440;
        var in = [In.ar(inL), In.ar(inR)];
        var voice = MedianTriggered.ar(
          in: in,
          trig: Blip.ar(freq)
        ) * amp;
        Out.ar(out, voice.dup);
      }
    ).add;
        
    context.server.sync;

    sEffects = Synth.new(\rabiesEffects, [
      \inBus, bEffects,
      \out, context.out_b.index,
      \amp, 1],
    context.xg);

    sNoise = Synth.new(\rabiesNoise, [
      \inL, context.in_b[0].index,      
      \inR, context.in_b[1].index,
      \out, bEffects,
      \amp, 0],
    context.xg);

    sNoiseSub = Synth.new(\rabiesNoise, [
      \inL, context.in_b[0].index,      
      \inR, context.in_b[1].index,
      \out, bEffects,
      \amp, 0],
    context.xg);

    sSines = Synth.new(\rabiesSines, [
      \inL, context.in_b[0].index,      
      \inR, context.in_b[1].index,
      \out, bEffects,
      \amp, 0],
    context.xg);

    // commands
    
    this.addCommand("mix", "f", {|msg|
      sNoiseSub.set(\amp, 1.0 - msg[1]);
      sNoise.set(\amp, msg[1]);
    });
    this.addCommand("noiseFreq", "f", {|msg|
      sNoise.set(\freq, msg[1]);
    });
    this.addCommand("subFreq", "f", {|msg|
      sNoiseSub.set(\freq, msg[1]);
    });
    this.addCommand("amp", "f", {|msg|
      sEffects.set(\amp, msg[1]);
    });
    this.addCommand("sineAmp", "f", {|msg|
      sSines.set(\amp, msg[1]);
    });
    this.addCommand("sineFreq", "f", {|msg|
      sSines.set(\freq, msg[1]);
    });
    this.addCommand("bend", "f", {|msg|
      var newBend = msg[1].linlin(0, 1, 0, -2);
      sEffects.set(\bend, newBend);
    });
  }

  free {
    sNoise.free;
    sNoiseSub.free;
    sSines.free;
    sEffects.free;
  }

} 