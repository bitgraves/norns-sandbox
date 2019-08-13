# norns-sandbox

This repo contains software for [norns](https://monome.org/norns/) written by [bit graves](https://bitgraves.com/) for use in live performance and studio recording.

## Prerequisites

- You need a norns
- You need filesystem access to your norns, one way or another
- Almost everything here acts as a signal processor rather than a generator, so you need some external audio running into the norns for this to be interesting
- You probably want some kind of midi controller (see below)

## Installation

- Clone this repo anywhere under `dust/code` on your norns.
- In our case, the final path is `/home/we/dust/code/bitgraves`. If you clone to some other path, you might need to adjust the include path on [this line](https://github.com/bitgraves/norns-sandbox/blob/master/common/bgutil.lua#L36).
- Restart the audio engine on the norns.

## MIDI

All of the norns engines here support a large handful of `params`. At the time of writing, we use an Akai MPD218 midi controller to change these. If you want to use a different controller, you'll need to adjust some constants in the lua scripts. Here is an example of the mapping from Akai constants to params: https://github.com/bitgraves/norns-sandbox/blob/4d50ce8f565a4863a1ee2cae7a58c33b65522723/super/super.lua#L67-L76