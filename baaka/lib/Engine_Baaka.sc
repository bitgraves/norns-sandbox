Engine_Baaka : CroneEngine {
	var amp = 0;
	var bEffects;
	var <sModulate;
	var <sNoise;
	var <sEffects;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
	  "Baaka alloc".postln;

	  bEffects = Bus.audio(context.server, 2);
	  context.server.sync;
	  
	  SynthDef.new(\baakaEffects,
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
	  
  	SynthDef.new(\baakaSeq,
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
    
    SynthDef.new(\baakaNoise,
    	{ arg inL, inR, out, amp = 1, freq = 1000;
    		var in = [In.ar(inL), In.ar(inR)];
    		var voice = MedianTriggered.ar(
    			in: in,
    			trig: Dust.ar(freq)
    		) * amp;
    		Out.ar(out, voice.dup);
    	}
    ).add;
      	
    context.server.sync;

		sEffects = Synth.new(\baakaEffects, [
		  \inBus, bEffects,
		  \out, context.out_b.index,
		  \amp, 1],
		context.xg);

		sModulate = Synth.new(\baakaSeq, [
			\inL, context.in_b[0].index,			
			\inR, context.in_b[1].index,
			\out, bEffects,
			\amp, 0],
		context.xg);
		
		sNoise = Synth.new(\baakaNoise, [
			\inL, context.in_b[0].index,			
			\inR, context.in_b[1].index,
			\out, bEffects,
			\amp, 1],
		context.xg);

    // commands
    
    this.addCommand("mix", "f", {|msg|
    	sNoise.set(\amp, 1.0 - msg[1]);
			sModulate.set(\amp, msg[1]);
    });
    this.addCommand("noiseFreq", "f", {|msg|
      sNoise.set(\freq, msg[1]);
    });
		this.addCommand("amp", "f", {|msg|
		  sEffects.set(\amp, msg[1]);
		});
		this.addCommand("seqFreq", "f", {|msg|
		  sModulate.set(\seqFreq, msg[1]);
		});
		this.addCommand("oscFreq", "f", {|msg|
		  sModulate.set(\oscFreq, msg[1]);
		});
		this.addCommand("bend", "f", {|msg|
			var newBend = msg[1].linlin(0, 1, 0, -2);
		  sEffects.set(\bend, newBend);
		});
	}

	free {
    sModulate.free;
    sNoise.free;
    sEffects.free;
	}

} 