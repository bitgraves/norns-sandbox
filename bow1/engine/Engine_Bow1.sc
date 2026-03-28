Engine_Bow1 : CroneEngine {
  var <sSynth;

  *new { arg context, doneCallback;
    ^super.new(context, doneCallback);
  }

  alloc {
    "Bow1 alloc".postln;

    SynthDef.new(\bgbowed2, {
      var in, snd, freq, amp, vibrato, velbow, force = 1, pos = 0.07, c1 = 0.25, c3 = 31;
      var mix = \mix.kr(0);
      var combAmp = \comb.kr(0);
      var combLerp = \combLerp.kr(0);
      var combNote = \combNote.kr(0);

      in = In.ar(context.in_b[0].index, 1);
      amp = \amp.kr(1);
      freq = \freq.kr(440 * (-36 - 7).midiratio);
      freq = TWChoose.kr(Dust.kr(3), [freq, freq * 2, freq * 4], [4, 4, 1].normalizeSum);

      vibrato = Gendy1.kr(1, 1, 1, 1, 0.1, 4, mul: 0.003, add: 1);
      velbow = \velbow.kr(0);
      velbow = EnvGen.kr(Env.adsr(), \gate.kr(0), -1.0 * velbow, velbow);
      snd = DWGBowed.ar(freq * vibrato, velbow, force, 1, pos, 0.5, c1, c3, fB: 3);
      snd = snd * -22.dbamp;

      snd = Mix.ar([snd * mix, in * (1.0 - mix)]);

      snd = DWGSoundBoard.ar(snd, mix: \sndboardmix.kr(0.5));
      snd = snd + (combAmp * Mix.ar(CombC.ar(snd, 0.2, combNote.midiratio * combLerp.linlin(0, 1, 1, 0.5) * [118, 430, 490, freq * (19.midiratio), freq * (10.midiratio)].reciprocal, 0.5, 0.2)));

      snd = snd * 14.dbamp;

      snd = Pan2.ar(snd, 0);
      Out.ar(\out.kr(0), snd * amp);
    }).add;

    context.server.sync;

    sSynth = Synth(\bgbowed2, [
      \freq, 27.midicps,
      \out, context.out_b.index],
    context.xg);

    this.addCommand("mix", "f", {|msg|
      sSynth.set(\mix, msg[1]);
    });
    this.addCommand("velbow", "f", {|msg|
      sSynth.set(\velbow, msg[1]);
    });
    this.addCommand("comb", "f", {|msg|
      sSynth.set(\comb, msg[1]);
    });
    this.addCommand("combLerp", "f", {|msg|
      sSynth.set(\combLerp, msg[1]);
    });
    this.addCommand("combNote", "i", {|msg|
      sSynth.set(\combNote, msg[1]);
    });
    this.addCommand("gate", "i", {|msg|
      sSynth.set(\gate, msg[1]);
    });
  }

  free {
    sSynth.free;
  }

}
