Engine_Truths : CroneEngine {
  var bCarrier, bModulator, bTrig, bPad;
  var <sCarrier, <sModulator, <sFilter, <sPad, sTrig;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Truths alloc".postln;

    bCarrier = Bus.audio(context.server, 1);
    bModulator = Bus.audio(context.server, 1);
    bTrig = Bus.control(context.server, 1);
    bPad = Buffer.read(
      context.server,
      Platform.userHomeDir +/+ "dust/code/bitgraves/samples/20260906-bitgraves-truths-pad.wav"
    );
    context.server.sync;

    SynthDef.new(\pgTrig,
      { arg outBus = 0, gate = 0, freq = 0;
        var allTrig = Impulse.kr(freq) + gate;
        Out.kr(outBus, allTrig);
      }
    ).add;
  
    // a mixture of monitor and noise.
    // the signal that will be filtered.
    SynthDef.new(\pgCarrier,
      { arg inBus = 2, outBus = 0, amp = 1, gateBus = 0, noiseAmp = 0, inAmp = 1, sustain = 0;
        var in = In.ar(inBus, 1);
        var mix = Mix.ar([
          in * inAmp,
          GrayNoise.ar(noiseAmp)
        ]);
        var gate = In.kr(gateBus, 1);
        var env = EnvGen.kr(
          Env.perc(0.04, 5.0),
          gate: gate,
          levelScale: 1 - sustain,
          levelBias: sustain,
        );
        Out.ar(outBus, mix * env * amp);
      }
    ).add;
  
    SynthDef.new(\pgModulator,
      { arg inBus = 2, outBus = 0, inAmp = 0, wvAmp = 1, wvFreq = 220, wvBend = 0;
        var in = In.ar(inBus, 1);
        var freq = wvFreq * wvBend.midiratio;
        var mix = Mix.ar([
          in * inAmp,
          SinOsc.ar(freq, mul: wvAmp),
        ]);
        Out.ar(outBus, mix);
      }
    ).add;
  
    SynthDef.new(\pgFilter,
      { arg inBus = 2, outBus = 0, modBus = 2, gateBus = 0, amp = 0;
        var freq, hasFreq, sound, dyno, pan, ampEnv;
        var in = In.ar(inBus, 1);
        var mod = In.ar(modBus, 1);
        var gate = In.kr(gateBus, 1);
        var h1 = Demand.kr(gate, 0, Diwhite.new(4, 6));
        var h2 = Demand.kr(gate, 0, Diwhite.new(7, 9));
        var h3 = Demand.kr(gate, 0, Diwhite.new(11, 13));
        # freq, hasFreq = Pitch.kr(
          mod,
          initFreq: 55.0,
          maxFreq: 880.0,
          ampThreshold: 0.01
        );
        // 2, 3, 4, 7, 11
        sound = Mix.ar(Ringz.ar(in, freq * [2, 3, h1, h2, h3], 0.5, 0.003));
        // sound = FBSineC.ar(freq * 4, LFNoise2.kr(1, 3, 2), 0.5, 1.005, 0.7);

         // because of the resonant nature of Ringz and frequent changes in
         // freq, the signal can sometimes pass rapidly through much louder sounds,
         // so compress the signal a bit to smooth that out
         dyno = Compander.ar(
                 sound,
                 sound,
                 thresh: 0.5,
                 slopeAbove: 0.2,
                 clampTime: 0.005,
                 relaxTime: 0.3
         );
         dyno = [
                 dyno,
                 DelayC.ar(dyno, 0.02, 0.02)
         ];
         pan = Pan2.ar(
                 dyno,
                 0 // Demand.kr(Changed.kr(freq, 10), 0, Dwhite.new(-0.5, 0.5))
         );
  
         Out.ar(outBus, pan * amp);
      }
    ).add;

    SynthDef.new(\pgPad,
      { arg buf = 0, outBus = 0, rate = 1, amp = 0,
            root = 59,        // B3
            decay = 6.0,
            rezMix = 0,        // 0 = dry loop, 1 = resonators only
            gate = 1;
        var src, exc, freqs, amps, res, sig, env;

        env = EnvGen.kr(Env.asr(0.05, 1, 0.3), gate, doneAction: 2);

        src = PlayBuf.ar(2, buf, BufRateScale.kr(buf) * rate, loop: 1) * 0.5;

        exc = LeakDC.ar(src);
        exc = HPF.ar(exc, 60);
        exc = LPF.ar(exc, 18000);

        freqs = (root + [0, 5, 7]).midicps;
        amps = SinOsc.kr([0.4, 0.44, 0.41], phase: [0, 0.5pi, pi], mul: 0.2, add: [0.9, 0.6, 0.7]);

        // Ringz gain scales with decay time, so normalise it back out
        res = [
          (Ringz.ar(exc[0], freqs, decay) * amps / decay.max(0.01).sqrt).sum * 0.3,
          (Ringz.ar(exc[1], freqs, decay) * amps / decay.max(0.01).sqrt).sum * 0.3
        ];

        sig = XFade2.ar(exc, res, rezMix * 2 - 1);
        sig = Limiter.ar(LeakDC.ar(sig), 0.9);
        
        sig = HPF.ar(sig, 180);

        Out.ar(outBus, sig * amp * env);
      }
    ).add;
        
    context.server.sync;
    
    sTrig = Synth.new(\pgTrig, [
      \outBus, bTrig],
    context.xg);
    sFilter = Synth.new(\pgFilter, [
      \inBus, bCarrier,
      \modBus, bModulator,
      \gateBus, bTrig,
      \outBus, context.out_b.index],
    context.xg);
    sCarrier = Synth.new(\pgCarrier, [
      \inBus, context.in_b[0].index,
      \outBus, bCarrier,
      \gateBus, bTrig],
    context.xg);
    sModulator = Synth.new(\pgModulator, [
      \inBus, context.in_b[0].index,
      \outBus, bModulator],
    context.xg);
    sPad = Synth.new(\pgPad, [
      \buf, bPad,
      \outBus, context.out_b.index],
    context.xg);

    // commands
    this.addCommand("amp", "f", {|msg|
      sFilter.set(\amp, msg[1]);
    });
    this.addCommand("sustain", "f", {|msg|
      sCarrier.set(\sustain, msg[1]);
    });
    this.addCommand("modSource", "f", {|msg|
      sModulator.set(\inAmp, msg[1]);
      sModulator.set(\wvAmp, 1 - msg[1]);
    });
    this.addCommand("carrierInAmp", "f", {|msg|
      sCarrier.set(\inAmp, msg[1]);
    });
    this.addCommand("carrierNoiseAmp", "f", {|msg|
      sCarrier.set(\noiseAmp, msg[1]);
    });
    this.addCommand("bend", "f", {|msg|
      sModulator.set(\wvBend, msg[1]);
    });
    this.addCommand("padAmp", "f", {|msg|
      sPad.set(\amp, msg[1]);
    });
    this.addCommand("padRez", "f", {|msg|
      sPad.set(\rezMix, msg[1]);
    });
    this.addCommand("noteOn", "i", {|msg|
      var index = msg[1];
      var wvFreq = 110 * index.midiratio;
      sTrig.set(\gate, 1);
      sModulator.set(\wvFreq, wvFreq);
    });
    this.addCommand("noteOff", "i", {|msg|
      sTrig.set(\gate, 0);
    });
  }

  free {
    sTrig.free;
    sCarrier.free;
    sModulator.free;
    sFilter.free;
    sPad.free;
    bPad.free;
  }

} 
