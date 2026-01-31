# ML Model Training Pipeline for Riwaz

This directory contains the training pipeline, scripts, and documentation for all machine learning models used in the Riwaz application.

## 📁 Directory Structure

```
ml_training/
├── README.md                          # This file
├── pitch_detection/                   # Pitch detection model training
│   ├── train_pitch_model.py
│   ├── data_preparation.py
│   ├── model_architecture.py
│   └── README.md
├── swar_classification/               # Swar classification model training
│   ├── train_swar_classifier.py
│   ├── data_preparation.py
│   ├── model_architecture.py
│   └── README.md
├── raga_classification/               # GMM raga classifier
│   ├── train_gmm_classifier.py
│   ├── feature_extraction.py
│   └── README.md
├── data/                              # Training data (not in git)
│   ├── pitch_dataset/
│   ├── swar_dataset/
│   └── raga_dataset/
├── models/                            # Trained models (not in git)
│   ├── pitch_model.h5
│   ├── pitch_model.tflite
│   ├── swar_classifier.h5
│   └── swar_classifier.tflite
└── utils/                             # Shared utilities
    ├── audio_processing.py
    ├── tflite_converter.py
    └── evaluation.py
```

## 🎯 Models Overview

### 1. Pitch Detection Model (Neural Network)
- **Architecture**: CREPE-inspired CNN
- **Input**: Raw audio waveform (1024 samples)
- **Output**: Pitch frequency + confidence
- **Framework**: TensorFlow/Keras
- **Target Size**: < 5 MB

### 2. Swar Classification Model (CNN)
- **Architecture**: Mel-spectrogram CNN
- **Input**: Mel spectrogram (128x128)
- **Output**: 12 swar classes + confidence
- **Framework**: TensorFlow/Keras
- **Target Size**: < 10 MB

### 3. Raga Classification Model (GMM)
- **Architecture**: Gaussian Mixture Models
- **Input**: Pitch sequence features
- **Output**: Raga probabilities (10 ragas)
- **Framework**: Scikit-learn
- **Implementation**: Pure Kotlin (no TFLite needed)

## 🚀 Quick Start

### Prerequisites
```bash
# Python 3.8+
pip install tensorflow==2.14.0
pip install librosa
pip install scikit-learn
pip install numpy
pip install matplotlib
```

### Training Workflow

1. **Collect/Prepare Data**
   ```bash
   python pitch_detection/data_preparation.py --input raw_audio/ --output data/pitch_dataset/
   ```

2. **Train Model**
   ```bash
   python pitch_detection/train_pitch_model.py --epochs 100 --batch-size 32
   ```

3. **Convert to TFLite**
   ```bash
   python utils/tflite_converter.py --model models/pitch_model.h5 --output models/pitch_model.tflite
   ```

4. **Deploy to Android**
   ```bash
   cp models/pitch_model.tflite ../app/src/main/assets/
   ```

## 📊 Data Collection Guidelines

### For Pitch Detection
- **Required**: 10,000+ labeled audio samples
- **Duration**: 1-2 seconds per sample
- **Format**: WAV, 44.1kHz, mono
- **Labels**: Ground truth pitch (Hz)
- **Sources**: 
  - Synthesized pure tones
  - Real vocal recordings with reference pitch
  - Indian classical music recordings

### For Swar Classification
- **Required**: 5,000+ samples per swar class (60,000 total)
- **Duration**: 0.5-1 second per sample
- **Format**: WAV, 44.1kHz, mono
- **Labels**: Swar name (Sa, Re, Ga, etc.)
- **Sources**:
  - Recordings from trained musicians
  - Synthesized swar samples
  - Extracted from raga performances

### For Raga Classification
- **Required**: 100+ recordings per raga (1,000 total)
- **Duration**: 30-60 seconds per recording
- **Format**: WAV, 44.1kHz, mono
- **Labels**: Raga name
- **Sources**:
  - Professional raga performances
  - Student practice sessions
  - Curated classical music database

## 🔧 Model Architecture Details

### Pitch Detection (CREPE-style)
```python
Input: [batch, 1024, 1]
  ↓
Conv1D(1024, kernel=512, stride=4) + ReLU
  ↓
Conv1D(128, kernel=64, stride=1) + ReLU
  ↓
Conv1D(128, kernel=64, stride=1) + ReLU
  ↓
MaxPool1D(pool=2)
  ↓
Flatten
  ↓
Dense(256) + ReLU + Dropout(0.25)
  ↓
Dense(360)  # Pitch bins (50-500 Hz)
  ↓
Softmax
Output: [batch, 360] (pitch probability distribution)
```

### Swar Classification (Mel-CNN)
```python
Input: [batch, 128, 128, 1]  # Mel spectrogram
  ↓
Conv2D(32, 3x3) + ReLU + BatchNorm
  ↓
MaxPool2D(2x2)
  ↓
Conv2D(64, 3x3) + ReLU + BatchNorm
  ↓
MaxPool2D(2x2)
  ↓
Conv2D(128, 3x3) + ReLU + BatchNorm
  ↓
GlobalAveragePooling2D
  ↓
Dense(128) + ReLU + Dropout(0.5)
  ↓
Dense(12)  # 12 swar classes
  ↓
Softmax
Output: [batch, 12] (swar probabilities)
```

## 📈 Training Best Practices

### Data Augmentation
- Pitch shifting (±2 semitones)
- Time stretching (0.9x - 1.1x)
- Background noise addition
- Volume variation

### Hyperparameters
- **Learning Rate**: 0.001 (Adam optimizer)
- **Batch Size**: 32-64
- **Epochs**: 50-100
- **Early Stopping**: Patience = 10

### Validation Strategy
- 80/10/10 train/val/test split
- Stratified sampling by class
- Cross-validation for small datasets

## 🎯 Performance Targets

| Model | Metric | Target | Current |
|-------|--------|--------|---------|
| Pitch Detection | Accuracy (±50 cents) | > 95% | TBD |
| Pitch Detection | Inference Time | < 50ms | TBD |
| Swar Classification | Top-1 Accuracy | > 90% | TBD |
| Swar Classification | Top-3 Accuracy | > 98% | TBD |
| Raga Classification | Top-1 Accuracy | > 80% | TBD |
| Raga Classification | Top-3 Accuracy | > 95% | TBD |

## 🔄 Model Update Workflow

1. **Collect new data** → Add to `data/` directory
2. **Retrain model** → Run training script
3. **Evaluate performance** → Compare with baseline
4. **Convert to TFLite** → Optimize for mobile
5. **Test on device** → Verify inference speed
6. **Deploy** → Copy to `app/src/main/assets/`
7. **Version control** → Tag release in git

## 📝 Model Versioning

Models should be versioned using semantic versioning:
- `pitch_model_v1.0.0.tflite`
- `swar_classifier_v1.0.0.tflite`

Version increments:
- **Major**: Architecture changes
- **Minor**: Significant accuracy improvements
- **Patch**: Bug fixes, minor improvements

## 🧪 Testing & Evaluation

### Unit Tests
```bash
python -m pytest tests/
```

### Model Evaluation
```bash
python utils/evaluation.py --model models/pitch_model.tflite --test-data data/pitch_dataset/test/
```

### Android Integration Test
1. Deploy model to device
2. Run app with test recordings
3. Compare ML vs DSP results
4. Measure inference latency

## 📚 References

- **CREPE**: Kim et al., "CREPE: A Convolutional Representation for Pitch Estimation" (2018)
- **Mel Spectrograms**: Logan, "Mel Frequency Cepstral Coefficients for Music Modeling" (2000)
- **GMM for Music**: Pampalk et al., "Content-based organization and visualization of music archives" (2002)
- **TensorFlow Lite**: https://www.tensorflow.org/lite
- **Indian Classical Music Theory**: Bhatkhande Notation System

## 🤝 Contributing

When adding new models or improving existing ones:
1. Document architecture changes
2. Provide training scripts
3. Include evaluation metrics
4. Update this README
5. Create pull request with model performance comparison

## 📧 Contact

For questions about the ML pipeline, contact the ML team or create an issue in the repository.
