Engine_Baikal : CroneEngine {
  var <sBounce, <sFx;
  var bFx;
  var minCombFreq;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Baikal alloc".postln;

    bFx = Bus.audio(context.server, 2);
    minCombFreq = (55 * 2.midiratio);
    
    SynthDef(\bklBounce,
      { arg inBus = 2, outBus = 0, outTrigBus, amp = 1, release = 10, lpfFreq = 18000, inTrig = 0, initialFreq = 0.5, finalFreq = 0.5, combFreq = 43.65, phaseDrift = 0, noise = 0, glitch = 0;
        var in = In.ar(inBus, 1);
        var glitchCombFreq = combFreq * TWChoose.kr(
          Dust.kr(5),
          [1, 2, 3], [10, glitch, glitch],
          normalize: 1
        );
        var thru = [
          in,
          CombC.ar(
            in,
            maxdelaytime: minCombFreq.reciprocal,
            delaytime: glitchCombFreq.reciprocal,
            decaytime: 8
          ).tanh * noise,
        ];
        var delaySweep = Sweep.ar(
          inTrig,
          1.0 / release
        ).linexp(0, 1, initialFreq.reciprocal, finalFreq.reciprocal, \minmax);
        var voices = Mix.ar([
          Braid.ar(in: thru, shift: 2.0, delayLength: delaySweep),
          Braid.ar(in: thru, shift: 1.0, delayLength: delaySweep),
          // Braid.ar(in: thru, shift: 0.5, delayLength: delaySweep),
        ]);
        var fs = FreqShift.ar(
          voices,
          Sweep.ar(
            trig: inTrig,
            rate: phaseDrift,
          ),
        );
        var filter = RHPF.ar(
          RLPF.ar(fs, lpfFreq),
          40 + EnvGen.kr(Env.perc(attackTime: 0.001), inTrig, levelScale: 80)
        ).tanh;
        Out.ar(outBus, Pan2.ar(filter, 0) * amp);
      }
    ).add;
  
    SynthDef.new(\bklFx,
      { arg inBus = 2, outBus = 0, gate = 0, envDepth = 1, amp = 0;
        var in = In.ar(inBus, 2);
        var duck = EnvGen.kr(
          // Env.perc(0.001, 3),
          Env.asr(0.001, 1, 3),
          gate,
          levelScale: -1 * envDepth,
          levelBias: 1,
        );
        Out.ar(outBus, in * duck * amp);
      }
    ).add;
    
    context.server.sync;

    sFx = Synth.new(\bklFx, [
      \inBus, bFx,
      \outBus, context.out_b.index],
    context.xg);
    sBounce = Synth.new(\bklBounce, [
      \inBus, context.in_b[0].index,
      \outBus, bFx],
    context.xg);

    // commands

    this.addCommand("amp", "f", {|msg|
      sFx.set(\amp, msg[1]);
    });
    this.addCommand("phaseDrift", "f", {|msg|
      sBounce.set(\phaseDrift, msg[1]);
    });
    this.addCommand("envDepth", "f", {|msg|
      var param = msg[1];
      sFx.set(\envDepth, param);
    });
    this.addCommand("noise", "f", {|msg|
      sBounce.set(\noise, msg[1]);
    });
    this.addCommand("initialFreq", "f", {|msg|
      sBounce.set(\initialFreq, msg[1]);
    });
    this.addCommand("stableFreq", "f", {|msg|
      sBounce.set(\finalFreq, msg[1]);
    });
    this.addCommand("combFreq", "f", {|msg|
      sBounce.set(\combFreq, msg[1]);
    });
    this.addCommand("lpf", "f", {|msg|
      sBounce.set(\lpfFreq, msg[1].clip(60, 20000));
    });
    this.addCommand("glitch", "f", {|msg|
      sBounce.set(\glitch, msg[1]);
    });
    this.addCommand("gate", "i", {|msg|
      var param = msg[1];
      sBounce.set(\inTrig, param);
      sFx.set(\gate, param);
    });
  }

  free {
    sFx.free;
    sBounce.free;
    bFx.free;
  }

} 