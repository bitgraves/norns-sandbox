// forked from PitShift to take advantage of multichannel bug.
Braid {
  * ar { arg in, shift, mul = 1, delayLength = 5024/44100;
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