Engine_Bounce2 : CroneEngine {
  var <sBounce, <sFx, <sTail, <sKick;
  var bFx, bKickTrig;
  var minCombFreq;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Bounce2 alloc".postln;

    bFx = Bus.audio(context.server, 2);
    bKickTrig = Bus.control(context.server, 1);
    minCombFreq = (55 * -4.midiratio);
    
    SynthDef.new(\foldBounce,
      { arg inBus = 2, outBus = 0, outTrigBus, amp = 1, lpfFreq = 60, inTrig = 0, initialFreq = 100, finalFreq = 0.5, combFreq = 43.65, phaseDrift = 0, noise = 0;
        var in = In.ar(inBus, 1);
        var thru = [
          in,
          CombL.ar(
            in,
            maxdelaytime: minCombFreq.reciprocal,
            delaytime: combFreq.reciprocal,
            decaytime: 8
          ).tanh * noise,
        ];
        var delaySweep = Sweep.ar(
          inTrig,
          1.0 / 5.0
        ).linexp(0, 1, initialFreq.reciprocal, finalFreq.reciprocal, \minmax);
        var voices = Mix.ar([
          Braid.ar(in: thru, shift: 2.0, delayLength: delaySweep),
          Braid.ar(in: thru, shift: 1.0, delayLength: delaySweep),
          Braid.ar(in: thru, shift: 0.5, delayLength: delaySweep),
        ]);
        var fs = FreqShift.ar(
          voices,
          Sweep.ar(
            trig: inTrig,
            rate: phaseDrift,
          ),
        );
        var filter = RLPF.ar(fs, lpfFreq).tanh;
        // TODO: if it goes too high or too low, fold to zero
        var delayFreq = delaySweep.reciprocal.clip(0, 15);
        var kkFreq = if(delayFreq >= 15, 0, delayFreq);
        var trig = Impulse.kr(kkFreq, 0.25) + inTrig;
        Out.kr(outTrigBus, trig);
        Out.ar(outBus, Pan2.ar(filter, 0) * amp);
      }
    ).add;
  
    SynthDef.new(\foldFx,
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
  
    SynthDef.new(\foldTail,
      { arg inBus = 2, outBus = 0, gate = 0, envDepth = 1, amp = 0.025;
        var in = In.ar(inBus, 2);
        // inverse of foldFx duck
        var env = EnvGen.kr(
          Env.asr(0.001, 1, 3),
          gate,
          levelScale: envDepth,
        );
        var sound = DriveNoise.ar(in, 1, 1.5);
        sound = SoftClipper4.ar(sound);
        Out.ar(outBus, sound * env * amp);
      }
    ).add;
    
    SynthDef.new(\foldBump,
      { |outBus = 0, trigBus = 2, gain = 1, n = 0, release = 0.5|
        var trig = In.kr(trigBus);
        var beat = Demand.kr(trig, 0, Diwhite.new(1, 5));
        var osc = Mix.ar(
          SinOsc.ar(
            freq: [
              Sweep.ar(trig, 1 / 0.01).linexp(0, 1, 800, 40 * n.midiratio),
              Sweep.ar(trig, 1 / 0.01).linexp(0, 1, 801, (40 + beat) * n.midiratio),
            ],
          );
        ).tanh;
        var mix = Mix.ar([
          osc,
          PinkNoise.ar()
        ]);
        var env = EnvGen.ar(
          Env.perc(0.01, release),
          gate: trig,
        );
        Out.ar(outBus, Pan2.ar(osc * env * gain));
      }
    ).add;

    context.server.sync;

    sFx = Synth.new(\foldFx, [
      \inBus, bFx,
      \outBus, context.out_b.index],
    context.xg);
    sTail = Synth.new(\foldTail, [
      \inBus, bFx,
      \outBus, context.out_b.index],
    context.xg);
    sBounce = Synth.new(\foldBounce, [
      \inBus, context.in_b[0].index,
      \outTrigBus, bKickTrig,
      \outBus, bFx],
    context.xg);
    sKick = Synth.new(\foldBump, [
      \trigBus, bKickTrig,
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
      sTail.set(\envDepth, param);
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
    this.addCommand("gate", "i", {|msg|
      var param = msg[1];
      sBounce.set(\inTrig, param);
      sFx.set(\gate, param);
      sTail.set(\gate, param);
    });
  }

  free {
    sFx.free;
    sTail.free;
    sKick.free;
    sBounce.free;
    bFx.free;
    bKickTrig.free;
  }

} 