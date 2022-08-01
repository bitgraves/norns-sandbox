Engine_Bigfish : CroneEngine {
  var gBaseGrainFreq, gBaseImpulseDuration, gBaseFilterResonanceFreq, gDetuneRange, gPaperGain;
  var bEffects, bPerc, bFilterRadius, bGrainFreq, bNoise, bSidechainTrig;
  var tPercClock, pPaper;
  var sEffects, sPerc;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Bigfish alloc".postln;
    
    gBaseGrainFreq = 440.0 * -27.midiratio;
    gBaseImpulseDuration = 1.0 / gBaseGrainFreq;
    gBaseFilterResonanceFreq = gBaseGrainFreq;
    gDetuneRange = 0.01;
    gPaperGain = 0;
    
    bEffects = Bus.audio(context.server, 2);
    bPerc = Bus.audio(context.server, 1);
    bFilterRadius = Bus.control(context.server, 1);
    bNoise = Bus.control(context.server, 1);
    bGrainFreq = Bus.control(context.server, 1);
    bSidechainTrig = Bus.control(context.server, 1);
    
    tPercClock = TempoClock.new(1 / 0.7);

    bFilterRadius.set(0.99); // max (0.996-0.999)
    bNoise.set(1);
    bGrainFreq.set(gBaseGrainFreq);

    context.server.sync;
    
    // perc bus
    SynthDef.new(\fishPerc,
      { arg outBus = 0, amp = 1, inBus = 2;
        var input = In.ar(inBus, 1);
        Out.ar(outBus, Pan2.ar(input) * amp);
      }
    ).add;
    
    // effects bus
    SynthDef.new(\fishEffects,
      { arg outBus = 0, amp = 1, inTrigBus = 2, inEffectsBus = 2;
        var input = In.ar(inEffectsBus, 2);
        var dyno = Compander.ar(
          input,
          input,
          thresh: 0.5,
          slopeAbove: 0.1,
          clampTime: 0.05,
          relaxTime: 0.3
        );
        var env = EnvGen.ar(
          Env.perc(0.01, 0.5),
          // Impulse.kr(3),
          In.kr(inTrigBus, 1),
          levelScale: -0.75,
          levelBias: 1.0,
        );
        Out.ar(outBus, dyno * env * amp);
      }
    ).add;
    
  SynthDef.new(\fishFilterGroup,
      { arg out = 0, in = 2, gate = 1, noteIndex = 0, grainFreq, detuneRange, filterRadius, noise = 1;
        var noteFreq = grainFreq * 2.pow(noteIndex / 12);
        var playbuf = In.ar(in, 1);
        var voice = Mix.fill(3, { |index|
          var detune = if(index > 0, Rand(-1.0 * detuneRange, detuneRange), 0);
          var filterFreq = (
            noteFreq *
            (1.0 + detune)
          );
          var combFreq = (
            noteFreq *
            (1.0 + detune)
          );
          DelayL.ar(
            HPF.ar(
              SOS.ar(
                DriveNoise.ar(
                  CombC.ar(
                    playbuf * 2,
                    1 / gBaseFilterResonanceFreq,
                    1 / combFreq,
                    16,
                  ),
                  noise,
                  1.5,
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
        }).tanh;
        var env = EnvGen.kr(
          Env.adsr(6.0, 0.2, 0.9, 16.0),
          gate: Trig.kr(1.0, 3.0),
          doneAction: Done.freeSelf
        );
        Out.ar(out, Pan2.ar(voice * env));
      }
    ).add;
    
    pPaper = Pbind(
      \instrument, \fishPaper,
      \outBus, bPerc,
      \outTrigBus, bSidechainTrig,
      \amp, Ptime().collect({ |t| ((t * 1.5).sin * 0.5) + 0.6 }) * Pfunc({ gPaperGain }),
      \dur, 0.25,
    ).play(tPercClock);
  
    SynthDef.new(\fishPaper,
      { arg outBus = 1, outTrigBus = 0, amp = 1;
        var noise = HPF.ar(
          PinkNoise.ar(),
          TIRand.ar(800, 2000, Impulse.kr(0)),
        );
        var env = EnvGen.ar(
          Env.perc(0.15, 0.01),
          doneAction: Done.freeSelf
        );
        Out.ar(outBus, noise * env * amp);
      }
    ).add;

    context.server.sync;
    
    sEffects = Synth.new(\fishEffects, [
      \inEffectsBus, bEffects,
      \inTrigBus, bSidechainTrig,
      \outBus, context.out_b.index],
    context.xg);
  
    sPerc = Synth.new(\fishPerc, [
      \inBus, bPerc,
      \outBus, context.out_b.index],
    context.xg);

    // commands

    this.addCommand("noteOn", "i", {|msg|
      var index = msg[1];
      var mapping = [
        0, 5, 10, 15, 19, 8, 12, 17,
        0 + 12, 5 + 12, 10 + 12, 15 + 12, 19 + 12, 8 + 12, 12 + 12, 17 + 12
      ];
      var noteIndex = mapping[index.wrap(0, mapping.size)];
      var note = Synth.new(\fishFilterGroup, [
        \in, 2,
        \out, bEffects,
        \detuneRange, gDetuneRange,
        \noteIndex, noteIndex],
      context.xg);
      note.map(\filterRadius, bFilterRadius);
      note.map(\grainFreq, bGrainFreq);
      note.map(\noise, bNoise);
      nil
    });
    this.addCommand("amp", "f", {|msg|
      sEffects.set(\amp, msg[1]);
    });
    this.addCommand("filterRadius", "f", {|msg|
      bFilterRadius.set(msg[1]);
    });
    this.addCommand("noise", "f", {|msg|
      bNoise.set(msg[1]);
    });
    this.addCommand("grainFreqMul", "f", {|msg|
      bGrainFreq.set(gBaseGrainFreq * msg[1]);
    });
    this.addCommand("detune", "f", {|msg|
      gDetuneRange = msg[1];
    });
    this.addCommand("paper", "f", {|msg|
      gPaperGain = msg[1];
    });
  }

  free {
    pPaper.stop;  
    sEffects.free;
    sPerc.free;
    bEffects.free;
    bPerc.free;
    bFilterRadius.free;
    bNoise.free;
    bGrainFreq.free;
    bSidechainTrig.free;
  }

} 
