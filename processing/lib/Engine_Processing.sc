Engine_Processing : CroneEngine {
  var gBaseGrainFreq, gBaseImpulseDuration, gBaseFilterResonanceFreq, gDetuneRange, gIsMonotonic, gPShudder, gTriadVel;
  var bEffects, bNoteOffset, bFilterRadius, bMasterGate;
  var <sEffects, <rMasterGate;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Processing alloc".postln;
    
    gBaseGrainFreq = 55;
    gBaseImpulseDuration = 1.0 / gBaseGrainFreq;
    gBaseFilterResonanceFreq = 440;
    gDetuneRange = 0;
    gIsMonotonic = 0;
    gPShudder = 0;
    gTriadVel = 0;
    
    bEffects = Bus.audio(context.server, 2);
    bNoteOffset = Bus.control(context.server, 1);
    bFilterRadius = Bus.control(context.server, 1);
    bMasterGate = Bus.control(context.server, 1);
    bNoteOffset.set(0);
    bMasterGate.set(1);
    bFilterRadius.set(0.999);

    context.server.sync;
    
    // master gate
    rMasterGate = Routine.new(
      {
        loop {
          var pDuck = gPShudder * 0.6;
          if (1.0.rand < pDuck, {
            bMasterGate.set(0);
            0.015.wait;
            bMasterGate.set(1);
          }, nil);
          if (gPShudder > 0.7, {
            (0.03 + 0.07.rand).wait;
          }, {
            0.1.wait;
          });
        }
      }
    ).play;
    
    // effects bus
    SynthDef.new(\procEffects,
      { arg outBus = 0, inBus = 2, gateBus = 0, amp = 1;
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
          Env.asr(0.2, 1, 0.015),
          gate: In.kr(gateBus),
        );
        Out.ar(outBus, env * amp);
      }
    ).add;
    
    SynthDef.new(\procFilterGroup,
      { arg outBus = 0, inL, inR, gate = 1, noteIndex = 0, noteOffset = 0, grainFreq, detuneRange, filterRadius, isMonotonic, velocity = 1;
        var buf = Buffer.alloc(context.server, context.server.sampleRate * gBaseImpulseDuration, 2);
        var in = In.ar(inL).dup;
        var recorder = RecordBuf.ar(
          in, buf,
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
              2.pow((noteIndex + noteOffset) / 12) *
              (1.0 + detune)
            );
            freq = if(isMonotonic, RunningMax.kr(freq), freq);
            DelayL.ar(
              // Resonz.ar with bwr of 0.025 also worked ok
              SOS.ar(
                playbuf,
                a0: 1, a1: 0, a2: -1,
                b2: filterRadius.squared.neg,
                b1: 2.0 * filterRadius * cos(2pi * freq / context.server.sampleRate),
                mul: 0.015 * (1.0 - (index * 0.1)) * velocity,
              ),
              delaytime: (30 * detune.abs) / 1000,
              maxdelaytime: 30 / 1000,
            )
          }),
          0
        );
        var env = filterBuf * EnvGen.kr(
          Env.adsr(0.01, 0.2, 0.9, if(isMonotonic, 10.0, 8.0)),
          gate: Trig.kr(1.0, 3.0),
          doneAction: Done.freeSelf
        );
        Out.ar(outBus, Pan2.ar(env));
      }
    ).add;
        
    context.server.sync;
    
    sEffects = Synth.new(\procEffects, [
      \inBus, bEffects,
      \gateBus, bMasterGate,
      \out, context.out_b.index,
      \amp, 1],
    context.xg);

    // commands

    this.addCommand("noteOn", "if", {|msg|
      var index = msg[1];
      var note = Synth.new(\procFilterGroup, [
        \inL, context.in_b[0].index,
        \inR, context.in_b[1].index,
        \outBus, bEffects,
        \grainFreq, gBaseGrainFreq,
        \detuneRange, gDetuneRange,
        \isMonotonic, gIsMonotonic,
        \velocity, msg[2],
        \noteIndex, index],
      context.xg);
      note.map(\noteOffset, bNoteOffset);
      note.map(\filterRadius, bFilterRadius);
    });
    this.addCommand("amp", "f", {|msg|
      sEffects.set(\amp, msg[1]);
    });
    this.addCommand("noteOffset", "f", {|msg|
      bNoteOffset.set(msg[1].linlin(0, 1, 0, -24).round);
    });
    this.addCommand("pShudder", "f", {|msg|
      gPShudder = msg[1];
    });
    this.addCommand("detune", "f", {|msg|
      gDetuneRange = msg[1].linlin(0, 1, 0.0006, 0.08);
    });
    this.addCommand("monotonic", "i", {|msg|
      gIsMonotonic = msg[1];
    });
  }

  free {
    sEffects.free;
    rMasterGate.free;
  }

} 