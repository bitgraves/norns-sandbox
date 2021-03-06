PitShift {
  // not used:
  // var wrapLength = (maxDelayLengthSamp - 24) / s.sampleRate;
  // var delayLengthBuffer = 12 / s.sampleRate;
  // var halfLength = wrapLength / 2;
  classvar delayLengthSamp = 5024;
    *ar { arg in, shift, mul = 1;
    var delayLength = delayLengthSamp / Server.default.sampleRate;
    var halfLength = delayLength / 2;
    var sweep = Sweep.ar(
      Impulse.kr(0),
      1.0 - shift
    );
    var env = EnvGen.ar(
      Env.triangle(delayLength),
      Impulse.ar(delayLength.reciprocal)
    );
    var lines = DelayL.ar(
      in,
      maxdelaytime: delayLength,
      delaytime: Wrap.ar(
        [sweep, sweep + halfLength],
        0, delayLength
      ),
      mul: [env, 1.0 - env] * mul
    );
        ^lines.sum;
    }
}
