Engine_Retina : CroneEngine {
  var bCarrier, bTrig, bDelay, bOscMix;
  var bufSamp;
  var <sTrembler, <sTrig, <sDelay, <sMonitor;
  var <notes;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Retina alloc".postln;

    bCarrier = Bus.audio(context.server, 1);
    bTrig = Bus.control(context.server, 1);
    bDelay = Bus.audio(context.server, 1);
    bOscMix = Bus.control(context.server, 1);
    bOscMix.set(0);
    bufSamp = Buffer.read(
      context.server,
      Platform.userHomeDir +/+ "dust/code/bitgraves/samples/street-child.wav"
    );
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
    
    SynthDef.new(\retSamp,
      { arg outBus = 0, gate = 1, buf = 0, index = 1;
        // var snd = PlayBuf.ar(2, buf, BufRateScale.kr(buf) * SinOsc.kr(0.08, mul: 0.008, add: 1), loop: 1) * -3.dbamp;
        
        var trigFreq = \speed.kr(0).linlin(0, 1, 0.5, 2);
  
        var segment = \segment.kr(0.04);
        var sweepFreq = (BufDur.ir(buf) * segment).reciprocal * trigFreq;
        var len = BufFrames.ir(buf) * segment * \tone.kr(1) * trigFreq.reciprocal;
        var add = Sweep.ar(1, 0.2) + Rand(0, 10);
  
        var snd = BufRd.ar(2, buf, LFSaw.ar(sweepFreq, add: add).range(0, len)) * -3.dbamp;
        
        snd = snd * EnvGen.kr(
          Env.adsr(1, 0.002, 1, 8),
          gate,
          doneAction: Done.freeSelf,
        );
        snd = snd.sum;
        snd = FreqShift.ar(snd, 100 * index);
        snd = HPF.ar(snd, 120);
        snd = snd * 5.dbamp;
        
        Out.ar(outBus, snd);
      }
    ).add;
  
    SynthDef.new(\retChordShape,
      { arg inBus = 2, outBus = 0, gate = 1, index = 0, oscMix = 0;
        var in = In.ar(inBus, 1);
        var shifts = [index.midiratio, (index + 5).midiratio, (index + 7).midiratio];
        var ps = PitShift.ar(
          in * -8.dbamp,
          shift: shifts,
        ).sum;
        
        var fFreq = 53.midicps; // sub-middle F
        var oscs = SinOsc.ar(fFreq * shifts, mul: 0.33).sum;
        
        var env = EnvGen.kr(
          Env.adsr(1, 0.002, 1, 8),
          gate,
          doneAction: Done.freeSelf,
        );
        
        var snd = XFade2.ar(ps, oscs, oscMix * 2 - 1);
        
        Out.ar(outBus, snd * env);
      }
    ).add;

    // TODO: this is a clone of \retChordShape but with different pitch shift indices
    SynthDef.new(\retChordShapeOct,
      { arg inBus = 2, outBus = 0, gate = 1, index = 0;
        var in = In.ar(inBus, 1);
        var ps = PitShift.ar(
          in * -8.dbamp,
          shift: [(index + 12).midiratio, (index + 31).midiratio],
        ).sum;
        var env = EnvGen.kr(
          Env.adsr(1, 0.002, 1, 8),
          gate,
          doneAction: Done.freeSelf,
        );
        Out.ar(outBus, ps * env);
      }
    ).add;
  
    SynthDef.new(\retTrembler,
      { arg inBus = 2, outBus = 0, delayBus, gateBus, amp = 1, destroy = 1, envDepth = 0;
        var in = In.ar(inBus, 1);
        var gate = In.kr(gateBus, 1);
        var env = EnvGen.kr(
          Env.adsr(0.03, 0.002, 1, 0.03),
          gate,
          levelScale: envDepth,
          levelBias: 1.0 - envDepth,
        );
        var snd = in;
        snd = HPF.ar(snd, 1000.0);
        snd = PitShift.ar(
          snd,
          shift: destroy
        );
        snd = snd * amp * env;
        Out.ar(outBus, [snd, DelayC.ar(snd, delaytime: 0.01)]);
        Out.ar(delayBus, snd);
      }
    ).add;
  
    SynthDef.new(\retModDelay,
      { arg inBus = 2, outBus = 0, amp = 0;
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
      var row = (index / 4).floor;
      
      if (notes[index] == nil,
        {
          var note;
          
          switch (row,
            0.0, {
              note = Synth.new(\retChordShape, [
                \inBus, context.in_b[0].index,
                \index, index + 24,
                \outBus, bCarrier],
              context.xg);
              note.map(\oscMix, bOscMix);
            },
            3.0, {
              note = Synth.new(\retSamp, [
                \buf, bufSamp,
                \index, index - 12,
                \outBus, bCarrier],
              context.xg);
            },
            {
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
      "note gate set off: %".format(index).postln;
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
    this.addCommand("oscMix", "f", {|msg|
      bOscMix.set(msg[1]);
    });
    this.addCommand("destroy", "f", {|msg|
      sTrembler.set(\destroy, msg[1]);
    });
    this.addCommand("envDepth", "f", {|msg|
      sTrembler.set(\envDepth, msg[1]);
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
    bCarrier.free;
    bDelay.free;
    bOscMix.free;
    bufSamp.free;
  }

} 