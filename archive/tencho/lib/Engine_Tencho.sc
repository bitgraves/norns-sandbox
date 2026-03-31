Engine_Tencho : CroneEngine {
  var bCarrier;
  var <sCarrier1, <sCarrier2, <sFilter, sTrig;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Tencho alloc".postln;

    bCarrier = Bus.audio(context.server, 2);
    context.server.sync;

    // adds effects to a single carrier
    // mono in and out
    SynthDef.new(\tnCarrier,
      { arg inBus = 2, outBus = 0, outChannel = 0, delayFeedbackTime = 0.2, delay = 0, chop = 0, chopTime = 0.4, amp = 1, inAmp = 1;
        var in = In.ar(inBus, 1);
        var buf = Buffer.alloc(context.server, context.server.sampleRate * 1, 1);
        var recorder = RecordBuf.ar(
          in,
          buf,
          recLevel: 0.5,
          preLevel: 0.5,
          loop: 1
        );
        var grainTrig = Impulse.kr(1 / chopTime);
        var grain = Warp1.ar(
          bufnum: buf,
          pointer: Demand.kr(grainTrig, 0, Dwhite(0, 1)),
          freqScale: TChoose.kr(grainTrig, [-1, 1]),
          windowSize: chopTime,
          mul: 3,
        );
        var mix = Mix.ar([
          in * (1 - chop),
          grain * chop,
        ]);
        var delayL = CombC.ar(
          mix,
          maxdelaytime: 0.5,
          delaytime: delayFeedbackTime,
          decaytime: 2.0
        );
        mix = Mix.ar([
          mix,
          delayL * delay // doesn't include dry signal
        ]);
  
        Out.ar(outBus + outChannel, mix * amp);
      }
    ).add;
  
    // adds bus effects to bCarrier
    // stereo in and out
    SynthDef.new(\tnFilter,
      { arg inBus = 2, outBus = 0, noiseAmp = 0, amp = 0;
        var in = In.ar(inBus, 2);
        var mix = Mix.ar(in); // before this, it's input 1 hard left, input 2 hard right
        mix = Mix.ar([
          mix,
          GrayNoise.ar(noiseAmp)
        ]);
        Out.ar(outBus, Pan2.ar(mix) * amp);
      }
    ).add;
        
    context.server.sync;
    
    sFilter = Synth.new(\tnFilter, [
      \inBus, bCarrier,
      \outBus, context.out_b.index],
    context.xg);
    sCarrier1 = Synth.new(\tnCarrier, [
      \inBus, context.in_b[0].index,
      \outBus, bCarrier],
    context.xg);
    sCarrier2 = Synth.new(\tnCarrier, [
      \inBus, context.in_b[1].index,
      \outChannel, 1, // send to R channel of carrier
      \outBus, bCarrier],
    context.xg);

    // commands
    this.addCommand("amp", "f", {|msg|
      sFilter.set(\amp, msg[1]);
    });
    this.addCommand("noise", "f", {|msg|
      sFilter.set(\noiseAmp, msg[1]);
    });
    this.addCommand("delayR", "f", {|msg|
      sCarrier2.set(\delay, msg[1]);
    });
    this.addCommand("chopTimeR", "f", {|msg|
      sCarrier2.set(\chopTime, msg[1]);
    });
    this.addCommand("chopR", "f", {|msg|
      sCarrier2.set(\chop, msg[1]);
    });
    this.addCommand("delayTime", "f", {|msg|
      sCarrier1.set(\delayFeedbackTime, msg[1]);
      sCarrier2.set(\delayFeedbackTime, msg[1]);
    });
  }

  free {
    sCarrier1.free;
    sCarrier2.free;
    sFilter.free;
  }

} 
