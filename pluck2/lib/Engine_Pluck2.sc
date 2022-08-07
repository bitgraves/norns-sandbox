Engine_Pluck2 : CroneEngine {
  var gBaseGrainFreq, gBaseImpulseDuration, gBaseFilterResonanceFreq, gDetuneRange;
  var bEffects, bFilterRadius, bCombDecay, bGrainFreq, bMasterGate;
  var <sEffects, <sMasterGate, <sMonitor2;
  var notes;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Pluck2 alloc".postln;
    
    gBaseGrainFreq = 440.0 * -21.midiratio;
    gBaseImpulseDuration = 1.0 / gBaseGrainFreq;
    gBaseFilterResonanceFreq = gBaseGrainFreq;
    gDetuneRange = 0.01;
    
    bEffects = Bus.audio(context.server, 2);
    bFilterRadius = Bus.control(context.server, 1);
    bCombDecay = Bus.control(context.server, 1);
    bGrainFreq = Bus.control(context.server, 1);
    bMasterGate = Bus.control(context.server, 1);
    notes = Array.newClear(16);

    bFilterRadius.set(0.99); // max (0.996-0.999)
    bCombDecay.set(3.0);
    bGrainFreq.set(gBaseGrainFreq);

    context.server.sync;
    
    // master gate
    SynthDef.new(\procMasterGate,
      { arg shudderDuration = 0.1, outGateBus = 0;
        var mixDuration = SinOsc.kr(0.2, mul: 0.02, add: 0.1 + shudderDuration);
        var gateFreq = 1.0 / (0.015 + mixDuration);
        var gate = LFPulse.kr(
          freq: gateFreq,
          width: mixDuration * gateFreq,
          mul: 2.0,
          add: -1.0
        );
        var env = EnvGen.kr(
          Env.adsr(0.05, 0.85, 1, 0.1),
          timeScale: 1.0 / gateFreq,
          gate: gate,
        );
        Out.kr(outGateBus, env);
      }
    ).add;
    
    // effects bus
    SynthDef.new(\procEffects,
      { arg inBus = 2, inGateBus = 2, out = 0, amp = 1;
        var input = In.ar(inBus, 2);
        var dyno = Compander.ar(
          input,
          input,
          thresh: 0.5,
          slopeAbove: 0.1,
          clampTime: 0.05,
          relaxTime: 0.3
        );
        var env = dyno * In.kr(inGateBus);
        Out.ar(out, env * amp);
      }
    ).add;
    
    SynthDef.new(\procFilterGroup,
      { arg out = 0, in = 2, gate = 1, noteIndex = 0, grainFreq, detuneRange, filterRadius, combDecayTime = 3;
        var buf = Buffer.alloc(context.server, context.server.sampleRate * gBaseImpulseDuration, 1);
        var inBus = In.ar(in, 1);
        var noteFreq = grainFreq * 2.pow(noteIndex / 12);
  
        var recorder = RecordBuf.ar(
          inBus, buf,
          trigger: gate,
          loop: 0,
        );
        var playbuf = PlayBuf.ar(
          1, buf,
          // note: patch gets more resonant if you change this to Impulse.ar(noteFreq)
          trigger: gate,// Impulse.ar(1 / (gBaseImpulseDuration * 2)),
          loop: 1.0,
        ) * EnvGen.ar(
          Env.triangle(1.0 / noteFreq),
          levelScale: 0.125,
          timeScale: 2.0,
          gate: Impulse.kr(noteFreq / 8.0),
        );
  
        var filterBuf = if(Done.kr(recorder),
          Mix.fill(2, { |index|
            // var detune = if(index > 0, TRand(-1.0 * detuneRange, detuneRange, gate), 0);
            var detune = index * 0.01;
            var filterFreq = (
              noteFreq
              * (1.0 + detune)
            );
            var combFreq = (
              noteFreq
              * (1.0 + detune)
            );
            DelayL.ar(
              // Resonz.ar with bwr of 0.025 also worked ok
              HPF.ar(
                SOS.ar(
                  CombC.ar(
                    playbuf,
                    1 / gBaseFilterResonanceFreq,
                    1 / combFreq,
                    combDecayTime,
                  ),
                  a0: 1, a1: 0, a2: -1,
                  b2: filterRadius.squared.neg,
                  b1: 2.0 * filterRadius * cos(2pi * filterFreq / context.server.sampleRate),
                  mul: 0.015 * (1.0 - (index * 0.1)),
                ).tanh,
                gBaseFilterResonanceFreq,
              ),
              delaytime: (30 * detune.abs) / 1000,
              maxdelaytime: 30 / 1000,
            )
          }),
          0
        ).tanh;
        var env = filterBuf * EnvGen.kr(
          Env.adsr(0.1, 0.2, 0.9, 6.0),
          gate: gate,
        );
        Out.ar(out, Pan2.ar(env));
      }
    ).add;
    
    SynthDef.new(\procMonitor2,
      { arg in = 3, out = 0, gain = 0;
        Out.ar(out, In.ar(in, 1).dup * gain);
      }
    ).add;
        
    context.server.sync;
    
    sMasterGate = Synth.new(\procMasterGate, [
      \outGateBus, bMasterGate],
    context.xg);
    
    sEffects = Synth.new(\procEffects, [
      \inBus, bEffects,
      \inGateBus, bMasterGate,
      \out, context.out_b.index,
      \amp, 1],
    context.xg);
    
    sMonitor2 = Synth.new(\procMonitor2, [
      \in, context.in_b[1].index,
      \out, context.out_b.index],
    context.xg);

    // commands

    this.addCommand("noteOn", "i", {|msg|
      var index = msg[1];
      var mapping = [
        0, 5, 10, 15, 19, 8, 12, 17,
        0 + 12, 5 + 12, 10 + 12, 15 + 12, 19 + 12, 8 + 12, 12 + 12, 17 + 12
      ];
      var noteIndex = mapping[index.wrap(0, mapping.size)];
      if(notes[index] == nil, {
        var note = Synth.new(\procFilterGroup, [
          \in, context.in_b[0].index,
          \out, bEffects,
          \detuneRange, gDetuneRange,
          \noteIndex, noteIndex],
        context.xg);
        note.map(\filterRadius, bFilterRadius);
        note.map(\combDecayTime, bCombDecay);
        note.map(\grainFreq, bGrainFreq);
        "alloc note %".format(index).postln;
        notes[index] = note;
        nil
      }, {
        "retrigger note %".format(index).postln;
        notes[index].set(\gate, 1);
      });
    });
    this.addCommand("noteOff", "i", {|msg|
      var index = msg[1];
      if(notes[index] != nil, {
        "release note %".format(index).postln;
        notes[index].set(\gate, 0);
      }, nil);
    });
    this.addCommand("amp", "f", {|msg|
      sEffects.set(\amp, msg[1]);
    });
    this.addCommand("filterRadius", "f", {|msg|
      bFilterRadius.set(msg[1]);
    });
    this.addCommand("combDecay", "f", {|msg|
      bCombDecay.set(msg[1]);
    });
    this.addCommand("grainFreqMul", "f", {|msg|
      bGrainFreq.set(gBaseGrainFreq * msg[1]);
    });
    this.addCommand("detune", "f", {|msg|
      gDetuneRange = msg[1];
    });
    this.addCommand("monitor2", "f", {|msg|
      sMonitor2.set(\gain, msg[1]);
    });
    this.addCommand("shudderDuration", "f", {|msg|
      sMasterGate.set(\shudderDuration, msg[1].linlin(0, 1, 0.1, 0.01));
    });
  }

  free {
    notes.do({ |item| item.free; }); // needed?
    notes.free;
    sEffects.free;
    sMasterGate.free;
    bEffects.free;
    bFilterRadius.free;
    bCombDecay.free;
    bGrainFreq.free;
    bMasterGate.free;
  }

} 
