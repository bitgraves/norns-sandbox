Engine_Retina : CroneEngine {
  var bCarrier, bTrig, bDelay;
  var <sChordShape, <sChordShapeOct, <sTrembler, <sTrig, <sDelay, <sMonitor;
  var <notes;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
	  "Retina alloc".postln;

	  bCarrier = Bus.audio(context.server, 1);
	  bTrig = Bus.control(context.server, 1);
	  bDelay = Bus.audio(context.server, 1);
	  notes = Array.newClear(16 * 3);
	  
	  context.server.sync;
  	  
  	SynthDef.new(\retTrig,
  		{ arg outBus = 0, speed = 1, speedMul = 1;
  			var freq = speedMul * speed * Demand.kr(Dust.kr(10), 0, Diwhite(2, 5));
  			var width = 0.05 / freq.reciprocal;
  			var allTrig = LFPulse.kr(
  				freq: freq,
  				width: width,
  			);
  			Out.kr(outBus, allTrig);
  		}
  	).add;
  
  	sChordShape = SynthDef.new(\retChordShape,
  		{ arg inBus = 2, outBus = 0, gate = 1, index = 0;
  			var in = In.ar(inBus, 1);
  			var ps = PitShift.ar(
  				in,
  				shift: [index.midiratio, (index + 5).midiratio, (index + 7).midiratio],
  			);
  			var env = EnvGen.kr(
  				Env.adsr(1, 0.002, 1, 8),
  				gate,
  				doneAction: Done.freeSelf,
  			);
  			Out.ar(outBus, Mix.ar(ps * env));
  		}
  	).add;

  	// TODO: this is a clone of \retChordShape but with different pitch shift indices
  	sChordShapeOct = SynthDef.new(\retChordShapeOct,
  		{ arg inBus = 2, outBus = 0, gate = 1, index = 0;
  			var in = In.ar(inBus, 1);
  			var ps = PitShift.ar(
  				in,
  				shift: [(index + 12).midiratio, (index + 31).midiratio],
  			);
  			var env = EnvGen.kr(
  				Env.adsr(1, 0.002, 1, 8),
  				gate,
  				doneAction: Done.freeSelf,
  			);
  			Out.ar(outBus, Mix.ar(ps * env));
  		}
  	).add;
  
  	SynthDef.new(\retTrembler,
  		{ arg inBus = 2, outBus = 0, delayBus, gateBus, amp = 1, destroy = 1;
  			var in = In.ar(inBus, 1);
  			var gate = In.kr(gateBus, 1);
  			var env = EnvGen.kr(
  				Env.adsr(0.03, 0.002, 1, 0.03),
  				gate,
  			);
  			var shift = PitShift.ar(
  				RHPF.ar(in, 1000.0, 0.87),
  				shift: destroy
  			);
  			var result = shift * amp * env;
  			Out.ar(outBus, [result, DelayN.ar(result, delaytime: 0.007)]);
  			Out.ar(delayBus, result);
  		}
  	).add;
  
  	SynthDef.new(\retModDelay,
  		{ arg inBus = 2, outBus = 0, amp = 1;
  			var in = In.ar(inBus, 1);
  			var delay = DelayC.ar(in, 0.25, 0.25);
  			var pitch = PitchShift.ar(delay, pitchRatio: 0.5, mul: 0.8);
  			Out.ar(outBus, Pan2.ar(pitch * amp, 0));
  		}
  	).add;
  	
		SynthDef.new(\retMonitor,
  		{ arg inBus = 2, outBus = 0, sidechainBus = 2, amp = 0;
  			var in = In.ar(inBus, 1);
  			var sidechain = In.ar(sidechainBus, 1);
  			var duck = Compander.ar(
  				in,
  				sidechain,
  				thresh: 0.025,
  				slopeAbove: 0.4,
  				clampTime: 0.005,
  				relaxTime: 0.01,
  			);
  			Out.ar(outBus, Pan2.ar(duck, 0) * amp);
  		}
  	).add;
      	
    context.server.sync;
    
  	sMonitor = Synth.new(\retMonitor, [
	  	\sidechainBus, bDelay,
	  	\outBus, context.out_b.index],
	  context.xg);
    
  	sTrig = Synth.new(\retTrig, [
  		\outBus, bTrig],
  	context.xg);
  	
  	sDelay = Synth.new(\retModDelay, [
  		\inBus, bDelay,
  		\outBus, context.out_b.index],
  	context.xg);
  	
  	sTrembler = Synth.new(\retTrembler, [
  		\inBus, bCarrier,
  		\outBus, context.out_b.index,
  		\gateBus, bTrig,
  		\delayBus, bDelay],
  	context.xg);

    // commands
    
    this.addCommand("noteOn", "i", {|msg|
      var index = msg[1];
    	if (notes[index] == nil,
    		{
					var note;
					if (index < 4,
						{
							note = Synth.new(\retChordShape, [
							  \inBus, context.in_b[0].index,
								\index, index + 24,
								\outBus, bCarrier],
							context.xg);
						}, {
							note = Synth.new(\retChordShapeOct, [
							  \inBus, context.in_b[0].index,
								\index, index + 20,
								\outBus, bCarrier],
							context.xg);
						}
					);
    			note.onFree({
    				if (notes[index] == note,
    					notes[index] = nil,
    					nil
    				);
    			});
    			notes[index] = note;
    		},
    		{
    			notes[index].set(\gate, 1);
    		},
    	);
    });
    this.addCommand("noteOff", "i", {|msg|
      var index = msg[1];
			notes[index].set(\gate, 0);
    });
    this.addCommand("amp", "f", {|msg|
		  sTrembler.set(\amp, msg[1]);
		});
    this.addCommand("speed", "f", {|msg|
		  sTrig.set(\speed, msg[1]);
		});
    this.addCommand("speedMul", "f", {|msg|
		  sTrig.set(\speedMul, msg[1]);
		});
    this.addCommand("delayAmp", "f", {|msg|
		  sDelay.set(\amp, msg[1]);
		});
    this.addCommand("destroy", "f", {|msg|
		  sTrembler.set(\destroy, msg[1]);
		});
		this.addCommand("sidechainMonitor", "f", {|msg|
		  sMonitor.set(\amp, msg[1]);
		});
	}

	free {
    sTrembler.free;
    sTrig.free;
    sDelay.free;
    sMonitor.free;
	}

} 