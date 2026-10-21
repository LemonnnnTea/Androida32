"""Regenerate Forest Pop's original short WAV effects; Python standard library only."""
import math
from pathlib import Path
import struct
import wave

OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"
SAMPLE_RATE = 22050
NOTES = {
    "pop": [(660, .07), (990, .09)],
    "combo": [(784, .07), (988, .07), (1175, .12)],
    "win": [(523, .1), (659, .1), (784, .1), (1047, .3)],
    "invalid": [(280, .08), (220, .09)],
}

OUTPUT.mkdir(parents=True, exist_ok=True)
for name, notes in NOTES.items():
    samples = []
    for frequency, duration in notes:
        count = int(SAMPLE_RATE * duration)
        for i in range(count):
            time = i / SAMPLE_RATE
            envelope = min(1, i / (SAMPLE_RATE * .007)) * max(0, 1 - i / count) ** 1.8
            value = (math.sin(2 * math.pi * frequency * time)
                     + .22 * math.sin(2 * math.pi * frequency * 2 * time)) * envelope * .36
            samples.append(int(value * 32767))
    with wave.open(str(OUTPUT / f"{name}.wav"), "w") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(struct.pack("<" + "h" * len(samples), *samples))
