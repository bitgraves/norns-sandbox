Engine_Linda : CroneEngine {
  var gBaseGrainFreq, gBaseImpulseDuration, gBaseFilterResonanceFreq, gDetuneRange;
  var bEffects, bNoteOffset, bFilterRadius, bFilterCreep, bFilterRand, bMasterGate;
  var <sEffects, <sModulateFilter, <sMasterGate;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Linda alloc".postln;
    
    gBaseGrainFreq = 293.66 / 4.0; // TODO: taken from linda patch
    gBaseImpulseDuration = 1.0 / gBaseGrainFreq;
    gBaseFilterResonanceFreq = 293.66;
    gDetuneRange = 0.08;
    
    bEffects = Bus.audio(context.server, 2);
    bNoteOffset = Bus.control(context.server, 1);
    bFilterRadius = Bus.control(context.server, 1);
    bFilterCreep = Bus.control(context.server, 1);
    bFilterRand = Bus.control(context.server, 1);
    bMasterGate = Bus.control(context.server, 1);
    bNoteOffset.set(-4);
    bFilterRadius.set(0.999); // max (0.996-0.999)
    bFilterCreep.set(0);
    bFilterRand.set(0);

    context.server.sync;
    
    // master gate
    SynthDef.new(\linMasterGate,
      { arg outBus, shudderDuration = 0.1;
        var gateFreq = 1.0 / (0.015 + shudderDuration);
        var gate = LFPulse.kr(
          freq: gateFreq,
          width: shudderDuration * gateFreq,
          mul: 2.0,
          add: -1.0
        );
        Out.kr(outBus, gate);
      }
    ).add;
    
    // modulate filter from master gate
    sModulateFilter = SynthDef.new(\linModulateFilter,
      { arg inGateBus, outFilterBus;
        var inGate = In.kr(inGateBus);
        Out.kr(outFilterBus, TRand.kr(0, 0.1, inGate));
      }
    ).add;
    
    // effects bus
    SynthDef.new(\linEffects,
      { arg inBus, inGateBus, out, amp = 1;
        var input = In.ar(inBus, 2);
        var dyno = Compander.ar(
          input,
          input,
          thresh: 0.5,
          slopeAbove: 0.1,
          clampTime: 0.05,
          relaxTime: 0.3
        );
        var env = dyno * EnvGen.kr(
          Env.asr(0.05, 1, 0.015),
          gate: In.kr(inGateBus),
        );
        Out.ar(out, env * amp);
      }
    ).add;
    
    SynthDef.new(\linFilterGroup,
      { arg out, inL, inR, noteIndex = 0, noteOffset = 0, filterCreep = 0, filterRand = 0, grainFreq, detuneRange, filterRadius;
        var buf = Buffer.alloc(context.server, context.server.sampleRate * gBaseImpulseDuration, 2);
        var inBus = In.ar(inL).dup;
        var recorder = RecordBuf.ar(
          inBus, buf,
          loop: 0,
        );
        var playbuf = PlayBuf.ar(
          2, buf,
          trigger: Impulse.ar(grainFreq)
        );
        var filterBuf = if(Done.kr(recorder),
          Mix.fill(3, { |index|
            var detune = if(index > 0, Rand(-1.0 * detuneRange, detuneRange), 0);
            var freq = (
              gBaseFilterResonanceFreq *
              (2 + filterCreep + filterRand).pow((noteIndex + noteOffset) / 12) *
              (1.0 + detune)
            );
            DelayL.ar(
              // Resonz.ar with bwr of 0.025 also worked ok
              SOS.ar(
                playbuf,
                a0: 1, a1: 0, a2: -1,
                b2: filterRadius.squared.neg,
                b1: 2.0 * filterRadius * cos(2pi * freq / context.server.sampleRate),
                mul: 0.015 * (1.0 - (index * 0.1)),
              ),
              delaytime: (30 * detune.abs) / 1000,
              maxdelaytime: 30 / 1000,
            )
          }),
          0
        );
        var env = filterBuf * EnvGen.kr(
          Env.adsr(3.0, 0.2, 0.9, 8.0),
          gate: Trig.kr(1.0, 3.0),
          doneAction: Done.freeSelf
        );
        Out.ar(out, Pan2.ar(env));
      }
    ).add;
        
    context.server.sync;
    
    sMasterGate = Synth.new(\linMasterGate, [
      \outBus, bMasterGate],
    context.xg);
    
    sModulateFilter = Synth.new(\linModulateFilter, [
      \inGateBus, bMasterGate,
      \outFilterBus, bFilterRand],
    context.xg);

    sEffects = Synth.new(\linEffects, [
      \inBus, bEffects,
      \inGateBus, bMasterGate,
      \out, context.out_b.index,
      \amp, 1],
    context.xg);

    // commands

    this.addCommand("noteOn", "i", {|msg|
      var index = msg[1];
      var note = Synth.new(\linFilterGroup, [
        \inL, context.in_b[0].index,
        \inR, context.in_b[1].index,
        \out, bEffects,
        \grainFreq, gBaseGrainFreq,
        \detuneRange, gDetuneRange,
        \noteIndex, index],
      context.xg);
      note.map(\noteOffset, bNoteOffset);
      note.map(\filterCreep, bFilterCreep);
      note.map(\filterRand, bFilterRand);
      note.map(\filterRadius, bFilterRadius);
    });
    this.addCommand("amp", "f", {|msg|
      sEffects.set(\amp, msg[1]);
    });
    this.addCommand("noteOffset", "f", {|msg|
      bNoteOffset.set(msg[1].round);
    });
    this.addCommand("filterCreep", "f", {|msg|
      bFilterCreep.set(msg[1]);
    });
    this.addCommand("shudderDuration", "f", {|msg|
      sMasterGate.set(\shudderDuration, msg[1].linlin(0, 1, 0.1, 0.01));
    });
  }

  free {
    sEffects.free;
    sModulateFilter.free;
    sMasterGate.free;
  }

} 
