# norns-sandbox

This repo contains software for [norns](https://monome.org/norns/) written by [bit graves](https://bitgraves.com/) for use in live performance and studio recording.

## Prerequisites

- You need a norns. (our norns was last updated to: [201202](https://github.com/monome/norns/releases/tag/v2.4.7))
- You need filesystem access to your norns, one way or another
- Almost everything here acts as a signal processor rather than a generator, so you need some external audio running into the norns for this to be interesting
- You probably want some kind of midi controller (see below)

## Installation

- Clone this repo anywhere under `dust/code` on your norns. In our case, the final path is `/home/we/dust/code/bitgraves`.
- The `Lighthouse` engine requires you to install the [Vowel](https://github.com/supercollider-quarks/Vowel/blob/master/Vowel.sc) quark anywhere under `dust/code`.
- Restart the audio engine on the norns.

## MIDI Input

All of the norns engines here support a large handful of `params`. At the time of writing, we use an Akai MPD218 midi controller to change these. If you want to use a different controller, you'll need to adjust some constants in the lua scripts. Here is an example of the mapping from Akai constants to params: https://github.com/bitgraves/norns-sandbox/blob/31b0b64db7fa8ffdee5124776480b40f96d26b3b/baaka/baaka.lua#L34-L41

## MIDI Output

Some engines can optionally send MIDI control to an external synthesizer via an interface (we use iConnectivity Mio). However, at the moment, this requires compiling a patch to `matron`. Please get in touch if you want to learn how to set this up. These engines should still run and play sound intrinsically without this; they just won't include the external synth part.

For MIDI Output, the cc mappings live in `common/bgmidi.lua`.