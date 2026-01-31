# Swar Classification Model Training

## Overview

This module trains a CNN-based classifier for identifying the 12 swars (musical notes) in Indian classical music from audio spectrograms.

## Model Architecture

**Input**: Mel spectrogram (128x128x1)

**Output**: 
- Swar class (12 classes)
- Confidence score (0-1)

**12 Swar Classes**:
1. Sa (सा)
2. Re(k) (रे॒) - Komal Re
3. Re (रे) - Shuddha Re
4. Ga(k) (ग॒) - Komal Ga
5. Ga (ग) - Shuddha Ga
6. Ma (म) - Shuddha Ma
7. Ma(t) (म॑) - Tivra Ma
8. Pa (प)
9. Dha(k) (ध॒) - Komal Dha
10. Dha (ध) - Shuddha Dha
11. Ni(k) (नि॒) - Komal Ni
12. Ni (नि) - Shuddha Ni

**Architecture**:
```
Input: [batch, 128, 128, 1]
├─ Conv2D(32, 3x3, activation='relu')
├─ BatchNormalization()
├─ MaxPooling2D(2x2)
├─ Dropout(0.25)
├─ Conv2D(64, 3x3, activation='relu')
├─ BatchNormalization()
├─ MaxPooling2D(2x2)
├─ Dropout(0.25)
├─ Conv2D(128, 3x3, activation='relu')
├─ BatchNormalization()
├─ GlobalAveragePooling2D()
├─ Dense(128, activation='relu')
├─ Dropout(0.5)
└─ Dense(12, activation='softmax')
```

## Data Preparation

### Dataset Structure
```
data/swar_dataset/
├── train/
│   ├── Sa/
│   │   ├── sa_001.wav
│   │   ├── sa_002.wav
│   │   └── ...
│   ├── Re_komal/
│   ├── Re/
│   └── ... (12 folders total)
├── val/
│   └── (same structure)
└── test/
    └── (same structure)
```

### Data Collection Guidelines

**Per Swar Requirements**:
- Minimum: 5,000 samples per swar
- Duration: 0.5-1 second each
- Format: WAV, 44.1kHz, mono
- Quality: Clean recordings, minimal background noise

**Recording Sources**:
1. **Professional Musicians** (40%)
   - Recorded in controlled environment
   - Multiple singers/instrumentalists
   - Verified by music teacher

2. **Student Recordings** (30%)
   - Practice sessions
   - Various skill levels
   - Real-world conditions

3. **Synthesized Samples** (30%)
   - Generated using harmonium/tanpura samples
   - Controlled pitch and timbre
   - Augmented variations

### Data Preparation Script
```bash
python data_preparation.py \
    --input-dir /path/to/raw/recordings \
    --output-dir ../data/swar_dataset \
    --sample-rate 44100 \
    --duration 1.0 \
    --n-mels 128
```

## Training

### Basic Training
```bash
python train_swar_classifier.py \
    --data-dir ../data/swar_dataset \
    --epochs 50 \
    --batch-size 64
```

### With Augmentation
```bash
python train_swar_classifier.py \
    --data-dir ../data/swar_dataset \
    --epochs 50 \
    --batch-size 64 \
    --augmentation \
    --mixup 0.2 \
    --spec-augment
```

### Training Parameters

**Optimizer**: Adam
- Learning rate: 0.001
- Beta1: 0.9
- Beta2: 0.999

**Loss**: Categorical crossentropy

**Metrics**: 
- Top-1 accuracy
- Top-3 accuracy
- Per-class F1 score

## Data Augmentation

### Audio Augmentation
- **Pitch shifting**: ±1 semitone
- **Time stretching**: 0.95x - 1.05x
- **Volume variation**: ±3 dB
- **Background noise**: SNR 25-40 dB

### Spectrogram Augmentation
- **SpecAugment**: Frequency and time masking
- **Mixup**: Alpha = 0.2
- **Cutout**: Random rectangular masks

## Evaluation

### Metrics

**Classification Metrics**:
- Top-1 Accuracy: > 90%
- Top-3 Accuracy: > 98%
- Per-class F1 score: > 0.85

**Confusion Analysis**:
- Identify commonly confused swar pairs
- Analyze komal vs shuddha confusion
- Evaluate octave errors

### Evaluation Script
```bash
python ../utils/evaluation.py \
    --model ../models/swar_classifier.h5 \
    --test-data ../data/swar_dataset/test \
    --output-report swar_evaluation.json \
    --confusion-matrix
```

## TFLite Conversion

```bash
python ../utils/tflite_converter.py \
    --model ../models/swar_classifier.h5 \
    --output ../models/swar_classifier.tflite \
    --optimize \
    --representative-dataset ../data/swar_dataset/calibration
```

## Model Performance

### Expected Results

| Metric | Target | Notes |
|--------|--------|-------|
| Top-1 Accuracy | > 90% | On balanced test set |
| Top-3 Accuracy | > 98% | Includes close variants |
| Inference Time | < 100ms | On mid-range Android device |
| Model Size | < 10 MB | Unquantized |
| Model Size | < 3 MB | INT8 quantized |

### Common Challenges

1. **Komal vs Shuddha Confusion**
   - Solution: Increase training data for komal notes
   - Use pitch-aware features

2. **Octave Errors**
   - Solution: Normalize by tonic frequency
   - Add octave-invariant features

3. **Timbre Variation**
   - Solution: Diverse training data
   - Data augmentation

## Deployment

1. Convert to TFLite
2. Test on device
3. Copy to assets:
   ```bash
   cp ../models/swar_classifier.tflite ../../app/src/main/assets/
   ```

## Integration with Pitch Hint

The swar classifier can use pitch information as a hint:

```kotlin
// In CNNSwarClassifier.kt
fun classifySwarWithPitchHint(
    audio: FloatArray,
    sampleRate: Int,
    pitchHint: Float,
    tonicFrequency: Float
): SwarPrediction
```

This improves accuracy by:
- Narrowing down candidate swars
- Resolving octave ambiguity
- Handling edge cases

## Future Improvements

- [ ] Multi-label classification (for gamakas)
- [ ] Temporal modeling (LSTM/Transformer)
- [ ] Few-shot learning for rare swars
- [ ] Active learning for data collection
