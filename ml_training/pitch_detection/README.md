# Pitch Detection Model Training

## Overview

This module trains a CREPE-inspired convolutional neural network for pitch detection in Indian classical music.

## Model Architecture

**Input**: Raw audio waveform (1024 samples @ 16kHz = 64ms window)

**Output**: 
- Pitch frequency (50-500 Hz range)
- Confidence score (0-1)

**Architecture**:
```
Input Layer: [batch, 1024, 1]
├─ Conv1D(filters=1024, kernel=512, stride=4, activation='relu')
├─ BatchNormalization()
├─ Conv1D(filters=128, kernel=64, stride=1, activation='relu')
├─ BatchNormalization()
├─ Conv1D(filters=128, kernel=64, stride=1, activation='relu')
├─ BatchNormalization()
├─ MaxPooling1D(pool_size=2)
├─ Dropout(0.25)
├─ Flatten()
├─ Dense(256, activation='relu')
├─ Dropout(0.25)
└─ Dense(360, activation='softmax')  # 360 pitch bins

Post-processing: Convert bin to Hz
```

## Data Preparation

### Dataset Structure
```
data/pitch_dataset/
├── train/
│   ├── audio/
│   │   ├── sample_0001.wav
│   │   ├── sample_0002.wav
│   │   └── ...
│   └── labels.csv  # filename,pitch_hz,confidence
├── val/
│   ├── audio/
│   └── labels.csv
└── test/
    ├── audio/
    └── labels.csv
```

### Data Generation Script

Run the data preparation script:
```bash
python data_preparation.py \
    --input-dir /path/to/raw/audio \
    --output-dir ../data/pitch_dataset \
    --sample-rate 16000 \
    --window-size 1024 \
    --hop-size 512
```

### Data Sources

1. **Synthesized Tones** (40% of dataset)
   - Pure sine waves at known frequencies
   - Harmonic complex tones
   - Frequency range: 50-500 Hz

2. **Vocal Recordings** (40% of dataset)
   - Indian classical vocal exercises (Sa-Re-Ga-Ma...)
   - Labeled with reference pitch from tuner
   - Multiple singers for diversity

3. **Instrument Recordings** (20% of dataset)
   - Harmonium, tanpura, flute
   - Labeled with MIDI reference

## Training

### Basic Training
```bash
python train_pitch_model.py \
    --data-dir ../data/pitch_dataset \
    --epochs 100 \
    --batch-size 32 \
    --learning-rate 0.001
```

### Advanced Options
```bash
python train_pitch_model.py \
    --data-dir ../data/pitch_dataset \
    --epochs 100 \
    --batch-size 32 \
    --learning-rate 0.001 \
    --augmentation \
    --early-stopping \
    --patience 10 \
    --checkpoint-dir checkpoints/ \
    --tensorboard-dir logs/
```

### Data Augmentation

Enabled with `--augmentation` flag:
- Pitch shifting: ±2 semitones
- Time stretching: 0.9x - 1.1x
- Gaussian noise: SNR 20-40 dB
- Volume variation: ±6 dB

## Evaluation

### Metrics

1. **Raw Pitch Accuracy (RPA)**
   - Percentage of frames within ±50 cents of ground truth

2. **Gross Pitch Error (GPE)**
   - Percentage of frames with error > 100 cents

3. **Voicing Decision Error (VDE)**
   - False positive/negative rate for voiced/unvoiced detection

### Evaluation Script
```bash
python ../utils/evaluation.py \
    --model ../models/pitch_model.h5 \
    --test-data ../data/pitch_dataset/test \
    --output-report evaluation_report.json
```

## TFLite Conversion

### Standard Conversion
```bash
python ../utils/tflite_converter.py \
    --model ../models/pitch_model.h5 \
    --output ../models/pitch_model.tflite \
    --optimize
```

### Quantization (for smaller model size)
```bash
python ../utils/tflite_converter.py \
    --model ../models/pitch_model.h5 \
    --output ../models/pitch_model_quantized.tflite \
    --optimize \
    --quantize int8
```

## Model Performance

### Expected Metrics (on test set)
- **RPA (±50 cents)**: > 95%
- **GPE**: < 2%
- **VDE**: < 5%
- **Inference Time** (on device): < 50ms
- **Model Size**: ~4 MB (unquantized), ~1 MB (quantized)

### Comparison with pYIN (DSP fallback)
| Metric | Neural Network | pYIN |
|--------|---------------|------|
| RPA | 95%+ | 90% |
| GPE | <2% | 5% |
| Speed | 50ms | 30ms |
| Robustness | High | Medium |

## Deployment

1. **Convert to TFLite**
   ```bash
   python ../utils/tflite_converter.py --model ../models/pitch_model.h5 --output pitch_model.tflite
   ```

2. **Copy to Android Assets**
   ```bash
   cp ../models/pitch_model.tflite ../../app/src/main/assets/
   ```

3. **Verify in App**
   - Build and run app
   - Check logs for "TFLite model loaded successfully"
   - Test with real audio input

## Troubleshooting

### Model not loading in Android
- Check file size (should be < 50 MB)
- Verify TFLite version compatibility
- Check asset folder path

### Poor accuracy
- Increase training data
- Add more augmentation
- Tune hyperparameters
- Check data quality

### Slow inference
- Use quantized model
- Reduce input size
- Enable GPU delegate

## Future Improvements

- [ ] Multi-pitch detection (for harmonies)
- [ ] Octave error correction
- [ ] Real-time streaming inference
- [ ] Model pruning for smaller size
- [ ] Transfer learning from CREPE
