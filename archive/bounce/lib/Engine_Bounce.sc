Engine_Bounce : CroneEngine {
  var <bounceLastAmp, <other;
  var <sHiTex;
  var <sBounce;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Bounce alloc".postln;
    other = 0;
    bounceLastAmp = 0;

    SynthDef.new(\bounceHiTex,
      { arg inL, inR, out, amp = 1, other = 0;
        var in = [In.ar(inL), In.ar(inR)];
        var shift = if (other, 55.midiratio, 60.midiratio);
        var voice = PitShift.ar(in, shift);
        var trig = Impulse.kr(12);
        var env = EnvGen.kr(
          Env.perc(0.05, 0.1),
          trig,
        );
        var filterFreq = Demand.kr(trig, 0, Diwhite.new(2, 12) * 1000);
        var hpf = RHPF.ar(voice, filterFreq, rq: 0.6);
        Out.ar(out,
          Pan2.ar(
            hpf * env,
            SinOsc.kr(0.1)
          ) * amp
        );
      }
    ).add;
    
    SynthDef.new(\bounceLead,
      { arg inL, inR, out, amp = 0, bounce = 0, drift = 0, lpfFreq = 100, inTrig = 0;
        var in = [In.ar(inL), In.ar(inR)]; // there is a deliberate bug here passing in 2 channels to PitShift
        var voice = PitShift.ar(in, 2.0);
        var sub = PitShift.ar(in, 0.5);
        var sweepVel = 200 * drift;
        var sweepTrig = inTrig + Dust.kr(2);
        var sweepRate = Demand.kr(
          sweepTrig,
          0,
          Diwhite.new(1, 5) * sweepVel
        );
        var env = EnvGen.kr(
          Env.perc(0.05, 0.05),
          Impulse.kr(10),
          levelBias: 1 - bounce,
        ).clip(0, 1);
        var fs = FreqShift.ar(
          Mix.ar([in, voice, sub]),
          Sweep.ar(
            trig: sweepTrig,
            rate: sweepRate,
          ),
        );
        var filter = RLPF.ar(fs, lpfFreq);
        Out.ar(out, filter * env * amp);
      }
    ).add;
        
    context.server.sync;

    sHiTex = Synth.new(\bounceHiTex, [
      \inL, context.in_b[0].index,      
      \inR, context.in_b[1].index,
      \out, context.out_b.index,
      \amp, 0],
    context.xg);

    sBounce = Synth.new(\bounceLead, [
      \inL, context.in_b[0].index,      
      \inR, context.in_b[1].index,
      \out, context.out_b.index,
      \amp, 1],
    context.xg);
    bounceLastAmp = 1;

    // commands

    this.addCommand("amp", "f", {|msg|
      bounceLastAmp = msg[1];
      sBounce.set(\amp, bounceLastAmp * (1 - other));
    });
    this.addCommand("hiTexAmp", "f", {|msg|
      sHiTex.set(\amp, msg[1]);
    });
    this.addCommand("bounce", "f", {|msg|
      sBounce.set(\bounce, msg[1]);
    });
    this.addCommand("drift", "f", {|msg|
      sBounce.set(\drift, msg[1]);
    });
    this.addCommand("driftTrig", "i", {|msg|
      sBounce.set(\inTrig, msg[1]);
    });
    this.addCommand("lpf", "f", {|msg|
      var newFreq = msg[1].linexp(0, 1, 100, 20000);
      sBounce.set(\lpfFreq, newFreq);
    });
    this.addCommand("other", "i", {|msg|
      other = msg[1];
      sHiTex.set(\other, other);
      sBounce.set(\amp, bounceLastAmp * (1 - other));
    });
  }

  free {
    sHiTex.free;
    sBounce.free;
  }

} 