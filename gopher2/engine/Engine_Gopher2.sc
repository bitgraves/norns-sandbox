Engine_Gopher2 : CroneEngine {
  var <sSparrow, <sFx;
  var bFx;
  var bufSample;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Gopher2 alloc".postln;

    bFx = Bus.audio(context.server, 2);
    bufSample = Buffer.read(context.server, "/home/we/dust/code/bitgraves/samples/808SIDE.wav");
    context.server.sync;

    SynthDef.new(\bggopher2, {
      var kick;
      var in = In.ar(context.in_b[0].index);
      var freq = 110 * 6.midiratio; // e flat
      var wfreq = XLine.kr(ExpRand(18, 20), ExpRand(0.08, 0.12), 11);
      var atk = \attack.kr(0.001);

      // fm gopher
      var snd = SinOsc.ar(freq * \harm1.kr(2), in * 2, SinOsc.kr(wfreq, 0, 2));
      snd = SinOsc.ar(freq * \harm2.kr(5), snd, -10.dbamp);
      snd = snd * -5.dbamp;

      // kick
      kick = SinOsc.ar(XLine.ar(freq * 2, freq * 0.25, 0.07));
      kick = kick * SinOsc.kr(wfreq, pi, XLine.kr(0.001, 0.5, 1), XLine.kr(1, 0.5, 1));
      kick = kick + (Hasher.ar(Sweep.ar()) * Env.perc(atk, 0.01).ar() * \click.kr(1));
      kick = RLPF.ar(kick, 300, 0.2);
      kick = kick * -5.dbamp;

      snd = snd + kick;
      snd = snd * Env.linen(atk, 12, 12, 1).ar(Done.freeSelf);

      snd = snd ! 2;
      Out.ar(\out.kr(0), snd);
    }).add;

    SynthDef.new(\bgsparrowbaby, {
      var freq = 110 * 1.midiratio * 2;
      var chirp = \chirp.kr(0);
      var tilt = \tilt.kr(0);

      var add = LFNoise2.ar(2, 1, 1);
      var sawfreq = Demand.kr(Impulse.kr(1.88), 0, Dseq([8, 3, 5, 8, 3, 5, 8, 3, 5, 8, 8, 6], inf));
      var saw = LFSaw.ar(sawfreq, 1000, SinOsc.kr(0.05, 0, 80, 2000));

      var trigFreq = 4;
      var bufnum = \bufnum.kr(0);
      var segment = 1;
      var sweepFreq = (BufDur.ir(bufnum) * segment).reciprocal * trigFreq;
      var len = BufFrames.ir(bufnum) * segment * trigFreq.reciprocal;

      var snd = BufRd.ar(2, bufnum, LFSaw.ar(saw).range(0, len));
      snd = snd.sum;
      snd = snd * 17.dbamp;

      // chirpy bird
      snd = (snd * (1.0 - tilt)) + ((Pulse.ar([freq, freq * 2, freq * 4], LFNoise2.kr(0.5, 0.4, 0.5), -5.dbamp * [1 - tilt, 1, tilt]).sum) * tilt);
      snd = RLPF.ar(snd, LFNoise2.kr(3, 400, 1000));
      snd = Latch.ar(snd, Impulse.ar(Saw.ar(sawfreq, 1000, SinOsc.kr(0.05, 0, 80, 2000))));
      snd = HPF.ar(snd, chirp.linexp(0, 1, 4000, 200));
      snd = snd * -18.dbamp;
      snd = Pan2.ar(snd, LFNoise2.kr(2));

      Out.ar(\out.kr(0), snd * \amp.kr(0));
    }).add;

    SynthDef.new(\bggopher2fx, {
      var in = In.ar(\in.kr(0), 2);
      var monitor = In.ar(context.in_b[0].index) * \monitorAmp.kr(0);
      var duck = 1.0 - EnvGen.ar(Env.perc(0.01, 6, 1.5), \trig.kr(0)).clip2(1);
      var duckslow = 1.0 - EnvGen.ar(Env.perc(0.01, 4), \trig.kr(0));

      var delays = (110 * 6.midiratio).reciprocal / [0.5, 2, 7, 9];
      var combs = \combs.kr(0);

      var snd = in;

      snd = snd * 5.dbamp;
      snd = snd + (CombC.ar(snd, delays, delays * LFNoise2.kr(0.5).range(0.97, 1), combs.linlin(0, 1, 0.1, 1), mul: combs.linlin(0, 1, 0, 0.5)).sum * -5.dbamp);

      // monitor
      snd = snd + HPF.ar(monitor, duckslow.linexp(0, 1, 18000, 30), -5.dbamp);

      snd = snd * duck;
      snd = snd * -5.dbamp;
      Out.ar(\out.kr(0), snd);
    }).add;

    context.server.sync;

    sFx = Synth.new(\bggopher2fx, [
      \in, bFx,
      \out, context.out_b.index],
    context.xg);

    sSparrow = Synth.new(\bgsparrowbaby, [
      \out, bFx,
      \bufnum, bufSample],
    context.xg);

    // fx commands
    this.addCommand("combs", "f", {|msg|
      sFx.set(\combs, msg[1]);
    });
    this.addCommand("monitorAmp", "f", {|msg|
      sFx.set(\monitorAmp, msg[1]);
    });
    this.addCommand("trig", "i", {|msg|
      sFx.set(\trig, msg[1]);
    });

    // sparrow commands
    this.addCommand("sparrowAmp", "f", {|msg|
      sSparrow.set(\amp, msg[1]);
    });
    this.addCommand("chirp", "f", {|msg|
      sSparrow.set(\chirp, msg[1]);
    });
    this.addCommand("tilt", "f", {|msg|
      sSparrow.set(\tilt, msg[1]);
    });

    // gopher note trigger (creates a new synth each time, self-freeing)
    this.addCommand("gopherNote", "ffff", {|msg|
      var harm1 = msg[1];
      var harm2 = msg[2];
      var attack = msg[3];
      var click = msg[4];
      Synth.new(\bggopher2, [
        \harm1, harm1,
        \harm2, harm2,
        \attack, attack,
        \click, click,
        \out, context.out_b.index],
      context.xg);
    });
  }

  free {
    sSparrow.free;
    sFx.free;
    bFx.free;
    bufSample.free;
  }

}
