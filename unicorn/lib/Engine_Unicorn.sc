Engine_Unicorn : CroneEngine {
  var bFx, bTrig, bPerc;
  var sVox, sFx, sPerc;
  var pKick, pHat, pClap;

  var gKickGain = 0, gNoiseGain = 0, gClapGain = 0;
  var baseMorphFreq;
  var seqDur;
  var kickSeqMul = 1;
  var tPercClock;
  
  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Unicorn alloc".postln;

    bFx = Bus.audio(context.server, 2);
    bTrig = Bus.audio(context.server, 1);
    bPerc = Bus.audio(context.server, 1);

    baseMorphFreq = 7.5;
    seqDur = 1 / baseMorphFreq;
    tPercClock = TempoClock.new(1 / 0.9);
    TempoClock.tempo = baseMorphFreq;
    
    context.server.sync;

    pKick = Pbind(
      \instrument, \mmrBump,
      \outBus, bPerc,
      \dur, Pn(
        Pconst(16, Pwhite(2, 3))
      ) * Pfunc({ kickSeqMul }),
      \release, Pn(
        Pwrand([2, 4], [0.8, 0.2]) * 0.1,
      ),
      \n, Pn(
        Pwrand([1, 4, -7], [0.8, 0.1, 0.1])
      ),
      \gain, Pfunc({ gKickGain }),
    ).play(TempoClock.default);
    SynthDef.new(\mmrBump,
      { |inBus = 2, outBus = 0, gain = 1, n = 0, release = 1|
        var beat = IRand(1, 7);
        var osc = Mix.ar(
          SinOsc.ar(
            freq: [
              XLine.ar(900, 44 * n.midiratio, 0.01),
              XLine.ar(901, (44 + beat) * n.midiratio, 0.01)
            ],
          );
        ).tanh;
        var mix = Mix.ar([
          osc, // bass
          LPF.ar(WhiteNoise.ar(mul: 0.7), XLine.ar(8000, 200, 0.01)), //click
        ]);
        var env = EnvGen.ar(
          Env.perc(0.01, release),
          doneAction: Done.freeSelf
        );
        Out.ar(outBus, mix * env * gain);
      }
    ).add;
  
    pHat = Pbind(
      \instrument, \mmrNoise,
      \outBus, bPerc,
      \outTrigBus, bTrig,
      \dur, 1,/* Pn(
        Pconst(8, Pgauss(1, 0.01)),
      ), */
      \release, Pn(
        Pwrand([0.2, 0.3], [0.9, 0.1]) * 0.05,
      ),
      \freq, Pn(
        Pwrand([8000, 7000], [0.8, 0.2])
      ),
      \gain, Pn(
        Pwrand([1, 0.2], [0.8, 0.2])
      ) * Pfunc({ gNoiseGain }),
    ).play(TempoClock.default);
  
    pClap = Pbind(
      \instrument, \mmrNoise,
      \outBus, bPerc,
      \outTrigBus, bTrig,
      \dur, Pn(
        Pwrand([8, 9, 7, 4], [0.7, 0.1, 0.1, 0.1]),
      ),
      \release, Pn(
        Pwrand([0.2, 0.25], [0.8, 0.2]),
      ),
      \freq, Pn(
        Pwrand([1400, 1200], [0.8, 0.2])
      ),
      \gain, Pn(
        Pwrand([1, 0.2], [0.8, 0.2])
      ) * Pfunc({ gClapGain }),
    ).play(TempoClock.default);
  
    SynthDef.new(\mmrNoise,
      { |inBus = 2, outBus = 0, outTrigBus = 0, gain = 1, n = 0, release = 0.01, freq = 10000|
        var snd = WhiteNoise.ar();
        var flt = Resonz.ar(snd, freq, 0.25);
        var env = EnvGen.ar(
          Env.perc(0.001, release),
          doneAction: Done.freeSelf
        );
        flt = (flt * 3).fold(-0.9, 0.9).tanh;
        Out.kr(outTrigBus, Impulse.kr(0));
        Out.ar(outBus, flt * env * gain);
      }
    ).add;
  
    SynthDef(\mmrVox,
      { arg inBus = 2, outBus = 0, amp = 1,
        modNoise = 1, modShift = 1, modFreq = 1,
        holdNoise = 0, holdShift = 0, holdFreq = 180,
        trigBus = 3,
        note1 = 0.01, note2 = 7, ampChord = 0; // TODO: refactor into other synth, move stuff to fx
        var in = In.ar(inBus, 1);
        var trig = In.kr(trigBus, 1);
  
        var morph = trig;
        var env = EnvGen.kr(
          Env.perc(0.001, 0.5), // adjust release for bouncier
          morph,
          levelScale: 0.8,
          levelBias: 0.8,
        );
        var pan = SinOsc.kr(7.5 * 0.4, mul: 0.7);
        var noiseMod = if(modNoise, TRand.kr(0, 0.6, morph), holdNoise);
        var freqMod = if(modFreq, TRand.kr(0, 1, morph).linexp(0, 1, 180, 18000), holdFreq);
        var shiftMod = if(modShift, TIRand.kr(0, 1, morph), holdShift);
  
        var harmonics = Mix.ar(
          Pulse.ar(
            [
              60.midicps,
              (60 + note1).midicps,
              (60 + note2).midicps,
            ],
            mul: shiftMod * ampChord,
          )
        );
  
        var sound = in;
        sound = PitShift.ar(sound, shiftMod + 1);
        sound = (sound * 2).fold(-1, 1);
        sound = Mix.ar([
          sound,
          harmonics,
        ]);
        sound = RLPF.ar(sound, freqMod);
        sound = DriveNoise.ar(sound, noiseMod, multi: 1.5);
        sound = sound.tanh;
  
        Out.ar(outBus, Pan2.ar(sound, pan) * amp * env);
      }
    ).add;
  
    SynthDef.new(\mmrFx,
      { arg inBus = 2, outBus = 0, gate = 0, freq = 1000, amp = 0;
        var in = In.ar(inBus, 2);
        in = Mix.ar([
          in,
          MedianTriggered.ar(
            in: in,
            trig: Dust.ar(freq)
          )
        ]);
        Out.ar(outBus, in * amp * 0.15);
      }
    ).add;
  
    SynthDef.new(\mmrPerc,
      { |inBus = 2, outBus = 0, gain = 1, noise = 0.1|
        var in = In.ar(inBus, 1);
        var sound = DriveNoise.ar(in, noise, 2);
        Out.ar(outBus, Pan2.ar(sound * gain, 0));
      }
    ).add;
    
    context.server.sync;
    
    sPerc = Synth.new(\mmrPerc, [
      \inBus, bPerc,
      \outBus, context.out_b.index],
    context.xg);
    sFx = Synth.new(\mmrFx, [
      \inBus, bFx,
      \outBus, context.out_b.index],
    context.xg);
    sVox = Synth.new(\mmrVox, [
      \inBus, context.in_b[0].index,
      \trigBus, bTrig,
      \outBus, bFx],
    context.xg);

    
    // commands
    this.addCommand("amp", "f", {|msg|
      sFx.set(\amp, msg[1]);
    });
    this.addCommand("percAmp", "f", {|msg|
      sPerc.set(\gain, msg[1]);
    });
  
    this.addCommand("noise", "f", {|msg|
      sVox.set(\holdNoise, msg[1]);
    });
    this.addCommand("shift", "f", {|msg|
      sVox.set(\holdShift, msg[1]);
    });
    this.addCommand("freq", "f", {|msg|
      sVox.set(\holdFreq, msg[1]);
    });
  
    this.addCommand("modNoise", "i", {|msg|
      sVox.set(\modNoise, msg[1]);
    });
    this.addCommand("modShift", "i", {|msg|
      sVox.set(\modShift, msg[1]);
    });
      this.addCommand("modFreq", "i", {|msg|
      sVox.set(\modFreq, msg[1]);
    });
  
    this.addCommand("tempo", "f", {|msg|
      TempoClock.default.play({
        sVox.set(\morphFreq, baseMorphFreq * msg[1]);
        TempoClock.tempo = baseMorphFreq * msg[1];
        seqDur = 1 / (baseMorphFreq * msg[1]);
      });
    });
  
    this.addCommand("chord", "f", {|msg|
      sVox.set(\ampChord, msg[1]);
    });
    this.addCommand("note1", "f", {|msg|
      sVox.set(\note1, msg[1]);
    });
    this.addCommand("note2", "f", {|msg|
      sVox.set(\note2, msg[1]);
    });
  
    this.addCommand("kick", "f", {|msg|
      gKickGain = msg[1];
    });
    this.addCommand("clap", "f", {|msg|
      gClapGain = msg[1];
    });
    this.addCommand("hat", "f", {|msg|
      gNoiseGain = msg[1];
    });

    this.addCommand("noteOn", "i", {|msg|
      sVox.set(\trig, 1);
      sFx.set(\gate, 1);
    });
    this.addCommand("noteOff", "i", {|msg|
      sVox.set(\trig, 0);
      sFx.set(\gate, 0);
    });
  }

  free {
    sVox.free;
    sFx.free;
    sPerc.free;
    pKick.stop;
    pHat.stop;
    pClap.stop;
    bFx.free;
    bTrig.free;
    bPerc.free;
  }

} 
