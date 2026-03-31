Engine_Hangs : CroneEngine {
  var bFx, bDuckTrig, bPerc;
  var sHng, sFx, sPerc;
  var pKick, pHat, pClick;

  var gKickGain = 0, gHatGain = 0, gClickGain = 0;
  var tPercClock;
  
  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Hangs alloc".postln;

    bFx = Bus.audio(context.server, 2);
    bDuckTrig = Bus.audio(context.server, 1);
    bPerc = Bus.audio(context.server, 1);

    bDuckTrig.set(0);
    tPercClock = TempoClock.new(1 / 0.9);
    
    context.server.sync;

    // HNG HNG
    SynthDef(\hngHng,
      { arg inBus = 2, outBus = 0, amp = 1, trig = 0, degree = 0, wob = 0.1, pulseWidth = 0.99, sixthLevel = 0;
        var in = In.ar(inBus, 1);
  
        var baseFreq = 440 * (-12 - 6).midiratio; // e flat
        var octatonic = [0, 1, 3, 4, 6, 7, 9, 10];
        var interval = Select.kr(degree.mod(octatonic.size), octatonic) + (12 * (degree / 8).floor);
        var freqs = [
            baseFreq * (interval).midiratio,
            baseFreq * (interval + 7).midiratio,
            baseFreq * (interval + 7 + 9).midiratio,
          ];
        var sixth = Pulse.ar(
          freq: freqs,
          width: pulseWidth,
          mul: [0.9, 1, 0.6] * 0.3,
        );
        var carrier = FreqShift.ar(
          DriveNoise.ar(in, 0.5, 1.5),
          freq: sixth * (baseFreq * 8),
          mul: [0.5, 0.25, 0.125],
        );
  
        var snd = Mix.ar([
          LPF.ar(sixth, sixthLevel.linexp(0, 1, 100, 18000)),
          carrier
        ]);
        snd = snd * EnvGen.kr(Env.triangle(wob), Impulse.kr(1 / wob));
        snd = Mix.ar(snd);
  
        Out.ar(outBus, Pan2.ar(snd, 0) * amp);
      }
    ).add;
  
    SynthDef.new(\hngFx,
      { arg inBus = 2, outBus = 0, trigBus = 2, amp = 0;
        var in = In.ar(inBus, 2);
        var trig = In.ar(trigBus);
        // var mic = In.ar(micBus, 1);
        // var snd = DriveNoise.ar(in, 0.5, 1.5);
        var snd = in * EnvGen.ar(
          Env.perc(0.01, 2),
          trig,
          levelScale: -1,
          levelBias: 1
        );
        snd = HPF.ar(
          snd,
          freq: EnvGen.ar(
            Env.perc(0.05, 3),
            trig,
            levelScale: 1200,
            levelBias: 40,
          ),
        );
        snd = [
          snd[0],
          DelayN.ar(snd[1], 0.025, 0.025)
        ];
  
        Out.ar(outBus, snd * amp);
      }
    ).add;
  
    // kek dram
    pKick = Pbind(
      \instrument, \hngKk,
      \outBus, bPerc,
      \trigBus, bDuckTrig,
      \dur, Pn(
        Pwrand([
          Pseq([1, 2, 1, 0.5]),
          Pseq([2, 0.5, 0.5]),
          Pseq([1, 0.66, 0.33]),
          Pseq([1, 1]),
        ], [7, 3, 2, 1].normalizeSum),
        inf
      ),
      \n, Pn(
        Pwrand([0, 12], [0.9, 0.1])
      ),
      \gain, Pfunc({ gKickGain }),
    ).play(tPercClock);
    SynthDef.new(\hngKk,
      { |inBus = 2, outBus = 0, trigBus = 0, gain = 1, n = 0, release = 2|
        var beat = IRand(1, 3) * 0.15;
        var finalFreq = 27.midicps; // low E flat
        var osc = Mix.ar(
          Pulse.ar(
            freq: [
              XLine.ar(800, finalFreq * n.midiratio, 0.01),
              XLine.ar(801, (finalFreq - beat) * n.midiratio, 0.01)
            ],
          ),
          SinOsc.ar(
            freq: XLine.ar(800, finalFreq * n.midiratio, 0.01),
            mul: 0.5,
          ).tanh,
          PinkNoise.ar() * 0.1,
        );
        var env = EnvGen.ar(
          Env.perc(0.01, release),
          doneAction: Done.freeSelf
        );
        Out.ar(trigBus, Impulse.ar(0) * gain);
        Out.ar(outBus, osc * env * gain);
      }
    ).add;
  
    // noises
    pHat = Pbind(
      \instrument, \hngScatter,
      \outBus, bPerc,
      \dur, Pn(
        Prand([
          Pseq([1, 0.5, 1, 0.5]),
          Pseq([1.5, 0.25, 0.5, 0.25]),
          Pseq([0.25, 0.25, 0.5]),
          Pseq([0.33, 0.33, 0.34]),
        ])
      ) * 0.5,
      \freq, Pn(
        Prand([2000, 1500]),
      ),
      \latchFreq, 6000,
      \release, Pn(
        Prand([
          Pseq([0.01, 0.01, 0.025]),
          Pseq([0.01, 0.01, 0.01, 0.02]),
        ])
      ) * 4,
      \gain, Pfunc({ gHatGain }),
    ).play(tPercClock);
    pClick = Pbind(
      \instrument, \hngScatter,
      \outBus, bPerc,
      \dur, 0.5 * 0.25,
      \freq, Pn(
        Prand([10000, 12000]),
      ),
      \latchFreq, 10000,
      \release, Pn(
        Prand([
          Pseq([0.01, 0.01, 0.025]),
          Pseq([0.01, 0.01, 0.01, 0.02]),
        ])
      ) * 2,
      \gain, Pfunc({ gClickGain }),
    ).play(tPercClock);

    SynthDef.new(\hngScatter,
      { |outBus = 0, gain = 1, n = 0, release = 0.01, freq = 10000, latchFreq = 10000|
        var snd = Latch.ar(
          WhiteNoise.ar(),
          Impulse.ar(latchFreq),
        );
        var flt = BPF.ar(snd, freq, 0.2);
        var env = EnvGen.ar(
          Env.perc(0.001, release),
          doneAction: Done.freeSelf
        );
        Out.ar(outBus, flt * env * gain * 3);
      }
    ).add;
  
    SynthDef.new(\hngPerc,
      { | inBus = 2, outBus = 0, gain = 0.85 |
        var snd = In.ar(inBus, 1);
        Out.ar(outBus, Pan2.ar(snd, 0) * gain);
      }
    ).add;

    context.server.sync;
    
    sFx = Synth.new(\hngFx, [
      \inBus, bFx,
      \trigBus, bDuckTrig,
      \outBus, context.out_b.index],
    context.xg);
    sHng = Synth.new(\hngHng, [
      \inBus, context.in_b[0].index,
      \outBus, bFx],
    context.xg);
    sPerc = Synth.new(\hngPerc, [
      \inBus, bPerc,
      \outBus, context.out_b.index],
    context.xg);
    
    // commands
    this.addCommand("amp", "f", {|msg|
      sFx.set(\amp, msg[1]);
    });
    this.addCommand("sixthLevel", "f", {|msg|
      sHng.set(\sixthLevel, msg[1]);
    });
    this.addCommand("wob", "f", {|msg|
      sHng.set(\wob, msg[1]);
    });
    this.addCommand("pulseWidth", "f", {|msg|
      sHng.set(\pulseWidth, msg[1]);
    });
    this.addCommand("kick", "f", {|msg|
      gKickGain = msg[1];
    });
    this.addCommand("click", "f", {|msg|
      gClickGain = msg[1];
    });
    this.addCommand("hat", "f", {|msg|
      gHatGain = msg[1];
    });

    this.addCommand("noteOn", "i", {|msg|
      sFx.set(\gate, 1);
      sHng.set(\degree, msg[1]);
      sHng.set(\trig, 1);
    });
    this.addCommand("noteOff", "i", {|msg|
      sFx.set(\gate, 0);
      sHng.set(\trig, 0);
    });
  }

  free {
    sHng.free;
    sFx.free;
    sPerc.free;
    pKick.stop;
    pHat.stop;
    pClick.stop;
    bFx.free;
    bDuckTrig.free;
    bPerc.free;
  }

} 
