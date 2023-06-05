Engine_Crystal : CroneEngine {
  var bEffects;
  var bGlitch;
  var bEnvLen;
  var notes;
  var <sEffects;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Crystal alloc".postln;

    notes = Array.newClear(16 * 3);
    bEffects = Bus.audio(context.server, 2);
    bGlitch = Bus.control(context.server, 1);
    bGlitch.set(0);
    bEnvLen = Bus.control(context.server, 1);
    bEnvLen.set(0);

    context.server.sync;
    
    SynthDef.new(\crystalEffects,
      { arg inBus, out, amp = 0, freqLpf = 40, bend = 0;
        var in = In.ar(inBus, 2);
        Out.ar(out,
          Pan2.ar(
            RLPF.ar(
              PitShift.ar(
                in,
                shift: bend.midiratio,
              ),
              freqLpf, 0.998
            )
          )
        );
      }
    ).add;
    
    SynthDef.new(\crystalMod,
      { arg inL, inR, out, glitch = 0, envLen = 0, index = 0, gate = 1;
        var in = Mix.ar([In.ar(inL), In.ar(inR)]);
        var offsetSeq = Diwhite(0, 1, inf);
        var glitchFreq = if(offsetSeq == 0, 5, 10 * glitch);
        var isOffset = Demand.kr(
          trig: Dust.kr(glitchFreq).dup,
          reset: 0,
          demandUGens: offsetSeq,
        );
        var voice = PitShift.ar(in: in, shift: (index + (12 * isOffset[0])).midiratio);
        var harmonic = PitShift.ar(in: in, shift: ((index + 19) + (12 * isOffset[1])).midiratio) * 0.5;
        var attack = envLen.linlin(0.0, 1.0, 3.0, 5.0);
        var release = envLen.linlin(0.0, 1.0, 4.0, 7.0);
        var note = Mix.ar([voice, harmonic]) *  EnvGen.kr(
          Env.adsr(attack, 0, 1.0, release),
          gate,
          levelScale: 0.7,
          doneAction: Done.freeSelf
        );
        Out.ar(out, note);
      }
    ).add;
        
    context.server.sync;

    sEffects = Synth.new(\crystalEffects, [
      \inBus, bEffects,
      \out, context.out_b.index,
      \amp, 1],
    context.xg);

    // commands

    this.addCommand("noteOn", "i", {|msg|
      var index = msg[1];
      if (notes[index] == nil,
        {
          var note = Synth.new(\crystalMod, [
            \inL, context.in_b[0].index,      
            \inR, context.in_b[1].index,
            \out, bEffects,
            \index, index],
          context.xg);
          note.map(\glitch, bGlitch);
          note.map(\envLen, bEnvLen);
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
      sEffects.set(\amp, msg[1]);
    });
    this.addCommand("bend", "f", {|msg|
      var newBend = msg[1].linlin(0, 1, 0, -2);
      sEffects.set(\bend, newBend);
    });
    this.addCommand("glitch", "f", {|msg|
      bGlitch.set(msg[1]);
    });
    this.addCommand("freqLpf", "f", {|msg|
      sEffects.set(\freqLpf, msg[1]);
    });
    this.addCommand("envLen", "f", {|msg|
      bEnvLen.set(msg[1]);
    });
  }

  free {
    sEffects.free;
  }

} 