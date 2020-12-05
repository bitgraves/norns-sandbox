Engine_Super : CroneEngine {
  var bCarrier;
  var bTrig;
  var <sFormant;
  var <sSub;
  var <sRhythm;
  var <sTrig;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Super alloc".postln;

    bCarrier = Bus.audio(context.server, 1);
    bTrig = Bus.control(context.server, 1);
    context.server.sync;
      
    SynthDef.new(\spTrig,
      { arg outBus = 0, gate = 0, freq = 0;
        var allTrig = Impulse.kr(freq) + gate;
        Out.kr(outBus, allTrig);
      }
    ).add;
  
    SynthDef.new(\spFormant,
      { arg inL, inR, outBus = 0, amp = 0, mul = 1, fundfreq = 440, formfreq = 400, bwmul = 2;
        var in = In.ar(inL); // [In.ar(inL), In.ar(inR)];
        var fs = FreqShift.ar(
          in,
          freq: Formant.ar(
            fundfreq: fundfreq,
            formfreq: formfreq,
            bwfreq: fundfreq * bwmul,
            mul: mul,
          ),
        );
        Out.ar(outBus, fs * amp);
      }
    ).add;
  
    SynthDef.new(\spSub,
      { arg inL, inR, outBus = 0, amp = 1, gateBus = 0, duckAmount = 1;
        var in = In.ar(inL); // [In.ar(inL), In.ar(inR)];
        var gate = In.kr(gateBus, 1);
        var mix = Mix.ar([
          in,
          WhiteNoise.ar(0.3)
        ]);
  
        var duck = EnvGen.kr(
          Env.perc(0.001, 2.2),
          gate: gate,
          levelScale: -1.0 * duckAmount,
          levelBias: 1.0
        );
        var lpfEnv = EnvGen.kr(
          Env.perc(0.02, 0.1, curve: -16.0),
          gate: gate,
          levelScale: 10000,
          levelBias: 110
        );
        var lpf = RLPF.ar(in, lpfEnv, rq: 0.5);
  
        var env = EnvGen.kr(
          Env.perc(0.01, 4.0),
          gate: gate
        );
        Out.ar(outBus, Pan2.ar(lpf * env * duck * amp, 0));
      }
    ).add;
  
    SynthDef.new(\spRhythm,
      { arg inBus = 2, outBus = 0, amp = 1, gateBus = 0, freq = 0, duckRelease = 2.2;
        var in = In.ar(inBus, 1);
        var gate = In.kr(gateBus, 1);
        // modulate all trig values by a very slow LFO
        var trigLfoMod =  SinOsc.kr(1.0 / 30.0, 0, 0.1, 1);
        var trig = Impulse.kr(
          freq: SinOsc.kr(
            freq: Demand.kr(Dust.kr(1), 0, Drand.new([0.1, 0.2, 0.5, 0.8, 1], inf)) * trigLfoMod,
            mul: 6,
            add: 8
          )
        );
        var beat = EnvGen.kr(
          Env.perc(0.02, 0.3),
          gate: trig,
          levelScale: -1.0,
          levelBias: 1.0
        );
        var duck = EnvGen.kr(
          Env.perc(0.01, duckRelease),
          gate: gate,
          levelScale: -1.0,
          levelBias: 1.0
        );
        var pan = Demand.kr(trig, 0, Dwhite(-1, 1));
        Out.ar(outBus, Pan2.ar(in, pan) * amp * duck * beat);
      }
    ).add;
        
    context.server.sync;

    sTrig = Synth.new(\spTrig, [
      \outBus, bTrig],
    context.xg);
    
    sRhythm = Synth.new(\spRhythm, [
      \inBus, bCarrier,
      \outBus, context.out_b.index,
      \gateBus, bTrig],
    context.xg);
    
    sSub = Synth.new(\spSub, [
      \inL, context.in_b[0].index,      
      \inR, context.in_b[1].index,
      \outBus, context.out_b.index,
      \gateBus, bTrig],
    context.xg);
    
    sFormant = Synth.new(\spFormant, [
      \inL, context.in_b[0].index,      
      \inR, context.in_b[1].index,
      \outBus, bCarrier],
    context.xg);

    // commands
    
    this.addCommand("amp", "f", {|msg|
      sFormant.set(\amp, msg[1]);
    });
    this.addCommand("formFreq", "f", {|msg|
      sFormant.set(\formfreq, msg[1]);
    });
    this.addCommand("bwmul", "f", {|msg|
      sFormant.set(\bwmul, msg[1]);
    });
    this.addCommand("formmul", "f", {|msg|
      sFormant.set(\mul, msg[1]);
    });
    this.addCommand("subatk", "f", {|msg|
      var param = msg[1].linlin(0, 1, 1, 0);
      sSub.set(\duckAmount, param);
    });
    this.addCommand("duckRelease", "f", {|msg|
      sRhythm.set(\duckRelease, msg[1]);
    });
    this.addCommand("trigFreq", "f", {|msg|
      sTrig.set(\freq, msg[1]);
    });
    this.addCommand("gate", "i", {|msg|
      sTrig.set(\gate, msg[1]);
    });
    this.addCommand("formantIndex", "i", {|msg|
      sFormant.set(\fundfreq, 220.0 * msg[1].midiratio);
    });
  }

  free {
    sFormant.free;
    sSub.free;
    sRhythm.free;
    sTrig.free;
  }

} 